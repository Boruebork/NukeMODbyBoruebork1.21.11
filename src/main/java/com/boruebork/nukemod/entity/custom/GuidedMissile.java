package com.boruebork.nukemod.entity.custom;

import com.boruebork.nukemod.entity.custom.sound.MissileWarningSound;
import com.boruebork.nukemod.explosion.ExplosionManager;
import com.boruebork.nukemod.missile.MissileManager;
import com.boruebork.nukemod.sound.ModSounds;
import com.boruebork.nukemod.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.boruebork.nukemod.explosion.NukeConfig.SIREN_ACTIVATION_DISTANCE;

public class GuidedMissile extends Projectile {

    public void setWarningSound(MissileWarningSound o) {
        this.warning = o;
    }

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
    private UUID targetUUID;
    private MissileWarningSound siren; //onlu client side
    private int soundTick = 0;
    private static final double maxSpeed = 2;
    // Настройки баллистики
    private final double cruiseHeight = 120.0; // Высота (Y), на которую ракета поднимается (выше деревьев/гор)
    // Повышенная скорость для баллистической ракеты
    private final float turnSpeed = 0.08f;     // Медленный разворот на фазе круиза для красивой дуги

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
    public void activate(){
        this.active = true;
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {

    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY || result.getType() == HitResult.Type.BLOCK){
            MissileManager.queuePhysicalRemoval(this);
            ExplosionManager.addExplosion((ServerLevel) this.level(), Util.Vec3toVec3i(this.position()));
            this.discard();
        }
    }
    MissileWarningSound warning = null;
    @Override
    public void tick() {
        super.tick();

        // -------------------------
        // CLIENT
        // -------------------------
        if (level().isClientSide()) {

            if (soundTick <= 0) {
                this.playSound(
                        ModSounds.MISSILE_LAUNCH.get(),
                        0.5f,
                        1f
                );
                soundTick = 20;
            } else {
                soundTick--;
            }

            LocalPlayer player = Minecraft.getInstance().player;

            if (player != null
                    && player.distanceTo(this) < SIREN_ACTIVATION_DISTANCE) {

                if (warning == null) {
                    warning = new MissileWarningSound(this);
                    Minecraft.getInstance()
                            .getSoundManager()
                            .play(warning);
                }
            }

            return;
        }

        // -------------------------
        // SERVER
        // -------------------------

        ServerLevel serverLevel = (ServerLevel) level();

        // Resolve target UUID -> actual player.
        // This is important after loading the entity from disk.
        if (targetPlayer == null && targetUUID != null) {
            targetPlayer = serverLevel.getServer()
                    .getPlayerList()
                    .getPlayer(targetUUID);
        }

        // -------------------------
        // ACTIVATION
        // -------------------------

        if (!active) {
            if (targetPlayer != null) {
                active = true;
            } else {
                // Target hasn't joined/loaded yet.
                return;
            }
        }

        // -------------------------
        // TARGET VALIDITY
        // -------------------------

        if (targetPlayer == null
                || !targetPlayer.isAlive()
                || targetPlayer.level() != level()) {

            state = LockingState.NA;

        } else if (state == LockingState.LOCKED) {

            // Keep updating the last known position while locked.
            lastTargetPosition = targetPlayer.position();
        }

        // -------------------------
        // DETERMINE TARGET POSITION
        // -------------------------

        Vec3 targetPos;

        if (state == LockingState.LOCKED && targetPlayer != null) {
            targetPos = targetPlayer.position();
        } else {
            targetPos = lastTargetPosition;
        }

        // If we don't have any target position at all,
        // there's nothing meaningful we can do.
        if (targetPos == null) {
            return;
        }

        // -------------------------
        // GUIDANCE
        // -------------------------

        switch (currentState) {

            case LAUNCH -> tickLaunch();

            case CRUISE -> tickCruise(targetPos);

            case PREDATOR -> tickTerminal(targetPos);
        }

        // -------------------------
        // ROTATION
        // -------------------------

        float yaw = (float) Math.toDegrees(
                Math.atan2(-direction.x, direction.z)
        );

        float pitch = (float) Math.toDegrees(
                Math.asin(-direction.y)
        );

        setRot(yaw, pitch);

        // -------------------------
        // MOVEMENT
        // -------------------------

        setDeltaMovement(direction.scale(speed));

        move(
                MoverType.SELF,
                getDeltaMovement()
        );

        // -------------------------
        // COLLISION / EXPLOSION
        // -------------------------

        if (explosionSHield <= 0) {

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
            explosionSHield--;
        }

        // -------------------------
        // PARTICLES
        // -------------------------

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
        speed = Math.min(speed + 0.1, maxSpeed);
        Vec3 desiredDirection =
                direction.add(0, 1.0, 0).normalize();

        direction = direction.lerp(
                desiredDirection,
                0.1
        ).normalize();

        if (getY() >= cruiseHeight) {
            currentState = MissileState.CRUISE;
        }
    }
    private void tickCruise(Vec3 targetPos) {
        speed = Math.min(speed + 0.05, maxSpeed);

        Vec3 toTarget =
                targetPos.subtract(position());
        double horizontalDistance =
                Math.sqrt(
                        toTarget.x * toTarget.x +
                                toTarget.z * toTarget.z
                );
        if (horizontalDistance < 30) {
            currentState = MissileState.PREDATOR;
            //System.err.println("swithcing to preadtor");
            return;
        }
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
        //System.err.println("hunting!");
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
    protected void addAdditionalSaveData(ValueOutput output) {
        if (targetUUID != null) {
            output.putString("TargetUUID", targetUUID.toString());
        }

        output.putString("MissileState", currentState.name());
        output.putString(
                "LockingState",
                state != null ? state.name() : LockingState.NA.name()
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
        output.putInt("ExplosionShield", explosionSHield);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {

        input.getString("TargetUUID").ifPresent(s -> {
            try {
                this.targetUUID = UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                this.targetUUID = null;
            }
        });

        this.targetPlayer = null;

        this.currentState = input.getString("MissileState")
                .map(s -> {
                    try {
                        return MissileState.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        return MissileState.LAUNCH;
                    }
                })
                .orElse(MissileState.LAUNCH);

        this.state = input.getString("LockingState")
                .map(s -> {
                    try {
                        return LockingState.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        return LockingState.NA;
                    }
                })
                .orElse(LockingState.NA);

        this.speed = input.getDoubleOr("Speed", 0.0);

        this.direction = new Vec3(
                input.getDoubleOr("DirectionX", 0.0),
                input.getDoubleOr("DirectionY", 0.0),
                input.getDoubleOr("DirectionZ", 0.0)
        );

        this.lastTargetPosition = new Vec3(
                input.getDoubleOr("TargetX", 0.0),
                input.getDoubleOr("TargetY", 0.0),
                input.getDoubleOr("TargetZ", 0.0)
        );

        this.active = input.getBooleanOr("Active", false);
        this.explosionSHield = input.getIntOr("ExplosionShield", 30);
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

