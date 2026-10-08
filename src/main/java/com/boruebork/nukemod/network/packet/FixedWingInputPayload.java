package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FixedWingInputPayload(int droneId, float forward, float dz, boolean up, boolean down, float xRotW, float yRotW, float xRot, float yRot, float roll, float rollW) implements CustomPacketPayload {
    public static final Type<FixedWingInputPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "fixed_wing_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FixedWingInputPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, FixedWingInputPayload::droneId,
                    ByteBufCodecs.FLOAT, FixedWingInputPayload::forward,
                    ByteBufCodecs.FLOAT, FixedWingInputPayload::dz,
                    ByteBufCodecs.BOOL, FixedWingInputPayload::up,
                    ByteBufCodecs.BOOL, FixedWingInputPayload::down,
                    ByteBufCodecs.FLOAT, FixedWingInputPayload::xRotW,
                    ByteBufCodecs.FLOAT, FixedWingInputPayload::yRotW,
                    ByteBufCodecs.FLOAT, FixedWingInputPayload::xRot,
                    ByteBufCodecs.FLOAT, FixedWingInputPayload::yRot,
                    ByteBufCodecs.FLOAT, FixedWingInputPayload::roll,
                    ByteBufCodecs.FLOAT, FixedWingInputPayload::rollW,
                    FixedWingInputPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
