package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record NotifyClientDroneExit() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NotifyClientDroneExit> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "notify_client_exit_drone"));
    public static final StreamCodec<ByteBuf, NotifyClientDroneExit> STREAM_CODEC = StreamCodec.unit(new NotifyClientDroneExit());
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
