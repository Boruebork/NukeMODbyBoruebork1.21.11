package com.boruebork.nukemod.uav.collision;

import com.boruebork.nukemod.ooblib.HasHitboxParts;
import com.boruebork.nukemod.ooblib.HitboxPart;
import com.boruebork.nukemod.ooblib.OBB;
import com.boruebork.nukemod.uav.flight.FlightModel;
import com.boruebork.nukemod.uav.flight.FlightState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.List;

/**
 * Terrain collision for a rigid body made of OBB hitbox parts.
 *
 * The only thing this class can ask the world is a yes/no question:
 * "does the (slightly shrunk) hitbox overlap terrain at this exact pose?"  (collidesAt)
 * Everything else is built from that, always as a bisection between a pose known to be free
 * and a pose known to be blocked.  The pipeline per tick is:
 *
 *   1. depenetrate()  if the body overlaps terrain (block placed on it, snap from the server, ...) push it out.
 *                     Afterwards the start pose is free, which every bisection below relies on.
 *   2. rotate()       rotation is resolved first, at the start position.
 *   3. translate()    sub-stepped sweep, one axis at a time, reporting which axes were stopped.
 *
 * What to do with the velocity afterwards is NOT decided here: see FlightModel.resolveContacts.
 */
public final class TerrainCollider implements FlightModel.Mover {
    /** Gap left between the (shrunk) hitbox and a surface after a sweep, so float noise can never tip it into overlap. */
    private static final double SKIN = 1.0E-3;
    /** The collision hitbox is this much smaller than the visual one: lets the body rest on / pivot against surfaces without jitter. */
    private static final float SHRINK = 0.02f;
    private static final double LIFT_MAX = 0.35;
    private static final double UNSTICK_STEP = 0.0625, UNSTICK_MAX = 2.0;
    /**
     * Longest distance moved between two overlap checks. Must be smaller than
     * (thinnest obstacle + thinnest hitbox part) or the body can jump over it between two checks.
     */
    private static final double MAX_SUBSTEP = 0.25;
    private static final int MAX_SUBSTEPS = 32;
    /** "Resting on something" distance used by supported(). */
    private static final double GROUND_PROBE = 0.02;
    /** Directions tried, in order of preference, when pushing an overlapping body out. */
    private static final int[][] OUT = {{0, 1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, -1, 0}};

    public record Contact(HitboxPart part, BlockPos pos, BlockState state) {}
    public record RotResult(Vec3 pos, double t) {}
    /** Where a translation ended up and on which axes it was stopped by terrain. */
    public record Sweep(Vec3 pos, boolean blockedX, boolean blockedY, boolean blockedZ) {}

    private final HasHitboxParts body;
    public TerrainCollider(HasHitboxParts body) { this.body = body; }
    /** The level of the collider's entity. */
    private Level level() { return body.self().level(); }

    /** True if the body, placed at (x,y,z), overlaps terrain. Unloaded chunks count as free. */
    public boolean collidesAt(Quaternionf rot, double x, double y, double z) {
        return scan(rot, x, y, z, SHRINK, true) != null;
    }

    /** First terrain contact at the body's current pose (no shrink), or null. */
    public @Nullable Contact findContact(Quaternionf rot, Vec3 at) {
        return scan(rot, at.x, at.y, at.z, 0f, false);
    }

    // ------------------------------------------------------------------ 1. depenetration

    /** If the body overlaps terrain at this pose, returns the nearest free position (up first, then sideways, then down). */
    @Override
    public Vec3 depenetrate(Quaternionf rot, Vec3 at) {
        if (!collidesAt(rot, at.x, at.y, at.z)) return at;
        for (double d = UNSTICK_STEP; d <= UNSTICK_MAX; d += UNSTICK_STEP)
            for (int[] o : OUT) {
                double x = at.x + o[0] * d, y = at.y + o[1] * d, z = at.z + o[2] * d;
                if (!collidesAt(rot, x, y, z)) return new Vec3(x, y, z);
            }
        return at;   // buried deeper than UNSTICK_MAX: stay put; rotate()/translate() refuse to move from an overlapping pose
    }

    // ------------------------------------------------------------------ 2. rotation

