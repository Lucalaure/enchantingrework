package lucalaure1007.enchantingrework.template;

import com.mojang.serialization.MapCodec;
import lucalaure1007.enchantingrework.catalyst.Catalyst;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Inscribing: an enchanted book in the middle of a crafting grid, surrounded by its theme's ingredients, makes one
 * enchanting template and gives the book back.
 * <pre>
 *  M A M     M = theme material (x4)     A = filler, amethyst shard (x3)
 *  A B A     B = enchanted book (kept)   C = core (paper / diamond / echo shard)
 *  M C M
 * </pre>
 * The ingredients come from each catalyst's {@code inscription}, so datapacks can change or add them. The material
 * picks the theme, so a book with several enchantments can make a template for any theme it fits.
 */
public class InscribeTemplateRecipe extends CustomRecipe {
	public static final InscribeTemplateRecipe INSTANCE = new InscribeTemplateRecipe();
	public static final MapCodec<InscribeTemplateRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
	public static final StreamCodec<RegistryFriendlyByteBuf, InscribeTemplateRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
	public static final RecipeSerializer<InscribeTemplateRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	private static final int BOOK = 4;
	private static final int CORE = 7;
	private static final int[] MATERIAL = {0, 2, 6, 8};
	private static final int[] FILLER = {1, 3, 5};

	/** Set while a server runs; {@link #assemble} gets no level, and only servers craft. */
	private static @Nullable MinecraftServer server;

	public static void setServer(@Nullable MinecraftServer current) {
		server = current;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return find(input, level.registryAccess()) != null;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		if (server == null) {
			return ItemStack.EMPTY;
		}

		Catalyst catalyst = find(input, server.registryAccess());
		return catalyst == null ? ItemStack.EMPTY : new ItemStack(catalyst.item());
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
		NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
		for (int slot = 0; slot < input.size(); slot++) {
			ItemStack stack = input.getItem(slot);
			ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
			if (remainder != null) {
				remaining.set(slot, remainder.create());
			}
		}

		// The book is the master copy: it comes back.
		if (input.size() > BOOK && input.getItem(BOOK).is(Items.ENCHANTED_BOOK)) {
			remaining.set(BOOK, input.getItem(BOOK).copyWithCount(1));
		}

		return remaining;
	}

	@Override
	public RecipeSerializer<InscribeTemplateRecipe> getSerializer() {
		return SERIALIZER;
	}

	/** The catalyst whose inscription this grid matches, or null. */
	public static @Nullable Catalyst find(CraftingInput input, RegistryAccess access) {
		if (input.width() != 3 || input.height() != 3) {
			return null;
		}

		ItemStack book = input.getItem(BOOK);
		ItemEnchantments stored = book.get(DataComponents.STORED_ENCHANTMENTS);
		if (!book.is(Items.ENCHANTED_BOOK) || stored == null || stored.isEmpty()) {
			return null;
		}

		Optional<Registry<Catalyst>> catalysts = access.lookup(Catalyst.REGISTRY_KEY);
		if (catalysts.isEmpty()) {
			return null;
		}

		for (Catalyst catalyst : catalysts.get()) {
			Catalyst.Inscription inscription = catalyst.inscription().orElse(null);
			if (inscription == null || !input.getItem(CORE).is(inscription.core())) {
				continue;
			}

			if (!all(input, MATERIAL, inscription.material()) || !all(input, FILLER, inscription.filler())) {
				continue;
			}

			if (canInscribe(catalyst, stored, access)) {
				return catalyst;
			}
		}

		return null;
	}

	/** Whether a book holding {@code stored} can be inscribed into this catalyst's template. */
	public static boolean canInscribe(Catalyst catalyst, ItemEnchantments stored, RegistryAccess access) {
		List<Holder<Enchantment>> candidates = catalyst.candidates(access);
		for (Holder<Enchantment> enchantment : stored.keySet()) {
			if (!enchantment.is(EnchantmentTags.TREASURE) && !enchantment.is(EnchantmentTags.CURSE) && candidates.contains(enchantment)) {
				return true;
			}
		}

		return false;
	}

	private static boolean all(CraftingInput input, int[] slots, net.minecraft.world.item.Item item) {
		for (int slot : slots) {
			if (!input.getItem(slot).is(item)) {
				return false;
			}
		}

		return true;
	}
}
