# Villager News Mod Port

A Fabric port of the **Villager News Add-On** for Minecraft Java Edition 26.2.
It brings the original Villager News characters, models, animations, textures,
voice acting, and contextual dialogue to Java Edition while retaining normal
Minecraft villager gameplay.

Current release candidate: **1.4.1-rc.2**. The installable file is
`Villager-News-Mod-Port-1.4.1-rc.2.jar`; the `-sources.jar` file is for developers,
not for the Minecraft `mods` folder.

## Community

Other version and discussion: [Villager News Addon Port Discord server](https://discord.gg/vEpbtj2ChP)

## Features

- Detailed animated Villager News models converted for Entity Model Features
- Biome, profession, and profession-level villager textures
- The Mayor, Testificate Man, Villager Number 5, Villager Number 9, and
  Villager Unreachable as named characters
- Wooly the Sheep and the Villager News wandering trader
- 2,212 original voice clips across 523 dialogue groups
- Timed, speaker-labelled subtitles with English and Russian translations;
  long lines wrap within the game window
- Context-aware dialogue for player actions, nearby mobs, weather, dimensions,
  combat, trading, work, sleep, spawning, growth, and other world events
- Multi-part conversations between nearby villagers
- Facial expressions and gestures synchronized with each voice line
- Server-controlled dialogue selection, sound playback, cooldowns, and
  villager behavior
- Speakers look toward the player, entity, block, or villager they are talking
  about
- Removable villager noses, character cosmetics, cosmetic reactions, and
  missing-nose conversations
- Wearable signs for adult villagers, with 12 wood types and 87 rotating jokes;
  the sign artwork is localized for Russian
- Character trades for the Mayor Hat, Testificate Man Helmet, Moustache, and
  Microphone
- Persistent natural spawning for one of each special character in distant
  villages
- A craftable Villager News Handbook
- A remappable `N` shortcut for opening Villager News settings in-game
- Optional Mod Menu configuration screen
- Automatic client-only dialogue fallback on servers without this mod

## Requirements

- Minecraft Java Edition 26.2
- Java 25 or newer
- Fabric Loader 0.19.5 or newer
- Fabric API for Minecraft 26.2
- Entity Model Features 3.3.5 or newer
- Entity Texture Features 7.2.1 or newer

EMF and ETF are external dependencies. This project does not bundle or modify
them. Entity Sound Features is not required.

Mod Menu is optional. When installed, its Configure button opens the Villager
News settings directly. The same settings can be opened in-game with the
remappable `N` key or from the Villager News Handbook.

## Installation

1. Install Fabric Loader for Minecraft 26.2.
2. Download Fabric API, EMF, and ETF for the same Minecraft version.
3. Put the dependency jars and `Villager-News-Mod-Port-1.4.1-rc.2.jar` in the
   Minecraft `mods` folder. Do not install the `-sources.jar` file.
4. Start Minecraft with the Fabric profile.

### Multiplayer modes

For synchronized multiplayer, install the mod and its required dependencies on
the server and on each player's client. The server selects dialogue and
broadcasts the matching audio, subtitles, and animation. Server-owned features
such as special-character spawning, trades, and wearable signs are available.

You can also join a server without the mod using a modded client. The client
detects the missing server channel and runs a local dialogue engine for events
it can observe. Dialogue may differ between players and cannot react to every
server-side event. The client suppresses vanilla villager and wandering-trader
voices, plus Wooly's vanilla sheep voice, while replacement dialogue is enabled;
setting **Villager Chattiness** to **Muted** restores those vanilla voices.
Client-only mode cannot spawn special villagers, change trades, attach signs or
cosmetics, or control server-side villager behavior. Its **Spawn Special
Villagers** setting is therefore disabled. Singleplayer uses the integrated
server and supports the full feature set.

## Characters

Use a name tag on a villager to select a character model and voice:

| Name tag | Character |
| --- | --- |
| `Mayor`, `Mayor Villager`, or `The Mayor` | Mayor Villager |
| `Testificate Man` | Testificate Man |
| `Villager Number 5` or `Villager #5` | Villager Number 5 |
| `Villager Number 9` or `Villager #9` | Villager Number 9 |
| `Villager Unreachable` or `Can't Catch Me!` | Villager Unreachable |

Name a sheep `Wooly` or `Wooly The Sheep` to use Wooly's model, animations,
and sounds. Ordinary villagers and wandering traders receive their Villager
News appearance and dialogue automatically.

With the mod installed on the server, special characters can also appear as
new distant villages are generated. Each character appears once at a time and
becomes eligible to spawn again after being killed.

On a modded server, naming an established trader as the Mayor, Testificate Man,
Villager #5, or Villager #9 temporarily replaces its trades. Renaming it back
restores its saved profession, level, experience, and original offers, including
offer usage. These backups persist with the villager. Trades already erased by
older mod builds cannot be reconstructed. A legacy special villager carrying
only the old mod-generated offer returns unemployed when renamed.

## Items

All custom items are available in the **Villager News** creative-mode tab.

Craft the Villager News Handbook from three pieces of paper. It includes the
add-on's overview, special-character and cosmetic guides, settings reference,
social and support pages, and the complete searchable Triggers & Reactions
guide.

Shear an adult villager to remove its nose. Interact with that villager while
holding the nose to return it. The Mayor, Testificate Man, Villager #5, and
Villager #9 sell their matching cosmetics. Cosmetics can be given to ordinary
villagers and removed again with shears.

On a modded server or in singleplayer, use a standing sign on an adult villager
to give it a wearable sign. All 12 wood types are supported: oak, spruce,
birch, jungle, acacia, dark oak, mangrove, cherry, pale oak, bamboo, crimson,
and warped. The first sign receives a random one of 87 jokes. Use an axe on the
villager to advance the message, or sneak while using the axe to go backward.
Shears remove the sign and return the sign item. Baby villagers do not display
signs.

## Dialogue

Villagers react to what happens around them. They can comment when a player
approaches, stares, changes game mode, wears armor, receives an effect, breaks
or places a block, uses an item, completes a trade, or spawns a villager with a
spawn egg. They also react to their profession, workstation, level, biome,
weather, time of day, nearby entities, damage source, and other villagers.

When the server has the mod, it chooses the exact voice variant and broadcasts
its matching animation. Each speaker remains occupied for the real length of
the clip, preventing unrelated lines from overlapping. Conversation partners
take turns and continue looking at each other throughout multi-part exchanges.

The custom subtitle HUD follows each voice line, identifies its speaker, and
wraps long text to fit the GUI. It is enabled by default and can be toggled
independently in Villager News settings. The interface, Handbook, dialogue
subtitles, and sign messages are available in English and Russian. Select
**Russian (Russia)** in Minecraft's language settings to use the Russian
translations and sign artwork; other languages currently use the English sign
artwork.

## Settings

Open settings with the default `N` key, through the Handbook, or through Mod
Menu's **Configure** button if Mod Menu is installed. Rebind the shortcut in
Minecraft's Controls menu.

- **Subtitles** is a client-local display preference.
- **Villager Chattiness** controls dialogue frequency; **Muted** disables it.
- **Rare Voicelines** controls how often uncommon variants are selected.
- **Spawn Special Villagers** affects natural spawning only when the server has
  the mod; it is unavailable in client-only mode.

In singleplayer and client-only multiplayer, dialogue settings are saved
locally. On a modded multiplayer server, dialogue settings are server-wide and
only an operator can change them; each player can still toggle their own
subtitles.

## Building from source

Use JDK 25. The Gradle wrapper downloads the required build tooling.

On Windows:

```powershell
.\gradlew.bat build
```

On Linux or macOS:

```bash
./gradlew build
```

The installable jar is written to
`build/libs/Villager-News-Mod-Port-1.4.1-rc.2.jar`. The adjacent
`Villager-News-Mod-Port-1.4.1-rc.2-sources.jar` is not a mod installation file.

To include the operator-only dialogue test command in a development build, set
`dialogue_test_command=true` in `gradle.properties` before building. Use
`/dialoguetest <1-523>` in game to spawn the matching speaker and subject, play
every variant from that dialogue group, and remove the test actors when each one ends.
Use `/dialoguetest continuous` to run all 523 groups in order. Each group is
announced with its variant number in chat, and the next variant begins one second
after the current voice line finishes.
The setting defaults to `false` for release builds.

Run the asset and dialogue verification with:

```powershell
node tools/verify-port.mjs
```

FFmpeg must be available on `PATH` for image verification and asset generation.
Alternatively, set `FFMPEG_PATH` to the FFmpeg executable.

After extracting the original Bedrock packs into `build/bedrock-source`, create
a formatted copy of the complete add-on, a dialogue symbol map, a feature
inventory, and a Java dialogue coverage report with:

```powershell
node tools/deobfuscate-addon.mjs
```

The output is written to `build/deobfuscated-bedrock-source/full-addon`.
Wooly's smaller focused source map can also be generated with:

```powershell
node tools/deobfuscate-wooly.mjs
```

The focused output is written to `build/deobfuscated-bedrock-source/wooly`. The
known source symbols are documented in
[`docs/bedrock-deobfuscation/wooly.md`](docs/bedrock-deobfuscation/wooly.md).

## Credits

Villager News and the original add-on assets were created by **Oreville
Studios Ltd** and **Element Animation**. The converted models, textures,
animations, and audio remain the property of their respective owners. See
[`LICENSE`](LICENSE) for repository licensing details. This work builds on the
[original Java port by MarcYohannTheScripter](https://github.com/MarcYohannTheScripter/villager-news-addon-port).
