package com.vnap.client;

import com.vnap.VillagerNewsAddonPort;
import com.vnap.config.VillagerNewsSettings;
import com.vnap.dialogue.DialogueCatalog;
import com.vnap.network.DialogueAnimationPayload;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Runs the locally observable part of contextual dialogue when the remote server does not expose
 * the Villager News networking channel. It never sends packets or mutates server-owned entities.
 */
public final class ClientOnlyDialogueController {
	private static final double OBSERVER_RANGE = 16.0;
	private static final long SHORT_COOLDOWN = 20L * 45L;
	private static final long LONG_COOLDOWN = 20L * 150L;
	private static final Set<String> BABY_DIALOGUES = Set.of(
		"abfwiv", "aezdiy", "ahcvzd", "cmrqhw", "durjjd", "ecslqo", "fzyrfm", "ggitzq",
		"gotjxf", "gzsztp", "hbalps", "hcdvqm", "jfuftm", "lgjtnf", "mqnapy", "msemoe",
		"nxalcz", "qrdzmt", "rfnirh", "saxuwk", "svdjdk", "vbclem", "vhwksn", "wkwcrf",
		"wsxfok", "wtuguc", "zeykfp", "cxeziv", "riezum", "rlkdqd"
	);
	private static final int CONDITION_POISON = 1;
	private static final int CONDITION_SLOWNESS = 1 << 1;
	private static final int CONDITION_WEAKNESS = 1 << 2;
	private static final int CONDITION_LAVA = 1 << 3;
	private static final int CONDITION_FIRE = 1 << 4;
	private static final int CONDITION_FROZEN = 1 << 5;
	private static final int CONDITION_SUFFOCATING = 1 << 6;
	private static final int[] CONDITION_BITS = {
		CONDITION_POISON, CONDITION_SLOWNESS, CONDITION_WEAKNESS, CONDITION_LAVA,
		CONDITION_FIRE, CONDITION_FROZEN, CONDITION_SUFFOCATING
	};
	private static final String[] CONDITION_DIALOGUES = {
		"onindz", "xemyaj", "yebifs", "elryje", "etkxko", "igebly", "vnaodx"
	};
	private static final Map<String, Long> COOLDOWNS = new HashMap<>();
	private static final Map<UUID, Long> BUSY_UNTIL = new HashMap<>();
	private static final Map<UUID, ActiveDialogue> ACTIVE_DIALOGUES = new HashMap<>();
	private static final Map<String, List<Integer>> RECENT_VARIANTS = new HashMap<>();
	private static final Map<UUID, EntitySnapshot> ENTITY_SNAPSHOTS = new HashMap<>();
	private static final Map<UUID, VillagerSnapshot> VILLAGER_SNAPSHOTS = new HashMap<>();
	private static final Map<UUID, Integer> CONDITION_HISTORY = new HashMap<>();
	private static final Map<UUID, Integer> CONDITION_CURSORS = new HashMap<>();
	private static final Map<UUID, String> PENDING_CONDITION_RELIEF = new HashMap<>();
	private static final Map<UUID, PendingAttack> PENDING_ATTACKS = new HashMap<>();
	private static final Map<UUID, PendingWake> PENDING_WAKES = new HashMap<>();
	private static final Map<BlockPos, PendingPlacement> PENDING_PLACEMENTS = new HashMap<>();
	private static final Map<BlockPos, PendingBreak> PENDING_BREAKS = new HashMap<>();
	private static final List<PendingBell> PENDING_BELLS = new ArrayList<>();
	private static final List<PendingBellReaction> PENDING_BELL_REACTIONS = new ArrayList<>();
	private static final Set<UUID> ENCOUNTERS = new HashSet<>();
	private static ClientLevel activeLevel;
	private static Vec3 lastPlayerPosition;
	private static BlockPos lastGroundPosition;
	private static String lastGroundBlock = "";
	private static String lastPlayerContext;
	private static long lastWorldTime = Long.MIN_VALUE;
	private static Difficulty lastDifficulty;
	private static UUID pendingTrader;
	private static long pendingTraderUntil;
	private static TradeSession tradeSession;
	private static long lastBreakTick = Long.MIN_VALUE / 2L;
	private static int breakStreak;
	private static int stillTicks;
	private static int stareTicks;
	private static int playerDeaths;
	private static long ticks;
	private static boolean running;

	private ClientOnlyDialogueController() {
	}

