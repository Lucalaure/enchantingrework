package lucalaure1007.enchantingrework.mixin;

import lucalaure1007.enchantingrework.table.ReworkedEnchantmentMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Opens the reworked menu instead of vanilla's {@code EnchantmentMenu}. */
@Mixin(EnchantingTableBlock.class)
public class EnchantingTableBlockMixin {
	@Inject(method = "getMenuProvider", at = @At("HEAD"), cancellable = true)
	private void enchantingrework$openReworkedMenu(BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<MenuProvider> cir) {
		if (level.getBlockEntity(pos) instanceof EnchantingTableBlockEntity enchantingTable) {
			Component title = enchantingTable.getDisplayName();
			cir.setReturnValue(new SimpleMenuProvider(
				(containerId, inventory, player) -> new ReworkedEnchantmentMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)), title
			));
		}
	}
}
