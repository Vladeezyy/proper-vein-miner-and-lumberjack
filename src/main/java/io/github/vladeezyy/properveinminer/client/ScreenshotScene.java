package io.github.vladeezyy.properveinminer.client;

import io.github.vladeezyy.properveinminer.ProperVeinMiner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Development check of the client part ({@code -Dpvm.screenshots}): on the title screen it opens the settings
 * screen, checks that the keys are registered, saves {@code screenshots/pvm_config.png} and quits. Prints
 * "[PVM client] PASS/FAIL".
 */
public final class ScreenshotScene {
    private static boolean enabled;
    private static int tick = -1;

    private ScreenshotScene() {}

    public static void enable() {
        enabled = true;
    }

    public static void tick(Minecraft mc) {
        if (!enabled) {
            return;
        }
        if (tick < 0) {
            if (Screens.current(mc) instanceof TitleScreen) {
                tick = 0;
            }
            return;
        }
        tick++;
        if (tick == 20) {
            Screens.open(mc, new PvmConfigScreen(Screens.current(mc)));
        } else if (tick == 60) {
            shot(mc, "pvm_config");
        } else if (tick == 70) {
            boolean keys = java.util.Arrays.asList(mc.options.keyMappings).contains(ClientToggles.TOGGLE);
            boolean screen = Screens.current(mc) instanceof PvmConfigScreen;
            ProperVeinMiner.LOGGER.info("[PVM client] {} keys registered: {}, settings screen open: {} (Minecraft {})", keys && screen ? "PASS" : "FAIL",
                    keys, screen, ProperVeinMiner.MINECRAFT);
        } else if (tick == 80) {
            mc.stop();
        }
    }

    private static void shot(Minecraft mc, String name) {
        //? if >=26.2 {
        /*Screenshot.grab(mc, false);
        *///?} else if >=1.21.6 {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1, msg -> ProperVeinMiner.LOGGER.info("[PVM client] {}", msg.getString()));
        //?} else {
        /*Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> ProperVeinMiner.LOGGER.info("[PVM client] {}", msg.getString()));
        *///?}
    }
}
