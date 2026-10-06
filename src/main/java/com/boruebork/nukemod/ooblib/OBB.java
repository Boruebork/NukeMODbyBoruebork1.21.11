package com.boruebork.nukemod.ooblib;

import net.minecraft.world.phys.AABB;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Oriented bounding box: a center, half-extents along its own local axes,
 * and a rotation from local space into world space.
 *
 * Deliberately built on plain JOML types (Vector3f / Quaternionf), not
 * Minecraft's AABB or Vec3, so this class has zero Minecraft dependency.
 * Adapter methods that convert to/from Minecraft types should live in a
 * separate class in the mod that uses this lib, not here.
 */
public final class OBB {

    public final Vector3f center = new Vector3f();
    public final Vector3f halfExtents = new Vector3f(); // half-size along local X/Y/Z
    public final Quaternionf rotation = new Quaternionf(); // local -> world

    public OBB() {}

    public OBB(Vector3f center, Vector3f halfExtents, Quaternionf rotation) {
        this.center.set(center);
        this.halfExtents.set(halfExtents);
        this.rotation.set(rotation);
    }
    // ============================================================
// 1. Add this method to your OBB class (com.boruebork.nukemod.ooblib.OBB)
// ============================================================
//
// Needs: import net.minecraft.world.phys.AABB;  (OBB already lives under
// the nukemod package now, so pulling in a Minecraft type here is
// consistent with that -- the "zero Minecraft dependency" framing from
// when this was a standalone lib no longer applies now that it's merged
// into nukemod.ooblib directly.)
//
// Computes the tightest AABB enclosing all 8 rotated corners -- this is
// what "AABB that bounds this rotated box" actually means; there's no
// shortcut via half-extents alone once rotation is involved, since a
// rotated box's axis-aligned footprint is wider than its own half-extents.

