package io.github.vladeezyy.properveinminer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The vein miner and lumberjack. After a player breaks a block the normal way, the rest of the vein or tree is broken
 * with {@code ServerPlayerGameMode.destroyBlock} one block at a time, exactly as if the player had mined each one: the
 * tool loses durability per block, enchantments (fortune, silk touch, unbreaking) and modded drops apply, experience,
 * hunger and statistics count, and protection mods can refuse single blocks.
 */
public final class VeinMiner {
    public enum Kind { ORE, TREE }

    private static final TagKey<Block> C_ORES = TagKey.create(Registries.BLOCK, ProperVeinMiner.id("c", "ores"));
    private static final TagKey<Block> C_LOGS = TagKey.create(Registries.BLOCK, ProperVeinMiner.id("c", "logs"));
    private static final TagKey<net.minecraft.world.item.Item> C_AXES = TagKey.create(Registries.ITEM, ProperVeinMiner.id("c", "tools/axe"));
    private static final TagKey<net.minecraft.world.item.Item> C_PICKAXES = TagKey.create(Registries.ITEM, ProperVeinMiner.id("c", "tools/pickaxe"));

    /** Set while a vein is being broken, so the extra blocks do not start veins of their own. */
    private static final ThreadLocal<Boolean> BREAKING = ThreadLocal.withInitial(() -> false);

    private VeinMiner() {}

    public static boolean breaking() {
        return BREAKING.get();
    }

    /** The block tags of a state (26.1 renamed getTags to tags). */
    private static java.util.stream.Stream<TagKey<Block>> tags(BlockState state) {
        //? if >=26.1 {
        /*return state.tags();
        *///?} else {
        return state.getTags();
        //?}
    }

    private static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    // --- what counts -----------------------------------------------------------------------------------------

    /** An ore: in c:ores or a vanilla *_ores tag, named *_ore (mods that tag nothing), or listed in the config. */
    public static boolean isOre(BlockState state) {
        PvmConfig.Ores config = ProperVeinMiner.config().ores;
        String id = id(state.getBlock());
        if (config.excluded_blocks.contains(id)) {
            return false;
        }
        if (config.extra_blocks.contains(id) || state.is(C_ORES) || state.is(Blocks.ANCIENT_DEBRIS)) {
            return true;
        }
        String path = id.substring(id.indexOf(':') + 1);
        return path.endsWith("_ore") || path.startsWith("ore_") || tags(state).anyMatch(t -> t.location().getPath().endsWith("_ores"));
    }

    /** A log or stem: minecraft:logs, c:logs, or listed in the config. */
    public static boolean isLog(BlockState state) {
        PvmConfig.Trees config = ProperVeinMiner.config().trees;
        String id = id(state.getBlock());
        if (config.excluded_blocks.contains(id)) {
            return false;
        }
        return config.extra_blocks.contains(id) || state.is(BlockTags.LOGS) || state.is(C_LOGS);
    }

    /** Leaves that grew there (not placed by a player), or the wart blocks of nether trees. */
    private static boolean isNaturalLeaves(BlockState state) {
        if (state.is(BlockTags.WART_BLOCKS)) {
            return true;
        }
        if (!state.is(BlockTags.LEAVES)) {
            return false;
        }
        return !state.hasProperty(LeavesBlock.PERSISTENT) || !state.getValue(LeavesBlock.PERSISTENT);
    }

    /** The ore "family" for mixing variants: the c:ores/<name> tag, else the block itself. */
    private static Optional<Identifier> oreFamily(BlockState state) {
        return tags(state).map(TagKey::location)
                .filter(l -> l.getNamespace().equals("c") && l.getPath().startsWith("ores/") && !l.getPath().startsWith("ores_in_ground"))
                .findFirst();
    }

    private static boolean sameOre(BlockState origin, BlockState other) {
        if (other.getBlock() == origin.getBlock()) {
            return true;
        }
        if (!ProperVeinMiner.config().ores.mix_variants || !isOre(other)) {
            return false;
        }
        Optional<Identifier> family = oreFamily(origin);
        return family.isPresent() && family.equals(oreFamily(other));
    }

    /**
     * Whether the held item is the right tool. Ores: it must harvest the block (a modded pickaxe of the right tier
     * does), or for ores that drop with anything, be a pickaxe. Logs: an axe (any item tagged as one, or that cuts
     * wood faster).
     */
    private static boolean rightTool(Kind kind, ItemStack tool, BlockState state) {
        if (tool.isEmpty()) {
            return false;
        }
        if (kind == Kind.ORE) {
            if (state.requiresCorrectToolForDrops()) {
                return tool.isCorrectToolForDrops(state);
            }
            return tool.is(ItemTags.PICKAXES) || tool.is(C_PICKAXES) || tool.getDestroySpeed(state) > 1.0F;
        }
        return tool.is(ItemTags.AXES) || tool.is(C_AXES) || tool.getDestroySpeed(state) > 1.0F;
    }

