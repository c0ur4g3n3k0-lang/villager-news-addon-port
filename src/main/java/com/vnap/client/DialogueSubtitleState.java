package com.vnap.client;

import com.vnap.dialogue.DialogueCatalog;
import com.vnap.network.DialogueAnimationPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DialogueSubtitleState {
	private static final double RANGE = 16.0;
	private static final double RANGE_SQUARED = RANGE * RANGE;
	private static final Map<UUID, ActiveSubtitle> ACTIVE = new HashMap<>();

	private DialogueSubtitleState() {
	}

	public static void start(DialogueAnimationPayload payload) {
		if (payload.groupId().isEmpty() || payload.durationTicks() <= 0) {
			ACTIVE.remove(payload.entityId());
			return;
		}
		DialogueCatalog.DialogueGroup group = DialogueCatalog.byId(payload.groupId());
		if (group == null) return;
		DialogueCatalog.DialogueVariant variant = group.variants().stream()
			.filter(candidate -> candidate.index() == payload.variantIndex()).findFirst().orElse(null);
		if (variant == null || variant.subtitles().isEmpty()) return;
		Minecraft minecraft = Minecraft.getInstance();
		Entity speaker = minecraft.level == null ? null : minecraft.level.getEntity(payload.entityId());
		long startNanos = System.nanoTime();
		ACTIVE.put(payload.entityId(), new ActiveSubtitle(
			startNanos,
			startNanos + payload.durationTicks() * 50_000_000L,
			variant.subtitles(),
			isDeathDialogue(payload.groupId()),
			speaker == null ? null : speaker.position(),
			speaker == null ? null : speakerName(speaker).copy()
		));
	}

	public static void tick(Minecraft minecraft) {
		if (minecraft.level == null || minecraft.player == null) {
			clear();
			return;
		}
		long now = System.nanoTime();
		Iterator<Map.Entry<UUID, ActiveSubtitle>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ActiveSubtitle> entry = iterator.next();
			ActiveSubtitle active = entry.getValue();
			Entity entity = minecraft.level.getEntity(entry.getKey());
			if (now >= active.endNanos() || (!active.persistsAfterDeath() && entity != null && !entity.isAlive())) {
				iterator.remove();
			}
		}
	}

	public static void clear() {
		ACTIVE.clear();
	}

	public static List<VisibleSubtitle> visible(Minecraft minecraft, long now) {
		if (minecraft.level == null || minecraft.player == null) return List.of();
		List<VisibleSubtitle> visible = new ArrayList<>();
		for (Map.Entry<UUID, ActiveSubtitle> entry : ACTIVE.entrySet()) {
			ActiveSubtitle active = entry.getValue();
			if (now >= active.endNanos()) continue;
			Entity entity = minecraft.level.getEntity(entry.getKey());
			boolean speakerPresent = entity != null;
			boolean speakerAlive = speakerPresent && entity.isAlive();
			boolean useSnapshot = usesSpeakerSnapshot(active.persistsAfterDeath(), speakerPresent, speakerAlive);
			if (!useSnapshot && !speakerAlive) continue;
			Vec3 position = useSnapshot ? active.position() : entity.position();
			Component name = useSnapshot ? active.speakerName() : speakerName(entity);
			if (position == null || name == null) continue;
			double distanceSquared = minecraft.player.distanceToSqr(position);
			if (distanceSquared > RANGE_SQUARED) continue;
			int frame = active.frame(now);
			if (frame < 0) continue;
			Component transcript = Component.translatable(active.subtitles().get(frame).key());
			visible.add(new VisibleSubtitle(
				distanceSquared,
				name.copy(),
				transcript,
				active.frameStartNanos(frame),
				active.frameEndNanos(frame)
			));
		}
		visible.sort(Comparator.comparingDouble(VisibleSubtitle::distanceSquared));
		return List.copyOf(visible);
	}

	static boolean isDeathDialogue(String groupId) {
		return groupId.equals("hivgme") || groupId.equals("ecslqo");
	}

	static boolean usesSpeakerSnapshot(boolean persistsAfterDeath, boolean speakerPresent, boolean speakerAlive) {
		return persistsAfterDeath && (!speakerPresent || !speakerAlive);
	}

	static int frameAt(List<DialogueCatalog.SubtitleFrame> subtitles, long startNanos, long now) {
		double elapsed = (now - startNanos) / 1_000_000_000.0;
		int frame = -1;
		for (int index = 0; index < subtitles.size(); index++) {
			if (subtitles.get(index).time() > elapsed) break;
			frame = index;
		}
		return frame;
	}

	static long frameStartNanos(List<DialogueCatalog.SubtitleFrame> subtitles, long startNanos, int frame) {
		return startNanos + Math.round(subtitles.get(frame).time() * 1_000_000_000.0);
	}

	static long frameEndNanos(
		List<DialogueCatalog.SubtitleFrame> subtitles,
		long startNanos,
		long endNanos,
		int frame
	) {
		return frame + 1 < subtitles.size()
			? frameStartNanos(subtitles, startNanos, frame + 1)
			: endNanos;
	}

	private static Component speakerName(Entity entity) {
		Component customName = entity.getCustomName();
		if (entity instanceof Villager villager) {
			if (customName == null) return villager.getVillagerData().profession().value().name();
			String key = SpecialSpeakerNames.villagerKey(customName.getString());
			if (key != null) return Component.translatable(key);
		}
		if (entity instanceof Sheep && customName != null) {
			String key = SpecialSpeakerNames.sheepKey(customName.getString());
			if (key != null) return Component.translatable(key);
		}
		return entity.getName();
	}

	public record VisibleSubtitle(
		double distanceSquared,
		Component speakerName,
		Component transcript,
		long frameStartNanos,
		long frameEndNanos
	) {
	}

	private record ActiveSubtitle(
		long startNanos,
		long endNanos,
		List<DialogueCatalog.SubtitleFrame> subtitles,
		boolean persistsAfterDeath,
		Vec3 position,
		Component speakerName
	) {
		int frame(long now) {
			return frameAt(subtitles, startNanos, now);
		}

		long frameStartNanos(int frame) {
			return DialogueSubtitleState.frameStartNanos(subtitles, startNanos, frame);
		}

		long frameEndNanos(int frame) {
			return DialogueSubtitleState.frameEndNanos(subtitles, startNanos, endNanos, frame);
		}
	}
}
