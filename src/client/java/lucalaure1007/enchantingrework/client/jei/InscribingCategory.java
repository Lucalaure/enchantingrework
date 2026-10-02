package lucalaure1007.enchantingrework.client.jei;

import lucalaure1007.enchantingrework.EnchantingRework;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/** "Inscribing" page: the 3x3 grid with the book in the middle, an arrow and the template. */
public class InscribingCategory extends AbstractRecipeCategory<InscribingDisplay> {
	public static final IRecipeType<InscribingDisplay> TYPE = IRecipeType.create(EnchantingRework.MOD_ID, "inscribing", InscribingDisplay.class);
	private static final int SLOT = 18;

	public InscribingCategory(IGuiHelper guiHelper) {
		super(TYPE, Component.translatable("enchantingrework.jei.inscribing"), guiHelper.createDrawableItemLike(Items.CRAFTING_TABLE), 116, 54);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, InscribingDisplay recipe, IFocusGroup focuses) {
		// M A M / A B A / M C M, matching InscribeTemplateRecipe.
		char[] layout = {'M', 'A', 'M', 'A', 'B', 'A', 'M', 'C', 'M'};
		for (int i = 0; i < layout.length; i++) {
			int x = (i % 3) * SLOT;
			int y = (i / 3) * SLOT;
			var slot = builder.addInputSlot(x + 1, y + 1).setStandardSlotBackground();
			switch (layout[i]) {
				case 'M' -> slot.add(recipe.material());
				case 'A' -> slot.add(recipe.filler());
				case 'C' -> slot.add(recipe.core());
				default -> slot.addItemStacks(recipe.books()).addRichTooltipCallback((view, tooltip) ->
					tooltip.add(Component.translatable("enchantingrework.jei.book_kept").withStyle(ChatFormatting.GREEN)));
			}
		}

		builder.addOutputSlot(95, 19).setOutputSlotBackground().add(recipe.result());
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, InscribingDisplay recipe, IFocusGroup focuses) {
		builder.addRecipeArrowWidget().setPosition(61, 19);
	}
}
