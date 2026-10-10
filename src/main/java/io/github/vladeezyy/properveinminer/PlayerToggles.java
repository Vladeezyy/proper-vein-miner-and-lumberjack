package io.github.vladeezyy.properveinminer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;

/**
 * Each player's own switches, set by the client's keys ({@link TogglePayload}): the vein miner and the lumberjack can
 * be turned off for one player (to break a single block) while the server config keeps them on.
 */
public final class PlayerToggles {
    private record State(boolean ores, boolean trees) {}

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private PlayerToggles() {}

    public static void set(ServerPlayer player, boolean ores, boolean trees) {
        STATES.put(player.getUUID(), new State(ores, trees));
    }

    public static boolean ores(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state == null || state.ores();
    }

    public static boolean trees(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state == null || state.trees();
    }

    public static void forget(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }
}
