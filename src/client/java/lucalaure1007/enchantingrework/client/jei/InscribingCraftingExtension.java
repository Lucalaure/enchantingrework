package lucalaure1007.enchantingrework.client.jei;

import lucalaure1007.enchantingrework.template.InscribeTemplateRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Draws inscribing recipes in JEI's crafting tab: M A M / A B A / M C M, with the looked-up book in the middle. */
public class InscribingCraftingExtension implements ICraftingCategoryExtension<InscribingDisplayRecipe> {
	private static final int BOOK = 4;

	@Override
	public List<SlotDisplay> getIngredients(RecipeHolder<InscribingDisplayRecipe> holder) {
		InscribingDisplayRecipe recipe = holder.value();
		List<SlotDisplay> displays = new ArrayList<>();
		for (Item item : grid(recipe, Items.ENCHANTED_BOOK)) {
			displays.add(new SlotDisplay.ItemSlotDisplay(item.builtInRegistryHolder()));
		}

		return displays;
	}

	@Override
	public int getWidth(RecipeHolder<InscribingDisplayRecipe> holder) {
		return 3;
	}

	@Override
	public int getHeight(RecipeHolder<InscribingDisplayRecipe> holder) {
		return 3;
	}

	@Override
	public void setRecipe(RecipeHolder<InscribingDisplayRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
		InscribingDisplayRecipe recipe = holder.value();
		// Looking up a specific book (any level, any number of enchantments)? Show exactly that book.
		List<ItemStack> books = focusedBook(recipe, focuses).map(List::of).orElse(recipe.books());

		List<List<ItemStack>> inputs = new ArrayList<>();
		List<Item> items = grid(recipe, Items.ENCHANTED_BOOK);
		for (int slot = 0; slot < items.size(); slot++) {
			inputs.add(slot == BOOK ? books : List.of(new ItemStack(items.get(slot))));
		}

		List<IRecipeSlotBuilder> slots = grid.createAndSetInputs(builder, inputs, 3, 3);
		if (slots.size() > BOOK) {
			slots.get(BOOK).addRichTooltipCallback((view, tooltip) ->
				tooltip.add(Component.translatable("enchantingrework.jei.book_kept").withStyle(ChatFormatting.GREEN)));
		}

		grid.createAndSetOutputs(builder, List.of(recipe.result()));
	}

	private static Optional<ItemStack> focusedBook(InscribingDisplayRecipe recipe, IFocusGroup focuses) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return Optional.empty();
		}

		return focuses.getItemStackFocuses(RecipeIngredientRole.INPUT)
			.map(focus -> focus.getTypedValue().getIngredient())
			.filter(stack -> canInscribe(recipe, stack))
			.findFirst()
			.map(stack -> stack.copyWithCount(1));
	}

	static boolean canInscribe(InscribingDisplayRecipe recipe, ItemStack stack) {
		Minecraft minecraft = Minecraft.getInstance();
		ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
		return minecraft.level != null && stack.is(Items.ENCHANTED_BOOK) && stored != null
			&& InscribeTemplateRecipe.canInscribe(recipe.catalyst(), stored, minecraft.level.registryAccess());
	}

	/** The grid's items in slot order, with {@code book} in the middle. */
	static List<Item> grid(InscribingDisplayRecipe recipe, Item book) {
		Item m = recipe.inscription().material();
		Item a = recipe.inscription().filler();
		Item c = recipe.inscription().core();
		return List.of(m, a, m, a, book, a, m, c, m);
	}
}
