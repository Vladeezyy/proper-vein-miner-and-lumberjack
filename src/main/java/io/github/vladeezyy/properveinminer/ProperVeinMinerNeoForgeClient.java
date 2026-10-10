package io.github.vladeezyy.properveinminer;

//? if neoforge {
/*import io.github.vladeezyy.properveinminer.client.ClientToggles;
import io.github.vladeezyy.properveinminer.client.PvmConfigScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ProperVeinMiner.MOD_ID, dist = Dist.CLIENT)
public class ProperVeinMinerNeoForgeClient {
    public ProperVeinMinerNeoForgeClient(IEventBus modEventBus, ModContainer modContainer) {
        // the Config button in the mod list
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> new PvmConfigScreen(parent));
        modEventBus.addListener((RegisterKeyMappingsEvent event) -> {
            for (var key : ClientToggles.keys()) {
                event.register(key);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ClientToggles.tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> ClientToggles.send());
        if (System.getProperty("pvm.screenshots") != null) {
            io.github.vladeezyy.properveinminer.client.ScreenshotScene.enable();
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> io.github.vladeezyy.properveinminer.client.ScreenshotScene.tick(Minecraft.getInstance()));
        }
    }
}
*///?}
