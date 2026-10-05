package lucalaure1007.enchantingrework.client.jei;

import lucalaure1007.enchantingrework.EnchantingRework;
import lucalaure1007.enchantingrework.catalyst.Catalyst;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI support (optional; loaded only when JEI is installed): inscribing recipes in JEI's crafting tab, findable from
 * any enchanted book they accept, and an info page on each template.
 */
public class EnchantingReworkJeiPlugin implements IModPlugin {
	@Override
	public Identifier getPluginUid() {
		return EnchantingRework.id("jei_plugin");
	}

	@Override
	public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
		registration.getCraftingCategory().addExtension(InscribingDisplayRecipe.class, new InscribingCraftingExtension());
	}

	@Override
	public void registerAdvanced(IAdvancedRegistration registration) {
		registration.addSimpleRecipeManagerPlugin(RecipeTypes.CRAFTING, new InscribingRecipeLookup(inscribingRecipes()));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		RegistryAccess access = registryAccess();
		if (access == null) {
			return;
		}

		access.lookup(Catalyst.REGISTRY_KEY).ifPresent(catalysts -> {
			for (Catalyst catalyst : catalysts) {
				List<Holder<Enchantment>> enchantments = inscribable(catalyst, access);
				if (enchantments.isEmpty()) {
					continue;
				}

				MutableComponent names = Component.empty();
				for (Holder<Enchantment> enchantment : enchantments) {
					if (!names.getSiblings().isEmpty()) {
						names.append(", ");
					}

					names.append(enchantment.value().description());
				}

				registration.addItemStackInfo(new ItemStack(catalyst.item()), Component.translatable("enchantingrework.jei.template_info", names));
			}
		});
	}

	private static List<RecipeHolder<CraftingRecipe>> inscribingRecipes() {
		RegistryAccess access = registryAccess();
		List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();
		if (access == null) {
			return recipes;
		}

		access.lookup(Catalyst.REGISTRY_KEY).ifPresent(catalysts -> {
			for (Catalyst catalyst : catalysts) {
				catalyst.inscription().ifPresent(inscription -> {
					List<ItemStack> books = inscribable(catalyst, access).stream()
						.map(e -> EnchantmentHelper.createBook(new EnchantmentInstance(e, e.value().getMaxLevel())))
						.toList();
					if (books.isEmpty()) {
						return;
					}

					Identifier id = EnchantingRework.id("inscribing/" + catalyst.theme());
					recipes.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, id), new InscribingDisplayRecipe(catalyst, inscription, books)));
				});
			}
		});
		return recipes;
	}

	private static List<Holder<Enchantment>> inscribable(Catalyst catalyst, RegistryAccess access) {
		return catalyst.candidates(access).stream()
			.filter(e -> !e.is(EnchantmentTags.TREASURE) && !e.is(EnchantmentTags.CURSE))
			.toList();
	}

	private static RegistryAccess registryAccess() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.level == null ? null : minecraft.level.registryAccess();
	}
}
