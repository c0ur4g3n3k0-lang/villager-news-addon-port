import { spawnSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";

const root = process.cwd();
const messages = JSON.parse(readFileSync(join(root, "tools", "sign-messages.json"), "utf8"));
const output = join(root, "src", "main", "resources", "assets", "villager-news-addon-port", "textures", "entity", "sign_text_ru_ru.png");
const fontCandidates = [
	process.env.SIGN_FONT_PATH,
	"C:\\Windows\\Fonts\\DejaVuSansMono.ttf",
	"/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
	"/usr/share/fonts/dejavu/DejaVuSansMono.ttf"
];
const font = fontCandidates.find((candidate) => candidate && existsSync(candidate));
if (!font) throw new Error("DejaVu Sans Mono font not found; set SIGN_FONT_PATH to a Cyrillic-capable TTF file");
if (!Array.isArray(messages) || messages.length !== 87 || messages.some((entry, index) =>
	entry.index !== index || !Array.isArray(entry.ru) || entry.ru.length < 1 || entry.ru.length > 4)) {
	throw new Error("The sign-message catalog must contain 87 indexed Russian messages");
}

const source = join(root, "tools", "SignAtlasGenerator.java");
const java = process.env.JAVA_HOME ? join(process.env.JAVA_HOME, "bin", process.platform === "win32" ? "java.exe" : "java") : "java";
const input = messages.map((entry) => `${entry.index}\t${Buffer.from(entry.ru.join("\n"), "utf8").toString("base64")}`).join("\n") + "\n";
const result = spawnSync(java, [source, font, output], {
	input,
	encoding: "utf8",
	maxBuffer: 16 * 1024 * 1024
});
if (result.stdout) process.stdout.write(result.stdout);
if (result.stderr) process.stderr.write(result.stderr);
if (result.error) throw result.error;
if (result.status !== 0) throw new Error(`Sign atlas generator exited with status ${result.status}`);
