package lucalaure1007.enchantingrework.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lucalaure1007.enchantingrework.EnchantingRework;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Every tunable number of the enchanting rework. Loaded from {@code config/enchantingrework.json};
 * missing fields keep their defaults and the file is rewritten so new options show up.
 *
 * <p>Array options are indexed by tier: index 0 is tier I, 1 is tier II, 2 is tier III.
 */
public class EnchantingConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** Minimum player level needed to open each tier. */
	public int[] tierPlayerLevel = {5, 15, 30};
	/** Minimum bookshelves around the table needed to open each tier. */
	public int[] tierBookshelves = {0, 8, 15};
	/** Highest level the main enchantment can reach at each tier; 0 means "the enchantment's own max". */
	public int[] tierMaxMainLevel = {2, 3, 0};
	/** How many random extras each tier can roll. */
	public int[] tierMaxExtras = {0, 1, 2};
	/** Highest level a random extra can roll at each tier. */
	public int[] tierMaxExtraLevel = {1, 1, 2};
	/**
	 * Lapis a catalyst enchant uses up, by the level of the main enchantment (index 0 is level I).
	 * Levels past the end of the list keep growing the same way (each adds the previous two).
	 */
	public int[] materialCostByLevel = {1, 2, 3, 5, 8};
	/** Catalysts (enchanting templates) used up by one guaranteed enchant. */
	public int catalystsPerEnchant = 1;
	/** Chance that each available extra slot actually rolls an extra. */
	public float extraChance = 0.5F;

	/** XP levels spent per level of the main enchantment. */
	public int xpPerEnchantmentLevel = 2;

	/** Horizontal radius around the table searched for chiseled bookshelves. */
	public int resonanceRadius = 4;
	/** Vertical range (below, above) around the table searched for chiseled bookshelves. */
	public int resonanceBelow = 1;
	public int resonanceAbove = 3;
	/** Weight added to an enchantment's extra-roll odds per resonating book. Vanilla weights are 1-10. */
	public int resonanceWeightPerBook = 10;
	/** Resonating books beyond this count for one enchantment add nothing. */
	public int resonanceMaxBooksPerEnchantment = 6;

	/**
	 * A chiseled bookshelf around the table counts as a bookshelf once it holds at least this many books
	 * (a normal bookshelf takes 3 books to craft). 0 disables it.
	 */
	public int chiseledShelfMinBooks = 3;

	/** Lapis needed for a main enchantment of {@code level}. */
	public int materialCost(int level) {
		if (level <= 0) {
			return 0;
		}

		int[] costs = this.materialCostByLevel;
		if (level <= costs.length) {
			return Math.max(1, costs[level - 1]);
		}

		// Modded enchantments above the list: continue the curve.
		int previous = costs.length >= 2 ? costs[costs.length - 2] : 1;
		int current = costs.length >= 1 ? costs[costs.length - 1] : 1;
		for (int l = costs.length; l < level; l++) {
			int next = previous + current;
			previous = current;
			current = next;
		}

		return Math.max(1, current);
	}

	public int tierCount() {
		return Math.min(this.tierPlayerLevel.length, Math.min(this.tierBookshelves.length, this.tierMaxMainLevel.length));
	}

	/** Tier value at 1-based {@code tier}, clamped to the array. */
	public static int at(int[] values, int tier) {
		if (values.length == 0 || tier <= 0) {
			return 0;
		}

		return values[Math.min(tier, values.length) - 1];
	}

	public String toJson() {
		return GSON.toJson(this);
	}

	public static EnchantingConfig fromJson(String json) {
		EnchantingConfig config = GSON.fromJson(json, EnchantingConfig.class);
		return config == null ? new EnchantingConfig() : config;
	}

	public static EnchantingConfig load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(EnchantingRework.MOD_ID + ".json");
		EnchantingConfig config = new EnchantingConfig();

		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				EnchantingConfig loaded = GSON.fromJson(reader, EnchantingConfig.class);
				if (loaded != null) {
					config = loaded;
				}
			} catch (IOException | RuntimeException e) {
				EnchantingRework.LOGGER.error("Failed to read {}, using defaults", path, e);
				return config;
			}
		}

		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(config, writer);
		} catch (IOException e) {
			EnchantingRework.LOGGER.warn("Failed to write {}", path, e);
		}

		return config;
	}
}
