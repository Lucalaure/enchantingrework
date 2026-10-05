package lucalaure1007.enchantingrework.client.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.function.Predicate;

/**
 * Answers JEI lookups for inscribing recipes. Books are matched by what they can be inscribed into, so a book of any
 * level, or with several enchantments, finds every template it can make.
 */
public class InscribingRecipeLookup implements ISimpleRecipeManagerPlugin<RecipeHolder<CraftingRecipe>> {
	private final List<RecipeHolder<CraftingRecipe>> recipes;

	public InscribingRecipeLookup(List<RecipeHolder<CraftingRecipe>> recipes) {
		this.recipes = recipes;
	}

	@Override
	public boolean isHandledInput(ITypedIngredient<?> input) {
		return !this.getRecipesForInput(input).isEmpty();
	}

	@Override
	public boolean isHandledOutput(ITypedIngredient<?> output) {
		return !this.getRecipesForOutput(output).isEmpty();
	}

	@Override
	public List<RecipeHolder<CraftingRecipe>> getRecipesForInput(ITypedIngredient<?> input) {
		return input.getIngredient(VanillaTypes.ITEM_STACK).map(stack -> this.filter(recipe -> stack.is(Items.ENCHANTED_BOOK)
			? InscribingCraftingExtension.canInscribe(recipe, stack)
			: InscribingCraftingExtension.grid(recipe, Items.ENCHANTED_BOOK).stream().anyMatch(stack::is))).orElse(List.of());
	}

	@Override
	public List<RecipeHolder<CraftingRecipe>> getRecipesForOutput(ITypedIngredient<?> output) {
		return output.getIngredient(VanillaTypes.ITEM_STACK).map(stack -> this.filter(recipe -> stack.is(recipe.catalyst().item()))).orElse(List.of());
	}

	@Override
	public List<RecipeHolder<CraftingRecipe>> getAllRecipes() {
		return this.recipes;
	}

	private List<RecipeHolder<CraftingRecipe>> filter(Predicate<InscribingDisplayRecipe> test) {
		return this.recipes.stream().filter(holder -> test.test((InscribingDisplayRecipe) holder.value())).toList();
	}
}
