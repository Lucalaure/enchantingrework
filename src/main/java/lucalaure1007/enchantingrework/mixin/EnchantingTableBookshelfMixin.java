package lucalaure1007.enchantingrework.mixin;

import lucalaure1007.enchantingrework.EnchantingRework;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chiseled bookshelves holding enough books count as bookshelves for the table. Uses the block state's
 * slot properties, so it works on the client too (enchanting particles fly from them).
 */
@Mixin(EnchantingTableBlock.class)
public class EnchantingTableBookshelfMixin {
	@Inject(method = "isValidBookShelf", at = @At("RETURN"), cancellable = true)
	private static void enchantingrework$countChiseledShelves(Level level, BlockPos pos, BlockPos offset, CallbackInfoReturnable<Boolean> cir) {
		int minBooks = EnchantingRework.CONFIG.chiseledShelfMinBooks;
		if (cir.getReturnValueZ() || minBooks <= 0) {
			return;
		}

		BlockState shelf = level.getBlockState(pos.offset(offset));
		if (!(shelf.getBlock() instanceof ChiseledBookShelfBlock)) {
			return;
		}

		int books = 0;
		for (BooleanProperty slot : ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES) {
			if (shelf.getValue(slot)) {
				books++;
			}
		}

		BlockPos between = pos.offset(offset.getX() / 2, offset.getY(), offset.getZ() / 2);
		if (books >= minBooks && level.getBlockState(between).is(BlockTags.ENCHANTMENT_POWER_TRANSMITTER)) {
			cir.setReturnValue(true);
		}
	}
}
