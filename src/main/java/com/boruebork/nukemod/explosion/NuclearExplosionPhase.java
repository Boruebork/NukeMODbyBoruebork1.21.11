package com.boruebork.nukemod.explosion;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

import java.util.function.IntFunction;

public enum NuclearExplosionPhase {
    FLASH(0),
    SHOCKWAVE(1),
    MUSHROOM_AND_RADIATION(2);
    private final int id;

    // Create a fast ID lookup map using Vanilla's helper
    private static final IntFunction<NuclearExplosionPhase> BY_ID = ByIdMap.continuous(
            NuclearExplosionPhase::getId,
            values(),
            ByIdMap.OutOfBoundsStrategy.ZERO
    );

    // The resulting StreamCodec for network packets
    public static final StreamCodec<ByteBuf, NuclearExplosionPhase> STREAM_CODEC = ByteBufCodecs.idMapper(
            BY_ID,
            NuclearExplosionPhase::getId
    );

    NuclearExplosionPhase(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}



