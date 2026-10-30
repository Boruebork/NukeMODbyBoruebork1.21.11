package com.boruebork.nukemod.ooblib;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

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
        AABB result = null;
        for (HitboxPart part : getHitboxParts()) {
            AABB aabb = getOBBAABB(getWorldOBB(part));
            result = result == null ? aabb : result.minmax(aabb);
        }
        return result == null ? self().getBoundingBox() : result;
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
}
