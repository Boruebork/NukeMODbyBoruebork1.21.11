package com.boruebork.nukemod.network.packet.patrol;

import com.boruebork.nukemod.NukeModbyBoruebork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChangePilotMode(int mode) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChangePilotMode> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "changed_pilot_mode"));
    public static final StreamCodec<ByteBuf, ChangePilotMode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ChangePilotMode::mode,
            ChangePilotMode::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return null;
    }
}
