package lucalaure1007.enchantingrework.client.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import lucalaure1007.enchantingrework.EnchantingRework;
import lucalaure1007.enchantingrework.catalyst.Catalyst;
import lucalaure1007.enchantingrework.config.EnchantingConfig;
import lucalaure1007.enchantingrework.table.EnchantingLogic;
import lucalaure1007.enchantingrework.table.ReworkedEnchantmentMenu;
import lucalaure1007.enchantingrework.table.TableMode;
import lucalaure1007.enchantingrework.table.TableStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Screen for the reworked table. Reuses vanilla's background and option sprites: the three rows are
 * tiers I-III. With a catalyst every row shows the guaranteed enchantment at that tier's level; without one
 * they are vanilla-style random rolls. The catalyst slot sits where vanilla draws the floating book.
 */
public class ReworkedEnchantmentScreen extends AbstractContainerScreen<ReworkedEnchantmentMenu> {
	private static final Identifier[] ENABLED_LEVEL_SPRITES = {
		Identifier.withDefaultNamespace("container/enchanting_table/level_1"),
		Identifier.withDefaultNamespace("container/enchanting_table/level_2"),
		Identifier.withDefaultNamespace("container/enchanting_table/level_3")
	};
	private static final Identifier[] DISABLED_LEVEL_SPRITES = {
		Identifier.withDefaultNamespace("container/enchanting_table/level_1_disabled"),
		Identifier.withDefaultNamespace("container/enchanting_table/level_2_disabled"),
		Identifier.withDefaultNamespace("container/enchanting_table/level_3_disabled")
	};
	private static final Identifier ROW_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/enchanting_table/enchantment_slot_disabled");
	private static final Identifier ROW_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/enchanting_table/enchantment_slot_highlighted");
	private static final Identifier ROW_SPRITE = Identifier.withDefaultNamespace("container/enchanting_table/enchantment_slot");
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/enchanting_table.png");

	private static final int ROW_X = 60;
	private static final int ROW_Y = 14;
	private static final int ROW_W = 108;
	private static final int ROW_H = 19;
	private static final int TEXT_COLOR = -9937334;
	private static final int TEXT_HOVER_COLOR = -128;
	private static final int TEXT_DISABLED_COLOR = ARGB.opaque((TEXT_COLOR & 16711422) >> 1);
	private static final int LABEL_COLOR = -12566464;
	private static final int PROBLEM_COLOR = -5636096;
	private static final int COST_COLOR = -8323296;
	private static final int COST_DISABLED_COLOR = -12550384;

	public ReworkedEnchantmentScreen(final ReworkedEnchantmentMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		this.minecraft.player.experienceDisplayStartTick = this.minecraft.player.tickCount;
	}

	private boolean overRow(final int row, final double mouseX, final double mouseY) {
		double x = mouseX - (this.leftPos + ROW_X);
		double y = mouseY - (this.topPos + ROW_Y + ROW_H * row);
		return x >= 0.0 && y >= 0.0 && x < ROW_W && y < ROW_H;
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		for (int row = 0; row < EnchantingLogic.ROWS; row++) {
			if (this.overRow(row, event.x(), event.y()) && this.menu.clickMenuButton(this.minecraft.player, row)) {
				this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, row);
				return true;
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int xo = this.leftPos;
		int yo = this.topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, xo + 24, yo + 21, 18, 18);

		EnchantmentNames.getInstance().initSeed(this.menu.getEnchantmentSeed());
		for (int row = 0; row < EnchantingLogic.ROWS; row++) {
			this.extractRow(graphics, row, mouseX, mouseY);
		}
	}

	private void extractRow(final GuiGraphicsExtractor graphics, final int row, final int mouseX, final int mouseY) {
		int x = this.leftPos + ROW_X;
		int y = this.topPos + ROW_Y + ROW_H * row;
		TableMode mode = this.menu.getMode();
		Optional<Holder.Reference<Enchantment>> main = this.menu.getMainEnchantment();

		if (mode == TableMode.NONE || (mode == TableMode.CATALYST && main.isEmpty())) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ROW_DISABLED_SPRITE, x, y, ROW_W, ROW_H);
			TableStatus status = this.menu.getStatus();
			if (row == 0 && status != TableStatus.READY && status != TableStatus.NO_ITEM) {
				graphics.textWithWordWrap(this.font, Component.translatable(status.translationKey()), x + 4, y + 2, ROW_W - 8, PROBLEM_COLOR, false);
			}

