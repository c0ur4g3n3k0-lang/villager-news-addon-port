package com.vnap.mixin.client;

import com.vnap.client.ClientOnlyVanillaSoundGate;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Shadow
	private ClientLevel level;

	@Inject(method = "handleSoundEvent", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/multiplayer/ClientLevel;playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V"),
		cancellable = true)
	private void vnap$suppressPositionedVanillaVoice(ClientboundSoundPacket packet, CallbackInfo ci) {
		if (ClientOnlyVanillaSoundGate.shouldSuppress(level, packet.getSound(),
				packet.getX(), packet.getY(), packet.getZ())) {
			ci.cancel();
		}
	}

	@Inject(method = "handleSoundEntityEvent", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/multiplayer/ClientLevel;playSeededSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V"),
		cancellable = true)
	private void vnap$suppressVanillaEntityVoice(ClientboundSoundEntityPacket packet, CallbackInfo ci) {
		Entity entity = level.getEntity(packet.getId());
		if (entity != null && ClientOnlyVanillaSoundGate.shouldSuppress(entity, packet.getSound())) {
			ci.cancel();
		}
	}
}
