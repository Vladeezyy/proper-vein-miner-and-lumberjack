package io.github.vladeezyy.properveinminer;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class ProperVeinMinerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ProperVeinMiner.init();
        // the keys' switches need Fabric API's networking; without it the mod still works (sneak for one block)
        if (FabricLoader.getInstance().isModLoaded("fabric-networking-api-v1")) {
            Networking.register();
        }
        // datapacks can change the ore and log tags on /reload
        if (FabricLoader.getInstance().isModLoaded("fabric-lifecycle-events-v1")) {
            Lifecycle.register();
        }
        if (System.getProperty("pvm.selftest") != null) {
            SelfTestHooks.fabric();
        }
    }

    /** Kept apart so Fabric API classes are only loaded when it is there. */
    static final class Lifecycle {
        static void register() {
            net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> VeinMiner.invalidate());
        }
    }

    /** Kept apart so Fabric API classes are only loaded when it is there. */
    static final class Networking {
        static void register() {
            //? if >=26.1 {
            /*net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(TogglePayload.TYPE, TogglePayload.CODEC);
            *///?} else {
            net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(TogglePayload.TYPE, TogglePayload.CODEC);
            //?}
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(TogglePayload.TYPE,
                    (payload, context) -> PlayerToggles.set(context.player(), payload.ores(), payload.trees()));
            net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PlayerToggles.forget(handler.player));
        }
    }
}
//?}
