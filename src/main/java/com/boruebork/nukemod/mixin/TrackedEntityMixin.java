package com.boruebork.nukemod.mixin;

import com.boruebork.nukemod.drone.AbstractDrone;
import com.boruebork.nukemod.drone.DroneManager;
import com.boruebork.nukemod.drone.Pilotable;
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
        if (!(this.entity instanceof AbstractDrone drone)) return;

        Pilotable piloted = DroneManager.getInstance().getPiloted(player);
        if (piloted == null) return;                     // player isn't piloting anything

        Entity pilotDrone = piloted.asEntity();
        if (pilotDrone == null) return;                  // drone entity not spawned yet
        if (!(pilotDrone instanceof AbstractDrone)) return;
        AbstractDrone pilotDrone2 = (AbstractDrone) pilotDrone;
        UUID pilotedDroneId = pilotDrone2.getUUID();
        if (!pilotedDroneId.equals(drone.getUUID())) return;

        if (player.connection == null) return;

        // bypass range + isChunkTracked gate — force this pairing
        if (this.seenBy.add(player.connection)) {
            this.serverEntity.addPairing(player);
        }
        ci.cancel();
    }
}