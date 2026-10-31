package com.boruebork.nukemod.uav.flight;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public record FlightState(Vec3 pos, Vec3 vel, float yaw, float pitch, float roll,
                          float throttle, float groundPitchRate, boolean onGround) {
    public static Quaternionf rotation(float yaw, float pitch, float roll) {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(pitch))
                .rotateZ((float) Math.toRadians(roll));
    }
}