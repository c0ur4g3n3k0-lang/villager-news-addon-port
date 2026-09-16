package com.vnap.entity;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

/** A deep copy of the villager's trading state before a name-based special profile takes over. */
public record VillagerTradeBackup(VillagerData data, MerchantOffers offers, int experience) {
	private static final String KEY = "VillagerNewsOriginalTrade";

	public VillagerTradeBackup {
		offers = offers.copy();
	}

	public static VillagerTradeBackup capture(Villager villager, boolean legacySpecialOnlyOffers) {
		MerchantOffers originalOffers = villager.getOffers().copy();
		if (legacySpecialOnlyOffers) originalOffers.clear();
		return new VillagerTradeBackup(villager.getVillagerData(), originalOffers,
			legacySpecialOnlyOffers ? 0 : villager.getVillagerXp());
	}

	public void restore(Villager villager) {
		villager.setVillagerData(data);
		villager.setOffers(offers.copy());
		villager.setVillagerXp(experience);
	}

	public void save(ValueOutput output) {
		ValueOutput backup = output.child(KEY);
		backup.store("Data", VillagerData.CODEC, data);
		backup.store("Offers", MerchantOffers.CODEC, offers);
		backup.putInt("OfferCount", offers.size());
		backup.putInt("Xp", experience);
	}

	public static Optional<VillagerTradeBackup> load(ValueInput input) {
		return input.child(KEY).flatMap(backup -> backup.read("Data", VillagerData.CODEC)
			.flatMap(data -> backup.getInt("Xp").flatMap(experience -> {
				int count = backup.getIntOr("OfferCount", -1);
				if (count < 0) return Optional.empty();
				Optional<MerchantOffers> offers = count == 0
					? Optional.of(new MerchantOffers()) : backup.read("Offers", MerchantOffers.CODEC);
				return offers.filter(value -> value.size() == count)
					.map(value -> new VillagerTradeBackup(data, value, experience));
			})));
	}
}
