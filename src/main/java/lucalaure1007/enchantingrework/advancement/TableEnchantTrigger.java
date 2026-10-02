package lucalaure1007.enchantingrework.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Optional;

/**
 * {@code enchantingrework:table_enchant}: fires whenever a player enchants at the reworked table.
 * <ul>
 *   <li>{@code catalyst}: whether an enchanting template (or other catalyst) was used,</li>
 *   <li>{@code resonant_books}: how many enchanted books were resonating in nearby chiseled bookshelves.</li>
 * </ul>
 */
public class TableEnchantTrigger extends SimpleCriterionTrigger<TableEnchantTrigger.TriggerInstance> {
	@Override
	public Codec<TriggerInstance> codec() {
		return TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer player, boolean catalyst, int resonantBooks) {
		this.trigger(player, instance -> instance.matches(catalyst, resonantBooks));
	}

	public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Optional<Boolean> catalyst, MinMaxBounds.Ints resonantBooks)
		implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
			LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
			Codec.BOOL.optionalFieldOf("catalyst").forGetter(TriggerInstance::catalyst),
			MinMaxBounds.Ints.CODEC.optionalFieldOf("resonant_books", MinMaxBounds.Ints.ANY).forGetter(TriggerInstance::resonantBooks)
		).apply(i, TriggerInstance::new));

		public boolean matches(boolean usedCatalyst, int books) {
			return (this.catalyst.isEmpty() || this.catalyst.get() == usedCatalyst) && this.resonantBooks.matches(books);
		}
	}
}
