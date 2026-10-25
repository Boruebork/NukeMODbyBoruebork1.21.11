package com.boruebork.nukemod.ooblib;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class OBBUtil {
    public static double rayVsOBB(Vec3 origin, Vec3 dir, OBB obb, double maxT) {
        Quaternionf inv = new Quaternionf(obb.rotation).conjugate();
        Vector3f o = new Vector3f(
                (float)(origin.x - obb.center.x),
                (float)(origin.y - obb.center.y),
                (float)(origin.z - obb.center.z));
        inv.transform(o);
        Vector3f d = new Vector3f((float)dir.x, (float)dir.y, (float)dir.z);
        inv.transform(d);
        Vector3f h = obb.halfExtents;
        double tmin = 0.0, tmax = maxT;

        for (int i = 0; i < 3; i++) {
            float oi = i == 0 ? o.x : i == 1 ? o.y : o.z;
            float di = i == 0 ? d.x : i == 1 ? d.y : d.z;
            float hi = i == 0 ? h.x : i == 1 ? h.y : h.z;
            if (Math.abs(di) < 1e-8f) {
                if (Math.abs(oi) > hi) return -1;
                continue;
            }
            double t1 = (-hi - oi) / di;
            double t2 = ( hi - oi) / di;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return -1;
        }
        return tmin;
    }

}
