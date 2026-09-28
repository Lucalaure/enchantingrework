package lucalaure1007.enchantingrework.table;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lucalaure1007.enchantingrework.EnchantingRework;
import lucalaure1007.enchantingrework.catalyst.Catalyst;
import lucalaure1007.enchantingrework.config.EnchantingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The four levers of the reworked table:
 * <ol>
 *   <li>player level + bookshelves unlock the tiers, and each tier caps the enchantment level,</li>
 *   <li>a catalyst guarantees the main enchantment,</li>
 *   <li>lapis and catalysts pay for it, in equal amounts that climb with the enchantment level,</li>
 *   <li>enchanted books in nearby chiseled bookshelves steer the random picks.</li>
 * </ol>
 */
public final class EnchantingLogic {
	private EnchantingLogic() {
	}

	/** Number of option rows on the table, one per tier (like vanilla's three rows). */
	public static final int ROWS = 3;

	/**
	 * One row of the table: what clicking it would do. {@code tier} is 1-based. {@code levelRequirement} is the
	 * player level needed to click (vanilla's big green number); {@code clue} is the enchantment vanilla would hint at.
	 */
	public record Option(
		int tier,
		TableStatus status,
		int level,
		int xpCost,
		int lapisCost,
		int maxExtras,
		int levelRequirement,
		int power,
		@Nullable Holder<Enchantment> clue,
		int clueLevel,
		int catalystCost
	) {
		static Option unavailable(int tier, TableStatus status) {
			return new Option(tier, status, 0, 0, 0, 0, 0, 0, null, 0, 0);
		}

		public boolean ready() {
			return this.status == TableStatus.READY;
		}
	}

	/** One enchantment resonating around the table and how many books hold it. */
	public record ResonanceEntry(Holder<Enchantment> enchantment, int books) {
	}

	/** Resonating books around the table: the total, and the enchantments with the most books. */
	public record ResonanceSummary(int books, List<ResonanceEntry> top) {
		public static final ResonanceSummary EMPTY = new ResonanceSummary(0, List.of());
		public static final int SHOWN = 3;

		static ResonanceSummary of(Object2IntMap<Holder<Enchantment>> resonance) {
			List<ResonanceEntry> entries = new ArrayList<>();
			for (Object2IntMap.Entry<Holder<Enchantment>> entry : resonance.object2IntEntrySet()) {
				entries.add(new ResonanceEntry(entry.getKey(), entry.getIntValue()));
			}

			entries.sort(java.util.Comparator.comparingInt(ResonanceEntry::books).reversed()
				.thenComparing(e -> e.enchantment().getRegisteredName()));
			int total = entries.stream().mapToInt(ResonanceEntry::books).sum();
			return new ResonanceSummary(total, List.copyOf(entries.subList(0, Math.min(SHOWN, entries.size()))));
		}
	}

	/** What the table offers with the current inputs; everything the screen needs to show. */
	public record Preview(
		TableMode mode,
		TableStatus status,
		@Nullable Holder<Enchantment> main,
		@Nullable Holder<Enchantment> hint,
		int playerTier,
		int bookshelves,
		int passes,
		List<Option> options,
		ResonanceSummary resonance
	) {
		static Preview empty(TableStatus status, int playerTier, int bookshelves) {
			List<Option> options = new ArrayList<>();
			for (int t = 1; t <= ROWS; t++) {
				options.add(Option.unavailable(t, status));
			}

			return new Preview(TableMode.NONE, status, null, null, playerTier, bookshelves, 0, options, ResonanceSummary.EMPTY);
		}

		public Option option(int row) {
			return this.options.get(row);
		}
	}

