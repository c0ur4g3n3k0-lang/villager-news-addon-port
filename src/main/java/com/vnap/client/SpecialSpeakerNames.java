package com.vnap.client;

import java.util.Locale;

/** Translation keys for subtitle labels only; entity names remain untouched for profile detection. */
final class SpecialSpeakerNames {
	private static final String PREFIX = "speaker.villager-news-addon-port.";

	private SpecialSpeakerNames() {
	}

	static String villagerKey(String customName) {
		return switch (customName.toLowerCase(Locale.ROOT)) {
			case "mayor", "the mayor", "mayor villager" -> PREFIX + "mayor";
			case "testificate man" -> PREFIX + "testificate_man";
			case "villager #5", "villager number 5" -> PREFIX + "number_5";
			case "villager #9", "villager number 9" -> PREFIX + "number_9";
			case "villager unreachable", "can't catch me!" -> PREFIX + "unreachable";
			default -> null;
		};
	}

	static String sheepKey(String customName) {
		return switch (customName.toLowerCase(Locale.ROOT)) {
			case "wooly", "wooly the sheep" -> PREFIX + "wooly";
			default -> null;
		};
	}
}
