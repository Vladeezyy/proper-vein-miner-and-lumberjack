package io.github.vladeezyy.properveinminer;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the player's own vein miner / lumberjack switches (sent on joining and when a key is pressed). */
public record TogglePayload(boolean ores, boolean trees) implements CustomPacketPayload {
    public static final Type<TogglePayload> TYPE = new Type<>(ProperVeinMiner.id(ProperVeinMiner.MOD_ID, "toggle"));
    public static final StreamCodec<ByteBuf, TogglePayload> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, TogglePayload::ores,
            ByteBufCodecs.BOOL, TogglePayload::trees, TogglePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
