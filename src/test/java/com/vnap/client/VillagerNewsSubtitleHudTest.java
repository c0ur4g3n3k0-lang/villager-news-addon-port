package com.vnap.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VillagerNewsSubtitleHudTest {
	@Test
	void opacityUsesShortPresentationOnlyTransitionsWithinTheFrameLifetime() {
		long second = 1_000_000_000L;
		assertEquals(0.0F, VillagerNewsSubtitleHud.frameOpacity(0L, second, 0L));
		assertEquals(0.5F, VillagerNewsSubtitleHud.frameOpacity(0L, second, 50_000_000L));
		assertEquals(1.0F, VillagerNewsSubtitleHud.frameOpacity(0L, second, 100_000_000L));
		assertEquals(1.0F, VillagerNewsSubtitleHud.frameOpacity(0L, second, 800_000_000L));
		assertEquals(0.5F, VillagerNewsSubtitleHud.frameOpacity(0L, second, 925_000_000L));
		assertEquals(0.0F, VillagerNewsSubtitleHud.frameOpacity(0L, second, second));
	}

	@Test
	void shortFramesStillReachFullOpacity() {
		long duration = 60_000_000L;
		assertEquals(1.0F, VillagerNewsSubtitleHud.frameOpacity(0L, duration, duration / 2L));
	}

	@Test
	void opacityPreservesEachColorsConfiguredMaximumAlpha() {
		assertEquals(0x50000000, VillagerNewsSubtitleHud.applyOpacity(0xA0000000, 0.5F));
		assertEquals(0x80FFD54F, VillagerNewsSubtitleHud.applyOpacity(0xFFFFD54F, 0.5F));
		assertEquals(0xFFFFFFFF, VillagerNewsSubtitleHud.applyOpacity(0xFFFFFFFF, 1.0F));
	}

	@Test
	void cardsStayWithinSixtyPercentOfTheGuiAndRetainSideMargins() {
		assertEquals(192, VillagerNewsSubtitleHud.maximumCardWidth(320));
		assertEquals(60, VillagerNewsSubtitleHud.maximumCardWidth(100));
		assertEquals(0, VillagerNewsSubtitleHud.maximumCardWidth(16));
	}

	@Test
	void cardHeightGrowsWithEveryWrappedSpeakerAndTranscriptLine() {
		assertEquals(28, VillagerNewsSubtitleHud.cardHeight(9, 1, 1));
		assertEquals(46, VillagerNewsSubtitleHud.cardHeight(9, 1, 3));
		assertEquals(55, VillagerNewsSubtitleHud.cardHeight(9, 2, 3));
	}

	@Test
	void smallGuiCanMoveTheFirstCardBelowThePreferredHotbarMargin() {
		assertEquals(188, VillagerNewsSubtitleHud.firstCardBottom(240, 46));
		assertEquals(54, VillagerNewsSubtitleHud.firstCardBottom(80, 46));
	}
}
