package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ReconcilePayload(
        int entityId,
        int inputTick,
        Motion motion,
        Attitude attitude,
        float throttle,
        float groundPitchRate,
        boolean onGround
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ReconcilePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "reconcile_uav"));
    public static final StreamCodec<ByteBuf, ReconcilePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, ReconcilePayload::entityId,
                    ByteBufCodecs.INT, ReconcilePayload::inputTick,
                    Motion.STREAM_CODEC, ReconcilePayload::motion,
                    Attitude.STREAM_CODEC, ReconcilePayload::attitude,
                    ByteBufCodecs.FLOAT, ReconcilePayload::throttle,
                    ByteBufCodecs.FLOAT, ReconcilePayload::groundPitchRate,
                    ByteBufCodecs.BOOL, ReconcilePayload::onGround,
                    ReconcilePayload::new
            );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

