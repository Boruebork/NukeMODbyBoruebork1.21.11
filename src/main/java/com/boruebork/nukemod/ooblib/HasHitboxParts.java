package com.boruebork.nukemod.ooblib;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Implement on any Entity subclass that has hitbox parts (tanks, planes, ...).
 * The default method does the local -> world transform each call: rotate the
 * part's local offset by the entity's orientation, add the entity's world
 * position, and compose the part's local rotation with the entity's.
 * <p>
 * "Entity orientation" here should come from wherever you already store it
 * for rendering -- e.g. your synced quaternion field for planes, or yRot for
 * something that only yaws like a tank hull. Swap getEntityRotation() below
 * for whatever that accessor actually is per entity type.
 */
public interface HasHitboxParts {
    default Entity self() { return (Entity) this; }

    Quaternionf getEntityRotation(); // world orientation of the entity's origin

    default List<OBB> computeWorldHitboxes() {
        List<HitboxPart> parts = HitboxDefinitions.get(self().getType());
        if (parts.isEmpty()) return List.of();

        Quaternionf entityRot = getEntityRotation();
        Vector3f entityPos = self().position().toVector3f();

        List<OBB> result = new ArrayList<>(parts.size());

        for (HitboxPart part : parts) {

            // Hitbox position relative to the pivot.
            Vector3f localOffset = new Vector3f(part.localOffset())
                    .sub(part.localPivot());

            // Rotate around the pivot.
            entityRot.transform(localOffset);

            // Put it back relative to the pivot.
            localOffset.add(part.localPivot());

            // Finally move it into world space.
            localOffset.add(entityPos);

            Quaternionf worldRot =
                    new Quaternionf(entityRot)
                            .mul(part.localRotation());

            result.add(new OBB(
                    localOffset,
                    new Vector3f(part.localHalfExtents()),
                    worldRot
            ));
        }

        return result;
    }

    /**
     * Convenience for hit-scan / projectile code: first part hit along a ray, or null.
     */
    default PartHit raycastParts(Vector3f origin, Vector3f dir) {
        List<HitboxPart> parts = HitboxDefinitions.get(self().getType());
        List<OBB> obbs = computeWorldHitboxes();

        HitboxPart bestPart = null;
        float bestT = Float.POSITIVE_INFINITY;

        for (int i = 0; i < obbs.size(); i++) {
            float t = obbs.get(i).rayIntersect(origin, dir);
            if (t >= 0f && t < bestT) {
                bestT = t;
                bestPart = parts.get(i);
            }
        }
        return bestPart == null ? null : new PartHit(bestPart, bestT);
    }

    record PartHit(HitboxPart part, float distance) {
    }
    default OBB getWorldOBB(HitboxPart part) {
        return getWorldOBBAt(part, getEntityRotation(), self().getX(), self().getY(), self().getZ());
    }
    default OBB getWorldOBBAt(HitboxPart part, Quaternionf entityRot, double ex, double ey, double ez) {
        Vector3f center = new Vector3f(part.localOffset()).sub(part.localPivot());
        entityRot.transform(center);
        center.add(part.localPivot());
        center.add((float) ex, (float) ey, (float) ez);

        Quaternionf worldRot = new Quaternionf(entityRot).mul(part.localRotation());
        return new OBB(center, new Vector3f(part.localHalfExtents()), worldRot);
    }
    default OBB getWorldOBBAt(HitboxPart part, double entityX, double entityY, double entityZ) {
        return getWorldOBBAt(part, getEntityRotation(), entityX, entityY, entityZ);
    }
    default AABB getOBBAABB(OBB obb) { return obb.toAABB(); }
    default AABB getBroadphaseAABB() {
        return computeEnclosingAABB(self().position());
    }
    default List<HitboxPart> getHitboxParts() {
        return HitboxDefinitions.get(self().getType());
    }
    default boolean thisOBBsIntersect(OBB first, OBB second) { return first.intersects(second); }
    default OBB getWorldOBBAt(HitboxPart part, Quaternionf rot, double x, double y, double z, float shrink) {
        OBB o = getWorldOBBAt(part, rot, x, y, z);
        o.halfExtents.sub(shrink, shrink, shrink).max(new Vector3f(0.001f));
        return o;
    }
    default @Nullable AABB computeEnclosingAABB(Vec3 pos) {
        List<HitboxPart> parts = getHitboxParts();
        if (parts.isEmpty()) return null;
        Quaternionf rot = getEntityRotation();                       // built once, not per part
        AABB result = null;
        for (HitboxPart p : parts) {
            AABB b = getWorldOBBAt(p, rot, pos.x, pos.y, pos.z).toAABB();
            result = result == null ? b : result.minmax(b);
        }
        return result;
    }

