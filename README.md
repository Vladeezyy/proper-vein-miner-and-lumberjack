# Proper Vein Miner and Lumberjack

Mine a whole ore vein or fell a whole tree by breaking one block, with vanilla **and modded** ores, logs, pickaxes and
axes. For **Fabric** (and Quilt) and **NeoForge**, Minecraft **1.21 to 26.3**.

It does it the proper way: every extra block is broken exactly as if you had mined it yourself.

- The tool loses durability **per block** (Unbreaking counts), and the vein stops before your tool would break.
- **Fortune, Silk Touch** and every mod's own drops work per block; experience, hunger and statistics count.
- **Protection mods** (claims, spawn protection) are asked for every block.
- Only with the **right tool**: a pickaxe that can harvest the ore (a wooden pickaxe will not vein mine iron), an axe for
  trees. Modded pickaxes and axes work through their tags and tiers.
- **Trees, not houses**: logs are only felled together when the tree has natural leaves (or nether wart blocks), and
  never below the log you cut.
- Deepslate and stone variants of an ore are one vein; the drops gather at the first block.
- **Server side only**: put it on the server, players need nothing (it works in single player too).

Sneak to break just one block, or press the key (default `` ` ``) to switch it off and on (configurable).

## Keys and settings

- **`` ` ``** (grave / tilde, change it in Controls under "Proper Vein Miner"): Vein Miner and Lumberjack off / on, to
  break single blocks. Two more keys, unbound by default: the Vein Miner alone and the Lumberjack alone. The state shows
  above the hotbar and applies to you only. (Needs the mod on your client too; on Fabric with Fabric API.)
- **Settings screen**: Mod Menu on Fabric, the Config button in the mod list on NeoForge. Everything in one place: the
  Vein Miner and the Lumberjack on or off separately, how it is activated, tool protection, gathered drops, most blocks,
  extra and excluded blocks, and a button to the keys. On a dedicated server the server's `config/properveinminer.json`
  decides.

## Downloads

| Minecraft | Fabric / Quilt | NeoForge |
|---|---|---|
| 26.3 | ✔ | ✔ |
| 26.2 | ✔ | ✔ |
| 26.1 – 26.1.2 | ✔ | ✔ |
| 1.21.11 | ✔ | ✔ |
| 1.21.9 – 1.21.10 | ✔ | ✔ |
| 1.21.6 – 1.21.8 | ✔ | ✔ |
| 1.21.5 | ✔ | ✔ |
| 1.21.4 | ✔ | ✔ |
| 1.21.2 – 1.21.3 | ✔ | ✔ |
| 1.21 – 1.21.1 | ✔ | ✔ |

Fabric API and Mod Menu are optional (the keys need Fabric API, the settings button needs Mod Menu).
Languages: English, Russian, Ukrainian, Spanish, Portuguese, Polish, German, French, Korean, Chinese, Arabic.

## What counts

- **Ores**: blocks in the `c:ores` tag (vanilla and most mods), the vanilla `*_ores` tags, ancient debris, and blocks
  named `*_ore` (mods that tag nothing).
- **Logs**: `minecraft:logs` and `c:logs` (all vanilla logs and stems, and modded ones).
- Anything else: add its block id to `extra_blocks` in the config; leave something out with `excluded_blocks`.

## Config

`config/properveinminer.json` (written on the first start):

| Option | Default | |
|---|---|---|
| `activation` | `unless_sneaking` | `unless_sneaking`, `while_sneaking` or `always` |
| `protect_tool` | `true` | stop before the last durability point |
| `gather_drops` | `true` | drops and experience appear at the first block |
| `in_creative` | `false` | also in creative mode |
| `ores.enabled`, `trees.enabled` | `true` | |
| `ores.max_blocks` / `trees.max_blocks` | `128` / `256` | most blocks at once |
| `ores.mix_variants` | `true` | deepslate + stone variants are one vein |
| `trees.require_leaves` | `true` | only fell logs that have natural leaves |
| `trees.only_upwards` | `true` | never break logs below the cut one |
| `extra_blocks`, `excluded_blocks` | `[]` | block ids, e.g. `"somemod:ruby_ore"` |

## Building

Run Gradle on JDK 25 (Loom needs it; the 1.21 versions are still built for Java 21):

```
./gradlew buildAndCollect
```

Every version's jars land in `build/libs/<mod version>/`. `tools/selftest.sh` runs a self-test on a real server of
every version (a fake player mines veins and trees and the results are checked), `tools/clientcheck.sh` opens the
settings screen on every client. The code is one source for all versions, with
[Stonecutter](https://stonecutter.kikugie.dev/) comments where they differ.

## License

**LGPL-3.0-only** ([LICENSE](LICENSE), with the GPL-3.0 text in [COPYING](licenses/COPYING)), Copyright (c) 2026 Vladeezyy. Forks and modified versions must stay open under the same license and credit the authors. Please download the mod only from the official release pages. Releases up to the license change were published under MIT.
