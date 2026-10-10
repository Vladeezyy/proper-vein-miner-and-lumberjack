package io.github.vladeezyy.properveinminer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**
 * {@code config/properveinminer.json}. Unknown or missing values keep their defaults; the file is written back with
 * every option so new ones show up after an update.
 */
public final class PvmConfig {
    /** When the vein miner works: "unless_sneaking" (sneak to break one block), "while_sneaking", "always". */
    public String activation = "unless_sneaking";
    /** Never use the last durability point: the vein stops before the tool would break. */
    public boolean protect_tool = true;
    /** Move the drops and experience of a vein to the block that was broken first. */
    public boolean gather_drops = true;
    /** Also in creative mode. */
    public boolean in_creative = false;
    public Ores ores = new Ores();
    public Trees trees = new Trees();

    public static final class Ores {
        public boolean enabled = true;
        /** Most blocks broken at once (the first included). */
        public int max_blocks = 128;
        /** Deepslate and normal variants of an ore (c:ores/iron, ...) count as one vein. */
        public boolean mix_variants = true;
        /** Block ids that are ores too (e.g. "somemod:ruby_cluster"), and ones that never are. */
        public List<String> extra_blocks = new ArrayList<>();
        public List<String> excluded_blocks = new ArrayList<>();
    }

    public static final class Trees {
        public boolean enabled = true;
        public int max_blocks = 256;
        /** Only fell trees: logs with natural (not player placed) leaves or nether wart blocks next to them. */
        public boolean require_leaves = true;
        /** Never break logs below the one you cut (keeps log floors and the stump of a tree you stand on). */
        public boolean only_upwards = true;
        public List<String> extra_blocks = new ArrayList<>();
        public List<String> excluded_blocks = new ArrayList<>();
    }

    static Path file() {
        return Platform.configDir().resolve("properveinminer.json");
    }

    /** Writes the settings (from the settings screen). */
    public void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ProperVeinMiner.LOGGER.warn("Could not write {}", file(), e);
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static PvmConfig load() {
        Path file = file();
        PvmConfig config = new PvmConfig();
        try {
            if (Files.exists(file)) {
                PvmConfig read = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), PvmConfig.class);
                if (read != null) {
                    config = read;
                    if (config.ores == null) config.ores = new Ores();
                    if (config.trees == null) config.trees = new Trees();
                }
            }
        } catch (IOException | JsonParseException e) {
            ProperVeinMiner.LOGGER.error("Could not read {}, using the defaults", file, e);
            return config;
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(config), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ProperVeinMiner.LOGGER.warn("Could not write {}", file, e);
        }
        return config;
    }
}