    default @Nullable Vec3 raycastOBBs(Vec3 origin, Vec3 dir, double maxDist) {
        Vec3 p = self().position();
        Quaternionf rot = getEntityRotation();
        double best = maxDist;
        boolean hit = false;
        for (HitboxPart part : getHitboxParts()) {
            double t = getWorldOBBAt(part, rot, p.x, p.y, p.z).rayIntersect(origin, dir, best);
            if (t >= 0 && t < best) { best = t; hit = true; }
        }
        return hit ? origin.add(dir.scale(best)) : null;
    }

    default double distanceSqToHitbox(Vec3 point) {
        Vec3 p = self().position();
        Quaternionf rot = getEntityRotation();
        double best = Double.MAX_VALUE;
        for (HitboxPart part : getHitboxParts())
            best = Math.min(best, getWorldOBBAt(part, rot, p.x, p.y, p.z).distanceSq(point));
        return best;
    }

    default void forEachOverlap(HasHitboxParts other, BiConsumer<HitboxPart, HitboxPart> out) {
        List<HitboxPart> a = getHitboxParts(), b = other.getHitboxParts();
        if (a.isEmpty() || b.isEmpty()) return;
        OBB[] oa = new OBB[a.size()], ob = new OBB[b.size()];
        for (int i = 0; i < oa.length; i++) oa[i] = getWorldOBB(a.get(i));          // each OBB built once,
        for (int j = 0; j < ob.length; j++) ob[j] = other.getWorldOBB(b.get(j));    // not n*m times
        for (int i = 0; i < oa.length; i++)
            for (int j = 0; j < ob.length; j++)
                if (oa[i].intersects(ob[j])) out.accept(a.get(i), b.get(j));
    }
    default AABB computeOBBEnclosingAABB() {
        AABB result = null;
        for (HitboxPart part : getHitboxParts()) {
            AABB box = getWorldOBB(part).toAABB();
            result = result == null ? box : result.minmax(box);
        }
        return result == null ? computeOBBEnclosingAABB() : result;
    }
    default boolean sourceCloseEnoughToHitbox(Vec3 p) {
        final double MAX_DIST = 1.2;
        double bestSq = Double.MAX_VALUE;

        for (HitboxPart part : getHitboxParts()) {
            OBB obb = getWorldOBB(part);
            double d2 = OBB.obbDistanceSq(obb, p);
            if (d2 < bestSq) bestSq = d2;
        }
        return bestSq <= MAX_DIST * MAX_DIST;
    }
    default boolean damageActuallyHitsHitbox(DamageSource source) {
        // 1. Projectiles: raycast the projectile's path against the drone's OBBs.
        if (source.getDirectEntity() instanceof Projectile proj) {
            Vec3 p1 = proj.position();
            Vec3 p0 = p1.subtract(proj.getDeltaMovement());   // previous tick position
            Vec3 delta = p1.subtract(p0);
            double len = delta.length();
            if (len < 1e-6) {
                float MAX_DIST = 1f;
                // Stationary projectile sitting in the drone — treat as contact.
                return distanceSqToHitbox(p1) <= MAX_DIST * MAX_DIST;
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
}
