package com.boruebork.nukemod.entity.custom.fpvdrones;

import com.boruebork.nukemod.drone.AbstractPilotedEntity;
import net.minecraft.world.phys.Vec3;

public interface DroneProjectile {
    public void setStartingSpeed(Vec3 speed, float xRot, float yRot, AbstractPilotedEntity parent);
}
