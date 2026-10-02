package lucalaure1007.enchantingrework.catalyst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lucalaure1007.enchantingrework.EnchantingRework;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A catalyst item and the theme it stands for. The first enchantment in {@code enchantments}
 * that supports the item being enchanted is the one the table applies; books take the first entry.
 *
 * <p>Entries are enchantment ids ({@code "minecraft:sharpness"}) or enchantment tags ({@code "#mymod:edge"}).
 * A tag expands to its members in tag order, so other mods can add their enchantments to a theme with a tag.
 *
 * <p>{@code inscription} (optional) gives the crafting ingredients that turn an enchanted book into this catalyst's
 * item; see {@code InscribeTemplateRecipe}.
 *
 * <p>Catalysts are datapack entries in {@code data/<namespace>/enchantingrework/catalyst/<name>.json}
 * and are synced to clients so the catalyst slot knows what it accepts.
 */
public record Catalyst(Item item, String theme, List<ExtraCodecs.TagOrElementLocation> enchantments, Optional<Inscription> inscription) {
	/** Crafting ingredients for inscribing: 4 material, 3 filler and 1 core around the enchanted book. */
	public record Inscription(Item material, Item core, Item filler) {
		public static final Codec<Inscription> CODEC = RecordCodecBuilder.create(i -> i.group(
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("material").forGetter(Inscription::material),
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("core").forGetter(Inscription::core),
			BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("filler", Items.AMETHYST_SHARD).forGetter(Inscription::filler)
		).apply(i, Inscription::new));
	}

	public static final ResourceKey<Registry<Catalyst>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnchantingRework.id("catalyst"));

	public static final Codec<Catalyst> CODEC = RecordCodecBuilder.create(i -> i.group(
		BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Catalyst::item),
		Codec.STRING.fieldOf("theme").forGetter(Catalyst::theme),
		ExtraCodecs.TAG_OR_ELEMENT_ID.listOf().fieldOf("enchantments").forGetter(Catalyst::enchantments),
		Inscription.CODEC.optionalFieldOf("inscription").forGetter(Catalyst::inscription)
	).apply(i, Catalyst::new));

	/** The theme's name, e.g. "Edge". Themes added by datapacks without a translation fall back to the theme id. */
	public Component displayName() {
		String fallback = this.theme.isEmpty() ? this.theme : Character.toUpperCase(this.theme.charAt(0)) + this.theme.substring(1).replace('_', ' ');
		return Component.translatableWithFallback("enchantingrework.theme." + this.theme, fallback);
	}

	/** Every enchantment this catalyst can give, in priority order, with tags expanded. Unknown ids are skipped. */
	public List<Holder<Enchantment>> candidates(RegistryAccess access) {
		Registry<Enchantment> registry = access.lookupOrThrow(Registries.ENCHANTMENT);
		Set<Holder<Enchantment>> result = new LinkedHashSet<>();

		for (ExtraCodecs.TagOrElementLocation entry : this.enchantments) {
			if (entry.tag()) {
				registry.get(TagKey.create(Registries.ENCHANTMENT, entry.id())).ifPresent(tag -> tag.forEach(result::add));
			} else {
				registry.get(ResourceKey.create(Registries.ENCHANTMENT, entry.id())).ifPresent(result::add);
			}
		}

		return List.copyOf(result);
	}

	/** The enchantment this catalyst produces on {@code stack}, or empty if none of its theme fits. */
	public Optional<Holder<Enchantment>> resolveFor(RegistryAccess access, ItemStack stack) {
		boolean isBook = stack.is(Items.BOOK);
		for (Holder<Enchantment> enchantment : this.candidates(access)) {
			if (isBook || enchantment.value().canEnchant(stack)) {
				return Optional.of(enchantment);
			}
		}

		return Optional.empty();
	}

	public static @Nullable Catalyst find(RegistryAccess access, ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}

		Optional<Registry<Catalyst>> registry = access.lookup(REGISTRY_KEY);
		if (registry.isEmpty()) {
			return null;
		}

		for (Catalyst catalyst : registry.get()) {
			if (stack.is(catalyst.item())) {
				return catalyst;
			}
		}

		return null;
	}

	public static boolean isCatalyst(RegistryAccess access, ItemStack stack) {
		return find(access, stack) != null;
	}
}
