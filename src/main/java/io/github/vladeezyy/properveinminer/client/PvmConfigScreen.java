package io.github.vladeezyy.properveinminer.client;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.vladeezyy.properveinminer.ProperVeinMiner;
import io.github.vladeezyy.properveinminer.PvmConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

/**
 * Every setting in one screen (from Mod Menu or NeoForge's mod list): the general options on top, the vein miner on
 * the left, the lumberjack on the right, and a button to the keys. Only vanilla widgets, so it looks and works the same
 * on every version. Every widget is full column width so no text is cut, and every text falls back to English when the
 * mod's language files are not loaded (Fabric without Fabric API). On a dedicated server the server's config decides;
 * this edits this game's.
 */
public class PvmConfigScreen extends Screen {
    private static final int ROW = 22;
    private static final Integer[] MAX_BLOCKS = {8, 16, 32, 64, 128, 256, 512, 1024};
    private static Map<String, String> english;

    private final Screen parent;
    private final PvmConfig config;

    public PvmConfigScreen(Screen parent) {
        super(t("title"));
        this.parent = parent;
        this.config = copy(ProperVeinMiner.config());
    }

    private static PvmConfig copy(PvmConfig from) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(from), PvmConfig.class);
    }

    /** The bundled English texts, used when the game has no translation for a key. */
    static String english(String key) {
        if (english == null) {
            english = new HashMap<>();
            try (InputStream in = PvmConfigScreen.class.getResourceAsStream("/assets/properveinminer/lang/en_us.json")) {
                if (in != null) {
                    english = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), new TypeToken<Map<String, String>>() {}.getType());
                }
            } catch (Exception e) {
                ProperVeinMiner.LOGGER.warn("Could not read the English texts", e);
            }
        }
        return english.getOrDefault(key, key);
    }

    static Component tr(String key, Object... args) {
        return Component.translatableWithFallback(key, english(key), args);
    }

    private static Component t(String key) {
        return tr("properveinminer.config." + key);
    }

    private CycleButton<Boolean> toggle(int x, int y, int w, String key, boolean value, Consumer<Boolean> set) {
        Function<Boolean, Component> names = v -> tr(v ? "properveinminer.on" : "properveinminer.off");
        return this.cycle(x, y, w, key, names, value, new Boolean[] {true, false}, set);
    }

    private <T> CycleButton<T> cycle(int x, int y, int w, String key, Function<T, Component> names, T value, T[] values, Consumer<T> set) {
        //? if >=1.21.11 {
        CycleButton<T> button = CycleButton.builder(names, value).withValues(values).create(x, y, w, 20, t(key), (b, v) -> set.accept(v));
        //?} else {
        /*CycleButton<T> button = CycleButton.builder(names).withValues(values).withInitialValue(value).create(x, y, w, 20, t(key), (b, v) -> set.accept(v));
        *///?}
        button.setTooltip(Tooltip.create(t(key + ".tip")));
        return this.addRenderableWidget(button);
    }

    /** A line of text, centred in (x, w) or at x; the widget is exactly as wide as the text (its own alignment differs between versions). */
    private void label(int x, int y, int w, Component text, boolean centred) {
        int width = this.font.width(text);
        this.addRenderableWidget(new StringWidget(centred ? x + (w - width) / 2 : x, y, width, 12, text, this.font));
    }

    private void heading(int x, int y, int w, Component text) {
        this.label(x, y, w, text.copy().withStyle(net.minecraft.ChatFormatting.YELLOW), true);
    }

    /** The nearest of the offered values, so a hand-edited config still shows up. */
    private static Integer nearest(int value) {
        Integer best = MAX_BLOCKS[0];
        for (Integer v : MAX_BLOCKS) {
            if (Math.abs(v - value) < Math.abs(best - value)) {
                best = v;
            }
        }
        return best;
    }

    /** A labelled text field for a list of block ids. */
    private void list(int x, int y, int w, String key, List<String> value, Consumer<List<String>> set) {
        this.label(x, y, w, t(key).copy().withStyle(net.minecraft.ChatFormatting.GRAY), false);
        EditBox box = new EditBox(this.font, x, y + 12, w, 20, t(key));
        box.setMaxLength(2000);
        box.setValue(String.join(", ", value));
        box.setResponder(s -> {
            List<String> ids = new ArrayList<>();
            Arrays.stream(s.split(",")).map(String::trim).filter(v -> !v.isEmpty()).forEach(ids::add);
            set.accept(ids);
        });
        box.setTooltip(Tooltip.create(t(key + ".tip")));
        this.addRenderableWidget(box);
    }

    @Override
    protected void init() {
        int column = Math.min(180, (this.width - 30) / 2);
        int left = this.width / 2 - column - 5;
        int right = this.width / 2 + 5;
        int both = column * 2 + 10;
        int y = 6;
        this.label(left, y, both, this.title.copy().withStyle(net.minecraft.ChatFormatting.WHITE, net.minecraft.ChatFormatting.BOLD), true);
        y += 16;

        // general
        this.heading(left, y, both, t("general"));
        y += 13;
        this.cycle(left, y, column, "activation", v -> t("activation." + v), this.config.activation,
                new String[] {"unless_sneaking", "while_sneaking", "always"}, v -> this.config.activation = v);
        this.toggle(right, y, column, "protect_tool", this.config.protect_tool, v -> this.config.protect_tool = v);
        y += ROW;
        this.toggle(left, y, column, "gather_drops", this.config.gather_drops, v -> this.config.gather_drops = v);
        this.toggle(right, y, column, "in_creative", this.config.in_creative, v -> this.config.in_creative = v);
        y += ROW + 6;

        // vein miner | lumberjack
        this.heading(left, y, column, t("ores"));
        this.heading(right, y, column, t("trees"));
        y += 13;
        PvmConfig.Ores ores = this.config.ores;
        PvmConfig.Trees trees = this.config.trees;
        this.toggle(left, y, column, "ores.enabled", ores.enabled, v -> ores.enabled = v);
        this.toggle(right, y, column, "trees.enabled", trees.enabled, v -> trees.enabled = v);
        y += ROW;
        this.cycle(left, y, column, "max_blocks", v -> Component.literal(String.valueOf(v)), nearest(ores.max_blocks), MAX_BLOCKS, v -> ores.max_blocks = v);
        this.cycle(right, y, column, "max_blocks", v -> Component.literal(String.valueOf(v)), nearest(trees.max_blocks), MAX_BLOCKS, v -> trees.max_blocks = v);
        y += ROW;
        this.toggle(left, y, column, "mix_variants", ores.mix_variants, v -> ores.mix_variants = v);
        this.toggle(right, y, column, "require_leaves", trees.require_leaves, v -> trees.require_leaves = v);
        y += ROW;
        this.toggle(right, y, column, "only_upwards", trees.only_upwards, v -> trees.only_upwards = v);
        y += ROW + 2;
        this.list(left, y, column, "extra_ores", ores.extra_blocks, v -> ores.extra_blocks = v);
        this.list(right, y, column, "extra_logs", trees.extra_blocks, v -> trees.extra_blocks = v);
        y += ROW + 14;
        this.list(left, y, column, "excluded_ores", ores.excluded_blocks, v -> ores.excluded_blocks = v);
        this.list(right, y, column, "excluded_logs", trees.excluded_blocks, v -> trees.excluded_blocks = v);
        y += ROW + 14;

        int bottom = Math.max(y + 4, this.height - 26);
        this.addRenderableWidget(Button.builder(t("controls"), b -> Screens.open(this.minecraft, new KeyBindsScreen(this, this.minecraft.options)))
                .bounds(left, bottom, column, 20).tooltip(Tooltip.create(t("controls.tip"))).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(right, bottom, column, 20).build());
    }

    @Override
    public void onClose() {
        ProperVeinMiner.apply(this.config);
        Screens.open(this.minecraft, this.parent);
    }
}
