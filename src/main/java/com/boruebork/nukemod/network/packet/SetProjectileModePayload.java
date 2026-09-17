package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Set;

public record SetProjectileModePayload(int mode) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetProjectileModePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "set_drone_weapon_mode"));

    public static final StreamCodec<ByteBuf, SetProjectileModePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SetProjectileModePayload::mode,
            SetProjectileModePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return null;
    }
}