    /**
     * The tightest axis-aligned box enclosing this OBB in its current
     * orientation. Broad-phase only -- recompute whenever center/rotation
     * change (i.e. every tick for a moving vehicle), never cache this.
     */
    public AABB toAABB() {
        float[][] signs = {
                {-1,-1,-1}, { 1,-1,-1}, { 1,-1, 1}, {-1,-1, 1},
                {-1, 1,-1}, { 1, 1,-1}, { 1, 1, 1}, {-1, 1, 1},
        };

        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        Vector3f corner = new Vector3f();
        for (float[] s : signs) {
            corner.set(halfExtents.x * s[0], halfExtents.y * s[1], halfExtents.z * s[2]);
            rotation.transform(corner); // local -> world orientation, no translation yet
            corner.add(center);

            minX = Math.min(minX, corner.x); maxX = Math.max(maxX, corner.x);
            minY = Math.min(minY, corner.y); maxY = Math.max(maxY, corner.y);
            minZ = Math.min(minZ, corner.z); maxZ = Math.max(maxZ, corner.z);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

// ============================================================
// 2. Fix in AbstractUAV.java -- replace the broken stub:
// ============================================================
//
//   protected AABB getOBBAABB(OBB obb) {
//       // Implement according to your OBB API.
//       return obb.getOBBAABB(obb);   // <- calls itself with itself, not valid
//   }
//
// with:

    protected AABB getOBBAABB(OBB obb) {
        return obb.toAABB();
    }
    /** Local-space unit axis i (0=X,1=Y,2=Z), rotated into world space. */
    public Vector3f axis(int i, Vector3f dest) {
        dest.set(i == 0 ? 1 : 0, i == 1 ? 1 : 0, i == 2 ? 1 : 0);
        return rotation.transform(dest);
    }
    public static OBB fromAABB(AABB aabb) {
        Vector3f center = new Vector3f(
                (float) ((aabb.minX + aabb.maxX) * 0.5),
                (float) ((aabb.minY + aabb.maxY) * 0.5),
                (float) ((aabb.minZ + aabb.maxZ) * 0.5)
        );

        Vector3f halfExtents = new Vector3f(
                (float) ((aabb.maxX - aabb.minX) * 0.5),
                (float) ((aabb.maxY - aabb.minY) * 0.5),
                (float) ((aabb.maxZ - aabb.minZ) * 0.5)
        );

        return new OBB(
                center,
                halfExtents,
                new Quaternionf()
        );
    }
    /** Convenience: fills a 3x3 matrix whose columns are the world-space axes. */
    public Matrix3f basis(Matrix3f dest) {
        return dest.identity().rotate(rotation);
    }

    // ---------------------------------------------------------------
    // Ray vs OBB
    // ---------------------------------------------------------------

    /**
     * Slab test. Transforms the ray into the OBB's local space (where the
     * box is just an AABB from -halfExtents to +halfExtents) and runs the
     * standard AABB slab test there.
     *
     * @param origin ray origin, world space
     * @param dir    ray direction, world space (does not need to be normalized,
     *               but tMin/tMax below are then in units of |dir|)
     * @return distance along the ray to the entry point, or -1 if it misses
     *         or the box is entirely behind the origin.
     */
    public float rayIntersect(Vector3f origin, Vector3f dir) {
        // World -> local: inverse rotation, then subtract center (order matters).
        Vector3f localOrigin = new Vector3f(origin).sub(center);
        rotation.transformInverse(localOrigin);

        Vector3f localDir = new Vector3f(dir);
        rotation.transformInverse(localDir);

        float tMin = Float.NEGATIVE_INFINITY;
        float tMax = Float.POSITIVE_INFINITY;

        for (int i = 0; i < 3; i++) {
            float o = component(localOrigin, i);
            float d = component(localDir, i);
            float he = component(halfExtents, i);

            if (Math.abs(d) < 1e-8f) {
                // Ray parallel to this slab; must already be within it.
                if (o < -he || o > he) return -1f;
                continue;
            }

            float t1 = (-he - o) / d;
            float t2 = (he - o) / d;
            if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }

            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return -1f;
        }

        if (tMax < 0f) return -1f; // box is behind the ray entirely
        return tMin >= 0f ? tMin : tMax; // origin may be inside the box
    }

    private static float component(Vector3f v, int i) {
        return i == 0 ? v.x : (i == 1 ? v.y : v.z);
    }

    // ---------------------------------------------------------------
    // OBB vs OBB (separating axis theorem, 15-axis test)
    // ---------------------------------------------------------------

    /**
     * True if this box and {@code other} overlap. Standard SAT over the
     * 6 face normals (3 per box) plus the 9 cross-products of edge axes.
     * Cheap-ish (a handful of dot products), fine to call per-tick for a
     * few dozen vehicle parts; not meant for thousands of pairs per tick.
     */
    public boolean intersects(OBB other) {
        Vector3f[] axesA = { axis(0, new Vector3f()), axis(1, new Vector3f()), axis(2, new Vector3f()) };
        Vector3f[] axesB = { other.axis(0, new Vector3f()), other.axis(1, new Vector3f()), other.axis(2, new Vector3f()) };

        Vector3f d = new Vector3f(other.center).sub(center);

        // 6 face-normal axes.
        for (Vector3f a : axesA) if (separatedOnAxis(a, d, axesA, axesB, this.halfExtents, other.halfExtents)) return false;
        for (Vector3f b : axesB) if (separatedOnAxis(b, d, axesA, axesB, this.halfExtents, other.halfExtents)) return false;

        // 9 edge-cross axes.
        for (Vector3f a : axesA) {
            for (Vector3f b : axesB) {
                Vector3f cross = new Vector3f(a).cross(b);
                if (cross.lengthSquared() < 1e-8f) continue; // near-parallel edges, skip
                if (separatedOnAxis(cross, d, axesA, axesB, this.halfExtents, other.halfExtents)) return false;
            }
        }

        return true; // no separating axis found -> boxes overlap
    }

    private static boolean separatedOnAxis(Vector3f axis, Vector3f centerDelta, Vector3f[] axesA, Vector3f[] axesB,
                                           Vector3f halfExtentsA, Vector3f halfExtentsB) {
        float distance = Math.abs(centerDelta.dot(axis));
        float ra = projectedRadius(halfExtentsA, axesA, axis);
        float rb = projectedRadius(halfExtentsB, axesB, axis);
        return distance > ra + rb;
    }

    private static float projectedRadius(Vector3f halfExtents, Vector3f[] axes, Vector3f onAxis) {
        return Math.abs(axes[0].dot(onAxis)) * halfExtents.x
                + Math.abs(axes[1].dot(onAxis)) * halfExtents.y
                + Math.abs(axes[2].dot(onAxis)) * halfExtents.z;
    }
}