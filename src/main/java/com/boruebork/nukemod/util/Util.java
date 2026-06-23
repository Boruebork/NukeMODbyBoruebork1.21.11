package com.boruebork.nukemod.util;

import net.minecraft.core.BlockPos;
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
    public static Vec3i BlockPosTooVec3i(BlockPos pos){
        return new Vec3i(
                (int) pos.getX(),
                (int) pos.getY(),
                (int) pos.getZ()
        );
    }
    public static Vec3 BlockPosTooVec3(BlockPos pos){
        return new Vec3(
                pos.getX(),
                pos.getY() + 0.6f,
                pos.getZ()
        );
    }
}
