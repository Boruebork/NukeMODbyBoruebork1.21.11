package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record SetPatrolTargetPayload(int droneId, int kind, Vec3 pos, UUID entityId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetPatrolTargetPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "set_patrol_center"));
    public static final StreamCodec<ByteBuf, SetPatrolTargetPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, SetPatrolTargetPayload::droneId,
                    ByteBufCodecs.INT, SetPatrolTargetPayload::kind,
                    Vec3.STREAM_CODEC, SetPatrolTargetPayload::pos,
                    UUIDUtil.STREAM_CODEC, SetPatrolTargetPayload::entityId,
                    SetPatrolTargetPayload::new
            );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
