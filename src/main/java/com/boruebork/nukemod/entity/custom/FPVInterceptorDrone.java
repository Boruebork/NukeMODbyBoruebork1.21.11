package com.boruebork.nukemod.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FPVInterceptorDrone extends AbstractFPVDrone{
    public FPVInterceptorDrone(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected float getMaxHealth() {
        return 3;
    }

    @Override
    protected float getVerticalSpeedModifier() {
        return 3;
    }

    @Override
    protected float getHorizontalSpeedModifier() {
        return 0.4f;
    }

    @Override
    protected float getOnHitExplosionRadius() {
        return 2;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {

    }
}
