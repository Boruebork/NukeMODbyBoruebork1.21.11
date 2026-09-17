package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DroneLaucnhProjectilePayload() implements CustomPacketPayload {
    public static final Type<DroneLaucnhProjectilePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "drone_attack"));
    public static final StreamCodec<ByteBuf,DroneLaucnhProjectilePayload> STREAM_CODEC = StreamCodec.unit(new DroneLaucnhProjectilePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
