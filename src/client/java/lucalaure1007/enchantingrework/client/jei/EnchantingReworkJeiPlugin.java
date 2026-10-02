package lucalaure1007.enchantingrework.client.jei;

import lucalaure1007.enchantingrework.EnchantingRework;
import lucalaure1007.enchantingrework.catalyst.Catalyst;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI support (optional; loaded only when JEI is installed): an "Inscribing" recipe page per template, and an info
 * page on each template explaining what it does at the enchanting table.
 */
public class EnchantingReworkJeiPlugin implements IModPlugin {
	@Override
	public Identifier getPluginUid() {
		return EnchantingRework.id("jei_plugin");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(new InscribingCategory(registration.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addCraftingStation(InscribingCategory.TYPE, Items.CRAFTING_TABLE);
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}

		RegistryAccess access = minecraft.level.registryAccess();
		var catalysts = access.lookup(Catalyst.REGISTRY_KEY);
		if (catalysts.isEmpty()) {
			return;
		}

		List<InscribingDisplay> displays = new ArrayList<>();
		for (Catalyst catalyst : catalysts.get()) {
			List<Holder<Enchantment>> enchantments = catalyst.candidates(access).stream()
				.filter(e -> !e.is(EnchantmentTags.TREASURE) && !e.is(EnchantmentTags.CURSE))
				.toList();

			catalyst.inscription().ifPresent(inscription -> {
				List<ItemStack> books = enchantments.stream()
					.map(e -> EnchantmentHelper.createBook(new EnchantmentInstance(e, 1)))
					.toList();
				if (!books.isEmpty()) {
					displays.add(new InscribingDisplay(books, inscription.material(), inscription.filler(), inscription.core(), new ItemStack(catalyst.item())));
				}
			});

			if (!enchantments.isEmpty()) {
				MutableComponent names = Component.empty();
				for (Holder<Enchantment> enchantment : enchantments) {
					if (!names.getSiblings().isEmpty()) {
						names.append(", ");
					}

					names.append(enchantment.value().description());
				}

				registration.addItemStackInfo(new ItemStack(catalyst.item()), Component.translatable("enchantingrework.jei.template_info", names));
			}
		}

		registration.addRecipes(InscribingCategory.TYPE, displays);
	}
}
