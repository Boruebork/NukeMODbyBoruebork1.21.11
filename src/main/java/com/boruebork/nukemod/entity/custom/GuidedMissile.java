package com.boruebork.nukemod.entity.custom;

import com.boruebork.nukemod.missile.MissileManager;
import com.boruebork.nukemod.explosion.NuclearExplosion;
import com.boruebork.nukemod.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class GuidedMissile extends Projectile {

    public enum MissileState {
        LAUNCH,    // Вертикальный взлет вверх
        CRUISE,    // Горизонтальный полет на большой высоте к цели
        PREDATOR   // Финальное пикирование строго на игрока
    }
    public enum LockingState {
        LOCKED,
        NA
    }
    private boolean active = false;
    private LivingEntity targetPlayer;
    private int explosionSHield = 30;
    private MissileState currentState = MissileState.LAUNCH;
    private Vec3 direction = new Vec3(0,0,0);
    private Vec3 lastTargetPosition;
    private LockingState state;
    public double speed;
    private static double maxSpeed = 2;
    // Настройки баллистики
    private final double cruiseHeight = 120.0; // Высота (Y), на которую ракета поднимается (выше деревьев/гор)
    private final double MAX_SPEED = 1.2;          // Повышенная скорость для баллистической ракеты
    private final float turnSpeed = 0.08f;     // Медленный разворот на фазе круиза для красивой дуги

    public GuidedMissile(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }
    public void setTarget(LivingEntity player) {
        this.targetPlayer = player;
        this.lastTargetPosition = player.position();
        this.currentState = MissileState.LAUNCH;
        this.state = LockingState.LOCKED;
        this.direction = new Vec3(0,0,0);
        this.active = true;
        System.err.println(player);
        System.err.println("set target!");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY || result.getType() == HitResult.Type.BLOCK){
            NuclearExplosion.createExplosion((ServerLevel) this.level(), Util.Vec3toVec3i(this.position()));
        }
    }

    @Override
    public void tick() {
        super.tick();
        System.err.println("OMG it is mr.Tick!");
        if (level().isClientSide()) {
            System.err.println("Client??? Cleint???");
            return;
        }

        if (!this.active) {
            if (this.targetPlayer != null) this.active = true;
            System.err.println("sleeping😴");
            return;

        }

        // Convert to data object if chunk stops ticking
        if (!((ServerLevel) level()).isPositionEntityTicking(blockPosition())) {
            MissileManager.convertToData(this);
            discard();
            System.err.println("badabim badabum!");
            return;
        }

        // Target validity
        if (targetPlayer == null
                || !targetPlayer.isAlive()
                || targetPlayer.level() != this.level()) {
            state = LockingState.NA;
            System.err.println("Where did the prey go?");
        }

        Vec3 targetPos = state == LockingState.LOCKED
                ? targetPlayer.position()
                : lastTargetPosition;

        switch (currentState) {

            case LAUNCH -> tickLaunch();

            case CRUISE -> tickCruise(targetPos);

            case PREDATOR -> tickTerminal(targetPos);
        }
        BlockPos nextPos = BlockPos.containing(position().add(direction.scale(speed)));
        if (!((ServerLevel) this.level()).isPositionEntityTicking(nextPos)) {
            MissileManager.convertToData(this);
            this.discard();
            System.err.println("Whoops it is time to convert!");
        }
        // Apply movement
        setDeltaMovement(direction.scale(speed));
        this.move(MoverType.SELF, this.getDeltaMovement());

        // Collision
        if (this.explosionSHield <= 0) {
            if (horizontalCollision
                    || verticalCollision
                    || minorHorizontalCollision) {

                NuclearExplosion.createExplosion(
                        (ServerLevel) level(),
                        Util.Vec3toVec3i(position())
                );

                discard();
                System.err.println("Nebelsturm!!!!");
                return;
            }
        }else{
            this.explosionSHield--;
        }
        // Save last known target position
        if (state == LockingState.LOCKED) {
            lastTargetPosition = targetPlayer.position();
        }

        // Smoke
        if (tickCount % 2 == 0) {
            level().addParticle(
                    ParticleTypes.FLAME,
                    getX(),
                    getY(),
                    getZ(),
                    0,
                    0,
                    0
            );
        }
    }
    private void tickLaunch() {
        System.err.println("climbing...");
        speed = Math.min(speed + 0.1, maxSpeed);

        // Force upward climb
        Vec3 desiredDirection =
                direction.add(0, 1.0, 0).normalize();

        direction = direction.lerp(
                desiredDirection,
                0.1
        ).normalize();

        if (getY() >= cruiseHeight) {
            currentState = MissileState.CRUISE;
            System.err.println("missile cruising!");
        }
    }
    private void tickCruise(Vec3 targetPos) {
        System.err.println("crusing");
        speed = Math.min(speed + 0.05, maxSpeed);

        Vec3 toTarget =
                targetPos.subtract(position());

        // Horizontal distance only
        double horizontalDistance =
                Math.sqrt(
                        toTarget.x * toTarget.x +
                                toTarget.z * toTarget.z
                );

        // Switch to terminal dive
        if (horizontalDistance < 30) {
            currentState = MissileState.PREDATOR;
            System.err.println("swithcing to preadtor");
            return;
        }

        // Maintain cruise altitude
        double altitudeError =
                cruiseHeight - getY();

        Vec3 desiredDirection =
                new Vec3(
                        toTarget.x,
                        altitudeError * 0.1,
                        toTarget.z
                ).normalize();

        direction = direction.lerp(
                desiredDirection,
                0.05
        ).normalize();
    }
    private void tickTerminal(Vec3 targetPos) {
        System.err.println("hunting!");
        speed = maxSpeed;

        Vec3 desiredDirection =
                targetPos.subtract(position())
                        .normalize();

        direction = direction.lerp(
                desiredDirection,
                0.15
        ).normalize();
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

    public Vec3 getDir() {
        return direction;
    }

    public LivingEntity targetPlayer() {
        return targetPlayer;
    }

    public void setTargetPlayer(LivingEntity targetPlayer) {
        this.targetPlayer = targetPlayer;
    }

    public boolean active() {
        return active;
    }

    public MissileState currentState() {
        return currentState;
    }

    public Vec3 direction() {
        return direction;
    }

    public LockingState state() {
        return state;
    }

    public Vec3 lastTargetPosition() {
        return lastTargetPosition;
    }

    public double speed() {
        return speed;
    }

    public double cruiseHeight() {
        return cruiseHeight;
    }

    public float turnSpeed() {
        return turnSpeed;
    }

    public void setDirection(Vec3 direction) {
        this.direction = direction;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setCurrentState(MissileState currentState) {
        this.currentState = currentState;
    }

    public void setLastTargetPosition(Vec3 lastTargetPosition) {
        this.lastTargetPosition = lastTargetPosition;
    }

    public void setState(LockingState state) {
        this.state = state;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

