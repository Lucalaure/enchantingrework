package lucalaure1007.enchantingrework.client;

import lucalaure1007.enchantingrework.EnchantingRework;
import lucalaure1007.enchantingrework.catalyst.Catalyst;
import lucalaure1007.enchantingrework.client.screen.ReworkedEnchantmentScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public class EnchantingReworkClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(EnchantingRework.ENCHANTMENT_MENU, ReworkedEnchantmentScreen::new);

		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			addCatalystSubtitle(stack, lines);

			Integer passes = stack.get(EnchantingRework.TABLE_PASSES);
			if (passes != null && passes > 0) {
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

		Registry<Enchantment> enchantments = minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		MutableComponent results = Component.empty();
		for (var key : catalyst.enchantments()) {
			var holder = enchantments.get(key);
			if (holder.isEmpty()) {
				continue;
			}

			if (!results.getSiblings().isEmpty()) {
				results.append(", ");
			}

			results.append(holder.get().value().description());
		}

		lines.add(1, Component.translatable("enchantingrework.tooltip.catalyst", Component.translatable(catalyst.translationKey()))
			.withStyle(ChatFormatting.DARK_PURPLE));
		if (!results.getSiblings().isEmpty()) {
			lines.add(2, results.withStyle(ChatFormatting.GRAY));
		}
	}
}
