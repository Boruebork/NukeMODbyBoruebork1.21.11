package com.boruebork.nukemod.mixin;

import com.boruebork.nukemod.drone.DroneManager;
import com.boruebork.nukemod.drone.AbstractDrone;
import com.boruebork.nukemod.drone.PilotHooks;
import com.boruebork.nukemod.drone.Pilotable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.EntityAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static com.boruebork.nukemod.drone.DroneManager.getInstance;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    @Redirect(
            method = {"move", "updatePlayerPos"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/SectionPos;of(Lnet/minecraft/world/level/entity/EntityAccess;)Lnet/minecraft/core/SectionPos;"
            )
    )
    private SectionPos drone$fakeSectionPosForTracking(EntityAccess entity) {
        if (entity instanceof ServerPlayer player) {
            Pilotable v = getInstance().getPiloted(player);
            if (v != null) {
                return SectionPos.of(BlockPos.containing(v.asEntity().getX(), v.asEntity().getY(), v.asEntity().getZ()));
            }
        }
        return SectionPos.of(entity);
    }

    @Redirect(
            method = "updateChunkTracking",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;chunkPosition()Lnet/minecraft/world/level/ChunkPos;"
            )
    )
    private ChunkPos drone$fakeChunkPosForTrackingView(ServerPlayer player) {
        Pilotable drone = getInstance().getPiloted(player);
        if (drone != null) {
            return new ChunkPos(drone.asEntity().blockPosition());
        }
        return player.chunkPosition();
    }
    @Inject(
            method = "collectSpawningChunks",
            at = @At("TAIL")
    )
    private void droneforce$addDroneChunks(List<LevelChunk> output, CallbackInfo ci) {
        ChunkMap self = (ChunkMap) (Object) this;

        for (var it = getInstance().pilotedDrones().iterator(); it.hasNext();) {
            long packedPos = it.next().chunkPosition().toLong();
            ChunkHolder holder = self.getVisibleChunkIfPresent(packedPos);
            if (holder == null) continue;

            LevelChunk chunk = holder.getTickingChunk();
            if (chunk != null && !output.contains(chunk)) {
                output.add(chunk);
            }
        }
    }
    @Inject(
            method = "playerIsCloseEnoughForSpawning",
            at = @At("HEAD"),
            cancellable = true
    )
    private void droneforce$spawnCheckFromDrone(
            ServerPlayer player,
            ChunkPos chunkPos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (player.isSpectator()) {
            cir.setReturnValue(false);
            return;
        }

        Pilotable drone = getInstance().getPiloted(player);
        if (drone == null) {
            return; // vanilla decides
        }

        double cx = chunkPos.getMinBlockX() + 8.0;
        double cz = chunkPos.getMinBlockZ() + 8.0;
        double dx = cx - drone.asEntity().getX();
        double dz = cz - drone.asEntity().getZ();
        cir.setReturnValue(dx * dx + dz * dz < 16384.0); // 128^2
    }
    @Shadow
    @Final
    ServerLevel level;
    @Inject(method = "tick", at = @At("HEAD"))
    private void droneforce$retrackEntitiesForPilots(CallbackInfo ci) {
        ChunkMap self = (ChunkMap) (Object) this;
        for (ServerPlayer player : level.players()) {
            Pilotable drone = DroneManager.getInstance().getPiloted(player);
            if (drone != null) {
                self.move(player);
            }
        }
    }
}