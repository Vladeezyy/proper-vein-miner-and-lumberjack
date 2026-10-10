package io.github.vladeezyy.properveinminer;

//? if fabric {
import io.github.vladeezyy.properveinminer.client.ClientToggles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class ProperVeinMinerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricLoader loader = FabricLoader.getInstance();
        //? if >=26.1 {
        /*String keys = "fabric-key-mapping-api-v1";
        *///?} else {
        String keys = "fabric-key-binding-api-v1";
        //?}
        if (loader.isModLoaded(keys) && loader.isModLoaded("fabric-lifecycle-events-v1") && loader.isModLoaded("fabric-networking-api-v1")) {
            Keys.register();
        }
        //? if =1.21.11 {
        if (System.getProperty("pvm.gallery") != null) {
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(io.github.vladeezyy.properveinminer.client.GalleryScene::tick);
        }
        //?}
        if (System.getProperty("pvm.screenshots") != null) {
            io.github.vladeezyy.properveinminer.client.ScreenshotScene.enable();
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(io.github.vladeezyy.properveinminer.client.ScreenshotScene::tick);
        }
    }

    static final class Keys {
        static void register() {
            for (var key : ClientToggles.keys()) {
                //? if >=26.1 {
                /*net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(key);
                *///?} else {
                net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(key);
                //?}
            }
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(ClientToggles::tick);
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientToggles.send());
        }
    }
}
//?}
