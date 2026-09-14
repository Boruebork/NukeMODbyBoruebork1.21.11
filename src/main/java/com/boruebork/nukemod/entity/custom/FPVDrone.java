package com.boruebork.nukemod.entity.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FPVDrone extends Drone {
    public FPVDrone(EntityType<? extends Drone> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float v) {
        return false;
    }


    @Override
    protected void readAdditionalSaveData (ValueInput valueInput){

    }

    @Override
    protected void addAdditionalSaveData (ValueOutput valueOutput){

    }
}