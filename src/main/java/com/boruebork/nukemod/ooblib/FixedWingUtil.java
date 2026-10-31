package com.boruebork.nukemod.ooblib;

import net.minecraft.world.phys.Vec3;

public class FixedWingUtil {
    public static final float GRAVITY        = 9.8f / 400.0f;  // 0.02450 b/t²
    public static final float AIR_DENSITY    = 1.225f;         // kg/m³
    public static final float LIFT_SLOPE     = 4.7f;           // dCL/dα, ~AR 6
    public static final float STALL_ANGLE    = 0.26f;          // rad (~15°)
    public static final float DRAG_BASE      = 0.020f;
    public static final float DRAG_INDUCED   = 0.050f;

    public static boolean isFinite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }
}
