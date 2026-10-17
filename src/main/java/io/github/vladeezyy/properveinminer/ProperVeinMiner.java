package io.github.vladeezyy.properveinminer;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared start-up of both loaders. */
public final class ProperVeinMiner {
    public static final String MOD_ID = "properveinminer";
    public static final Logger LOGGER = LoggerFactory.getLogger("Proper Vein Miner");
    public static final String VERSION = /*$ mod_version*/ "1.0.0";
    public static final String MINECRAFT = /*$ minecraft*/ "1.21.11";

    private static PvmConfig config = new PvmConfig();

    private ProperVeinMiner() {}

    public static void init() {
        config = PvmConfig.load();
        LOGGER.info("Proper Vein Miner {} for Minecraft {}: ores {}, trees {}, {}", VERSION, MINECRAFT, config.ores.enabled ? "on" : "off",
                config.trees.enabled ? "on" : "off", config.activation);
    }

    public static PvmConfig config() {
        return config;
    }

    /** Replaces the settings (the settings screen) and saves them. */
    public static void apply(PvmConfig changed) {
        config = changed;
        VeinMiner.invalidate();
        changed.save();
    }

    public static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
