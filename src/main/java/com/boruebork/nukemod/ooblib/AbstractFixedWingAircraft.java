package com.boruebork.nukemod.ooblib;

import com.boruebork.nukemod.network.packet.ReconcilePayload;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;

import static com.boruebork.nukemod.ooblib.FixedWingUtil.*;
import static com.boruebork.nukemod.ooblib.FixedWingUtil.AIR_DENSITY;
import static com.boruebork.nukemod.ooblib.FixedWingUtil.DRAG_BASE;
import static com.boruebork.nukemod.ooblib.FixedWingUtil.DRAG_INDUCED;
import static com.boruebork.nukemod.ooblib.FixedWingUtil.LIFT_SLOPE;
import static com.boruebork.nukemod.ooblib.FixedWingUtil.STALL_ANGLE;

public abstract class AbstractFixedWingAircraft extends EntityWIthRoll implements HasOBBs{
    private final OBBEntityCollisionHandler collisionHandler;
    private double throttle;
    float groundPitchRate;
    /** Distance from the wheel contact point up to the CoM, in blocks. */
    private static final float COM_HEIGHT = 0.18f;

    /** Max angular acceleration from full elevator at full airspeed, deg/tick². */
    private static final float ELEVATOR_PITCH_ACCEL = 3.0f;

    /** Wheel friction + low-speed damping. <1 dissipates energy. */
    private static final float GROUND_PITCH_DAMPING = 0.80f;

