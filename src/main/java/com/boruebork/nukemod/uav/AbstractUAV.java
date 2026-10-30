package com.boruebork.nukemod.uav;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.drone.ClientDroneManager;
import com.boruebork.nukemod.drone.AbstractDrone;
import com.boruebork.nukemod.network.packet.ReconcilePayload;
import com.boruebork.nukemod.ooblib.HasHitboxParts;
import com.boruebork.nukemod.ooblib.HitboxDefinitions;
import com.boruebork.nukemod.ooblib.HitboxPart;
import com.boruebork.nukemod.ooblib.OBB;
import com.boruebork.nukemod.uav.collision.TerrainCollider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;

import static com.boruebork.nukemod.ooblib.FixedWingUtil.*;

public abstract class AbstractUAV extends AbstractDrone implements HasHitboxParts {
    // --- ground pitch physics ---
    private float groundPitchRate = 0f;   // deg / tick, only used while onGround

    /** Distance from the wheel contact point up to the CoM, in blocks. */
    private static final float COM_HEIGHT = 0.18f;

    /** Max angular acceleration from full elevator at full airspeed, deg/tick². */
    private static final float ELEVATOR_PITCH_ACCEL = 3.0f;

    /** Wheel friction + low-speed damping. <1 dissipates energy. */
    private static final float GROUND_PITCH_DAMPING = 0.80f;

    /** Airspeed (blocks/tick) at which elevator reaches full authority. */
    private static final float ELEVATOR_FULL_AUTHORITY_SPEED = 0.5f;
    private static final double COLLISION_SKIN = 1.0E-3;
    private static final boolean DEBUG = false;
    private static final int DEBUG_FULL_TICKS = 200;
    private static final int DEBUG_THROTTLE = 20;

    private BlockPos patrolCenter;
    private Entity patrolEntity;
    private float yawWanted;
    private float pitchWanted;
    private float rollWanted;
    private float throttle = 0f;
    private double patrolRadius = 50.0;
    private double patrolAltitude = 100.0;
    private double patrolCorrection = 0.05;

    private float inForward;
    private float inPitch;
    private float inRoll;
    private float inYaw;
    private float inThrottle;
    private int debugTick = 0;
    private boolean debugFirstTick = false;

    private TerrainCollider terrain;
    protected abstract float getMass();
    protected abstract float getMaxPitchRatePerTick();
    protected abstract float getWingArea();
    protected abstract float getThrottleRate();
    protected abstract float getMaxThrust();
    public abstract float getRollStep();
    protected abstract float getMaxRoll();
    protected abstract float getRollFactor();
    @Override
    public boolean isClientAuthoritative() {
        if (level().isClientSide()) {
            return ClientDroneManager.PilotingClientState.drone == this
                    && ClientDroneManager.PilotingClientState.mode
                    == ClientDroneManager.PilotingClientState.PilotingMode.FIXED;
        }
        return !this.entityData.get(CONTROLLER_DATA).isEmpty();
    }
    public AbstractUAV(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.terrain = new TerrainCollider(this);
        this.interpolation = new InterpolationHandler(this, 1);
    }

    private boolean shouldLog(int tick) {
        return DEBUG && (tick <= DEBUG_FULL_TICKS || tick % DEBUG_THROTTLE == 0);
    }

    private static String fmt(Vec3 v) {
        return String.format("(%.3f, %.3f, %.3f)", v.x, v.y, v.z);
    }

    private static String fmt4(Vec3 v) {
        return String.format("(%.4f, %.4f, %.4f)", v.x, v.y, v.z);
    }

    private void log(String msg) {
        if (DEBUG) System.out.println("[UAV] " + msg);
    }

    // ---------------------------------------------------------------------
    // Force helpers
    // ---------------------------------------------------------------------

    protected float getWeight() {
        return getMass() * GRAVITY;
    }

    protected float controlAuthority() {
        double speed = getDeltaMovement().length();
        return (float) Mth.clamp(speed / 0.3, 0.0, 1.0);
    }

