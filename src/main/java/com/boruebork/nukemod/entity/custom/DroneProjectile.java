package com.boruebork.nukemod.entity.custom;

import net.minecraft.world.phys.Vec3;

public interface DroneProjectile {
    public void setStartingSpeed(Vec3 speed, float xRot, float yRot, AbstractFPVProjectileLaunchingDrone parent);
}
