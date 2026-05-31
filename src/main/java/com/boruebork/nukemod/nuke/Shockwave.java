package com.boruebork.nukemod.nuke;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class Shockwave {
    public final Vec3 center;
    public float radius;
    public final float maxRadius;
    public final float speed;
    private final Level level;
    private List<Vec3> rays;

    public Shockwave(Vec3 center, float speed, float maxRadius, Level level) {
        this.center = center;
        this.speed = speed;
        this.maxRadius = maxRadius;
        this.radius = 0;
        this.level = level;
        this.rays = this.generateDirections(2500);
    }

    public boolean tick() {
        radius += speed;
        applyShockwave();
        double newRadius = radius + NukeConfig.SHOCKWAVE_SPEED;
        for (Vec3 dir : rays) {
            Vec3 pos = center.add(dir.scale(radius));
            BlockPos bp = BlockPos.containing(pos);

            BlockState state = level.getBlockState(bp);
            if (state.isAir()) continue;

            float resistance = state.getExplosionResistance(level, bp, null);

            level.destroyBlock(bp, false);

        }
        radius = (float) newRadius;
        return radius >= maxRadius;
    }
    private void applyShockwave() {
        Shockwave wave = this;
        AABB box = new AABB(
                wave.center.x - wave.radius,
                wave.center.y - wave.radius,
                wave.center.z - wave.radius,
                wave.center.x + wave.radius,
                wave.center.y + wave.radius,
                wave.center.z + wave.radius
        );

        List<Entity> entities = level.getEntities(null, box);

        for (Entity e : entities) {
            Vec3 delta = e.position().subtract(wave.center);
            double dist = delta.length();

            // Only affect the ring, not everything inside
            if (dist < wave.radius && dist > wave.radius - wave.speed) {
                Vec3 push = delta.normalize()
                        .scale(1.5 + (wave.maxRadius - dist) * 0.01);
                e.hurt(e.damageSources().generic(), 5.0f);
                e.push(push.x, push.y * 0.6, push.z);
                e.hurtMarked = true;
            }
        }

    }
    public List<Vec3> generateDirections(int count) {
        List<Vec3> dirs = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            double theta = Math.acos(1 - 2 * Math.random());
            double phi = 2 * Math.PI * Math.random();

            double x = Math.sin(theta) * Math.cos(phi);
            double y = Math.cos(theta);
            double z = Math.sin(theta) * Math.sin(phi);

            dirs.add(new Vec3(x, y, z));
        }
        return dirs;
    }


}
