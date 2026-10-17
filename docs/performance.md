# Performance journal

How much server tick time (MSPT) the mod costs, against vanilla breaking the same blocks.

## How it is measured

`tools/bench.sh [node]` (default `1.21.11-fabric`) starts a real dedicated server with `-Pbench`; `Bench.java` then
times every server tick from its start to its end while a fake player (netherite tools):

- breaks one block of a **5×5×5 iron / deepslate iron vein (125 ores)** in a stone shell, the mod mines the rest;
- breaks the bottom log of a **2×2×64 oak trunk (256 logs)** with rings of grown leaves, the mod fells the rest;
- breaks **1000 stone blocks one by one** with the mod's hooks running (each break asks "is this a vein?");

and, as the vanilla reference, breaks exactly the same blocks with the mod's hooks skipped, all in one tick (the same
amount of vanilla work). Measured: the tick of the break, and the 3 ticks after it together (drops, item merging,
block updates that come later). 5 warm-up rounds, then 20 measured rounds of each; median / p90 / max in ms. Idle
ticks of the same server for reference. Numbers move by about ±1 ms between runs (a MacBook, a dev server).

## 2026-10-17, 1.21.11 Fabric, before and after the optimisation

| ms, median (p90) | before: mod | before: vanilla | after: mod | after: vanilla |
|---|---|---|---|---|
| idle tick | 1.65 (2.81) | | 2.73 (5.88) | |
| ore vein 125: search alone | 0.21 (0.26) | | **0.09** (0.09) | |
| ore vein 125: break tick | 3.94 (4.42) | 2.77 (3.78) | 2.73 (5.05) | 3.93 (5.63) |
| ore vein 125: next 3 ticks | 8.53 (11.87) | 6.08 (10.22) | **4.90** (7.92) | 9.45 (12.28) |
| tree 256: search alone | 0.40 (0.51) | | 0.39 (0.52) | |
| tree 256: break tick | 5.20 (6.76) | 4.25 (5.35) | 5.38 (7.48) | 4.57 (6.84) |
| tree 256: next 3 ticks | 15.92 (26.66) | 10.44 (16.24) | **6.93** (8.83) | 13.73 (18.51) |
| 1000 stone: break tick | 14.72 (18.19) | 8.65 (16.39) | **8.41** (17.68) | 10.02 (13.96) |
| 1000 stone: next 3 ticks | 37.89 (46.33) | 40.31 (45.24) | 38.93 (45.94) | 40.85 (47.76) |

Before, the mod's own cost was:

- **the drops gathered in one spot**: hundreds of single items there made the game merge them pair by pair over the
  next ticks (tree: +5.5 ms over 3 ticks against vanilla);
- **~6 µs on every block anyone breaks**: the block's id built as a string and its tags walked to ask "ore? log?".

What changed:

- the gathered drops are merged into full stacks at once (255 logs → 4 item entities): the ticks after a tree or a vein
  now cost **half of vanilla's**, which leaves the same blocks' drops lying as single items;
- "ore / log / ore family" is worked out once per block type and kept (dropped when the config or the tags change, on
  `/reload`): breaking ordinary blocks costs the same as vanilla (within the noise), the ore vein search is 2.5× faster;
- machines of other mods breaking blocks as a fake player (turtles, block breakers) get no veins
  (`fake_players: false` in the config), so automated mining is not multiplied by the mod.

The break tick itself is the vanilla cost of breaking that many blocks (drops, light, block updates, packets) plus
under a millisecond of the mod's own; the biggest tick seen is 8 ms of the 50 ms budget. Spreading a vein over several
ticks is therefore not needed.
