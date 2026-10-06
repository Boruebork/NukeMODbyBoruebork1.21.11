package com.boruebork.nukemod.ooblib;

import com.boruebork.nukemod.drone.ClientDroneManager;
import com.boruebork.nukemod.mixin.*;

import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractDrone;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVDrone;
import com.boruebork.nukemod.entity.custom.uav.RQ4;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

public abstract class AbstractUAV extends AbstractDrone implements HasHitboxParts {

    // --- throttle / speed ---
    // Persistent, not instant: W/S nudge this toward getMaxSpeed()/getMinSpeed() each tick,
    // rather than the FPV model's direct "input * speedModifier = velocity this instant".
    private float speed = 0f;
    private static final double COLLISION_SKIN = 1.0E-3;      // back off a tiny bit from impact
    private static final double MIN_MOVE_SQR = 1.0E-10;       // dead-zone to suppress tiny jitter

    protected abstract float getMinSpeed();        // e.g. 0f, or a stall speed if you want "too slow = falls"
    protected abstract float getMaxSpeed();
    protected abstract float getAcceleration();     // speed change per tick at full throttle input

    // --- bank-derived (coordinated) turning ---
    // No direct yaw target from the pilot at all -- yaw only changes as a side effect of
    // however much the aircraft is currently banked, same as a real fixed-wing turn.
    protected abstract float getYawPerRollDegreePerTick(); // e.g. 0.05f: small, since it compounds every tick while banked
    protected abstract float getMaxPitchRatePerTick();     // still rate-limited; see note below on where targetPitch comes from

    // ============================================================
    // Client-side render interpolation.
    // ============================================================
    private double lerpXOld, lerpYOld, lerpZOld;
    private double lerpX, lerpY, lerpZ;
    private float lerpYawOld, lerpYaw;

    public AbstractUAV(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void applyMovement(float forward, float strafe, boolean up, boolean down, float targetYaw, float targetPitch) {
        // Runs on BOTH sides unconditionally (unlike AbstractFPVDrone, which only sets
        // rotation server-side because the client already got instant camera-coupled look
        // via updateLookClientSide). Here the client needs to run the same throttle/roll/yaw
        // physics locally too, or its own predicted flight won't match what the server
        // eventually confirms -- there's no shortcut via "the client already set it directly".

        // 1) throttle: W/S (forward) nudges persistent speed, doesn't set it directly
        this.speed = Mth.clamp(this.speed + forward * getAcceleration(), getMinSpeed(), getMaxSpeed());

        // 2) roll: A/D (strafe) is the bank target, eased by the inherited roll state machine
        float targetRoll = strafe * MAX_ROLL_DEGREES;
        updateRollTowards(targetRoll);

        // 3) yaw: derived from CURRENT roll (post-easing), not from any pilot-supplied target.
        float yawDeltaThisTick = -this.roll * getYawPerRollDegreePerTick();
        this.yRotO = this.getYRot();
        this.setYRot(this.getYRot() + yawDeltaThisTick);

        // 4) pitch: still rate-limited via the target passed in, independent of the yaw logic.
        // NOTE: this only actually does something useful once AbstractDrone.tick()'s client
        // branch passes the real mouse-desired pitch (PilotingClientState.xRot) here instead
        // of this.getXRot() -- see the AbstractDrone patch.
        this.xRotO = this.getXRot();
        float pitchDelta = Mth.clamp(targetPitch - this.getXRot(), -getMaxPitchRatePerTick(), getMaxPitchRatePerTick());
        this.setXRot(this.getXRot() + pitchDelta);

        // 5) move forward along the body's ACTUAL (lagging) orientation
        float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
        float pitchRad = this.getXRot() * Mth.DEG_TO_RAD;
        float sinYaw = Mth.sin(-yawRad);
        float cosYaw = Mth.cos(-yawRad);
        float sinPitch = Mth.sin(-pitchRad);
        float cosPitch = Mth.cos(pitchRad);
        Vec3 forwardVec = new Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);

        Vec3 desiredMovement = forwardVec.scale(this.speed);

        Vec3 actualMovement = moveWithOBBTerrainCollision(desiredMovement);
        this.setDeltaMovement(actualMovement);
    }

    @Override
    public Entity self() {
        return this;
    }

