package com.boruebork.nukemod.entity.custom;

import com.boruebork.nukemod.Config;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class NukeEntity extends Entity {
    private Level level;
    private int targetX;
    private boolean goodToGo = false;
    private int targetY;
    private int targetZ;
    private float yRot;
    public float speed = 50;
    private float xRot;
    private float ySpeed = 0;
    private float xSpeed = 0;
    private float zSpeed = 0;
    public String senderName = "Null💀";
    public NukeEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.level = level;
    }
    /*public NukeEntity(EntityType<?> entityType, Level level, int x, int y, int z){
        this(entityType, level);
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }*/

    public void setTargetX(int targetX) {
        this.targetX = targetX;
    }

    public void setTargetY(int targetY) {
        this.targetY = targetY;
    }

    public int getTargetZ() {
        return targetZ;
    }

    public int getTargetY() {
        return targetY;
    }

    public int getTargetX() {
        return targetX;
    }

    public float getxRot() {
        return xRot;
    }

    public float getyRot() {
        return yRot;
    }

    public void setTargetZ(int targetZ) {
        this.targetZ = targetZ;
    }
    public static Vec2 calculateBallisticRotation(
            Vec3 start,
            Vec3 target,
            double speed,
            double gravity
    ) {
        double dx = target.x - start.x;
        double dy = target.y - start.y;
        double dz = target.z - start.z;

        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // Yaw (horizontal)
        float yRot = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;

        // Ballistic pitch
        double speedSq = speed * speed;
        double underRoot = speedSq * speedSq
                - gravity * (gravity * horizontalDist * horizontalDist + 2 * dy * speedSq);

        // Target unreachable → fallback to direct aim
        if (underRoot < 0) {
            float xRot = (float) -Math.toDegrees(Math.atan2(dy, horizontalDist));
            return new Vec2(xRot, yRot);
        }

        double root = Math.sqrt(underRoot);

        // Lower trajectory (more direct)
        double angle = Math.atan((speedSq - root) / (gravity * horizontalDist));

        float xRot = (float) -Math.toDegrees(angle);

        return new Vec2(xRot, yRot);
    }
    public static Vec3 velocityFromRotation(float xRot, float yRot, double speed) {
        double pitch = Math.toRadians(xRot);
        double yaw = Math.toRadians(yRot);

        double vx = -Math.sin(yaw) * Math.cos(pitch) * speed;
        double vy = -Math.sin(pitch) * speed;
        double vz =  Math.cos(yaw) * Math.cos(pitch) * speed;

        return new Vec3(vx, vy, vz);
    }
    public void setRotation(Vec2 rotation) {
        this.xRot = rotation.x;
        this.yRot = rotation.y;
        Vec3 temp = NukeEntity.velocityFromRotation(this.xRot, this.yRot, this.speed);
        this.xSpeed = (float) temp.x;
        this.ySpeed = (float) temp.y;
        this.zSpeed = (float) temp.z;
        this.goodToGo = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide() && this.goodToGo){
            if (this.verticalCollision || this.horizontalCollision){
                this.level.explode(this, this.getX(), this.getY(), this.getZ(), 16.0f, Level.ExplosionInteraction.TNT);
                this.discard();
            }
            this.move(MoverType.SELF, new Vec3(this.xSpeed, this.ySpeed, this.zSpeed));
            this.yRot -= Config.GRAVITY;

        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float v) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        if (valueInput.getString("render").isPresent())
            this.senderName = valueInput.getString("render").get();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {
        valueOutput.putString("sender", this.senderName);
    }
}