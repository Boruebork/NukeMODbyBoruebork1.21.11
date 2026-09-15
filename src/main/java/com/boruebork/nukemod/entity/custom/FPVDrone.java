package com.boruebork.nukemod.entity.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FPVDrone extends Drone {
    private float rotorSpeed = 0f;
    private float rotorAngle = 0f;
    public FPVDrone(EntityType<? extends Drone> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float v) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        float targetSpeed = isBeingPiloted() ? 45f : 0f; // degrees/tick at full spin
        // ease toward target so rotors spin up/down instead of snapping
        this.rotorSpeed += (targetSpeed - this.rotorSpeed) * 0.1f;
        this.rotorAngle = (this.rotorAngle + this.rotorSpeed) % 360f;
    }

    @Override
    protected void readAdditionalSaveData (ValueInput valueInput){

    }

    @Override
    protected void addAdditionalSaveData (ValueOutput valueOutput){

    }public float getRotorAngle() {
        return rotorAngle;
    }
    public float getRotorSpeed() {
        return rotorSpeed;
    }
}