package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record TargetSelectedPacket(BlockPos pos, UUID target) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TargetSelectedPacket> TYPE = new Type<>(NukeModbyBoruebork.identifierFromPath("target_selected_packet"));

    public static final StreamCodec<ByteBuf, TargetSelectedPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TargetSelectedPacket::pos,
            UUIDUtil.STREAM_CODEC,
            TargetSelectedPacket::target,
            TargetSelectedPacket::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
