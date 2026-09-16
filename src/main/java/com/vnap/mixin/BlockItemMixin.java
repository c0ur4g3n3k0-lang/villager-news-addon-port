package com.vnap.mixin;

import com.vnap.dialogue.ContextualDialogueController;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Inject(method = "placeBlock", at = @At("RETURN"))
	private void vnap$afterSuccessfulPlacement(BlockPlaceContext context, BlockState state,
			CallbackInfoReturnable<Boolean> cir) {
		Player player = context.getPlayer();
		if (cir.getReturnValue() && player != null && context.getLevel() instanceof ServerLevel level) {
			ContextualDialogueController.onBlockPlaced(level, player, context.getClickedPos(), state);
		}
	}
}
