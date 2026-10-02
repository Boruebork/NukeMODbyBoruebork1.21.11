package com.boruebork.nukemod.entity.custom.fpvdrones;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FPVDrone extends AbstractFPVDrone {

    public FPVDrone(EntityType<? extends AbstractFPVDrone> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected float getMaxHealth() {
        return 5;
    }

    @Override
    protected float getVerticalSpeedModifier() {
        return 2;
    }

    @Override
    protected float getHorizontalSpeedModifier() {
        return 0.3f;
    }

    @Override
    protected float getOnHitExplosionRadius() {
        return 4;
    }
    @Override
    protected void readAdditionalSaveData (ValueInput valueInput){

    }

    @Override
    protected void addAdditionalSaveData (ValueOutput valueOutput){

    }
}