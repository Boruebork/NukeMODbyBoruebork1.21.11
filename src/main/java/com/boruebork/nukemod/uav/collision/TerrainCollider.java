package com.boruebork.nukemod.uav.collision;

import com.boruebork.nukemod.ooblib.HasHitboxParts;
import com.boruebork.nukemod.ooblib.HitboxPart;
import com.boruebork.nukemod.ooblib.OBB;
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

public final class TerrainCollider {
    private static final double SKIN = 1.0E-3;
    private static final float SHRINK = 0.02f;
    private static final double UNSTICK_STEP = 0.0625, UNSTICK_MAX = 2.0;

    public record Contact(HitboxPart part, BlockPos pos, BlockState state) {}
    public record Result(Vec3 newPos, boolean vertical, boolean horizontal,
                         boolean onGround, boolean flagsValid) {}

    private final HasHitboxParts body;
    public TerrainCollider(HasHitboxParts body) { this.body = body; }
    /// retruns the `Level`of the collider
    private Level level() { return body.self().level(); }

    /** True if the body, placed at (x,y,z), overlaps terrain. Unloaded chunks count as free. */
    public boolean collidesAt(Quaternionf rot, double x, double y, double z) {
        return scan(rot, x, y, z, SHRINK, true) != null;
    }

    /** First terrain contact at the body's current pose (no shrink), or null. */
    public @Nullable Contact findContact(Quaternionf rot, Vec3 at) {
        return scan(rot, at.x, at.y, at.z, 0f, false);
    }

    public Result move(Vec3 from, Vec3 desired, Quaternionf rot, Quaternionf desRot) {
        double x = from.x, y = from.y, z = from.z;
        /// if collides in starting pos
        if (collidesAt(rot, x, y, z)) {
            double nudge = 0;
            /// try unsticking by pushing the plane up
            for (double d = UNSTICK_STEP; d <= UNSTICK_MAX; d += UNSTICK_STEP) {
                if (!collidesAt(rot, x, y + d, z)) { nudge = d; break; }
            }
            /// if we weren't able to find a good unsticking pos, we can't move ups so return original state
            if (nudge == 0) return new Result(from, true, true, true, true);   // truly stuck
            /// otherwise increment y pos by nudge
            y += nudge;
            //x += clip(rot, x, y, z, desired.x, 0, 0);
            x += clipTransform(new Vec3(x, y, z), new Vec3(desired.x, 0, 0), rot, desRot);
           // z += clip(rot, x, y, z, 0, 0, desired.z);
            z += clipTransform(new Vec3(x, y, z), new Vec3(0, 0, desired.z), rot, desRot);
            /// return enw result
            return new Result(new Vec3(x, y, z), false, false, false, false);   // leave flags as they were
        }

        //double dy = clip(rot, x, y, z, 0, desired.y, 0);  y += dy;
        double dy = clipTransform(new Vec3(x, y, z), new Vec3(0, desired.y, 0), rot, desRot);
        //double dx = clip(rot, x, y, z, desired.x, 0, 0);  x += dx;
        double dx = clipTransform(new Vec3(x, y, z), new Vec3(desired.x, 0, 0), rot, desRot);
        //double dz = clip(rot, x, y, z, 0, 0, desired.z);  z += dz;
        double dz = clipTransform(new Vec3(x, y, z), new Vec3(0, 0, desired.z), rot, desRot);
        boolean vert = Math.abs(dy - desired.y) > 1e-7;
        boolean horiz = Math.abs(dx - desired.x) > 1e-7 || Math.abs(dz - desired.z) > 1e-7;
        return new Result(new Vec3(x, y, z), vert, horiz, vert && desired.y < 0, true);
    }

