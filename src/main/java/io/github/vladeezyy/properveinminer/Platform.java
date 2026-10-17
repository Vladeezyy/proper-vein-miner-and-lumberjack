package io.github.vladeezyy.properveinminer;

import java.nio.file.Path;

/** What differs between the loaders: where the config lives, what a fake player is. */
public final class Platform {
    private Platform() {}

    public static Path configDir() {
        //? if fabric {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        //?} else {
        /*return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        *///?}
    }

    //? if fabric {
    /** Fabric API's fake player class, if Fabric API is there (it is optional). */
    private static final Class<?> FAKE_PLAYER = fakePlayerClass();

    private static Class<?> fakePlayerClass() {
        try {
            return Class.forName("net.fabricmc.fabric.api.entity.FakePlayer");
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }
    //?}

    /** A machine of another mod breaking blocks as a player (a turtle, a block breaker). */
    public static boolean isFakePlayer(net.minecraft.server.level.ServerPlayer player) {
        //? if fabric {
        return FAKE_PLAYER != null && FAKE_PLAYER.isInstance(player);
        //?} else {
        /*return player instanceof net.neoforged.neoforge.common.util.FakePlayer;
        *///?}
    }
}
