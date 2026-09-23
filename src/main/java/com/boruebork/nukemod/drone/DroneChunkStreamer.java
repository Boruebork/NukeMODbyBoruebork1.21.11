package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.entity.custom.AbstractFPVDrone;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;

public class DroneChunkStreamer {
    private static final int STREAM_RADIUS = 6; // chunks — keep modest, this is real bandwidth per chunk
    private final Map<UUID, Set<ChunkPos>> streamedChunks = new HashMap<>();
    private Map<UUID, ChunkPos> lastStreamedCenter = new HashMap<>();
    public void onExit(ServerPlayer player) {
        ChunkPos realPos = new ChunkPos(player.blockPosition());

        player.connection.send(new ClientboundSetChunkCacheCenterPacket(realPos.x, realPos.z));
        lastStreamedCenter.remove(player.getUUID());

        // forget leftover manually-streamed drone chunks that vanilla doesn't independently track,
        // so they don't linger as stale entries occupying ring-buffer slots
        Set<ChunkPos> current = streamedChunks.remove(player.getUUID());
        if (current != null) {
            for (ChunkPos pos : current) {
                if (!isVanillaTracked(player, pos)) {
                    player.connection.send(new ClientboundForgetLevelChunkPacket(pos));
                }
            }
        }

        resendChunksAround(player, realPos, STREAM_RADIUS);
    }

    private void resendChunksAround(ServerPlayer player, ChunkPos center, int radius) {
        ServerLevel level = (ServerLevel) player.level();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos pos = new ChunkPos(center.x + dx, center.z + dz);
                LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
                if (chunk != null) {
                    player.connection.send(new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null));
                }
            }
        }
    }

    private boolean isVanillaTracked(ServerPlayer player, ChunkPos pos) {
        return player.getChunkTrackingView().contains(pos);
    }
}
