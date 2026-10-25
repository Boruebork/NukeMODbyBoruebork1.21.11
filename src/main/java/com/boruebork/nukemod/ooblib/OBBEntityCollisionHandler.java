package com.boruebork.nukemod.ooblib;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

import static com.boruebork.nukemod.ooblib.OBBUtil.rayVsOBB;

public class OBBEntityCollisionHandler implements HasHitboxParts{
    protected final EntityWIthRoll entity;
    private static final double COLLISION_SKIN = 1.0E-3;
    public OBBEntityCollisionHandler(EntityWIthRoll entity) {
        this.entity = entity;
    }
    protected EntityWIthRoll getSelf() {return this.entity;}
    @Override
    public Entity self() {
        return getSelf();
    }

    @Override
    public Quaternionf getEntityRotation() {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-getSelf().getYRot()))
                .rotateX((float) Math.toRadians(getSelf().getXRot()))
                .rotateZ((float) Math.toRadians(getSelf().roll));

    }
    public Quaternionf getClientEntityRotation(float partialTick) {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-getSelf().getYRot(partialTick)))
                .rotateX((float) Math.toRadians(getSelf().getXRot(partialTick)))
                .rotateZ((float) Math.toRadians(getSelf().getRenderRoll(partialTick)));
    }
    public Vec3 raycastOBBs(Vec3 origin, Vec3 dir, double maxDist) {
        Quaternionf entityRot = getEntityRotation();
        double ex = getSelf().getX(), ey = getSelf().getY(), ez = getSelf().getZ();
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

    protected Vec3 moveWithOBBTerrainCollision(Vec3 desired) {
        double x = getSelf().getX(), y = getSelf().getY(), z = getSelf().getZ();

        if (hasTerrainCollisionAt(x, y, z)) {
            // Unstick vertically only
            double nudgeY = 0;
            for (double dy = 0.0625; dy <= 2.0; dy += 0.0625) {
                if (!hasTerrainCollisionAt(x, y + dy, z)) {
                    nudgeY = dy;
                    y += dy;
                    break;
                }
            }
            if (nudgeY == 0) {
                // Truly stuck
                getSelf().verticalCollision = true;
                getSelf().horizontalCollision = true;
                getSelf().setOnGround(true);
                return Vec3.ZERO;
            }

            // Now apply horizontal as normal
            double dx = clipAxis(x, y, z, desired.x, 0, 0);
            x += dx;
            double dz = clipAxis(x, y, z, 0, 0, desired.z);
            z += dz;

            Vec3 moved = new Vec3(x - getSelf().getX(), y - getSelf().getY(), z - getSelf().getZ());
            getSelf().setPos(x, y, z);
            return moved;
        }

        double dy = clipAxis(x, y, z, 0, desired.y, 0);
        y += dy;
        double dx = clipAxis(x, y, z, desired.x, 0, 0);
        x += dx;
        double dz = clipAxis(x, y, z, 0, 0, desired.z);
        z += dz;

        boolean hitVertical   = Math.abs(dy - desired.y) > 1e-7;
        boolean hitHorizontal = Math.abs(dx - desired.x) > 1e-7
                || Math.abs(dz - desired.z) > 1e-7;

        getSelf().verticalCollision   = hitVertical;
        getSelf().horizontalCollision = hitHorizontal;
        getSelf().setOnGround(hitVertical && desired.y < 0);

        Vec3 moved = new Vec3(x - getSelf().getX(), y - getSelf().getY(), z - getSelf().getZ());
        getSelf().setPos(x, y, z);
        return moved;
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
    protected boolean hasTerrainCollisionAt(double ex, double ey, double ez) {
        List<HitboxPart> parts = getHitboxParts();
        if (parts.isEmpty()) return false;

        final float SHRINK = 0.02f;

        Quaternionf rot = getEntityRotation();
        OBB[] obbs = new OBB[parts.size()];
        AABB[] boxes = new AABB[parts.size()];
        AABB broad = null;

        for (int i = 0; i < parts.size(); i++) {
            Vector3f he = new Vector3f(parts.get(i).localHalfExtents());
            he.x = Math.max(0.001f, he.x - SHRINK);
            he.y = Math.max(0.001f, he.y - SHRINK);
            he.z = Math.max(0.001f, he.z - SHRINK);

            Vector3f center = new Vector3f(parts.get(i).localOffset())
                    .sub(parts.get(i).localPivot());
            rot.transform(center);
            center.add(parts.get(i).localPivot());
            center.add((float) ex, (float) ey, (float) ez);

            Quaternionf worldRot = new Quaternionf(rot).mul(parts.get(i).localRotation());
            obbs[i] = new OBB(center, he, worldRot);
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
                if (!getSelf().level().hasChunkAt(pos)) return false;
                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);
                    BlockState state = getSelf().level().getBlockState(pos);
                    if (state.isAir()) continue;
                    VoxelShape shape = state.getCollisionShape(getSelf().level(), pos);
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
    protected boolean checkGroundCollision() {
        List<HitboxPart> parts = getHitboxParts();
        if (parts.isEmpty()) return false;

        AABB broadphase = getBroadphaseAABB();
        List<OBB> partOBBs = parts.stream().map(this::getWorldOBB).toList();

        int minX = Mth.floor(broadphase.minX), maxX = Mth.floor(broadphase.maxX);
        int minY = Mth.floor(broadphase.minY), maxY = Mth.floor(broadphase.maxY);
        int minZ = Mth.floor(broadphase.minZ), maxZ = Mth.floor(broadphase.maxZ);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                pos.set(x, minY, z);
                if (!getSelf().level().hasChunkAt(pos)) continue;
                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);
                    BlockState state = getSelf().level().getBlockState(pos);
                    if (state.isAir()) continue;

                    VoxelShape shape = state.getCollisionShape(getSelf().level(), pos);
                    if (shape.isEmpty()) continue;

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
    protected void onGroundCollision(HitboxPart part, BlockPos pos, BlockState state) {}
    protected void checkOBBCollisions() {
        AABB broadphase = getBroadphaseAABB();
        List<Entity> entities = getSelf().level().getEntitiesOfClass(
                Entity.class,
                broadphase,
                entity -> entity != getSelf() && entity.isPickable()
        );
        for (Entity entity : entities) {
            checkOBBCollision(entity);
        }
    }
    protected void checkOBBCollision(Entity ent) {
        if (!(ent instanceof HasOBBs other)) return;
        for (HitboxPart thisPart : getHitboxParts()) {
            for (HitboxPart otherPart : other.getOBBEntity().getHitboxParts()) {
                if (thisOBBsIntersect(getWorldOBB(thisPart), other.getOBBEntity().getWorldOBB(otherPart))) {
                    onOBBCollision(thisPart, other, otherPart);
                }
            }
        }
    }
    protected void onOBBCollision(HitboxPart thisPart, HasOBBs other, HitboxPart otherPart) {}
}
