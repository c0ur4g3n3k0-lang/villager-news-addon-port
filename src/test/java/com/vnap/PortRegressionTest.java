package com.vnap;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortRegressionTest {
	private static final Path ROOT = Path.of("").toAbsolutePath();

	@Test
	void clientCompanionsAreAvailableInDevelopmentRuntime() {
		String build = read("build.gradle");
		assertTrue(build.contains("localRuntime \"maven.modrinth:4I1XuqiY:${project.emf_version}\""));
		assertTrue(build.contains("localRuntime \"maven.modrinth:BVzZfTc1:${project.etf_version}\""));
		assertFalse(build.contains("IMuO8COj"));
	}

	@Test
	void unsupportedServersPreserveVanillaEntitySounds() {
		String client = read("src/main/java/com/vnap/client/VillagerNewsAddonPortClient.java");
		String compatibility = read("src/main/java/com/vnap/client/ServerCompatibilityState.java");
		String villagerSound = read("src/main/java/com/vnap/mixin/VillagerSoundMixin.java");
		String traderSound = read("src/main/java/com/vnap/mixin/WanderingTraderSoundMixin.java");
		String sheepSound = read("src/main/java/com/vnap/mixin/SheepSoundMixin.java");
		String manifest = read("src/main/resources/fabric.mod.json");
		String sounds = read("src/main/resources/assets/villager-news-addon-port/sounds.json");
		assertTrue(client.contains("ServerCompatibilityState::detectServerSupport"));
		assertTrue(client.contains("ServerCompatibilityState.markServerModPresent()"));
		assertTrue(compatibility.contains("ClientPlayNetworking.canSend(VillagerNewsSettingsPayload.TYPE)"));
		assertTrue(villagerSound.contains("level().isClientSide()"));
		assertTrue(traderSound.contains("vnap$isLogicalServer()"));
		assertTrue(sheepSound.contains("vnap$isServerWooly()"));
		assertFalse(manifest.contains("entity_sound_features"));
		assertFalse(sounds.contains("silence"));
		assertFalse(Files.exists(ROOT.resolve("src/main/resources/assets/minecraft/esf/entity/villager/ambient.properties")));
		assertFalse(Files.exists(ROOT.resolve("src/main/resources/assets/minecraft/esf/entity/wandering_trader/ambient.properties")));
		assertFalse(Files.exists(ROOT.resolve("src/main/resources/assets/minecraft/esf/entity/sheep/ambient.properties")));
	}

	@Test
	void unsupportedServersUseAnIsolatedClientDialogueEngine() {
		String client = read("src/main/java/com/vnap/client/VillagerNewsAddonPortClient.java");
		String engine = read("src/main/java/com/vnap/client/ClientOnlyDialogueController.java");
		String compatibility = read("src/main/java/com/vnap/client/ServerCompatibilityState.java");
		String settings = read("src/main/java/com/vnap/client/VillagerNewsSettingsState.java");
		String mixins = read("src/main/resources/villager-news-addon-port.client.mixins.json");
		assertTrue(client.contains("ClientOnlyDialogueController.register()"));
		assertTrue(client.contains("ClientOnlyDialogueController.tick(client)"));
		assertTrue(client.contains("ClientOnlyDialogueController.clear(client)"));
		assertTrue(engine.contains("ServerCompatibilityState.clientOnlyFallback()"));
		assertTrue(engine.contains("DialogueSoundState.start(payload)"));
		assertTrue(engine.contains("DialogueAnimationState.start(payload)"));
		assertTrue(engine.contains("DialogueSubtitleState.start(payload)"));
		assertTrue(engine.contains("ClientPlayerBlockBreakEvents.AFTER.register"));
		assertTrue(engine.contains("PENDING_PLACEMENTS.put"));
		assertTrue(engine.contains("PENDING_BREAKS.put"));
		assertFalse(engine.contains("ClientPlayNetworking.send"));
		assertFalse(engine.contains("villager.getOffers()"));
		assertTrue(engine.contains("menu.getOffers()"));
		assertTrue(compatibility.contains("VillagerNewsSettingsState.activateClientOnly()"));
		assertTrue(settings.contains("!ServerCompatibilityState.clientOnlyFallback()"));
		assertTrue(mixins.contains("ClientBlockItemMixin"));
	}

	@Test
	void villagerNewsSubtitlePreferenceIsClientOnlyAndDefaultsToEnabled() {
		String client = read("src/main/java/com/vnap/client/VillagerNewsAddonPortClient.java");
		String clientSettings = read("src/main/java/com/vnap/client/VillagerNewsClientSettings.java");
		String sharedSettings = read("src/main/java/com/vnap/config/VillagerNewsSettings.java");
		String payload = read("src/main/java/com/vnap/network/VillagerNewsSettingsPayload.java");
		String network = read("src/main/java/com/vnap/network/VillagerNewsSettingsNetwork.java");
		String handbook = read("src/main/java/com/vnap/client/HandbookScreen.java");
		String language = read("src/main/resources/assets/villager-news-addon-port/lang/en_us.json");
		assertTrue(client.contains("VillagerNewsClientSettings.load()"));
		assertTrue(clientSettings.contains("villager-news-addon-port-client.json"));
		assertTrue(clientSettings.contains("private static boolean subtitlesEnabled = true"));
		assertTrue(clientSettings.contains("setSubtitlesEnabled(boolean enabled)"));
		assertFalse(sharedSettings.contains("subtitlesEnabled"));
		assertFalse(payload.contains("subtitlesEnabled"));
		assertFalse(network.contains("subtitlesEnabled"));
		assertTrue(handbook.contains("VillagerNewsClientSettings.setSubtitlesEnabled(enabled)"));
		assertTrue(handbook.contains("settings.villager-news-addon-port.subtitles"));
		assertFalse(handbook.contains("showSubtitles"));
		assertTrue(language.contains("settings.villager-news-addon-port.subtitles"));
	}

	@Test
	void settingsScreenHasARemappableClientKeyMapping() {
		String client = read("src/main/java/com/vnap/client/VillagerNewsAddonPortClient.java");
		String keyMappings = read("src/main/java/com/vnap/client/VillagerNewsKeyMappings.java");
		String language = read("src/main/resources/assets/villager-news-addon-port/lang/en_us.json");
		assertTrue(client.contains("VillagerNewsKeyMappings.register()"));
		assertTrue(client.contains("VillagerNewsKeyMappings.tick(client)"));
		assertTrue(keyMappings.contains("KeyMappingHelper.registerKeyMapping(new KeyMapping("));
		assertTrue(keyMappings.contains("KeyMapping.Category.register(VillagerNewsAddonPort.id(\"general\"))"));
		assertTrue(keyMappings.contains("InputConstants.KEY_N"));
		assertTrue(keyMappings.contains("openSettings.consumeClick()"));
		assertTrue(keyMappings.contains("client.gui.screen() == null"));
		assertTrue(keyMappings.contains("HandbookScreen.settingsScreen(null)"));
		assertFalse(keyMappings.contains("InputConstants.isKeyDown"));
		assertTrue(language.contains("\"key.category.villager-news-addon-port.general\": \"Villager News\""));
		assertTrue(language.contains("\"key.villager-news-addon-port.open_settings\": \"Open Villager News Settings\""));
	}

	@Test
	void villagerNewsSubtitleStateAndHudAreSeparatedFromVanillaSubtitles() {
		String client = read("src/main/java/com/vnap/client/VillagerNewsAddonPortClient.java");
		String state = read("src/main/java/com/vnap/client/DialogueSubtitleState.java");
		String hud = read("src/main/java/com/vnap/client/VillagerNewsSubtitleHud.java");
		assertTrue(client.contains("VillagerNewsSubtitleHud.register()"));
		assertTrue(client.contains("DialogueSubtitleState.tick(client)"));
		assertTrue(state.contains("public static List<VisibleSubtitle> visible(Minecraft minecraft, long now)"));
		assertFalse(state.contains("HudElementRegistry"));
		assertFalse(state.contains("GuiGraphicsExtractor"));
		assertTrue(hud.contains("HudElementRegistry.attachElementAfter"));
		assertTrue(hud.contains("DialogueSubtitleState.visible"));
		assertTrue(hud.contains("VillagerNewsClientSettings.subtitlesEnabled()"));
		assertFalse(state.contains("showSubtitles"));
		assertFalse(hud.contains("showSubtitles"));
	}

	@Test
	void customSubtitleHudUsesStackedBottomCenterCards() {
		String hud = read("src/main/java/com/vnap/client/VillagerNewsSubtitleHud.java");
		assertTrue(hud.contains("MAX_CARDS = 4"));
		assertTrue(hud.contains("graphics.guiHeight() - BOTTOM_MARGIN"));
		assertTrue(hud.contains("MAX_WIDTH_RATIO = 0.60F"));
		assertTrue(hud.contains("font.split(speaker, textWidth)"));
		assertTrue(hud.contains("font.split(transcript, textWidth)"));
		assertTrue(hud.contains("cardHeight(font.lineHeight, speakerLines.size(), transcriptLines.size())"));
		assertTrue(hud.contains("(graphics.guiWidth() - layout.width()) / 2"));
		assertTrue(hud.contains("graphics.fill(left, top, left + layout.width(), top + layout.height(), applyOpacity(BACKGROUND_COLOR, opacity))"));
		assertTrue(hud.contains("for (FormattedCharSequence line : layout.speakerLines())"));
		assertTrue(hud.contains("for (FormattedCharSequence line : layout.transcriptLines())"));
		assertTrue(hud.contains("bottomY = top - CARD_GAP"));
		assertFalse(hud.contains("subtitleLine("));
	}

	@Test
	void customSubtitleFadeUsesExistingFrameLifetime() {
		String state = read("src/main/java/com/vnap/client/DialogueSubtitleState.java");
		String hud = read("src/main/java/com/vnap/client/VillagerNewsSubtitleHud.java");
		assertTrue(state.contains("active.frameStartNanos(frame)"));
		assertTrue(state.contains("active.frameEndNanos(frame)"));
		assertTrue(hud.contains("subtitle.frameStartNanos()"));
		assertTrue(hud.contains("subtitle.frameEndNanos()"));
		assertTrue(hud.contains("FADE_IN_NANOS = 100_000_000L"));
		assertTrue(hud.contains("FADE_OUT_NANOS = 150_000_000L"));
		assertTrue(hud.contains("applyOpacity(BACKGROUND_COLOR, opacity)"));
		assertTrue(hud.contains("applyOpacity(SPEAKER_COLOR, opacity)"));
		assertTrue(hud.contains("applyOpacity(TRANSCRIPT_COLOR, opacity)"));
	}

	@Test
	void clientOnlyDialogueSuppressesVanillaCharacterVoices() {
		String client = read("src/main/java/com/vnap/client/VillagerNewsAddonPortClient.java");
		String gate = read("src/main/java/com/vnap/client/ClientOnlyVanillaSoundGate.java");
		String soundState = read("src/main/java/com/vnap/client/DialogueSoundState.java");
		String packetMixin = read("src/main/java/com/vnap/mixin/client/ClientPacketListenerMixin.java");
		String mixins = read("src/main/resources/villager-news-addon-port.client.mixins.json");
		assertFalse(client.contains("ClientOnlyVanillaSoundGate.tick(client)"));
		assertFalse(client.contains("ClientOnlyVanillaSoundGate.clear()"));
		assertTrue(gate.contains("ServerCompatibilityState.clientOnlyFallback()"));
		assertTrue(gate.contains("VillagerNewsSettings.dialogueEnabled()"));
		assertTrue(gate.contains("isCharacterVoice(entity, sound.value())"));
		assertTrue(gate.contains("SoundEvents.VILLAGER_AMBIENT"));
		assertTrue(gate.contains("SoundEvents.WANDERING_TRADER_AMBIENT"));
		assertTrue(gate.contains("SoundEvents.SHEEP_AMBIENT"));
		assertFalse(gate.contains("DELAY_NANOS"));
		assertFalse(gate.contains("playSeededSound"));
		assertFalse(soundState.contains("hasActiveOrPending(UUID id)"));
		assertTrue(packetMixin.contains("ClientOnlyVanillaSoundGate.shouldSuppress"));
		assertTrue(packetMixin.contains("ClientboundSoundPacket"));
		assertTrue(packetMixin.contains("method = \"handleSoundEvent\""));
		assertTrue(packetMixin.contains("handleSoundEntityEvent"));
		assertTrue(gate.contains("getEntitiesOfClass(Sheep.class"));
		assertTrue(mixins.contains("ClientPacketListenerMixin"));
	}

	@Test
	void disablingDialogueReleasesPendingSleep() {
		String controller = read("src/main/java/com/vnap/dialogue/ContextualDialogueController.java");
		String tick = methodBody(controller, "private static void tick(MinecraftServer server)");
		String disabledBranch = blockBody(tick, tick.indexOf("if (!VillagerNewsSettings.dialogueEnabled())"));
		assertTrue(disabledBranch.contains("releasePendingSleep();"));
		assertTrue(disabledBranch.contains("return;"));
	}

	@Test
	void deathDialogueSubtitlesKeepTheirSpeakerSnapshot() {
		String subtitles = read("src/main/java/com/vnap/client/DialogueSubtitleState.java");
		String start = methodBody(subtitles, "public static void start(DialogueAnimationPayload payload)");
		String tick = methodBody(subtitles, "public static void tick(Minecraft minecraft)");
		String visible = methodBody(subtitles, "public static List<VisibleSubtitle> visible(Minecraft minecraft, long now)");
		assertTrue(start.contains("isDeathDialogue(payload.groupId())"));
		assertTrue(start.contains("speaker == null ? null : speaker.position()"));
		assertTrue(start.contains("speaker == null ? null : speakerName(speaker).copy()"));
		assertTrue(tick.contains("!active.persistsAfterDeath() && entity != null && !entity.isAlive()"));
		assertTrue(visible.contains("usesSpeakerSnapshot(active.persistsAfterDeath(), speakerPresent, speakerAlive)"));
		assertTrue(visible.contains("useSnapshot ? active.position() : entity.position()"));
		assertTrue(visible.contains("useSnapshot ? active.speakerName() : speakerName(entity)"));
	}

	@Test
	void serverAndClientOnlyDialoguesShareOneSubtitlePipeline() {
		String client = read("src/main/java/com/vnap/client/VillagerNewsAddonPortClient.java");
		String fallback = read("src/main/java/com/vnap/client/ClientOnlyDialogueController.java");
		String hud = read("src/main/java/com/vnap/client/VillagerNewsSubtitleHud.java");
		int serverReceiver = client.indexOf("ClientPlayNetworking.registerGlobalReceiver(DialogueAnimationPayload.TYPE");
		int serverSound = client.indexOf("DialogueSoundState.start(payload);", serverReceiver);
		int serverAnimation = client.indexOf("DialogueAnimationState.start(payload);", serverSound);
		int serverSubtitle = client.indexOf("DialogueSubtitleState.start(payload);", serverAnimation);
		assertTrue(serverReceiver >= 0 && serverSound > serverReceiver && serverAnimation > serverSound && serverSubtitle > serverAnimation);
		int fallbackPayload = fallback.indexOf("DialogueAnimationPayload payload =");
		int fallbackSound = fallback.indexOf("DialogueSoundState.start(payload);", fallbackPayload);
		int fallbackAnimation = fallback.indexOf("DialogueAnimationState.start(payload);", fallbackSound);
		int fallbackSubtitle = fallback.indexOf("DialogueSubtitleState.start(payload);", fallbackAnimation);
		assertTrue(fallbackPayload >= 0 && fallbackSound > fallbackPayload && fallbackAnimation > fallbackSound
			&& fallbackSubtitle > fallbackAnimation);
		assertFalse(fallback.contains("HudElementRegistry"));
		assertTrue(client.indexOf("VillagerNewsSubtitleHud.register()") == client.lastIndexOf("VillagerNewsSubtitleHud.register()"));
		assertTrue(hud.contains("DialogueSubtitleState.visible(minecraft, now)"));
	}

	@Test
	void blockDialogueRunsOnlyAfterSuccessfulPlacement() {
		String mixin = read("src/main/java/com/vnap/mixin/BlockItemMixin.java");
		String configuration = read("src/main/resources/villager-news-addon-port.mixins.json");
		assertTrue(configuration.contains("\"BlockItemMixin\""));
		assertTrue(mixin.contains("@Inject(method = \"placeBlock\", at = @At(\"RETURN\"))"));
		assertTrue(mixin.contains("if (cir.getReturnValue()"));
		assertTrue(mixin.contains("ContextualDialogueController.onBlockPlaced"));
	}

	@Test
	void spectatorBellClicksDoNotStartDialogueBeforeVanillaRejectsTheInteraction() {
		for (String path : new String[] {
			"src/main/java/com/vnap/dialogue/ContextualDialogueController.java",
			"src/main/java/com/vnap/client/ClientOnlyDialogueController.java"
		}) {
			String source = read(path);
			String callback = blockBody(source, source.indexOf("UseBlockCallback.EVENT.register"));
			int spectatorGuard = callback.indexOf("if (player.isSpectator()) return InteractionResult.PASS;");
			int bellReaction = callback.indexOf("clickedPath.equals(\"bell\")");
			assertTrue(spectatorGuard >= 0 && spectatorGuard < bellReaction, path);
		}
	}

	@Test
	void adultMayorUsesBabySizedHitboxAndRestoresNormalDimensionsWhenRenamed() {
		String mixin = read("src/main/java/com/vnap/mixin/VillagerDataMixin.java");
		String dialogue = read("src/main/java/com/vnap/dialogue/ContextualDialogueController.java");
		String mayorRig = read("src/main/resources/assets/minecraft/optifine/cem/villager2.jem");
		String tick = methodBody(mixin, "private void vnap$syncSpecialTrade(CallbackInfo ci)");
		String dimensions = methodBody(mixin,
			"private void vnap$mayorHitbox(Pose pose, CallbackInfoReturnable<EntityDimensions> cir)");
		assertTrue(mayorRig.contains("\"this.sy\": \"0.33333*vnap_root_sy\""));
		assertTrue(mixin.contains("EntityDimensions.scalable(0.49F, 0.98F).withEyeHeight(0.63F)"));
		assertTrue(dialogue.contains("return cast(villager) == CastProfile.MAYOR;"));
		assertTrue(tick.contains("boolean mayor = !villager.isBaby() && ContextualDialogueController.isMayor(villager)"));
		assertTrue(tick.contains("if (mayor != vnap$mayorDimensions)"));
		assertTrue(tick.contains("villager.refreshDimensions()"));
		assertTrue(dimensions.contains("!villager.isBaby() && ContextualDialogueController.isMayor(villager)"));
		assertTrue(dimensions.contains("cir.setReturnValue(VNAP_MAYOR_DIMENSIONS)"));
	}

	@Test
	void specificBlockPlacementRulesPrecedeTheirGeneralCategories() {
		String controller = read("src/main/java/com/vnap/dialogue/ContextualDialogueController.java");
		String selector = methodBody(controller,
			"private static String selectPlaceContext(Block block, ServerLevel level, BlockPos position)");
		int valuable = selector.indexOf("return \"Place a Valuable Block\"");
		int redstone = selector.indexOf("return \"Place a Redstone Component\"");
		for (String title : new String[] {
			"Place an Iron Block", "Place a Gold Block", "Place a Diamond Block", "Place an Emerald Block"
		}) {
			assertTrue(selector.indexOf("return \"" + title + "\"") < valuable, title + " is shadowed");
		}
		for (String title : new String[] { "Place a Button", "Place a Lever" }) {
			assertTrue(selector.indexOf("return \"" + title + "\"") < redstone, title + " is shadowed");
		}
	}

	@Test
	void handbookPaginationAdaptsAndClampsTheCurrentPage() {
		String handbook = read("src/main/java/com/vnap/client/HandbookScreen.java");
		String rowsPerPage = methodBody(handbook, "private int rowsPerPage(int firstRowY)");
		String pageStart = methodBody(handbook, "private int pageStart(int count, int rows)");
		assertTrue(rowsPerPage.contains("height - FOOTER_Y_OFFSET"));
		assertTrue(rowsPerPage.contains("Math.max(1"));
		assertTrue(pageStart.contains("Math.max(0, Math.min(pageIndex, pageCount(count, rows) - 1))"));
	}

	@Test
	void handbookUiAndSearchResolveStableTranslationKeys() {
		String handbook = read("src/main/java/com/vnap/client/HandbookScreen.java");
		String generator = read("tools/port-addon.mjs");
		String localization = read("tools/sync-handbook-language.mjs");
		String data = read("src/main/resources/assets/villager-news-addon-port/handbook.json");
		String language = read("src/main/resources/assets/villager-news-addon-port/lang/en_us.json");
		assertTrue(handbook.contains("screen.villager-news-addon-port.search.placeholder"));
		assertTrue(handbook.contains("private static String localized(String key)"));
		assertTrue(handbook.contains("return I18n.get(key);"));
		assertFalse(handbook.contains("Search Triggers"));
		assertFalse(handbook.contains("Dialogue settings are saved locally"));
		assertTrue(data.contains("\"headline\": \"handbook.villager-news-addon-port.headline\""));
		assertTrue(data.contains("handbook.villager-news-addon-port.context.xfpjxq.browse_title"));
		assertTrue(language.contains("\"handbook.villager-news-addon-port.headline\": \"Breaking News!"));
		assertTrue(generator.contains("localizeHandbook(handbook, javaLanguage, modNamespace)"));
		assertTrue(localization.contains("export function localizeHandbook"));
		assertTrue(localization.contains("if (key.startsWith(prefix)) delete language[key]"));
	}

	@Test
	void basicRussianLocalizationCoversClientFacingControlsAndKeepsActivationNamesExact() {
		String language = read("src/main/resources/assets/villager-news-addon-port/lang/ru_ru.json");
		assertTrue(language.contains("\"item.villager-news-addon-port.handbook\": \"Справочник Villager News\""));
		assertTrue(language.contains("\"settings.villager-news-addon-port.subtitles\": \"Субтитры Villager News\""));
		assertTrue(language.contains("\"key.villager-news-addon-port.open_settings\": \"Открыть настройки Villager News\""));
		assertTrue(language.contains("\"screen.villager-news-addon-port.search.results\": \"Найдено условий: %s\""));
		assertTrue(language.contains("Mayor Villager, Testificate Man, Villager #5, Villager #9 или Villager Unreachable"));
		assertTrue(language.contains("Wooly The Sheep"));
	}

	@Test
	void russianHandbookTranslationIsCompleteAndUsesLocalizedGuideTerminology() {
		String language = read("src/main/resources/assets/villager-news-addon-port/lang/ru_ru.json");
		assertTrue(language.contains("\"handbook.villager-news-addon-port.headline\": \"Срочные новости!"));
		assertTrue(language.contains("\"handbook.villager-news-addon-port.category.ctbedw.title\": \"Особые персонажи\""));
		assertTrue(language.contains("\"handbook.villager-news-addon-port.section.fexqvd.title\": \"Измерения\""));
		assertTrue(language.contains("§eУсловие\\n\\n§7"));
		assertTrue(language.contains("§eРеакция\\n\\n§7"));
		assertFalse(language.contains("§eTrigger"));
		assertFalse(language.contains("§eReaction"));
		assertFalse(language.contains("\"handbook.villager-news-addon-port.special_villagers.0.title\": \"Villager #5\""));
		assertFalse(language.contains("\"handbook.villager-news-addon-port.special_villagers.3.title\": \"Testificate Man\""));
	}

	@Test
	void firstRussianSubtitleBatchPreservesLocalizedNamesAndTimingSegments() {
		String language = read("src/main/resources/assets/villager-news-addon-port/lang/ru_ru.json");
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.kxrhxt.0.0\": \"Ааа!\""));
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.xfpjxq.0.1\": \"Тестификат-мэне?\""));
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.opxfuo.4.2\": \"Покупайте шерсть!\""));
		assertFalse(language.matches("(?s).*subtitles\\.[^\n]*[\\uE000-\\uF8FF].*"));
	}

	@Test
	void secondRussianSubtitleBatchEndsOnACompleteGroupAndUsesGuideTerminology() {
		String language = read("src/main/resources/assets/villager-news-addon-port/lang/ru_ru.json");
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.mgmzeh.0.0\": \"Хммм...\""));
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.gcoysc.4.0\": \"Кадавр, кажется?\""));
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.vxycol.2.0\": \"Вулли! Это ты?\""));
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.lqxmlx.9.0\": \"Ты сейчас очень грубо обращаешься с гравитацией!\""));
	}

	@Test
	void thirdRussianSubtitleBatchEndsOnACompleteGroupAndUsesMinecraftTerminology() {
		String language = read("src/main/resources/assets/villager-news-addon-port/lang/ru_ru.json");
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.yiwncn.0.0\": \"Кнопки лучше, чем рычаги\""));
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.nwlcij.2.0\": \"Скрипун!\""));
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.jicosq.2.0\": \"Тише! Это Хранитель!\""));
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.satsrf.3.0\": \"О нет, визер!\""));
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.rnlher.1.0\": \"Тихоня!\""));
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.tqishj.2.0\": \"Эй, нюхач!\""));
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.vskjkl.12.0\": \"Ура! Я только что вылупился!\""));
	}

	@Test
	void fourthRussianSubtitleBatchCompletesTheCatalogAndUsesLocalizedNames() {
		String language = read("src/main/resources/assets/villager-news-addon-port/lang/ru_ru.json");
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.tkkegl.0.0\": \"Надеюсь, Санта не заберёт\""));
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.ididel.2.1\": \"Нижний мир? Нарочно?\""));
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.lxvofx.0.0\": \"Счастливый гаст!\""));
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.rdugrl.2.1\": \"Тестификат-мэном?!\""));
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.xccwah.0.2\": \"в деревне. Передаём Жителю №9...\""));
		assertTrue(language.contains("\"subtitles.villager-news-addon-port.dialogue.bygaxwmwtiaf.0.2\": \"*Хрмр*\""));
		assertFalse(language.matches("(?s).*subtitles\\.[^\n]*[\\uE000-\\uF8FF].*"));
	}

	@Test
	void signLayerPlacesTheBoardBelowTheVillagersHead() {
		String signLayer = read("src/main/java/com/vnap/client/VillagerNewsSignLayer.java");
		assertTrue(signLayer.contains("poseStack.translate(0.0F, 5.75F / 16.0F, -1.75F / 16.0F)"));
		assertFalse(signLayer.contains("poseStack.translate(0.0F, -5.75F / 16.0F"));
		assertTrue(signLayer.contains("getParentModel().translateToArms(state, poseStack)"));
		assertTrue(signLayer.contains("getPositionerForAttachment(EMFAttachment.Type.VILLAGER)"));
		assertTrue(signLayer.contains("poseStack.mulPose(Axis.XP.rotationDegrees(42.97F))"));
		// The vanilla arms pivot is (0, 3, -1) pixels and pitches back by 0.75 radians.
		// A board centered here must clear the head (ends at model Y=0) and the torso front (Z=-3).
		double armPitch = -0.75;
		double boardCenterY = 3.0 + Math.cos(armPitch) * 5.75 - Math.sin(armPitch) * -1.75;
		double boardCenterZ = -1.0 + Math.sin(armPitch) * 5.75 + Math.cos(armPitch) * -1.75;
		assertTrue(boardCenterY - 0.25625 * 16.0 > 0.0);
		assertTrue(boardCenterZ < -3.0);
	}

	@Test
	void russianSignAtlasKeepsEveryIndexedMessageAndLocalizedOverlay() throws IOException {
		String signLayer = read("src/main/java/com/vnap/client/VillagerNewsSignLayer.java");
		String controller = read("src/main/java/com/vnap/dialogue/ContextualDialogueController.java");
		String english = read("src/main/resources/assets/villager-news-addon-port/lang/en_us.json");
		String russian = read("src/main/resources/assets/villager-news-addon-port/lang/ru_ru.json");
		assertTrue(signLayer.contains("\"ru_ru\".equals(Minecraft.getInstance().getLanguageManager().getSelected())"));
		assertTrue(signLayer.contains("textures/entity/sign_text_ru_ru.png"));
		assertTrue(signLayer.contains("? TEXT_TEXTURE_RU_RU : TEXT_TEXTURE"));
		assertTrue(controller.contains("Component.translatable(\"message.villager-news-addon-port.sign_message\", message + 1, 87)"));
		assertTrue(english.contains("\"message.villager-news-addon-port.sign_message\": \"Sign message %s / %s\""));
		assertTrue(russian.contains("\"message.villager-news-addon-port.sign_message\": \"Надпись таблички %s / %s\""));
		Path texture = ROOT.resolve("src/main/resources/assets/villager-news-addon-port/textures/entity/sign_text_ru_ru.png");
		BufferedImage atlas = ImageIO.read(texture.toFile());
		assertNotNull(atlas);
		assertEquals(96, atlas.getWidth());
		assertEquals(87 * 35, atlas.getHeight());
		for (int message = 0; message < 87; message++) {
			boolean hasText = false;
			for (int y = message * 35; y < (message + 1) * 35; y++) {
				for (int x = 0; x < 96; x++) {
					int pixel = atlas.getRGB(x, y);
					assertEquals(0, pixel & 0x00FFFFFF, "Nonblack text pixel in message " + message);
					hasText |= (pixel >>> 24) != 0;
				}
			}
			assertTrue(hasText, "Blank sign message " + message);
		}
	}

	@Test
	void entityInteractionUsesTheReportedHand() {
		String controller = read("src/main/java/com/vnap/dialogue/ContextualDialogueController.java");
		String interaction = methodBody(controller,
			"private static InteractionResult onUseEntity(Player player, Entity entity, InteractionHand hand)");
		assertTrue(interaction.contains("ItemStack heldStack = player.getItemInHand(hand);"));
		assertTrue(interaction.contains("heldItem.equals(\"lead\")"));
		assertTrue(interaction.contains("heldItem.equals(\"shears\")"));
		assertFalse(interaction.contains("getMainHandItem()"));
	}

	@Test
	void specialTradingBacksUpAndRestoresProfessionOffersAndExperience() {
		String backup = read("src/main/java/com/vnap/entity/VillagerTradeBackup.java");
		String mixin = read("src/main/java/com/vnap/mixin/VillagerDataMixin.java");
		String controller = read("src/main/java/com/vnap/dialogue/ContextualDialogueController.java");
		String sync = methodBody(controller, "public static void ensureSpecialTrade(Villager villager)");
		assertTrue(backup.contains("villager.getVillagerData()"));
		assertTrue(backup.contains("villager.getOffers().copy()"));
		assertTrue(backup.contains("villager.getVillagerXp()"));
		assertTrue(backup.contains("villager.setVillagerData(data)"));
		assertTrue(backup.contains("villager.setOffers(offers.copy())"));
		assertTrue(backup.contains("villager.setVillagerXp(experience)"));
		assertTrue(backup.contains("backup.store(\"Data\", VillagerData.CODEC, data)"));
		assertTrue(backup.contains("backup.store(\"Offers\", MerchantOffers.CODEC, offers)"));
		assertTrue(backup.contains("backup.putInt(\"OfferCount\", offers.size())"));
		assertTrue(backup.contains("value.size() == count"));
		assertTrue(backup.contains("backup.putInt(\"Xp\", experience)"));
		assertTrue(mixin.contains("if (vnap$tradeBackup != null) vnap$tradeBackup.save(output)"));
		assertTrue(mixin.contains("VillagerTradeBackup.load(input).orElse(null)"));
		assertTrue(mixin.contains("if (vnap$readingSaveData) return value"));
		assertTrue(mixin.contains("@Inject(method = \"tick\", at = @At(\"HEAD\"))"));
		assertTrue(sync.contains("backup.restore(villager)"));
		assertTrue(sync.contains("state.vnap$setTradeBackup(null)"));
		assertTrue(sync.indexOf("state.vnap$setTradeBackup(VillagerTradeBackup.capture")
			< sync.indexOf("villager.getOffers().removeIf"));
		assertTrue(sync.contains("legacySpecialOnlyOffers"));
	}

	@Test
	void specialSpeakerLocalizationChangesOnlySubtitlePresentation() {
		String subtitles = read("src/main/java/com/vnap/client/DialogueSubtitleState.java");
		String helper = read("src/main/java/com/vnap/client/SpecialSpeakerNames.java");
		String russian = read("src/main/resources/assets/villager-news-addon-port/lang/ru_ru.json");
		assertTrue(subtitles.contains("SpecialSpeakerNames.villagerKey(customName.getString())"));
		assertTrue(subtitles.contains("SpecialSpeakerNames.sheepKey(customName.getString())"));
		assertTrue(subtitles.contains("return Component.translatable(key)"));
		assertFalse(subtitles.contains("setCustomName("));
		assertTrue(helper.contains("case \"villager #5\", \"villager number 5\""));
		assertTrue(russian.contains("\"speaker.villager-news-addon-port.mayor\": \"Мэр\""));
		assertTrue(russian.contains("\"speaker.villager-news-addon-port.number_9\": \"Житель №9\""));
		assertTrue(russian.contains("\"speaker.villager-news-addon-port.wooly\": \"Вулли\""));
	}

	@Test
	void acceptedSettingsAreBroadcastToEveryConnectedClient() {
		String network = read("src/main/java/com/vnap/network/VillagerNewsSettingsNetwork.java");
		String receiver = methodBody(network, "public static void register()");
		String broadcast = methodBody(network, "private static void sendAll(MinecraftServer server)");
		assertTrue(receiver.contains("sendAll(context.player().level().getServer());"));
		assertTrue(broadcast.contains("server.getPlayerList().getPlayers()"));
		assertTrue(broadcast.contains("send(player)"));
	}

	@Test
	void clientOnlySettingsCannotPretendToControlServerSpecialSpawns() {
		String handbook = read("src/main/java/com/vnap/client/HandbookScreen.java");
		String settings = read("src/main/java/com/vnap/client/VillagerNewsSettingsState.java");
		String english = read("src/main/resources/assets/villager-news-addon-port/lang/en_us.json");
		String russian = read("src/main/resources/assets/villager-news-addon-port/lang/ru_ru.json");
		String buildSettings = methodBody(handbook, "private void buildSettings(int left, int contentWidth)");
		String spawnSetter = methodBody(settings, "public static void setSpawnSpecialVillagers(boolean value)");
		assertTrue(buildSettings.contains("spawnSpecialVillagers.active = canEdit && !localSettings"));
		assertTrue(buildSettings.contains("spawnSpecialVillagers.setTooltip(Tooltip.create(Component.translatable("));
		assertTrue(buildSettings.contains("settings.spawn_special_villagers.server_only"));
		assertTrue(buildSettings.contains("chattiness.active = canEdit"));
		assertTrue(buildSettings.contains("rareVoicelines.active = canEdit"));
		assertTrue(spawnSetter.contains("if (!canEdit || localSettings) return;"));
		assertTrue(english.contains("\"screen.villager-news-addon-port.settings.spawn_special_villagers.server_only\": \"Server-side only\""));
		assertTrue(russian.contains("\"screen.villager-news-addon-port.settings.spawn_special_villagers.server_only.tooltip\": \"Доступно только при установленном моде на сервере.\""));
	}

	@Test
	void projectVersionAndPortableVerificationStayAligned() {
		Properties properties = new Properties();
		try (var reader = Files.newBufferedReader(ROOT.resolve("gradle.properties"))) {
			properties.load(reader);
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
		String version = properties.getProperty("version");
		String language = read("src/main/resources/assets/villager-news-addon-port/lang/en_us.json");
		String russian = read("src/main/resources/assets/villager-news-addon-port/lang/ru_ru.json");
		String manifest = read("src/main/resources/fabric.mod.json");
		String build = read("build.gradle");
		String readme = read("README.md");
		String verifier = read("tools/verify-port.mjs");
		String generator = read("tools/port-addon.mjs");
		String workflow = read(".github/workflows/build.yml");
		assertTrue(version.matches("\\d+\\.\\d+\\.\\d+(?:-rc\\.\\d+)?"));
		assertNotEquals("1.3.2", version);
		assertTrue(language.contains("\"guide.villager-news-addon-port.header\": \"Villager News " + version + "\""));
		assertTrue(russian.contains("\"guide.villager-news-addon-port.header\": \"Villager News " + version + "\""));
		String displayName = "Villager News Mod Port";
		String jarName = displayName.replace(' ', '-') + "-" + version + ".jar";
		assertTrue(manifest.contains("\"name\": \"" + displayName + "\""));
		assertTrue(build.contains("archivesName = \"" + displayName.replace(' ', '-') + "\""));
		assertTrue(readme.startsWith("# " + displayName));
		assertTrue(readme.contains(jarName));
		assertTrue(verifier.contains("`Villager News ${projectVersion}`"));
		assertTrue(verifier.contains("[process.env.FFMPEG_PATH, \"ffmpeg\"].find(executableAvailable)"));
		assertTrue(generator.contains("[process.env.FFMPEG_PATH, \"ffmpeg\"].find(executableAvailable)"));
		assertFalse(verifier.contains("C:\\\\Users\\\\marcy"));
		assertFalse(generator.contains("C:\\\\Users\\\\marcy"));
		assertTrue(workflow.contains("run: node tools/verify-port.mjs"));
	}

	private static String read(String relativePath) {
		try {
			return Files.readString(ROOT.resolve(relativePath));
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	private static String methodBody(String source, String signature) {
		int signatureStart = source.indexOf(signature);
		assertTrue(signatureStart >= 0, "Missing method: " + signature);
		return blockBody(source, signatureStart);
	}

	private static String blockBody(String source, int start) {
		assertTrue(start >= 0, "Missing block start");
		int openingBrace = source.indexOf('{', start);
		assertTrue(openingBrace >= 0, "Missing opening brace");
		int depth = 0;
		for (int index = openingBrace; index < source.length(); index++) {
			char character = source.charAt(index);
			if (character == '{') depth++;
			else if (character == '}' && --depth == 0) return source.substring(openingBrace + 1, index);
		}
		throw new AssertionError("Missing closing brace");
	}
}
