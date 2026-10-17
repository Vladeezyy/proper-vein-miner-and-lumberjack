**Proper Vein Miner and Lumberjack {{version}} for Minecraft {{minecraft}}**

| Loader | File |
|---|---|
| Fabric / Quilt | `properveinminer-fabric-{{version}}+{{node}}.jar` |
| NeoForge | `properveinminer-neoforge-{{version}}+{{node}}.jar` |

Put the jar for your loader in `mods/` (on the server; on clients too for the key and the settings screen).
Fabric API and Mod Menu are optional.

**New in 1.0.1: less server tick time** ([measured against vanilla](https://github.com/Vladeezyy/proper-vein-miner-and-lumberjack/blob/main/docs/performance.md))

- The gathered drops are merged into full stacks at once: the ticks after felling a big tree cost half as much as
  vanilla breaking the same logs (they were 50% more before)
- Breaking ordinary blocks costs the same as vanilla: what is an ore or a log is worked out once per block type
- Machines of other mods that break blocks as a fake player (turtles, block breakers) no longer mine whole veins;
  `fake_players: true` in `config/properveinminer.json` turns it back on
- The license is now LGPL-3.0-only

**The mod**

- Vein Miner: whole ore veins, vanilla and modded, stone and deepslate variants together
- Lumberjack: whole trees, vanilla and modded, only with natural leaves, never below the cut
- Every block mined the vanilla way: durability per block, Fortune / Silk Touch / Unbreaking, mod drops, experience, protection mods
- Only with the right pickaxe or axe (modded tools too); stops before your tool breaks; drops gathered at the first block
- The ` key switches it off and on (two more keys for each part); one settings screen (Mod Menu / NeoForge Config button)
- 11 languages
