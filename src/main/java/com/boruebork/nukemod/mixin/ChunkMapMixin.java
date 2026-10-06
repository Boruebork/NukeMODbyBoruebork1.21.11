package com.boruebork.nukemod.mixin;

import com.boruebork.nukemod.drone.DroneManager;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractDrone;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVDrone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
            AbstractDrone drone = DroneManager.getInstance().getPilotedDrone(player);
            if (drone != null) {
                return SectionPos.of(BlockPos.containing(drone.getX(), drone.getY(), drone.getZ()));
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
        AbstractDrone drone = DroneManager.getInstance().getPilotedDrone(player);
        if (drone != null) {
            return new ChunkPos(drone.blockPosition());
        }
        return player.chunkPosition();
    }
}