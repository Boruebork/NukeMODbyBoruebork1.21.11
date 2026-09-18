package com.boruebork.nukemod.entity.custom;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class Grenade extends Entity implements DroneProjectile {
    private int ticks = 0;

    public Grenade(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public void setStartingSpeed(Vec3 speed, float xRot, float yRot, AbstractFPVProjectileLaunchingDrone parent) {
        this.setDeltaMovement(speed);
        this.setXRot(xRot);
        this.setYRot(yRot);
        this.ticks = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (ticks == 0){
            ticks = 1;
            return;
        }
        this.setOldPosAndRot(); // baseline for render interpolation
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().add(0, -2 / 20f, 0)); // gravity

        if (!level().isClientSide()) {
            boolean hitBlock = this.horizontalCollision || this.verticalCollision;
            boolean hitEntity = !level().getEntities(this, this.getBoundingBox(), e -> e.isPickable() && e != this).isEmpty();
            if ((hitBlock || hitEntity) && ticks > 20) {
                explode();
            }
        }
        ticks++;
    }

    private void explode() {
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), 3f, Level.ExplosionInteraction.TNT);
        this.discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float v) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {}
}