    public float getYawRatePerTick() {
        return 1.5f;
    }

    public float getPitchRatePerTick() {
        return getMaxPitchRatePerTick();
    }

    // ---------------------------------------------------------------------
    // Input
    // ---------------------------------------------------------------------
    private PilotMode getModeFromStreamCodec(int sMode) {
        if (sMode == 0) return PilotMode.FIXED;
        if (sMode == 1) return PilotMode.FREE;
        if (sMode == 2) return PilotMode.PATROL;
        return null;
    }
    public void onInput(float forward, float pitch, float roll, float yaw, float throttle, int mode) {
        this.inForward  = Mth.clamp(forward,  -1f, 1f);
        this.inPitch    = Mth.clamp(pitch,    -1f, 1f);
        this.inRoll     = Mth.clamp(roll,     -1f, 1f);
        this.inYaw      = Mth.clamp(yaw,      -1f, 1f);
        this.inThrottle = Mth.clamp(throttle, -1f, 1f);
        this.serverMode = getModeFromStreamCodec(mode);
        if (DEBUG && debugTick <= 60) {
            log("onInput: fwd=" + forward + " pitch=" + pitch + " yaw=" + yaw
                    + " roll=" + roll + " → inForward=" + inForward);
        }
    }

    // ---------------------------------------------------------------------
    // Main physics — server only
    // ---------------------------------------------------------------------
    protected abstract float getMaxSpeed();
    @Override
    protected void applyMovement(float forwardStick, float rollStick, boolean up, boolean down, float yawStick, float pitchStick) {
        //if (level().isClientSide()) return;
        boolean isPilot = level().isClientSide()
                && ClientDroneManager.PilotingClientState.drone == this;
        if (level().isClientSide() && !isPilot) return;

        /*if (DEBUG && !debugFirstTick) {
            debugFirstTick = true;
            log("=== applyMovement first call. Class=" + getClass().getSimpleName());
            log("  UUID=" + getUUID() + " hash=" + System.identityHashCode(this));
            log("  args: fwd=" + forwardStick + " pitch=" + pitchStick
                    + " yaw=" + yawStick + " roll=" + rollStick);
            log("  mass=" + getMass() + " maxThrust=" + getMaxThrust()
                    + " wingArea=" + getWingArea() + " throttleRate=" + getThrottleRate());
            log("  gravity=" + GRAVITY + " airDensity=" + AIR_DENSITY
                    + " dragBase=" + DRAG_BASE + " dragInduced=" + DRAG_INDUCED
                    + " liftSlope=" + LIFT_SLOPE + " stallAngle=" + STALL_ANGLE);
            log("  startPos=" + fmt(position()) + " startVel=" + fmt(getDeltaMovement()));
        }*/

        debugTick++;

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

            // Vertical-tail side-force model:
            //   F_side = -q * S_tail/S_wing * Cn_beta * beta
            //   beta   = asin(vSide / speed) ~ vSide/speed
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
        Vec3 actualMovement = moveWithOBBTerrainCollision(newVel);

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

        if (shouldLog(debugTick)) {
            log("T" + debugTick
                    + " pos=" + fmt(position())
                    + " rot=(p=" + String.format("%.2f", getXRot())
                    + " y=" + String.format("%.2f", getYRot())
                    + " r=" + String.format("%.2f", this.roll) + ")"
                    + " fwd=" + forwardStick
                    + " pitch=" + pitchStick
                    + " thr=" + String.format("%.3f", this.throttle)
                    + " speed=" + String.format("%.3f", speed)
                    + " auth=" + String.format("%.2f", gate));
            log("  F: thrust=" + fmt4(thrustForce)
                    + " weight=" + fmt4(weightForce)
                    + " lift=" + fmt4(liftForce)
                    + " drag=" + fmt4(dragForce)
                    + " side=" + fmt4(sideForce));
            log("  α=" + String.format("%.4f", alphaDbg)
                    + " q=" + String.format("%.4f", qDbg)
                    + " CL=" + String.format("%.4f", liftCoefDbg)
                    + " CD=" + String.format("%.4f", dragCoefDbg));
            log("  net=" + fmt4(netForce)
                    + " accel=" + fmt4(accel)
                    + " newVel=" + fmt4(newVel));
            log("  MOVE: req=" + fmt4(newVel)
                    + " actual=" + fmt4(actualMovement)
                    + " afterPos=" + fmt(position())
                    + " onGround=" + onGround());
        }
    }

