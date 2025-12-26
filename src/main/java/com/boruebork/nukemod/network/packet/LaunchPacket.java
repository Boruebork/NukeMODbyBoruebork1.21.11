package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LaunchPacket(int x, int y, int z) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LaunchPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "launch"));

    public static final StreamCodec<FriendlyByteBuf, LaunchPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            LaunchPacket::x,
            ByteBufCodecs.INT,
            LaunchPacket::y,
            ByteBufCodecs.INT,
            LaunchPacket::z,
            LaunchPacket::new//the constructor call for the record
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
