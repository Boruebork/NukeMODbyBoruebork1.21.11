package com.boruebork.nukemod.explosion.client.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FlashPacket(int flashTime) implements CustomPacketPayload {
    public static final Type<FlashPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "flash_packet"));

    public static final StreamCodec<FriendlyByteBuf, FlashPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            FlashPacket::flashTime,
            FlashPacket::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
