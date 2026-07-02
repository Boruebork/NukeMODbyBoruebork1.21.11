package com.boruebork.nukemod.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class ExpandingExplosion {
    private final ServerLevel level;
    private final BlockPos center;
    public static final int MAX_RADIUS = 120;
    public static int currentShellGened = 1;
    public Set<Integer> shockedEntities = new HashSet<>();
    public static Map<Integer, List<Vec3i>> shells = new HashMap<>();
    private final int maxRadius;
    private int currentRadius = 1;
    private int ageInTicks = 0;
    private boolean finished = false;

    public ExpandingExplosion(ServerLevel level, BlockPos center, int maxRadius) {
        this.level = level;
        this.center = center;
        this.maxRadius = maxRadius;
    }
    public static void generateShells() {
        int r = currentShellGened;
        List<Vec3i> shell = new ArrayList<>();
        int outerSq = r * r;
        int innerSq = (r - 1) * (r - 1);
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    int distSq = x*x + y*y + z*z;
                    if (distSq <= outerSq && distSq > innerSq) {
                        shell.add(new Vec3i(x, y, z));
                    }
                }
            }
        }

        shells.put(r, shell);
        currentShellGened++;
        System.err.println("generated shell for radius: " + r);
    }

    /*public static ExpandingExplosion createExplosion(ServerLevel level, BlockPos pos, int radius){
        ExplosionManager.getInstance().add(new ExpandingExplosion(level, pos, radius));
        return NukeModbyBoruebork.explosions.getLast();
    }*/

    public boolean isFinished() {
        return finished;
    }
    public void tick(){
        System.err.println("Explosion tick");
        if (ageInTicks % 2 == 0){
            ageInTicks = 1;
            return;
        }
        ageInTicks++;
        if (finished){
            return;
        }
        if (currentRadius > this.maxRadius){
            finished = true;
            return;
        }
        applyShockwave(currentRadius);
        int r = currentRadius;
        int fillRadius = Math.max(1, r / 20);
        for (Vec3i pos : shells.get(r)){
            BlockPos shellPos = center.offset(pos);
            destroyMiniSphere(shellPos, fillRadius);
            if (Math.abs(pos.getY()) > 2 || pos.getZ()*pos.getZ() + pos.getX()* pos.getX() > (r  - 1)*(r- 1))
                continue;

            BlockPos pos1 = center.offset(pos);
            level.sendParticles(
                    ParticleTypes.CLOUD,
                    pos1.getX(),
                    pos1.getY() + 1,
                    pos1.getZ(),
                    5,
                    0.3,
                    0.3,
                    0.3,
                    0.02
            );
        }
        currentRadius++;
    }
    /*public void tick() {
        if (finished) {
            return;
        }
        int r = currentRadius;
        // Dynamically scale shell thickness
        int shellThickness = Math.max(1, r / 24);
        // Dynamically scale local destruction radius
        int fillRadius = Math.max(1, r / 20);
        int outerSq = r * r;
        int innerSq = (r - shellThickness) * (r - shellThickness);
        // Iterate ONLY current shell cube
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    int distSq = x * x + y * y + z * z;
                    // Is voxel inside shell?
                    if (distSq <= outerSq && distSq >= innerSq) {
                        BlockPos shellPos = center.offset(x, y, z);
                        // Destroy local mini sphere
                        destroyMiniSphere(shellPos, fillRadius);
                    }
                }
            }
        }
        currentRadius++;
        if (currentRadius > maxRadius) {
            finished = true;
        }
    }*/

    private void destroyMiniSphere(BlockPos centerPos, int radius) {

        int radiusSq = radius * radius;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    int distSq = x * x + y * y + z * z;
                    if (distSq <= radiusSq) {
                        BlockPos pos = centerPos.offset(x, y, z);
                        if (!level.hasChunkAt(pos)) {
                            continue;
                        }
                        BlockState state = level.getBlockState(pos);
                        // Skip air
                        if (state.isAir()) {
                            continue;
                        }
                        // Skip bedrock
                        if (state.is(Blocks.BEDROCK)) {
                            continue;
                        }
                        // Remove block
                        level.setBlock(
                                pos,
                                Blocks.AIR.defaultBlockState(),
                                Block.UPDATE_CLIENTS
                        );
                    }
                }
            }
        }
    }
    private void applyShockwave(int radius) {

        AABB box = new AABB(
                this.center.getX() - radius,
                this.center.getY() - radius,
                this.center.getZ() - radius,
                this.center.getX() + radius,
                this.center.getY() + radius,
                this.center.getZ() + radius
        );

        List<Entity> entities = level.getEntitiesOfClass(Entity.class, box);

        for (Entity e : entities) {

            if (shockedEntities.contains(e.getId())) continue;

            Vec3 delta = e.position().subtract(new Vec3(this.center.getX(), this.center.getY(), this.center.getZ()));
            double dist = delta.length();

            // Only affect the ring, not everything inside
            if (dist < radius) {
                Vec3 push = delta.normalize()
                        .scale(1.5 + (maxRadius - dist) * 0.01);
                e.hurt(e.damageSources().generic(), (1 - (float) radius /maxRadius) * 30);
                System.err.println(push.x + " " + push.z);
                e.addDeltaMovement(new Vec3(push.x*100, 0, push.z*100));
                shockedEntities.add(e.getId());
            }
        }

    }
}
