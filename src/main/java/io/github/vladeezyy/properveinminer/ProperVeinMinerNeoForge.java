package io.github.vladeezyy.properveinminer;

//? if neoforge {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(ProperVeinMiner.MOD_ID)
public class ProperVeinMinerNeoForge {
    public ProperVeinMinerNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        ProperVeinMiner.init();
        // optional: players and servers without the mod can still connect
        modEventBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").optional().playToServer(TogglePayload.TYPE,
                TogglePayload.CODEC, (payload, context) -> PlayerToggles.set((net.minecraft.server.level.ServerPlayer) context.player(), payload.ores(), payload.trees())));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                PlayerToggles.forget(player);
            }
        });
        // datapacks can change the ore and log tags on /reload
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.TagsUpdatedEvent event) -> VeinMiner.invalidate());
        if (System.getProperty("pvm.selftest") != null) {
            SelfTestHooks.neoforge();
        }
    }
}
*///?}
