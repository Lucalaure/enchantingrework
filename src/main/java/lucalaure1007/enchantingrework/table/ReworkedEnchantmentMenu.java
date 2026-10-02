package lucalaure1007.enchantingrework.table;

import lucalaure1007.enchantingrework.EnchantingRework;
import lucalaure1007.enchantingrework.catalyst.Catalyst;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Optional;

/**
 * Replaces vanilla's {@code EnchantmentMenu}. Slots: 0 item, 1 lapis, 2 catalyst. Buttons 0-2 are the three
 * tier rows, like vanilla's three options.
 * The server computes a {@link EnchantingLogic.Preview} and syncs it to the client through {@link #data}.
 */
public class ReworkedEnchantmentMenu extends AbstractContainerMenu {
	public static final int ITEM_SLOT = 0;
	public static final int LAPIS_SLOT = 1;
	public static final int CATALYST_SLOT = 2;
	private static final int INV_START = 3;
	private static final int INV_END = INV_START + 36;

	private static final Identifier EMPTY_SLOT_LAPIS_LAZULI = Identifier.withDefaultNamespace("container/slot/lapis_lazuli");

	// Synced data indices: shared values first, then ROW_FIELDS values per row.
	private static final int D_MODE = 0;
	private static final int D_STATUS = 1;
	private static final int D_MAIN = 2;
	private static final int D_HINT = 3;
	private static final int D_TIER = 4;
	private static final int D_SHELVES = 5;
	private static final int D_SEED = 6;
	private static final int D_RESONANCE = 7;
	/** Top resonating enchantments: id and book count, SHOWN pairs. */
	private static final int D_RES_TOP = 8;
	private static final int D_ROWS = D_RES_TOP + 2 * EnchantingLogic.ResonanceSummary.SHOWN;
	private static final int R_STATUS = 0;
	private static final int R_LEVEL = 1;
	private static final int R_XP = 2;
	private static final int R_LAPIS = 3;
	private static final int R_EXTRAS = 4;
	private static final int R_REQUIREMENT = 5;
	private static final int R_CLUE = 6;
	private static final int R_CLUE_LEVEL = 7;
	private static final int R_CATALYST = 8;
	private static final int ROW_FIELDS = 9;
	private static final int D_COUNT = D_ROWS + ROW_FIELDS * EnchantingLogic.ROWS;

	private final Container enchantSlots = new SimpleContainer(3) {
		@Override
		public void setChanged() {
			super.setChanged();
			ReworkedEnchantmentMenu.this.slotsChanged(this);
		}
	};
	private final ContainerLevelAccess access;
	private final Player player;
	private final ContainerData data = new SimpleContainerData(D_COUNT);
	private int lastPlayerLevel = -1;

