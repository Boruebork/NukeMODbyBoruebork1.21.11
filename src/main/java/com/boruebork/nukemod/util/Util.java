package com.boruebork.nukemod.util;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

public class Util {
    public static Vec3i Vec3toVec3i(Vec3 vec3){
        return new Vec3i(
                (int) vec3.x,
                (int) vec3.y,
                (int) vec3.z
        );
    }
}