    /** Largest fraction of a0->a1 (yaw, pitch, roll) that fits. allowLift: free the pose by raising the body instead. */
    @Override
    public RotResult rotate(Vec3 from, float[] a0, float[] a1, boolean allowLift) {
        if (a0[0] == a1[0] && a0[1] == a1[1] && a0[2] == a1[2]) return new RotResult(from, 1.0);   // nothing to resolve
        Quaternionf r1 = FlightState.rotation(a1[0], a1[1], a1[2]);
        if (!collidesAt(r1, from.x, from.y, from.z)) return new RotResult(from, 1.0);
        if (allowLift) {
            double lift = minLift(r1, from.x, from.y, from.z);      // -1 if more than LIFT_MAX is needed
            if (lift >= 0) return new RotResult(from.add(0, lift, 0), 1.0);
        }
        double lo = 0, hi = 1;
        for (int i = 0; i < 8; i++) {
            double mid = (lo + hi) * 0.5;
            Quaternionf q = FlightState.rotation(Mth.lerp((float) mid, a0[0], a1[0]),
                    Mth.lerp((float) mid, a0[1], a1[1]), Mth.lerp((float) mid, a0[2], a1[2]));
            if (collidesAt(q, from.x, from.y, from.z)) hi = mid; else lo = mid;
        }
        return new RotResult(from, lo);
    }

    private double minLift(Quaternionf rot, double x, double y, double z) {
        if (!collidesAt(rot, x, y, z)) return 0;
        if (collidesAt(rot, x, y + LIFT_MAX, z)) return -1;
        double lo = 0, hi = LIFT_MAX;
        for (int i = 0; i < 9; i++) {
            double m = (lo + hi) * 0.5;
            if (collidesAt(rot, x, y + m, z)) lo = m; else hi = m;
        }
        return hi;
    }

    // ------------------------------------------------------------------ 3. translation

    /**
     * Sub-stepped sweep. Each sub-step moves Y, then X, then Z, so a diagonal move follows the real diagonal
     * path instead of "all of X, then all of Z" (which cuts corners). Once an axis is stopped it stays stopped
     * for the rest of the tick, because its velocity is going to be removed anyway.
     * Rotation is fixed here; the start pose must be free.
     */
    @Override
    public Sweep translate(Vec3 from, Vec3 desired, Quaternionf rot) {
        int n = Mth.clamp((int) Math.ceil(desired.length() / MAX_SUBSTEP), 1, MAX_SUBSTEPS);
        if (n > 1 && sweepIsClear(rot, from, desired)) return new Sweep(from.add(desired), false, false, false);
        double sx = desired.x / n, sy = desired.y / n, sz = desired.z / n;
        double x = from.x, y = from.y, z = from.z;
        boolean bx = false, by = false, bz = false;
        for (int i = 0; i < n; i++) {
            if (sy != 0) {
                double d = clip(rot, x, y, z, 0, sy, 0); y += d;
                if (Math.abs(d - sy) > 1e-9) { by = true; sy = 0; }
            }
            if (sx != 0) {
                double d = clip(rot, x, y, z, sx, 0, 0); x += d;
                if (Math.abs(d - sx) > 1e-9) { bx = true; sx = 0; }
            }
            if (sz != 0) {
                double d = clip(rot, x, y, z, 0, 0, sz); z += d;
                if (Math.abs(d - sz) > 1e-9) { bz = true; sz = 0; }
            }
        }
        return new Sweep(new Vec3(x, y, z), bx, by, bz);
    }

