package com.boruebork.nukemod.uav;

import com.boruebork.nukemod.aircraft.AbstractFixedWingAircraft;
import com.boruebork.nukemod.drone.ClientDroneManager;
import com.boruebork.nukemod.uav.flight.FlightParams;
import com.boruebork.nukemod.uav.flight.Sticks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public abstract class AbstractUAV extends AbstractFixedWingAircraft {
    protected abstract float getMass();
    protected abstract float getMaxPitchRatePerTick();
    protected abstract float getWingArea();
    protected abstract float getThrottleRate();
    protected abstract float getMaxThrust();
    public abstract float getRollStep();
    protected abstract float getMaxRoll();
    protected abstract float getRollFactor();
    public AbstractUAV(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
    // ---------------------------------------------------------------------
    // Force helpers
    // ---------------------------------------------------------------------

    public abstract float getYawRatePerTick();

    public float getPitchRatePerTick() {
        return getMaxPitchRatePerTick();
    }

    // ---------------------------------------------------------------------
    // Input
    // ---------------------------------------------------------------------
    private FlightParams params;
    protected FlightParams flightParams() {
        if (params == null) params = new FlightParams(getMass(), getMaxThrust(), getWingArea(),
                getThrottleRate(), getMaxPitchRatePerTick(), getYawRatePerTick(), getMaxRoll(),
                getRollFactor(), getRollStep(), getMaxSpeed());
        return params;
    }   // if any getter depends on mutable state (payload mass, say), don't cache

    protected abstract float getMaxSpeed();
    //=====================CAMERA BULLSHIT===================
    public Vector3f getCameraLocalOffset() {
        return getHitboxParts().getFirst().localOffset();
    }
    public Quaternionf getCameraRotation(float partialTick) {
        if (level().isClientSide()) {
            return new Quaternionf()
                    .rotateY((float) ClientDroneManager.PilotingClientState.yW)
                    .rotateX((float) ClientDroneManager.PilotingClientState.xW)
                    .rotateZ((float) Math.toRadians(getRenderRoll(partialTick)));
        } else {
            return new Quaternionf()
                    .rotateY((float) Math.toRadians(-getRenderYaw(partialTick)))
                    .rotateX((float) Math.toRadians(getRenderPitch(partialTick)))
                    .rotateZ((float) Math.toRadians(getRenderRoll(partialTick)));
        }
    }
    public Vec3 getCameraPosition(float partialTick) { return getRenderPosition(partialTick); }
    public Vec3 camPosFrom(Vector3f offset, float partialTick) {
        Vec3 center = getRenderPosition(partialTick);
        float yawRad = ClientDroneManager.PilotingClientState.cameraYaw * Mth.DEG_TO_RAD;
        float pitchRad = ClientDroneManager.PilotingClientState.cameraPitch * Mth.DEG_TO_RAD;
        double x = -Mth.sin(yawRad) * Mth.cos(pitchRad);
        double y = -Mth.sin(pitchRad);
        double z = Mth.cos(yawRad) * Mth.cos(pitchRad);
        double radius = offset.length();
        return center.add(-x * radius, -y * radius, -z * radius);
    }
    // ---------------------------------------------------------------------
    // Patrol mode
    // ---------------------------------------------------------------------

    //======================PATROL MODE=======================
    public BlockPos getPatrolCenter() {
        return ClientDroneManager.PilotingClientState.patrolCenter;
    }
    private PilotMode serverMode = PilotMode.FIXED;
    private BlockPos serverPatrolCenter;
    private UUID serverPatrolEntityUuid;
    private double patrolAngle = 0;
    public void setServerMode(PilotMode m) {
        if (m != this.serverMode) this.patrolAngle = 0;   // restart the orbit
        this.serverMode = m;
    }
    public void setPatrolBlockTarget(BlockPos pos) {
        this.serverPatrolCenter = pos;
        this.serverPatrolEntityUuid = null;
    }
    public void setPatrolEntityTarget(UUID uuid) {
        this.serverPatrolEntityUuid = uuid;
        this.serverPatrolCenter = null;
    }
    private BlockPos getServerPatrolCenter() {
        if (serverPatrolCenter != null) return serverPatrolCenter;
        if (serverPatrolEntityUuid != null && level() instanceof ServerLevel sl) {
            Entity e = sl.getEntity(serverPatrolEntityUuid);
            if (e != null) return e.blockPosition();
        }
        return null;
    }
    private static final double PATROL_RADIUS       = 40.0;
    private static final double PATROL_ALT_AGL      = 25.0;
    private static final double PATROL_ORBIT_RATE   = 0.012;   // rad/tick
    private static final float  PATROL_CRUISE_THR   = 0.55f;
    //=====================NETWORKING=======================
    /** Transformed sticks for the current mode. Used both for the packet and for local prediction. */
    public Sticks computeClientSticks() {
        float stickPitch = 0f, stickYaw = 0f, stickRoll = 0f, throttle = 0f;
        var st = ClientDroneManager.PilotingClientState.class;

        if (ClientDroneManager.PilotingClientState.mode
                == ClientDroneManager.PilotingClientState.PilotingMode.FIXED) {
            float yawErr = Mth.wrapDegrees(ClientDroneManager.PilotingClientState.cameraYaw - getYRot());
            final float LEVEL_FLIGHT_AOA = 5.0f;
            float pitchErr = Mth.wrapDegrees(
                    ClientDroneManager.PilotingClientState.cameraPitch
                            - LEVEL_FLIGHT_AOA - getXRot());
            stickYaw   = Mth.clamp(yawErr   / 30f, -1f, 1f);
            stickPitch = Mth.clamp(pitchErr / 30f, -1f, 1f);
            stickRoll  = Mth.clamp(ClientDroneManager.PilotingClientState.x, -1f, 1f);
            throttle   = ClientDroneManager.PilotingClientState.z;
        }
        return new Sticks(throttle, stickPitch, stickRoll, stickYaw);
    }
    //===================SERIALIZTION=================================
    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("PilotMode", this.serverMode.name());
        out.putDouble("PatrolAngle", this.patrolAngle);

        if (this.serverPatrolCenter != null) {
            out.store("PatrolCenter", BlockPos.CODEC, this.serverPatrolCenter);
        }
        if (this.serverPatrolEntityUuid != null) {
            out.store("PatrolEntity", net.minecraft.core.UUIDUtil.CODEC, this.serverPatrolEntityUuid);
        }
    }
    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);

        String modeStr = in.getStringOr("PilotMode", PilotMode.FIXED.name());
        try {
            this.serverMode = PilotMode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            this.serverMode = PilotMode.FIXED;
        }
        this.patrolAngle = in.getDoubleOr("PatrolAngle", 0.0);
        this.serverPatrolCenter = null;
        this.serverPatrolEntityUuid = null;
        in.read("PatrolCenter", BlockPos.CODEC).ifPresent(pos -> this.serverPatrolCenter = pos);
        in.read("PatrolEntity", net.minecraft.core.UUIDUtil.CODEC)
                .ifPresent(uuid -> this.serverPatrolEntityUuid = uuid);
        // Guard against PATROL with no target after load — don't idle silently.
        if (this.serverMode == PilotMode.PATROL
                && this.serverPatrolCenter == null
                && this.serverPatrolEntityUuid == null) {
            this.serverMode = PilotMode.FIXED;
        }
    }
    public enum PilotMode {
        FIXED, FREE, PATROL;
        public static PilotMode fromId(int id) { PilotMode[] v = values(); return id >= 0 && id < v.length ? v[id] : FIXED; }
    }

    @Override
    protected boolean predictsLocally() {
        return !level().isClientSide()
                || ClientDroneManager.PilotingClientState.mode == ClientDroneManager.PilotingClientState.PilotingMode.FIXED;
    }

    @Override protected void onModeReceived(int mode) { setServerMode(PilotMode.fromId(mode)); }

    @Override
    protected Sticks serverSticks() {
        Sticks pilot = super.serverSticks();                 // always call: it drains the input queue (see section 2)
        return serverMode == PilotMode.PATROL ? patrolSticks() : pilot;
    }

    private static final double RADIAL_GAIN = 0.15;

    private Sticks patrolSticks() {
        BlockPos c = getServerPatrolCenter();
        if (c == null) return Sticks.ZERO;

        double rx = getX() - (c.getX() + 0.5), rz = getZ() - (c.getZ() + 0.5);
        double rLen = Math.sqrt(rx * rx + rz * rz);
        if (rLen < 1e-3) { rx = PATROL_RADIUS; rz = 0; rLen = PATROL_RADIUS; }
        double ux = rx / rLen, uz = rz / rLen;
        double radialError = rLen - PATROL_RADIUS;

        double dx = -uz - ux * radialError * RADIAL_GAIN;
        double dz =  ux - uz * radialError * RADIAL_GAIN;
        double dLen = Math.sqrt(dx * dx + dz * dz);
        dx /= dLen; dz /= dLen;

        float desiredYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float desiredPitch = Mth.clamp((float) (-(c.getY() + PATROL_ALT_AGL - getY()) * 4.0), -15f, 15f);

        float yaw   = Mth.clamp(Mth.wrapDegrees(desiredYaw - getYRot()) / 25f, -1f, 1f);
        float pitch = Mth.clamp(Mth.wrapDegrees(desiredPitch - getXRot()) / 25f, -1f, 1f);
        float boost = Mth.clamp((float) Math.abs(radialError) * 0.02f, 0f, 0.15f);
        float thr   = Mth.clamp((PATROL_CRUISE_THR + boost - throttle) * 3f, -1f, 1f);

        patrolAngle += PATROL_ORBIT_RATE;
        return new Sticks(thr, pitch, 0f, yaw);              // Sticks(throttle, pitch, roll, yaw)
    }
}