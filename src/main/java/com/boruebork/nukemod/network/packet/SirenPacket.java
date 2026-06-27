package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SirenPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SirenPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "siren_packet"));
    public static StreamCodec<ByteBuf, SirenPacket> STREAM_CODEC = StreamCodec.unit(new SirenPacket());
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
