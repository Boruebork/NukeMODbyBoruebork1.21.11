package com.boruebork.nukemod.entity.custom;

import com.boruebork.nukemod.entity.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class GrenadeDrone extends AbstractFPVProjectileLaunchingDrone{
    public GrenadeDrone(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
    private static final int FIRST_GRENADE = 0;
    private static final int SECOND_GRENADE = 1;
    @Override
    protected void defineProjectileOffsets() {
        addProjectileBay(ModEntities.GRENADE.get(), new Vec3(-4.3, -4, 0).scale(thisEntity.getWidth()/14).add(0, 0.7f, 0));
        addProjectileBay(ModEntities.GRENADE.get(), new Vec3(4.3, -4, 0).scale(thisEntity.getWidth()/14).add(0, 0.7f, 0));
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
        return 0.2f;
    }

    @Override
    protected float getOnHitExplosionRadius() {
        if (grenade1()) return 8f;
        if (grenade2()) return 6f;
        return 2;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {

    }
    public boolean grenade1(){
        return getNextBay(ModEntities.GRENADE.get()) == 0;
    }
    public boolean grenade2(){
        return getNextBay(ModEntities.GRENADE.get()) == 1 || getNextBay(ModEntities.GRENADE.get()) == 0;
    }
}
