package com.boruebork.nukemod.entity.custom;

import com.boruebork.nukemod.entity.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class RocketDrone extends AbstractFPVProjectileLaunchingDrone{
    public RocketDrone(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineProjectileOffsets() {
        addProjectileBay(ModEntities.ROCKET.get(),new Vec3(7.75, -4, 0).scale(thisEntity.getWidth()/14).add(0, 0.7f, 0));
        addProjectileBay(ModEntities.ROCKET.get(),new Vec3(-7.75, -4, 0).scale(thisEntity.getWidth()/14).add(0, 0.7f, 0));
        addProjectileBay(ModEntities.ROCKET.get(),new Vec3(-4.5, -4, 0).scale(thisEntity.getWidth()/14).add(0, 0.7f, 0));
        addProjectileBay(ModEntities.ROCKET.get(),new Vec3(4.5, -4, 0).scale(thisEntity.getWidth()/14).add(0, 0.7f, 0));
    }

    @Override
    protected float getMaxHealth() {
        return 4;
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
        return 2;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {

    }
    public int getNextRocket(){
        return this.getNextBay(ModEntities.ROCKET.get());
    }
}
