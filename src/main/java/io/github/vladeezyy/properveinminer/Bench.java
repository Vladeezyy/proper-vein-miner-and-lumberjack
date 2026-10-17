package io.github.vladeezyy.properveinminer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The server tick cost of the mod ({@code -Dpvm.bench}, dev only): a fake player breaks a big ore vein, a big tree and
 * a cube of stone with the mod, and the same blocks the vanilla way (the hooks skipped, all in one tick, so the work is
 * the same), and every tick is timed from its start to its end. Prints "[PVM bench] ..." lines, then stops the server.
 */
public final class Bench {
    private static final int WARMUP = 5;
    private static final int ROUNDS = 20;
    /** Unmeasured ticks after building, so the build's own updates are done before the measured tick. */
    private static final int SETTLE = 5;
    /** Measured ticks after the break (drops, item merging, block updates that come later). */
    private static final int AFTER = 3;

    private static final BlockPos ORE = SelfTest.HOME;
    private static final BlockPos TREE = SelfTest.HOME.offset(20, 0, 0);
    private static final BlockPos STONE = SelfTest.HOME.offset(0, 0, 20);
    private static final int ORE_SIZE = 5;   // 5x5x5 = 125 ores in a stone shell
    private static final int TRUNK = 64;     // 2x2x64 = 256 logs
    private static final int STONE_SIZE = 10; // 10x10x10 = 1000 stone

    private record Step(Runnable action, LongConsumer time) {}

    private final MinecraftServer server;
    private final ServerLevel level;
    private final ServerPlayer player;
    private final ArrayDeque<Step> steps = new ArrayDeque<>();
    private final Map<String, List<Double>> results = new LinkedHashMap<>();
    private long tickStart;

    private static Bench running;

    private Bench(MinecraftServer server, ServerPlayer player) {
        this.server = server;
        this.level = server.overworld();
        this.player = player;
    }

    public static void start(MinecraftServer server, ServerPlayer player) {
        Bench bench = new Bench(server, player);
        bench.plan();
        running = bench;
        ProperVeinMiner.LOGGER.info("[PVM bench] started: {} ticks (Minecraft {})", bench.steps.size(), ProperVeinMiner.MINECRAFT);
    }

    public static void startTick() {
        if (running != null) {
            running.tickStart = System.nanoTime();
        }
    }

    /** At the end of every tick: runs this tick's step, so its time counts in the tick. */
    public static void endTick() {
        if (running == null) {
            return;
        }
        Step step = running.steps.poll();
        if (step == null) {
            running.report();
            MinecraftServer server = running.server;
            running = null;
            server.halt(false);
            return;
        }
        step.action().run();
        if (step.time() != null && running.tickStart != 0) {
            step.time().accept(System.nanoTime() - running.tickStart);
        }
    }

    // --- the plan --------------------------------------------------------------------------------------------

    private void plan() {
        this.player.setGameMode(GameType.SURVIVAL);
        this.player.setShiftKeyDown(false);
        PvmConfig config = ProperVeinMiner.config();
        config.fake_players = true;
        config.protect_tool = false;
        config.ores.max_blocks = Math.max(config.ores.max_blocks, ORE_SIZE * ORE_SIZE * ORE_SIZE);
        config.trees.max_blocks = Math.max(config.trees.max_blocks, 4 * TRUNK);

        for (int i = 0; i < 100; i++) {
            this.add(() -> {}, this.timer("idle tick", false, true));
        }
        for (int round = 0; round < WARMUP + ROUNDS; round++) {
            boolean measured = round >= WARMUP;
            this.round("ore vein 125, mod", measured, this::buildOre, () -> this.breakAt(ORE), true);
            this.round("ore vein 125, vanilla", measured, this::buildOre, () -> this.vanilla(this.oreBlocks()), false);
            this.round("tree 256 logs, mod", measured, this::buildTree, () -> this.breakAt(TREE), true);
            this.round("tree 256 logs, vanilla", measured, this::buildTree, () -> this.vanilla(this.logBlocks()), false);
            this.round("1000 stone one by one, mod", measured, this::buildStone, () -> this.each(this.stoneBlocks()), true);
            this.round("1000 stone one by one, vanilla", measured, this::buildStone, () -> this.vanilla(this.stoneBlocks()), false);
        }
    }

    private void add(Runnable action, LongConsumer time) {
        this.steps.add(new Step(action, time));
    }

    private LongConsumer timer(String name, boolean measured, boolean always) {
        return nanos -> {
            if (measured || always) {
                this.results.computeIfAbsent(name, k -> new ArrayList<>()).add(nanos / 1_000_000.0);
            }
        };
    }

