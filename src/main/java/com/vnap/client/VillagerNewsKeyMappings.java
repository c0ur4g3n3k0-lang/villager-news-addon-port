package com.vnap.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.vnap.VillagerNewsAddonPort;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Remappable client controls owned by Villager News. */
public final class VillagerNewsKeyMappings {
	private static KeyMapping openSettings;

	private VillagerNewsKeyMappings() {
	}

	public static void register() {
		KeyMapping.Category category = KeyMapping.Category.register(VillagerNewsAddonPort.id("general"));
		openSettings = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.villager-news-addon-port.open_settings",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_N,
			category
		));
	}

	public static void tick(Minecraft client) {
		while (openSettings != null && openSettings.consumeClick()) {
			if (client.level != null && client.player != null && client.gui.screen() == null) {
				client.setScreenAndShow(HandbookScreen.settingsScreen(null));
			}
		}
	}
}
