package io.github.vladeezyy.properveinminer;

/** Starts the self-test once the server is up (dev only); kept apart so the mod never needs Fabric API. */
final class SelfTestHooks {
    /** Ticks to wait after forcing the test chunks. */
    private static final int DELAY = 60;
    private static int ticks = -1;

    private SelfTestHooks() {}

    private static void started(net.minecraft.server.MinecraftServer server) {
        SelfTest.prepare(server);
        ticks = 0;
    }

    private static void tick(net.minecraft.server.MinecraftServer server) {
        if (ticks >= 0 && ++ticks == DELAY) {
            SelfTest.run(server, player(server));
        }
    }

    //? if fabric {
    private static net.minecraft.server.level.ServerPlayer player(net.minecraft.server.MinecraftServer server) {
        return net.fabricmc.fabric.api.entity.FakePlayer.get(server.overworld());
    }

    static void fabric() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(SelfTestHooks::started);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(SelfTestHooks::tick);
    }
    //?}

    //? if neoforge {
    /*private static net.minecraft.server.level.ServerPlayer player(net.minecraft.server.MinecraftServer server) {
        return net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(server.overworld());
    }

    static void neoforge() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent event) -> started(event.getServer()));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) -> tick(event.getServer()));
    }
    *///?}
}