	public ReworkedEnchantmentMenu(final int containerId, final Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public ReworkedEnchantmentMenu(final int containerId, final Inventory inventory, final ContainerLevelAccess access) {
		super(EnchantingRework.ENCHANTMENT_MENU, containerId);
		this.access = access;
		this.player = inventory.player;

		this.addSlot(new Slot(this.enchantSlots, ITEM_SLOT, 15, 47) {
			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		this.addSlot(new Slot(this.enchantSlots, LAPIS_SLOT, 35, 47) {
			@Override
			public boolean mayPlace(final ItemStack itemStack) {
				return itemStack.is(Items.LAPIS_LAZULI);
			}

			@Override
			public Identifier getNoItemIcon() {
				return EMPTY_SLOT_LAPIS_LAZULI;
			}
		});
		this.addSlot(new Slot(this.enchantSlots, CATALYST_SLOT, 25, 22) {
			@Override
			public boolean mayPlace(final ItemStack itemStack) {
				return Catalyst.isCatalyst(ReworkedEnchantmentMenu.this.player.level().registryAccess(), itemStack);
			}
		});
		this.addStandardInventorySlots(inventory, 8, 84);
		this.addDataSlots(this.data);
		this.data.set(D_MAIN, -1);
		this.data.set(D_HINT, -1);
		for (int row = 0; row < EnchantingLogic.ROWS; row++) {
			this.data.set(rowIndex(row, R_CLUE), -1);
		}

		for (int i = 0; i < EnchantingLogic.ResonanceSummary.SHOWN; i++) {
			this.data.set(D_RES_TOP + 2 * i, -1);
		}
	}

	@Override
	public void slotsChanged(final Container container) {
		if (container == this.enchantSlots) {
			this.refresh();
		}
	}

	@Override
	public void broadcastChanges() {
		// The tier depends on the player's level, which can change while the screen is open.
		if (this.player.experienceLevel != this.lastPlayerLevel) {
			this.refresh();
		}

		super.broadcastChanges();
	}

	private void refresh() {
		this.access.execute((level, pos) -> {
			this.lastPlayerLevel = this.player.experienceLevel;
			EnchantingLogic.Preview preview = EnchantingLogic.compute(
				level, pos, this.player,
				this.enchantSlots.getItem(ITEM_SLOT), this.enchantSlots.getItem(LAPIS_SLOT), this.enchantSlots.getItem(CATALYST_SLOT)
			);
			IdMap<Holder<Enchantment>> ids = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();

			this.data.set(D_MODE, preview.mode().ordinal());
			this.data.set(D_STATUS, preview.status().ordinal());
			this.data.set(D_MAIN, preview.main() == null ? -1 : ids.getId(preview.main()));
			this.data.set(D_HINT, preview.hint() == null ? -1 : ids.getId(preview.hint()));
			this.data.set(D_TIER, preview.playerTier());
			this.data.set(D_SHELVES, preview.bookshelves());
			this.data.set(D_SEED, EnchantingLogic.rollSeed(this.player, EnchantingLogic.resonance(level, pos)));
			this.data.set(D_RESONANCE, preview.resonance().books());
			List<EnchantingLogic.ResonanceEntry> top = preview.resonance().top();
			for (int i = 0; i < EnchantingLogic.ResonanceSummary.SHOWN; i++) {
				boolean present = i < top.size();
				this.data.set(D_RES_TOP + 2 * i, present ? ids.getId(top.get(i).enchantment()) : -1);
				this.data.set(D_RES_TOP + 2 * i + 1, present ? top.get(i).books() : 0);
			}
			for (int row = 0; row < EnchantingLogic.ROWS; row++) {
				EnchantingLogic.Option option = preview.option(row);
				this.data.set(rowIndex(row, R_STATUS), option.status().ordinal());
				this.data.set(rowIndex(row, R_LEVEL), option.level());
				this.data.set(rowIndex(row, R_XP), option.xpCost());
				this.data.set(rowIndex(row, R_LAPIS), option.lapisCost());
				this.data.set(rowIndex(row, R_EXTRAS), option.maxExtras());
				this.data.set(rowIndex(row, R_REQUIREMENT), option.levelRequirement());
				this.data.set(rowIndex(row, R_CLUE), option.clue() == null ? -1 : ids.getId(option.clue()));
				this.data.set(rowIndex(row, R_CLUE_LEVEL), option.clueLevel());
				this.data.set(rowIndex(row, R_CATALYST), option.catalystCost());
			}

			super.broadcastChanges();
		});
	}

	private static int rowIndex(final int row, final int field) {
		return D_ROWS + row * ROW_FIELDS + field;
	}

	/** Whether row {@code row} can be clicked by {@code player}, judged from the synced preview. */
	public boolean canEnchant(final Player player, final int row) {
		if (this.getMode() == TableMode.NONE || this.getRowStatus(row) != TableStatus.READY) {
			return false;
		}

		return player.hasInfiniteMaterials()
			|| (player.experienceLevel >= this.getXpCost(row) && player.experienceLevel >= this.getLevelRequirement(row));
	}

	@Override
	public boolean clickMenuButton(final Player player, final int buttonId) {
		if (buttonId < 0 || buttonId >= EnchantingLogic.ROWS) {
			return false;
		}

		if (!this.canEnchant(player, buttonId)) {
			return false;
		}

		this.access.execute((level, pos) -> {
			ItemStack item = this.enchantSlots.getItem(ITEM_SLOT);
			ItemStack lapis = this.enchantSlots.getItem(LAPIS_SLOT);
			ItemStack catalyst = this.enchantSlots.getItem(CATALYST_SLOT);
			EnchantingLogic.Preview preview = EnchantingLogic.compute(level, pos, player, item, lapis, catalyst);
			EnchantingLogic.Option option = preview.option(buttonId);
			boolean affordable = player.experienceLevel >= option.xpCost() && player.experienceLevel >= option.levelRequirement();
			if (!option.ready() || (!player.hasInfiniteMaterials() && !affordable)) {
				return;
			}

			List<EnchantmentInstance> enchantments = EnchantingLogic.roll(level, pos, player, item, preview, option);
			if (enchantments.isEmpty()) {
				return;
			}

			player.onEnchantmentPerformed(item, option.xpCost());
			ItemStack result = item.is(Items.BOOK) ? item.transmuteCopy(Items.ENCHANTED_BOOK) : item;
			for (EnchantmentInstance enchantment : enchantments) {
				result.enchant(enchantment.enchantment(), enchantment.level());
			}

			this.enchantSlots.setItem(ITEM_SLOT, result);
			lapis.consume(option.lapisCost(), player);
			if (lapis.isEmpty()) {
				this.enchantSlots.setItem(LAPIS_SLOT, ItemStack.EMPTY);
			}

			if (preview.mode() == TableMode.CATALYST) {
				catalyst.consume(option.catalystCost(), player);
				if (catalyst.isEmpty()) {
					this.enchantSlots.setItem(CATALYST_SLOT, ItemStack.EMPTY);
				}
			}

			player.awardStat(Stats.ENCHANT_ITEM);
			if (player instanceof ServerPlayer serverPlayer) {
				CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, result, option.xpCost());
				EnchantingRework.TABLE_ENCHANT.trigger(serverPlayer, preview.mode() == TableMode.CATALYST, preview.resonance().books());
			}

			this.enchantSlots.setChanged();
			level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
		});
		return true;
	}

	public TableMode getMode() {
		return TableMode.byId(this.data.get(D_MODE));
	}

	public TableStatus getStatus() {
		return TableStatus.byId(this.data.get(D_STATUS));
	}

	public Optional<Holder.Reference<Enchantment>> getMainEnchantment() {
		return this.enchantment(this.data.get(D_MAIN));
	}

	public Optional<Holder.Reference<Enchantment>> getHint() {
		return this.enchantment(this.data.get(D_HINT));
	}

	private Optional<Holder.Reference<Enchantment>> enchantment(final int id) {
		if (id < 0) {
			return Optional.empty();
		}

		return this.player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(id);
	}

	public TableStatus getRowStatus(final int row) {
		return TableStatus.byId(this.data.get(rowIndex(row, R_STATUS)));
	}

	public int getEnchantLevel(final int row) {
		return this.data.get(rowIndex(row, R_LEVEL));
	}

	public int getXpCost(final int row) {
		return this.data.get(rowIndex(row, R_XP));
	}

	public int getLapisCost(final int row) {
		return this.data.get(rowIndex(row, R_LAPIS));
	}

	public int getMaxExtras(final int row) {
		return this.data.get(rowIndex(row, R_EXTRAS));
	}

	public int getLevelRequirement(final int row) {
		return this.data.get(rowIndex(row, R_REQUIREMENT));
	}

	/** The enchantment vanilla-style rows hint at ("Sharpness III . . . ?"). */
	public Optional<Holder.Reference<Enchantment>> getClue(final int row) {
		return this.enchantment(this.data.get(rowIndex(row, R_CLUE)));
	}

	public int getClueLevel(final int row) {
		return this.data.get(rowIndex(row, R_CLUE_LEVEL));
	}

	public int getEnchantmentSeed() {
		return this.data.get(D_SEED);
	}

	/** Enchanted books in chiseled bookshelves around the table that count for resonance. */
	public int getResonantBooks() {
		return this.data.get(D_RESONANCE);
	}

	public int getCatalystCost(final int row) {
		return this.data.get(rowIndex(row, R_CATALYST));
	}

	/** Up to {@link EnchantingLogic.ResonanceSummary#SHOWN} enchantments with the most resonating books, most first. */
	public List<EnchantingLogic.ResonanceEntry> getTopResonance() {
		List<EnchantingLogic.ResonanceEntry> top = new java.util.ArrayList<>();
		for (int i = 0; i < EnchantingLogic.ResonanceSummary.SHOWN; i++) {
			int books = this.data.get(D_RES_TOP + 2 * i + 1);
			this.enchantment(this.data.get(D_RES_TOP + 2 * i))
				.ifPresent(enchantment -> top.add(new EnchantingLogic.ResonanceEntry(enchantment, books)));
		}

		return top;
	}

	public int getTier() {
		return this.data.get(D_TIER);
	}

	public int getBookshelves() {
		return this.data.get(D_SHELVES);
	}

	public ItemStack getCatalyst() {
		return this.enchantSlots.getItem(CATALYST_SLOT);
	}

	@Override
	public void removed(final Player player) {
		super.removed(player);
		this.access.execute((level, pos) -> this.clearContainer(player, this.enchantSlots));
	}

	@Override
	public boolean stillValid(final Player player) {
		return stillValid(this.access, player, Blocks.ENCHANTING_TABLE);
	}

	@Override
	public ItemStack quickMoveStack(final Player player, final int slotIndex) {
		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);
		if (slot == null || !slot.hasItem()) {
			return clicked;
		}

		ItemStack stack = slot.getItem();
		clicked = stack.copy();
		if (slotIndex < INV_START) {
			if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.is(Items.LAPIS_LAZULI)) {
			if (!this.moveItemStackTo(stack, LAPIS_SLOT, LAPIS_SLOT + 1, true)) {
				return ItemStack.EMPTY;
			}
		} else if (this.slots.get(CATALYST_SLOT).mayPlace(stack)) {
			if (!this.moveItemStackTo(stack, CATALYST_SLOT, CATALYST_SLOT + 1, true)) {
				return ItemStack.EMPTY;
			}
		} else {
			Slot itemSlot = this.slots.get(ITEM_SLOT);
			if (itemSlot.hasItem() || !itemSlot.mayPlace(stack)) {
				return ItemStack.EMPTY;
			}

			ItemStack single = stack.copyWithCount(1);
			stack.shrink(1);
			itemSlot.setByPlayer(single);
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}

		if (stack.getCount() == clicked.getCount()) {
			return ItemStack.EMPTY;
		}

		slot.onTake(player, stack);
		return clicked;
	}
}