    /** Build (with the search timed alone for mod rounds), settle, the measured break tick, the ticks after it. */
    private void round(String name, boolean measured, Runnable build, Runnable action, boolean mod) {
        this.add(() -> {
            this.clearDrops();
            this.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(name.startsWith("tree") ? Items.NETHERITE_AXE : Items.NETHERITE_PICKAXE));
            build.run();
            if (mod && !name.startsWith("1000")) {
                BlockPos origin = name.startsWith("tree") ? TREE : ORE;
                BlockState state = this.level.getBlockState(origin);
                VeinMiner.Kind kind = VeinMiner.kind(this.player, state);
                long t = System.nanoTime();
                int found = kind == null ? -1 : VeinMiner.find(this.level, origin, state, kind).size();
                long nanos = System.nanoTime() - t;
                if (measured) {
                    this.results.computeIfAbsent(name.replace(", mod", "") + ": vein search alone (" + found + " found)", k -> new ArrayList<>())
                            .add(nanos / 1_000_000.0);
                }
            }
        }, null);
        for (int i = 0; i < SETTLE; i++) {
            this.add(() -> {}, null);
        }
        this.add(action, this.timer(name + ": break tick", measured, false));
        List<Double> after = new ArrayList<>();
        for (int i = 0; i < AFTER; i++) {
            boolean last = i == AFTER - 1;
            this.add(() -> {}, nanos -> {
                after.add(nanos / 1_000_000.0);
                if (last && measured) {
                    this.results.computeIfAbsent(name + ": next " + AFTER + " ticks together", k -> new ArrayList<>())
                            .add(after.stream().mapToDouble(Double::doubleValue).sum());
                }
            });
        }
    }

    // --- the scenes ------------------------------------------------------------------------------------------

    private void set(BlockPos pos, BlockState state) {
        this.level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    private void clearDrops() {
        AABB box = new AABB(SelfTest.HOME).inflate(40, 80, 40);
        this.level.getEntitiesOfClass(ItemEntity.class, box).forEach(e -> e.discard());
        this.level.getEntitiesOfClass(ExperienceOrb.class, box).forEach(e -> e.discard());
    }

    /** 125 iron and deepslate iron ores in a stone shell, as in the ground. */
    private void buildOre() {
        for (int x = -1; x <= ORE_SIZE; x++) {
            for (int y = -1; y <= ORE_SIZE; y++) {
                for (int z = -1; z <= ORE_SIZE; z++) {
                    boolean inside = x >= 0 && y >= 0 && z >= 0 && x < ORE_SIZE && y < ORE_SIZE && z < ORE_SIZE;
                    Block block = !inside ? Blocks.STONE : (x + y + z) % 2 == 0 ? Blocks.IRON_ORE : Blocks.DEEPSLATE_IRON_ORE;
                    this.set(ORE.offset(x, y, z), block.defaultBlockState());
                }
            }
        }
    }

    private List<BlockPos> oreBlocks() {
        List<BlockPos> list = new ArrayList<>();
        for (int x = 0; x < ORE_SIZE; x++) for (int y = 0; y < ORE_SIZE; y++) for (int z = 0; z < ORE_SIZE; z++) list.add(ORE.offset(x, y, z));
        return list;
    }

    /** A 2x2 trunk of 64 logs with rings of grown leaves every 4 blocks from the 8th up, and a leaf top. */
    private void buildTree() {
        BlockState leaf = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 1);
        for (int y = 0; y < TRUNK; y++) {
            for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) this.set(TREE.offset(x, y, z), Blocks.OAK_LOG.defaultBlockState());
            if (y >= 8 && y % 4 == 0) {
                for (int x = -1; x <= 2; x++) {
                    for (int z = -1; z <= 2; z++) {
                        if (x < 0 || x > 1 || z < 0 || z > 1) this.set(TREE.offset(x, y, z), leaf);
                    }
                }
            }
        }
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) this.set(TREE.offset(x, TRUNK, z), leaf);
    }

    private List<BlockPos> logBlocks() {
        List<BlockPos> list = new ArrayList<>();
        for (int y = 0; y < TRUNK; y++) for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) list.add(TREE.offset(x, y, z));
        return list;
    }

    private void buildStone() {
        for (BlockPos pos : this.stoneBlocks()) this.set(pos, Blocks.STONE.defaultBlockState());
    }

    private List<BlockPos> stoneBlocks() {
        List<BlockPos> list = new ArrayList<>();
        for (int x = 0; x < STONE_SIZE; x++) for (int y = 0; y < STONE_SIZE; y++) for (int z = 0; z < STONE_SIZE; z++) list.add(STONE.offset(x, y, z));
        return list;
    }

    // --- breaking --------------------------------------------------------------------------------------------

    private void breakAt(BlockPos pos) {
        this.player.gameMode.destroyBlock(pos);
    }

    /** Each block broken by the player with the mod's hooks running (stone: they decide "no vein" every time). */
    private void each(List<BlockPos> blocks) {
        for (BlockPos pos : blocks) this.player.gameMode.destroyBlock(pos);
    }

    /** The same blocks broken the vanilla way: the mod's hooks are skipped. */
    private void vanilla(List<BlockPos> blocks) {
        VeinMiner.setBreaking(true);
        try {
            for (BlockPos pos : blocks) this.player.gameMode.destroyBlock(pos);
        } finally {
            VeinMiner.setBreaking(false);
        }
    }

    // --- the report ------------------------------------------------------------------------------------------

    private void report() {
        ProperVeinMiner.LOGGER.info("[PVM bench] results (Minecraft {}, {} rounds after {} warm-up, ms: median / p90 / max)",
                ProperVeinMiner.MINECRAFT, ROUNDS, WARMUP);
        for (Map.Entry<String, List<Double>> e : this.results.entrySet()) {
            List<Double> v = new ArrayList<>(e.getValue());
            Collections.sort(v);
            double median = v.get(v.size() / 2);
            double p90 = v.get(Math.min(v.size() - 1, (int) Math.ceil(v.size() * 0.9) - 1));
            double max = v.get(v.size() - 1);
            ProperVeinMiner.LOGGER.info("[PVM bench] {}: {} / {} / {}", e.getKey(), fmt(median), fmt(p90), fmt(max));
        }
        ProperVeinMiner.LOGGER.info("[PVM bench] done");
    }

    private static String fmt(double ms) {
        return String.format(java.util.Locale.ROOT, "%.3f", ms);
    }
}
