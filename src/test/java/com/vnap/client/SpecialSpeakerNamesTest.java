package com.vnap.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpecialSpeakerNamesTest {
	@Test
	void villagerAliasesUseTheSamePresentationKeysAsTheirProfiles() {
		assertEquals("speaker.villager-news-addon-port.mayor", SpecialSpeakerNames.villagerKey("Mayor Villager"));
		assertEquals("speaker.villager-news-addon-port.mayor", SpecialSpeakerNames.villagerKey("The Mayor"));
		assertEquals("speaker.villager-news-addon-port.mayor", SpecialSpeakerNames.villagerKey("Mayor"));
		assertEquals("speaker.villager-news-addon-port.testificate_man", SpecialSpeakerNames.villagerKey("Testificate Man"));
		assertEquals("speaker.villager-news-addon-port.number_5", SpecialSpeakerNames.villagerKey("Villager #5"));
		assertEquals("speaker.villager-news-addon-port.number_5", SpecialSpeakerNames.villagerKey("Villager Number 5"));
		assertEquals("speaker.villager-news-addon-port.number_9", SpecialSpeakerNames.villagerKey("Villager #9"));
		assertEquals("speaker.villager-news-addon-port.number_9", SpecialSpeakerNames.villagerKey("Villager Number 9"));
		assertEquals("speaker.villager-news-addon-port.unreachable", SpecialSpeakerNames.villagerKey("Villager Unreachable"));
		assertEquals("speaker.villager-news-addon-port.unreachable", SpecialSpeakerNames.villagerKey("Can't Catch Me!"));
		assertNull(SpecialSpeakerNames.villagerKey("Bob"));
		assertNull(SpecialSpeakerNames.villagerKey("Wooly The Sheep"));
	}

	@Test
	void woolyIsLocalizedOnlyWhenTheSpeakerIsASheep() {
		assertEquals("speaker.villager-news-addon-port.wooly", SpecialSpeakerNames.sheepKey("Wooly"));
		assertEquals("speaker.villager-news-addon-port.wooly", SpecialSpeakerNames.sheepKey("Wooly The Sheep"));
		assertNull(SpecialSpeakerNames.sheepKey("Bob"));
		assertNull(SpecialSpeakerNames.sheepKey("Mayor Villager"));
	}
}
