package com.vnap.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WanderingTrader.class)
public abstract class WanderingTraderSoundMixin {
	@Inject(method = "getAmbientSound", at = @At("HEAD"), cancellable = true)
	private void vnap$removeVanillaAmbientSound(CallbackInfoReturnable<SoundEvent> cir) {
		if (vnap$isLogicalServer()) cir.setReturnValue(SoundEvents.EMPTY);
	}

	@Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
	private void vnap$removeVanillaHurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
		if (vnap$isLogicalServer()) cir.setReturnValue(SoundEvents.EMPTY);
	}

	@Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
	private void vnap$removeVanillaDeathSound(CallbackInfoReturnable<SoundEvent> cir) {
		if (vnap$isLogicalServer()) cir.setReturnValue(SoundEvents.EMPTY);
	}

	@Inject(method = "getNotifyTradeSound", at = @At("HEAD"), cancellable = true)
	private void vnap$removeVanillaTradeSound(CallbackInfoReturnable<SoundEvent> cir) {
		if (vnap$isLogicalServer()) cir.setReturnValue(SoundEvents.EMPTY);
	}

	@Inject(method = "getTradeUpdatedSound", at = @At("HEAD"), cancellable = true)
	private void vnap$removeVanillaTradeUpdatedSound(boolean sold, CallbackInfoReturnable<SoundEvent> cir) {
		if (vnap$isLogicalServer()) cir.setReturnValue(SoundEvents.EMPTY);
	}

	private boolean vnap$isLogicalServer() {
		return !((WanderingTrader) (Object) this).level().isClientSide();
	}
}
