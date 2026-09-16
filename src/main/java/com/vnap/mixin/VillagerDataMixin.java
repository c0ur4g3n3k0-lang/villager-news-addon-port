package com.vnap.mixin;

import com.vnap.dialogue.ContextualDialogueController;
import com.vnap.entity.VillagerNewsData;
import com.vnap.entity.VillagerTradeBackup;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public abstract class VillagerDataMixin implements VillagerNewsData {
	@Unique
	private VillagerTradeBackup vnap$tradeBackup;
	@Unique
	private boolean vnap$readingSaveData;
	@Unique
	private boolean vnap$mayorDimensions;
	// Match Minecraft 26.2's baby-villager dimensions without changing the Mayor's adult age or trades.
	@Unique
	private static final EntityDimensions VNAP_MAYOR_DIMENSIONS = EntityDimensions.scalable(0.49F, 0.98F).withEyeHeight(0.63F);
	@Unique
	private static final EntityDataAccessor<Boolean> VNAP_HAS_NOSE = SynchedEntityData.defineId(Villager.class, EntityDataSerializers.BOOLEAN);
	@Unique
	private static final EntityDataAccessor<Integer> VNAP_COSMETIC = SynchedEntityData.defineId(Villager.class, EntityDataSerializers.INT);
	@Unique
	private static final EntityDataAccessor<Integer> VNAP_SIGN_MESSAGE = SynchedEntityData.defineId(Villager.class, EntityDataSerializers.INT);
	@Unique
	private static final EntityDataAccessor<Integer> VNAP_SIGN_TYPE = SynchedEntityData.defineId(Villager.class, EntityDataSerializers.INT);

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void vnap$defineData(SynchedEntityData.Builder builder, CallbackInfo ci) {
		builder.define(VNAP_HAS_NOSE, true);
		builder.define(VNAP_COSMETIC, 0);
		builder.define(VNAP_SIGN_MESSAGE, -1);
		builder.define(VNAP_SIGN_TYPE, -1);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void vnap$saveData(ValueOutput output, CallbackInfo ci) {
		output.putBoolean("VillagerNewsHasNose", vnap$hasNose());
		output.putInt("VillagerNewsCosmetic", vnap$cosmetic());
		output.putInt("VillagerNewsSignMessage", vnap$signMessage());
		output.putInt("VillagerNewsSignType", vnap$signType());
		if (vnap$tradeBackup != null) vnap$tradeBackup.save(output);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
	private void vnap$beginLoadData(ValueInput input, CallbackInfo ci) {
		vnap$readingSaveData = true;
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void vnap$loadData(ValueInput input, CallbackInfo ci) {
		vnap$setHasNose(input.getBooleanOr("VillagerNewsHasNose", true));
		vnap$setCosmetic(input.getIntOr("VillagerNewsCosmetic", 0));
		int signMessage = input.getIntOr("VillagerNewsSignMessage", -1);
		vnap$setSignMessage(signMessage);
		int equippedSign = ContextualDialogueController.signType(((Villager) (Object) this).getMainHandItem());
		vnap$setSignType(input.getIntOr("VillagerNewsSignType", equippedSign >= 0 ? equippedSign : signMessage >= 0 ? 0 : -1));
		if (equippedSign >= 0) ((Villager) (Object) this).setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		vnap$tradeBackup = VillagerTradeBackup.load(input).orElse(null);
		vnap$readingSaveData = false;
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void vnap$syncSpecialTrade(CallbackInfo ci) {
		Villager villager = (Villager) (Object) this;
		boolean mayor = !villager.isBaby() && ContextualDialogueController.isMayor(villager);
		if (mayor != vnap$mayorDimensions) {
			vnap$mayorDimensions = mayor;
			villager.refreshDimensions();
		}
		if (!villager.level().isClientSide()) ContextualDialogueController.ensureSpecialTrade(villager);
	}

	@Inject(method = "getDefaultDimensions", at = @At("RETURN"), cancellable = true)
	private void vnap$mayorHitbox(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		Villager villager = (Villager) (Object) this;
		if (!villager.isBaby() && ContextualDialogueController.isMayor(villager)) {
			cir.setReturnValue(VNAP_MAYOR_DIMENSIONS);
		}
	}

	@Redirect(
		method = "customServerAiStep",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;stopTrading()V")
	)
	private void vnap$keepSpecialTradeOpen(Villager villager) {
		if (!ContextualDialogueController.isSpecialTrader(villager)) villager.setTradingPlayer(null);
	}

	@ModifyVariable(method = "setVillagerData", at = @At("HEAD"), argsOnly = true)
	private VillagerData vnap$preventSpecialProfession(VillagerData value) {
		Villager villager = (Villager) (Object) this;
		if (vnap$readingSaveData) return value;
		if (ContextualDialogueController.isSpecialTrader(villager)
				&& !villager.level().isClientSide() && vnap$tradeBackup == null) {
			vnap$tradeBackup = VillagerTradeBackup.capture(villager, false);
		}
		return ContextualDialogueController.isSpecialTrader(villager)
			? value.withProfession(villager.level().registryAccess(), VillagerProfession.NONE).withLevel(1)
			: value;
	}

	@Override
	public VillagerTradeBackup vnap$tradeBackup() {
		return vnap$tradeBackup;
	}

	@Override
	public void vnap$setTradeBackup(VillagerTradeBackup value) {
		vnap$tradeBackup = value;
	}

	@Override
	public boolean vnap$hasNose() {
		return ((Villager) (Object) this).getEntityData().get(VNAP_HAS_NOSE);
	}

	@Override
	public void vnap$setHasNose(boolean value) {
		((Villager) (Object) this).getEntityData().set(VNAP_HAS_NOSE, value);
	}

	@Override
	public int vnap$cosmetic() {
		return ((Villager) (Object) this).getEntityData().get(VNAP_COSMETIC);
	}

	@Override
	public void vnap$setCosmetic(int value) {
		((Villager) (Object) this).getEntityData().set(VNAP_COSMETIC, Math.max(0, Math.min(4, value)));
	}

	@Override
	public int vnap$signMessage() {
		return ((Villager) (Object) this).getEntityData().get(VNAP_SIGN_MESSAGE);
	}

	@Override
	public void vnap$setSignMessage(int value) {
		((Villager) (Object) this).getEntityData().set(VNAP_SIGN_MESSAGE, Math.max(-1, Math.min(86, value)));
	}

	@Override
	public int vnap$signType() {
		return ((Villager) (Object) this).getEntityData().get(VNAP_SIGN_TYPE);
	}

	@Override
	public void vnap$setSignType(int value) {
		((Villager) (Object) this).getEntityData().set(VNAP_SIGN_TYPE, Math.max(-1, Math.min(11, value)));
	}
}