    private double clip(Quaternionf rot, double x, double y, double z, double dx, double dy, double dz) {
        /// full change in pos
        double full = dx + dy + dz;
        /// if the movement is very small we return 0
        if (Math.abs(full) < 1.0E-9) return 0;
        /// if not colliding return full change in pos
        if (!collidesAt(rot, x + dx, y + dy, z + dz)) return full;
        /// binary search on real numbers for the safe pos
        double low = 0, high = 1;
        for (int i = 0; i < 10; i++) {
            /// calc mid
            double mid = (low + high) * 0.5;
            ///  using collideAt as a binSearch checker
            ///  if we are colliding then high = mid else low = mid
            if (collidesAt(rot, x + dx * mid, y + dy * mid, z + dz * mid)) high = mid; else low = mid;
        }
        /// safe is full * bin search result, why though? what is even the point of this binary search?
        double safe = full * low;
        /// if safe is too small return 0, else wtf is signum?
        return Math.abs(safe) <= SKIN ? 0 : safe - Math.signum(safe) * SKIN;
        /// what this even returns??? what is the point of it?????
    }
    private double clipTransform(
            Vec3 from,
            Vec3 desired,
            Quaternionf oldRot,
            Quaternionf desiredRot
    ) {
        Vec3 delta = desired.subtract(from);

        double low = 0.0;
        double high = 1.0;

        Vec3 testPos;
        Quaternionf testRot = new Quaternionf();

        for (int i = 0; i < 10; i++) {
            double mid = (low + high) * 0.5;

            testPos = from.add(delta.scale(mid));
            oldRot.slerp(desiredRot, (float) mid, testRot);

            if (collidesAt(testRot,
                    testPos.x, testPos.y, testPos.z)) {
                high = mid;
            } else {
                low = mid;
            }
        }

        return low;
    }

    /** One scan serves both queries; they differ only in shrink and in how unloaded chunks are treated. */
    private @Nullable Contact scan(Quaternionf rot, double ex, double ey, double ez,
                                   float shrink, boolean abortOnUnloaded) {
        /// get hitbox parts
        List<HitboxPart> parts = body.getHitboxParts();
        /// if there are no parts they could be no contact
        if (parts.isEmpty()) return null;
        /// convert parts to OBBs and AABBs
        OBB[] obbs = new OBB[parts.size()];
        AABB[] boxes = new AABB[parts.size()];
        /// broad phase AABBB
        AABB broad = null;
        /// for loop
        for (int i = 0; i < obbs.length; i++) {
            /// convert part to obb
            obbs[i] = shrink > 0 ? body.getWorldOBBAt(parts.get(i), rot, ex, ey, ez, shrink)
                    : body.getWorldOBBAt(parts.get(i), rot, ex, ey, ez);
            /// convert part -> obb ->aabb
            boxes[i] = obbs[i].toAABB();
            /// update broad phase AABB
            broad = broad == null ? boxes[i] : broad.minmax(boxes[i]);
        }
        /// get level
        Level level = level();
        /// new blockpos
        var pos = new BlockPos.MutableBlockPos();
        /// for each x integer coord in broad phase
        for (int x = Mth.floor(broad.minX); x <= Mth.floor(broad.maxX); x++) {
            /// for each z itneger coord in broad phase
            for (int z = Mth.floor(broad.minZ); z <= Mth.floor(broad.maxZ); z++) {
                /// in result we loop over every single block in broad phase
                /// update the pos to the pos of the current block
                pos.set(x, Mth.floor(broad.minY), z);
                /// if the chunks is not loaded or doesn't exist ig, we skip the block or abort if `abortUnloaded` is true
                if (!level.hasChunkAt(pos)) {
                    if (abortOnUnloaded) return null;
                    continue;
                }
                /// loop through each y coord in the colomn
                for (int y = Mth.floor(broad.minY); y <= Mth.floor(broad.maxY); y++) {
                    /// upd pos
                    pos.set(x, y, z);
                    /// get blockstate at pos
                    BlockState state = level.getBlockState(pos);
                    /// if air skip
                    if (state.isAir()) continue;
                    /// get collsion shape of the block
                    VoxelShape shape = state.getCollisionShape(level, pos);
                    /// if has no collision shape then skip
                    if (shape.isEmpty()) continue;
                    /// for each AABB in blocks collsion shape
                    for (AABB local : shape.toAabbs()) {
                        /// get the absolute world AABB of the current part of the blocks collsion shape
                        AABB world = local.move(pos);
                        OBB block = null;
                        /// for loop
                        for (int i = 0; i < obbs.length; i++) {
                            /// if the broad phase of a OBB part doesn't intersects the block we just skip it for optimization
                            if (!boxes[i].intersects(world)) continue;
                            /// assigne the block OBB to the OBB of the blocks collision shape aprt
                            if (block == null) block = OBB.fromAABB(world);
                            /// if the hitboxpart of HasHitboxPArts intersects with the collison part of the block return the contact
                            if (obbs[i].intersects(block)) return new Contact(parts.get(i), pos.immutable(), state);
                        }
                    }
                }
            }
        }
        return null;
        /// what this function even is useful for....
    }
}