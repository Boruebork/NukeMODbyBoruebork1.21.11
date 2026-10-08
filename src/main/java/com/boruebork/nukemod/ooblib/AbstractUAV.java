package com.boruebork.nukemod.ooblib;

import com.boruebork.nukemod.drone.ClientDroneManager;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractDrone;
import com.boruebork.nukemod.network.packet.FixedWingInputPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public abstract class AbstractUAV extends AbstractDrone implements HasHitboxParts {

    private static final double COLLISION_SKIN = 1.0E-3;

    private float speed = 0f;

    private float yawWanted;
    private float yawWantedO;
    private float pitchWanted;
    private float pitchWantedO;
    private float rollWanted;
    private float rollWantedO;

    private double lerpXOld, lerpYOld, lerpZOld;
    private double lerpX, lerpY, lerpZ;
    private float lerpYawOld, lerpYaw;
    private float lerpPitchOld, lerpPitch;
    private float lerpRollOld, lerpRoll;

    protected abstract float getMinSpeed();
    protected abstract float getMaxSpeed();
    protected abstract float getAcceleration();
    protected abstract float getMaxPitchRatePerTick();

    public AbstractUAV(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void applyMovement(
            float forward,
            float strafe,
            boolean up,
            boolean down,
            float targetYaw,
            float targetPitch
    ) {
        this.speed = Mth.clamp(
                this.speed + forward * getAcceleration(),
                getMinSpeed(),
                getMaxSpeed()
        );
        //float yawErr = Mth.wrapDegrees(this.yawWanted - this.getYRot());
        //float autoBank = Mth.clamp(-yawErr * 2.0f, -MAX_ROLL_DEGREES, MAX_ROLL_DEGREES);
        /*this.rollWanted = Mth.clamp(
                autoBank + strafe * MAX_ROLL_DEGREES,
                -MAX_ROLL_DEGREES,
                MAX_ROLL_DEGREES
        );*/
        if (level().isClientSide()) {
            applyWantedRotation();

        } else {
            applyServerRotation(targetPitch, targetYaw);
        }

        float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
        float pitchRad = this.getXRot() * Mth.DEG_TO_RAD;
        Vec3 forwardVec = new Vec3(
                Mth.sin(-yawRad) * Mth.cos(pitchRad),
                Mth.sin(-pitchRad),
                Mth.cos(-yawRad) * Mth.cos(pitchRad)
        );

        Vec3 actualMovement = moveWithOBBTerrainCollision(forwardVec.scale(this.speed));
        this.setDeltaMovement(actualMovement);
    }
    protected void applyServerRotation(float newXRot, float newYRot) {
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        System.out.println("new: " + newXRot + "; " + newYRot);
        this.setXRot(newXRot);
        this.setYRot(newYRot);
        System.out.println("Server rotation changed to: " + getXRot() + "; " + getYRot());
    }
    protected abstract float getRollStep();
    protected abstract float getMaxRoll();
    protected abstract float getRollFactor();
    /** Only place UAV body rotation should change. Once per tick, from applyMovement. */
    protected void applyWantedRotation() {
        float yawStep = Mth.clamp(
                Mth.wrapDegrees(this.yawWanted - this.getYRot()),
                -getYawRatePerTick(),
                getYawRatePerTick()
        );
        this.yRotO = this.getYRot();
        this.setYRot(this.getYRot() + yawStep);

        float pitchStep = Mth.clamp(
                Mth.wrapDegrees(this.pitchWanted - this.getXRot()),
                -getPitchRatePerTick(),
                getPitchRatePerTick()
        );
        this.xRotO = this.getXRot();
        this.setXRot(Mth.clamp(this.getXRot() + pitchStep, -90f, 90f));

        float rollStep = Mth.clamp(
                Mth.wrapDegrees(this.rollWanted - this.roll),
                -getRollStep(),
                getRollStep()
        );

        this.rollO = this.roll;
        this.roll += rollStep;
    }

    protected float getYawRatePerTick() {
        return 1.5f;
    }

    protected float getPitchRatePerTick() {
        return getMaxPitchRatePerTick();
    }

    public float getRollWanted() {
        return rollWanted;
    }

    @Override
    public Entity self() {
        return this;
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
    @Override
    public void tick() {
        if (level().isClientSide()) {
            lerpXOld = getX();
            lerpYOld = getY();
            lerpZOld = getZ();
            lerpYawOld = getYRot();
            lerpPitchOld = getXRot();
            lerpRollOld = this.roll;
        }

        setOldPos(position());
        super.tick();

        if (!level().isClientSide()) {
            checkOBBCollisions();
            checkGroundCollision();
            System.err.println("=================Server side===============");
            System.err.println("x: " + this.getX() + " y: " + this.getY() + " z: " + this.getZ());
            System.err.println("xRot: " + this.getXRot() + " yRot: " + this.getYRot());
            System.err.println("xRotW:" + this.pitchWanted + " yRotW:" + this.yawWanted);
        } else {
            applyWantedRotation();
            lerpX = getX();
            lerpY = getY();
            lerpZ = getZ();
            lerpYaw = getYRot();
            lerpPitch = getXRot();
            lerpRoll = this.roll;
            System.err.println("=================Client side===============");
            System.err.println("x: " + this.getX() + " y: " + this.getY() + " z: " + this.getZ());
            System.err.println("xRot: " + this.getXRot() + " yRot: " + this.getYRot());
            System.err.println("xRotW:" + this.pitchWanted + " yRotW:" + this.yawWanted);
            System.err.println("PCS xRotW:" + ClientDroneManager.PilotingClientState.xW + " yRotW:" + ClientDroneManager.PilotingClientState.yW);

        }
    }

    private void setOldPos(Vec3 pos) {
        this.xo = this.xOld = pos.x;
        this.yo = this.yOld = pos.y;
        this.zo = this.zOld = pos.z;
    }

    public Vec3 getRenderPosition(float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, this.xo, this.getX()),
                Mth.lerp(partialTick, this.yo, this.getY()),
                Mth.lerp(partialTick, this.zo, this.getZ())
        );
    }

    public float getRenderYaw(float partialTick) {
        return Mth.rotLerp(partialTick, lerpYawOld, lerpYaw);
    }

    public float getRenderPitch(float partialTick) {
        return Mth.lerp(partialTick, lerpPitchOld, lerpPitch);
    }

    public float getRenderRoll(float partialTick) {
        return Mth.lerp(partialTick, rollO, roll);
    }

    public Vector3f getCameraLocalOffset() {
        return getHitboxParts().getFirst().localOffset();
    }

    public Quaternionf getCameraRotation(float partialTick) {
        if (level().isClientSide()) {
            return new Quaternionf()
                    .rotateY((float) ClientDroneManager.PilotingClientState.yW)
                    .rotateX((float) ClientDroneManager.PilotingClientState.xW)
                    .rotateZ((float) Math.toRadians(getRenderRoll(partialTick)));
        }else {
            return new Quaternionf()
                    .rotateY((float) Math.toRadians(-getRenderYaw(partialTick)))
                    .rotateX((float) Math.toRadians(getRenderPitch(partialTick)))
                    .rotateZ((float) Math.toRadians(getRenderRoll(partialTick)));
        }
    }

    public Vec3 getCameraPosition(float partialTick) {
        return getRenderPosition(partialTick);
    }
    //client
    public Vec3 camPosFrom(Vector3f offset, float partialTick) {
        Vec3 center = getRenderPosition(partialTick);

        float yawRad = ClientDroneManager.PilotingClientState.yW * Mth.DEG_TO_RAD;
        float pitchRad = ClientDroneManager.PilotingClientState.xW * Mth.DEG_TO_RAD;

        double x = -Mth.sin(yawRad) * Mth.cos(pitchRad);
        double y = -Mth.sin(pitchRad);
        double z = Mth.cos(yawRad) * Mth.cos(pitchRad);

        double radius = offset.length();

        return center.add(
                -x * radius,
                -y * radius,
                -z * radius
        );
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
        boolean hitHorizontal = Math.abs(dx - desired.x) > 1.0E-7
                || Math.abs(dz - desired.z) > 1.0E-7;

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
        if (parts.isEmpty()) {
            return false;
        }

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
                    if (state.isAir()) {
                        continue;
                    }

                    VoxelShape shape = state.getCollisionShape(level(), pos);
                    if (shape.isEmpty()) {
                        continue;
                    }

                    for (AABB local : shape.toAabbs()) {
                        AABB world = local.move(pos);
                        for (int i = 0; i < obbs.length; i++) {
                            if (!boxes[i].intersects(world)) {
                                continue;
                            }
                            if (obbs[i].intersects(OBB.fromAABB(world))) {
                                return true;
                            }
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

    protected OBB getWorldOBBAt(HitboxPart part, double entityX, double entityY, double entityZ) {
        return getWorldOBBAt(part, getEntityRotation(), entityX, entityY, entityZ);
    }

    private double clipAxis(double x, double y, double z, double dx, double dy, double dz) {
        double full = dx + dy + dz;
        if (Math.abs(full) < 1.0E-9) {
            return 0;
        }
        if (!hasTerrainCollisionAt(x + dx, y + dy, z + dz)) {
            return full;
        }

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

    protected boolean checkGroundCollision() {
        List<HitboxPart> parts = getHitboxParts();
        if (parts.isEmpty()) {
            return false;
        }

        AABB broadphase = getBroadphaseAABB();
        List<OBB> partOBBs = parts.stream().map(this::getWorldOBB).toList();

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
                    if (state.isAir()) {
                        continue;
                    }

                    VoxelShape shape = state.getCollisionShape(level(), pos);
                    if (shape.isEmpty()) {
                        continue;
                    }

                    for (AABB localBox : shape.toAabbs()) {
                        OBB blockOBB = OBB.fromAABB(localBox.move(pos));
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
        float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
        float pitchRad = this.getXRot() * Mth.DEG_TO_RAD;
        Vec3 forwardVec = new Vec3(
                Mth.sin(-yawRad) * Mth.cos(pitchRad),
                Mth.sin(-pitchRad),
                Mth.cos(-yawRad) * Mth.cos(pitchRad)
        );
        this.setDeltaMovement(moveWithOBBTerrainCollision(forwardVec.scale(this.speed)));
    }

    protected void checkOBBCollision(Entity other) {
        if (!(other instanceof AbstractUAV otherUav)) {
            return;
        }
        for (HitboxPart thisPart : getHitboxParts()) {
            for (HitboxPart otherPart : otherUav.getHitboxParts()) {
                if (thisOBBsIntersect(getWorldOBB(thisPart), otherUav.getWorldOBB(otherPart))) {
                    onOBBCollision(thisPart, otherUav, otherPart);
                }
            }
        }
    }

    protected AABB getBroadphaseAABB() {
        AABB result = null;
        for (HitboxPart part : getHitboxParts()) {
            AABB aabb = getOBBAABB(getWorldOBB(part));
            result = result == null ? aabb : result.minmax(aabb);
        }
        return result == null ? getBoundingBox() : result;
    }

    private List<HitboxPart> getHitboxParts() {
        return HitboxDefinitions.get(this.getType());
    }

    protected OBB getWorldOBB(HitboxPart part) {
        return getWorldOBBAt(part, getEntityRotation(), getX(), getY(), getZ());
    }

    protected AABB getOBBAABB(OBB obb) {
        return obb.toAABB();
    }

    protected boolean thisOBBsIntersect(OBB first, OBB second) {
        return first.intersects(second);
    }

    protected void onOBBCollision(HitboxPart thisPart, AbstractUAV other, HitboxPart otherPart) {
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
    /// server gets info about rotations from the client!
    public void updateFixedWingRot(FixedWingInputPayload payload) {
        this.yawWantedO = this.yawWanted;
        this.yawWanted = payload.yRotW();
        this.pitchWantedO = this.pitchWanted;
        this.pitchWanted = payload.xRotW();
        this.rollWantedO = this.rollWanted;
        this.rollWanted = payload.rollW();
        this.roll = payload.roll();
        applyMovement(payload.dz(), payload.forward(), false, false, payload.yRot(), payload.xRot());
    }
    private void updateRollWanted() {
        float yawDelta = Mth.wrapDegrees(yawWanted - yawWantedO);
        rollWanted = Mth.clamp(
                -yawDelta * getRollFactor(),
                -getMaxRoll(),
                getMaxRoll()
        );
    }
    public void setYawWanted(float yW) {
        this.yawWantedO = this.yawWanted;
        this.yawWanted = yW;
        updateRollWanted();
    }
    public void setPitchWanted(float xW) {
        this.pitchWantedO = this.pitchWanted;
        this.pitchWanted = xW;
    }
}