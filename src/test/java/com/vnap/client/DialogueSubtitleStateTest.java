package com.vnap.client;

import com.vnap.dialogue.DialogueCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueSubtitleStateTest {
	private static final List<DialogueCatalog.SubtitleFrame> FRAMES = List.of(
		new DialogueCatalog.SubtitleFrame(0.25, "subtitle.first"),
		new DialogueCatalog.SubtitleFrame(0.75, "subtitle.second"),
		new DialogueCatalog.SubtitleFrame(1.50, "subtitle.third")
	);

	@Test
	void frameSelectionPreservesExactSubtitleBoundaries() {
		long start = 10_000_000_000L;
		assertEquals(-1, DialogueSubtitleState.frameAt(FRAMES, start, start + 249_999_999L));
		assertEquals(0, DialogueSubtitleState.frameAt(FRAMES, start, start + 250_000_000L));
		assertEquals(0, DialogueSubtitleState.frameAt(FRAMES, start, start + 749_999_999L));
		assertEquals(1, DialogueSubtitleState.frameAt(FRAMES, start, start + 750_000_000L));
		assertEquals(2, DialogueSubtitleState.frameAt(FRAMES, start, start + 1_500_000_000L));
	}

	@Test
	void frameLifetimeUsesTheNextTimestampAndOriginalDialogueEnd() {
		long start = 10_000_000_000L;
		long end = start + 2_000_000_000L;
		assertEquals(start + 250_000_000L, DialogueSubtitleState.frameStartNanos(FRAMES, start, 0));
		assertEquals(start + 750_000_000L, DialogueSubtitleState.frameEndNanos(FRAMES, start, end, 0));
		assertEquals(start + 1_500_000_000L, DialogueSubtitleState.frameEndNanos(FRAMES, start, end, 1));
		assertEquals(end, DialogueSubtitleState.frameEndNanos(FRAMES, start, end, 2));
	}

	@Test
	void onlyDeathDialoguesSwitchToAStoredSpeakerSnapshot() {
		assertTrue(DialogueSubtitleState.isDeathDialogue("hivgme"));
		assertTrue(DialogueSubtitleState.isDeathDialogue("ecslqo"));
		assertFalse(DialogueSubtitleState.isDeathDialogue("wyvzhk"));
		assertFalse(DialogueSubtitleState.usesSpeakerSnapshot(false, false, false));
		assertFalse(DialogueSubtitleState.usesSpeakerSnapshot(true, true, true));
		assertTrue(DialogueSubtitleState.usesSpeakerSnapshot(true, true, false));
		assertTrue(DialogueSubtitleState.usesSpeakerSnapshot(true, false, false));
	}
}
