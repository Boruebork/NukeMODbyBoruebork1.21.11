package com.boruebork.nukemod.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Motion(
        double x, double y, double z,
        double dx, double dy, double dz
) {
    public static final StreamCodec<ByteBuf, Motion> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, Motion::x,
                    ByteBufCodecs.DOUBLE, Motion::y,
                    ByteBufCodecs.DOUBLE, Motion::z,
                    ByteBufCodecs.DOUBLE, Motion::dx,
                    ByteBufCodecs.DOUBLE, Motion::dy,
                    ByteBufCodecs.DOUBLE, Motion::dz,
                    Motion::new
            );
}