	public static void register() {
		ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> {
			if (!accepts(player, level)) return;
			PENDING_BREAKS.put(pos.immutable(), new PendingBreak(state, ticks + 10L, ticks + 40L));
		});

		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (player.isSpectator()) return InteractionResult.PASS;
			if (!accepts(player, level)) return InteractionResult.PASS;
			ItemStack held = player.getItemInHand(hand);
			BlockState clicked = level.getBlockState(hitResult.getBlockPos());
			String clickedPath = blockPath(clicked);
			boolean itemOnlyInteraction = player.isSecondaryUseActive() && !held.isEmpty();
			if (!itemOnlyInteraction && clickedPath.equals("bell") && level instanceof ClientLevel clientLevel) {
				queueBell(clientLevel, hitResult.getLocation());
				return InteractionResult.PASS;
			}
			String title = selectHeldBlockContext(held, clicked);
			if (title == null && !itemOnlyInteraction) title = selectUseBlockContext(clicked);
			if (!itemOnlyInteraction && clickedPath.endsWith("_door") && clicked.hasProperty(BlockStateProperties.OPEN)
					&& clicked.getValue(BlockStateProperties.OPEN)
					&& !nearbyVillagers(level, hitResult.getLocation(), 3.0).isEmpty()) {
				title = "Close a Door in a Villager's Face";
			}
			if (title != null) playObserved(level, player, hitResult.getLocation(), title, SHORT_COOLDOWN);
			return InteractionResult.PASS;
		});

		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (accepts(player, level)) {
				String title = selectUseItemContext(player.getItemInHand(hand));
				if (title != null) playObserved(level, player, player.position(), title, SHORT_COOLDOWN);
			}
			return InteractionResult.PASS;
		});

		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (accepts(player, level)) onUseEntity(player, entity, hand);
			return InteractionResult.PASS;
		});

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (accepts(player, level) && entity instanceof LivingEntity) {
				if (entity instanceof Villager villager && villager.isSleeping()) {
					PENDING_WAKES.put(villager.getUUID(), new PendingWake(player.getUUID(), ticks + 40L));
				}
				PENDING_ATTACKS.put(entity.getUUID(), new PendingAttack(itemPath(player.getItemInHand(hand)), ticks + 20L));
			}
			return InteractionResult.PASS;
		});
	}

	public static void onBlockPlacementPredicted(Player player, BlockPos position, BlockState state) {
		if (!accepts(player, player.level())) return;
		PENDING_PLACEMENTS.put(position.immutable(), new PendingPlacement(state.getBlock(), ticks + 10L, ticks + 40L));
	}

	public static void tick(Minecraft minecraft) {
		if (!ServerCompatibilityState.clientOnlyFallback() || minecraft.level == null || minecraft.player == null
				|| !VillagerNewsSettings.dialogueEnabled()) {
			if (running) clear(minecraft);
			return;
		}
		if (activeLevel != minecraft.level) {
			clear(minecraft);
			activeLevel = minecraft.level;
		}
		if (!running) {
			running = true;
			VillagerNewsAddonPort.LOGGER.info("Client-only contextual dialogue engine enabled for this server");
		}
		ticks++;
		ACTIVE_DIALOGUES.entrySet().removeIf(entry -> entry.getValue().endTick() <= ticks);
		BUSY_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= ticks);
		processPendingPlacements(minecraft.level, minecraft.player);
		processPendingBreaks(minecraft.level, minecraft.player);
		processPendingBells();
		if (ticks % 2L == 0L) processEntityChanges(minecraft.level, minecraft.player);
		processTrade(minecraft);
		if (ticks % 10L == 0L) processPlayer(minecraft.level, minecraft.player);
		if (ticks % 20L == 0L) processVillagerStates(minecraft.level, minecraft.player);
		if (ticks % 1200L == 0L) {
			COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() + 20L * 600L < ticks);
			PENDING_ATTACKS.entrySet().removeIf(entry -> entry.getValue().expiresAt() < ticks);
		}
	}

	public static void clear(Minecraft minecraft) {
		for (UUID id : List.copyOf(ACTIVE_DIALOGUES.keySet())) stopDialogue(id);
		COOLDOWNS.clear();
		BUSY_UNTIL.clear();
		ACTIVE_DIALOGUES.clear();
		RECENT_VARIANTS.clear();
		ENTITY_SNAPSHOTS.clear();
		VILLAGER_SNAPSHOTS.clear();
		CONDITION_HISTORY.clear();
		CONDITION_CURSORS.clear();
		PENDING_CONDITION_RELIEF.clear();
		PENDING_ATTACKS.clear();
		PENDING_WAKES.clear();
		PENDING_PLACEMENTS.clear();
		PENDING_BREAKS.clear();
		PENDING_BELLS.clear();
		PENDING_BELL_REACTIONS.clear();
		ENCOUNTERS.clear();
		activeLevel = null;
		lastPlayerPosition = null;
		lastGroundPosition = null;
		lastGroundBlock = "";
		lastPlayerContext = null;
		lastWorldTime = Long.MIN_VALUE;
		lastDifficulty = null;
		pendingTrader = null;
		tradeSession = null;
		lastBreakTick = Long.MIN_VALUE / 2L;
		breakStreak = 0;
		stillTicks = 0;
		stareTicks = 0;
		playerDeaths = 0;
		ticks = 0L;
		running = false;
	}

	private static boolean accepts(Player player, Level level) {
		Minecraft minecraft = Minecraft.getInstance();
		return ServerCompatibilityState.clientOnlyFallback() && VillagerNewsSettings.dialogueEnabled()
			&& level.isClientSide() && minecraft.player == player;
	}

	private static void processPendingPlacements(ClientLevel level, Player player) {
		PENDING_PLACEMENTS.entrySet().removeIf(entry -> {
			PendingPlacement pending = entry.getValue();
			if (ticks > pending.expiresAt()) return true;
			if (ticks < pending.confirmAt()) return false;
			BlockPos position = entry.getKey();
			if (level.getBlockState(position).getBlock() != pending.block()) return true;
			String path = blockPath(level.getBlockState(position));
			String title = selectPlaceContext(pending.block(), level, position);
			if ((path.equals("pumpkin") || path.equals("carved_pumpkin")) && nearBlock(level, position, "iron_block", 3)) {
				title = "Build an Iron Golem Frame";
			}
			playObserved(level, player, Vec3.atCenterOf(position), title, SHORT_COOLDOWN);
			return true;
		});
	}

	private static void processPendingBreaks(ClientLevel level, Player player) {
		PENDING_BREAKS.entrySet().removeIf(entry -> {
			PendingBreak pending = entry.getValue();
			if (ticks > pending.expiresAt()) return true;
			if (ticks < pending.confirmAt()) return false;
			BlockPos position = entry.getKey();
			if (level.getBlockState(position).getBlock() == pending.state().getBlock()) return false;
			breakStreak = ticks - lastBreakTick <= 30L ? breakStreak + 1 : 1;
			lastBreakTick = ticks;
			String title = selectBreakContext(pending.state(), breakStreak >= 4);
			if (title.equals("Harvest Crops") && nearbyVillagers(level, Vec3.atCenterOf(position), OBSERVER_RANGE).stream()
					.anyMatch(villager -> profession(villager).equals("farmer"))) {
				title = "Harvest Crops Near a Farmer";
			}
			playObserved(level, player, Vec3.atCenterOf(position), title, SHORT_COOLDOWN);
			return true;
		});
	}

	private static void queueBell(ClientLevel level, Vec3 position) {
		boolean queued = PENDING_BELLS.stream().anyMatch(pending -> pending.level == level
			&& pending.position.distanceToSqr(position) < 0.25 && pending.dueTick > ticks);
		if (!queued) PENDING_BELLS.add(new PendingBell(level, position, ticks + 15L));
	}

	private static void processPendingBells() {
		PENDING_BELLS.removeIf(pending -> {
			if (pending.dueTick > ticks) return false;
			for (Villager villager : nearbyVillagers(pending.level, pending.position, 50.0)) {
				if (villager.isSleeping() || cast(villager) != CastProfile.VILLAGER) continue;
				long dueTick = ticks + ThreadLocalRandom.current().nextInt(5);
				PENDING_BELL_REACTIONS.add(new PendingBellReaction(pending.level, villager.getUUID(), pending.position,
					dueTick, dueTick + 10L));
			}
			return true;
		});
		PENDING_BELL_REACTIONS.removeIf(pending -> {
			if (pending.dueTick > ticks) return false;
			Entity entity = pending.level.getEntity(pending.villagerId);
			if (!(entity instanceof Villager villager) || !villager.isAlive() || villager.isSleeping()
					|| cast(villager) != CastProfile.VILLAGER) return true;
			if (isBusy(villager)) return ticks >= pending.expireTick;
			String id = villager.isBaby() ? "nxalcz" : "kljgyu";
			return playId(villager, id, "bell:" + pending.dueTick + ":" + villager.getUUID(), 1L, false)
				|| ticks >= pending.expireTick;
		});
	}

	private static void processEntityChanges(ClientLevel level, Player player) {
		List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class,
			AABB.ofSize(player.position(), 48.0, 32.0, 48.0), entity -> true);
		if (!player.isAlive()) entities = new ArrayList<>(entities);
		if (!entities.contains(player)) entities.add(player);
		Set<UUID> visible = new HashSet<>();
		for (LivingEntity entity : entities) {
			visible.add(entity.getUUID());
			EntitySnapshot current = new EntitySnapshot(entity.hurtTime, entity.deathTime, entity.isAlive());
			EntitySnapshot previous = ENTITY_SNAPSHOTS.put(entity.getUUID(), current);
			if (previous == null) continue;
			if ((entity.deathTime > 0 && previous.deathTime() == 0) || (!current.alive() && previous.alive())) {
				onDeath(level, entity, player);
			} else if (entity.hurtTime > 0 && previous.hurtTime() == 0) {
				onHurt(level, entity, player);
			}
		}
		if (ticks % 100L == 0L) ENTITY_SNAPSHOTS.keySet().retainAll(visible);
	}

	private static void onHurt(ClientLevel level, LivingEntity entity, Player player) {
		PendingAttack attack = PENDING_ATTACKS.remove(entity.getUUID());
		boolean attackedByPlayer = attack != null && attack.expiresAt() >= ticks;
		if (entity instanceof Villager || entity instanceof WanderingTrader || entity instanceof Sheep sheep && isWooly(sheep)) {
			stopDialogue(entity.getUUID());
		}
		if (entity instanceof Sheep sheep && isWooly(sheep)) {
			playId(sheep, attackedByPlayer ? "ncyeaw" : "eyiraw", "hurt:" + sheep.getUUID(), 20L, false);
		} else if (entity instanceof WanderingTrader trader) {
			playId(trader, attackedByPlayer ? "vevdkl" : "wyvzhk", "hurt:" + trader.getUUID(), 20L, true);
		} else if (entity instanceof Villager villager) {
			String id;
			boolean shared = false;
			if (villager.isBaby()) id = attackedByPlayer ? "ahcvzd" : "ecslqo";
			else {
				CastProfile profile = cast(villager);
				if (profile != CastProfile.VILLAGER) id = attackedByPlayer ? profile.attack : profile.hurt;
				else id = attackedByPlayer ? weaponAttackDialogue(attack.itemPath()) : "wyvzhk";
				shared = profile == CastProfile.UNREACHABLE;
			}
			playId(villager, id, "hurt:" + villager.getUUID() + ":" + id, 20L, shared);
		}
		nearbyVillagers(level, entity.position(), OBSERVER_RANGE).stream()
			.filter(witness -> witness != entity && !witness.isBaby() && !witness.isSleeping())
			.min(Comparator.comparingDouble(witness -> witness.distanceToSqr(entity)))
			.ifPresent(witness -> playId(witness, "pkvhpv", "witness_hurt:" + witness.getUUID(), SHORT_COOLDOWN, true));
	}

	private static void onDeath(ClientLevel level, LivingEntity entity, Player player) {
		stopDialogue(entity.getUUID());
		if (entity instanceof Villager villager) {
			playId(villager, villager.isBaby() ? "ecslqo" : "hivgme", "death:" + villager.getUUID(), 1L, !villager.isBaby());
		}
		if (entity instanceof Villager || entity instanceof WanderingTrader || entity instanceof Sheep sheep && isWooly(sheep)) {
			nearbyVillagers(level, entity.position(), OBSERVER_RANGE).stream()
				.filter(witness -> witness != entity && !witness.isBaby() && !witness.isSleeping())
				.min(Comparator.comparingDouble(witness -> witness.distanceToSqr(entity)))
				.ifPresent(witness -> playId(witness, "pmqrpb", "witness_death:" + witness.getUUID(), SHORT_COOLDOWN, true));
		} else if (entity == player) {
			playerDeaths++;
			nearbyVillagers(level, player.position(), OBSERVER_RANGE).stream()
				.filter(witness -> !witness.isBaby() && !witness.isSleeping())
				.min(Comparator.comparingDouble(witness -> witness.distanceToSqr(player)))
				.ifPresent(witness -> playId(witness, playerDeaths > 1 ? "dxcjqn" : "hzjycq",
					"player_death:" + witness.getUUID() + ":" + playerDeaths, SHORT_COOLDOWN, true));
		}
	}

	private static void processPlayer(ClientLevel level, Player player) {
		Vec3 movement = lastPlayerPosition == null ? Vec3.ZERO : player.position().subtract(lastPlayerPosition);
		lastPlayerPosition = player.position();
		if (movement.horizontalDistanceSqr() < 0.0004 && Math.abs(movement.y) < 0.01) stillTicks += 10;
		else stillTicks = 0;
		BlockPos ground = player.blockPosition().below();
		String groundBlock = blockPath(level.getBlockState(ground));
		if (ground.equals(lastGroundPosition) && lastGroundBlock.equals("farmland") && groundBlock.equals("dirt")) {
			playObserved(level, player, player.position(), "Trample Crops", SHORT_COOLDOWN);
		}
		lastGroundPosition = ground;
		lastGroundBlock = groundBlock;

		List<Villager> nearby = nearbyVillagers(level, player.position(), OBSERVER_RANGE).stream()
			.filter(villager -> !villager.isSleeping() && villager.hasLineOfSight(player)).toList();
		Set<UUID> currentEncounters = new HashSet<>();
		nearby.forEach(villager -> currentEncounters.add(villager.getUUID()));
		Villager speaker = nearby.stream().filter(villager -> !villager.isBaby())
			.min(Comparator.comparingDouble(villager -> villager.distanceToSqr(player))).orElse(null);
		if (speaker == null) speaker = nearby.stream()
			.min(Comparator.comparingDouble(villager -> villager.distanceToSqr(player))).orElse(null);
		if (speaker != null && ENCOUNTERS.add(speaker.getUUID())) {
			String id = speaker.isBaby() ? (player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) ? "fzyrfm" : "wtuguc")
				: player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) ? "gnetsk" : cast(speaker).approach;
			if (playId(speaker, id, "approach:" + speaker.getUUID(), LONG_COOLDOWN, cast(speaker) != CastProfile.VILLAGER)) {
				processSpecialSpeakers(level, player, currentEncounters);
				ENCOUNTERS.retainAll(currentEncounters);
				return;
			}
		}
		if (speaker != null) {
			Vec3 direction = speaker.getEyePosition().subtract(player.getEyePosition()).normalize();
			if (player.getLookAngle().dot(direction) > 0.985) stareTicks += 10;
			else stareTicks = 0;
			if (stareTicks >= 60 && playTitle(speaker, "Stare at a Villager", "stare:" + speaker.getUUID(), LONG_COOLDOWN, true)) {
				stareTicks = 0;
			} else if (ticks % 400L == 0L && stillTicks >= 2400
					&& playTitle(speaker, "Stand Completely Still", "still:" + player.getUUID(), LONG_COOLDOWN, true)) {
				stillTicks = 0;
			} else {
				String context = playerContext(player);
				boolean changed = context != null && !context.equals(lastPlayerContext);
				lastPlayerContext = context;
				if (changed && playTitle(speaker, context, "player_context:" + player.getUUID() + ":" + context, LONG_COOLDOWN, true)) {
					processSpecialSpeakers(level, player, currentEncounters);
					return;
				}
				String environment = environmentContext(level, speaker);
				if (environment != null) playTitle(speaker, environment,
					"environment:" + speaker.getUUID() + ":" + environment, LONG_COOLDOWN, true);
				if (ticks % 200L == 0L) {
					String time = timeContext(level, speaker);
					playTitle(speaker, time, "time:" + speaker.getUUID() + ":" + time, LONG_COOLDOWN, true);
				}
				if (ticks % 100L == 0L && speaker.getDeltaMovement().horizontalDistanceSqr() > 0.0004) {
					CastProfile profile = cast(speaker);
					playId(speaker, profile == CastProfile.VILLAGER ? ambientDialogue(speaker) : profile.idle,
						"idle:" + speaker.getUUID(), LONG_COOLDOWN, profile != CastProfile.VILLAGER);
				}
			}
		}
		processTimeAndDifficulty(level, player);
		processSpecialSpeakers(level, player, currentEncounters);
		ENCOUNTERS.retainAll(currentEncounters);
	}

	private static void processSpecialSpeakers(ClientLevel level, Player player, Set<UUID> currentEncounters) {
		AABB area = AABB.ofSize(player.position(), OBSERVER_RANGE * 2.0, OBSERVER_RANGE, OBSERVER_RANGE * 2.0);
		WanderingTrader trader = level.getEntitiesOfClass(WanderingTrader.class, area, Entity::isAlive).stream()
			.filter(candidate -> candidate.hasLineOfSight(player))
			.min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(player))).orElse(null);
		if (trader != null) {
			currentEncounters.add(trader.getUUID());
			if (ENCOUNTERS.add(trader.getUUID())) playId(trader, "hxlyuc", "approach:" + trader.getUUID(), LONG_COOLDOWN, false);
			else if (trader.hasEffect(MobEffects.INVISIBILITY)) playId(trader, "dbzjqi", "invisible:" + trader.getUUID(), LONG_COOLDOWN, false);
			else if (level.isRainingAt(trader.blockPosition())) playId(trader, "kxoqky", "rain:" + trader.getUUID(), LONG_COOLDOWN, false);
			else if (ticks % 100L == 0L && trader.getDeltaMovement().horizontalDistanceSqr() > 0.0004) {
				playId(trader, "stqafd", "idle:" + trader.getUUID(), LONG_COOLDOWN, false);
			}
		}
		Sheep wooly = level.getEntitiesOfClass(Sheep.class, area, sheep -> sheep.isAlive() && isWooly(sheep)).stream()
			.filter(candidate -> candidate.hasLineOfSight(player))
			.min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(player))).orElse(null);
		if (wooly != null) {
			currentEncounters.add(wooly.getUUID());
			if (ENCOUNTERS.add(wooly.getUUID())) playId(wooly, "uvtocs", "approach:" + wooly.getUUID(), LONG_COOLDOWN, false);
			else if (ticks % 100L == 0L && wooly.getDeltaMovement().horizontalDistanceSqr() > 0.0004) {
				playId(wooly, "vmohcm", "idle:" + wooly.getUUID(), LONG_COOLDOWN, false);
			}
		}
	}

	private static void processTimeAndDifficulty(ClientLevel level, Player player) {
		long worldTime = level.getOverworldClockTime();
		if (lastWorldTime != Long.MIN_VALUE && Math.abs(worldTime - lastWorldTime) > 40L) {
			Villager speaker = nearestAdult(level, player.position(), 32.0);
			if (speaker != null) {
				boolean wasDay = Math.floorMod(lastWorldTime, 24000L) < 12000L;
				boolean isDay = Math.floorMod(worldTime, 24000L) < 12000L;
				playId(speaker, wasDay == isDay ? "uqwdqn" : isDay ? "mgmzeh" : "ohdwnz",
					"time_skip:" + level.dimension().identifier(), SHORT_COOLDOWN, true);
			}
		}
		lastWorldTime = worldTime;
		Difficulty difficulty = level.getDifficulty();
		if (lastDifficulty != null && lastDifficulty != difficulty) {
			Villager speaker = nearestAdult(level, player.position(), 32.0);
			if (speaker != null) playId(speaker, difficulty == Difficulty.HARD ? "arzojk"
				: difficulty == Difficulty.PEACEFUL ? "xuyypm" : "ibcrvx",
				"difficulty:" + difficulty.name(), SHORT_COOLDOWN, true);
		}
		lastDifficulty = difficulty;
	}

	private static void processVillagerStates(ClientLevel level, Player player) {
		List<Villager> villagers = nearbyVillagers(level, player.position(), 24.0);
		Set<UUID> visible = new HashSet<>();
		for (Villager villager : villagers) {
			visible.add(villager.getUUID());
			VillagerSnapshot current = snapshot(villager);
			VillagerSnapshot previous = VILLAGER_SNAPSHOTS.put(villager.getUUID(), current);
			if (!villager.isSleeping()) processConditionDialogues(villager);
			if (villager.isSleeping()) {
				playTitle(villager, "Sleeping", "sleeping:" + villager.getUUID(), LONG_COOLDOWN, true);
				continue;
			}
			if (previous == null) continue;
			if (previous.sleeping() && !current.sleeping()) {
				PendingWake wake = PENDING_WAKES.remove(villager.getUUID());
				if (wake != null && wake.expiresAt() >= ticks) {
					playId(villager, "viwaal", "wake_interact:" + villager.getUUID(), 1L, true);
				} else {
					playTitle(villager, "Wake Up Naturally", "wake:" + villager.getUUID(), LONG_COOLDOWN, true);
				}
			} else if (previous.baby() && !current.baby()) {
				playId(villager, "smvnbj", "grow:" + villager.getUUID(), 1L, false);
			} else if ((previous.profession().equals("none") || previous.profession().equals("nitwit"))
					&& !current.profession().equals("none") && !current.profession().equals("nitwit")) {
				playId(villager, "zndzjx", "job:" + villager.getUUID(), 1L, false);
			} else if (current.level() > previous.level()) {
				playId(villager, current.level() >= 5 ? "pnvkfy" : "fltegg",
					"level:" + villager.getUUID() + ":" + current.level(), 1L, false);
			} else if (!current.name().isEmpty() && !current.name().equals(previous.name())) {
				String lower = current.name().toLowerCase(Locale.ROOT);
				String id = current.baby() ? (lower.equals("dragon") ? "cmrqhw" : "gzsztp")
					: lower.equals("dinnerbone") ? "qmpcxi" : lower.equals("jeb") || lower.equals("jeb_") ? "armupg" : "spfsrr";
				playId(villager, id, "name:" + villager.getUUID() + ":" + current.name(), 1L, false);
			}
			if (villager.isBaby() && villager.getDeltaMovement().horizontalDistanceSqr() > 0.02) {
				DayOfWeek day = LocalDate.now().getDayOfWeek();
				playId(villager, day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY ? "vbclem" : "vhwksn",
					"baby_sprint:" + villager.getUUID(), LONG_COOLDOWN, false);
			}
		}
		VILLAGER_SNAPSHOTS.keySet().retainAll(visible);
		CONDITION_HISTORY.keySet().retainAll(visible);
		CONDITION_CURSORS.keySet().retainAll(visible);
		PENDING_CONDITION_RELIEF.keySet().retainAll(visible);
		PENDING_WAKES.entrySet().removeIf(entry -> entry.getValue().expiresAt() < ticks || !visible.contains(entry.getKey()));
	}

	private static int activeConditionMask(Villager villager) {
		int active = 0;
		if (villager.hasEffect(MobEffects.POISON)) active |= CONDITION_POISON;
		if (villager.hasEffect(MobEffects.SLOWNESS)) active |= CONDITION_SLOWNESS;
		if (villager.hasEffect(MobEffects.WEAKNESS)) active |= CONDITION_WEAKNESS;
		if (villager.isInLava()) active |= CONDITION_LAVA;
		else if (villager.isOnFire()) active |= CONDITION_FIRE;
		if (villager.isFullyFrozen()) active |= CONDITION_FROZEN;
		if (villager.isInWall()) active |= CONDITION_SUFFOCATING;
		return active;
	}

	private static boolean conditionIncludesDialogue(int active, String dialogue) {
		for (int index = 0; index < CONDITION_DIALOGUES.length; index++) {
			if ((active & CONDITION_BITS[index]) != 0 && CONDITION_DIALOGUES[index].equals(dialogue)) return true;
		}
		return false;
	}

	private static String conditionDialogueAt(int active, int ordinal) {
		for (int index = 0; index < CONDITION_BITS.length; index++) {
			if ((active & CONDITION_BITS[index]) == 0) continue;
			if (ordinal-- == 0) return CONDITION_DIALOGUES[index];
		}
		return null;
	}

	private static boolean canSpeakDuringCondition(Villager villager, String dialogue) {
		if (dialogue.equals("hivgme") || dialogue.equals("ecslqo")) return true;
		int active = activeConditionMask(villager);
		String relief = PENDING_CONDITION_RELIEF.get(villager.getUUID());
		if (active == 0) return relief == null || relief.equals(dialogue);
		if (conditionIncludesDialogue(active, dialogue)) return true;
		if (villager.isBaby()) return dialogue.equals("ahcvzd") || dialogue.equals("ecslqo");
		CastProfile profile = cast(villager);
		return profile != CastProfile.VILLAGER && (dialogue.equals(profile.hurt) || dialogue.equals(profile.attack));
	}

	private static void processConditionDialogues(Villager villager) {
		UUID id = villager.getUUID();
		int active = activeConditionMask(villager);
		if (active == 0) {
			Integer history = CONDITION_HISTORY.remove(id);
			CONDITION_CURSORS.remove(id);
			if (history != null && history != 0 && !PENDING_CONDITION_RELIEF.containsKey(id)) {
				String relief = villager.isBaby() ? "wsxfok"
					: cast(villager) == CastProfile.VILLAGER
						? ((history & CONDITION_SUFFOCATING) != 0 ? "fxbysi" : "wbbxpo")
						: null;
				if (relief != null) PENDING_CONDITION_RELIEF.put(id, relief);
			}
			String relief = PENDING_CONDITION_RELIEF.get(id);
			if (relief != null && !isBusy(villager)
					&& playId(villager, relief, "condition_relief:" + id + ":" + relief, 1L, false)) {
				PENDING_CONDITION_RELIEF.remove(id);
			}
			return;
		}
		PENDING_CONDITION_RELIEF.remove(id);
		Integer previousHistory = CONDITION_HISTORY.get(id);
		CONDITION_HISTORY.put(id, previousHistory == null ? active : previousHistory | active);
		CastProfile profile = villager.isBaby() ? CastProfile.VILLAGER : cast(villager);
		int reactionCount = villager.isBaby() || profile != CastProfile.VILLAGER ? 1 : Integer.bitCount(active);
		ActiveDialogue current = ACTIVE_DIALOGUES.get(id);
		if (current != null && current.endTick() > ticks) {
			boolean matching = villager.isBaby() ? current.groupId().equals("ecslqo")
				: profile != CastProfile.VILLAGER ? current.groupId().equals(profile.hurt)
				: conditionIncludesDialogue(active, current.groupId());
			if (matching) return;
		}
		if (isBusy(villager)) stopDialogue(id);
		int cursor = Math.floorMod(CONDITION_CURSORS.getOrDefault(id, 0), reactionCount);
		for (int offset = 0; offset < reactionCount; offset++) {
			int index = (cursor + offset) % reactionCount;
			String dialogue = villager.isBaby() ? "ecslqo"
				: profile != CastProfile.VILLAGER ? profile.hurt : conditionDialogueAt(active, index);
			if (dialogue == null) break;
			if (playId(villager, dialogue, "condition:" + id + ":" + dialogue, 1L, false)) {
				CONDITION_CURSORS.put(id, index + 1);
				break;
			}
		}
	}

	private static void processTrade(Minecraft minecraft) {
		boolean merchantOpen = minecraft.player.containerMenu instanceof MerchantMenu;
		if (merchantOpen && tradeSession == null && pendingTrader != null && ticks <= pendingTraderUntil) {
			Entity trader = minecraft.level.getEntity(pendingTrader);
			if (trader instanceof LivingEntity living) {
				MerchantMenu menu = (MerchantMenu) minecraft.player.containerMenu;
				int uses = tradeUses(menu);
				tradeSession = new TradeSession(pendingTrader, uses, false);
				if (trader instanceof Villager villager) {
					String id = tradeOpeningId(villager, menu);
					playId(villager, id, "trade_open:" + villager.getUUID() + ":" + id, SHORT_COOLDOWN, false);
				} else if (trader instanceof WanderingTrader wanderingTrader) {
					playId(wanderingTrader, "yubpbb", "trade_open:" + wanderingTrader.getUUID(), SHORT_COOLDOWN, false);
				}
			}
			pendingTrader = null;
		}
		if (merchantOpen && tradeSession != null) {
			int uses = tradeUses((MerchantMenu) minecraft.player.containerMenu);
			if (uses > tradeSession.initialUses()) tradeSession = new TradeSession(tradeSession.traderId(), tradeSession.initialUses(), true);
		} else if (!merchantOpen && tradeSession != null) {
			Entity trader = minecraft.level.getEntity(tradeSession.traderId());
			if (trader instanceof Villager villager) {
				CastProfile profile = cast(villager);
				String id = switch (profile) {
					case MAYOR -> tradeSession.completed() ? "shrrya" : "bgzmea";
					case TESTIFICATE_MAN -> tradeSession.completed() ? "xcjort" : "rdugrl";
					case NUMBER_5 -> tradeSession.completed() ? "msofrj" : "lilimm";
					case NUMBER_9 -> tradeSession.completed() ? "czvvwy" : "lilimm";
					default -> tradeSession.completed() ? "czvvwy" : "laztau";
				};
				playId(villager, id, "trade_close:" + villager.getUUID(), 10L, false);
			} else if (trader instanceof WanderingTrader wanderingTrader) {
				playId(wanderingTrader, tradeSession.completed() ? "uzdvsi" : "erbcfn",
					"trade_close:" + wanderingTrader.getUUID(), 10L, false);
			}
			tradeSession = null;
		}
		if (pendingTrader != null && ticks > pendingTraderUntil) {
			Entity trader = minecraft.level.getEntity(pendingTrader);
			if (trader instanceof Villager villager) {
				String profession = profession(villager);
				if (profession.equals("nitwit") || profession.equals("none")) {
					String id = tradeOpeningId(villager, null);
					playId(villager, id, "trade_unavailable:" + villager.getUUID() + ":" + id,
						SHORT_COOLDOWN, false);
				}
			}
			pendingTrader = null;
		}
	}

	private static int tradeUses(MerchantMenu menu) {
		return menu.getOffers().stream().mapToInt(offer -> offer.getUses()).sum();
	}

	private static void onUseEntity(Player player, Entity entity, InteractionHand hand) {
		String heldItem = itemPath(player.getItemInHand(hand));
		if (entity instanceof Villager villager) {
			String gift = foodGiftDialogue(villager.isBaby(), heldItem);
			if (gift != null) {
				playId(villager, gift, "food_gift:" + villager.getUUID() + ":" + heldItem, SHORT_COOLDOWN, false);
				return;
			}
			if (villager.isSleeping()) {
				PENDING_WAKES.put(villager.getUUID(), new PendingWake(player.getUUID(), ticks + 40L));
				return;
			}
			if (villager.isBaby()) {
				playId(villager, "aezdiy", "baby_trade:" + villager.getUUID(), SHORT_COOLDOWN, false);
			} else if (hand == InteractionHand.MAIN_HAND) {
				pendingTrader = villager.getUUID();
				pendingTraderUntil = ticks + 20L;
			}
		} else if (entity instanceof WanderingTrader trader && hand == InteractionHand.MAIN_HAND) {
			pendingTrader = trader.getUUID();
			pendingTraderUntil = ticks + 20L;
		} else if (entity instanceof Sheep sheep && isWooly(sheep)) {
			playId(sheep, heldItem.equals("shears") ? "jqaekk" : "fskcce",
				"interact:" + sheep.getUUID(), SHORT_COOLDOWN, false);
		}
		if (heldItem.equals("lead")) playObserved(player.level(), player, entity.position(), "Use a Lead", SHORT_COOLDOWN);
		if (entity instanceof Sheep && heldItem.equals("shears")) {
			playObserved(player.level(), player, entity.position(), "Shear a Sheep", SHORT_COOLDOWN);
		}
	}

	private static boolean playObserved(Level level, Player player, Vec3 position, String title, long cooldown) {
		Villager speaker = nearbyVillagers(level, position, OBSERVER_RANGE).stream()
			.filter(villager -> !villager.isBaby() && !villager.isSleeping() && cast(villager) == CastProfile.VILLAGER
				&& villager.hasLineOfSight(player))
			.min(Comparator.comparingDouble(villager -> villager.distanceToSqr(position))).orElse(null);
		return speaker != null && playTitle(speaker, title,
			"observed:" + speaker.getUUID() + ":" + title, cooldown, true);
	}

	private static boolean playTitle(LivingEntity speaker, String title, String key, long cooldown, boolean sharedAdult) {
		DialogueCatalog.DialogueGroup group = DialogueCatalog.byTitle(title, sharedAdult ? "villager" : speakerType(speaker));
		return group != null && play(speaker, group, key, cooldown, sharedAdult);
	}

	private static boolean playId(LivingEntity speaker, String id, String key, long cooldown, boolean sharedAdult) {
		DialogueCatalog.DialogueGroup group = DialogueCatalog.byId(id);
		return group != null && play(speaker, group, key, cooldown, sharedAdult);
	}

	private static boolean play(LivingEntity speaker, DialogueCatalog.DialogueGroup group, String key, long cooldown,
			boolean sharedAdult) {
		boolean sharedVoice = speaker instanceof Villager villager && !villager.isBaby() && cast(villager) == CastProfile.VILLAGER
			|| speaker instanceof WanderingTrader;
		boolean blockedByCondition = speaker instanceof Villager conditionVillager
			&& !canSpeakDuringCondition(conditionVillager, group.id());
		if (!(matchesSpeaker(speaker, group) || sharedAdult && sharedVoice && group.speaker().equals("villager"))
				|| blockedByCondition || isBusy(speaker)
				|| !ready(key, VillagerNewsSettings.scaleCooldown(cooldown))) return false;
		List<Integer> recent = RECENT_VARIANTS.getOrDefault(group.id(), List.of());
		DialogueCatalog.DialogueVariant variant = group.chooseVariant(VillagerNewsSettings.rareVoicelines(), Set.copyOf(recent));
		if (variant == null) return false;
		DialogueAnimationPayload payload = new DialogueAnimationPayload(speaker.getUUID(), group.id(), variant.index(),
			(int) variant.durationTicks());
		DialogueSoundState.start(payload);
		DialogueAnimationState.start(payload);
		DialogueSubtitleState.start(payload);
		long end = ticks + variant.durationTicks();
		ACTIVE_DIALOGUES.put(speaker.getUUID(), new ActiveDialogue(group.id(), end));
		BUSY_UNTIL.put(speaker.getUUID(), end + 10L);
		COOLDOWNS.put(key, ticks);
		rememberVariant(group, variant, recent);
		return true;
	}

	private static void rememberVariant(DialogueCatalog.DialogueGroup group, DialogueCatalog.DialogueVariant variant,
			List<Integer> recent) {
		int maximumWeight = group.variants().stream().mapToInt(DialogueCatalog.DialogueVariant::weight).max().orElse(1);
		int eligible = VillagerNewsSettings.rareVoicelines() == 0
			? (int) group.variants().stream().filter(candidate -> candidate.weight() >= maximumWeight * 0.8).count()
			: group.variants().size();
		int historySize = Math.min(8, eligible - 1);
		if (historySize <= 0) return;
		List<Integer> updated = new ArrayList<>(recent);
		updated.remove(Integer.valueOf(variant.index()));
		updated.add(variant.index());
		while (updated.size() > historySize) updated.removeFirst();
		RECENT_VARIANTS.put(group.id(), updated);
	}

	private static boolean matchesSpeaker(LivingEntity speaker, DialogueCatalog.DialogueGroup group) {
		if (speaker instanceof Villager villager) {
			if (villager.isBaby()) return BABY_DIALOGUES.contains(group.id());
			if (BABY_DIALOGUES.contains(group.id())) return false;
		}
		return group.speaker().equals(speakerType(speaker));
	}

	private static boolean ready(String key, long cooldown) {
		return ticks - COOLDOWNS.getOrDefault(key, Long.MIN_VALUE / 2L) >= cooldown;
	}

	private static boolean isBusy(LivingEntity speaker) {
		ActiveDialogue active = ACTIVE_DIALOGUES.get(speaker.getUUID());
		return BUSY_UNTIL.getOrDefault(speaker.getUUID(), 0L) > ticks || active != null && active.endTick() > ticks;
	}

	private static void stopDialogue(UUID id) {
		ACTIVE_DIALOGUES.remove(id);
		BUSY_UNTIL.remove(id);
		DialogueAnimationPayload stop = new DialogueAnimationPayload(id, "", 0, 0);
		DialogueSoundState.start(stop);
		DialogueAnimationState.start(stop);
		DialogueSubtitleState.start(stop);
	}

	private static List<Villager> nearbyVillagers(Level level, Vec3 position, double range) {
		return level.getEntitiesOfClass(Villager.class, AABB.ofSize(position, range * 2.0, range, range * 2.0),
			entity -> entity.isAlive());
	}

	private static Villager nearestAdult(Level level, Vec3 position, double range) {
		return nearbyVillagers(level, position, range).stream().filter(villager -> !villager.isBaby() && !villager.isSleeping())
			.min(Comparator.comparingDouble(villager -> villager.distanceToSqr(position))).orElse(null);
	}

	private static String speakerType(LivingEntity speaker) {
		if (speaker instanceof Villager villager) return cast(villager).speaker;
		if (speaker instanceof WanderingTrader) return "wandering_trader";
		if (speaker instanceof Sheep sheep && isWooly(sheep)) return "wooly";
		return "";
	}

	private static CastProfile cast(Villager villager) {
		String name = villager.getName().getString().toLowerCase(Locale.ROOT);
		if (name.equals("mayor") || name.equals("the mayor") || name.equals("mayor villager")) return CastProfile.MAYOR;
		if (name.equals("testificate man")) return CastProfile.TESTIFICATE_MAN;
		if (name.equals("villager #5") || name.equals("villager number 5")) return CastProfile.NUMBER_5;
		if (name.equals("villager #9") || name.equals("villager number 9")) return CastProfile.NUMBER_9;
		if (name.equals("villager unreachable") || name.equals("can't catch me!")) return CastProfile.UNREACHABLE;
		return CastProfile.VILLAGER;
	}

	private static boolean isWooly(Sheep sheep) {
		String name = sheep.getName().getString().toLowerCase(Locale.ROOT);
		return name.equals("wooly") || name.equals("wooly the sheep");
	}

	private static VillagerSnapshot snapshot(Villager villager) {
		String name = villager.hasCustomName() && villager.getCustomName() != null ? villager.getCustomName().getString() : "";
		return new VillagerSnapshot(villager.isBaby(), profession(villager), villager.getVillagerData().level(), name,
			villager.isSleeping());
	}

	private static String profession(Villager villager) {
		return villager.getVillagerData().profession().unwrapKey()
			.map(key -> key.identifier().getPath()).orElse("none");
	}

	private static String tradeOpeningId(Villager villager, MerchantMenu menu) {
		CastProfile profile = cast(villager);
		if (profile != CastProfile.VILLAGER) return profile.trade;
		String profession = profession(villager);
		if (profession.equals("nitwit")) return "nukxsf";
		if (profession.equals("none")) return "nlbhku";
		if (menu != null && menu.getOffers().isEmpty()) return "zalmof";
		return profile.trade;
	}

	private static String foodGiftDialogue(boolean baby, String item) {
		if (baby) return switch (item) {
			case "beetroot" -> "qrdzmt";
			case "bread" -> "hbalps";
			case "carrot" -> "hcdvqm";
			case "potato" -> "gotjxf";
			default -> null;
		};
		return switch (item) {
			case "beetroot" -> "rlfjux";
			case "bread" -> "bbjsik";
			case "carrot" -> "nqktml";
			case "potato" -> "ytydjc";
			case "wheat" -> "ebyrtk";
			default -> null;
		};
	}

	private static String weaponAttackDialogue(String item) {
		if (item.endsWith("_sword")) return "rueszy";
		if (item.endsWith("_axe")) return "yjctyw";
		if (item.endsWith("_hoe")) return "qqyjjg";
		if (item.endsWith("_shovel")) return "hpnsfu";
		return "vevdkl";
	}

	private static String environmentContext(Level level, Villager villager) {
		Entity vehicle = villager.getVehicle();
		if (vehicle != null) {
			String path = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).getPath();
			if (path.contains("minecart")) return vehicle.getDeltaMovement().horizontalDistanceSqr() > 0.001
				? "Ride in a Moving Minecart" : "Sit in a Minecart";
			if (path.contains("boat")) return vehicle.isInWater() ? "Boat on Water" : "Boat on Land";
		}
		if (level.dimension() == Level.NETHER) return "Wander in the Nether";
		if (level.dimension() == Level.END) return "Wander in the End";
		if (level.dimension() != Level.OVERWORLD) return "Wander in Another Dimension";
		if (level.isRainingAt(villager.blockPosition())) return "Caught in the Rain";
		if (villager.isInWater()) return "Stand in Shallow Water";
		String biome = level.getBiome(villager.blockPosition()).unwrapKey().map(key -> key.identifier().getPath()).orElse("");
		if (biome.contains("desert") || biome.contains("badlands") || biome.contains("savanna")) return "Wander Somewhere Hot";
		if (biome.contains("snow") || biome.contains("frozen") || biome.contains("ice") || biome.contains("cold")) return "Wander Somewhere Cold";
		BlockState below = level.getBlockState(villager.blockPosition().below());
		if (below.is(Blocks.ICE) || below.is(Blocks.PACKED_ICE) || below.is(Blocks.BLUE_ICE)) return "Stand on Ice";
		if (below.is(Blocks.SNOW_BLOCK) || below.is(Blocks.POWDER_SNOW)) return "Stand on Snow";
		if (below.is(Blocks.MAGMA_BLOCK)) return "Stand on Magma";
		for (int x = -3; x <= 3; x++) for (int y = -2; y <= 2; y++) for (int z = -3; z <= 3; z++) {
			String block = blockPath(level.getBlockState(villager.blockPosition().offset(x, y, z)));
			if (block.contains("campfire")) return "See a Campfire";
			if (block.equals("fire") || block.equals("soul_fire")) return "Stand Near Fire";
			if (block.equals("bookshelf") && villager.getDeltaMovement().horizontalDistanceSqr() < 0.0004) {
				return "Inspect Bookshelves";
			}
		}
		if (villager.getY() < level.getSeaLevel() - 30) return "Wander Deep Underground";
		if (villager.getY() > level.getSeaLevel() + 75) return "Wander High Above the Ground";
		return null;
	}

	private static String timeContext(Level level, Villager villager) {
		float angle = level.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE, villager.blockPosition());
		if (angle < 0.125F || angle >= 0.875F) return "Morning";
		if (angle < 0.45F) return "Afternoon";
		if (angle < 0.625F) return "Evening";
		return "Night";
	}

	private static String ambientDialogue(Villager villager) {
		String profession = profession(villager);
		if (profession.equals("nitwit")) return "uookqp";
		if (profession.equals("none")) return "gbxzxv";
		LocalDate date = LocalDate.now();
		List<String> choices = new ArrayList<>();
		if (date.getMonthValue() == 10) choices.add("mltyge");
		if (date.getMonthValue() == 12) choices.add("tkkegl");
		if (date.getMonthValue() == 1 && date.getDayOfMonth() == 1) choices.add("uyqiwv");
		if (date.getMonthValue() == 2 && date.getDayOfMonth() == 14) choices.add("fabiyx");
		if (date.getMonthValue() == 4 && date.getDayOfMonth() == 1) choices.add("obitls");
		if (date.getMonthValue() == 10 && date.getDayOfMonth() == 31) choices.add("adhxce");
		if (date.getMonthValue() == 12 && date.getDayOfMonth() == 25) choices.add("rclyrl");
		if (date.getDayOfMonth() == 13 && date.getDayOfWeek() == DayOfWeek.FRIDAY) choices.add("qfcwvz");
		if (LocalDateTime.now().getMinute() == 0) choices.add("jqgkhy");
		choices.add(switch (date.getDayOfWeek()) {
			case MONDAY -> ticks / LONG_COOLDOWN % 2L == 0L ? "gkvlqc" : "jpucos";
			case TUESDAY -> ticks / LONG_COOLDOWN % 2L == 0L ? "dkpihl" : "lgeeem";
			case WEDNESDAY -> ticks / LONG_COOLDOWN % 2L == 0L ? "gwakiz" : "qiqiez";
			case THURSDAY -> ticks / LONG_COOLDOWN % 2L == 0L ? "zglkgp" : "caiyte";
			case FRIDAY -> ticks / LONG_COOLDOWN % 2L == 0L ? "ypyumu" : "cxtvsx";
			case SATURDAY -> ticks / LONG_COOLDOWN % 2L == 0L ? "ildosa" : "lfhnxz";
			case SUNDAY -> ticks / LONG_COOLDOWN % 2L == 0L ? "uzvatl" : "zckxrc";
		});
		if (choices.size() == 1) choices.add("lvigit");
		return choices.get(Math.floorMod((int) (ticks / LONG_COOLDOWN), choices.size()));
	}

	private static String playerContext(Player player) {
		if (player.isFallFlying()) return "Glide with Elytra";
		if (player.isCreative() && player.getAbilities().flying) return "Fly in Creative Mode";
		if (player.isShiftKeyDown() && player.getDeltaMovement().horizontalDistanceSqr() > 0.0001) return "Crouch-Walk";
		if (player.getHealth() <= player.getMaxHealth() * 0.3F) return "Low Health";
		if (player.hasEffect(MobEffects.INVISIBILITY)) return "Invisibility";
		if (player.hasEffect(MobEffects.DARKNESS)) return "Darkness";
		if (player.hasEffect(MobEffects.NIGHT_VISION)) return "Night Vision";
		if (player.hasEffect(MobEffects.WATER_BREATHING)) return "Water Breathing";
		if (player.hasEffect(MobEffects.SPEED)) return "Swiftness";
		if (player.hasEffect(MobEffects.SLOWNESS)) return "Slowness";
		if (player.hasEffect(MobEffects.STRENGTH)) return "Strength";
		if (player.hasEffect(MobEffects.WEAKNESS)) return "Weakness";
		if (player.hasEffect(MobEffects.HUNGER)) return "Hunger";
		if (player.hasEffect(MobEffects.NAUSEA)) return "Nausea";
		if (player.hasEffect(MobEffects.BAD_OMEN) || player.hasEffect(MobEffects.RAID_OMEN)) return "Bad Omen";
		if (player.hasEffect(MobEffects.OOZING)) return "Oozing";
		if (player.getActiveEffects().size() >= 2) return "Multiple Status Effects";
		if (blockPath(player.level().getBlockState(player.blockPosition().below())).endsWith("_bed")) return "Stand on a Villager's Bed";
		ItemStack held = player.getMainHandItem();
		if (held.isDamageableItem() && held.getDamageValue() >= held.getMaxDamage() * 0.85F) return "Hold a Nearly Broken Item";
		int armor = 0;
		Set<String> materials = new HashSet<>();
		for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
			ItemStack stack = player.getItemBySlot(slot);
			if (stack.isEmpty()) continue;
			armor++;
			String path = itemPath(stack);
			materials.add(path.substring(0, path.indexOf('_') > 0 ? path.indexOf('_') : path.length()));
		}
		if (armor == 4 && materials.size() == 1 && materials.contains("iron")) return "Wear Full Iron Armor";
		if (armor == 4 && materials.size() > 1) return "Wear Mixed Armor";
		if (armor == 4 && (materials.contains("diamond") || materials.contains("netherite"))) return "Wear High-Level Armor";
		if (armor > 0) return "Wear Armor";
		return null;
	}

	private static String selectBreakContext(BlockState state, boolean multiple) {
		if (multiple) return "Break Multiple Blocks";
		String path = blockPath(state);
		if (path.equals("wheat") || path.equals("carrots") || path.equals("potatoes") || path.equals("beetroots")
			|| path.equals("torchflower_crop") || path.equals("pitcher_crop")) return "Harvest Crops";
		if (path.contains("flower") || path.contains("candle") || path.contains("coral") || path.contains("banner")
			|| path.contains("decorated_pot")) return "Break a Decorative Block";
		if (path.endsWith("_door")) return "Break a Door";
		if (path.endsWith("_bed")) return "Break a Bed";
		if (path.equals("bell")) return "Break a Bell";
		if (isWorkstation(path)) return "Break a Workstation";
		if (path.contains("log") || path.contains("wood") || path.contains("stem") || path.contains("hyphae")) return "Break Wood";
		if (path.contains("stone") || path.contains("deepslate") || path.contains("cobblestone")) return "Break Stone";
		return "Break a Block";
	}

	private static String selectPlaceContext(Block block, Level level, BlockPos position) {
		String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
		if (level.dimension() == Level.END) return "Place a Block from the End";
		if (level.dimension() == Level.NETHER) return "Place a Block from the Nether";
		String biome = level.getBiome(position).unwrapKey().map(key -> key.identifier().getPath()).orElse("");
		if (biome.contains("ocean")) return "Place a Block from the Ocean";
		if (path.equals("redstone_wire")) return "Place Redstone Dust";
		if (path.equals("daylight_detector")) return "Place a Daylight Detector";
		if (path.equals("detector_rail")) return "Place a Detector Rail";
		if (path.equals("lightning_rod")) return "Place a Lightning Rod";
		if (path.equals("melon")) return "Place a Melon";
		if (path.equals("observer")) return "Place an Observer";
		if (path.endsWith("pressure_plate")) return "Place a Pressure Plate";
		if (path.equals("pumpkin")) return "Place a Pumpkin";
		if (path.equals("redstone_lamp")) return "Place a Redstone Lamp";
		if (path.equals("repeater")) return "Place a Redstone Repeater";
		if (path.equals("redstone_torch") || path.equals("redstone_wall_torch")) return "Place a Redstone Torch";
		if (path.contains("sculk_sensor")) return "Place a Sculk Sensor";
		if (path.equals("tripwire_hook")) return "Place a Tripwire Hook";
		if (path.equals("jack_o_lantern")) return "Place a Jack o'Lantern";
		if (path.equals("end_stone")) return "Place End Stone";
		if (path.contains("purpur")) return "Place Purpur";
		if (path.contains("copper")) return "Place a Copper Block";
		if (path.contains("brick")) return "Place Bricks";
		if (path.equals("powder_snow")) return "Place Powder Snow";
		if (path.equals("light")) return "Place a Light Block";
		if (path.equals("barrier") || path.contains("command_block") || path.equals("structure_block") || path.equals("jigsaw")) {
			return "Place a Creative-Only Block";
		}
		if (path.endsWith("sand") || path.endsWith("gravel") || path.equals("anvil")) return "Place a Gravity-Affected Block";
		if (path.equals("iron_block")) return "Place an Iron Block";
		if (path.equals("gold_block")) return "Place a Gold Block";
		if (path.equals("diamond_block")) return "Place a Diamond Block";
		if (path.equals("emerald_block")) return "Place an Emerald Block";
		if (path.equals("netherite_block")) return "Place a Valuable Block";
		if (path.endsWith("_button")) return "Place a Button";
		if (path.equals("lever")) return "Place a Lever";
		if (path.contains("redstone") || path.endsWith("button") || path.endsWith("rail")) return "Place a Redstone Component";
		if (path.endsWith("_bed")) return "Place a Bed";
		if (path.equals("chest")) return "Place a Chest";
		if (path.equals("trapped_chest")) return "Place a Trapped Chest";
		if (path.equals("crafting_table")) return "Place a Crafting Table";
		if (path.equals("furnace")) return "Place a Furnace";
		if (path.equals("bookshelf")) return "Place a Bookshelf";
		if (path.equals("jukebox")) return "Place a Jukebox";
		if (path.equals("armor_stand")) return "Place an Armor Stand";
		if (path.equals("beacon")) return "Place a Beacon";
		if (isWorkstation(path)) return "Place a Workstation";
		if (path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_planks")) return "Place Wood";
		if (path.contains("dirt")) return "Place Dirt";
		if (path.contains("leaves") || path.contains("sapling") || path.contains("flower")) return "Place Leaves or Plants";
		if (path.contains("wool")) return "Place Wool";
		if (path.contains("glass")) return "Place Glass";
		if (path.contains("concrete_powder")) return "Place Concrete Powder";
		if (path.contains("concrete")) return "Place Concrete";
		if (path.contains("glazed_terracotta")) return "Place Glazed Terracotta";
		if (path.contains("terracotta")) return "Place Terracotta";
		if (path.equals("lapis_block")) return "Place a Lapis Block";
		if (path.contains("ice")) return "Place Ice";
		if (path.contains("snow")) return "Place Snow";
		return "Place a Block";
	}

	private static String selectUseBlockContext(BlockState state) {
		String path = blockPath(state);
		if (path.endsWith("_button")) return "Press a Button";
		if (path.equals("bell")) return "Hear a Bell Ring";
		if (path.equals("lever")) return "Flip a Lever";
		if (path.endsWith("_door")) return "Use a Door";
		if (path.endsWith("_fence_gate")) return state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN)
			? "Close a Fence Gate" : "Open a Fence Gate";
		if (path.equals("crafter")) return "Use a Crafter";
		if (path.equals("dispenser")) return "Use a Dispenser";
		if (path.equals("dropper")) return "Use a Dropper";
		if (path.equals("jukebox")) return "Use a Jukebox";
		if (path.equals("loom")) return "Use a Loom";
		if (path.contains("shulker_box")) return "Use a Shulker Box";
		if (path.equals("stonecutter")) return "Use a Stonecutter";
		if (path.equals("beacon")) return "Use a Beacon";
		if (path.contains("campfire")) return "Use a Campfire";
		if (path.equals("cartography_table")) return "Use a Cartography Table";
		if (path.equals("cauldron") || path.endsWith("_cauldron")) return "Use a Cauldron";
		if (path.equals("chiseled_bookshelf")) return "Use a Chiseled Bookshelf";
		if (path.equals("composter")) return "Use a Composter";
		if (path.equals("ender_chest")) return "Use an Ender Chest";
		if (path.contains("shelf")) return "Use Shelves";
		if (path.contains("chest")) return "Open a Chest";
		if (path.equals("crafting_table")) return "Use a Crafting Table";
		if (path.equals("furnace") || path.equals("blast_furnace") || path.equals("smoker")) return "Use a Furnace";
		if (path.equals("anvil") || path.endsWith("_anvil")) return "Use an Anvil";
		if (path.equals("enchanting_table")) return "Use an Enchanting Table";
		if (path.equals("brewing_stand")) return "Use a Brewing Stand";
		if (path.equals("grindstone")) return "Use a Grindstone";
		if (path.equals("smithing_table")) return "Use a Smithing Table";
		return null;
	}

	private static String selectHeldBlockContext(ItemStack stack, BlockState clicked) {
		String item = itemPath(stack);
		String block = blockPath(clicked);
		if ((item.equals("flint_and_steel") || item.equals("fire_charge")) && block.equals("tnt")) return "Light TNT";
		if (block.equals("tnt")) return "See TNT";
		if ((item.equals("flint_and_steel") || item.equals("fire_charge")) && block.contains("campfire")) return "Light a Campfire";
		if ((item.equals("flint_and_steel") || item.equals("fire_charge")) && block.contains("candle")) return "Light a Candle";
		if (block.contains("campfire") && (item.contains("beef") || item.contains("porkchop") || item.contains("chicken")
			|| item.contains("mutton") || item.contains("rabbit") || item.equals("potato"))) return "Cook Food on a Campfire";
		if ((item.equals("water_bucket") || item.endsWith("_shovel")) && block.contains("campfire")) return "Extinguish a Campfire";
		if (item.equals("water_bucket") && block.contains("candle")) return "Extinguish a Candle";
		if (item.equals("shears") && block.equals("pumpkin")) return "Carve a Pumpkin";
		return null;
	}

	private static String selectUseItemContext(ItemStack stack) {
		String path = itemPath(stack);
		if (path.equals("firework_rocket")) return "Set Off a Firework";
		if (path.equals("ender_pearl")) return "Teleport with an Ender Pearl";
		if (path.equals("snowball")) return "Snowball";
		if (path.contains("apple") || path.contains("bread") || path.contains("carrot") || path.contains("potato")
			|| path.contains("beef") || path.contains("porkchop") || path.contains("chicken") || path.contains("mutton")
			|| path.contains("rabbit") || path.contains("stew") || path.contains("berries") || path.contains("melon")) return "Eat Food";
		return null;
	}

	private static boolean isWorkstation(String path) {
		return path.equals("composter") || path.equals("barrel") || path.equals("blast_furnace") || path.equals("smoker")
			|| path.equals("cartography_table") || path.equals("brewing_stand") || path.equals("fletching_table")
			|| path.equals("cauldron") || path.equals("lectern") || path.equals("stonecutter") || path.equals("loom")
			|| path.equals("smithing_table") || path.equals("grindstone");
	}

	private static boolean nearBlock(Level level, BlockPos origin, String pathPart, int range) {
		for (int x = -range; x <= range; x++) for (int y = -range; y <= range; y++) for (int z = -range; z <= range; z++) {
			if (blockPath(level.getBlockState(origin.offset(x, y, z))).contains(pathPart)) return true;
		}
		return false;
	}

	private static String itemPath(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
	}

	private static String blockPath(BlockState state) {
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
	}

	private enum CastProfile {
		VILLAGER("villager", "xfpjxq", "lvigit", "clbjww", "wyvzhk", "vevdkl"),
		MAYOR("mayor", "dpwhhs", "xxehbq", "njyapy", "ssbhiv", "ltdnvy"),
		TESTIFICATE_MAN("testificate_man", "nmwmrz", "luoibc", "mpbnsm", "fzoqwd", "fzoqwd"),
		NUMBER_5("number_5", "xccwah", "legnsy", "sclaoa", "behifz", "behifz"),
		NUMBER_9("number_9", "kzogzi", "ezgbfw", "snnkrl", "wrbvvp", "asuufu"),
		UNREACHABLE("unreachable", "eltxge", "eltxge", "eltxge", "wyvzhk", "vevdkl");

		private final String speaker;
		private final String approach;
		private final String idle;
		private final String trade;
		private final String hurt;
		private final String attack;

		CastProfile(String speaker, String approach, String idle, String trade, String hurt, String attack) {
			this.speaker = speaker;
			this.approach = approach;
			this.idle = idle;
			this.trade = trade;
			this.hurt = hurt;
			this.attack = attack;
		}
	}

	private record PendingWake(UUID sourceId, long expiresAt) {
	}

	private record PendingBell(ClientLevel level, Vec3 position, long dueTick) {
	}

	private record PendingBellReaction(ClientLevel level, UUID villagerId, Vec3 position, long dueTick, long expireTick) {
	}

	private record ActiveDialogue(String groupId, long endTick) {
	}

	private record EntitySnapshot(int hurtTime, int deathTime, boolean alive) {
	}

	private record VillagerSnapshot(boolean baby, String profession, int level, String name, boolean sleeping) {
	}

	private record PendingAttack(String itemPath, long expiresAt) {
	}

	private record PendingPlacement(Block block, long confirmAt, long expiresAt) {
	}

	private record PendingBreak(BlockState state, long confirmAt, long expiresAt) {
	}

	private record TradeSession(UUID traderId, int initialUses, boolean completed) {
	}
}
