package com.boruebork.nukemod.entity.custom;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class MushroomEntity extends Entity {
    public static final int MUSHROOM_LIFE_IN_TICKS = 400;
    private int ageInTicks = 0;
    public MushroomEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.ageInTicks = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    @Override
    public void tick() {
        if (ageInTicks > MUSHROOM_LIFE_IN_TICKS){
            this.discard();
            return;
        }
        this.ageInTicks++;
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float v) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {

    }
}
