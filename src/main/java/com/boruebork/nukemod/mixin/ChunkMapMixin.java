package com.boruebork.nukemod.mixin;

import com.boruebork.nukemod.drone.DroneManager;
import com.boruebork.nukemod.entity.custom.AbstractFPVDrone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    @ModifyVariable(method = "move", at = @At("STORE"), ordinal = 1)
    private SectionPos drone$fakeSectionPosForTracking(SectionPos original, ServerPlayer player) {
        AbstractFPVDrone drone = DroneManager.getInstance().getPilotedDrone(player);
        if (drone != null) {
            return SectionPos.of(BlockPos.containing(drone.getX(), drone.getY(), drone.getZ()));
        }
        return original;
    }

    @ModifyVariable(method = "updateChunkTracking", at = @At("STORE"), ordinal = 0)
    private ChunkPos drone$fakeChunkPosForTracking(ChunkPos original, ServerPlayer player) {
        AbstractFPVDrone drone = DroneManager.getInstance().getPilotedDrone(player);
        if (drone != null) {
            return new ChunkPos(drone.blockPosition());
        }
        return original;
    }
}