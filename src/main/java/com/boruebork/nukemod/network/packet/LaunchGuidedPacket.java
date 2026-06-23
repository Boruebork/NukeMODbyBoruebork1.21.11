package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.block.entity.GuidedMissileLauncherBE;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FriendlyByteBufUtil;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.UUID;

public record LaunchGuidedPacket(UUID targetUUID) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LaunchGuidedPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID,
            "launch_guided_packet"));
    public static final StreamCodec<FriendlyByteBuf, LaunchGuidedPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    LaunchGuidedPacket::targetUUID,
                    LaunchGuidedPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
