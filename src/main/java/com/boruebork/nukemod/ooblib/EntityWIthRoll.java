package com.boruebork.nukemod.ooblib;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public abstract class EntityWIthRoll extends Entity {
    public float roll;
    public float rollO;
    public static final EntityDataAccessor<Float> ROLL_DATA =
            SynchedEntityData.defineId(EntityWIthRoll.class, EntityDataSerializers.FLOAT);

    public EntityWIthRoll(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public double getRenderRoll(float partialTick) {
        return roll;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ROLL_DATA, 0f);
    }
}
