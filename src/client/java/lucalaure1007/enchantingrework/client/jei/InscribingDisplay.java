package lucalaure1007.enchantingrework.client.jei;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** One inscribing recipe for JEI: the books that work, the theme's ingredients and the template it makes. */
public record InscribingDisplay(List<ItemStack> books, Item material, Item filler, Item core, ItemStack result) {
}
