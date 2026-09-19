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
    public void update(ServerPlayer player, AbstractFPVDrone drone) {
        ServerLevel level = (ServerLevel) drone.level();
        ChunkPos center = new ChunkPos(drone.blockPosition());
        if (!center.equals(lastStreamedCenter.get(player.getUUID()))) {
            // this is the actual hack: tell this ONE client its view center is the drone's chunk,
            // completely independent of the player's real (stationary) position
            player.connection.send(new ClientboundSetChunkCacheCenterPacket(center.x, center.z));
            lastStreamedCenter.put(player.getUUID(), center);
        }
        Set<ChunkPos> desired = new HashSet<>();
        for (int dx = -STREAM_RADIUS; dx <= STREAM_RADIUS; dx++) {
            for (int dz = -STREAM_RADIUS; dz <= STREAM_RADIUS; dz++) {
                desired.add(new ChunkPos(center.x + dx, center.z + dz));
            }
        }

        Set<ChunkPos> current = streamedChunks.computeIfAbsent(player.getUUID(), k -> new HashSet<>());

        // stop streaming chunks no longer in range — but never touch a chunk vanilla already owns for this player
        current.removeIf(pos -> {
            if (!desired.contains(pos) && !isVanillaTracked(player, pos)) {
                player.connection.send(new ClientboundForgetLevelChunkPacket(pos));
                return true;
            }
            return false;
        });

        // stream newly-in-range chunks — skip anything vanilla is already sending this player
        for (ChunkPos pos : desired) {
            if (current.contains(pos) || isVanillaTracked(player, pos)) continue;

            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk == null) continue; // not loaded yet this tick — ticket should catch up shortly, retry next pass

            player.connection.send(new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null));
            current.add(pos);
        }
        resendChunksAround(player, player.chunkPosition(), player.level().getServer().getPlayerList().getViewDistance());
    }

    public void clearFor(ServerPlayer player) {
        Set<ChunkPos> current = streamedChunks.remove(player.getUUID());
        if (current == null) return;
        for (ChunkPos pos : current) {
            if (!isVanillaTracked(player, pos)) {
                player.connection.send(new ClientboundForgetLevelChunkPacket(pos));
            }
        }
    }
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
