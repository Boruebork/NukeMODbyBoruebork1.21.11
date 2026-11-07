package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.uav.flight.Sticks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record FixedWIngInputPayload(UUID aircraftId,
                                    Sticks sticks,
                                    int clientTick
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FixedWIngInputPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "new_fixed_wing"));
    public static final StreamCodec<ByteBuf, FixedWIngInputPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, FixedWIngInputPayload::aircraftId,
                    Sticks.STREAM_CODEC, FixedWIngInputPayload::sticks,
                    ByteBufCodecs.INT, FixedWIngInputPayload::clientTick,
                    FixedWIngInputPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
