package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ExitDronePacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ExitDronePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "exit_drone_packet"));
    public static final StreamCodec<ByteBuf, ExitDronePacket> STREAM_CODEC = StreamCodec.unit(new ExitDronePacket());
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
