package lucalaure1007.enchantingrework.client.jei;

import lucalaure1007.enchantingrework.catalyst.Catalyst;
import lucalaure1007.enchantingrework.template.InscribeTemplateRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A display-only crafting recipe for JEI: one per template, shown in JEI's normal crafting tab. The real crafting is
 * done by {@link InscribeTemplateRecipe}; this only carries what to draw.
 *
 * @param books the books shown in the middle slot when no specific book is being looked up
 */
public class InscribingDisplayRecipe extends CustomRecipe {
	private final Catalyst catalyst;
	private final Catalyst.Inscription inscription;
	private final List<ItemStack> books;

	public InscribingDisplayRecipe(Catalyst catalyst, Catalyst.Inscription inscription, List<ItemStack> books) {
		this.catalyst = catalyst;
		this.inscription = inscription;
		this.books = books;
	}

	public Catalyst catalyst() {
		return this.catalyst;
	}

	public Catalyst.Inscription inscription() {
		return this.inscription;
	}

	public List<ItemStack> books() {
		return this.books;
	}

	public ItemStack result() {
		return new ItemStack(this.catalyst.item());
	}

	@Override
	public boolean isSpecial() {
		return false;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return false;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		return this.result();
	}

	@Override
	public RecipeSerializer<? extends CustomRecipe> getSerializer() {
		return InscribeTemplateRecipe.SERIALIZER;
	}
}
