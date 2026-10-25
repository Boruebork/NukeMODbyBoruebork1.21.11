package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.drone.ClientDroneManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record NewFixedWingPayload(int droneId,
                                  float forward,   // -1..1  stick
                                  float pitch,     // -1..1  stick
                                  float roll,      // -1..1  stick (or strafe -> roll, your call)
                                  float yaw,       // -1..1  stick (rudder)
                                  float throttle,
                                  int mode,
                                  int clientTick
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NewFixedWingPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "new_fixed_wing"));
    public static final StreamCodec<ByteBuf, NewFixedWingPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, NewFixedWingPayload::droneId,
                    ByteBufCodecs.FLOAT, NewFixedWingPayload::forward,
                    ByteBufCodecs.FLOAT, NewFixedWingPayload::pitch,
                    ByteBufCodecs.FLOAT, NewFixedWingPayload::roll,
                    ByteBufCodecs.FLOAT, NewFixedWingPayload::yaw,
                    ByteBufCodecs.FLOAT, NewFixedWingPayload::throttle,
                    ByteBufCodecs.INT, NewFixedWingPayload::mode,
                    ByteBufCodecs.INT, NewFixedWingPayload::clientTick,
                    NewFixedWingPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
