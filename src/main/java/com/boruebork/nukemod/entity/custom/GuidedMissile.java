package com.boruebork.nukemod.entity.custom;

import com.boruebork.nukemod.explosion.ExplosionManager;
import com.boruebork.nukemod.missile.MissileManager;
import com.boruebork.nukemod.util.Util;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class GuidedMissile extends Projectile {

    public enum MissileState {
        LAUNCH,
        CRUISE,
        PREDATOR
    }

    public enum LockingState {
        LOCKED,
        NA
    }

    private static final double MAX_SPEED = 2.0;
    private static final double CRUISE_HEIGHT = 120.0;

    private boolean active;
    private int explosionShield = 30;

    private LivingEntity targetPlayer;
    private UUID targetUUID;

    private MissileState currentState = MissileState.LAUNCH;
    private LockingState state;

    private Vec3 direction = Vec3.ZERO;
    private Vec3 lastTargetPosition;

    private double speed;


    public GuidedMissile(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }


    public void setTarget(LivingEntity player) {
        this.targetPlayer = player;
        this.targetUUID = player.getUUID();

        this.lastTargetPosition = player.position();
        this.currentState = MissileState.LAUNCH;
        this.state = LockingState.LOCKED;
        this.direction = Vec3.ZERO;
        this.active = false;

        this.setSilent(false);
    }


    public void activate() {
        this.active = true;
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }


    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY
                || result.getType() == HitResult.Type.BLOCK) {

            MissileManager.queuePhysicalRemoval(this);

            ExplosionManager.addExplosion(
                    (ServerLevel) level(),
                    Util.Vec3toVec3i(position())
            );

            discard();
        }
    }


    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level();

        // Resolve target after loading from disk.
        if (targetPlayer == null && targetUUID != null) {
            targetPlayer = serverLevel.getServer()
                    .getPlayerList()
                    .getPlayer(targetUUID);
        }

        // Wait until the target is available.
        if (!active) {
            if (targetPlayer != null) {
                active = true;
            } else {
                return;
            }
        }

        // Check target validity.
        if (targetPlayer == null
                || !targetPlayer.isAlive()
                || targetPlayer.level() != level()) {

            state = LockingState.NA;

        } else if (state == LockingState.LOCKED) {

            lastTargetPosition = targetPlayer.position();
        }

        // Determine target position.
        Vec3 targetPos;

        if (state == LockingState.LOCKED && targetPlayer != null) {
            targetPos = targetPlayer.position();
        } else {
            targetPos = lastTargetPosition;
        }

        if (targetPos == null) {
            return;
        }

        // Guidance.
        switch (currentState) {
            case LAUNCH -> tickLaunch();
            case CRUISE -> tickCruise(targetPos);
            case PREDATOR -> tickTerminal(targetPos);
        }

        // Rotation.
        float yaw = (float) Math.toDegrees(
                Math.atan2(-direction.x, direction.z)
        );

        float pitch = (float) Math.toDegrees(
                Math.asin(-direction.y)
        );

        setRot(yaw, pitch);

        // Movement.
        setDeltaMovement(direction.scale(speed));

        move(
                MoverType.SELF,
                getDeltaMovement()
        );

        // Collision / explosion.
        if (explosionShield <= 0) {

            if (horizontalCollision
                    || verticalCollision
                    || minorHorizontalCollision) {

                MissileManager.queuePhysicalRemoval(this);

                ExplosionManager.addExplosion(
                        serverLevel,
                        Util.Vec3toVec3i(position())
                );

                discard();
                return;
            }

        } else {
            explosionShield--;
        }

        // Missile trail.
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
        speed = Math.min(speed + 0.1, MAX_SPEED);

        Vec3 desiredDirection =
                direction.add(0, 1.0, 0).normalize();

        direction = direction
                .lerp(desiredDirection, 0.1)
                .normalize();

        if (getY() >= CRUISE_HEIGHT) {
            currentState = MissileState.CRUISE;
        }
    }


    private void tickCruise(Vec3 targetPos) {
        speed = Math.min(speed + 0.05, MAX_SPEED);

        Vec3 toTarget = targetPos.subtract(position());

        double horizontalDistance = Math.sqrt(
                toTarget.x * toTarget.x
                        + toTarget.z * toTarget.z
        );

        if (horizontalDistance < 30) {
            currentState = MissileState.PREDATOR;
            return;
        }

        double altitudeError = CRUISE_HEIGHT - getY();

        Vec3 desiredDirection = new Vec3(
                toTarget.x,
                altitudeError * 0.1,
                toTarget.z
        ).normalize();

        direction = direction
                .lerp(desiredDirection, 0.05)
                .normalize();
    }


    private void tickTerminal(Vec3 targetPos) {
        speed = MAX_SPEED;

        Vec3 desiredDirection =
                targetPos.subtract(position())
                        .normalize();

        direction = direction
                .lerp(desiredDirection, 0.15)
                .normalize();
    }


    @Override
    public boolean hurtServer(
            ServerLevel serverLevel,
            DamageSource damageSource,
            float damage
    ) {
        return false;
    }


    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (targetUUID != null) {
            output.putString(
                    "TargetUUID",
                    targetUUID.toString()
            );
        }

        output.putString(
                "MissileState",
                currentState.name()
        );

        output.putString(
                "LockingState",
                state != null
                        ? state.name()
                        : LockingState.NA.name()
        );

        output.putDouble("Speed", speed);

        output.putDouble("DirectionX", direction.x);
        output.putDouble("DirectionY", direction.y);
        output.putDouble("DirectionZ", direction.z);

        if (lastTargetPosition != null) {
            output.putDouble("TargetX", lastTargetPosition.x);
            output.putDouble("TargetY", lastTargetPosition.y);
            output.putDouble("TargetZ", lastTargetPosition.z);
        }

        output.putBoolean("Active", active);
        output.putInt("ExplosionShield", explosionShield);
    }


    @Override
    protected void readAdditionalSaveData(ValueInput input) {

        input.getString("TargetUUID").ifPresent(s -> {
            try {
                targetUUID = UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                targetUUID = null;
            }
        });

        targetPlayer = null;

        currentState = input.getString("MissileState")
                .map(s -> {
                    try {
                        return MissileState.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        return MissileState.LAUNCH;
                    }
                })
                .orElse(MissileState.LAUNCH);

        state = input.getString("LockingState")
                .map(s -> {
                    try {
                        return LockingState.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        return LockingState.NA;
                    }
                })
                .orElse(LockingState.NA);

        speed = input.getDoubleOr("Speed", 0.0);

        direction = new Vec3(
                input.getDoubleOr("DirectionX", 0.0),
                input.getDoubleOr("DirectionY", 0.0),
                input.getDoubleOr("DirectionZ", 0.0)
        );

        lastTargetPosition = new Vec3(
                input.getDoubleOr("TargetX", 0.0),
                input.getDoubleOr("TargetY", 0.0),
                input.getDoubleOr("TargetZ", 0.0)
        );

        active = input.getBooleanOr("Active", false);

        explosionShield =
                input.getIntOr("ExplosionShield", 30);
    }


    public Vec3 getDir() {
        return direction;
    }

    public LivingEntity targetPlayer() {
        return targetPlayer;
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
        return CRUISE_HEIGHT;
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