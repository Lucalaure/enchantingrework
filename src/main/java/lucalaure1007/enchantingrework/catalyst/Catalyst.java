package lucalaure1007.enchantingrework.catalyst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lucalaure1007.enchantingrework.EnchantingRework;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * A catalyst item and the theme it stands for. The first enchantment in {@code enchantments}
 * that supports the item being enchanted is the one the table applies; books take the first entry.
 *
 * <p>Catalysts are datapack entries in {@code data/<namespace>/enchantingrework/catalyst/<name>.json}
 * and are synced to clients so the catalyst slot knows what it accepts.
 */
public record Catalyst(Item item, String theme, List<ResourceKey<Enchantment>> enchantments) {
	public static final ResourceKey<Registry<Catalyst>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnchantingRework.id("catalyst"));

	public static final Codec<Catalyst> CODEC = RecordCodecBuilder.create(i -> i.group(
		BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Catalyst::item),
		Codec.STRING.fieldOf("theme").forGetter(Catalyst::theme),
		ResourceKey.codec(Registries.ENCHANTMENT).listOf().fieldOf("enchantments").forGetter(Catalyst::enchantments)
	).apply(i, Catalyst::new));

	public String translationKey() {
		return "enchantingrework.theme." + this.theme;
	}

	/** The enchantment this catalyst produces on {@code stack}, or empty if none of its theme fits. */
	public Optional<Holder<Enchantment>> resolveFor(RegistryAccess access, ItemStack stack) {
		Registry<Enchantment> registry = access.lookupOrThrow(Registries.ENCHANTMENT);
		boolean isBook = stack.is(Items.BOOK);

		for (ResourceKey<Enchantment> key : this.enchantments) {
			Optional<Holder.Reference<Enchantment>> holder = registry.get(key);
			if (holder.isPresent() && (isBook || holder.get().value().canEnchant(stack))) {
				return Optional.of(holder.get());
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
