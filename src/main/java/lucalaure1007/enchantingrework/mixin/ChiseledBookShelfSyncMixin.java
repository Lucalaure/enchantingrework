package lucalaure1007.enchantingrework.mixin;

import lucalaure1007.enchantingrework.EnchantingRework;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Sends a chiseled bookshelf's books to clients (vanilla only sends which slots are filled), so the client can
 * make enchanted books glint. Updates go out whenever a slot changes, since that changes the block state.
 */
@Mixin(ChiseledBookShelfBlockEntity.class)
public abstract class ChiseledBookShelfSyncMixin extends BlockEntity {
	@Shadow
	@Final
	private NonNullList<ItemStack> items;

	private ChiseledBookShelfSyncMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.problemPath(), EnchantingRework.LOGGER)) {
			TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
			ContainerHelper.saveAllItems(output, this.items, true);
			return output.buildResult();
		}
	}
}
