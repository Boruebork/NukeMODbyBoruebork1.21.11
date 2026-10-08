package com.boruebork.nukemod.entity.custom.uav;

import com.boruebork.nukemod.ooblib.AbstractUAV;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class RQ4 extends AbstractUAV {
    @Override
    protected float getMinSpeed() {
        return 0;
    }

    @Override
    protected float getMaxSpeed() {
        return 0.4f;
    }

    @Override
    protected float getAcceleration() {
        return 0.05f;
    }

    protected float getYawPerRollDegreePerTick() {
        return 1;
    }

    @Override
    protected float getMaxPitchRatePerTick() {
        return 1;
    }

    public RQ4(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected float getRollStep() {
        return 2f;
    }

    @Override
    protected float getMaxRoll() {
        return 90f;
    }

    @Override
    protected float getRollFactor() {
        return 2f;
    }
}
