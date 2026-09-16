package com.vnap.client;

import com.vnap.VillagerNewsAddonPort;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Presentation layer for Villager News subtitles. */
public final class VillagerNewsSubtitleHud {
	private static final int MAX_CARDS = 4;
	private static final float MAX_WIDTH_RATIO = 0.60F;
	private static final int SIDE_MARGIN = 8;
	private static final int BOTTOM_MARGIN = 52;
	private static final int TOP_MARGIN = 8;
	private static final int HORIZONTAL_PADDING = 6;
	private static final int VERTICAL_PADDING = 4;
	private static final int SPEAKER_TEXT_GAP = 2;
	private static final int CARD_GAP = 3;
	private static final int BACKGROUND_COLOR = 0xA0000000;
	private static final int SPEAKER_COLOR = 0xFFFFD54F;
	private static final int TRANSCRIPT_COLOR = 0xFFFFFFFF;
	private static final long FADE_IN_NANOS = 100_000_000L;
	private static final long FADE_OUT_NANOS = 150_000_000L;

	private VillagerNewsSubtitleHud() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(
			VanillaHudElements.OVERLAY_MESSAGE,
			VillagerNewsAddonPort.id("dialogue_subtitles"),
			VillagerNewsSubtitleHud::render
		);
	}

	private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null || !VillagerNewsClientSettings.subtitlesEnabled()) return;
		long now = System.nanoTime();
		List<DialogueSubtitleState.VisibleSubtitle> visible = DialogueSubtitleState.visible(minecraft, now);
		int bottomY = graphics.guiHeight() - BOTTOM_MARGIN;
		int drawnCards = 0;
		for (DialogueSubtitleState.VisibleSubtitle subtitle : visible) {
			if (drawnCards >= MAX_CARDS) break;
			CardLayout layout = layout(minecraft.font, subtitle.speakerName(), subtitle.transcript(), graphics.guiWidth());
			if (layout == null) break;
			if (drawnCards == 0) bottomY = firstCardBottom(graphics.guiHeight(), layout.height());
			int left = (graphics.guiWidth() - layout.width()) / 2;
			int top = bottomY - layout.height();
			if (top < TOP_MARGIN) break;
			drawCard(graphics, minecraft.font, layout, left, top, frameOpacity(
				subtitle.frameStartNanos(),
				subtitle.frameEndNanos(),
				now
			));
			bottomY = top - CARD_GAP;
			drawnCards++;
		}
	}

	private static CardLayout layout(Font font, Component speaker, Component transcript, int guiWidth) {
		int maximumWidth = maximumCardWidth(guiWidth);
		int textWidth = maximumWidth - HORIZONTAL_PADDING * 2;
		if (textWidth <= 0) return null;
		List<FormattedCharSequence> speakerLines = font.split(speaker, textWidth);
		List<FormattedCharSequence> transcriptLines = font.split(transcript, textWidth);
		int contentWidth = Math.max(lineWidth(font, speakerLines), lineWidth(font, transcriptLines));
		int width = Math.min(maximumWidth, contentWidth + HORIZONTAL_PADDING * 2);
		int height = cardHeight(font.lineHeight, speakerLines.size(), transcriptLines.size());
		return new CardLayout(speakerLines, transcriptLines, width, height);
	}

	private static int lineWidth(Font font, List<FormattedCharSequence> lines) {
		return lines.stream().mapToInt(font::width).max().orElse(0);
	}

	static int maximumCardWidth(int guiWidth) {
		return Math.max(0, Math.min((int) Math.floor(guiWidth * MAX_WIDTH_RATIO), guiWidth - SIDE_MARGIN * 2));
	}

	static int cardHeight(int lineHeight, int speakerLines, int transcriptLines) {
		return VERTICAL_PADDING * 2 + lineHeight * (speakerLines + transcriptLines) + SPEAKER_TEXT_GAP;
	}

	static int firstCardBottom(int guiHeight, int cardHeight) {
		return Math.min(guiHeight - TOP_MARGIN, Math.max(guiHeight - BOTTOM_MARGIN, TOP_MARGIN + cardHeight));
	}

	private static void drawCard(
		GuiGraphicsExtractor graphics,
		Font font,
		CardLayout layout,
		int left,
		int top,
		float opacity
	) {
		int centerX = left + layout.width() / 2;
		int lineY = top + VERTICAL_PADDING;
		graphics.fill(left, top, left + layout.width(), top + layout.height(), applyOpacity(BACKGROUND_COLOR, opacity));
		for (FormattedCharSequence line : layout.speakerLines()) {
			graphics.text(font, line, centerX - font.width(line) / 2, lineY, applyOpacity(SPEAKER_COLOR, opacity), true);
			lineY += font.lineHeight;
		}
		lineY += SPEAKER_TEXT_GAP;
		for (FormattedCharSequence line : layout.transcriptLines()) {
			graphics.text(font, line, centerX - font.width(line) / 2, lineY, applyOpacity(TRANSCRIPT_COLOR, opacity), true);
			lineY += font.lineHeight;
		}
	}

	private record CardLayout(
		List<FormattedCharSequence> speakerLines,
		List<FormattedCharSequence> transcriptLines,
		int width,
		int height
	) {
	}

	static float frameOpacity(long frameStartNanos, long frameEndNanos, long now) {
		long duration = frameEndNanos - frameStartNanos;
		if (duration <= 0L || now < frameStartNanos || now >= frameEndNanos) return 0.0F;
		long fadeIn = Math.min(FADE_IN_NANOS, duration / 3L);
		long fadeOut = Math.min(FADE_OUT_NANOS, duration / 3L);
		float fadeInOpacity = fadeIn == 0L ? 1.0F : clamp01((float) (now - frameStartNanos) / fadeIn);
		float fadeOutOpacity = fadeOut == 0L ? 1.0F : clamp01((float) (frameEndNanos - now) / fadeOut);
		return Math.min(fadeInOpacity, fadeOutOpacity);
	}

	static int applyOpacity(int color, float opacity) {
		int baseAlpha = color >>> 24;
		int alpha = Math.round(baseAlpha * clamp01(opacity));
		return color & 0x00FFFFFF | alpha << 24;
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}
}
