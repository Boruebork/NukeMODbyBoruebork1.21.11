package com.boruebork.nukemod.missile;

import com.boruebork.nukemod.entity.custom.GuidedMissile;
import com.boruebork.nukemod.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.UUID;

public class GuidedMissileData extends MissileData {

    public final UUID targetUUID;
    public GuidedMissile.MissileState missileState = GuidedMissile.MissileState.LAUNCH;
    public GuidedMissile.LockingState lockingState = GuidedMissile.LockingState.LOCKED;
    public Vec3 lastTargetPosition;
    public double cruiseHeight = 200;
    public double maxSpeed = 10;

    public GuidedMissileData(GuidedMissile missile) {
        super(
                missile.position(),
                missile.getDir(),
                missile.targetPlayer().position(),
                missile.speed
        );

        this.targetUUID = missile.targetPlayer().getUUID();
        this.missileState = missile.currentState();
        this.lockingState = missile.state();
        this.lastTargetPosition = missile.lastTargetPosition();
    }

    @Override
    public void tick(MinecraftServer server) {
        //System.err.println("You are not alone...");
        Level level = server.getLevel(Level.OVERWORLD);
        ServerPlayer targetPlayer = null;

        if (level instanceof ServerLevel serverLevel) {
            targetPlayer = serverLevel.getServer()
                    .getPlayerList()
                    .getPlayer(targetUUID);
        }

        if (targetPlayer == null
                || !targetPlayer.isAlive()
                || targetPlayer.level() != level) {

            lockingState = GuidedMissile.LockingState.NA;
        }

        Vec3 targetPos =
                lockingState == GuidedMissile.LockingState.LOCKED
                        ? targetPlayer.position()
                        : lastTargetPosition;

        switch (missileState) {

            case LAUNCH -> tickLaunch();

            case CRUISE -> tickCruise(targetPos);

            case PREDATOR -> tickTerminal(targetPos);
        }

        // Move missile in RAM
        position = position.add(
                direction.scale(speed)
        );

        if (lockingState == GuidedMissile.LockingState.LOCKED) {
            lastTargetPosition = targetPlayer.position();
        }
        position = position.add(direction.scale(speed));

        ServerLevel level1 = server.getLevel(Level.OVERWORLD);

        assert level1 != null;
        if (level1.isPositionEntityTicking(BlockPos.containing(position))) {
            MissileManager.queueRemoval(this);
            return;
        }
    }
    private void tickLaunch() {
        //System.err.println("launch RAM!");
        speed = Math.min(speed + 0.1, maxSpeed);

        Vec3 desiredDirection =
                direction.add(0, 1, 0).normalize();

        direction =
                direction.lerp(
                        desiredDirection,
                        0.1
                ).normalize();

        if (position.y >= cruiseHeight) {
            missileState = GuidedMissile.MissileState.CRUISE;
        }
    }
    private void tickCruise(Vec3 targetPos) {
        //System.err.println("cruise RAM!");
        Vec3 toTarget =
                targetPos.subtract(position);

        double horizontalDistance =
                Math.sqrt(
                        toTarget.x * toTarget.x +
                                toTarget.z * toTarget.z
                );

        if (horizontalDistance < 30) {
            missileState = GuidedMissile.MissileState.PREDATOR;
            return;
        }

        double altitudeError =
                cruiseHeight - position.y;

        Vec3 desiredDirection =
                new Vec3(
                        toTarget.x,
                        altitudeError * 0.1,
                        toTarget.z
                ).normalize();

        direction =
                direction.lerp(
                        desiredDirection,
                        0.05
                ).normalize();

        speed = Math.min(speed + 0.05, maxSpeed);
    }
    private void tickTerminal(Vec3 targetPos) {
        //System.err.println("RAM HUNT!!!!");
        speed = maxSpeed;

        Vec3 desiredDirection =
                targetPos.subtract(position)
                        .normalize();

        direction =
                direction.lerp(
                        desiredDirection,
                        0.15
                ).normalize();
    }
}