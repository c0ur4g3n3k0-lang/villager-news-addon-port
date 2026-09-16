import { existsSync, readFileSync, readdirSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const resources = join(root, "src", "main", "resources");
const modAssets = join(resources, "assets", "villager-news-addon-port");
const cem = join(resources, "assets", "minecraft", "optifine", "cem");
const catalog = JSON.parse(readFileSync(join(modAssets, "dialogues.json"), "utf8"));
const sounds = JSON.parse(readFileSync(join(modAssets, "sounds.json"), "utf8"));
const animations = JSON.parse(readFileSync(join(modAssets, "dialogue_animations.json"), "utf8"));
const handbook = JSON.parse(readFileSync(join(modAssets, "handbook.json"), "utf8"));
const behaviorSource = readFileSync(join(root, "src/main/java/com/vnap/dialogue/ContextualDialogueController.java"), "utf8");
const dialogueTestSource = readFileSync(join(root, "src/main/java/com/vnap/command/DialogueTestCommand.java"), "utf8");
const buildSettingsSource = readFileSync(join(root, "src/main/java/com/vnap/config/VillagerNewsBuildSettings.java"), "utf8");
const initializerSource = readFileSync(join(root, "src/main/java/com/vnap/VillagerNewsAddonPort.java"), "utf8");
const buildSource = readFileSync(join(root, "build.gradle"), "utf8");
const buildSettingsResource = readFileSync(join(resources, "villager-news-addon-port-build.properties"), "utf8");
const fabricMod = JSON.parse(readFileSync(join(resources, "fabric.mod.json"), "utf8"));
const itemSource = readFileSync(join(root, "src/main/java/com/vnap/item/VillagerNewsItems.java"), "utf8");
const handbookSource = readFileSync(join(root, "src/main/java/com/vnap/client/HandbookScreen.java"), "utf8");
const clientSource = readFileSync(join(root, "src/main/java/com/vnap/client/VillagerNewsAddonPortClient.java"), "utf8");
const signLayerSource = readFileSync(join(root, "src/main/java/com/vnap/client/VillagerNewsSignLayer.java"), "utf8");
const professionLayerSource = readFileSync(join(root, "src/main/java/com/vnap/mixin/client/VillagerProfessionLayerMixin.java"), "utf8");
const villagerRendererSource = readFileSync(join(root, "src/main/java/com/vnap/mixin/client/VillagerRendererMixin.java"), "utf8");
const villagerSoundSource = readFileSync(join(root, "src/main/java/com/vnap/mixin/VillagerSoundMixin.java"), "utf8");
const sheepSoundSource = readFileSync(join(root, "src/main/java/com/vnap/mixin/SheepSoundMixin.java"), "utf8");
const wanderingTraderSoundSource = readFileSync(join(root, "src/main/java/com/vnap/mixin/WanderingTraderSoundMixin.java"), "utf8");
const compatibilitySource = readFileSync(join(root, "src/main/java/com/vnap/client/ServerCompatibilityState.java"), "utf8");
const clientOnlySource = readFileSync(join(root, "src/main/java/com/vnap/client/ClientOnlyDialogueController.java"), "utf8");
const clientOnlySoundGateSource = readFileSync(join(root, "src/main/java/com/vnap/client/ClientOnlyVanillaSoundGate.java"), "utf8");
const clientPacketListenerMixinSource = readFileSync(join(root, "src/main/java/com/vnap/mixin/client/ClientPacketListenerMixin.java"), "utf8");
const subtitleSource = readFileSync(join(root, "src/main/java/com/vnap/client/DialogueSubtitleState.java"), "utf8");
const specialSpeakerNamesSource = readFileSync(join(root, "src/main/java/com/vnap/client/SpecialSpeakerNames.java"), "utf8");
const subtitleHudSource = readFileSync(join(root, "src/main/java/com/vnap/client/VillagerNewsSubtitleHud.java"), "utf8");
const soundStateSource = readFileSync(join(root, "src/main/java/com/vnap/client/DialogueSoundState.java"), "utf8");
const animationStateSource = readFileSync(join(root, "src/main/java/com/vnap/client/DialogueAnimationState.java"), "utf8");
const settingsSource = readFileSync(join(root, "src/main/java/com/vnap/config/VillagerNewsSettings.java"), "utf8");
const settingsStateSource = readFileSync(join(root, "src/main/java/com/vnap/client/VillagerNewsSettingsState.java"), "utf8");
const clientSettingsSource = readFileSync(join(root, "src/main/java/com/vnap/client/VillagerNewsClientSettings.java"), "utf8");
const keyMappingsSource = readFileSync(join(root, "src/main/java/com/vnap/client/VillagerNewsKeyMappings.java"), "utf8");
const modMenuSource = readFileSync(join(root, "src/main/java/com/vnap/client/VillagerNewsModMenu.java"), "utf8");
const settingsNetworkSource = readFileSync(join(root, "src/main/java/com/vnap/network/VillagerNewsSettingsNetwork.java"), "utf8");
const settingsPayloadSource = readFileSync(join(root, "src/main/java/com/vnap/network/VillagerNewsSettingsPayload.java"), "utf8");
const abstractVillagerSource = readFileSync(join(root, "src/main/java/com/vnap/mixin/AbstractVillagerMixin.java"), "utf8");
const villagerDataSource = readFileSync(join(root, "src/main/java/com/vnap/mixin/VillagerDataMixin.java"), "utf8");
const tradeBackupSource = readFileSync(join(root, "src/main/java/com/vnap/entity/VillagerTradeBackup.java"), "utf8");
const mixinConfiguration = readFileSync(join(resources, "villager-news-addon-port.mixins.json"), "utf8");
const clientMixinConfiguration = readFileSync(join(resources, "villager-news-addon-port.client.mixins.json"), "utf8");
const generatorSource = readFileSync(join(root, "tools/port-addon.mjs"), "utf8");
const signMessages = JSON.parse(readFileSync(join(root, "tools/sign-messages.json"), "utf8"));
const handbookLocalizationSource = readFileSync(join(root, "tools/sync-handbook-language.mjs"), "utf8");
const villagerModelSource = readFileSync(join(cem, "villager.jem"), "utf8");
const gradleProperties = readFileSync(join(root, "gradle.properties"), "utf8");
const language = JSON.parse(readFileSync(join(modAssets, "lang", "en_us.json"), "utf8"));
const russianLanguage = JSON.parse(readFileSync(join(modAssets, "lang", "ru_ru.json"), "utf8"));

function executableAvailable(executable) {
  if (!executable) return false;
  try {
    execFileSync(executable, ["-version"], { stdio: "ignore" });
    return true;
  } catch {
    return false;
  }
}

const ffmpeg = [process.env.FFMPEG_PATH, "ffmpeg"].find(executableAvailable);

function check(condition, message) {
  if (!condition) throw new Error(message);
}

check(ffmpeg, "FFmpeg is required for image verification. Add it to PATH or set FFMPEG_PATH to its executable.");

const groups = Object.entries(catalog.groups);
check(groups.length === 523, `Expected 523 dialogue groups, found ${groups.length}`);
let variantCount = 0;
let subtitleCount = 0;
for (const [id, group] of groups) {
  check(group.variants?.length, `Dialogue ${id} has no variants`);
  check(animations.groups[id]?.length === group.variants.length, `Dialogue ${id} has mismatched animation variants`);
  for (const variant of group.variants) {
    const event = sounds[`dialogue.${id}.${variant.index}`];
    check(event?.sounds?.length === 1, `Dialogue ${id}.${variant.index} must have one exact sound`);
    check(event.subtitle === undefined, `Dialogue ${id}.${variant.index} still uses the bottom-right subtitle overlay`);
    check(variant.subtitles?.length > 0, `Dialogue ${id}.${variant.index} has no original subtitle timeline`);
    check(variant.subtitles.every((entry, index) => typeof entry.key === "string"
      && typeof language[entry.key] === "string" && language[entry.key].length > 0
      && Number.isFinite(entry.time) && (index === 0 || entry.time >= variant.subtitles[index - 1].time)),
      `Dialogue ${id}.${variant.index} has an invalid subtitle timeline`);
		subtitleCount += variant.subtitles.length;
    variantCount++;
    const sound = event.sounds[0];
		check(typeof sound === "object" && sound.stream === true, `Dialogue ${id}.${variant.index} is not streamed`);
    const name = typeof sound === "string" ? sound : sound.name;
    const relative = name.replace("villager-news-addon-port:", "");
    check(existsSync(join(modAssets, "sounds", `${relative}.ogg`)), `Missing audio file for ${name}`);
  }
}
check(variantCount === 2212, `Expected 2212 synchronized variants, found ${variantCount}`);
check(subtitleCount === 3741, `Expected 3741 timed subtitles, found ${subtitleCount}`);
check(clientSource.includes("DialogueSubtitleState.start(payload)")
  && clientSource.includes("VillagerNewsSubtitleHud.register()")
  && clientSource.includes("DialogueSubtitleState.tick(client)"),
"The timed subtitle client is not registered");
check(clientSource.includes("VillagerNewsClientSettings.load()")
	&& clientSettingsSource.includes("villager-news-addon-port-client.json")
	&& clientSettingsSource.includes("private static boolean subtitlesEnabled = true")
	&& clientSettingsSource.includes("setSubtitlesEnabled(boolean enabled)")
	&& handbookSource.includes("VillagerNewsClientSettings.setSubtitlesEnabled(enabled)")
	&& handbookSource.includes("settings.villager-news-addon-port.subtitles")
	&& !handbookSource.includes("showSubtitles")
	&& language["settings.villager-news-addon-port.subtitles"] === "Villager News Subtitles"
	&& !settingsSource.includes("subtitlesEnabled")
	&& !settingsPayloadSource.includes("subtitlesEnabled")
	&& !settingsNetworkSource.includes("subtitlesEnabled"),
"Villager News subtitles do not have an isolated, enabled-by-default client preference");
check(clientSource.includes("VillagerNewsKeyMappings.register()")
	&& clientSource.includes("VillagerNewsKeyMappings.tick(client)")
	&& keyMappingsSource.includes("KeyMappingHelper.registerKeyMapping(new KeyMapping(")
	&& keyMappingsSource.includes("KeyMapping.Category.register(VillagerNewsAddonPort.id(\"general\"))")
	&& keyMappingsSource.includes("InputConstants.KEY_N")
	&& keyMappingsSource.includes("openSettings.consumeClick()")
	&& keyMappingsSource.includes("client.gui.screen() == null")
	&& keyMappingsSource.includes("HandbookScreen.settingsScreen(null)")
	&& !keyMappingsSource.includes("InputConstants.isKeyDown")
	&& language["key.category.villager-news-addon-port.general"] === "Villager News"
	&& language["key.villager-news-addon-port.open_settings"] === "Open Villager News Settings",
"The in-game settings shortcut is not a remappable client key mapping");
check(!subtitleSource.includes("HudElementRegistry")
  && !subtitleSource.includes("GuiGraphicsExtractor")
	&& subtitleSource.includes("public static List<VisibleSubtitle> visible(Minecraft minecraft, long now)")
	&& subtitleSource.includes("frameStartNanos")
	&& subtitleSource.includes("frameEndNanos")
	&& subtitleHudSource.includes("HudElementRegistry.attachElementAfter")
	&& subtitleHudSource.includes("DialogueSubtitleState.visible")
	&& subtitleHudSource.includes("VillagerNewsClientSettings.subtitlesEnabled()")
	&& !subtitleSource.includes("showSubtitles")
	&& !subtitleHudSource.includes("showSubtitles")
	&& subtitleHudSource.includes("MAX_CARDS = 4")
	&& subtitleHudSource.includes("graphics.guiHeight() - BOTTOM_MARGIN")
	&& subtitleHudSource.includes("(graphics.guiWidth() - layout.width()) / 2")
	&& subtitleHudSource.includes("graphics.fill(left, top, left + layout.width(), top + layout.height(), applyOpacity(BACKGROUND_COLOR, opacity))")
	&& subtitleHudSource.includes("graphics.text(font, line, centerX - font.width(line) / 2, lineY, applyOpacity(SPEAKER_COLOR, opacity), true)")
	&& subtitleHudSource.includes("graphics.text(font, line, centerX - font.width(line) / 2, lineY, applyOpacity(TRANSCRIPT_COLOR, opacity), true)")
	&& subtitleSource.includes("RANGE_SQUARED")
	&& subtitleHudSource.includes("bottomY = top - CARD_GAP")
	&& !subtitleHudSource.includes("subtitleLine("), "The separated bottom-center subtitle card HUD is incomplete");
check(subtitleHudSource.includes("MAX_WIDTH_RATIO = 0.60F")
	&& subtitleHudSource.includes("font.split(speaker, textWidth)")
	&& subtitleHudSource.includes("font.split(transcript, textWidth)")
	&& subtitleHudSource.includes("cardHeight(font.lineHeight, speakerLines.size(), transcriptLines.size())")
	&& subtitleHudSource.includes("for (FormattedCharSequence line : layout.speakerLines())")
	&& subtitleHudSource.includes("for (FormattedCharSequence line : layout.transcriptLines())")
	&& subtitleHudSource.includes("firstCardBottom(graphics.guiHeight(), layout.height())"),
"Subtitle cards can exceed the GUI or truncate wrapped lines");
check(subtitleHudSource.includes("subtitle.frameStartNanos()")
	&& subtitleHudSource.includes("subtitle.frameEndNanos()")
	&& subtitleHudSource.includes("FADE_IN_NANOS = 100_000_000L")
	&& subtitleHudSource.includes("FADE_OUT_NANOS = 150_000_000L")
	&& subtitleHudSource.includes("applyOpacity(BACKGROUND_COLOR, opacity)")
	&& subtitleHudSource.includes("applyOpacity(SPEAKER_COLOR, opacity)")
	&& subtitleHudSource.includes("applyOpacity(TRANSCRIPT_COLOR, opacity)"),
"The subtitle HUD fade is not derived from the existing frame lifetime");
const serverReceiver = clientSource.indexOf("ClientPlayNetworking.registerGlobalReceiver(DialogueAnimationPayload.TYPE");
const serverSound = clientSource.indexOf("DialogueSoundState.start(payload);", serverReceiver);
const serverAnimation = clientSource.indexOf("DialogueAnimationState.start(payload);", serverSound);
const serverSubtitle = clientSource.indexOf("DialogueSubtitleState.start(payload);", serverAnimation);
const fallbackPayload = clientOnlySource.indexOf("DialogueAnimationPayload payload =");
const fallbackSound = clientOnlySource.indexOf("DialogueSoundState.start(payload);", fallbackPayload);
const fallbackAnimation = clientOnlySource.indexOf("DialogueAnimationState.start(payload);", fallbackSound);
const fallbackSubtitle = clientOnlySource.indexOf("DialogueSubtitleState.start(payload);", fallbackAnimation);
check(serverReceiver >= 0 && serverSound > serverReceiver && serverAnimation > serverSound && serverSubtitle > serverAnimation
	&& fallbackPayload >= 0 && fallbackSound > fallbackPayload && fallbackAnimation > fallbackSound
	&& fallbackSubtitle > fallbackAnimation
	&& !clientOnlySource.includes("HudElementRegistry")
	&& subtitleHudSource.includes("DialogueSubtitleState.visible(minecraft, now)"),
"Server-backed and client-only dialogues do not share one subtitle pipeline");
check(subtitleSource.includes("usesSpeakerSnapshot(active.persistsAfterDeath(), speakerPresent, speakerAlive)")
	&& subtitleSource.includes("useSnapshot ? active.position() : entity.position()")
	&& subtitleSource.includes("useSnapshot ? active.speakerName() : speakerName(entity)")
	&& subtitleSource.includes("return frameAt(subtitles, startNanos, now)")
	&& subtitleSource.includes("DialogueSubtitleState.frameEndNanos(subtitles, startNanos, endNanos, frame)"),
"Subtitle segment timing or death speaker snapshots are not preserved");
const localizedSpeakers = {
  mayor: "Мэр",
  testificate_man: "Тестификат-мэн",
  number_5: "Житель №5",
  number_9: "Житель №9",
  unreachable: "Неприкасаемый житель",
  wooly: "Вулли"
};
check(Object.entries(localizedSpeakers).every(([speaker, name]) =>
    language[`speaker.villager-news-addon-port.${speaker}`]
    && russianLanguage[`speaker.villager-news-addon-port.${speaker}`] === name)
  && subtitleSource.includes("SpecialSpeakerNames.villagerKey(customName.getString())")
  && subtitleSource.includes("SpecialSpeakerNames.sheepKey(customName.getString())")
  && subtitleSource.includes("return Component.translatable(key)")
  && !subtitleSource.includes("setCustomName(")
  && specialSpeakerNamesSource.includes('case "wooly", "wooly the sheep"')
  && specialSpeakerNamesSource.includes('case "villager #9", "villager number 9"'),
"Special speaker names are not translated exclusively in the subtitle presentation layer");
check(animations.gestures.length === 46, `Expected 46 dialogue gestures, found ${animations.gestures.length}`);
check(animations.locomotion?.duration === 0.4375
  && Object.keys(animations.locomotion.tracks).length === 14
  && animations.locomotion.tracks.left_leg_rx
  && animations.locomotion.tracks.left_leg_ty
  && animations.locomotion.tracks.right_leg_rx
  && animations.locomotion.tracks.right_leg_ty,
"The original Bedrock walking animation is incomplete");
check(animations.idles?.length === 6 && animations.idles.every((idle) => idle.duration > 0
  && Object.keys(idle.tracks).length > 0), "The six original Bedrock idle animations are incomplete");
check(animations.continuousIdle === "animation.oreville_vn.fyqjnp"
  && animations.targetLook === "animation.oreville_vn.vqhynx", "The continuous idle and target-look layers are missing");
check(animations.turnLeft === "animation.oreville_vn.aiqbsm"
	&& animations.turnRight === "animation.oreville_vn.pypqgk"
	&& animationStateSource.includes("TURN_STATES")
	&& animationStateSource.includes("legRotation(time")
	&& animationStateSource.includes("legLift(time"),
"The original left-turn and right-turn animation controller is missing");
check(animationStateSource.includes("walkAnimation.position(partialTick)")
  && animationStateSource.includes("IDLE_STATES")
	&& animationStateSource.includes("horizontalDistanceSqr() > 0.0001")
	&& animationStateSource.includes("startNext(tick, -1)")
	&& animationStateSource.includes("blendFromIndex")
	&& animationStateSource.includes("getGameTimeDeltaPartialTick(true)")
	&& animationStateSource.includes("IDLE_BLEND_SECONDS")
	&& animationStateSource.includes("idle.update(age, canIdle)")
	&& animationStateSource.includes("poseWeightAt(active.elapsedSeconds())")
	&& animationStateSource.includes("LOOK_STATES")
	&& animationStateSource.includes("Mth.wrapDegrees(targetYaw - yaw)")
	&& animationStateSource.includes("previous.poseSnapshot()")
	&& animationStateSource.includes("active.transition(variableName, result)")
	&& animationStateSource.includes("EMPTY_TIMELINE")
	&& animationStateSource.includes("if (active) advance(tick)")
	&& animationStateSource.includes("locomotion.valueAt"), "The client does not continuously and smoothly play locomotion and stationary idle tracks");
	check(generatorSource.includes("torad(vnap_look_pitch*0.5)")
		&& generatorSource.includes("torad(vnap_look_yaw*0.77)")
		&& generatorSource.includes("max(-0.45,min(0.45,vnap_look_yaw/60))*-1")
		&& generatorSource.includes("max(-0.45,min(0.45,vnap_look_pitch/60))")
		&& !generatorSource.includes("lookEyeScale")
		&& !generatorSource.includes('rotationTerms.push("torad(head_pitch*0.5)")')
		&& !generatorSource.includes('rotationTerms.push("torad(head_yaw*0.77)")'),
	"The villager rig still applies unsmoothed vanilla look rotations");
check(behaviorSource.includes("easedRotation(mob.yBodyRot")
	&& behaviorSource.includes("easedRotation(mob.getYHeadRot()")
	&& behaviorSource.includes("easedRotation(mob.getXRot()")
	&& behaviorSource.includes("distance * proportion, 0.2F, maximumStep"),
"Dialogue participants snap into vanilla-style subject-facing rotations");

const referencedGroups = groups.filter(([id, group]) => behaviorSource.includes(`"${id}"`)
  || (group.title && behaviorSource.includes(`"${group.title}"`)));
const unreferencedGroups = groups.filter((entry) => !referencedGroups.includes(entry));
check(referencedGroups.length === groups.length, `Expected all 523 server-triggered dialogue groups, found ${referencedGroups.length}`);
check(unreferencedGroups.length === 0, `Found ${unreferencedGroups.length} dialogue groups without Java triggers`);
check(/^dialogue_test_command=(true|false)$/m.test(gradleProperties)
  && buildSource.includes('filesMatching("villager-news-addon-port-build.properties")')
  && buildSettingsResource.includes("dialogue_test_command=${dialogue_test_command}")
  && buildSettingsSource.includes('getProperty("dialogue_test_command", "false")')
  && initializerSource.includes("if (VillagerNewsBuildSettings.dialogueTestCommand()) DialogueTestCommand.register()"),
"The dialogue test command is not guarded by the disabled-by-default build setting");
check(dialogueTestSource.includes('Commands.literal("dialoguetest")')
	&& dialogueTestSource.includes("IntegerArgumentType.integer(1, DialogueCatalog.groups().size())")
	&& dialogueTestSource.includes("new ArrayList<>(DialogueCatalog.groups().values())")
	&& dialogueTestSource.includes("CLIENT_TRACKING_DELAY")
	&& dialogueTestSource.includes("playTestDialogue")
	&& dialogueTestSource.includes("removeSession")
	&& dialogueTestSource.includes("entity.discard()"),
"The numbered dialogue test command lifecycle is incomplete");
check(dialogueTestSource.includes('Commands.literal("continuous")')
	&& dialogueTestSource.includes("CONTINUOUS_GAP = 20L")
	&& dialogueTestSource.includes("variantOffset + 1 < group.variants().size()")
	&& dialogueTestSource.includes("run.number = number + 1")
	&& dialogueTestSource.includes("session.variant.index()")
	&& dialogueTestSource.includes('Component.literal("[Dialogue "')
	&& dialogueTestSource.includes("Continuous dialogue test complete"),
"The continuous dialogue test sequence is incomplete");
check(soundStateSource.includes("PENDING_TIMEOUT_NANOS")
	&& soundStateSource.includes("tryStart(minecraft, payload)")
	&& soundStateSource.includes("PendingSound")
	&& soundStateSource.includes("!entity.isSilent()"),
"Dialogue audio is discarded before newly spawned test actors reach the client");
check(dialogueTestSource.includes('case "mayor" -> "The Mayor"')
  && dialogueTestSource.includes('case "testificate_man" -> "Testificate Man"')
  && dialogueTestSource.includes('case "number_5" -> "Villager #5"')
  && dialogueTestSource.includes('case "number_9" -> "Villager #9"')
  && dialogueTestSource.includes("requiresBabySpeaker")
	&& dialogueTestSource.includes("setProfession")
	&& dialogueTestSource.includes("createSubject"),
"Dialogue tests do not reproduce speaker and subject context");
check(dialogueTestSource.includes("isCosmeticRecipientDialogue(group.id())")
	&& dialogueTestSource.includes('id.equals("cxeziv")')
	&& dialogueTestSource.includes('id.equals("bygaxwbayahw")')
	&& dialogueTestSource.includes('id.equals("bygaxwfobzlt")')
	&& dialogueTestSource.includes('id.equals("ckniqq")')
	&& dialogueTestSource.includes("specialSubjectName(title) != null")
	&& !dialogueTestSource.includes('title.startsWith("Meet ")'),
"Dialogue tests confuse cosmetic or no-nose speakers with their subjects");
check(dialogueTestSource.includes('group.id().equals("qffeco")')
	&& dialogueTestSource.includes('createEntity(level, "iron_golem")')
	&& !dialogueTestSource.includes('Map.entry("Iron Golem Targets the Player", "iron_golem")'),
"The iron-golem attack test confuses the attacker with the spoken-to player");
check((behaviorSource.match(/entity\.entityTags\(\)\.contains\(DIALOGUE_TEST_TAG\)/g) ?? []).length >= 4
  && behaviorSource.includes("!entity.entityTags().contains(DIALOGUE_TEST_TAG)"),
"Dialogue test actors can be interrupted or enter normal dialogue selection");
check(behaviorSource.includes("EntitySpawnReason.SPAWN_ITEM_USE"), "Spawn-egg dialogue does not use the server spawn reason");
check(behaviorSource.includes("maintainSpeechTargets"), "Server-side subject facing is missing");
check(behaviorSource.includes("NEARBY_SUBJECT_RANGE = 8.0")
	&& behaviorSource.includes("speaker.distanceToSqr(entity) <= NEARBY_SUBJECT_RANGE * NEARBY_SUBJECT_RANGE")
	&& behaviorSource.includes("filter(speaker::hasLineOfSight)")
	&& behaviorSource.includes("if (speech.lockMovement) holdMob(mob, position)")
	&& behaviorSource.includes("else faceMob(mob, position)"),
"Nearby observations can select distant or hidden subjects, or mobile dialogue loses subject tracking");
check(behaviorSource.includes("mob.getNavigation().stop()")
	&& behaviorSource.includes("mob.setYBodyRot(bodyYaw)")
	&& behaviorSource.includes("mob.setYHeadRot(headYaw)")
	&& behaviorSource.includes("mob.setXRot(easedRotation")
  && behaviorSource.includes("holdListener"), "Bedrock speaking movement and mutual-facing locks are incomplete");
check(behaviorSource.includes("reputation < -225")
  && behaviorSource.includes("reputation < -75")
  && behaviorSource.includes("reputation >= 75")
  && behaviorSource.includes("reputation >= 25"), "Bedrock reputation tiers are not preserved");
check(behaviorSource.includes("negativeGossip")
  && behaviorSource.includes("isNegativeReputation(first, player)"), "Player-directed gossip is not gated by bad reputation");
check(behaviorSource.includes("RECENT_VARIANTS")
  && behaviorSource.includes("Set.copyOf(recentVariants)"), "Dialogue variants can immediately repeat");
check(behaviorSource.includes("playHurtWitness(entity)")
	&& behaviorSource.includes('playSharedId(witness, "pkvhpv"')
	&& behaviorSource.includes('playSharedId(witness, "pmqrpb"')
	&& behaviorSource.includes("entity instanceof WanderingTrader")
	&& behaviorSource.includes("entity instanceof Sheep sheep && isWooly(sheep)")
	&& !behaviorSource.includes("cast(witness) == CastProfile.VILLAGER"),
"Special and regular villagers cannot react when another villager-like entity is hurt or dies");
check(behaviorSource.includes("playDamageDialogue(villager, dialogue")
	&& behaviorSource.includes("isPlaying(speaker, id) || !ready(cooldownKey, cooldown)")
	&& behaviorSource.includes("source.getEntity() == null ? SHORT_COOLDOWN")
	&& behaviorSource.includes("new ActiveSound(group.id()")
	&& behaviorSource.includes("sound.groupId.equals(groupId)")
	&& behaviorSource.includes("DAMAGE_LOCK_DIALOGUES")
	&& behaviorSource.includes("hasDamageLock(entity)")
	&& ["elryje", "onindz", "rogpvp", "etkxko", "igebly", "vnaodx"]
		.every((id) => behaviorSource.includes(`"${id}"`)),
"Repeated damage interrupts and restarts the same active hurt dialogue");
check(behaviorSource.includes("COSMETIC_RECIPIENT_DIALOGUES")
	&& behaviorSource.includes('case 2 -> "cxeziv"')
	&& behaviorSource.includes('case 3 -> "riezum"')
	&& behaviorSource.includes('case 4 -> "rlkdqd"')
	&& !behaviorSource.includes("CastProfile expected = switch (cosmetic)"),
"Cosmetic recipient dialogue is assigned to the nearby special villager");
check(behaviorSource.includes('NUMBER_5("xccwah", "legnsy", "sclaoa", "behifz", "behifz")')
  && behaviorSource.includes('TESTIFICATE_MAN("nmwmrz", "luoibc", "mpbnsm", "fzoqwd", "fzoqwd")'),
"Special villagers use incompatible regular-villager attack dialogue");
check(behaviorSource.includes("MOBILE_DIALOGUES")
  && behaviorSource.includes("!MOBILE_DIALOGUES.contains(group.id())")
  && behaviorSource.includes('"uzdxum"') && behaviorSource.includes('"behifz"'),
"Damage dialogue still removes knockback or prevents fleeing");
check(catalog.groups.cmkesu?.speaker === "mayor"
	&& generatorSource.includes('"cmkesu"')
	&& behaviorSource.includes("profile == CastProfile.MAYOR) id = \"cmkesu\""),
"The Mayor-hat observation is assigned to the wrong speaker");
check(behaviorSource.includes('Map.entry("armor_stand", "ckniqq")')
	&& behaviorSource.includes('Map.entry("cave_spider", "gtmfpl")')
	&& behaviorSource.includes("playSharedId")
	&& behaviorSource.includes("sharedAdult && sharedVillagerVoice")
	&& behaviorSource.includes('DialogueCatalog.byTitle(title, "villager")')
	&& behaviorSource.includes('new PendingSpeech(level, parent.getUUID(), "fbuabj", villager.getUUID(), ticks + 2L, true)')
	&& behaviorSource.includes('playSharedId(villager, "dfdkli"')
	&& behaviorSource.includes('playSharedId(villager, "ikrwzy"')
	&& behaviorSource.includes('playSharedId(witness, "slbqfwswxeva"')
	&& behaviorSource.includes('playSharedId(speaker, id, "time_skip:"')
	&& behaviorSource.includes('playSharedId(speaker, id, "difficulty:"'),
"Nearby-entity contexts are incomplete or blocked for special adult villagers");
check(behaviorSource.includes("playIronGolemAttackWitness(entity, source)")
	&& behaviorSource.includes('hurtType.equals("iron_golem")')
	&& behaviorSource.includes('attackerType.equals("iron_golem")')
	&& behaviorSource.includes('playSharedId(witness, "qffeco"')
	&& behaviorSource.includes("target = player")
	&& behaviorSource.includes('direct.equals("snowball")')
	&& behaviorSource.includes('direct.equals("falling_block")'),
"Damage observers or projectile and falling-block reactions have incorrect subjects");
check(behaviorSource.includes("entity instanceof WanderingTrader trader")
	&& behaviorSource.includes('attacker instanceof Player ? "vevdkl" : "wyvzhk"')
	&& behaviorSource.includes("cast(villager) == CastProfile.UNREACHABLE")
	&& behaviorSource.includes("speaker instanceof WanderingTrader")
	&& behaviorSource.includes("sharedAdult ? playSharedId"),
"Wandering Trader or Villager Unreachable hurt dialogue is rejected by speaker validation");
check(behaviorSource.includes('playSharedId(witness, id, "player_death:"')
	&& behaviorSource.includes('playSharedTitle(adult, "Stare at a Villager"')
	&& behaviorSource.includes('playSharedTitle(adult, "Stand Completely Still"')
	&& behaviorSource.includes("playSharedTitle(adult, playerContext")
	&& behaviorSource.includes('return "Stand on a Villager\'s Bed";'),
"Shared player observers are blocked for named adult villagers or use the wrong actor");
check(behaviorSource.includes('playSharedId(villagers.getFirst(), "kzemrz"')
	&& behaviorSource.includes('playSharedId(adult, "pbmrxx"')
	&& behaviorSource.includes("Villager gatheringSpeaker = villagers.stream()")
	&& behaviorSource.includes("!villager.isBaby() && !villager.isSleeping()"),
"Named villagers block crowd, baby, or difficulty observations");
check(behaviorSource.includes("droppedItems.size() >= 5"), "Dropped-item pile dialogue does not require a real pile");
check(clientSource.includes("DialogueSoundState.start(payload)")
  && clientSource.includes("DialogueSoundState.tick(client)")
  && soundStateSource.includes("EntityBoundSoundInstance")
  && soundStateSource.includes("getSoundManager().stop(active.instance())")
  && !behaviorSource.includes("speaker.getX(), speaker.getY(), speaker.getZ(), variant.sound()"),
"Dialogue sounds are not bound to and stopped for their exact speaker");
check(soundStateSource.includes('!payload.groupId().equals("hivgme")')
  && soundStateSource.includes('!payload.groupId().equals("ecslqo")'), "Villager death dialogue still stops with its dying entity");
check(behaviorSource.includes("BABY_DIALOGUES")
  && behaviorSource.includes("matchesSpeaker(speaker, group)")
  && behaviorSource.includes('if (villager.isBaby()) playId(villager, "ecslqo"')
  && behaviorSource.includes('else playSharedId(villager, "hivgme"')
  && behaviorSource.includes('if (villager.isBaby()) return BABY_DIALOGUES.contains(group.id())')
  && behaviorSource.includes('"cxeziv", "riezum", "rlkdqd"'),
"Baby villagers can speak adult dialogue or use the adult death voice");
check(villagerSoundSource.includes("vnap$removeVanillaHurtSound")
	&& villagerSoundSource.includes("level().isClientSide()")
	&& villagerSoundSource.includes("cir.setReturnValue(SoundEvents.EMPTY)"), "Vanilla villager hurt sounds can overlap dialogue or be suppressed client-side");
check(villagerSoundSource.includes("vnap$removeVanillaAmbientSound")
  && abstractVillagerSource.includes("vnap$removeVanillaTradeSound")
  && abstractVillagerSource.includes("vnap$removeVanillaTradeUpdatedSound")
	&& abstractVillagerSource.includes("vnap$removeVanillaCelebrateSound")
	&& abstractVillagerSource.includes("level().isClientSide()")
	&& sheepSoundSource.includes("vnap$isServerWooly")
	&& wanderingTraderSoundSource.includes("vnap$isLogicalServer"), "Vanilla entity voices are not suppressed exclusively by the logical server");
check(clientSource.includes("ServerCompatibilityState::detectServerSupport")
	&& clientSource.includes("ServerCompatibilityState.markServerModPresent()")
	&& compatibilitySource.includes("ClientPlayNetworking.canSend(VillagerNewsSettingsPayload.TYPE)")
	&& compatibilitySource.includes("Support.ABSENT"), "Client/server compatibility mode is not detected on connection");
check(clientSource.includes("ClientOnlyDialogueController.register()")
	&& clientSource.includes("ClientOnlyDialogueController.tick(client)")
	&& clientSource.includes("ClientOnlyDialogueController.clear(client)")
	&& clientOnlySource.includes("ServerCompatibilityState.clientOnlyFallback()")
	&& clientOnlySource.includes("DialogueSoundState.start(payload)")
	&& clientOnlySource.includes("DialogueAnimationState.start(payload)")
	&& clientOnlySource.includes("DialogueSubtitleState.start(payload)")
	&& clientOnlySource.includes("ClientPlayerBlockBreakEvents.AFTER.register")
	&& clientOnlySource.includes("PENDING_PLACEMENTS.put")
	&& clientOnlySource.includes("PENDING_BREAKS.put")
	&& !clientOnlySource.includes("ClientPlayNetworking.send")
	&& !clientOnlySource.includes("villager.getOffers()")
	&& clientOnlySource.includes("menu.getOffers()")
	&& compatibilitySource.includes("VillagerNewsSettingsState.activateClientOnly()")
	&& settingsStateSource.includes("!ServerCompatibilityState.clientOnlyFallback()")
	&& clientMixinConfiguration.includes("ClientBlockItemMixin")
	&& existsSync(join(root, "src/main/java/com/vnap/mixin/client/ClientBlockItemMixin.java")),
"Unsupported servers do not activate an isolated client-only dialogue engine");
check(!clientSource.includes("ClientOnlyVanillaSoundGate.tick(client)")
	&& !clientSource.includes("ClientOnlyVanillaSoundGate.clear()")
	&& clientOnlySoundGateSource.includes("ServerCompatibilityState.clientOnlyFallback()")
	&& clientOnlySoundGateSource.includes("VillagerNewsSettings.dialogueEnabled()")
	&& clientOnlySoundGateSource.includes("isCharacterVoice(entity, sound.value())")
	&& clientOnlySoundGateSource.includes("SoundEvents.VILLAGER_AMBIENT")
	&& clientOnlySoundGateSource.includes("SoundEvents.WANDERING_TRADER_AMBIENT")
	&& clientOnlySoundGateSource.includes("SoundEvents.SHEEP_AMBIENT")
	&& !clientOnlySoundGateSource.includes("DELAY_NANOS")
	&& !clientOnlySoundGateSource.includes("playSeededSound")
	&& !soundStateSource.includes("hasActiveOrPending(UUID id)")
	&& clientPacketListenerMixinSource.includes("ClientOnlyVanillaSoundGate.shouldSuppress")
	&& clientPacketListenerMixinSource.includes("ClientboundSoundPacket")
	&& clientPacketListenerMixinSource.includes('method = "handleSoundEvent"')
	&& clientPacketListenerMixinSource.includes("handleSoundEntityEvent")
	&& clientOnlySoundGateSource.includes("getEntitiesOfClass(Sheep.class")
	&& clientMixinConfiguration.includes("ClientPacketListenerMixin"),
"Client-only dialogue does not suppress vanilla character voices");
check(animationStateSource.includes("ACTIVE.entrySet().removeIf")
  && soundStateSource.includes("ACTIVE.entrySet().iterator()"), "Expired client dialogue state is not cleaned up");
check((behaviorSource.match(/tickRateManager\(\)\.runsNormally\(\)/g) ?? []).length >= 2
  && behaviorSource.includes("stopActiveDialogue(server)"), "Dialogue is not paused and stopped by /tick freeze");
check(behaviorSource.includes("!VillagerNewsSettings.dialogueEnabled()")
  && settingsStateSource.includes("getConnection() != null"), "Muting dialogue is not handled safely");
check((behaviorSource.match(/!villager\.isSleeping\(\)/g) ?? []).length >= 5
	&& behaviorSource.includes("if (sleeping)")
	&& behaviorSource.includes('villager.isSleeping() && !group.id().equals("asqzby")'), "Sleeping villagers still react through normal observer paths");
check(behaviorSource.includes("delayVillagerSleep")
  && behaviorSource.includes("processPendingSleep")
  && behaviorSource.includes("PENDING_SLEEP.remove(villager.getUUID())")
  && existsSync(join(root, "src/main/java/com/vnap/mixin/VillagerSleepMixin.java"))
  && mixinConfiguration.includes("VillagerSleepMixin"), "Villager sleep dialogue timing and interruption are incomplete");
check(behaviorSource.includes("updatedVillagers.add(villager.getUUID())")
  && behaviorSource.includes("checkedPairs.add(pair)"), "Nearby multiplayer scans still repeat villager and pair work");
check(behaviorSource.includes("tryCreateNaturalSpecial"), "Natural special-character spawning is missing");
check(!behaviorSource.includes("InteractionResult.FAIL"), "Dialogue hooks still reject vanilla trading interactions");
check(existsSync(join(root, "src/main/java/com/vnap/mixin/AbstractVillagerMixin.java")), "Trade completion mixin is missing");
check(existsSync(join(root, "src/main/java/com/vnap/mixin/VillagerDataMixin.java")), "Villager cosmetic state mixin is missing");
check(itemSource.includes("FabricCreativeModeTab.builder()"), "Villager News creative tab is missing");
check(language["itemGroup.villager-news-addon-port.items"] === "Villager News", "Villager News creative tab name is missing");
check(handbook.categories.length === 12, `Expected 12 handbook trigger categories, found ${handbook.categories.length}`);
check(handbook.categories.flatMap((category) => category.sections).length === 62, "The handbook section hierarchy is incomplete");
check(Object.keys(handbook.contexts).length === 491, "The handbook is missing documented add-on contexts");
check(handbook.overview.length === 12 && handbook.specialVillagers.length === 6
  && handbook.cosmetics.length === 6 && handbook.generalInformation.length === 8,
"The handbook guide pages do not match the add-on");
check(handbook.categories.flatMap((category) => category.sections)
  .find((section) => section.id === "whrsem")?.entries.length === 3,
"The handbook is missing the original real-world day guide");
const handbookTextKeys = [handbook.headline, handbook.guideIntro, handbook.support];
for (const property of ["overview", "specialVillagers", "cosmetics", "generalInformation", "socials", "settings"]) {
	for (const entry of handbook[property]) handbookTextKeys.push(entry.title, entry.body);
}
for (const entry of Object.values(handbook.contexts)) handbookTextKeys.push(entry.title, entry.browseTitle, entry.body);
for (const category of handbook.categories) {
	handbookTextKeys.push(category.title);
	for (const section of category.sections) {
		handbookTextKeys.push(section.title);
		for (const entry of section.entries) handbookTextKeys.push(entry.title, entry.body);
	}
}
check(handbookTextKeys.length === 1634
	&& new Set(handbookTextKeys).size === handbookTextKeys.length
	&& handbookTextKeys.every((key) => key.startsWith("handbook.villager-news-addon-port.")
		&& typeof language[key] === "string" && language[key].length > 0),
"The handbook does not use a complete set of stable English translation keys");
const russianHandbookKeys = Object.keys(russianLanguage).filter((key) => key.startsWith("handbook.villager-news-addon-port."));
const untranslatedRussianHandbookKeys = new Set([
	"handbook.villager-news-addon-port.socials.0.title",
	"handbook.villager-news-addon-port.socials.1.title",
]);
const formattingCodes = (value) => value.match(/§[0-9a-fklmnor]/giu) ?? [];
check(russianHandbookKeys.length === 1634
	&& russianHandbookKeys.every((key) => handbookTextKeys.includes(key))
	&& handbookTextKeys.every((key) => typeof russianLanguage[key] === "string"
		&& russianLanguage[key].trim().length > 0
		&& (russianLanguage[key] !== language[key] || untranslatedRussianHandbookKeys.has(key))
		&& (russianLanguage[key].match(/\n/g) ?? []).length === (language[key].match(/\n/g) ?? []).length
		&& JSON.stringify(formattingCodes(russianLanguage[key])) === JSON.stringify(formattingCodes(language[key])))
	&& handbookTextKeys.filter((key) => /[А-Яа-яЁё]/u.test(russianLanguage[key])).length === 1632,
"The Russian handbook translation is incomplete or has damaged formatting");
check(Object.entries(russianLanguage)
	.filter(([key]) => key.startsWith("handbook.villager-news-addon-port.context.") && key.endsWith(".body"))
	.every(([, value]) => /^§eУсловие\n\n§7.+\n\n§eРеакция\n\n§7.+$/su.test(value))
	&& !russianHandbookKeys.some((key) => /(?:[\uE000-\uF8FF]|Villager #[59]|Testificate Man|\bWooly\b|§eTrigger|§eReaction)/u.test(russianLanguage[key])),
"The Russian trigger guide contains untranslated text, damaged markers, or inconsistent structure");
const subtitlePrefix = "subtitles.villager-news-addon-port.dialogue.";
const englishSubtitleKeys = Object.keys(language).filter((key) => key.startsWith(subtitlePrefix));
const russianSubtitleKeys = Object.keys(russianLanguage).filter((key) => key.startsWith(subtitlePrefix));
const translatedRussianSubtitleGroupIds = groups.map(([id]) => id);
const translatedRussianSubtitleGroupIdSet = new Set(translatedRussianSubtitleGroupIds);
const expectedRussianSubtitleKeys = englishSubtitleKeys.filter((key) => translatedRussianSubtitleGroupIdSet.has(key.split(".")[3]));
const allowedLatinRussianSubtitleKeys = new Set([
	"subtitles.villager-news-addon-port.dialogue.armupg.0.1",
	"subtitles.villager-news-addon-port.dialogue.lvigit.18.1",
	"subtitles.villager-news-addon-port.dialogue.adhvqz.2.2",
]);
const allowedUnchangedRussianSubtitleKeys = new Set([
	"subtitles.villager-news-addon-port.dialogue.lvigit.18.1",
	"subtitles.villager-news-addon-port.dialogue.fzoqwd.2.1",
]);
const englishSubtitleCountsByGroup = new Map();
const russianSubtitleCountsByGroup = new Map();
for (const key of englishSubtitleKeys) {
	const id = key.split(".")[3];
	englishSubtitleCountsByGroup.set(id, (englishSubtitleCountsByGroup.get(id) ?? 0) + 1);
}
for (const key of russianSubtitleKeys) {
	const id = key.split(".")[3];
	russianSubtitleCountsByGroup.set(id, (russianSubtitleCountsByGroup.get(id) ?? 0) + 1);
}
check(translatedRussianSubtitleGroupIds.at(-1) === "bygaxwmwtiaf"
	&& expectedRussianSubtitleKeys.length === 3741
	&& russianSubtitleKeys.length === expectedRussianSubtitleKeys.length
	&& expectedRussianSubtitleKeys.every((key) => Object.hasOwn(russianLanguage, key))
	&& russianSubtitleKeys.every((key) => Object.hasOwn(language, key))
	&& [...russianSubtitleCountsByGroup].every(([id, count]) => count === englishSubtitleCountsByGroup.get(id)),
"The completed Russian subtitle batches are incomplete or cross a dialogue-group boundary");
check(russianSubtitleKeys.every((key) => typeof russianLanguage[key] === "string"
		&& russianLanguage[key].trim().length > 0
		&& !/^[,;]/u.test(russianLanguage[key])
		&& !/[\uE000-\uF8FF]/u.test(russianLanguage[key])
		&& (!/[A-Za-z]/u.test(russianLanguage[key]) || allowedLatinRussianSubtitleKeys.has(key))
		&& (russianLanguage[key] !== language[key] || allowedUnchangedRussianSubtitleKeys.has(key)))
	&& russianLanguage["subtitles.villager-news-addon-port.dialogue.xfpjxq.0.1"] === "Тестификат-мэне?"
	&& russianLanguage["subtitles.villager-news-addon-port.dialogue.lvigit.18.1"] === "Villager News!"
	&& russianLanguage["subtitles.villager-news-addon-port.dialogue.vxycol.2.0"] === "Вулли! Это ты?"
	&& russianLanguage["subtitles.villager-news-addon-port.dialogue.gcoysc.4.0"] === "Кадавр, кажется?"
	&& russianLanguage["subtitles.villager-news-addon-port.dialogue.nwlcij.2.0"] === "Скрипун!"
	&& russianLanguage["subtitles.villager-news-addon-port.dialogue.jicosq.2.0"] === "Тише! Это Хранитель!"
	&& russianLanguage["subtitles.villager-news-addon-port.dialogue.satsrf.3.0"] === "О нет, визер!"
	&& russianLanguage["subtitles.villager-news-addon-port.dialogue.rnlher.1.0"] === "Тихоня!"
	&& russianLanguage["subtitles.villager-news-addon-port.dialogue.tqishj.2.0"] === "Эй, нюхач!"
	&& russianLanguage["subtitles.villager-news-addon-port.dialogue.ididel.2.1"] === "Нижний мир? Нарочно?"
	&& russianLanguage["subtitles.villager-news-addon-port.dialogue.lxvofx.0.0"] === "Счастливый гаст!"
	&& russianLanguage["subtitles.villager-news-addon-port.dialogue.rdugrl.2.1"] === "Тестификат-мэном?!"
	&& russianLanguage["subtitles.villager-news-addon-port.dialogue.xccwah.0.2"] === "в деревне. Передаём Жителю №9..."
	&& russianLanguage["subtitles.villager-news-addon-port.dialogue.adhvqz.2.2"] === "Villager News гордится тобой!"
	&& russianLanguage["subtitles.villager-news-addon-port.dialogue.bygaxwmwtiaf.0.2"] === "*Хрмр*",
"The completed Russian subtitle batches contain untranslated text, damaged markers, or inconsistent terminology");
const baseLanguageKeys = Object.keys(language).filter((key) => !key.startsWith("handbook.")
	&& !key.startsWith("subtitles."));
const placeholders = (value) => value.match(/%(?:\d+\$)?[sdf]/g) ?? [];
check(baseLanguageKeys.length === 68
	&& baseLanguageKeys.every((key) => typeof russianLanguage[key] === "string"
		&& russianLanguage[key].trim().length > 0
		&& JSON.stringify(placeholders(russianLanguage[key])) === JSON.stringify(placeholders(language[key])))
	&& Object.keys(russianLanguage).every((key) => Object.hasOwn(language, key))
	&& Object.values(russianLanguage).filter((value) => /[А-Яа-яЁё]/u.test(value)).length >= 54,
"The basic Russian item, guide, settings, and screen localization is incomplete");
check(russianLanguage["guide.villager-news-addon-port.specials"].includes("Mayor Villager")
	&& russianLanguage["guide.villager-news-addon-port.specials"].includes("Testificate Man")
	&& russianLanguage["guide.villager-news-addon-port.specials"].includes("Villager Unreachable")
	&& russianLanguage["guide.villager-news-addon-port.specials"].includes("Wooly The Sheep"),
"The Russian guide does not preserve the exact special-character activation names");
check(handbookSource.includes("screen.villager-news-addon-port.search.placeholder")
	&& handbookSource.includes("I18n.get(key)")
	&& handbookSource.includes("DialogueCatalog") === false
	&& !handbookSource.includes("Search Triggers"),
"The handbook's localized searchable trigger browser is missing or using a reduced catalog");
check(generatorSource.includes('import { localizeHandbook } from "./sync-handbook-language.mjs"')
	&& generatorSource.includes("localizeHandbook(handbook, javaLanguage, modNamespace)")
	&& handbookLocalizationSource.includes("export function localizeHandbook")
	&& handbookLocalizationSource.includes("if (key.startsWith(prefix)) delete language[key]"),
"Regenerating the port does not preserve the handbook localization architecture");
check(clientSource.includes("new HandbookScreen()"), "Using the handbook does not open its client screen");
check(clientSource.includes("if (!level.isClientSide()) return InteractionResult.PASS;"), "The handbook opener can run on the integrated server thread");
check(handbookSource.includes("VillagerNewsSettingsState.setChattiness")
  && handbookSource.includes("VillagerNewsSettingsState.setRareVoicelines")
  && handbookSource.includes("VillagerNewsSettingsState.setSpawnSpecialVillagers")
  && handbookSource.includes("VillagerNewsClientSettings.setSubtitlesEnabled"), "The handbook settings are not interactive");
check(handbookSource.includes("spawnSpecialVillagers.active = canEdit && !localSettings")
  && handbookSource.includes("spawnSpecialVillagers.setTooltip(Tooltip.create(Component.translatable(")
  && settingsStateSource.includes("if (!canEdit || localSettings) return;")
  && language["screen.villager-news-addon-port.settings.spawn_special_villagers.server_only"] === "Server-side only"
  && russianLanguage["screen.villager-news-addon-port.settings.spawn_special_villagers.server_only"] === "Только на сервере"
  && russianLanguage["screen.villager-news-addon-port.settings.spawn_special_villagers.server_only.tooltip"]
    === "Доступно только при установленном моде на сервере.",
"Client-only settings still expose the server-only special-spawning toggle");
check(settingsSource.includes("scaleCooldown") && settingsSource.includes("rareVoicelines")
  && settingsSource.includes("spawnSpecialVillagers"), "The Bedrock settings are not persisted on the server");
check(behaviorSource.includes("VillagerNewsSettings.scaleCooldown")
  && behaviorSource.includes("VillagerNewsSettings.rareVoicelines")
  && behaviorSource.includes("VillagerNewsSettings.spawnSpecialVillagers"), "The server behavior does not apply every supported setting");
check(settingsNetworkSource.includes("Permissions.COMMANDS_GAMEMASTER")
  && settingsNetworkSource.includes("if (!canEdit(context.player()))")
  && settingsNetworkSource.includes("sendAll(context.player().level().getServer())")
  && settingsNetworkSource.includes("server.getPlayerList().getPlayers()")
  && settingsPayloadSource.includes("boolean canEdit")
  && settingsStateSource.includes("if (!canEdit) return")
	&& handbookSource.includes("screen.villager-news-addon-port.settings.scope.permission")
	&& language["screen.villager-news-addon-port.settings.scope.permission"] === "Server dialogue settings require operator permission.",
"Handbook server settings are not permission protected");
check(buildSource.includes('compileOnly "com.terraformersmc:modmenu:${project.modmenu_version}"')
  && /^modmenu_version=20\.0\.2$/m.test(gradleProperties)
  && fabricMod.entrypoints?.modmenu?.includes("com.vnap.client.VillagerNewsModMenu")
  && !fabricMod.depends?.modmenu
  && modMenuSource.includes("implements ModMenuApi")
  && modMenuSource.includes("HandbookScreen::settingsScreen")
  && handbookSource.includes("public static HandbookScreen settingsScreen(Screen parent)")
  && handbookSource.includes("if (settingsOnly)")
  && settingsStateSource.includes("prepareConfigScreen")
  && settingsStateSource.includes("VillagerNewsSettings.update"),
"Optional Mod Menu configuration does not preserve local and server settings behavior");
check(behaviorSource.includes("ServerLifecycleEvents.SERVER_STOPPING")
  && behaviorSource.includes("private static void clearState()")
  && clientSource.includes("ClientPlayConnectionEvents.DISCONNECT")
  && clientSource.includes("DialogueSoundState.clear(client)"), "World shutdown leaves dialogue state active");
check(villagerDataSource.includes("vnap$keepSpecialTradeOpen") && villagerDataSource.includes("isSpecialTrader"),
  "Special villagers still inherit the vanilla unemployed-villager trade closure");
check(villagerDataSource.includes('VillagerTradeBackup.load(input).orElse(null)')
  && villagerDataSource.includes('if (vnap$tradeBackup != null) vnap$tradeBackup.save(output)')
  && villagerDataSource.includes('ContextualDialogueController.ensureSpecialTrade(villager)')
  && tradeBackupSource.includes('backup.store("Data", VillagerData.CODEC, data)')
  && tradeBackupSource.includes('backup.store("Offers", MerchantOffers.CODEC, offers)')
  && tradeBackupSource.includes('villager.setOffers(offers.copy())')
  && behaviorSource.includes('state.vnap$setTradeBackup(VillagerTradeBackup.capture(villager, legacySpecialOnlyOffers))')
  && behaviorSource.includes('backup.restore(villager)'),
"Special-name conversion does not preserve and restore the villager's original trading state");
check(villagerDataSource.includes("VillagerNewsSignMessage")
  && villagerDataSource.includes("VillagerNewsSignType")
  && behaviorSource.includes("state.vnap$setSignType(offeredSign)")
  && behaviorSource.includes("Math.floorMod(state.vnap$signMessage() + direction, 87)"),
"Villagers do not hold, remove, and cycle their Bedrock signs");
check(behaviorSource.includes('equals("firework_rocket")')
	&& behaviorSource.includes('playSharedId(villager, "dfdkli"')
	&& behaviorSource.includes('playId(villager, "zeykfp"'), "Firework spawn reactions are incomplete");
check(behaviorSource.includes("playHomeChestReaction")
  && behaviorSource.includes("MemoryModuleType.HOME")
  && behaviorSource.includes('playId(villager, "qfhrlh"'), "Villager home chest reactions are incomplete");
check(behaviorSource.includes("source.getDirectEntity() == player")
  && behaviorSource.includes("weaponAttackDialogue(player.getMainHandItem())"), "Player attacks can trigger competing dialogue paths");
check(villagerRendererSource.includes("StableVillagerData")
  && villagerRendererSource.includes("tick - pendingSince >= 2")
  && villagerRendererSource.includes("state.villagerData = stableData.resolve"), "Transient profession texture states are not filtered");
check(!villagerModelSource.includes("villager_news_sign_board_")
  && clientSource.includes("LivingEntityRenderLayerRegistrationCallback.EVENT.register")
  && signLayerSource.includes('"textures/block/" + wood + "_sign.png"')
  && !signLayerSource.includes("textures/entity/signs/")
  && signLayerSource.includes("getPositionerForAttachment(EMFAttachment.Type.VILLAGER)")
  && signLayerSource.includes("poseStack.translate(0.0F, 5.75F / 16.0F, -1.75F / 16.0F)")
  && !signLayerSource.includes("poseStack.translate(0.0F, -5.75F / 16.0F")
  && villagerModelSource.includes('"villager_item"')
  && existsSync(join(root, "src/main/java/com/vnap/mixin/client/VillagerRendererMixin.java"))
  && existsSync(join(modAssets, "textures", "entity", "sign_text.png")),
"The original sign board or its 87-message text atlas is missing");
check(Array.isArray(signMessages)
  && signMessages.length === 87
  && signMessages.every((entry, index) => entry.index === index
    && [entry.en, entry.ru].every((lines) => Array.isArray(lines)
      && lines.length >= 1 && lines.length <= 4
      && lines.every((line) => typeof line === "string" && line.trim().length > 0)))
  && signMessages.every((entry) => entry.ru.some((line) => /[А-Яа-яЁё]/u.test(line))
    && !entry.ru.some((line) => /[A-Za-z]/u.test(line)))
  && JSON.stringify(signMessages[23].ru) === JSON.stringify(signMessages[47].ru)
  && JSON.stringify(signMessages[39].ru) === JSON.stringify(signMessages[52].ru)
  && signMessages[59].ru.join(" ").includes("←")
  && signMessages[65].ru.join(" ").includes("→")
  && signMessages[83].ru.join(" ").includes("←")
  && signMessages[85].ru.join(" ").includes("→"),
"The indexed English/Russian sign-message catalog is incomplete or reordered");
const russianSignAtlas = readFileSync(join(modAssets, "textures", "entity", "sign_text_ru_ru.png"));
check(russianSignAtlas.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))
  && russianSignAtlas.readUInt32BE(16) === 96
  && russianSignAtlas.readUInt32BE(20) === 87 * 35
  && signLayerSource.includes('"ru_ru".equals(Minecraft.getInstance().getLanguageManager().getSelected())')
  && signLayerSource.includes("? TEXT_TEXTURE_RU_RU : TEXT_TEXTURE")
  && behaviorSource.includes('Component.translatable("message.villager-news-addon-port.sign_message", message + 1, 87)')
  && language["message.villager-news-addon-port.sign_message"] === "Sign message %s / %s"
  && russianLanguage["message.villager-news-addon-port.sign_message"] === "Надпись таблички %s / %s",
"The Russian sign atlas, locale selection, or message overlay is missing");
check(/"villager_item":\s*\[\s*0,\s*0,\s*0\s*\]/.test(villagerModelSource)
  && villagerModelSource.includes('.visible": "vnap_has_nose==1"'),
"Held items or sheared noses retain the wrong model visibility transform");
check(mixinConfiguration.includes("VillagerSoundMixin")
	&& mixinConfiguration.includes("WanderingTraderSoundMixin")
	&& existsSync(join(root, "src/main/java/com/vnap/mixin/VillagerSoundMixin.java"))
	&& existsSync(join(root, "src/main/java/com/vnap/mixin/WanderingTraderSoundMixin.java")),
"Vanilla villager death sounds are not deterministically suppressed");
check(professionLayerSource.includes("vnap$alignAdultClothingWithEmfModel")
  && professionLayerSource.includes("return layer.getParentModel()"),
"Villager profession clothing is not aligned with the EMF model");
const professionTextures = {
  none: "din",
  armorer: "djv",
  butcher: "djw",
  cartographer: "djx",
  cleric: "djy",
  farmer: "djz",
  fisherman: "dka",
  fletcher: "dkb",
  leatherworker: "dkc",
  librarian: "dkd",
  mason: "dkg",
  nitwit: "dkh",
  shepherd: "dke",
  toolsmith: "djg",
  weaponsmith: "dkf",
};
for (const [profession, texture] of Object.entries(professionTextures)) {
  check(generatorSource.includes(`"profession/${profession}.png": "${texture}"`),
    `${profession} does not use its original Bedrock profession texture`);
  check(existsSync(join(resources, "assets", "minecraft", "textures", "entity", "villager", "profession", `${profession}.png`)),
    `${profession} profession texture was not generated`);
}
const projectVersion = gradleProperties.match(/^version=(.+)$/m)?.[1]?.trim();
check(projectVersion, "The project version is missing from gradle.properties");
check(language["guide.villager-news-addon-port.header"] === `Villager News ${projectVersion}`,
  `The handbook version does not match project version ${projectVersion}`);
const merchantCheck = behaviorSource.indexOf("player.containerMenu instanceof MerchantMenu");
const openingDialogue = behaviorSource.indexOf("trade_open:");
check(merchantCheck >= 0 && openingDialogue > merchantCheck, "Trade opening dialogue still runs before the merchant menu opens");

for (const item of ["handbook", "mayor_hat", "microphone", "moustache", "testificate_man_helmet", "villager_nose"]) {
  check(existsSync(join(modAssets, "items", `${item}.json`)), `Missing client item definition for ${item}`);
  check(existsSync(join(modAssets, "models", "item", `${item}.json`)), `Missing item model for ${item}`);
  check(existsSync(join(modAssets, "textures", "item", `${item}.png`)), `Missing item texture for ${item}`);
}
for (const item of ["mayor_villager_spawn_egg", "testificate_man_spawn_egg", "villager_5_spawn_egg",
  "villager_9_spawn_egg", "untouchable_villager_spawn_egg", "wooly_spawn_egg"]) {
  check(itemSource.includes(item.toUpperCase()), `Missing registered spawn egg ${item}`);
  check(existsSync(join(modAssets, "items", `${item}.json`)), `Missing client item definition for ${item}`);
  check(existsSync(join(modAssets, "models", "item", `${item}.json`)), `Missing item model for ${item}`);
  check(existsSync(join(modAssets, "textures", "item", `${item}.png`)), `Missing original texture for ${item}`);
  check(typeof language[`item.villager-news-addon-port.${item}`] === "string", `Missing item name for ${item}`);
}
check(itemSource.includes("ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE")
  && !itemSource.includes('{\\"text\\":\\"')
  && !itemSource.includes('putByte("Color"'), "Spawn eggs still write malformed names or dye Wooly red");
check(behaviorSource.includes("normalizeSpecialEntity(entity)")
  && behaviorSource.includes("sheep.setColor(DyeColor.WHITE)")
  && !behaviorSource.includes("sheep.setColor(DyeColor.RED)"), "Existing special entities are not repaired on load");
for (const file of readdirSync(join(modAssets, "sounds", "voice")).filter((name) => name.endsWith(".ogg"))) {
  const data = readFileSync(join(modAssets, "sounds", "voice", file));
  let offset = 0;
  while (offset + 27 <= data.length && data.toString("ascii", offset, offset + 4) === "OggS") {
    const segmentCount = data[offset + 26];
    let bodySize = 0;
    for (let index = 0; index < segmentCount; index++) bodySize += data[offset + 27 + index];
    const granule = data.readBigUInt64LE(offset + 6);
    check(!(granule > 0xffffffffn && granule < 0x200000000n), `${file} has a malformed Bedrock OGG granule timestamp`);
    offset += 27 + segmentCount + bodySize;
  }
}
const wearableGeometry = {
  mayor_hat: { elementCount: 8, from: [2.4, 14.4, 2.4], to: [13.6, 16, 13.6], textureSize: [32, 32] },
  moustache: { elementCount: 1, from: [4.8, 4, 0.4], to: [11.2, 5.6, 0.8], textureSize: [16, 16] },
  testificate_man_helmet: { elementCount: 3, from: [0.72, 2.32, 0.72], to: [15.28, 20.08, 15.28], textureSize: [16, 32] },
  villager_nose: { elementCount: 1, from: [6.4, 0, -1.6], to: [9.6, 6.4, 1.6], textureSize: [64, 64] },
};
for (const [item, expected] of Object.entries(wearableGeometry)) {
  const definition = JSON.parse(readFileSync(join(modAssets, "items", `${item}.json`), "utf8"));
  const worn = JSON.parse(readFileSync(join(modAssets, "models", "item", `${item}_worn.json`), "utf8"));
  const textureFile = join(modAssets, "textures", "item", "worn", `${item}.png`);
  const headCase = definition.model?.cases?.find((entry) => entry.when === "head");
  check(definition.model?.type === "minecraft:select"
    && definition.model?.property === "minecraft:display_context"
    && headCase?.model?.model === `villager-news-addon-port:item/${item}_worn`,
  `${item} does not use its worn model on a player head`);
  check(definition.model?.fallback?.model === `villager-news-addon-port:item/${item}`,
    `${item} does not preserve its inventory model`);
  check(worn.elements?.length === expected.elementCount, `${item} has incomplete wearable geometry`);
  check(worn.elements.every((element) => element.from?.length === 3 && element.to?.length === 3
    && Object.keys(element.faces ?? {}).length > 0), `${item} has malformed wearable cubes`);
  check(JSON.stringify(worn.elements[0].from) === JSON.stringify(expected.from)
    && JSON.stringify(worn.elements[0].to) === JSON.stringify(expected.to),
  `${item} is not anchored to the original Bedrock player-head coordinates`);
  check(existsSync(textureFile), `${item} is missing its original wearable texture`);
  const texture = readFileSync(textureFile);
  check(texture.readUInt32BE(16) === expected.textureSize[0]
    && texture.readUInt32BE(20) === expected.textureSize[1], `${item} has an unsafe atlas texture size`);
  const rgba = execFileSync(ffmpeg, [
    "-v", "error", "-i", textureFile, "-f", "rawvideo", "-pix_fmt", "rgba", "-",
  ]);
  let transparentPixels = 0;
  let opaquePixels = 0;
  for (let index = 3; index < rgba.length; index += 4) {
    if (rgba[index] === 0) transparentPixels++;
    if (rgba[index] === 255) opaquePixels++;
  }
  check(transparentPixels > 0 && opaquePixels > 0, `${item} wearable texture lost its alpha channel`);
}
check(existsSync(join(resources, "data", "villager-news-addon-port", "recipe", "handbook.json")), "Handbook recipe is missing");

for (const { file, localScale, armsRest } of [
  { file: "villager.jem", localScale: 1, armsRest: "-0.74997+vnap_arms_rx" },
  { file: "villager_baby.jem", localScale: 3, armsRest: "-1.0472+vnap_arms_rx" },
  { file: "villager2.jem", localScale: 3, armsRest: "-1.0472+vnap_arms_rx" },
  { file: "villager3.jem", localScale: 1, armsRest: "-0.74997+vnap_arms_rx" },
  { file: "villager4.jem", localScale: 1, armsRest: "-0.74997+vnap_arms_rx" },
  { file: "villager5.jem", localScale: 1, armsRest: "-0.74997+vnap_arms_rx" },
  { file: "villager6.jem", localScale: 1, armsRest: "-0.74997+vnap_arms_rx" },
  { file: "wandering_trader.jem", localScale: 1, armsRest: "-0.74997+vnap_arms_rx" },
]) {
  const model = JSON.parse(readFileSync(join(cem, file), "utf8"));
  const flatten = (entries) => entries.flatMap((entry) => [entry, ...flatten(entry.submodels ?? [])]);
  const all = flatten(model.models);
  const base = (bone) => all.find((entry) => entry.id?.endsWith(`_base_${bone}`));
  const rootModel = base("root");
  check(rootModel?.part === "root" && rootModel.attach === true, `${file} does not attach the Bedrock root rig`);
  check(JSON.stringify(rootModel.translate) === "[0,-24,0]", `${file} has the wrong Bedrock-to-Java root offset`);
  for (const part of ["head", "nose", "headwear", "headwear2", "body", "bodywear", "arms", "right_leg", "left_leg"]) {
    const suppressor = model.models.find((entry) => entry.part === part);
    check(suppressor?.attach === false && !suppressor.boxes?.length, `${file} does not suppress the vanilla ${part}`);
  }

  const face = all.filter((entry) => /_(egfg3jgo|egml9|l66l9|d67l_6q6|ja89l_6q6)$/.test(entry.id ?? ""));
  check(face.length >= 5, `${file} is missing animated facial bones`);
  check(face.every((entry) => Math.abs(entry.translate?.[1] ?? 0) < 6 * localScale), `${file} contains a non-local facial pivot`);
  check(face.flatMap((entry) => entry.boxes ?? []).every((box) => Math.abs(box.coordinates?.[1] ?? 0) < 8 * localScale), `${file} contains a world-space facial cube`);

  const topLevel = new Set(model.models);
  check(all.filter((entry) => !topLevel.has(entry)).every((entry) => !entry.animations?.length), `${file} contains nested animations that EMF will not collect`);
  const animationText = JSON.stringify(rootModel.animations ?? []);
  check(animationText.includes("vnap_root_rx"), `${file} root motion is not kept on its authored pivot`);
  check(animationText.includes("vnap_waist_rx"), `${file} waist motion is not kept on its authored pivot`);
  check(animationText.includes("vnap_body_rx"), `${file} body motion is not kept on its authored pivot`);
  check(animationText.includes("vnap_head_rx"), `${file} head motion is not kept on its authored pivot`);
  check(animationText.includes("vnap_head_inner_rx"), `${file} inner-head motion is not kept on its authored pivot`);
  check(animationText.includes(armsRest), `${file} has malformed crossed-arm motion`);
  check(animationText.includes("_egml9.sx") && animationText.includes("vnap_mouth_open"), `${file} mouth animation was not hoisted`);
  check(animationText.includes('_base_egml9.sz":"1"'), `${file} does not show the neutral mouth line at rest`);
  check(animationText.includes('_l66l9lgh.sx":"(0.75+vnap_mouth_width*0.25-vnap_mouth_closed)*vnap_speaking"')
    && animationText.includes('_l66l93gllge.sx":"(0.75+vnap_mouth_width*0.25-vnap_mouth_closed)*vnap_speaking"'),
  `${file} does not resize both rendered teeth strips directly`);
  const toothTravel = localScale === 3 ? "1.5" : "0.75";
  check(animationText.includes('_l66l9lgh.ty":"') && animationText.includes(`vnap_mouth_open*-${toothTravel}`)
    && animationText.includes('_l66l93gllge.ty":"') && animationText.includes(`vnap_mouth_open*${toothTravel}`),
  `${file} does not separate its upper and lower teeth while speaking`);
  check(!animationText.includes('_l66l9.sx"'), `${file} still applies tooth scaling to the empty parent bone`);
  check(animationText.includes("_egfg3jgo.ty") && animationText.includes("vnap_brow_ty"), `${file} brow animation was not hoisted`);
  check(animationText.includes("6q6da5kmhh6j.sy") && animationText.includes("6q6da5kdgo6j.sy") && animationText.includes("2.02"), `${file} does not animate both eyelid halves`);
	check(animationText.includes("vnap_left_leg_rx") && animationText.includes("vnap_right_leg_rx"), `${file} does not animate both upper-leg pivots`);
	check(!animationText.includes("sin(limb_swing"), `${file} still uses the simplified walk instead of the original Bedrock track`);
  check(!animationText.includes("limb_speed*(1-vnap_speaking)"), `${file} freezes its legs while dialogue is playing`);
  check(!animationText.includes("_jggl_leftleg.rx\":\"sin(limb_swing") && !animationText.includes("_jggl_rightleg.rx\":\"sin(limb_swing"), `${file} still walks from the foot pivots`);
  if (file !== "wandering_trader.jem") {
    check(animationText.includes("vnap_has_nose"), `${file} does not respond to synchronized nose state`);
  }
  if (file === "villager.jem" || file === "villager_baby.jem") {
    for (const cosmetic of ["mayor_hat", "helmet", "microphone", "moustache"]) {
      check(JSON.stringify(model).includes(`vnap_cosmetic_${cosmetic}`), `${file} is missing the ${cosmetic} cosmetic`);
    }
    const wearablePrefix = file === "villager.jem" ? "villager_news" : "villager_news_baby";
    const helmetBelt = all.find((entry) => entry.id === `${wearablePrefix}_extra_1_l6kla7a42l636dl`);
    check(helmetBelt?.boxes?.[0]?.sizeAdd === 0.0625, `${file} has a belt coplanar with the villager robe`);
  }

  const head = base("headjgl2l6");
  const nose = base("fgk6");
  const arms = base("2jek");
  const bodywear = base("jg36");
  if (file === "villager3.jem") {
    const testificateBelt = all.find((entry) => entry.id === "testificate_extra_0_l6kla7a42l636dl");
    check(rootModel.texture === "villager-news-addon-port:textures/entity/testificate_man.png",
      "Testificate Man lost his original character texture");
    check(testificateBelt?.boxes?.[0]?.sizeAdd === undefined,
      "The named Testificate Man model was changed with the wearable belt adjustment");
  }
  if (file === "villager2.jem") {
    const mayorExtra = all.find((entry) => entry.id === "mayor_extra_0_lghhat");
    check(head?.boxes?.some((box) => box.coordinates?.slice(3).includes(24)), "Mayor is not using the large baby base rig");
    check(mayorExtra?.boxes?.some((box) => box.coordinates?.slice(3).includes(18)), "Mayor hat geometry is missing");
    check(animationText.includes("0.33333*vnap_root_sx"), "Mayor base rig is not scaled to its Bedrock entity size");
    const extraRoot = model.models.find((entry) => entry.id === "mayor_extra_0_root");
    check(JSON.stringify(extraRoot?.animations ?? []).includes("0.33333*vnap_root_sx"), "Mayor hat does not share the base rig scale");
	  } else if (file === "villager.jem") {
	    const mayorHat = all.find((entry) => entry.id === "villager_news_extra_0_lghhat");
	    const mayorMonocle = all.find((entry) => entry.id === "villager_news_extra_0_egfg4d6");
	    const mayorAnimations = JSON.stringify(model.models.find((entry) => entry.id === "villager_news_extra_0_root")?.animations ?? []);
	    check(animationText.includes('"villager_news_base_hat.visible":"vnap_cosmetic_mayor_hat==0&&vnap_cosmetic_helmet==0"'),
	      "The ordinary villager headwear visibility is not a boolean EMF expression");
	    check(mayorHat?.boxes?.some((box) => box.coordinates?.slice(3).includes(8)), "The villager Mayor hat is using the oversized special-character geometry");
	    check(mayorAnimations.includes('"this.sx":"vnap_cosmetic_mayor_hat"')
	      && mayorAnimations.includes('"villager_news_extra_0_lghhat.sx":0.9')
	      && mayorAnimations.includes('"villager_news_extra_0_lghhat.sz":0.9'),
	    "The wearable Mayor hat is not reduced around its own pivot");
	    check(JSON.stringify(mayorHat?.translate) === "[0,7.01998,0]"
	      && JSON.stringify(mayorHat?.boxes?.map((box) => box.coordinates[1])) === "[4.53002,3.28602]",
	    "The wearable Mayor hat cubes are not lowered onto the head");
	    check(JSON.stringify(mayorMonocle?.translate) === "[-3.27,4.805,-3.526]", "The wearable Mayor monocle is floating in front of the face");
  } else if (file === "villager_baby.jem") {
    check(head?.boxes?.some((box) => box.coordinates?.slice(3).includes(24)), "Baby villager is not using the add-on's large-head rig");
    check(animationText.includes("0.33333*vnap_root_sx") && animationText.includes("0.33333*vnap_root_sy")
      && animationText.includes("0.33333*vnap_root_sz"), "Baby villager does not apply its authored one-third rig scale");
  } else {
    check(JSON.stringify(head?.boxes?.[0]?.coordinates) === "[-4,0,-4,8,10,8]", `${file} has malformed local head geometry`);
    check(JSON.stringify(head?.boxes?.[0]?.uvSouth) === "[24,8,32,18]", `${file} has unconverted Bedrock face UVs`);
    check(JSON.stringify(nose?.translate) === "[0,2.5,-4]", `${file} has a displaced local nose pivot`);
    check(JSON.stringify(nose?.boxes?.[0]?.coordinates) === "[-1,-3.5,-2,2,4,2]", `${file} has malformed local nose geometry`);
    check(JSON.stringify(arms?.translate) === "[0,-3,-1]", `${file} has a displaced local arm pivot`);
    check(JSON.stringify(arms?.boxes?.[0]?.coordinates) === "[-4,-6,-2,8,4,4]", `${file} has malformed local crossed-arm geometry`);
    check(bodywear?.boxes?.[0]?.sizeAdd === 0.5, `${file} does not preserve the authored robe shell size`);
  }
}

check(existsSync(join(resources, "assets", "minecraft", "textures", "entity", "villager", "villager_baby.png")),
  "Baby villager base texture is missing");
check(readFileSync(join(resources, "assets", "minecraft", "textures", "entity", "villager", "villager_baby.png"))
  .equals(readFileSync(join(modAssets, "textures", "entity", "dkn.png"))),
"Baby villager is not using the original add-on's dedicated baby face texture");

check(!fabricMod.depends?.entity_sound_features
  && !buildSource.includes("IMuO8COj")
  && !JSON.stringify(sounds).includes("silence")
  && !existsSync(join(resources, "assets", "minecraft", "esf", "entity", "villager", "ambient.properties"))
  && !existsSync(join(resources, "assets", "minecraft", "esf", "entity", "wandering_trader", "ambient.properties"))
  && !existsSync(join(resources, "assets", "minecraft", "esf", "entity", "sheep", "ambient.properties")),
"Client resources still replace vanilla entity sounds when the server does not have the mod");

{
  const model = JSON.parse(readFileSync(join(cem, "sheep2.jem"), "utf8"));
  const flatten = (entries) => entries.flatMap((entry) => [entry, ...flatten(entry.submodels ?? [])]);
  const all = flatten(model.models);
  const woolyBone = (bone) => all.find((entry) => entry.id === `wooly_base_${bone}`);
  const rootModel = model.models.find((entry) => entry.id === "wooly_base_root");
  check(rootModel?.part === "root" && rootModel.attach === true, "Wooly does not attach its Bedrock root rig");
  check(JSON.stringify(rootModel.translate) === "[0,-24,0]", "Wooly has the wrong Bedrock-to-Java root offset");
  for (const bone of [
    "root", "body", "46fljga5", "oggd_46fljga5", "k966h_head", "oggd_head", "3dafc", "7246gn6jd2q",
    "l66l9", "l66l9lgh", "l66l93gllge", "egml9", "root_d680", "d0_7dggj", "d680", "oggd_d680",
    "root_d681", "d1_7dggj", "d681", "oggd_d681", "root_d682", "d2_7dggj", "d682", "oggd_d682",
    "root_d683", "d3_7dggj", "d683", "oggd_d683",
  ]) check(woolyBone(bone), `Wooly is missing source bone ${bone}`);
  check(all.filter((entry) => entry.id?.startsWith("wooly_base_")).flatMap((entry) => entry.boxes ?? []).length === 22,
    "Wooly does not preserve all 22 source cubes");
  check(JSON.stringify(woolyBone("body")?.translate) === "[0,14.25,0]", "Wooly's body pivot is malformed");
  check(JSON.stringify(woolyBone("46fljga5")?.rotate) === "[-90,0,0]", "Wooly's body cube has the wrong rotation");
  check(JSON.stringify(woolyBone("k966h_head")?.translate) === "[0,3.75,-8]", "Wooly's head pivot is malformed");
  check(JSON.stringify(woolyBone("k966h_head")?.boxes?.[0]?.uvNorth) === "[8,8,14,14]", "Wooly's neutral face backing is missing");
  check(JSON.stringify(woolyBone("7246gn6jd2q")?.translate) === "[0,0,-0.025]", "Wooly's expression plane is not separated from its head");
  check(woolyBone("7246gn6jd2q")?.boxes?.every((box) => box.coordinates?.[5] === 0.05),
    "Wooly's expression geometry still contains zero-depth planes");
  for (const leg of ["d680", "d681", "d682", "d683"]) {
    check(JSON.stringify(woolyBone(leg)?.translate) === "[0,11.5,0]", `Wooly's ${leg} hip pivot is malformed`);
    const base = woolyBone(leg);
    const box = base?.boxes?.[0] ?? base?.submodels?.find((child) => child.id === `wooly_base_${leg}_cube_0`)?.boxes?.[0];
    check(JSON.stringify(box?.textureOffset) === "[0,16]", `Wooly's ${leg} source hoof UV is malformed`);
  }
  for (const part of ["head", "body", "leg1", "leg2", "leg3", "leg4"]) {
    const suppressor = model.models.find((entry) => entry.part === part);
    check(suppressor?.attach === false && !suppressor.boxes?.length, `Wooly does not suppress the vanilla ${part}`);
  }
  const animationText = JSON.stringify(rootModel.animations ?? []);
  check(animationText.includes("wooly_base_k966h_head.rx") && animationText.includes("head_pitch"), "Wooly's head look animation is missing");
  for (const leg of ["d680", "d681", "d682", "d683"]) {
    check(animationText.includes(`wooly_base_${leg}.rx`) && animationText.includes("limb_swing"), `Wooly's ${leg} leg animation is missing`);
  }
  check(!animationText.includes("wooly_base_root_d680.rx") && !animationText.includes("wooly_base_root_d681.rx")
    && !animationText.includes("wooly_base_root_d682.rx") && !animationText.includes("wooly_base_root_d683.rx"),
  "Wooly still walks from the feet-level controller pivots");
  check(animationText.includes("wooly_base_46fljga5.rx") && animationText.includes("wooly_base_k966h_head.ty"), "Wooly's walking body and head animation is missing");
  check(!animationText.includes("time*pi") && !animationText.includes("time*0.7854"), "Wooly still has tick-rate idle shaking");
  check(animationText.includes("wooly_base_3dafc.sx") && animationText.includes("36.6666"), "Wooly's blink animation is missing");
  for (const fleece of ["oggd_46fljga5", "oggd_head", "oggd_d680", "oggd_d681", "oggd_d682", "oggd_d683"]) {
    check(animationText.includes(`wooly_base_${fleece}.visible`) && animationText.includes("!nbt(Sheared,1)"), `Wooly's ${fleece} does not hide when sheared`);
  }
  check(animationText.includes("wooly_base_egml9.sx") && animationText.includes("vnap_mouth_open"), "Wooly's dialogue mouth animation is missing");
  check(animationText.includes('wooly_base_l66l9.sz\":\"(1-vnap_mouth_closed)*vnap_speaking'), "Wooly's neutral mouth does not hide the closed-mouth layer");
  check(animationText.includes('wooly_base_egml9.sx\":\"(0.5+vnap_mouth_width*0.5)*vnap_speaking+(1-vnap_speaking)'),
    "Wooly's pink mouth is not visible in the resting pose");
  const mouth = woolyBone("egml9");
  const closedMouth = woolyBone("l66l9");
  const upperLip = woolyBone("l66l9lgh");
  const lowerLip = woolyBone("l66l93gllge");
  check(JSON.stringify(mouth?.translate) === "[0,-1.7,-6.35]", "Wooly's pink mouth is not separated from the face");
  check(JSON.stringify(closedMouth?.translate) === "[0,-1,-6.875]", "Wooly's mouth strips are not separated from the pink mouth");
  for (const lip of [upperLip, lowerLip]) {
    check(lip?.boxes?.every((box) => JSON.stringify(box.uvNorth) === "[12,10,13,11]"),
      "Wooly's point-sampled mouth strip UV was not expanded for EMF");
  }
  const faceDepth = woolyBone("7246gn6jd2q").translate[2] + woolyBone("7246gn6jd2q").boxes[0].coordinates[2];
  const mouthDepth = mouth.translate[2] + mouth.boxes[0].coordinates[2];
  const lipDepth = closedMouth.translate[2] + upperLip.translate[2] + upperLip.boxes[0].coordinates[2];
  check(lipDepth < mouthDepth && mouthDepth < faceDepth,
    `Wooly's mouth layers have an unstable depth order: lip=${lipDepth}, mouth=${mouthDepth}, face=${faceDepth}`);

  const shearedModel = JSON.parse(readFileSync(join(cem, "sheep3.jem"), "utf8"));
  const shearedAll = flatten(shearedModel.models);
  check(shearedAll.filter((entry) => entry.id?.startsWith("wooly_base_")).flatMap((entry) => entry.boxes ?? []).length === 16,
    "Wooly's sheared model does not contain exactly the 16 skin and face cubes");
  for (const fleece of ["oggd_46fljga5", "oggd_head", "oggd_d680", "oggd_d681", "oggd_d682", "oggd_d683"]) {
    const fleeceModel = shearedAll.find((entry) => entry.id === `wooly_base_${fleece}`);
    check(fleeceModel && !(fleeceModel.boxes?.length), `Wooly's sheared ${fleece} still contains fleece geometry`);
  }
  const shearedRoot = shearedModel.models.find((entry) => entry.id === "wooly_base_root");
  check(JSON.stringify(shearedRoot?.animations ?? []) === JSON.stringify(rootModel?.animations ?? []),
    "Wooly's sheared variant does not preserve the working animations");
  const sheepProperties = readFileSync(join(cem, "sheep.properties"), "utf8");
  check(sheepProperties.includes("models.1=3") && sheepProperties.includes("nbt.1.Sheared=1")
    && sheepProperties.includes("models.2=2"), "Wooly's sheared model selector is missing");

  for (const layer of ["sheep_wool_undercoat", "sheep_wool"]) {
    const woolLayer = JSON.parse(readFileSync(join(cem, `${layer}2.jem`), "utf8"));
    check(woolLayer.models.length === 6 && woolLayer.models.every((entry) => entry.attach === false && !entry.boxes?.length), `Wooly's ${layer} layer is not suppressed`);
    const woolProperties = readFileSync(join(cem, `${layer}.properties`), "utf8");
    check(woolProperties.includes("models.1=2") && woolProperties.includes("Wooly The Sheep"), `Wooly's ${layer} selector is missing`);
  }

  const woolyTexture = join(modAssets, "textures", "entity", "diw.png");
  const rgba = execFileSync(ffmpeg, [
    "-hide_banner", "-loglevel", "error", "-i", woolyTexture,
    "-f", "rawvideo", "-pix_fmt", "rgba", "-frames:v", "1", "pipe:1",
  ]);
  const alphaValues = new Set();
  for (let offset = 3; offset < rgba.length; offset += 4) alphaValues.add(rgba[offset]);
  check(alphaValues.has(0) && alphaValues.has(255) && alphaValues.size === 2,
    `Wooly's texture alpha was not converted from Bedrock's mask semantics: ${[...alphaValues].sort((a, b) => a - b)}`);
}

for (const [gesture, companion] of Object.entries({
  phmycx: "clnzxd", qcjrlv: "xmtqdi", srtjvb: "aahqsf", hlofgw: "cytfsh",
  tlrowv: "pbfspv", hmnopd: "qswzxh", ypxycs: "kkagqa",
})) {
  const baked = animations.gestures.find((entry) => entry.name === gesture);
  check(baked?.layers?.includes(companion), `Gesture ${gesture} is missing companion layer ${companion}`);
}

const trackNames = new Set(animations.gestures.flatMap((gesture) => Object.keys(gesture.tracks)));
for (const target of ["root", "waist", "body", "head", "head_inner", "arms", "left_leg_root", "left_leg", "right_leg_root", "right_leg", "brow", "eye_group", "lower_face", "pupil_left", "pupil_right", "eye_left", "eye_right", "nose"]) {
  check([...trackNames].some((name) => name.startsWith(`${target}_`)), `No gesture animates the ${target} bone`);
}

for (const file of readdirSync(cem).filter((name) => name.endsWith(".jem"))) {
  JSON.parse(readFileSync(join(cem, file), "utf8"));
}

console.log(JSON.stringify({
  dialogueGroups: groups.length,
  soundEvents: Object.keys(sounds).length,
  synchronizedVariants: variantCount,
  dialogueGestures: animations.gestures.length,
  voiceFiles: readdirSync(join(modAssets, "sounds", "voice")).filter((name) => name.endsWith(".ogg")).length,
  cemModels: readdirSync(cem).filter((name) => name.endsWith(".jem")).length,
  serverTriggeredDialogueGroups: referencedGroups.length,
  unreferencedDialogueGroups: unreferencedGroups.length,
  handbookContexts: Object.keys(handbook.contexts).length,
  handbookCategories: handbook.categories.length,
}, null, 2));