			return;
		}

		TableStatus rowStatus = this.menu.getRowStatus(row);
		if (mode == TableMode.GAMBLE && (rowStatus == TableStatus.NO_OFFER || rowStatus == TableStatus.GAMBLE_FRESH_ONLY)) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ROW_DISABLED_SPRITE, x, y, ROW_W, ROW_H);
			if (row == 0 && rowStatus == TableStatus.GAMBLE_FRESH_ONLY) {
				graphics.textWithWordWrap(this.font, Component.translatable(rowStatus.translationKey()), x + 4, y + 2, ROW_W - 8, PROBLEM_COLOR, false);
			}

			return;
		}

		// Vanilla rows show the level requirement; catalyst rows show what they actually cost.
		int shownCost = mode == TableMode.GAMBLE ? this.menu.getLevelRequirement(row) : this.menu.getXpCost(row);
		String costText = String.valueOf(shownCost);
		int textWidth = 86 - this.font.width(costText);
		boolean enabled = this.menu.canEnchant(this.minecraft.player, row);

		int textColor;
		int costColor;
		if (!enabled) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ROW_DISABLED_SPRITE, x, y, ROW_W, ROW_H);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DISABLED_LEVEL_SPRITES[row], x + 1, y + 1, 16, 16);
			textColor = TEXT_DISABLED_COLOR;
			costColor = COST_DISABLED_COLOR;
		} else {
			if (this.overRow(row, mouseX, mouseY)) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ROW_HIGHLIGHTED_SPRITE, x, y, ROW_W, ROW_H);
				graphics.requestCursor(CursorTypes.POINTING_HAND);
				textColor = TEXT_HOVER_COLOR;
			} else {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ROW_SPRITE, x, y, ROW_W, ROW_H);
				textColor = TEXT_COLOR;
			}

			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ENABLED_LEVEL_SPRITES[row], x + 1, y + 1, 16, 16);
			costColor = COST_COLOR;
		}

		if (mode == TableMode.GAMBLE) {
			FormattedText name = EnchantmentNames.getInstance().getRandomName(this.font, textWidth);
			graphics.textWithWordWrap(this.font, name, x + 20, y + 2, textWidth, textColor, false);
		} else {
			String name = Enchantment.getFullname(main.get(), this.menu.getEnchantLevel(row)).getString();
			graphics.text(this.font, this.font.plainSubstrByWidth(name, textWidth), x + 20, y + 2, textColor, false);
			if (this.menu.getMaxExtras(row) > 0) {
				Component extras = Component.translatable("enchantingrework.row.extras", this.menu.getMaxExtras(row));
				graphics.text(this.font, extras, x + 20, y + 10, textColor, false);
			}
		}

		graphics.text(this.font, costText, x + 20 + 86 - this.font.width(costText), y + 9, costColor);
	}

	@Override
	protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
		super.extractLabels(graphics, xm, ym);
		int tier = this.menu.getTier();
		Component tierText = tier <= 0
			? Component.translatable("enchantingrework.header.no_tier")
			: Component.translatable("enchantingrework.header.tier", Component.translatable("enchantment.level." + tier));
		Component header = Component.translatable("enchantingrework.header", tierText, this.menu.getBookshelves());
		graphics.text(this.font, header, this.imageWidth - 8 - this.font.width(header), this.titleLabelY, LABEL_COLOR, false);
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);

		for (int row = 0; row < EnchantingLogic.ROWS; row++) {
			if (this.overRow(row, mouseX, mouseY)) {
				List<Component> lines = this.rowTooltip(row);
				if (!lines.isEmpty()) {
					graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
				}

				return;
			}
		}

		if (this.hoveredSlot != null && this.hoveredSlot.index == ReworkedEnchantmentMenu.CATALYST_SLOT && !this.hoveredSlot.hasItem()) {
			graphics.setComponentTooltipForNextFrame(this.font, List.of(
				Component.translatable("enchantingrework.catalyst_slot").withStyle(ChatFormatting.WHITE),
				Component.translatable("enchantingrework.catalyst_slot.desc").withStyle(ChatFormatting.GRAY)
			), mouseX, mouseY);
		}
	}

	private List<Component> rowTooltip(final int row) {
		List<Component> lines = new ArrayList<>();
		TableMode mode = this.menu.getMode();
		int tier = row + 1;

		if (mode == TableMode.CATALYST) {
			Optional<Holder.Reference<Enchantment>> main = this.menu.getMainEnchantment();
			if (main.isEmpty()) {
				return lines;
			}

			lines.add(Enchantment.getFullname(main.get(), this.menu.getEnchantLevel(row)).copy().withStyle(ChatFormatting.WHITE));
			Catalyst catalyst = Catalyst.find(this.minecraft.level.registryAccess(), this.menu.getCatalyst());
			if (catalyst != null) {
				lines.add(Component.translatable("enchantingrework.tooltip.theme", catalyst.displayName()).withStyle(ChatFormatting.DARK_PURPLE));
			}

			int extras = this.menu.getMaxExtras(row);
			if (extras > 0) {
				lines.add(Component.translatable("enchantingrework.tooltip.extras", extras).withStyle(ChatFormatting.GRAY));
				this.menu.getHint().ifPresent(hint ->
					lines.add(Component.translatable("enchantingrework.tooltip.hint", hint.value().description()).withStyle(ChatFormatting.GRAY)));
				this.addResonanceLine(lines);
			} else {
				lines.add(Component.translatable("enchantingrework.tooltip.no_extras").withStyle(ChatFormatting.GRAY));
			}

			if (this.menu.getPasses() > 0) {
				lines.add(Component.translatable("enchantingrework.tooltip.pass", this.menu.getPasses() + 1, EnchantingRework.CONFIG.maxTablePasses)
					.withStyle(ChatFormatting.GRAY));
			}
		} else if (mode == TableMode.GAMBLE) {
			return this.vanillaTooltip(row);
		} else {
			return lines;
		}

		lines.add(CommonComponents.EMPTY);
		TableStatus status = this.menu.getRowStatus(row);
		if (mode == TableMode.CATALYST) {
			lines.add(this.tierRequirement(tier));
			int catalysts = this.menu.getCatalystCost(row);
			if (catalysts > 0 && !this.minecraft.player.hasInfiniteMaterials()) {
				lines.add(Component.translatable("enchantingrework.tooltip.catalyst_cost", catalysts, this.menu.getCatalyst().getHoverName())
					.withStyle(status == TableStatus.NEED_CATALYST ? ChatFormatting.RED : ChatFormatting.GRAY));
			}
		}

		if (status != TableStatus.READY) {
			lines.add(Component.translatable(status.translationKey()).withStyle(ChatFormatting.RED));
			return lines;
		}

		if (!this.minecraft.player.hasInfiniteMaterials()) {
			int lapis = this.menu.getLapisCost(row);
			lines.add(Component.translatable(lapis == 1 ? "container.enchant.lapis.one" : "container.enchant.lapis.many", lapis).withStyle(ChatFormatting.GRAY));
			int xp = this.menu.getXpCost(row);
			boolean affordable = this.minecraft.player.experienceLevel >= xp;
			lines.add(Component.translatable(xp == 1 ? "container.enchant.level.one" : "container.enchant.level.many", xp)
				.withStyle(affordable ? ChatFormatting.GRAY : ChatFormatting.RED));
		}

		return lines;
	}

	/** Same tooltip as vanilla's table: the clue, then the level requirement or the lapis/level cost. */
	private List<Component> vanillaTooltip(final int row) {
		List<Component> lines = new ArrayList<>();
		Optional<Holder.Reference<Enchantment>> clue = this.menu.getClue(row);
		if (clue.isEmpty()) {
			return lines;
		}

		lines.add(Component.translatable("container.enchant.clue", Enchantment.getFullname(clue.get(), this.menu.getClueLevel(row)))
			.withStyle(ChatFormatting.WHITE));
		lines.add(Component.translatable("enchantingrework.tooltip.gamble.desc").withStyle(ChatFormatting.DARK_PURPLE));
		this.addResonanceLine(lines);
		lines.add(CommonComponents.EMPTY);
		lines.add(this.tierRequirement(row + 1));
		if (this.menu.getRowStatus(row) == TableStatus.TIER_LOCKED) {
			lines.add(Component.translatable(TableStatus.TIER_LOCKED.translationKey()).withStyle(ChatFormatting.RED));
			return lines;
		}

		if (this.minecraft.player.hasInfiniteMaterials()) {
			return lines;
		}

		int requirement = this.menu.getLevelRequirement(row);
		if (this.minecraft.player.experienceLevel < requirement) {
			lines.add(Component.translatable("container.enchant.level.requirement", requirement).withStyle(ChatFormatting.RED));
			return lines;
		}

		int cost = this.menu.getLapisCost(row);
		boolean enoughLapis = this.menu.getRowStatus(row) != TableStatus.NEED_LAPIS;
		lines.add(Component.translatable(cost == 1 ? "container.enchant.lapis.one" : "container.enchant.lapis.many", cost)
			.withStyle(enoughLapis ? ChatFormatting.GRAY : ChatFormatting.RED));
		lines.add(Component.translatable(cost == 1 ? "container.enchant.level.one" : "container.enchant.level.many", cost).withStyle(ChatFormatting.GRAY));
		return lines;
	}

	private void addResonanceLine(final List<Component> lines) {
		int books = this.menu.getResonantBooks();
		if (books <= 0) {
			return;
		}

		lines.add(Component.translatable("enchantingrework.tooltip.resonance", books).withStyle(ChatFormatting.AQUA));
		int shown = 0;
		for (EnchantingLogic.ResonanceEntry entry : this.menu.getTopResonance()) {
			lines.add(Component.translatable("enchantingrework.tooltip.resonance.entry", entry.enchantment().value().description(), entry.books())
				.withStyle(ChatFormatting.DARK_AQUA));
			shown += entry.books();
		}

		if (shown < books) {
			lines.add(Component.translatable("enchantingrework.tooltip.resonance.more", books - shown).withStyle(ChatFormatting.DARK_AQUA));
		}
	}

	private Component tierRequirement(final int tier) {
		EnchantingConfig config = EnchantingRework.CONFIG;
		int level = EnchantingConfig.at(config.tierPlayerLevel, tier);
		int shelves = EnchantingConfig.at(config.tierBookshelves, tier);
		boolean met = this.menu.getTier() >= tier;
		return Component.translatable(
			"enchantingrework.tooltip.requires",
			Component.translatable("enchantment.level." + tier),
			level,
			shelves,
			this.menu.getBookshelves()
		).withStyle(met ? ChatFormatting.GREEN : ChatFormatting.RED);
	}
}
