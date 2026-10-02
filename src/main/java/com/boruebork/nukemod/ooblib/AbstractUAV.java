package com.boruebork.nukemod.ooblib;

import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVDrone;
import com.boruebork.nukemod.entity.custom.uav.RQ4;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public abstract class AbstractUAV extends AbstractFPVDrone implements HasHitboxParts {

    public AbstractUAV(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public Entity self() {
        return this;
    }

    @Override
    public Quaternionf getEntityRotation() {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-this.getYRot()))
                .rotateX((float) Math.toRadians(this.getXRot()));
    }

    @Override
    public void tick() {
        super.tick();

        checkOBBCollisions();
    }

    /**
     * Broad-phase collision check.
     *
     * Finds nearby entities using one large AABB and then
     * performs the expensive OBB checks only on those entities.
     */
    protected void checkOBBCollisions() {
        AABB broadphase = getBroadphaseAABB();
        if (!level().isClientSide())
            System.err.println(1);
        List<Entity> entities = level().getEntitiesOfClass(
                Entity.class,
                broadphase,
                entity -> entity != this && (entity.isPickable() || entity instanceof AbstractUAV)
        );
        if (!level().isClientSide())
            System.err.println("Enttiies " + entities.size());

        for (Entity entity : entities) {
            checkOBBCollision(entity);
        }
    }

    /**
     * Performs the narrow-phase OBB collision check against one entity.
     */
    protected void checkOBBCollision(Entity other) {
       if (!level().isClientSide())
           System.err.println(2);
        // If the other entity isn't an OBB entity,
        // there is nothing for our OBB system to check yet.
        if (!(other instanceof AbstractUAV otherOBBEntity)) {
            return;
        }
        if (!level().isClientSide())
           System.err.println(3);

        for (HitboxPart thisPart : getHitboxParts()) {
            if (!level().isClientSide())
                System.err.println(4);
            for (HitboxPart otherPart : otherOBBEntity.getHitboxParts()) {
               if (!level().isClientSide())
                    System.err.println(5);
                OBB thisOBB = getWorldOBB(thisPart);
                OBB otherOBB = otherOBBEntity.getWorldOBB(otherPart);

                if (thisOBBsIntersect(thisOBB, otherOBB)) {
                    onOBBCollision(
                            thisPart,
                            otherOBBEntity,
                            otherPart
                    );
                }
            }
        }
    }

    /**
     * Returns the AABB enclosing the entire OBB entity.
     *
     * This is the cheap broad-phase collision volume.
     */
    protected AABB getBroadphaseAABB() {
        AABB result = null;

        for (HitboxPart part : getHitboxParts()) {
            OBB obb = getWorldOBB(part);
            AABB aabb = getOBBAABB(obb);

            if (result == null) {
                result = aabb;
            } else {
                result = result.minmax(aabb);
            }
        }

        // Fallback if the entity has no hitboxes.
        if (result == null) {
            return getBoundingBox();
        }

        return result;
    }

    private List<HitboxPart> getHitboxParts() {
        if (this instanceof RQ4){
            return List.of(
                    HitboxPart.of("main", 0, -1, 0, 1, 1, 3),
                    HitboxPart.of("wing_left", 1, 0, 0, 2 , 1, 2)
            );
        }
        return List.of();

    }

    /**
     * Converts a local hitbox definition into a world-space OBB.
     */
    protected OBB getWorldOBB(HitboxPart part) {
        Vector3f localCenter = part.localOffset();
        Vector3f worldCenter = new Vector3f(localCenter);

        // Rotate the local hitbox position around the entity.
        getEntityRotation().transform(worldCenter);

        // Move it into world space.
        worldCenter.add(
                (float) getX(),
                (float) getY(),
                (float) getZ()
        );

        return new OBB(
                worldCenter,
                new Vector3f(part.localHalfExtents()),
                getEntityRotation()
        );
    }
    /**
     * Returns an AABB enclosing an OBB.
     * This is used only for broad-phase collision detection.
     */
    protected AABB getOBBAABB(OBB obb) {
        // Implement according to your OBB API.
        return obb.toAABB();
    }

    /**
     * Actual OBB-vs-OBB collision test.
     */
    protected boolean thisOBBsIntersect(OBB first, OBB second) {
        return first.intersects(second);
    }

    /**
     * Called when two hitbox parts actually collide.
     */
    protected void onOBBCollision(
            HitboxPart thisPart,
            AbstractUAV other,
            HitboxPart otherPart
    ) {
        // Override this in UAVEntity / missile / etc.
    }
    @Override
    public boolean hurtServer(
            ServerLevel serverLevel,
            DamageSource damageSource,
            float damage
    ) {
        return false;
    }
    @Override
    public boolean isPickable() {
        return true;
    }
}