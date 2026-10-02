package lucalaure1007.enchantingrework.client;

import lucalaure1007.enchantingrework.EnchantingRework;
import lucalaure1007.enchantingrework.catalyst.Catalyst;
import lucalaure1007.enchantingrework.client.render.ChiseledBookShelfGlintRenderer;
import lucalaure1007.enchantingrework.client.screen.ReworkedEnchantmentScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import lucalaure1007.enchantingrework.config.EnchantingConfig;
import lucalaure1007.enchantingrework.network.ConfigSyncPayload;
import lucalaure1007.enchantingrework.template.EnchantingTemplates;
import lucalaure1007.enchantingrework.template.InscribeTemplateRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class EnchantingReworkClient implements ClientModInitializer {
	private static EnchantingConfig localConfig;

	@Override
	public void onInitializeClient() {
		MenuScreens.register(EnchantingRework.ENCHANTMENT_MENU, ReworkedEnchantmentScreen::new);
		BlockEntityRenderers.register(BlockEntityTypes.CHISELED_BOOKSHELF, ChiseledBookShelfGlintRenderer::new);

		// On a dedicated server, show the server's tiers and limits; restore our own config when we leave.
		// In singleplayer the integrated server already shares this config, so there's nothing to swap.
		ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE, (payload, context) -> {
			if (!context.client().hasSingleplayerServer()) {
				if (localConfig == null) {
					localConfig = EnchantingRework.CONFIG;
				}

				EnchantingRework.CONFIG = EnchantingConfig.fromJson(payload.json());
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			if (localConfig != null) {
				EnchantingRework.CONFIG = localConfig;
				localConfig = null;
			}
		});

		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> addCatalystSubtitle(stack, lines));
	}

	/**
	 * Templates get a vanilla smithing-template style tooltip; other catalysts get their theme and enchantments.
	 * Enchanted books list the templates they can be inscribed into.
	 */
	private static void addCatalystSubtitle(ItemStack stack, List<Component> lines) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || lines.isEmpty()) {
			return;
		}

		RegistryAccess access = minecraft.level.registryAccess();
		if (stack.is(Items.ENCHANTED_BOOK)) {
			addInscribableThemes(stack, lines, access);
			return;
		}

		Catalyst catalyst = Catalyst.find(access, stack);
		if (catalyst == null) {
			return;
		}

		MutableComponent results = Component.empty();
		for (var enchantment : catalyst.candidates(access)) {
			if (!results.getSiblings().isEmpty()) {
				results.append(", ");
			}

			results.append(enchantment.value().description());
		}

		if (EnchantingTemplates.BY_THEME.containsValue(stack.getItem())) {
			// Laid out like vanilla smithing templates: name, grey item type, then what it grants and needs.
			int at = 1;
			lines.add(at++, Component.translatable("enchantingrework.template").withStyle(ChatFormatting.GRAY));
			lines.add(at++, CommonComponents.EMPTY);
			lines.add(at++, Component.translatable("enchantingrework.template.grants").withStyle(ChatFormatting.GRAY));
			lines.add(at++, CommonComponents.space().append(results).withStyle(ChatFormatting.BLUE));
			lines.add(at++, Component.translatable("item.minecraft.smithing_template.ingredients").withStyle(ChatFormatting.GRAY));
			lines.add(at, CommonComponents.space().append(Items.LAPIS_LAZULI.getName(Items.LAPIS_LAZULI.getDefaultInstance())).withStyle(ChatFormatting.BLUE));
			return;
		}

		// Other (datapack) catalysts: theme and enchantments under the name.
		lines.add(1, Component.translatable("enchantingrework.tooltip.catalyst", catalyst.displayName()).withStyle(ChatFormatting.DARK_PURPLE));
		if (!results.getSiblings().isEmpty()) {
			lines.add(2, results.withStyle(ChatFormatting.GRAY));
		}
	}

	private static void addInscribableThemes(ItemStack book, List<Component> lines, RegistryAccess access) {
		ItemEnchantments stored = book.get(DataComponents.STORED_ENCHANTMENTS);
		var registry = access.lookup(Catalyst.REGISTRY_KEY);
		if (stored == null || stored.isEmpty() || registry.isEmpty()) {
			return;
		}

		MutableComponent themes = Component.empty();
		for (Catalyst catalyst : registry.get()) {
			if (catalyst.inscription().isPresent() && InscribeTemplateRecipe.canInscribe(catalyst, stored, access)) {
				if (!themes.getSiblings().isEmpty()) {
					themes.append(", ");
				}

				themes.append(catalyst.displayName());
			}
		}

		if (!themes.getSiblings().isEmpty()) {
			lines.add(Component.translatable("enchantingrework.tooltip.can_inscribe", themes).withStyle(ChatFormatting.DARK_PURPLE));
		}
	}
}
