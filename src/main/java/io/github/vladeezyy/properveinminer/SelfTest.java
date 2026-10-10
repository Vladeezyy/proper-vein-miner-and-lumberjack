package io.github.vladeezyy.properveinminer;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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
 * A self-test for development ({@code -Dpvm.selftest}): on a real server of each Minecraft version a fake player
 * breaks veins and trees, and the results are checked. Prints "[PVM selftest] PASS ..." / "FAIL ..." and a summary,
 * then stops the server.
 */
public final class SelfTest {
    private final ServerLevel level;
    private final ServerPlayer player;
    private final List<String> failures = new ArrayList<>();
    private int checks;
    private final BlockPos home;

    private SelfTest(ServerLevel level, ServerPlayer player) {
        this.level = level;
        this.player = player;
        this.home = HOME;
    }

    /** Far from spawn (spawn protection); its chunks are forced, so dropped items exist there and can be counted. */
    static final BlockPos HOME = new BlockPos(3000, 200, 3000);

    /** Forces the test chunks; the test runs a few seconds later, once they tick with their entities. */
    public static void prepare(MinecraftServer server) {
        int cx = HOME.getX() >> 4;
        int cz = HOME.getZ() >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                server.overworld().setChunkForced(cx + dx, cz + dz, true);
            }
        }
    }

    public static void run(MinecraftServer server, ServerPlayer player) {
        SelfTest test = new SelfTest(server.overworld(), player);
        try {
            test.all();
        } catch (Throwable t) {
            test.failures.add("crashed: " + t);
            ProperVeinMiner.LOGGER.error("[PVM selftest] crashed", t);
        }
        String result = test.failures.isEmpty() ? "PASS all " + test.checks + " checks" : "FAIL " + test.failures.size() + " of " + test.checks + ": " + test.failures;
        ProperVeinMiner.LOGGER.info("[PVM selftest] {} (Minecraft {})", result, ProperVeinMiner.MINECRAFT);
        server.halt(false);
    }

    private void check(boolean ok, String what) {
        this.checks++;
        ProperVeinMiner.LOGGER.info("[PVM selftest] {} {}", ok ? "PASS" : "FAIL", what);
        if (!ok) {
            this.failures.add(what);
        }
    }

    /** The test spot high above spawn, cleared to air and of items (spawn protection is off on the test server). */
    private BlockPos spot() {
        BlockPos base = this.home;
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-7, -4, -7), base.offset(7, 12, 7))) {
            this.level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        this.level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(20)).forEach(e -> e.discard());
        this.level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, new AABB(base).inflate(20)).forEach(e -> e.discard());
        return base;
    }

    private void hold(ItemStack tool) {
        this.player.setItemInHand(InteractionHand.MAIN_HAND, tool);
    }

    private int count(BlockPos base, Block block) {
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-7, -4, -7), base.offset(7, 12, 7))) {
            if (this.level.getBlockState(pos).is(block)) n++;
        }
        return n;
    }

    private int drops(BlockPos base, net.minecraft.world.item.Item item) {
        return this.level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(16)).stream().filter(e -> e.getItem().is(item))
                .mapToInt(e -> e.getItem().getCount()).sum();
    }

    private int ores(BlockPos base) {
        return this.count(base, Blocks.IRON_ORE) + this.count(base, Blocks.DEEPSLATE_IRON_ORE);
    }

    private boolean breakAt(BlockPos pos) {
        return this.player.gameMode.destroyBlock(pos);
    }

    /** An L-shaped vein of iron ore with deepslate iron ore mixed in (10 blocks). */
    private BlockPos ironVein(BlockPos base) {
        for (int i = 0; i < 6; i++) {
            this.level.setBlock(base.east(i), (i % 2 == 0 ? Blocks.IRON_ORE : Blocks.DEEPSLATE_IRON_ORE).defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (int i = 1; i <= 4; i++) {
            this.level.setBlock(base.east(5).above(i), Blocks.IRON_ORE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        return base;
    }

    /** A small oak tree: 6 logs and a crown of grown leaves. */
    private void tree(BlockPos base, boolean leaves) {
        for (int i = 0; i < 6; i++) {
            this.level.setBlock(base.above(i), Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        if (leaves) {
            BlockState leaf = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 1);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        this.level.setBlock(base.offset(dx, 5, dz), leaf, Block.UPDATE_CLIENTS);
                    }
                }
            }
            this.level.setBlock(base.above(6), leaf, Block.UPDATE_CLIENTS);
        }
    }

    private void all() {
        this.player.setGameMode(GameType.SURVIVAL);
        this.player.setShiftKeyDown(false);
        PvmConfig config = ProperVeinMiner.config();

        // a whole iron vein with an iron pickaxe: every block, 10 durability, 10 raw iron
        BlockPos base = this.ironVein(this.spot());
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        this.hold(pick);
        this.check(this.breakAt(base), "the first ore breaks");
        this.check(this.ores(base) == 0, "the whole iron vein (normal + deepslate) is mined (left " + this.ores(base) + ")");
        this.check(this.player.getMainHandItem().getDamageValue() == 10, "the pickaxe lost 10 durability (was " + this.player.getMainHandItem().getDamageValue() + ")");
        this.check(this.drops(base, Items.RAW_IRON) == 10, "10 raw iron dropped (got " + this.drops(base, Items.RAW_IRON) + ")");

        // sneaking: one block only
        base = this.ironVein(this.spot());
        this.hold(new ItemStack(Items.IRON_PICKAXE));
        this.player.setShiftKeyDown(true);
        this.breakAt(base);
        this.player.setShiftKeyDown(false);
        this.check(this.ores(base) == 9, "sneaking breaks only one ore (left " + this.ores(base) + " of 10)");

        // the wrong tool: a wooden pickaxe cannot mine iron, so no vein
        base = this.ironVein(this.spot());
        this.hold(new ItemStack(Items.WOODEN_PICKAXE));
        this.breakAt(base);
        this.check(this.ores(base) == 9, "a wooden pickaxe does not vein mine iron (left " + this.ores(base) + " of 10)");

        // a pickaxe with 4 uses left: it mines 3 more and keeps its last point (protect_tool)
        base = this.ironVein(this.spot());
        ItemStack worn = new ItemStack(Items.IRON_PICKAXE);
        worn.setDamageValue(worn.getMaxDamage() - 4);
        this.hold(worn);
        this.breakAt(base);
        ItemStack after = this.player.getMainHandItem();
        this.check(config.protect_tool && !after.isEmpty() && after.getMaxDamage() - after.getDamageValue() == 1,
                "a worn pickaxe stops at its last durability point");
        this.check(this.ores(base) == 7, "and mined 3 ores with it (left " + this.ores(base) + " of 10)");

        // a tree with an axe: all 6 logs, 6 durability
        base = this.spot();
        this.tree(base, true);
        this.hold(new ItemStack(Items.IRON_AXE));
        this.breakAt(base);
        this.check(this.count(base, Blocks.OAK_LOG) == 0, "the whole tree is felled");
        this.check(this.player.getMainHandItem().getDamageValue() == 6, "the axe lost 6 durability (was " + this.player.getMainHandItem().getDamageValue() + ")");
        this.check(this.drops(base, Items.OAK_LOG) == 6, "6 oak logs dropped (got " + this.drops(base, Items.OAK_LOG) + ")");

        // logs without leaves (a build): only the one
        base = this.spot();
        this.tree(base, false);
        this.hold(new ItemStack(Items.IRON_AXE));
        this.breakAt(base);
        this.check(this.count(base, Blocks.OAK_LOG) == 5, "a log pillar without leaves is not felled (left " + this.count(base, Blocks.OAK_LOG) + " of 6)");

        // a pickaxe on a tree: no lumberjack
        base = this.spot();
        this.tree(base, true);
        this.hold(new ItemStack(Items.IRON_PICKAXE));
        this.breakAt(base);
        this.check(this.count(base, Blocks.OAK_LOG) == 5, "a pickaxe does not fell trees (left " + this.count(base, Blocks.OAK_LOG) + " of 6)");

        // drops gathered at the first block
        BlockPos first = this.ironVein(this.spot());
        this.hold(new ItemStack(Items.DIAMOND_PICKAXE));
        this.breakAt(first);
        long far = this.level.getEntitiesOfClass(ItemEntity.class, new AABB(first).inflate(16)).stream()
                .filter(e -> e.blockPosition().distManhattan(first) > 2).count();
        int gathered = this.drops(first, Items.RAW_IRON);
        this.check(!config.gather_drops || (far == 0 && gathered >= 10), "the drops are gathered at the first block (" + gathered + " there, " + far + " elsewhere)");
    }
}
