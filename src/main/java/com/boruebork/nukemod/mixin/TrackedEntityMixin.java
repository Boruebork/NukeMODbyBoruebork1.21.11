package com.boruebork.nukemod.mixin;

import com.boruebork.nukemod.drone.DroneManager;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVDrone;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.UUID;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin {

    @Shadow @Final Entity entity;
    @Shadow @Final ServerEntity serverEntity;
    @Shadow @Final Set<ServerPlayerConnection> seenBy;

    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void drone$forceTrackForPilot(ServerPlayer player, CallbackInfo ci) {
        if (!(this.entity instanceof AbstractFPVDrone drone)) return;

        UUID pilotedDroneId = DroneManager.getInstance().playerToDrone.get(player.getUUID());
        if (pilotedDroneId == null || !pilotedDroneId.equals(drone.getUUID())) return;

        // bypass the range + isChunkTracked gate entirely — force this pairing to exist
        if (this.seenBy.add(player.connection)) {
            this.serverEntity.addPairing(player);
        }
        ci.cancel();
    }
}