    @Override
    public Quaternionf getEntityRotation() {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-this.getYRot()))
                .rotateX((float) Math.toRadians(this.getXRot()));
    }

    /**
     * Overridden to a no-op. AbstractDrone's version directly snaps xRot/yRot from the
     * mouse every rendered FRAME -- correct for FPV's instant camera-body coupling, but
     * for a UAV it was silently overwriting the once-per-TICK rate-limited turning from
     * applyMovement()/turnTowards() on every single frame between ticks, which is why
     * rotation was fighting itself. UAV's rotation should change ONLY via applyMovement's
     * turnTowards() call, driven by the target passed in from AbstractDrone.tick() --
     * nothing here needs to touch the entity's rotation directly at all.
     */
    @Override
    public void updateLookClientSide(double mouseYaw, double mousePitch) {
        // intentionally empty
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {
            lerpXOld = getX();
            lerpYOld = getY();
            lerpZOld = getZ();
            lerpYawOld = getYRot();
            lerpPitchOld = getXRot();
        }
        setOldPos(position());
        super.tick();
        if (!level().isClientSide()) {
            checkOBBCollisions();
            checkGroundCollision();
        } else {
            // snapshot AFTER this tick's update -- the "new" end of the lerp
            lerpX = getX();
            lerpY = getY();
            lerpZ = getZ();

            lerpYaw = getYRot();
            lerpPitch = getXRot();
        }
    }

    private void setOldPos(Vec3 pos) {
        this.xo = this.xOld = pos.x;
        this.yo = this.yOld = pos.y;
        this.zo = this.zOld = pos.z;
    }

    /** Smoothed world position for rendering -- use instead of raw getX()/Y()/Z(). */
    public Vec3 getRenderPosition(float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, this.xo, this.getX()),
                Mth.lerp(partialTick, this.yo, this.getY()),
                Mth.lerp(partialTick, this.zo, this.getZ())
        );
    }

    /** Smoothed yaw for rendering -- wraps correctly across the +/-180 boundary. */
    public float getRenderYaw(float partialTick) {
        return Mth.rotLerp(partialTick, lerpYawOld, lerpYaw);
    }
    private float lerpPitchOld, lerpPitch;
    /** Smoothed pitch for rendering -- use instead of raw getXRot(). */
    public float getRenderPitch(float partialTick) {
        return Mth.lerp(partialTick, lerpPitchOld, lerpPitch);
    }

    /** Smoothed roll for rendering -- use instead of raw this.roll. */
    public float getRenderRoll(float partialTick) {
        return ClientDroneManager.PilotingClientState.drone.getRoll();
    }

    /** Nose-camera offset in the drone's own local space. Tune to your model. */
    public Vector3f getCameraLocalOffset() {
        return getHitboxParts().getFirst().localOffset();
    }

    /** Full camera orientation (yaw+pitch+roll), smoothed for rendering. */
    public Quaternionf getCameraRotation(float partialTick) {
        float yaw = getRenderYaw(partialTick);
        float pitch = getRenderPitch(partialTick);
        float roll = getRenderRoll(partialTick);
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(pitch))
                .rotateZ((float) Math.toRadians(roll));
    }

    /** World-space camera position for this tick/frame -- offset rotated by current orientation. */
    public Vec3 getCameraPosition(float partialTick) {
        return getRenderPosition(partialTick);
    }

    protected Vec3 moveWithOBBTerrainCollision(Vec3 desired) {
        double x = getX(), y = getY(), z = getZ();

        for (int i = 0; i < 16 && hasTerrainCollisionAt(x, y, z); i++) {
            y += 1.0 / 16.0;
        }

        double dy = clipAxis(x, y, z, 0, desired.y, 0);
        y += dy;
        double dx = clipAxis(x, y, z, desired.x, 0, 0);
        x += dx;
        double dz = clipAxis(x, y, z, 0, 0, desired.z);
        z += dz;

        boolean hitVertical = Math.abs(dy - desired.y) > 1.0E-7;
        boolean hitHorizontal = Math.abs(dx - desired.x) > 1.0E-7 || Math.abs(dz - desired.z) > 1.0E-7;

        this.verticalCollision = hitVertical;
        this.horizontalCollision = hitHorizontal;
        this.setOnGround(hitVertical && desired.y < 0);

        if (hitHorizontal) {
            this.speed *= 0.5f;
        }

        Vec3 moved = new Vec3(x - getX(), y - getY(), z - getZ());
        this.setPos(x, y, z);
        return moved;
    }

    protected boolean hasTerrainCollisionAt(double ex, double ey, double ez) {
        List<HitboxPart> parts = getHitboxParts();
        if (parts.isEmpty()) return false;

        Quaternionf rot = getEntityRotation();
        OBB[] obbs = new OBB[parts.size()];
        AABB[] boxes = new AABB[parts.size()];
        AABB broad = null;
        for (int i = 0; i < parts.size(); i++) {
            obbs[i] = getWorldOBBAt(parts.get(i), rot, ex, ey, ez);
            boxes[i] = obbs[i].toAABB();
            broad = broad == null ? boxes[i] : broad.minmax(boxes[i]);
        }

        int minX = Mth.floor(broad.minX), maxX = Mth.floor(broad.maxX);
        int minY = Mth.floor(broad.minY), maxY = Mth.floor(broad.maxY);
        int minZ = Mth.floor(broad.minZ), maxZ = Mth.floor(broad.maxZ);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                pos.set(x, minY, z);
                if (!level().hasChunkAt(pos)) {
                    return true;
                }
                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);
                    BlockState state = level().getBlockState(pos);
                    if (state.isAir()) continue;

                    VoxelShape shape = state.getCollisionShape(level(), pos);
                    if (shape.isEmpty()) continue;

                    for (AABB local : shape.toAabbs()) {
                        AABB world = local.move(pos);
                        for (int i = 0; i < obbs.length; i++) {
                            if (!boxes[i].intersects(world)) continue;
                            if (obbs[i].intersects(OBB.fromAABB(world))) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected OBB getWorldOBBAt(HitboxPart part, Quaternionf entityRot, double ex, double ey, double ez) {
        Vector3f center = new Vector3f(part.localOffset()).sub(part.localPivot());
        entityRot.transform(center);
        center.add(part.localPivot());
        center.add((float) ex, (float) ey, (float) ez);

        Quaternionf worldRot = new Quaternionf(entityRot).mul(part.localRotation());
        return new OBB(center, new Vector3f(part.localHalfExtents()), worldRot);
    }

    private double clipAxis(double x, double y, double z, double dx, double dy, double dz) {
        double full = dx + dy + dz;
        if (Math.abs(full) < 1.0E-9) return 0;

        if (!hasTerrainCollisionAt(x + dx, y + dy, z + dz)) return full;

        double low = 0, high = 1;
        for (int i = 0; i < 10; i++) {
            double mid = (low + high) * 0.5;
            if (hasTerrainCollisionAt(x + dx * mid, y + dy * mid, z + dz * mid)) high = mid;
            else low = mid;
        }
        double safe = full * low;
        return Math.abs(safe) <= COLLISION_SKIN ? 0 : safe - Math.signum(safe) * COLLISION_SKIN;
    }

    protected OBB getWorldOBBAt(HitboxPart part, double entityX, double entityY, double entityZ) {
        Vector3f localCenter = part.localOffset();
        Vector3f worldCenter = new Vector3f(localCenter).add(part.localPivot());

        getEntityRotation().transform(worldCenter);

        worldCenter.add(
                (float) entityX,
                (float) entityY,
                (float) entityZ
        );

        return new OBB(
                worldCenter,
                new Vector3f(part.localHalfExtents()),
                getEntityRotation()
        );
    }

    protected boolean checkGroundCollision() {
        AABB broadphase = getBroadphaseAABB();
        List<HitboxPart> parts = getHitboxParts();
        if (parts.isEmpty()) {
            return false;
        }

        List<OBB> partOBBs = parts.stream()
                .map(this::getWorldOBB)
                .toList();

        int minX = Mth.floor(broadphase.minX);
        int maxX = Mth.floor(broadphase.maxX);
        int minY = Mth.floor(broadphase.minY);
        int maxY = Mth.floor(broadphase.maxY);
        int minZ = Mth.floor(broadphase.minZ);
        int maxZ = Mth.floor(broadphase.maxZ);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                pos.set(x, minY, z);
                if (!level().hasChunkAt(pos)) {
                    continue;
                }

                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);

                    BlockState state = level().getBlockState(pos);
                    if (state.isAir()) continue;

                    VoxelShape shape = state.getCollisionShape(level(), pos);
                    if (shape.isEmpty()) continue;

                    for (AABB localBox : shape.toAabbs()) {
                        AABB worldBox = localBox.move(pos);
                        OBB blockOBB = OBB.fromAABB(worldBox);

                        for (int i = 0; i < partOBBs.size(); i++) {
                            if (partOBBs.get(i).intersects(blockOBB)) {
                                onGroundCollision(parts.get(i), pos.immutable(), state);
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    protected void onGroundCollision(HitboxPart part, BlockPos pos, BlockState state) {
        // this.setDeltaMovement(Vec3.ZERO);
        // this.speed = 0f;
    }

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

    @Override
    public void move(MoverType type, Vec3 movement) {
        Vec3 forwardVec = new Vec3(sin(this.getYRot()) * cos(this.getXRot()), sin(getXRot()), cos(getYRot()) * cos(getXRot()));

        Vec3 desiredMovement = forwardVec.scale(this.speed);
        Vec3 actualMovement = moveWithOBBTerrainCollision(desiredMovement);

        this.setDeltaMovement(actualMovement);
    }

    protected void checkOBBCollision(Entity other) {
        if (!(other instanceof AbstractUAV otherOBBEntity)) {
            return;
        }

        for (HitboxPart thisPart : getHitboxParts()) {
            for (HitboxPart otherPart : otherOBBEntity.getHitboxParts()) {
                OBB thisOBB = getWorldOBB(thisPart);
                OBB otherOBB = otherOBBEntity.getWorldOBB(otherPart);

                if (thisOBBsIntersect(thisOBB, otherOBB)) {
                    onOBBCollision(thisPart, otherOBBEntity, otherPart);
                }
            }
        }
    }

    protected AABB getBroadphaseAABB() {
        AABB result = null;

        for (HitboxPart part : getHitboxParts()) {
            OBB obb = getWorldOBB(part);
            AABB aabb = getOBBAABB(obb);

            if (result == null) {
                result = aabb;
            } else {
                result = result.minmax(aabb);
            }
        }

        if (result == null) {
            return getBoundingBox();
        }

        return result;
    }

    private List<HitboxPart> getHitboxParts() {
        return HitboxDefinitions.get(this.getType());
    }

    protected OBB getWorldOBB(HitboxPart part) {
        Vector3f localCenter = part.localOffset();
        Vector3f worldCenter = new Vector3f(localCenter).add(part.localPivot());

        getEntityRotation().transform(worldCenter);

        worldCenter.add(
                (float) getX(),
                (float) getY(),
                (float) getZ()
        );

        return new OBB(
                worldCenter,
                new Vector3f(part.localHalfExtents()),
                getEntityRotation()
        );
    }

    protected AABB getOBBAABB(OBB obb) {
        return obb.toAABB();
    }

    protected boolean thisOBBsIntersect(OBB first, OBB second) {
        return first.intersects(second);
    }

    protected void onOBBCollision(HitboxPart thisPart, AbstractUAV other, HitboxPart otherPart) {
        // Override this in RQ4 / specific UAV types.
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return super.isPushable();
    }

    public Vec3 camPosFrom(AbstractUAV uav, Vector3f local, float partialTick) {
        // 1) CG — vanilla entity interpolation (updated by setPos / packets)
        Vec3 cg = new Vec3(
                Mth.lerp(partialTick, uav.xo, uav.getX()),
                Mth.lerp(partialTick, uav.yo, uav.getY()),
                Mth.lerp(partialTick, uav.zo, uav.getZ())
        );

        // 2) Body attitude — same sources the renderer uses
        float yaw   = ClientDroneManager.PilotingClientState.yRot;   // must be rotLerp internally
        float pitch = ClientDroneManager.PilotingClientState.xRot;
        float roll  = ClientDroneManager.PilotingClientState.roll;

        Quaternionf body = new Quaternionf()
                .rotateY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(pitch))
                .rotateZ((float) Math.toRadians(roll));

        // 3) Rigid offset on the airframe
        Vector3f off = new Vector3f(local);
        body.transform(off);

        return cg.add(off.x, off.y, off.z);
    }
}