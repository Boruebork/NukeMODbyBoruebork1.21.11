package com.boruebork.nukemod.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FPVDrone extends AbstractFPVDrone {
    private float rotorSpeed = 0f;
    private float rotorAngle = 0f;
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