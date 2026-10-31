package com.boruebork.nukemod.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Attitude(
        float yRot, float xRot, float roll
) {
    public static final StreamCodec<ByteBuf, Attitude> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, Attitude::yRot,
                    ByteBufCodecs.FLOAT, Attitude::xRot,
                    ByteBufCodecs.FLOAT, Attitude::roll,
                    Attitude::new
            );
}
