package com.boruebork.nukemod.entity.custom;

import com.boruebork.nukemod.explosion.NuclearExplosion;
import com.boruebork.nukemod.util.Util;
import net.minecraft.core.Vec3i;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class GuidedMissile extends Projectile {

    private enum MissileState {
        LAUNCH,    // Вертикальный взлет вверх
        CRUISE,    // Горизонтальный полет на большой высоте к цели
        PREDATOR   // Финальное пикирование строго на игрока
    }

    private LivingEntity targetPlayer;
    private MissileState currentState = MissileState.LAUNCH;

    // Настройки баллистики
    private final double cruiseHeight = 120.0; // Высота (Y), на которую ракета поднимается (выше деревьев/гор)
    private final double speed = 1.2;          // Повышенная скорость для баллистической ракеты
    private final float turnSpeed = 0.08f;     // Медленный разворот на фазе круиза для красивой дуги

    public GuidedMissile(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    public void setTarget(LivingEntity target) {
        this.targetPlayer = target;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    @Override
    public HitResult pick(double hitDistance, float partialTick, boolean hitFluids) {
        return super.pick(hitDistance, partialTick, hitFluids);
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY || result.getType() == HitResult.Type.BLOCK){
            NuclearExplosion.createExplosion((ServerLevel) this.level(), Util.Vec3toVec3i(this.position()));
        }
    }

    @Override
    public void tick() {
       if (!this.level().isClientSide()) {
            // Если цель погибла во время полета, ракета просто падает и взрывается
            if (targetPlayer == null || !targetPlayer.isAlive() || targetPlayer.level() != this.level()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, -0.05, 0)); // падение
                return;
            }

            Vec3 currentPos = this.position();
            Vec3 targetPos = targetPlayer.position();
            Vec3 motion = this.getDeltaMovement();

            switch (currentState) {
                case LAUNCH:
                    // 1. Стадия взлета: Летим строго вверх, игнорируя координаты цели по X и Z
                    motion = new Vec3(0, speed, 0);

                    // Как только достигли нужной высоты (например, Y=120) — переключаемся на круиз
                    if (currentPos.y() >= cruiseHeight) {
                        currentState = MissileState.CRUISE;
                    }
                    break;

                case CRUISE:
                    // 2. Стадия круиза: Летим к цели, но удерживаем высоту в небе
                    Vec3 cruiseTarget = new Vec3(targetPos.x(), cruiseHeight, targetPos.z());
                    Vec3 desiredCruiseDir = cruiseTarget.subtract(currentPos).normalize();

                    // Плавно поворачиваем в сторону цели в горизонтальной плоскости
                    motion = motion.normalize().lerp(desiredCruiseDir, turnSpeed).normalize().scale(speed);

                    // Вычисляем горизонтальное расстояние до цели (без учета Y)
                    double distanceX = targetPos.x() - currentPos.x();
                    double distanceZ = targetPos.z() - currentPos.z();
                    double horizontalDistance = Math.sqrt(distanceX * distanceX + distanceZ * distanceZ);

                    // Если подлетели к цели достаточно близко по горизонтали (например, ближе 30 блоков)
                    // или цель находится прямо под нами — включаем режим «Хищник» (пикирование)
                    if (horizontalDistance < 30.0) {
                        currentState = MissileState.PREDATOR;
                    }
                    break;

                case PREDATOR:
                    // 3. Стадия пикирования: Агрессивное наведение прямо на игрока
                    Vec3 exactTargetPos = targetPos.add(0, targetPlayer.getBbHeight() / 2.0, 0);
                    Vec3 desiredPredatorDir = exactTargetPos.subtract(currentPos).normalize();

                    // На этой стадии ракета наводится очень резко (turnSpeed увеличен до 0.3), чтобы игрок не увернулся
                    motion = motion.normalize().lerp(desiredPredatorDir, 0.3f).normalize().scale(speed * 1.5);
                    break;
            }

            // Применяем вычисленное движение
            this.setDeltaMovement(motion);

            // Визуальный эффект: спавним много дыма и огоньков сзади ракеты
            if (this.tickCount % 2 == 0) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                        this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
        }
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

