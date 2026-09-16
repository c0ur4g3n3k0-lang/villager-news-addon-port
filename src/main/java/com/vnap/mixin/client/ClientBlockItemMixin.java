package com.vnap.mixin.client;

import com.vnap.client.ClientOnlyDialogueController;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class ClientBlockItemMixin {
	@Inject(method = "placeBlock", at = @At("RETURN"))
	private void vnap$confirmClientPlacement(BlockPlaceContext context, BlockState state,
			CallbackInfoReturnable<Boolean> cir) {
		Player player = context.getPlayer();
		if (cir.getReturnValue() && player != null && context.getLevel().isClientSide()) {
			ClientOnlyDialogueController.onBlockPlacementPredicted(player, context.getClickedPos(), state);
		}
	}
}
