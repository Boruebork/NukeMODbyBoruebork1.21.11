package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UavFirePayload(int uavId, float yaw, float pitch) implements CustomPacketPayload {
    public static final Type<UavFirePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "uav_fire"));
    public static final StreamCodec<ByteBuf, UavFirePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UavFirePayload::uavId,
            ByteBufCodecs.FLOAT, UavFirePayload::yaw,
            ByteBufCodecs.FLOAT, UavFirePayload::pitch,
            UavFirePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
