package com.vnap.client;

import com.vnap.VillagerNewsAddonPort;
import com.vnap.network.VillagerNewsSettingsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ServerCompatibilityState {
	private static Support support = Support.UNKNOWN;

	private ServerCompatibilityState() {
	}

	public static void detectServerSupport() {
		setSupport(ClientPlayNetworking.canSend(VillagerNewsSettingsPayload.TYPE)
			? Support.PRESENT : Support.ABSENT);
	}

	public static void markServerModPresent() {
		setSupport(Support.PRESENT);
	}

	public static boolean serverModPresent() {
		return support == Support.PRESENT;
	}

	public static boolean clientOnlyFallback() {
		return support == Support.ABSENT;
	}

	public static void reset() {
		support = Support.UNKNOWN;
	}

	private static void setSupport(Support next) {
		if (support == next) return;
		support = next;
		if (next == Support.PRESENT) {
			VillagerNewsAddonPort.LOGGER.info("Detected Villager News on the server; using synchronized dialogue");
		} else {
			VillagerNewsSettingsState.activateClientOnly();
			VillagerNewsAddonPort.LOGGER.info("Villager News is not installed on the server; using client-only dialogue and suppressing vanilla character voices locally");
		}
	}

	private enum Support {
		UNKNOWN,
		PRESENT,
		ABSENT
	}
}
