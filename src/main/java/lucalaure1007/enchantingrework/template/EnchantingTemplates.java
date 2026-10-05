package lucalaure1007.enchantingrework.template;

import lucalaure1007.enchantingrework.EnchantingRework;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One enchanting template per catalyst theme. A template is made by inscribing an enchanted book (which is given
 * back) and is used up by one guaranteed enchant at the table. Rarity follows the inscription's core ingredient.
 */
public final class EnchantingTemplates {
	/** Templates by theme, in creative-tab order. */
	public static final Map<String, Item> BY_THEME = new LinkedHashMap<>();

	private EnchantingTemplates() {
	}

	public static void register() {
		// Common: paper core.
		register("air", Rarity.UNCOMMON);
		register("force", Rarity.UNCOMMON);
		register("blast", Rarity.UNCOMMON);
		register("deflect", Rarity.UNCOMMON);
		// Uncommon: diamond core.
		register("endurance", Rarity.RARE);
		register("speed", Rarity.RARE);
		register("edge", Rarity.RARE);
		register("guard", Rarity.RARE);
		register("fire", Rarity.RARE);
		register("water", Rarity.RARE);
		register("holy", Rarity.RARE);
		register("venom", Rarity.RARE);
		// Rare: echo shard core.
		register("fortune", Rarity.EPIC);
		register("delicacy", Rarity.EPIC);
	}

	private static void register(String theme, Rarity rarity) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, EnchantingRework.id(theme + "_enchanting_template"));
		Item.Properties properties = new Item.Properties()
			.setId(key)
			.rarity(rarity)
			.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		BY_THEME.put(theme, Registry.register(BuiltInRegistries.ITEM, key, new Item(properties)));
	}
}
