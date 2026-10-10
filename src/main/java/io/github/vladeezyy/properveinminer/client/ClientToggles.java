package io.github.vladeezyy.properveinminer.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.vladeezyy.properveinminer.ProperVeinMiner;
import io.github.vladeezyy.properveinminer.TogglePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * The keys: one switches the vein miner and the lumberjack off and on (to break a single block), two more (unbound)
 * switch each alone. The state goes to the server, which applies it to this player only.
 */
public final class ClientToggles {
    //? if >=1.21.9 {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(ProperVeinMiner.id(ProperVeinMiner.MOD_ID, "main"));
    //?} else {
    /*private static final String CATEGORY = "key.categories.properveinminer";
    *///?}

    public static final KeyMapping TOGGLE = new KeyMapping("key.properveinminer.toggle", InputConstants.KEY_GRAVE, CATEGORY);
    public static final KeyMapping TOGGLE_ORES = new KeyMapping("key.properveinminer.toggle_ores", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping TOGGLE_TREES = new KeyMapping("key.properveinminer.toggle_trees", InputConstants.UNKNOWN.getValue(), CATEGORY);

    private static boolean ores = true;
    private static boolean trees = true;

    private ClientToggles() {}

    public static KeyMapping[] keys() {
        return new KeyMapping[] {TOGGLE, TOGGLE_ORES, TOGGLE_TREES};
    }

    /** Every client tick: handles key presses. */
    public static void tick(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        boolean changed = false;
        while (TOGGLE.consumeClick()) {
            toggleBoth();
            changed = true;
        }
        while (TOGGLE_ORES.consumeClick()) {
            ores = !ores;
            changed = true;
        }
        while (TOGGLE_TREES.consumeClick()) {
            trees = !trees;
            changed = true;
        }
        if (changed) {
            send();
            overlay(mc, PvmConfigScreen.tr("properveinminer.status", state(ores), state(trees)));
        }
    }

    /** The main key: both off, or both on again. */
    static void toggleBoth() {
        boolean on = !(ores || trees);
        ores = on;
        trees = on;
    }

    /** As if the main key was pressed (the gallery scene). */
    public static void pressToggle(Minecraft mc) {
        toggleBoth();
        send();
        overlay(mc, PvmConfigScreen.tr("properveinminer.status", state(ores), state(trees)));
    }

    private static Component state(boolean on) {
        return PvmConfigScreen.tr(on ? "properveinminer.on" : "properveinminer.off");
    }

    private static void overlay(Minecraft mc, Component message) {
        //? if >=26.1 {
        /*mc.player.sendOverlayMessage(message);
        *///?} else {
        mc.player.displayClientMessage(message, true);
        //?}
    }

    /** Tells the server (on joining and after a key), if it has the mod. */
    public static void send() {
        ClientNetwork.send(new TogglePayload(ores, trees));
    }

    public static boolean ores() {
        return ores;
    }

    public static boolean trees() {
        return trees;
    }
}
