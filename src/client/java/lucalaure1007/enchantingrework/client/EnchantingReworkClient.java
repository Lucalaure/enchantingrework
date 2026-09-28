package lucalaure1007.enchantingrework.client;

import lucalaure1007.enchantingrework.EnchantingRework;
import lucalaure1007.enchantingrework.catalyst.Catalyst;
import lucalaure1007.enchantingrework.client.screen.ReworkedEnchantmentScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import lucalaure1007.enchantingrework.config.EnchantingConfig;
import lucalaure1007.enchantingrework.network.ConfigSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class EnchantingReworkClient implements ClientModInitializer {
	private static EnchantingConfig localConfig;

	@Override
	public void onInitializeClient() {
		MenuScreens.register(EnchantingRework.ENCHANTMENT_MENU, ReworkedEnchantmentScreen::new);

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

		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			addCatalystSubtitle(stack, lines);

			Integer passes = stack.get(EnchantingRework.TABLE_PASSES);
			if (passes != null && passes > 0 && stack.isEnchanted()) {
				lines.add(Component.translatable("enchantingrework.tooltip.passes", passes, EnchantingRework.CONFIG.maxTablePasses)
					.withStyle(ChatFormatting.DARK_GRAY));
			}
		});
	}

	/** Puts "Edge Catalyst" and the enchantments it can give right under the item name. */
	private static void addCatalystSubtitle(ItemStack stack, List<Component> lines) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || lines.isEmpty()) {
			return;
		}

		Catalyst catalyst = Catalyst.find(minecraft.level.registryAccess(), stack);
		if (catalyst == null) {
			return;
		}

		MutableComponent results = Component.empty();
		for (var enchantment : catalyst.candidates(minecraft.level.registryAccess())) {
			if (!results.getSiblings().isEmpty()) {
				results.append(", ");
			}

			results.append(enchantment.value().description());
		}

		lines.add(1, Component.translatable("enchantingrework.tooltip.catalyst", catalyst.displayName())
			.withStyle(ChatFormatting.DARK_PURPLE));
		if (!results.getSiblings().isEmpty()) {
			lines.add(2, results.withStyle(ChatFormatting.GRAY));
		}
	}
}