	public static int countBookshelves(Level level, BlockPos pos) {
		int count = 0;
		for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
			if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
				count++;
			}
		}

		return count;
	}

	/** Highest tier (1-3) the player can use here, or 0 if the table is locked for them. */
	public static int tierFor(Player player, int bookshelves) {
		EnchantingConfig config = EnchantingRework.CONFIG;
		int playerLevel = player.hasInfiniteMaterials() ? Integer.MAX_VALUE : player.experienceLevel;
		int tier = 0;

		for (int t = 1; t <= config.tierCount(); t++) {
			if (playerLevel >= EnchantingConfig.at(config.tierPlayerLevel, t) && bookshelves >= EnchantingConfig.at(config.tierBookshelves, t)) {
				tier = t;
			}
		}

		return tier;
	}

	public static int passesOf(ItemStack stack) {
		// No enchantments (fresh, or cleaned at a grindstone) means no passes used, whatever the item remembers.
		if (!stack.isEnchanted()) {
			return 0;
		}

		// Enchanted elsewhere (loot, anvil, trades) counts as one pass.
		return Math.max(1, stack.getOrDefault(EnchantingRework.TABLE_PASSES, 1));
	}

	public static int passPenalty(int passes) {
		return EnchantingRework.CONFIG.passPenaltyBase * ((1 << Math.min(passes, 16)) - 1);
	}

	public static Preview compute(Level level, BlockPos pos, Player player, ItemStack item, ItemStack lapis, ItemStack catalystStack) {
		EnchantingConfig config = EnchantingRework.CONFIG;
		RegistryAccess access = level.registryAccess();
		int bookshelves = countBookshelves(level, pos);
		int playerTier = tierFor(player, bookshelves);
		int lapisCount = player.hasInfiniteMaterials() ? Integer.MAX_VALUE : lapis.getCount();

		if (item.isEmpty()) {
			return Preview.empty(TableStatus.NO_ITEM, playerTier, bookshelves);
		}

		if (!item.has(DataComponents.ENCHANTABLE)) {
			return Preview.empty(TableStatus.NOT_ENCHANTABLE, playerTier, bookshelves);
		}

		boolean enchanted = item.isEnchanted();
		int passes = passesOf(item);
		Catalyst catalyst = Catalyst.find(access, catalystStack);
		List<Option> options = new ArrayList<>();
		Object2IntMap<Holder<Enchantment>> resonance = resonance(level, pos);
		ResonanceSummary summary = ResonanceSummary.of(resonance);

		if (catalyst == null) {
			return vanillaRolls(level, player, item, lapisCount, playerTier, bookshelves, passes, resonance, summary);
		}

		Optional<Holder<Enchantment>> resolved = catalyst.resolveFor(access, item);
		if (resolved.isEmpty()) {
			return Preview.empty(TableStatus.NO_MATCH, playerTier, bookshelves);
		}

		// Catalyst: every row guarantees the catalyst's enchantment; higher tiers give a higher level and more extras.
		Holder<Enchantment> main = resolved.get();
		ItemEnchantments existing = EnchantmentHelper.getEnchantmentsForCrafting(item);
		Set<Holder<Enchantment>> others = new HashSet<>(existing.keySet());
		others.remove(main);
		boolean compatible = EnchantmentHelper.isEnchantmentCompatible(others, main);
		int catalystCount = player.hasInfiniteMaterials() ? Integer.MAX_VALUE : catalystStack.getCount();
		int anyExtras = 0;

		for (int tier = 1; tier <= ROWS; tier++) {
			int enchantLevel = maxMainLevel(tier, main);
			int xpCost = config.xpPerEnchantmentLevel * enchantLevel + (enchanted ? passPenalty(passes) : 0);
			int maxExtras = EnchantingConfig.at(config.tierMaxExtras, tier);
			// Lapis and catalysts cost the same, and climb steeply with the enchantment's level.
			int materialCost = config.materialCost(enchantLevel);
			anyExtras = Math.max(anyExtras, maxExtras);

			TableStatus status = TableStatus.READY;
			if (tier > playerTier) {
				status = TableStatus.TIER_LOCKED;
			} else if (enchanted && tier < config.reenchantMinTier) {
				status = TableStatus.REENCHANT_TIER;
			} else if (enchanted && passes >= config.maxTablePasses) {
				status = TableStatus.MAX_PASSES;
			} else if (existing.getLevel(main) >= enchantLevel) {
				status = TableStatus.ALREADY_STRONGER;
			} else if (!compatible) {
				status = TableStatus.INCOMPATIBLE;
			} else if (lapisCount < materialCost) {
				status = TableStatus.NEED_LAPIS;
			} else if (catalystCount < materialCost) {
				status = TableStatus.NEED_CATALYST;
			}

			options.add(new Option(tier, status, enchantLevel, xpCost, materialCost, maxExtras, 0, 0, null, 0, materialCost));
		}

		Holder<Enchantment> hint = null;
		if (anyExtras > 0) {
			others.add(main);
			hint = mostLikelyExtra(extraPool(level, item, others, resonance));
		}

		return new Preview(TableMode.CATALYST, TableStatus.READY, main, hint, playerTier, bookshelves, passes, options, summary);
	}

	/**
	 * No catalyst: vanilla's table inside the tier system. Row N is tier N: it unlocks with that tier and caps every
	 * enchantment it rolls at the tier's level (II / III / max). Bookshelves set each row's power as in vanilla,
	 * resonance steers which enchantments come up, rows cost 1 / 2 / 3 levels and lapis, and everything is seeded
	 * by the player's enchantment seed so the hints are honest.
	 */
	private static Preview vanillaRolls(
		Level level, Player player, ItemStack item, int lapisCount, int playerTier, int bookshelves, int passes,
		Object2IntMap<Holder<Enchantment>> resonance, ResonanceSummary summary
	) {
		List<Option> options = new ArrayList<>();
		if (item.isEnchanted()) {
			for (int row = 1; row <= ROWS; row++) {
				options.add(Option.unavailable(row, TableStatus.GAMBLE_FRESH_ONLY));
			}

			return new Preview(TableMode.GAMBLE, TableStatus.GAMBLE_FRESH_ONLY, null, null, playerTier, bookshelves, passes, options, summary);
		}

		RandomSource random = RandomSource.create(rollSeed(player, resonance));
		int[] powers = new int[ROWS];
		for (int row = 0; row < ROWS; row++) {
			powers[row] = EnchantmentHelper.getEnchantmentCost(random, row, bookshelves, item);
			if (powers[row] < row + 1) {
				powers[row] = 0;
			}
		}

		EnchantingConfig config = EnchantingRework.CONFIG;
		for (int row = 0; row < ROWS; row++) {
			int tier = row + 1;
			int cost = tier;
			int power = powers[row];
			if (power <= 0) {
				options.add(Option.unavailable(tier, TableStatus.NO_OFFER));
				continue;
			}

			RandomSource rowRandom = vanillaRowRandom(player, resonance, row);
			List<EnchantmentInstance> list = vanillaList(level.registryAccess(), item, power, tier, rowRandom, resonance);
			if (list.isEmpty()) {
				options.add(Option.unavailable(tier, TableStatus.NO_OFFER));
				continue;
			}

			EnchantmentInstance clue = list.get(rowRandom.nextInt(list.size()));
			TableStatus status = TableStatus.READY;
			if (tier > playerTier) {
				status = TableStatus.TIER_LOCKED;
			} else if (lapisCount < cost) {
				status = TableStatus.NEED_LAPIS;
			}

			int requirement = Math.max(power, EnchantingConfig.at(config.tierPlayerLevel, tier));
			options.add(new Option(tier, status, 0, cost, cost, 0, requirement, power, clue.enchantment(), clue.level(), 0));
		}

		return new Preview(TableMode.GAMBLE, TableStatus.READY, null, null, playerTier, bookshelves, passes, options, summary);
	}

	private static RandomSource vanillaRowRandom(Player player, Object2IntMap<Holder<Enchantment>> resonance, int row) {
		return RandomSource.create(rollSeed(player, resonance) + row);
	}

	/**
	 * The seed every roll at this table uses: the player's enchantment seed (which changes after each enchant, as in
	 * vanilla) mixed with the books resonating around the table. Changing the books gives a fresh roll; putting the
	 * same books back gives the same roll again, so swapping one book in and out can't be used to reroll forever.
	 * With no resonating books it is exactly vanilla's seed.
	 */
	public static int rollSeed(Player player, Object2IntMap<Holder<Enchantment>> resonance) {
		return rollSeed(player.getEnchantmentSeed(), resonance);
	}

	public static int rollSeed(int enchantmentSeed, Object2IntMap<Holder<Enchantment>> resonance) {
		int maxBooks = EnchantingRework.CONFIG.resonanceMaxBooksPerEnchantment;
		int hash = 0;
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : resonance.object2IntEntrySet()) {
			String id = entry.getKey().unwrapKey().map(key -> key.identifier().toString()).orElse("");
			// Order-independent: sum of per-enchantment mixes, using the count that actually matters for the odds.
			hash += it.unimi.dsi.fastutil.HashCommon.mix(id.hashCode() * 31 + Math.min(entry.getIntValue(), maxBooks));
		}

		return enchantmentSeed ^ hash;
	}

	private static List<EnchantmentInstance> vanillaList(
		RegistryAccess access, ItemStack item, int power, int tier, RandomSource random, Object2IntMap<Holder<Enchantment>> resonance
	) {
		Optional<HolderSet.Named<Enchantment>> tag = access.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);
		if (tag.isEmpty()) {
			return new ArrayList<>();
		}

		List<EnchantmentInstance> list = selectEnchantment(random, item, power, tag.get().stream(), resonance);
		if (item.is(Items.BOOK) && list.size() > 1) {
			list.remove(random.nextInt(list.size()));
		}

		// Resonance picks which enchantments; the tier alone decides how strong they can be.
		list.replaceAll(e -> new EnchantmentInstance(e.enchantment(), Math.min(e.level(), maxMainLevel(tier, e.enchantment()))));
		return list;
	}

	/**
	 * Vanilla's {@link EnchantmentHelper#selectEnchantment}, step for step, except each enchantment's weight gets the
	 * resonance bonus. With no resonating books it makes the same choices as vanilla.
	 */
	private static List<EnchantmentInstance> selectEnchantment(
		RandomSource random, ItemStack item, int power, java.util.stream.Stream<Holder<Enchantment>> source, Object2IntMap<Holder<Enchantment>> resonance
	) {
		List<EnchantmentInstance> results = new ArrayList<>();
		var enchantable = item.get(DataComponents.ENCHANTABLE);
		if (enchantable == null) {
			return results;
		}

		power += 1 + random.nextInt(enchantable.value() / 4 + 1) + random.nextInt(enchantable.value() / 4 + 1);
		float randomSpan = (random.nextFloat() + random.nextFloat() - 1.0F) * 0.15F;
		power = Mth.clamp(Math.round(power + power * randomSpan), 1, Integer.MAX_VALUE);
		List<EnchantmentInstance> available = EnchantmentHelper.getAvailableEnchantmentResults(power, item, source);
		if (available.isEmpty()) {
			return results;
		}

		java.util.function.ToIntFunction<EnchantmentInstance> weight = instance -> instance.weight() + resonanceBonus(resonance, instance.enchantment());
		WeightedRandom.getRandomItem(random, available, weight).ifPresent(results::add);

		while (random.nextInt(50) <= power) {
			if (!results.isEmpty()) {
				EnchantmentHelper.filterCompatibleEnchantments(available, results.getLast());
			}

			if (available.isEmpty()) {
				break;
			}

			WeightedRandom.getRandomItem(random, available, weight).ifPresent(results::add);
			power /= 2;
		}

		return results;
	}

	private static int resonanceBonus(Object2IntMap<Holder<Enchantment>> resonance, Holder<Enchantment> enchantment) {
		EnchantingConfig config = EnchantingRework.CONFIG;
		return Math.min(resonance.getInt(enchantment), config.resonanceMaxBooksPerEnchantment) * config.resonanceWeightPerBook;
	}

	public static int maxMainLevel(int tier, Holder<Enchantment> enchantment) {
		int max = enchantment.value().getMaxLevel();
		int cap = EnchantingConfig.at(EnchantingRework.CONFIG.tierMaxMainLevel, tier);
		return cap <= 0 ? max : Math.min(cap, max);
	}

	/** The enchantments row {@code option} applies, rolled with {@code random}. Empty means nothing happens. */
	public static List<EnchantmentInstance> roll(Level level, BlockPos pos, Player player, ItemStack item, Preview preview, Option option) {
		Object2IntMap<Holder<Enchantment>> resonance = resonance(level, pos);
		RandomSource random = RandomSource.create(rollSeed(player, resonance) + option.tier() - 1);
		return switch (preview.mode()) {
			case GAMBLE -> vanillaList(level.registryAccess(), item, option.power(), option.tier(), vanillaRowRandom(player, resonance, option.tier() - 1), resonance);
			case CATALYST -> rollCatalyst(level, item, preview.main(), option, random, resonance);
			case NONE -> List.of();
		};
	}

	private static List<EnchantmentInstance> rollCatalyst(
		Level level, ItemStack item, Holder<Enchantment> main, Option option, RandomSource random, Object2IntMap<Holder<Enchantment>> resonance
	) {
		EnchantingConfig config = EnchantingRework.CONFIG;
		List<EnchantmentInstance> result = new ArrayList<>();
		result.add(new EnchantmentInstance(main, option.level()));

		Set<Holder<Enchantment>> taken = new HashSet<>(EnchantmentHelper.getEnchantmentsForCrafting(item).keySet());
		taken.add(main);
		int maxExtraLevel = Math.max(1, EnchantingConfig.at(config.tierMaxExtraLevel, option.tier()));

		for (int i = 0; i < option.maxExtras(); i++) {
			if (random.nextFloat() >= config.extraChance) {
				continue;
			}

			Object2IntMap<Holder<Enchantment>> pool = extraPool(level, item, taken, resonance);
			Holder<Enchantment> pick = pickWeighted(pool, random);
			if (pick == null) {
				break;
			}

			int extraLevel = 1 + random.nextInt(Math.min(pick.value().getMaxLevel(), maxExtraLevel));
			result.add(new EnchantmentInstance(pick, extraLevel));
			taken.add(pick);
		}

		return result;
	}

	/**
	 * Enchantments that can appear as a random extra, with their odds. Only table enchantments qualify, so
	 * treasure enchantments (Mending, Frost Walker, curses...) never appear and never resonate.
	 */
	public static Object2IntMap<Holder<Enchantment>> extraPool(Level level, ItemStack item, Set<Holder<Enchantment>> taken, Object2IntMap<Holder<Enchantment>> resonance) {
		Object2IntMap<Holder<Enchantment>> pool = new Object2IntOpenHashMap<>();
		Optional<HolderSet.Named<Enchantment>> tag = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);
		if (tag.isEmpty()) {
			return pool;
		}

		boolean isBook = item.is(Items.BOOK);

		for (Holder<Enchantment> enchantment : tag.get()) {
			if (taken.contains(enchantment) || enchantment.is(EnchantmentTags.TREASURE) || enchantment.is(EnchantmentTags.CURSE)) {
				continue;
			}

			if (!isBook && !enchantment.value().isPrimaryItem(item)) {
				continue;
			}

			if (!EnchantmentHelper.isEnchantmentCompatible(taken, enchantment)) {
				continue;
			}

			pool.put(enchantment, enchantment.value().getWeight() + resonanceBonus(resonance, enchantment));
		}

		return pool;
	}

	/** How many enchanted books near the table hold each enchantment. */
	public static Object2IntMap<Holder<Enchantment>> resonance(Level level, BlockPos pos) {
		EnchantingConfig config = EnchantingRework.CONFIG;
		Object2IntMap<Holder<Enchantment>> counts = new Object2IntOpenHashMap<>();
		int r = config.resonanceRadius;

		for (BlockPos at : BlockPos.betweenClosed(pos.offset(-r, -config.resonanceBelow, -r), pos.offset(r, config.resonanceAbove, r))) {
			if (!(level.getBlockEntity(at) instanceof ChiseledBookShelfBlockEntity shelf)) {
				continue;
			}

			for (ItemStack book : shelf.getItems()) {
				ItemEnchantments stored = book.get(DataComponents.STORED_ENCHANTMENTS);
				if (stored == null) {
					continue;
				}

				for (Holder<Enchantment> enchantment : stored.keySet()) {
					if (enchantment.is(EnchantmentTags.IN_ENCHANTING_TABLE)) {
						counts.mergeInt(enchantment, 1, Integer::sum);
					}
				}
			}
		}

		return counts;
	}

	private static @Nullable Holder<Enchantment> mostLikelyExtra(Object2IntMap<Holder<Enchantment>> pool) {
		Holder<Enchantment> best = null;
		int bestWeight = 0;
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : pool.object2IntEntrySet()) {
			if (entry.getIntValue() > bestWeight) {
				best = entry.getKey();
				bestWeight = entry.getIntValue();
			}
		}

		return best;
	}

	private static @Nullable Holder<Enchantment> pickWeighted(Object2IntMap<Holder<Enchantment>> pool, RandomSource random) {
		int total = 0;
		for (int weight : pool.values()) {
			total += Math.max(0, weight);
		}

		if (total <= 0) {
			return null;
		}

		int roll = random.nextInt(total);
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : pool.object2IntEntrySet()) {
			roll -= Math.max(0, entry.getIntValue());
			if (roll < 0) {
				return entry.getKey();
			}
		}

		return null;
	}
}
