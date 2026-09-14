package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DroneInputPayload(int droneId, float forward, float dz, boolean up, boolean down, float xRot, float yRot)
        implements CustomPacketPayload {
    public static final Type<DroneInputPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "drone_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DroneInputPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, DroneInputPayload::droneId,
                    ByteBufCodecs.FLOAT, DroneInputPayload::forward,
                    ByteBufCodecs.FLOAT, DroneInputPayload::dz,
                    ByteBufCodecs.BOOL, DroneInputPayload::up,
                    ByteBufCodecs.BOOL, DroneInputPayload::down,
                    ByteBufCodecs.FLOAT, DroneInputPayload::xRot,
                    ByteBufCodecs.FLOAT, DroneInputPayload::yRot,
                    DroneInputPayload::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
