package com.boruebork.nukemod.mixin;

import com.boruebork.nukemod.ooblib.AbstractUAV;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilMixin {

    @Inject(
            method = "getEntityHitResult("
                    + "Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/world/entity/Entity;"
                    + "Lnet/minecraft/world/phys/Vec3;"
                    + "Lnet/minecraft/world/phys/Vec3;"
                    + "Lnet/minecraft/world/phys/AABB;"
                    + "Ljava/util/function/Predicate;"
                    + "F"
                    + ")Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void nukemod$obbAwareEntityHit(
            Level level, Entity projectile,
            Vec3 startVec, Vec3 endVec,
            AABB boundingBox, Predicate<Entity> filter,
            float inflationAmount,
            CallbackInfoReturnable<EntityHitResult> cir
    ) {
        EntityHitResult vanilla = cir.getReturnValue();

        // An AABB-based hit on an OBB entity is wrong by construction.
        // Drop it and let the OBB pass recompute it.
        if (vanilla != null && vanilla.getEntity() instanceof AbstractUAV) {
            vanilla = null;
        }

        double bestDistSqr = vanilla == null
                ? Double.MAX_VALUE
                : startVec.distanceToSqr(vanilla.getLocation());

        Vec3 ray = endVec.subtract(startVec);
        double rayLen = ray.length();
        if (rayLen < 1e-6) {
            if (cir.getReturnValue() != vanilla) cir.setReturnValue(vanilla);
            return;
        }

        Vec3 rayDir = ray.scale(1.0 / rayLen);
        EntityHitResult best = vanilla;

        for (Entity e : level.getEntities(projectile, boundingBox, filter)) {
            if (!(e instanceof AbstractUAV uav)) continue;

            Vec3 hit = uav.raycastOBBs(startVec, rayDir, rayLen);
            if (hit == null) continue;

            double d = startVec.distanceToSqr(hit);
            if (d < bestDistSqr) {
                bestDistSqr = d;
                best = new EntityHitResult(e, hit);
            }
        }

        if (best != cir.getReturnValue()) {
            cir.setReturnValue(best);
        }
    }
}