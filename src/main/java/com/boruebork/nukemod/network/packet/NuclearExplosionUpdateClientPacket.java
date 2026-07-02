package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.explosion.NuclearExplosionPhase;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record NuclearExplosionUpdateClientPacket(UUID id, Vec3i pos, NuclearExplosionPhase phase, int ticksSinceStartOfPhase) implements CustomPacketPayload {
    public static final  CustomPacketPayload.Type<NuclearExplosionUpdateClientPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "flash_packet"));

    public static final StreamCodec<FriendlyByteBuf, NuclearExplosionUpdateClientPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            NuclearExplosionUpdateClientPacket::id,
            Vec3i.STREAM_CODEC,
            NuclearExplosionUpdateClientPacket::pos,
            NuclearExplosionPhase.STREAM_CODEC,
            NuclearExplosionUpdateClientPacket::phase,
            ByteBufCodecs.INT,
            NuclearExplosionUpdateClientPacket::ticksSinceStartOfPhase,
            NuclearExplosionUpdateClientPacket::new//the constructor call for the record
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
