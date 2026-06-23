package com.boruebork.nukemod.missile;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;

public class MissileData
{
    public MissileData(Vec3 current, Vec3 rotation, Vec3 target, double speed) {
        this.position = current;
        this.direction = rotation;
        this.target = target;
        this.speed = speed;
    }
    double speed;
    Vec3 position;
    Vec3 direction;
    Vec3 target = new Vec3(0,0,0);
    public void tick(MinecraftServer server){

    }
}
