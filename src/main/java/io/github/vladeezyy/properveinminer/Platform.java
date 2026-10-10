package io.github.vladeezyy.properveinminer;

import java.nio.file.Path;

/** What differs between the loaders: where the config lives. */
public final class Platform {
    private Platform() {}

    public static Path configDir() {
        //? if fabric {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        //?} else {
        /*return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        *///?}
    }
}
