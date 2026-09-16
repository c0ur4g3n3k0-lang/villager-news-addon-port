package com.vnap.client;

import com.vnap.config.VillagerNewsSettings;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.phys.AABB;

/**
 * Suppresses vanilla character voices while the isolated client-only dialogue
 * engine is replacing them. Non-vocal entity sounds are left untouched.
 */
public final class ClientOnlyVanillaSoundGate {
	private static final double WOOLY_SOUND_RADIUS = 1.5D;

	private ClientOnlyVanillaSoundGate() {
	}

	public static boolean shouldSuppress(Entity entity, Holder<SoundEvent> sound) {
		return suppressionEnabled() && isCharacterVoice(entity, sound.value());
	}

	public static boolean shouldSuppress(ClientLevel level, Holder<SoundEvent> sound,
			double x, double y, double z) {
		if (!suppressionEnabled()) return false;
		SoundEvent event = sound.value();
		if (isVillagerVoice(event) || isWanderingTraderVoice(event)) return true;
		if (!isSheepVoice(event)) return false;
		AABB searchArea = new AABB(
			x - WOOLY_SOUND_RADIUS, y - WOOLY_SOUND_RADIUS, z - WOOLY_SOUND_RADIUS,
			x + WOOLY_SOUND_RADIUS, y + WOOLY_SOUND_RADIUS, z + WOOLY_SOUND_RADIUS
		);
		return !level.getEntitiesOfClass(Sheep.class, searchArea, ClientOnlyVanillaSoundGate::isWooly).isEmpty();
	}

	private static boolean suppressionEnabled() {
		return ServerCompatibilityState.clientOnlyFallback() && VillagerNewsSettings.dialogueEnabled();
	}

	private static boolean isCharacterVoice(Entity entity, SoundEvent sound) {
		if (entity instanceof Villager) {
			return isVillagerVoice(sound);
		}
		if (entity instanceof WanderingTrader) {
			return isWanderingTraderVoice(sound);
		}
		if (entity instanceof Sheep sheep && isWooly(sheep)) {
			return isSheepVoice(sound);
		}
		return false;
	}

	private static boolean isVillagerVoice(SoundEvent sound) {
		return sound.equals(SoundEvents.VILLAGER_AMBIENT)
			|| sound.equals(SoundEvents.VILLAGER_CELEBRATE)
			|| sound.equals(SoundEvents.VILLAGER_DEATH)
			|| sound.equals(SoundEvents.VILLAGER_HURT)
			|| sound.equals(SoundEvents.VILLAGER_NO)
			|| sound.equals(SoundEvents.VILLAGER_TRADE)
			|| sound.equals(SoundEvents.VILLAGER_YES);
	}

	private static boolean isWanderingTraderVoice(SoundEvent sound) {
		return sound.equals(SoundEvents.WANDERING_TRADER_AMBIENT)
			|| sound.equals(SoundEvents.WANDERING_TRADER_DEATH)
			|| sound.equals(SoundEvents.WANDERING_TRADER_HURT)
			|| sound.equals(SoundEvents.WANDERING_TRADER_NO)
			|| sound.equals(SoundEvents.WANDERING_TRADER_TRADE)
			|| sound.equals(SoundEvents.WANDERING_TRADER_YES);
	}

	private static boolean isSheepVoice(SoundEvent sound) {
		return sound.equals(SoundEvents.SHEEP_AMBIENT)
			|| sound.equals(SoundEvents.SHEEP_DEATH)
			|| sound.equals(SoundEvents.SHEEP_HURT);
	}

	private static boolean isWooly(Sheep sheep) {
		String name = sheep.getName().getString();
		return name.equalsIgnoreCase("Wooly") || name.equalsIgnoreCase("Wooly The Sheep");
	}
}
