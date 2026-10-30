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

    private Level level() { return body.self().level(); }

    /** True if the body, placed at (x,y,z), overlaps terrain. Unloaded chunks count as free. */
    public boolean collidesAt(Quaternionf rot, double x, double y, double z) {
        return scan(rot, x, y, z, SHRINK, true) != null;
    }

    /** First terrain contact at the body's current pose (no shrink), or null. */
    public @Nullable Contact findContact(Quaternionf rot, Vec3 at) {
        return scan(rot, at.x, at.y, at.z, 0f, false);
    }

    public Result move(Vec3 from, Vec3 desired, Quaternionf rot) {
        double x = from.x, y = from.y, z = from.z;

        if (collidesAt(rot, x, y, z)) {
            double nudge = 0;
            for (double d = UNSTICK_STEP; d <= UNSTICK_MAX; d += UNSTICK_STEP) {
                if (!collidesAt(rot, x, y + d, z)) { nudge = d; break; }
            }
            if (nudge == 0) return new Result(from, true, true, true, true);   // truly stuck
            y += nudge;
            x += clip(rot, x, y, z, desired.x, 0, 0);
            z += clip(rot, x, y, z, 0, 0, desired.z);
            return new Result(new Vec3(x, y, z), false, false, false, false);   // leave flags as they were
        }

        double dy = clip(rot, x, y, z, 0, desired.y, 0);  y += dy;
        double dx = clip(rot, x, y, z, desired.x, 0, 0);  x += dx;
        double dz = clip(rot, x, y, z, 0, 0, desired.z);  z += dz;

        boolean vert = Math.abs(dy - desired.y) > 1e-7;
        boolean horiz = Math.abs(dx - desired.x) > 1e-7 || Math.abs(dz - desired.z) > 1e-7;
        return new Result(new Vec3(x, y, z), vert, horiz, vert && desired.y < 0, true);
    }

    private double clip(Quaternionf rot, double x, double y, double z, double dx, double dy, double dz) {
        double full = dx + dy + dz;
        if (Math.abs(full) < 1.0E-9) return 0;
        if (!collidesAt(rot, x + dx, y + dy, z + dz)) return full;

        double low = 0, high = 1;
        for (int i = 0; i < 10; i++) {
            double mid = (low + high) * 0.5;
            if (collidesAt(rot, x + dx * mid, y + dy * mid, z + dz * mid)) high = mid; else low = mid;
        }
        double safe = full * low;
        return Math.abs(safe) <= SKIN ? 0 : safe - Math.signum(safe) * SKIN;
    }

    /** One scan serves both queries; they differ only in shrink and in how unloaded chunks are treated. */
    private @Nullable Contact scan(Quaternionf rot, double ex, double ey, double ez,
                                   float shrink, boolean abortOnUnloaded) {
        List<HitboxPart> parts = body.getHitboxParts();
        if (parts.isEmpty()) return null;

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
                            if (!boxes[i].intersects(world)) continue;
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