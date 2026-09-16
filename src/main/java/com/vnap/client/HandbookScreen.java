package com.vnap.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class HandbookScreen extends Screen {
	private static final HandbookData DATA = load();
	private static final int BUTTON_HEIGHT = 20;
	private static final int LIST_ROW_STEP = 22;
	private static final int MENU_ROW_STEP = 24;
	private static final int FOOTER_Y_OFFSET = 30;
	private static final int FOOTER_GAP = 6;
	private final Screen parent;
	private final boolean settingsOnly;
	private Page page;
	private Page returnPage = Page.TRIGGERS;
	private int pageIndex;
	private int entryIndex;
	private int categoryIndex;
	private int sectionIndex;
	private String search = "";
	private Entry detail;

	public HandbookScreen() {
		this(null, Page.HOME, false);
	}

	private HandbookScreen(Screen parent, Page page, boolean settingsOnly) {
		super(Component.translatable("screen.villager-news-addon-port.title"));
		this.parent = parent;
		this.page = page;
		this.settingsOnly = settingsOnly;
	}

	public static HandbookScreen settingsScreen(Screen parent) {
		VillagerNewsSettingsState.prepareConfigScreen();
		return new HandbookScreen(parent, Page.SETTINGS, true);
	}

	@Override
	protected void init() {
		int contentWidth = Math.min(380, width - 32);
		int left = (width - contentWidth) / 2;
		addText(left, 16, contentWidth, titleForPage().copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), true);
		switch (page) {
			case HOME -> buildHome(left, contentWidth);
			case GUIDE -> buildGuide(left, contentWidth);
			case OVERVIEW -> buildEntryPage(left, contentWidth, DATA.overview, Page.GUIDE);
			case SPECIALS -> buildEntryPage(left, contentWidth, DATA.specialVillagers, Page.GUIDE);
			case COSMETICS -> buildEntryPage(left, contentWidth, DATA.cosmetics, Page.GUIDE);
			case GENERAL -> buildEntryPage(left, contentWidth, DATA.generalInformation, Page.TRIGGERS);
			case SETTINGS -> buildSettings(left, contentWidth);
			case SOCIALS -> buildEntryPage(left, contentWidth, DATA.socials, Page.HOME);
			case SUPPORT -> buildSupport(left, contentWidth);
			case TRIGGERS -> buildTriggers(left, contentWidth);
			case CATEGORY -> buildCategory(left, contentWidth);
			case SECTION -> buildSection(left, contentWidth);
			case DETAIL -> buildDetail(left, contentWidth);
		}
	}

	private void buildHome(int left, int contentWidth) {
		addText(left, 48, contentWidth, Component.translatable(DATA.headline), true);
		int y = menuTop(126, 4);
		addMenuButton(left, y, contentWidth, "screen.villager-news-addon-port.page.guide", Page.GUIDE);
		addMenuButton(left, y + MENU_ROW_STEP, contentWidth, "screen.villager-news-addon-port.page.settings", Page.SETTINGS);
		addMenuButton(left, y + MENU_ROW_STEP * 2, contentWidth, "screen.villager-news-addon-port.page.socials", Page.SOCIALS);
		addMenuButton(left, y + MENU_ROW_STEP * 3, contentWidth, "screen.villager-news-addon-port.page.support", Page.SUPPORT);
		addRenderableWidget(Button.builder(Component.translatable("screen.villager-news-addon-port.button.close"), button -> onClose())
			.bounds(left, height - FOOTER_Y_OFFSET, contentWidth, BUTTON_HEIGHT).build());
	}

	private void buildGuide(int left, int contentWidth) {
		addText(left, 44, contentWidth, Component.translatable(DATA.guideIntro), true);
		int y = menuTop(112, 4);
		addMenuButton(left, y, contentWidth, "screen.villager-news-addon-port.page.overview", Page.OVERVIEW);
		addMenuButton(left, y + MENU_ROW_STEP, contentWidth, "screen.villager-news-addon-port.page.specials", Page.SPECIALS);
		addMenuButton(left, y + MENU_ROW_STEP * 2, contentWidth, "screen.villager-news-addon-port.page.cosmetics", Page.COSMETICS);
		addMenuButton(left, y + MENU_ROW_STEP * 3, contentWidth, "screen.villager-news-addon-port.page.triggers", Page.TRIGGERS);
		addBackButton(left, contentWidth, Page.HOME);
	}

	private void buildTriggers(int left, int contentWidth) {
		Component searchLabel = Component.translatable("screen.villager-news-addon-port.search.placeholder");
		EditBox field = new EditBox(font, left, 44, contentWidth - 62, 20, searchLabel);
		field.setValue(search);
		field.setMaxLength(80);
		field.setHint(searchLabel);
		addRenderableWidget(field);
		addRenderableWidget(Button.builder(Component.translatable("screen.villager-news-addon-port.button.go"), button -> {
			search = field.getValue().trim();
			pageIndex = 0;
			rebuildWidgets();
		}).bounds(left + contentWidth - 58, 44, 58, 20).build());
		if (!search.isBlank()) {
			buildSearchResults(left, contentWidth);
			return;
		}
		addText(left, 70, contentWidth, Component.translatable("screen.villager-news-addon-port.triggers.hint"), true);
		addRenderableWidget(Button.builder(Component.translatable("screen.villager-news-addon-port.page.general"), button -> navigate(Page.GENERAL))
			.bounds(left, 94, contentWidth, 20).build());
		List<Category> categories = DATA.categories;
		int rows = rowsPerPage(118);
		int start = pageStart(categories.size(), rows);
		for (int index = start; index < Math.min(categories.size(), start + rows); index++) {
			int selected = index;
			addRenderableWidget(Button.builder(Component.translatable(categories.get(index).title), button -> {
				categoryIndex = selected;
				pageIndex = 0;
				page = Page.CATEGORY;
				rebuildWidgets();
			}).bounds(left, 118 + (index - start) * LIST_ROW_STEP, contentWidth, BUTTON_HEIGHT).build());
		}
		addPager(left, contentWidth, categories.size(), Page.GUIDE, rows);
	}

	private void buildSearchResults(int left, int contentWidth) {
		String query = search.toLowerCase(Locale.ROOT);
		List<Entry> results = DATA.searchable.stream()
			.filter(entry -> clean(localized(entry.title)).toLowerCase(Locale.ROOT).contains(query)
				|| clean(localized(entry.body)).toLowerCase(Locale.ROOT).contains(query))
			.sorted(Comparator.comparing(entry -> localized(entry.title), String.CASE_INSENSITIVE_ORDER))
			.toList();
		addText(left, 70, contentWidth,
			Component.translatable("screen.villager-news-addon-port.search.results", results.size()), true);
		int rows = rowsPerPage(94);
		int start = pageStart(results.size(), rows);
		for (int index = start; index < Math.min(results.size(), start + rows); index++) {
			Entry entry = results.get(index);
			addRenderableWidget(Button.builder(Component.literal(clean(localized(entry.title))), button -> openDetail(entry, Page.TRIGGERS))
				.bounds(left, 94 + (index - start) * LIST_ROW_STEP, contentWidth, BUTTON_HEIGHT).build());
		}
		if (results.isEmpty()) addText(left, 110, contentWidth,
			Component.translatable("screen.villager-news-addon-port.search.empty").withStyle(ChatFormatting.RED), true);
		addPager(left, contentWidth, results.size(), Page.GUIDE, rows);
	}

	private void buildCategory(int left, int contentWidth) {
		Category category = DATA.categories.get(categoryIndex);
		addText(left, 44, contentWidth, Component.translatable("screen.villager-news-addon-port.category.hint"), true);
		int rows = rowsPerPage(72);
		int start = pageStart(category.sections.size(), rows);
		for (int index = start; index < Math.min(category.sections.size(), start + rows); index++) {
			int selected = index;
			addRenderableWidget(Button.builder(Component.translatable(category.sections.get(index).title), button -> {
				sectionIndex = selected;
				pageIndex = 0;
				page = Page.SECTION;
				rebuildWidgets();
			}).bounds(left, 72 + (index - start) * LIST_ROW_STEP, contentWidth, BUTTON_HEIGHT).build());
		}
		addPager(left, contentWidth, category.sections.size(), Page.TRIGGERS, rows);
	}

	private void buildSection(int left, int contentWidth) {
		Section section = DATA.categories.get(categoryIndex).sections.get(sectionIndex);
		List<Entry> groups = new ArrayList<>();
		for (String id : section.groups) {
			Entry entry = DATA.contexts.get(id);
			if (entry != null && !entry.title.isBlank()) groups.add(entry);
		}
		groups.addAll(section.entries);
		addText(left, 44, contentWidth, Component.translatable("screen.villager-news-addon-port.section.hint"), true);
		int rows = rowsPerPage(76);
		int start = pageStart(groups.size(), rows);
		for (int index = start; index < Math.min(groups.size(), start + rows); index++) {
			Entry entry = groups.get(index);
			addRenderableWidget(Button.builder(Component.literal(clean(localized(entry.title))), button -> openDetail(entry, Page.SECTION))
				.bounds(left, 76 + (index - start) * LIST_ROW_STEP, contentWidth, BUTTON_HEIGHT).build());
		}
		if (groups.isEmpty()) addText(left, 100, contentWidth,
			Component.translatable("screen.villager-news-addon-port.section.empty"), true);
		addPager(left, contentWidth, groups.size(), Page.CATEGORY, rows);
	}

	private void buildDetail(int left, int contentWidth) {
		if (detail != null) {
			addText(left, 52, contentWidth, Component.literal(clean(localized(detail.body))), false);
		}
		addBackButton(left, contentWidth, returnPage);
	}

	private void buildEntryPage(int left, int contentWidth, List<Entry> entries, Page back) {
		Entry entry = entries.get(Math.max(0, Math.min(entryIndex, entries.size() - 1)));
		addText(left, 48, contentWidth, Component.literal(clean(localized(entry.title))).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), true);
		addText(left, 74, contentWidth, Component.literal(clean(localized(entry.body))), false);
		int half = (contentWidth - 6) / 2;
		Button previous = Button.builder(Component.translatable("screen.villager-news-addon-port.button.previous"), button -> {
			entryIndex--;
			rebuildWidgets();
		}).bounds(left, height - 54, half, 20).build();
		previous.active = entryIndex > 0;
		addRenderableWidget(previous);
		Button next = Button.builder(Component.translatable("screen.villager-news-addon-port.button.next"), button -> {
			entryIndex++;
			rebuildWidgets();
		}).bounds(left + half + 6, height - 54, half, 20).build();
		next.active = entryIndex + 1 < entries.size();
		addRenderableWidget(next);
		addBackButton(left, contentWidth, back);
	}

	private void buildSupport(int left, int contentWidth) {
		addText(left, 52, contentWidth, Component.translatable(DATA.support), false);
		addBackButton(left, contentWidth, Page.HOME);
	}

	private void buildSettings(int left, int contentWidth) {
		boolean canEdit = VillagerNewsSettingsState.canEdit();
		boolean localSettings = VillagerNewsSettingsState.localSettings();
		addText(left, 42, contentWidth, Component.translatable(canEdit
			? localSettings
				? "screen.villager-news-addon-port.settings.scope.local"
				: "screen.villager-news-addon-port.settings.scope.server"
			: "screen.villager-news-addon-port.settings.scope.permission"), true);
		int labelWidth = Math.min(166, contentWidth / 2);
		int buttonLeft = left + labelWidth;
		int buttonWidth = contentWidth - labelWidth;
		int y = 66;
		addText(left, y + 6, labelWidth - 6,
			Component.translatable("settings.villager-news-addon-port.subtitles"), false);
		addRenderableWidget(Button.builder(toggleComponent(VillagerNewsClientSettings.subtitlesEnabled()), button -> {
			boolean enabled = !VillagerNewsClientSettings.subtitlesEnabled();
			VillagerNewsClientSettings.setSubtitlesEnabled(enabled);
			button.setMessage(toggleComponent(enabled));
		}).bounds(buttonLeft, y, buttonWidth, 20).build());
		y += 26;
		addText(left, y + 6, labelWidth - 6,
			Component.translatable("screen.villager-news-addon-port.settings.chattiness"), false);
		Button chattiness = Button.builder(chattinessLabel(VillagerNewsSettingsState.chattiness()), button -> {
			VillagerNewsSettingsState.setChattiness(VillagerNewsSettingsState.chattiness() + 1);
			button.setMessage(chattinessLabel(VillagerNewsSettingsState.chattiness()));
		}).bounds(buttonLeft, y, buttonWidth, 20).build();
		chattiness.active = canEdit;
		addRenderableWidget(chattiness);
		y += 26;
		addText(left, y + 6, labelWidth - 6,
			Component.translatable("screen.villager-news-addon-port.settings.rare_voicelines"), false);
		Button rareVoicelines = Button.builder(rareLabel(VillagerNewsSettingsState.rareVoicelines()), button -> {
			VillagerNewsSettingsState.setRareVoicelines(VillagerNewsSettingsState.rareVoicelines() + 1);
			button.setMessage(rareLabel(VillagerNewsSettingsState.rareVoicelines()));
		}).bounds(buttonLeft, y, buttonWidth, 20).build();
		rareVoicelines.active = canEdit;
		addRenderableWidget(rareVoicelines);
		y += 26;
		addText(left, y + 6, labelWidth - 6,
			Component.translatable("screen.villager-news-addon-port.settings.spawn_special_villagers"), false);
		Button spawnSpecialVillagers = Button.builder(localSettings
			? Component.translatable("screen.villager-news-addon-port.settings.spawn_special_villagers.server_only")
			: toggleComponent(VillagerNewsSettingsState.spawnSpecialVillagers()), button -> {
			VillagerNewsSettingsState.setSpawnSpecialVillagers(!VillagerNewsSettingsState.spawnSpecialVillagers());
			button.setMessage(toggleComponent(VillagerNewsSettingsState.spawnSpecialVillagers()));
		}).bounds(buttonLeft, y, buttonWidth, 20).build();
		spawnSpecialVillagers.active = canEdit && !localSettings;
		if (localSettings) spawnSpecialVillagers.setTooltip(Tooltip.create(Component.translatable(
			"screen.villager-news-addon-port.settings.spawn_special_villagers.server_only.tooltip")));
		addRenderableWidget(spawnSpecialVillagers);
		y += 26;
		addText(left, y + 6, labelWidth - 6,
			Component.translatable("screen.villager-news-addon-port.settings.style"), false);
		Button style = Button.builder(Component.translatable("screen.villager-news-addon-port.settings.style.villager_news"), button -> {
		}).bounds(buttonLeft, y, buttonWidth, 20).build();
		style.active = false;
		addRenderableWidget(style);
		if (settingsOnly) {
			addRenderableWidget(Button.builder(Component.translatable("screen.villager-news-addon-port.button.done"), button -> onClose())
				.bounds(left, height - FOOTER_Y_OFFSET, contentWidth, BUTTON_HEIGHT).build());
		} else addBackButton(left, contentWidth, Page.HOME);
	}

	private static Component toggleComponent(boolean enabled) {
		return Component.translatable(enabled ? "options.on" : "options.off");
	}

	private static Component chattinessLabel(int value) {
		String key = switch (value) {
			case 0 -> "muted";
			case 1 -> "shy";
			case 3 -> "super_chatty";
			default -> "chatty";
		};
		return Component.translatable("screen.villager-news-addon-port.settings.chattiness." + key);
	}

	private static Component rareLabel(int value) {
		String key = switch (value) {
			case 0 -> "never";
			case 2 -> "often";
			default -> "default";
		};
		return Component.translatable("screen.villager-news-addon-port.settings.rare_voicelines." + key);
	}

	private void addPager(int left, int contentWidth, int count, Page back, int rows) {
		int pages = pageCount(count, rows);
		int third = (contentWidth - 12) / 3;
		Button previous = Button.builder(Component.translatable("screen.villager-news-addon-port.button.previous"), button -> {
			pageIndex--;
			rebuildWidgets();
		}).bounds(left, height - FOOTER_Y_OFFSET, third, BUTTON_HEIGHT).build();
		previous.active = pageIndex > 0;
		addRenderableWidget(previous);
		addRenderableWidget(Button.builder(Component.translatable("screen.villager-news-addon-port.button.back"), button -> navigate(back))
			.bounds(left + third + 6, height - FOOTER_Y_OFFSET, third, BUTTON_HEIGHT).build());
		Button next = Button.builder(Component.translatable("screen.villager-news-addon-port.button.next"), button -> {
			pageIndex++;
			rebuildWidgets();
		}).bounds(left + (third + 6) * 2, height - FOOTER_Y_OFFSET, third, BUTTON_HEIGHT).build();
		next.active = pageIndex + 1 < pages;
		addRenderableWidget(next);
	}

	private int rowsPerPage(int firstRowY) {
		int spaceForOffsets = height - FOOTER_Y_OFFSET - FOOTER_GAP - BUTTON_HEIGHT - firstRowY;
		return Math.max(1, Math.floorDiv(spaceForOffsets, LIST_ROW_STEP) + 1);
	}

	private int pageStart(int count, int rows) {
		pageIndex = Math.max(0, Math.min(pageIndex, pageCount(count, rows) - 1));
		return pageIndex * rows;
	}

	private static int pageCount(int count, int rows) {
		return Math.max(1, (count + rows - 1) / rows);
	}

	private int menuTop(int preferredY, int buttonCount) {
		int latestY = height - FOOTER_Y_OFFSET - FOOTER_GAP - BUTTON_HEIGHT - (buttonCount - 1) * MENU_ROW_STEP;
		return Math.min(preferredY, latestY);
	}

	private void addMenuButton(int left, int y, int contentWidth, String labelKey, Page destination) {
		addRenderableWidget(Button.builder(Component.translatable(labelKey), button -> navigate(destination))
			.bounds(left, y, contentWidth, BUTTON_HEIGHT).build());
	}

	private void addBackButton(int left, int contentWidth, Page destination) {
		addRenderableWidget(Button.builder(Component.translatable("screen.villager-news-addon-port.button.back"), button -> navigate(destination))
			.bounds(left, height - FOOTER_Y_OFFSET, contentWidth, BUTTON_HEIGHT).build());
	}

	private void navigate(Page destination) {
		page = destination;
		pageIndex = 0;
		entryIndex = 0;
		if (destination != Page.TRIGGERS) search = "";
		rebuildWidgets();
	}

	private void openDetail(Entry entry, Page back) {
		detail = entry;
		returnPage = back;
		page = Page.DETAIL;
		rebuildWidgets();
	}

	private MultiLineTextWidget addText(int x, int y, int textWidth, Component text, boolean centered) {
		MultiLineTextWidget widget = new MultiLineTextWidget(x, y, text, font).setMaxWidth(textWidth).setCentered(centered);
		addRenderableWidget(widget);
		return widget;
	}

	private Component titleForPage() {
		return switch (page) {
			case HOME -> Component.translatable("screen.villager-news-addon-port.title");
			case GUIDE -> Component.translatable("screen.villager-news-addon-port.page.guide");
			case OVERVIEW -> Component.translatable("screen.villager-news-addon-port.page.overview");
			case SPECIALS -> Component.translatable("screen.villager-news-addon-port.page.specials");
			case COSMETICS -> Component.translatable("screen.villager-news-addon-port.page.cosmetics");
			case GENERAL -> Component.translatable("screen.villager-news-addon-port.page.general");
			case SETTINGS -> Component.translatable("screen.villager-news-addon-port.page.settings");
			case SOCIALS -> Component.translatable("screen.villager-news-addon-port.page.socials");
			case SUPPORT -> Component.translatable("screen.villager-news-addon-port.page.support");
			case TRIGGERS -> Component.translatable("screen.villager-news-addon-port.page.triggers");
			case CATEGORY -> Component.translatable(DATA.categories.get(categoryIndex).title);
			case SECTION -> Component.translatable(DATA.categories.get(categoryIndex).sections.get(sectionIndex).title);
			case DETAIL -> detail == null
				? Component.translatable("screen.villager-news-addon-port.page.trigger")
				: Component.literal(clean(localized(detail.title)));
		};
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}

	private static String clean(String value) {
		return value.replace("Â", "").replaceAll("§[0-9a-fk-or]", "");
	}

	private static String localized(String key) {
		return I18n.get(key);
	}

	private static HandbookData load() {
		String path = "/assets/villager-news-addon-port/handbook.json";
		try (InputStream stream = HandbookScreen.class.getResourceAsStream(path)) {
			if (stream == null) throw new IOException("Missing " + path);
			JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			List<Category> categories = new ArrayList<>();
			Map<String, Entry> contexts = new LinkedHashMap<>();
			for (Map.Entry<String, JsonElement> context : root.getAsJsonObject("contexts").entrySet()) {
				JsonObject value = context.getValue().getAsJsonObject();
				String title = value.get("browseTitle").getAsString();
				if (title.isBlank()) title = value.get("title").getAsString();
				contexts.put(context.getKey(), new Entry(title, value.get("body").getAsString()));
			}
			List<Entry> searchable = new ArrayList<>();
			for (JsonElement categoryElement : root.getAsJsonArray("categories")) {
				JsonObject category = categoryElement.getAsJsonObject();
				List<Section> sections = new ArrayList<>();
				for (JsonElement sectionElement : category.getAsJsonArray("sections")) {
					JsonObject section = sectionElement.getAsJsonObject();
					List<String> groups = new ArrayList<>();
					for (JsonElement group : section.getAsJsonArray("groups")) groups.add(group.getAsString());
					List<Entry> sectionEntries = entries(section.getAsJsonArray("entries"));
					for (String group : groups) {
						Entry entry = contexts.get(group);
						if (entry != null && !entry.title.isBlank()) searchable.add(entry);
					}
					searchable.addAll(sectionEntries);
					sections.add(new Section(section.get("title").getAsString(), List.copyOf(groups), sectionEntries));
				}
				categories.add(new Category(category.get("title").getAsString(), List.copyOf(sections)));
			}
			return new HandbookData(
				root.get("headline").getAsString(),
				root.get("guideIntro").getAsString(),
				entries(root.getAsJsonArray("overview")),
				entries(root.getAsJsonArray("specialVillagers")),
				entries(root.getAsJsonArray("cosmetics")),
				entries(root.getAsJsonArray("generalInformation")),
				entries(root.getAsJsonArray("socials")),
				entries(root.getAsJsonArray("settings")),
				root.get("support").getAsString(),
				List.copyOf(categories),
				Map.copyOf(contexts),
				List.copyOf(searchable)
			);
		} catch (IOException | RuntimeException exception) {
			throw new IllegalStateException("Could not load the Villager News handbook", exception);
		}
	}

	private static List<Entry> entries(JsonArray array) {
		List<Entry> result = new ArrayList<>();
		for (JsonElement element : array) {
			JsonObject entry = element.getAsJsonObject();
			result.add(new Entry(entry.get("title").getAsString(), entry.get("body").getAsString()));
		}
		return List.copyOf(result);
	}

	private enum Page {
		HOME, GUIDE, OVERVIEW, SPECIALS, COSMETICS, GENERAL, SETTINGS, SOCIALS, SUPPORT, TRIGGERS, CATEGORY, SECTION, DETAIL
	}

	private record Entry(String title, String body) {
	}

	private record Section(String title, List<String> groups, List<Entry> entries) {
	}

	private record Category(String title, List<Section> sections) {
	}

	private record HandbookData(String headline, String guideIntro, List<Entry> overview,
		List<Entry> specialVillagers, List<Entry> cosmetics, List<Entry> generalInformation,
		List<Entry> socials, List<Entry> settings, String support, List<Category> categories,
		Map<String, Entry> contexts, List<Entry> searchable) {
	}
}
