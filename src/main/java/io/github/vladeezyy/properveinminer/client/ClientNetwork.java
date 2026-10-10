package io.github.vladeezyy.properveinminer.client;

import io.github.vladeezyy.properveinminer.TogglePayload;

/** Sends the switches when the server knows them (a server without the mod is left alone). */
final class ClientNetwork {
    private ClientNetwork() {}

    static void send(TogglePayload payload) {
        //? if fabric {
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("fabric-networking-api-v1")
                && net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(TogglePayload.TYPE)) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
        }
        //?} else {
        /*var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection != null && connection.hasChannel(TogglePayload.TYPE)) {
            connection.send(new net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket(payload));
        }
        *///?}
    }
}
