package com.boruebork.nukemod.mixin;

import com.boruebork.nukemod.drone.DroneManager;
import com.boruebork.nukemod.drone.AbstractDrone;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityDroneViewMixin {

    @Redirect(
            method = "updatePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;position()Lnet/minecraft/world/phys/Vec3;"
            ),
            require = 1
    )
    private Vec3 droneforce$playerPosAsDrone(ServerPlayer player) {
        AbstractDrone drone = DroneManager.getInstance().getPilotedDrone(player);
        if (drone != null) {
            return drone.position();
        }
        return player.position();
    }
}