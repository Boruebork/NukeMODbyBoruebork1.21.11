package com.boruebork.nukemod.uav.flight;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Sticks(float throttle, float pitch, float roll, float yaw) {
    public static final Sticks ZERO = new Sticks(0, 0, 0, 0);
    public static final StreamCodec<ByteBuf, Sticks> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Sticks::throttle,
            ByteBufCodecs.FLOAT, Sticks::pitch,
            ByteBufCodecs.FLOAT, Sticks::roll,
            ByteBufCodecs.FLOAT, Sticks::yaw,
            Sticks::new
    );
}
