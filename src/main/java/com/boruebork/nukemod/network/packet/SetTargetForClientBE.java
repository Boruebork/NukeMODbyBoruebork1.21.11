package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record SetTargetForClientBE(BlockPos pos, UUID id) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetTargetForClientBE> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "set_target_for_client_guided_be"));

    public static final StreamCodec<ByteBuf, SetTargetForClientBE> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SetTargetForClientBE::pos,
            UUIDUtil.STREAM_CODEC,
            SetTargetForClientBE::id,
            SetTargetForClientBE::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
