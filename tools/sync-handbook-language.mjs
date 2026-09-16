import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const currentFile = fileURLToPath(import.meta.url);
const projectRoot = resolve(dirname(currentFile), "..");
const namespace = "villager-news-addon-port";
const handbookFile = join(projectRoot, "src/main/resources/assets", namespace, "handbook.json");
const languageFile = join(projectRoot, "src/main/resources/assets", namespace, "lang/en_us.json");

export function localizeHandbook(sourceHandbook, language, modNamespace = namespace) {
	const prefix = `handbook.${modNamespace}.`;
	const previousLanguage = { ...language };
	for (const key of Object.keys(language)) {
		if (key.startsWith(prefix)) delete language[key];
	}
	const handbook = structuredClone(sourceHandbook);
	const store = (key, source) => {
		const value = source.startsWith(prefix) ? previousLanguage[source] : source;
		if (typeof value !== "string") throw new Error(`Missing English handbook text for ${source}`);
		language[key] = value;
		return key;
	};
	const localizeEntry = (entry, base) => ({
		...entry,
		title: store(`${base}.title`, entry.title),
		body: store(`${base}.body`, entry.body),
	});

	handbook.headline = store(`${prefix}headline`, handbook.headline);
	handbook.guideIntro = store(`${prefix}guide_intro`, handbook.guideIntro);
	handbook.support = store(`${prefix}support`, handbook.support);
	for (const [property, key] of [
		["overview", "overview"],
		["specialVillagers", "special_villagers"],
		["cosmetics", "cosmetics"],
		["generalInformation", "general_information"],
		["socials", "socials"],
		["settings", "settings"],
	]) {
		handbook[property] = handbook[property].map((entry, index) =>
			localizeEntry(entry, `${prefix}${key}.${index}`));
	}
	for (const [id, entry] of Object.entries(handbook.contexts)) {
		const base = `${prefix}context.${id}`;
		handbook.contexts[id] = {
			...entry,
			title: store(`${base}.title`, entry.title),
			browseTitle: store(`${base}.browse_title`, entry.browseTitle),
			body: store(`${base}.body`, entry.body),
		};
	}
	for (const category of handbook.categories) {
		category.title = store(`${prefix}category.${category.id}.title`, category.title);
		for (const section of category.sections) {
			const base = `${prefix}section.${section.id}`;
			section.title = store(`${base}.title`, section.title);
			section.entries = section.entries.map((entry, index) => localizeEntry(entry, `${base}.entry.${index}`));
		}
	}
	return handbook;
}

if (process.argv[1] && resolve(process.argv[1]) === currentFile) {
	const handbook = JSON.parse(readFileSync(handbookFile, "utf8"));
	const language = JSON.parse(readFileSync(languageFile, "utf8"));
	const localized = localizeHandbook(handbook, language, namespace);
	writeFileSync(handbookFile, `${JSON.stringify(localized, null, 2)}\n`);
	writeFileSync(languageFile, `${JSON.stringify(language, null, 2)}\n`);
	console.log(JSON.stringify({
		handbookKeys: Object.keys(language).filter((key) => key.startsWith(`handbook.${namespace}.`)).length,
		languageKeys: Object.keys(language).length,
	}, null, 2));
}
