package io.github.vladeezyy.properveinminer.client;

//? if =1.21.11 {
import io.github.vladeezyy.properveinminer.ProperVeinMiner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The pictures for the mod page (development, 1.21.11 only: -Dpvm.gallery with the world "pvm_gallery"): an ore vein
 * in a stone wall before and after one block was mined, a big oak before and after one log was cut, and the toggle
 * message. Saves screenshots/gallery_*.png and quits.
 */
public final class GalleryScene {
    private static int tick = -1;
    private static BlockPos stage;
    private static BlockPos firstOre;
    private static BlockPos trunk;

    private GalleryScene() {}

    private static void server(Minecraft mc, java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayer(mc.player.getUUID())));
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "gallery_" + name + ".png", mc.getMainRenderTarget(), 1, msg -> ProperVeinMiner.LOGGER.info("[PVM gallery] {}", msg.getString()));
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** A clear grass stage east of the player: 24 x 24, air up to 30 blocks. */
    private static void clear(ServerLevel level) {
        for (BlockPos pos : BlockPos.betweenClosed(stage.offset(-14, 0, -12), stage.offset(20, 30, 12))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (BlockPos pos : BlockPos.betweenClosed(stage.offset(-14, -1, -12), stage.offset(20, -1, 12))) {
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Every dropped item and orb on the stage (flowers and grass the stage building knocked off) */
    private static void clearDrops(ServerLevel level) {
        var box = new net.minecraft.world.phys.AABB(stage).inflate(40);
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box).forEach(e -> e.discard());
        level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, box).forEach(e -> e.discard());
    }

    /** The cut face of a hill: deepslate below, stone above, dirt and grass on top, stepping back like a slope */
    private static void oreWall(ServerLevel level) {
        BlockPos wall = stage.offset(7, 0, 0);
        for (int dz = -9; dz <= 9; dz++) {
            for (int dx = 0; dx < 8; dx++) {
                int height = 9 + dx + (Math.abs(dz) > 6 ? -1 : 0);
                for (int dy = 0; dy < height; dy++) {
                    Block block = dy < 2 ? Blocks.DEEPSLATE : dy < height - 2 ? Blocks.STONE : dy < height - 1 ? Blocks.DIRT : Blocks.GRASS_BLOCK;
                    set(level, wall.offset(dx, dy, dz), block);
                }
            }
        }
        // an iron vein running through stone and deepslate, a coal vein beside it
        int[][] iron = {{0, 1, -1}, {0, 1, 0}, {0, 2, 0}, {0, 2, 1}, {0, 3, 1}, {0, 3, 2}, {0, 4, 2}, {1, 2, 0}, {0, 0, -1}, {0, 1, -2}};
        for (int[] o : iron) {
            BlockPos pos = wall.offset(o[0], o[1], o[2]);
            set(level, pos, pos.getY() - wall.getY() < 2 ? Blocks.DEEPSLATE_IRON_ORE : Blocks.IRON_ORE);
        }
        int[][] coal = {{0, 4, -3}, {0, 5, -3}, {0, 5, -4}, {0, 4, -4}, {0, 5, -2}};
        for (int[] o : coal) {
            set(level, wall.offset(o[0], o[1], o[2]), Blocks.COAL_ORE);
        }
        firstOre = wall.offset(0, 1, 0);
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.getSingleplayerServer() == null) {
            return;
        }
        mc.options.pauseOnLostFocus = false;
        tick++;
        switch (tick) {
            case 40 -> server(mc, p -> {
                ServerLevel level = (ServerLevel) p.level();
                p.setGameMode(GameType.SURVIVAL);
                level.setDayTime(5000);
                level.setWeatherParameters(20000, 0, false, false);
                BlockPos at = p.blockPosition();
                int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX() + 8, at.getZ());
                stage = new BlockPos(at.getX(), ground, at.getZ());
                clear(level);
                oreWall(level);
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
                // looking east at the wall
                p.connection.teleport(stage.getX() - 2.5, stage.getY(), stage.getZ() + 0.5, -90.0F, -6.0F);
            });
            case 45 -> mc.options.hideGui = true;
            case 95 -> server(mc, p -> clearDrops((ServerLevel) p.level()));
            case 100 -> shot(mc, "vein_before");
            case 105 -> server(mc, p -> p.gameMode.destroyBlock(firstOre));
            case 125 -> shot(mc, "vein_after");
            case 130 -> server(mc, p -> {
                ServerLevel level = (ServerLevel) p.level();
                clearDrops(level);
                clear(level);
                trunk = stage.offset(9, 0, 0);
                var feature = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getValueOrThrow(TreeFeatures.FANCY_OAK);
                for (int attempt = 0; attempt < 20 && !level.getBlockState(trunk).is(Blocks.OAK_LOG); attempt++) {
                    feature.place(level, level.getChunkSource().getGenerator(), level.getRandom(), trunk);
                }
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
                // further back and looking up a little: the whole tree in view
                p.connection.teleport(stage.getX() - 7.5, stage.getY(), stage.getZ() + 0.5, -90.0F, -24.0F);
            });
            case 185 -> server(mc, p -> clearDrops((ServerLevel) p.level()));
            case 190 -> shot(mc, "tree_before");
            case 195 -> server(mc, p -> p.gameMode.destroyBlock(trunk));
            case 215 -> shot(mc, "tree_after");
            case 220 -> {
                mc.options.hideGui = false;
                ClientToggles.pressToggle(mc);
            }
            case 230 -> shot(mc, "toggle");
            case 240 -> mc.stop();
            default -> {}
        }
    }
}
//?}