    /** Airspeed (blocks/tick) at which elevator reaches full authority. */
    private static final float ELEVATOR_FULL_AUTHORITY_SPEED = 0.5f;
    private float inForward;
    private float inPitch;
    private float inRoll;
    private float inYaw;
    private float inThrottle;
    public AbstractFixedWingAircraft(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.collisionHandler = new OBBEntityCollisionHandler(this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

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

    @Override
    public OBBEntityCollisionHandler getOBBEntity() {
        return collisionHandler;
    }
    protected float controlAuthority() {
        double speed = getDeltaMovement().length();
        return (float) Mth.clamp(speed / 0.3, 0.0, 1.0);
    }
    protected abstract boolean isPilot();
    protected void applyMovement(
            float forwardStick,
            float rollStick,
            boolean up,
            boolean down,
            float yawStick,
            float pitchStick
    ) {
        //if (level().isClientSide()) return;
        boolean isPilot = isPilot();
        if (level().isClientSide() && !isPilot) return;
        // 1. Throttle integrator
        this.throttle = Mth.clamp(this.throttle + forwardStick * getThrottleRate(), 0f, 1f);

        // 2. Rotation from sticks, gated by control authority only
        // 2. Rotation
        float gate = controlAuthority();

        this.xRotO = getXRot();
        this.yRotO = getYRot();
        this.rollO  = this.roll;
        Vec3 vel = getDeltaMovement();
        double speed = vel.length();

        if (onGround()) {
            // ---- ground: torque model around the wheel contact point ----
            //
            // Elevator: angular acceleration from the elevator, scaled by airspeed.
            // Zero airspeed → zero effect, regardless of stick.
            float elevatorAuth = Mth.clamp(
                    (float) (speed / ELEVATOR_FULL_AUTHORITY_SPEED), 0f, 1f);
            float alphaElevator = pitchStick * ELEVATOR_PITCH_ACCEL * elevatorAuth;

            // Gravity restoring torque: alpha = -g·sin(θ) / h_com
            // GRAVITY is in blocks/tick², h_com in blocks, result in rad/tick² → deg/tick².
            double alphaGravRad = -GRAVITY * Math.sin(Math.toRadians(getXRot())) / COM_HEIGHT;
            float alphaGrav = (float) Math.toDegrees(alphaGravRad);

            // Integrate angular velocity
            groundPitchRate += alphaElevator + alphaGrav;
            groundPitchRate *= GROUND_PITCH_DAMPING;
            groundPitchRate  = Mth.clamp(groundPitchRate, -15f, 15f);

            setXRot(Mth.clamp(getXRot() + groundPitchRate, -90f, 90f));
        } else {
            // ---- air: kinematic pitch control (unchanged feel) ----
            setXRot(Mth.clamp(getXRot() + pitchStick * getMaxPitchRatePerTick() * gate, -90f, 90f));
            groundPitchRate = 0f;   // reset for next ground phase
        }

// Yaw is unchanged in both cases. If you add nose-wheel steering later,
// branch this line the same way and use a speed-independent rate on ground.
        setYRot(getYRot() + yawStick * getYawRatePerTick() * gate);

// Roll: kinematic both ground and air. Real ground roll is constrained
// by the landing gear, so the equilibrium is 0 — the auto-level in
// AbstractDrone.tick() handles that when no pilot is aboard.
        float rollTarget = Mth.clamp(
                -yawStick * getRollFactor() + rollStick * getMaxRoll(),
                -getMaxRoll(),
                getMaxRoll()
        );
        rollTarget = - rollTarget;
        float rollStep   = Mth.clamp(rollTarget - this.roll, -getRollStep(), getRollStep());
        this.roll += rollStep * gate;
        this.entityData.set(ROLL_DATA, this.roll);

        // 3. Body axes
        float yawRad   = this.getYRot() * Mth.DEG_TO_RAD;
        float pitchRad = this.getXRot() * Mth.DEG_TO_RAD;

        Vec3 forwardVec = new Vec3(
                Mth.sin(-yawRad) * Mth.cos(pitchRad),
                Mth.sin(-pitchRad),
                Mth.cos(-yawRad) * Mth.cos(pitchRad)
        );
        Vec3 bodyUp = new Vec3(
                Mth.sin(-yawRad) * Mth.sin(pitchRad),
                Mth.cos(pitchRad),
                Mth.cos(-yawRad) * Mth.sin(pitchRad)
        );
        Vec3 bodyRight = forwardVec.cross(bodyUp).normalize();
        // 4. Current velocity


        // 5. Forces
        Vec3 sideForce = Vec3.ZERO;
        // ---- Ground brake ----
        Vec3 brakeForce = Vec3.ZERO;
        if (onGround() && forwardStick < -0.05f) {
            Vec3 horizVel = new Vec3(vel.x, 0.0, vel.z);
            double horizSpeed = horizVel.length();
            if (horizSpeed > 1e-5) {
                // Deceleration magnitude in blocks/tick². Higher = harder brake.
                // 0.02 stops from 0.4 b/t in ~20 ticks (1 s).
                final double BRAKE_DECEL = 0.02;
                double applied = Math.min(BRAKE_DECEL, horizSpeed);
                brakeForce = horizVel.scale(-applied * getMass() / horizSpeed);
            }
        }
        if (speed > 1e-4) {
            double vSide = vel.dot(bodyRight);                 // sideways velocity (body frame)

            double q = 0.5 * AIR_DENSITY * speed * speed * getWingArea();

            final double TAIL_AREA_RATIO = 0.3;   // tail area / wing area
            final double CN_BETA         = 4.0;   // per radian, dCn/dbeta

            double sideMag = q * TAIL_AREA_RATIO * CN_BETA * (vSide / speed);
            sideForce = bodyRight.scale(-sideMag);
        }
        Vec3 thrustForce = forwardVec.scale(getMaxThrust() * this.throttle);
        Vec3 weightForce = new Vec3(0, -getMass() * GRAVITY, 0);

        Vec3 liftForce = Vec3.ZERO;
        Vec3 dragForce = Vec3.ZERO;
        double alphaDbg = 0;
        double qDbg = 0;
        double liftCoefDbg = 0;
        double dragCoefDbg = 0;

        if (speed > 1e-4) {
            Vec3 vDir = vel.scale(1.0 / speed);

            double sinAlpha = Mth.clamp(-vDir.dot(bodyUp), -1.0, 1.0);
            double alpha    = Math.asin(sinAlpha);
            double alphaClamped = Mth.clamp(alpha, -STALL_ANGLE, STALL_ANGLE);

            double q = 0.5 * AIR_DENSITY * speed * speed * getWingArea();

            Vec3 liftDir = bodyUp.subtract(vDir.scale(vDir.dot(bodyUp)));
            double liftDirLen = liftDir.length();
            if (liftDirLen > 1e-4) {
                liftDir = liftDir.scale(1.0 / liftDirLen);
            } else {
                liftDir = Vec3.ZERO;
            }

            double liftCoef = LIFT_SLOPE * alphaClamped;
            double dragCoef = DRAG_BASE + DRAG_INDUCED * alpha * alpha;

            liftForce = liftDir.scale(q * liftCoef);
            dragForce = vDir.scale(-q * dragCoef);

            alphaDbg    = alpha;
            qDbg        = q;
            liftCoefDbg = liftCoef;
            dragCoefDbg = dragCoef;
        }

        // 6. Integrate
        Vec3 netForce = thrustForce
                .add(liftForce)
                .add(dragForce)
                .add(weightForce)
                .add(sideForce)
                .add(brakeForce);
        Vec3 accel = netForce.scale(1.0 / getMass());
        Vec3 newVel = vel.add(accel);

        // Hard cap velocity and position.
        double spd = newVel.length();
        if (spd > getMaxSpeed()) newVel = newVel.scale(getMaxSpeed() / spd);

        // 7. Move and resolve collisions
        Vec3 actualMovement = collisionHandler.moveWithOBBTerrainCollision(newVel);

        Vec3 blocked = newVel.subtract(actualMovement);
        if (Math.abs(blocked.x) > 1e-4) newVel = new Vec3(0, newVel.y, newVel.z);
        if (Math.abs(blocked.y) > 1e-4) newVel = new Vec3(newVel.x, 0, newVel.z);
        if (Math.abs(blocked.z) > 1e-4) newVel = new Vec3(newVel.x, newVel.y, 0);

        double nx = getX() + newVel.x;
        double nz = getZ() + newVel.z;
        if (Math.abs(nx) > 29_999_000 || Math.abs(nz) > 29_999_000) {
            newVel = new Vec3(0, newVel.y, 0);
        }
        this.setDeltaMovement(newVel);
    }

    protected abstract float getThrottleRate();
    protected abstract float getMaxPitchRatePerTick();
    protected abstract float getWingArea();
    protected abstract float getMaxThrust();
    protected abstract float getRollStep();
    protected abstract float getMass();
    protected abstract float getMaxRoll();
    protected abstract float getRollFactor();
    protected abstract float getMaxSpeed();
    protected abstract float getYawRatePerTick();
    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            applyMovement(inForward, inRoll, false, false, inYaw, inPitch);
            collisionHandler.checkOBBCollisions();
            collisionHandler.checkGroundCollision();
        }
    }
    //=====================NETWORKING=======================
    /** Transformed sticks for the current mode. Used both for the packet and for local prediction. */
    public AbstractUAV.ClientSticks computeClientSticks() {
        float stickPitch = 0f, stickYaw = 0f, stickRoll = 0f, throttle = 0f;
        if (shouldSendSticks()) {
            float yawErr = Mth.wrapDegrees(FixedWingClientManager.PilotingClientState.cameraYaw - getYRot());
            final float LEVEL_FLIGHT_AOA = 5.0f;
            float pitchErr = Mth.wrapDegrees(
                    FixedWingClientManager.PilotingClientState.cameraPitch
                            - LEVEL_FLIGHT_AOA - getXRot());
            stickYaw   = Mth.clamp(yawErr   / 30f, -1f, 1f);
            stickPitch = Mth.clamp(pitchErr / 30f, -1f, 1f);
            stickRoll  = Mth.clamp(FixedWingClientManager.PilotingClientState.x, -1f, 1f);
            throttle   = FixedWingClientManager.PilotingClientState.z;
        }
        return new AbstractUAV.ClientSticks(throttle, stickPitch, stickRoll, stickYaw);
    }
    protected boolean shouldSendSticks(){
        return true;
    }
    protected boolean shouldClearReconciliationHistory() {
        return false;
    }
    public record ClientSticks(float throttle, float pitch, float roll, float yaw) {}
    protected void onClientPrediction() {
        AbstractUAV.ClientSticks s = computeClientSticks();
        applyMovement(s.throttle(), s.roll(), false, false, s.yaw(), s.pitch());
        FixedWingClientManager.PilotingClientState.roll  = this.roll;
        FixedWingClientManager.PilotingClientState.rollO = this.rollO;
        if (shouldClearReconciliationHistory()) {
            history.clear();
        }
        history.addLast(new StateTick(
                this.tickCount, position(), getDeltaMovement(),
                getYRot(), getXRot(), this.roll,
                new InputTick(s.throttle(), s.roll(), s.yaw(), s.pitch(),
                        0)));
        while (history.size() > HISTORY_SIZE) history.removeFirst();
    }
    private int lastInputTick = -1;
    public void setLastInputTick(int t) { this.lastInputTick = t; }
    public int getLastInputTick()      { return this.lastInputTick; }
    private record InputTick(float forward, float roll, float yaw, float pitch, int mode) {}
    private record StateTick(int tick, Vec3 pos, Vec3 vel, float yRot, float xRot, float roll, InputTick input) {}
    private final ArrayDeque<StateTick> history = new ArrayDeque<>();
    private static final int    HISTORY_SIZE      = 40;
    private static final double RECONCILE_THRESHOLD = 0.15;   // blocks
    private static final int    MAX_REPLAY_TICKS  = 8;
    public void reconcileFromServer(ReconcilePayload p) {
        // Find the history entry the server is acknowledging.
        StateTick match = null;
        for (StateTick h : history) {
            if (h.tick == p.inputTick()) { match = h; break; }
        }
        if (match == null) {
            // No local history for that tick — either it's stale or we weren't predicting.
            // Snap and move on. History will be repopulated next prediction tick.
            hardSet(p);
            return;
        }

        Vec3 serverPos = new Vec3(p.x(), p.y(), p.z());
        double err = match.pos.distanceTo(serverPos);
        int behind = this.tickCount - p.inputTick();

        if (err < RECONCILE_THRESHOLD || behind > MAX_REPLAY_TICKS) {
            // Prediction was accurate, or the correction is too old to replay against.
            // Snap position lightly to prevent slow drift, but keep the visual state.
            // A no-op when err is tiny.
            if (behind <= MAX_REPLAY_TICKS) {
                setDeltaMovement(p.dx(), p.dy(), p.dz());
            } else {
                hardSet(p);
            }
            return;
        }

        // Genuine divergence within the replay window: snap to authoritative state,
        // then replay our un-acknowledged inputs on top.
        hardSet(p);

        for (StateTick h : history) {
            if (h.tick <= p.inputTick()) continue;
            // Skip PATROL ticks — the server doesn't run our input there.
            //if (h.input.mode() != AbstractUAV.PilotMode.FIXED.ordinal()) continue;
            applyMovement(h.input.forward(), h.input.roll(),
                    false, false, h.input.yaw(), h.input.pitch());
        }

        // History is now inconsistent with the replayed state; drop it.
        history.clear();
    }
    private void hardSet(ReconcilePayload p) {
        setPos(p.x(), p.y(), p.z());
        setDeltaMovement(p.dx(), p.dy(), p.dz());
        setYRot(p.yRot());
        setXRot(p.xRot());
        this.roll = p.roll();
        this.rollO = p.roll();
        if (!level().isClientSide()) this.entityData.set(ROLL_DATA, this.roll);
    }
}
