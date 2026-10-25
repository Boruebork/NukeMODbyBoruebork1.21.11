package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ReconcilePayload(
        int    entityId,        // which drone
        int    inputTick,       // the client's tickCount that produced this state
        double x, double y, double z,     // authoritative position
        double dx, double dy, double dz,  // authoritative velocity (used to seed newVel before replay)
        float  yRot, float xRot, float roll // authoritative attitude
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ReconcilePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "reconcile_uav"));
    public static final StreamCodec<ByteBuf, ReconcilePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ReconcilePayload::entityId,
            ByteBufCodecs.INT, ReconcilePayload::inputTick,
            ByteBufCodecs.DOUBLE, ReconcilePayload::x,
            ByteBufCodecs.DOUBLE, ReconcilePayload::y,
            ByteBufCodecs.DOUBLE, ReconcilePayload::z,
            ByteBufCodecs.DOUBLE, ReconcilePayload::dx,
            ByteBufCodecs.DOUBLE, ReconcilePayload::dy,
            ByteBufCodecs.DOUBLE, ReconcilePayload::dz,
            ByteBufCodecs.FLOAT, ReconcilePayload::yRot,
            ByteBufCodecs.FLOAT, ReconcilePayload::xRot,
            ByteBufCodecs.FLOAT, ReconcilePayload::roll,
            ReconcilePayload::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