    /**
     * Cheap early-out for fast free flight: the box that bounds the whole move (start pose + end pose, every part)
     * contains no block that has a collision shape, so nothing can be hit and sub-stepping is pointless.
     * Conservative: anything unclear (a solid block anywhere in the box, an unloaded column) returns false
     * and the careful path decides.
     */
    private boolean sweepIsClear(Quaternionf rot, Vec3 from, Vec3 desired) {
        List<HitboxPart> parts = body.getHitboxParts();
        if (parts.isEmpty()) return true;
        AABB box = null;
        for (HitboxPart part : parts) {
            AABB a = body.getWorldOBBAt(part, rot, from.x, from.y, from.z, SHRINK).toAABB();
            AABB b = body.getWorldOBBAt(part, rot, from.x + desired.x, from.y + desired.y, from.z + desired.z, SHRINK).toAABB();
            AABB both = a.minmax(b);
            box = box == null ? both : box.minmax(both);
        }
        Level level = level();
        var pos = new BlockPos.MutableBlockPos();
        for (int x = Mth.floor(box.minX); x <= Mth.floor(box.maxX); x++) {
            for (int z = Mth.floor(box.minZ); z <= Mth.floor(box.maxZ); z++) {
                pos.set(x, Mth.floor(box.minY), z);
                if (!level.hasChunkAt(pos)) return false;
                for (int y = Mth.floor(box.minY); y <= Mth.floor(box.maxY); y++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && !state.getCollisionShape(level, pos).isEmpty()) return false;
                }
            }
        }
        return true;
    }

    /**
     * How far (signed) can the body move along ONE axis? Exactly one of dx/dy/dz is non-zero.
     *
     * collidesAt only says yes/no, it cannot say "how far". So when the full move is blocked we bisect the
     * fraction of the move between a pose known to be free (lo) and one known to be blocked (hi).
     * lo * full is the signed distance (full carries the direction), and we back off by SKIN along that
     * direction so the body stops a hair short of the surface instead of exactly on it.
     */
    private double clip(Quaternionf rot, double x, double y, double z, double dx, double dy, double dz) {
        double full = dx + dy + dz;
        if (Math.abs(full) < 1.0E-9) return 0;
        if (!collidesAt(rot, x + dx, y + dy, z + dz)) return full;            // whole move is free

        // Already touching? A nudge of 2*SKIN collides => there is no room, skip the search.
        double nudge = Math.min(1.0, 2 * SKIN / Math.abs(full));
        if (nudge >= 1.0 || collidesAt(rot, x + dx * nudge, y + dy * nudge, z + dz * nudge)) return 0;

        double lo = nudge, hi = 1;                                             // lo is free, hi is blocked
        for (int i = 0; i < 10; i++) {
            double mid = (lo + hi) * 0.5;
            if (collidesAt(rot, x + dx * mid, y + dy * mid, z + dz * mid)) hi = mid; else lo = mid;
        }
        double safe = full * lo;
        return Math.abs(safe) <= SKIN ? 0 : safe - Math.signum(safe) * SKIN;
    }

    /** True if terrain is within GROUND_PROBE below the body. */
    @Override
    public boolean supported(Quaternionf rot, Vec3 at) {
        return collidesAt(rot, at.x, at.y - GROUND_PROBE, at.z);
    }

    // ------------------------------------------------------------------ the world query

    /** One scan serves both queries; they differ only in shrink and in how unloaded chunks are treated. */
    private @Nullable Contact scan(Quaternionf rot, double ex, double ey, double ez,
                                   float shrink, boolean abortOnUnloaded) {
        List<HitboxPart> parts = body.getHitboxParts();
        if (parts.isEmpty()) return null;

        // world-space OBB per part, the AABB around each, and one broad-phase AABB around all of them
        OBB[] obbs = new OBB[parts.size()];
        AABB[] boxes = new AABB[parts.size()];
        AABB broad = null;
        for (int i = 0; i < obbs.length; i++) {
            obbs[i] = shrink > 0 ? body.getWorldOBBAt(parts.get(i), rot, ex, ey, ez, shrink)
                    : body.getWorldOBBAt(parts.get(i), rot, ex, ey, ez);
            boxes[i] = obbs[i].toAABB();
            broad = broad == null ? boxes[i] : broad.minmax(boxes[i]);
        }

        Level level = level();
        var pos = new BlockPos.MutableBlockPos();
        for (int x = Mth.floor(broad.minX); x <= Mth.floor(broad.maxX); x++) {
            for (int z = Mth.floor(broad.minZ); z <= Mth.floor(broad.maxZ); z++) {
                pos.set(x, Mth.floor(broad.minY), z);
                if (!level.hasChunkAt(pos)) {
                    if (abortOnUnloaded) return null;
                    continue;
                }
                for (int y = Mth.floor(broad.minY); y <= Mth.floor(broad.maxY); y++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    VoxelShape shape = state.getCollisionShape(level, pos);
                    if (shape.isEmpty()) continue;
                    for (AABB local : shape.toAabbs()) {
                        AABB world = local.move(pos);
                        OBB block = null;
                        for (int i = 0; i < obbs.length; i++) {
                            if (!boxes[i].intersects(world)) continue;          // cheap AABB reject first
                            if (block == null) block = OBB.fromAABB(world);
                            if (obbs[i].intersects(block)) return new Contact(parts.get(i), pos.immutable(), state);
                        }
                    }
                }
            }
        }
        return null;
    }
}
