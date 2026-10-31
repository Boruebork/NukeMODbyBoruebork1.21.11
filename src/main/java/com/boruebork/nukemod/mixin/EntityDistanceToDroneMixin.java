package com.boruebork.nukemod.mixin;

import com.boruebork.nukemod.drone.DroneManager;
import com.boruebork.nukemod.drone.Pilotable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityDistanceToDroneMixin {

    @Inject(
            method = "distanceToSqr(Lnet/minecraft/world/entity/Entity;)D",
            at = @At("HEAD"),
            cancellable = true
    )
    private void droneforce$pilotDistanceFromDrone(Entity other, CallbackInfoReturnable<Double> cir) {
        if ((Object) this instanceof ServerPlayer player) {
            Pilotable drone = DroneManager.getInstance().getPiloted(player);
            if (drone != null) {
                cir.setReturnValue(drone.asEntity().distanceToSqr(other));
            }
        }
    }
}