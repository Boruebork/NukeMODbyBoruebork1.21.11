package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FlashPacket(int x, int y, int z) implements CustomPacketPayload {
    public static final  CustomPacketPayload.Type<FlashPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "flash_packet"));

    public static final StreamCodec<FriendlyByteBuf, FlashPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            FlashPacket::x,
            ByteBufCodecs.INT,
            FlashPacket::y,
            ByteBufCodecs.INT,
            FlashPacket::z,
            FlashPacket::new//the constructor call for the record
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