    private static boolean active(ServerPlayer player) {
        PvmConfig config = ProperVeinMiner.config();
        if (player.isCreative() && !config.in_creative) {
            return false;
        }
        return switch (config.activation) {
            case "always" -> true;
            case "while_sneaking" -> player.isShiftKeyDown();
            default -> !player.isShiftKeyDown();
        };
    }

    /** What a broken block starts, if anything (checked before it is broken, with its state). */
    public static Kind kind(ServerPlayer player, BlockState state) {
        if (breaking() || !active(player)) {
            return null;
        }
        PvmConfig config = ProperVeinMiner.config();
        ItemStack tool = player.getMainHandItem();
        if (config.ores.enabled && isOre(state) && rightTool(Kind.ORE, tool, state)) {
            return Kind.ORE;
        }
        if (config.trees.enabled && isLog(state) && rightTool(Kind.TREE, tool, state)) {
            return Kind.TREE;
        }
        return null;
    }

    // --- finding the vein ------------------------------------------------------------------------------------

    /** The connected blocks of the vein or tree (all 26 neighbours, nearest first), without the broken one. */
    static List<BlockPos> find(ServerLevel level, BlockPos origin, BlockState originState, Kind kind) {
        PvmConfig config = ProperVeinMiner.config();
        int max = Math.max(1, kind == Kind.ORE ? config.ores.max_blocks : config.trees.max_blocks) - 1;
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(origin);
        queue.add(origin);
        boolean leaves = false;
        while (!queue.isEmpty() && found.size() < max) {
            BlockPos pos = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (!seen.add(next) || found.size() >= max || !level.isLoaded(next)) continue;
                        BlockState state = level.getBlockState(next);
                        if (kind == Kind.TREE) {
                            if (isNaturalLeaves(state)) {
                                leaves = true;
                                continue;
                            }
                            if (!isLog(state) || (config.trees.only_upwards && next.getY() < origin.getY())) continue;
                        } else if (!sameOre(originState, state)) {
                            continue;
                        }
                        found.add(next);
                        queue.add(next);
                    }
                }
            }
        }
        if (kind == Kind.TREE && config.trees.require_leaves && !leaves) {
            // logs without natural leaves: a building, not a tree
            return List.of();
        }
        return found;
    }

    // --- breaking --------------------------------------------------------------------------------------------

    /** Whether the tool can take one more block (it breaks on the last point, unless protect_tool). */
    private static boolean toolLeft(ItemStack tool, net.minecraft.world.item.Item item) {
        if (tool.isEmpty() || tool.getItem() != item) {
            return false;
        }
        if (!tool.isDamageableItem() || !ProperVeinMiner.config().protect_tool) {
            return true;
        }
        return tool.getMaxDamage() - tool.getDamageValue() > 1;
    }

    /** Breaks the rest of the vein; called after the player broke {@code origin}. Returns how many more broke. */
    public static int breakVein(ServerPlayer player, ServerLevel level, BlockPos origin, BlockState originState, Kind kind) {
        List<BlockPos> vein = find(level, origin, originState, kind);
        if (vein.isEmpty()) {
            return 0;
        }
        net.minecraft.world.item.Item item = player.getMainHandItem().getItem();
        int broken = 0;
        BREAKING.set(true);
        try {
            for (BlockPos pos : vein) {
                if (!toolLeft(player.getMainHandItem(), item)) {
                    break;
                }
                if (player.gameMode.destroyBlock(pos)) {
                    broken++;
                }
            }
        } finally {
            BREAKING.set(false);
        }
        if (broken > 0 && ProperVeinMiner.config().gather_drops) {
            gather(level, origin, vein);
        }
        return broken;
    }

    /** Moves the items and experience the vein just dropped to the first block. */
    private static void gather(ServerLevel level, BlockPos origin, List<BlockPos> vein) {
        int minX = origin.getX(), minY = origin.getY(), minZ = origin.getZ(), maxX = minX, maxY = minY, maxZ = minZ;
        for (BlockPos pos : vein) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        AABB box = new AABB(minX - 1, minY - 1, minZ - 1, maxX + 2, maxY + 2, maxZ + 2);
        Vec3 target = Vec3.atCenterOf(origin);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box, e -> e.tickCount == 0)) {
            item.setPos(target.x, target.y, target.z);
            item.setDeltaMovement(Vec3.ZERO);
        }
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, box, e -> e.tickCount == 0)) {
            orb.setPos(target.x, target.y, target.z);
        }
    }
}