    // ---------------------------------------------------------------------
    // Tick
    // ---------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (DEBUG && debugTick == 0) {
                log("tick: SERVER side. UUID=" + getUUID()
                        + " hash=" + System.identityHashCode(this)
                        + " inFwd=" + inForward + " inPitch=" + inPitch
                        + " inYaw=" + inYaw);
            }
            //setOldPos();
            if (serverMode == PilotMode.PATROL) {
                runPatrolAutopilot();
            }

            applyMovement(inForward, inRoll, false, false, inYaw, inPitch);
            checkOBBCollisions();
            checkGroundCollision();
        } else {

            if (DEBUG && debugTick == 0) {

                log("tick: CLIENT side. UUID=" + getUUID()
                        + " hash=" + System.identityHashCode(this));
            }
        }
        sanitizeState();
    }

    // ---------------------------------------------------------------------
    // Collision / movement
    // ---------------------------------------------------------------------

    protected Vec3 moveWithOBBTerrainCollision(Vec3 desired) {
        Vec3 before = position();
        TerrainCollider.Result r = terrain.move(before, desired, getEntityRotation());
        if (r.flagsValid()) {
            this.verticalCollision = r.vertical();
            this.horizontalCollision = r.horizontal();
            this.setOnGround(r.onGround());
        }
        if (r.newPos() != before) setPos(r.newPos().x, r.newPos().y, r.newPos().z);
        return r.newPos().subtract(before);
    }
    private double clipAxis(double x, double y, double z, double dx, double dy, double dz) {
        double full = dx + dy + dz;
        if (Math.abs(full) < 1.0E-9) return 0;
        if (!hasTerrainCollisionAt(x + dx, y + dy, z + dz)) return full;

        double low = 0, high = 1;
        for (int i = 0; i < 10; i++) {
            double mid = (low + high) * 0.5;
            if (hasTerrainCollisionAt(x + dx * mid, y + dy * mid, z + dz * mid)) {
                high = mid;
            } else {
                low = mid;
            }
        }

        double safe = full * low;
        return Math.abs(safe) <= COLLISION_SKIN ? 0 : safe - Math.signum(safe) * COLLISION_SKIN;
    }
    protected boolean hasTerrainCollisionAt(double x, double y, double z) {   // kept for resolveInitialPenetration
        return terrain.collidesAt(getEntityRotation(), x, y, z);
    }
    protected boolean checkGroundCollision() {
        TerrainCollider.Contact c = terrain.findContact(getEntityRotation(), position());
        if (c == null) return false;
        onGroundCollision(c.part(), c.pos(), c.state());
        return true;
    }

    protected void onGroundCollision(HitboxPart part, BlockPos pos, BlockState state) {}
    protected void checkOBBCollisions() {
        AABB broadphase = getBroadphaseAABB();
        List<Entity> entities = level().getEntitiesOfClass(
                Entity.class,
                broadphase,
                entity -> entity != this && entity.isPickable()
        );
        for (Entity entity : entities) {
            checkOBBCollision(entity);
        }
    }
    protected void checkOBBCollision(Entity other) {
        if (!(other instanceof AbstractUAV otherUav)) return;
        for (HitboxPart thisPart : getHitboxParts()) {
            for (HitboxPart otherPart : otherUav.getHitboxParts()) {
                if (thisOBBsIntersect(getWorldOBB(thisPart), otherUav.getWorldOBB(otherPart))) {
                    onOBBCollision(thisPart, otherUav, otherPart);
                }
            }
        }
    }
    protected void onOBBCollision(HitboxPart thisPart, AbstractUAV other, HitboxPart otherPart) {}
    public AABB getBroadphaseAABB() {
        AABB result = null;
        for (HitboxPart part : getHitboxParts()) {
            AABB aabb = getOBBAABB(getWorldOBB(part));
            result = result == null ? aabb : result.minmax(aabb);
        }
        return result == null ? getBoundingBox() : result;
    }
    public AABB getOBBAABB(OBB obb) { return obb.toAABB(); }
    public boolean thisOBBsIntersect(OBB first, OBB second) { return first.intersects(second); }
    // ---------------------------------------------------------------------
    // OBB machinery
    // ---------------------------------------------------------------------

    public List<HitboxPart> getHitboxParts() {
        return HitboxDefinitions.get(this.getType());
    }
    public OBB getWorldOBB(HitboxPart part) {
        return getWorldOBBAt(part, getEntityRotation(), getX(), getY(), getZ());
    }
    public OBB getWorldOBBAt(HitboxPart part, Quaternionf entityRot, double ex, double ey, double ez) {
        Vector3f center = new Vector3f(part.localOffset()).sub(part.localPivot());
        entityRot.transform(center);
        center.add(part.localPivot());
        center.add((float) ex, (float) ey, (float) ez);

        Quaternionf worldRot = new Quaternionf(entityRot).mul(part.localRotation());
        return new OBB(center, new Vector3f(part.localHalfExtents()), worldRot);
    }
    public OBB getWorldOBBAt(HitboxPart part, double entityX, double entityY, double entityZ) {
        return getWorldOBBAt(part, getEntityRotation(), entityX, entityY, entityZ);
    }
    @Override
    public Quaternionf getEntityRotation() {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-this.getYRot()))
                .rotateX((float) Math.toRadians(this.getXRot()))
                .rotateZ((float) Math.toRadians(this.roll));
    }
    public Quaternionf getClientEntityRotation(float partialTick) {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-this.getYRot(partialTick)))
                .rotateX((float) Math.toRadians(this.getXRot(partialTick)))
                .rotateZ((float) Math.toRadians(this.getRenderRoll(partialTick)));
    }
    public Vec3 raycastOBBs(Vec3 origin, Vec3 dir, double maxDist) {
        Quaternionf entityRot = getEntityRotation();
        double ex = getX(), ey = getY(), ez = getZ();
        double bestT = maxDist;
        boolean hit = false;

        for (HitboxPart part : getHitboxParts()) {
            OBB obb = getWorldOBBAt(part, entityRot, ex, ey, ez);
            double t = rayVsOBB(origin, dir, obb, bestT);
            if (t >= 0 && t < bestT) {
                bestT = t;
                hit = true;
            }
        }
        return hit ? origin.add(dir.scale(bestT)) : null;
    }
    private static double rayVsOBB(Vec3 origin, Vec3 dir, OBB obb, double maxT) {
        Quaternionf inv = new Quaternionf(obb.rotation).conjugate();
        Vector3f o = new Vector3f(
                (float)(origin.x - obb.center.x),
                (float)(origin.y - obb.center.y),
                (float)(origin.z - obb.center.z));
        inv.transform(o);
        Vector3f d = new Vector3f((float)dir.x, (float)dir.y, (float)dir.z);
        inv.transform(d);
        Vector3f h = obb.halfExtents;
        double tmin = 0.0, tmax = maxT;

        for (int i = 0; i < 3; i++) {
            float oi = i == 0 ? o.x : i == 1 ? o.y : o.z;
            float di = i == 0 ? d.x : i == 1 ? d.y : d.z;
            float hi = i == 0 ? h.x : i == 1 ? h.y : h.z;
            if (Math.abs(di) < 1e-8f) {
                if (Math.abs(oi) > hi) return -1;
                continue;
            }
            double t1 = (-hi - oi) / di;
            double t2 = ( hi - oi) / di;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return -1;
        }
        return tmin;
    }
    // ---------------------------------------------------------------------
    // Bounding box / rendering helpers
    // ---------------------------------------------------------------------

    @Override
    protected @NonNull AABB makeBoundingBox(Vec3 pos) {
        return computeOBBEnclosingAABB();
    }
    public AABB computeOBBEnclosingAABB() {
        AABB result = null;
        for (HitboxPart part : getHitboxParts()) {
            AABB box = getWorldOBB(part).toAABB();
            result = result == null ? box : result.minmax(box);
        }
        return result == null ? super.makeBoundingBox() : result;
    }
    //=========================RENDERING BULLSHIT==================
    public Vec3 getRenderPosition(float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, this.xo, this.getX()),
                Mth.lerp(partialTick, this.yo, this.getY()),
                Mth.lerp(partialTick, this.zo, this.getZ())
        );
    }
    public float getRenderYaw(float partialTick) { return Mth.rotLerp(partialTick, yRotO, getYRot()); }
    public float getRenderPitch(float partialTick) { return Mth.lerp(partialTick, xRotO, getXRot()); }
    public float getRenderRoll(float partialTick) {
        boolean isPilot = level().isClientSide()
                && ClientDroneManager.PilotingClientState.drone == this;
        float current = isPilot ? this.roll : this.entityData.get(ROLL_DATA);
        return Mth.lerp(partialTick, rollO, current);
    }
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
    // Sanitizer and startup
    // ---------------------------------------------------------------------

    private Vec3 lastGoodPos = null;
    private void sanitizeState() {
        if (lastGoodPos == null) lastGoodPos = position();

        if (!isFinite(getDeltaMovement())) setDeltaMovement(Vec3.ZERO);

        if (isFinite(position())) {
            lastGoodPos = position();
        } else {
            setDeltaMovement(Vec3.ZERO);
            this.throttle = 0f;
            setPos(lastGoodPos.x, lastGoodPos.y, lastGoodPos.z);
            NukeModbyBoruebork.LOGGER.warn("Recovered drone from NaN position → {}", lastGoodPos);
        }

        if (!Float.isFinite(this.throttle)) this.throttle = 0f;
        if (!Float.isFinite(this.roll))     this.roll = 0f;
        if (!Float.isFinite(getYRot()))     setYRot(0f);
        if (!Float.isFinite(getXRot()))     setXRot(0f);
    }
    private void resolveInitialPenetration() {
        double x = getX(), y = getY(), z = getZ();
        boolean stuck = hasTerrainCollisionAt(x, y, z);
        log("onAddedToLevel: pos=" + fmt(position()) + " stuck=" + stuck);
        for (int i = 0; i < 32 && hasTerrainCollisionAt(x, y, z); i++) {
            y += 1.0 / 16.0;
        }
        if (y != getY()) {
            setPos(x, y, z);
            log("onAddedToLevel: unstuck → " + fmt(position()));
        }
    }
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        resolveInitialPenetration();
    }
    private static boolean isFinite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    // ---------------------------------------------------------------------
    // Patrol mode
    // ---------------------------------------------------------------------

    @Override
    public boolean isPickable() {
        return true;
    }
    //======================PATROL MODE=======================
    public BlockPos getPatrolCenter() {
        return ClientDroneManager.PilotingClientState.patrolCenter;
    }
    public enum PilotMode { FIXED, FREE, PATROL }
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
    private void runPatrolAutopilot() {
        BlockPos center = getServerPatrolCenter();
        if (center == null) {
            inForward = inRoll = inYaw = inPitch = 0;
            return;
        }

        double cx = center.getX() + 0.5;
        double cz = center.getZ() + 0.5;
        double targetY = center.getY() + PATROL_ALT_AGL;

        // Vector from circle center to drone (horizontal).
        double rx = getX() - cx;
        double rz = getZ() - cz;
        double rLen = Math.sqrt(rx * rx + rz * rz);
        if (rLen < 1.0e-3) {
            // Sitting on the center — pick an arbitrary point on the circle.
            rx = PATROL_RADIUS; rz = 0; rLen = PATROL_RADIUS;
        }
        double ux = rx / rLen;   // unit radial out
        double uz = rz / rLen;

        // Tangent (90° CCW from radial): orbit travels counter-clockwise when viewed from above.
        double tx = -uz;
        double tz =  ux;

        // Radial error: positive if outside the circle, negative if inside.
        double radialError = rLen - PATROL_RADIUS;
        // Small inward/outward correction, scaled to not overwhelm the tangent.
        final double RADIAL_GAIN = 0.15;
        double cx_ = -ux * radialError * RADIAL_GAIN;
        double cz_ = -uz * radialError * RADIAL_GAIN;

        // Desired horizontal direction: mostly tangent, plus radial correction.
        double dx = tx + cx_;
        double dz = tz + cz_;
        double dLen = Math.sqrt(dx * dx + dz * dz);
        dx /= dLen; dz /= dLen;

        // Yaw that points the drone along (dx, dz).
        float desiredYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

        // Altitude hold: proportional to vertical error.
        double altErr = targetY - getY();
        float desiredPitch = Mth.clamp((float)(-altErr * 4.0), -15f, 15f);

        // Attitude commands.
        inYaw   = Mth.clamp(Mth.wrapDegrees(desiredYaw - getYRot()) / 25f, -1f, 1f);
        inPitch = Mth.clamp(Mth.wrapDegrees(desiredPitch - getXRot()) / 25f, -1f, 1f);
        inRoll  = 0f;

        // Throttle: hold cruise speed, but accelerate a touch harder when far off-target.
        float boost = Mth.clamp((float) Math.abs(radialError) * 0.02f, 0f, 0.15f);
        float targetThrottle = PATROL_CRUISE_THR + boost;
        inForward = Mth.clamp((targetThrottle - this.throttle) * 3f, -1f, 1f);

        // Advance the angle only for bookkeeping — the drone no longer chases it directly.
        patrolAngle += PATROL_ORBIT_RATE;
    }
    //=====================NETWORKING=======================
    /** Transformed sticks for the current mode. Used both for the packet and for local prediction. */
    public ClientSticks computeClientSticks() {
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
        return new ClientSticks(throttle, stickPitch, stickRoll, stickYaw);
    }
    public record ClientSticks(float throttle, float pitch, float roll, float yaw) {}
    @Override
    protected void onClientPrediction() {
        AbstractUAV.ClientSticks s = computeClientSticks();
        applyMovement(s.throttle(), s.roll(), false, false, s.yaw(), s.pitch());
        ClientDroneManager.PilotingClientState.roll  = this.roll;
        ClientDroneManager.PilotingClientState.rollO = this.rollO;
        if (ClientDroneManager.PilotingClientState.PilotingMode.FIXED != ClientDroneManager.PilotingClientState.mode) {
            history.clear();
        }
        history.addLast(new StateTick(
                this.tickCount, position(), getDeltaMovement(),
                getYRot(), getXRot(), this.roll,
                new InputTick(s.throttle(), s.roll(), s.yaw(), s.pitch(),
                        ClientDroneManager.PilotingClientState.mode.ordinal())));
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
            if (h.input.mode() != PilotMode.FIXED.ordinal()) continue;
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
    //===================SERIALIZTION=================================
    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);

        out.putString("PilotMode", this.serverMode.name());
        out.putFloat("Throttle", this.throttle);
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

        this.throttle  = in.getFloatOr("Throttle", 0f);
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
    //======================DAMAGE SYSTEMS============================
    public static final EntityDataAccessor<Float> HEALTH_DATA =
            SynchedEntityData.defineId(AbstractUAV.class, EntityDataSerializers.FLOAT);
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);   // AbstractDrone's ROLL_DATA and CONTROLLER_DATA
        builder.define(HEALTH_DATA, getMaxHealth());
    }
    public float getHealth() { return this.entityData.get(HEALTH_DATA); }
    public void setHealth(float h) { this.entityData.set(HEALTH_DATA, Mth.clamp(h, 0f, getMaxHealth())); }
    public abstract float getMaxHealth();
    public abstract float getDeathExplosionPower();
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isRemoved()) return false;
        if (!this.damageActuallyHitsHitbox(source)) return false;
        if (isInvulnerableTo(level, source)) return false;

        // Vanilla-style invulnerability frames: ignore rapid re-hits.
        if (this.invulnerableTime > 10) return false;

        float scaled = scaleDamage(source, amount);
        if (scaled <= 0f) return false;

        float newHealth = getHealth() - scaled;
        setHealth(newHealth);
        this.invulnerableTime = 10;

        if (newHealth <= 0f) {
            destroy(source);
        }
        return true;
    }

    private float scaleDamage(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return amount;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return amount * 0.25f;
        if (source.getDirectEntity() instanceof Player || source.getDirectEntity() instanceof Mob) {
            return amount * 0.5f;
        }
        return 0f;
    }
    private boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (source.is(DamageTypeTags.IS_FALL))    return true;
        if (source.is(DamageTypeTags.IS_FIRE))    return true;
        if (source.is(DamageTypeTags.IS_DROWNING)) return true;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return false;
    }
    protected void destroy(DamageSource source) {
        if (!(level() instanceof ServerLevel sl)) return;

        sl.explode(this,
                getX(), getY(), getZ(),
                getDeathExplosionPower(),
                Level.ExplosionInteraction.NONE);   // NONE = no terrain damage

        this.stopOperating();   // clears controller, drops camera on the pilot's client
        this.discard();
    }
    /**
     * Reject damage whose source passed through the AABB but not through any OBB.
     * Vanilla has already done the AABB check; we refine it.
     */
    private boolean damageActuallyHitsHitbox(DamageSource source) {
        // 1. Projectiles: raycast the projectile's path against the drone's OBBs.
        if (source.getDirectEntity() instanceof Projectile proj) {
            Vec3 p1 = proj.position();
            Vec3 p0 = p1.subtract(proj.getDeltaMovement());   // previous tick position
            Vec3 delta = p1.subtract(p0);
            double len = delta.length();
            if (len < 1e-6) {
                // Stationary projectile sitting in the drone — treat as contact.
                return sourceCloseEnoughToHitbox(p1);
            }
            Vec3 dir = delta.scale(1.0 / len);
            // Extend the ray a bit so hits on the far side count.
            return raycastOBBs(p0, dir, len + 0.5) != null;
        }

        // 2. Explosions and melee: proximity from the source position.
        Vec3 srcPos = source.getSourcePosition();
        if (srcPos == null && source.getEntity() != null) {
            srcPos = source.getEntity().getEyePosition();
        }
        if (srcPos == null) return true;   // can't verify → allow

        return sourceCloseEnoughToHitbox(srcPos);
    }

    private boolean sourceCloseEnoughToHitbox(Vec3 p) {
        final double MAX_DIST = 1.2;
        double bestSq = Double.MAX_VALUE;

        for (HitboxPart part : getHitboxParts()) {
            OBB obb = getWorldOBB(part);
            double d2 = obbDistanceSq(obb, p);
            if (d2 < bestSq) bestSq = d2;
        }
        return bestSq <= MAX_DIST * MAX_DIST;
    }

    /** Squared distance from a world-space point to the nearest point on an OBB. */
    private static double obbDistanceSq(OBB obb, Vec3 p) {
        Vector3f local = new Vector3f(
                (float)(p.x - obb.center.x),
                (float)(p.y - obb.center.y),
                (float)(p.z - obb.center.z));
        new Quaternionf(obb.rotation).conjugate().transform(local);

        float dx = Math.max(0f, Math.abs(local.x) - obb.halfExtents.x);
        float dy = Math.max(0f, Math.abs(local.y) - obb.halfExtents.y);
        float dz = Math.max(0f, Math.abs(local.z) - obb.halfExtents.z);
        return dx * dx + dy * dy + dz * dz;
    }
}