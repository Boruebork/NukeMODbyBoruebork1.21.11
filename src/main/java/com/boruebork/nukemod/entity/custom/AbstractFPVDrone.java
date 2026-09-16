package com.boruebork.nukemod.entity.custom;

import com.boruebork.nukemod.drone.ClientDroneManager;
import com.boruebork.nukemod.drone.DroneManager;
import com.boruebork.nukemod.network.packet.DroneInputPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static net.minecraft.world.entity.player.Player.MAX_HEALTH;

public abstract class AbstractFPVDrone extends Entity {
    private UUID controllerId;
    private int tickNum = 0;
    public static final EntityDataAccessor<String> CONTROLLER_DATA =
            SynchedEntityData.defineId(
                    // The class of the entity.
                    AbstractFPVDrone.class,
                    // The entity data accessor type.
                    EntityDataSerializers.STRING
            );
    public static final EntityDataAccessor<Float> HEALTH_DATA =
            SynchedEntityData.defineId(AbstractFPVDrone.class, EntityDataSerializers.FLOAT);
    public AbstractFPVDrone(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CONTROLLER_DATA, this.controllerId == null ? "" : this.controllerId.toString());
        builder.define(HEALTH_DATA, getMaxHealth());
    }
    public float getHealth() {
        return this.entityData.get(HEALTH_DATA);
    }

    public void setHealth(float health) {
        this.entityData.set(HEALTH_DATA, Mth.clamp(health, 0.0F, MAX_HEALTH));
    }
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (!this.entityData.get(CONTROLLER_DATA).isEmpty()) {
                if (ClientDroneManager.PilotingClientState.drone == null) {
                    ClientDroneManager.PilotingClientState.drone = this;
                    ClientDroneManager.PilotingClientState.yRot = this.getYRot();
                    ClientDroneManager.PilotingClientState.xRot = this.getXRot();
                    Minecraft.getInstance().setCameraEntity(this);
                }

                if (ClientDroneManager.PilotingClientState.drone == this) {
                    applyMovement(
                            ClientDroneManager.PilotingClientState.z,
                            ClientDroneManager.PilotingClientState.x,
                            ClientDroneManager.PilotingClientState.up,
                            ClientDroneManager.PilotingClientState.down,
                            ClientDroneManager.PilotingClientState.movementYRot
                    );
                }
            }
            //System.out.println("Client pos: " +  this.getOnPos());
        }else{
            if (this.tickNum == 0){
                this.move(MoverType.SELF, new Vec3(0, 0.5, 0));
            }
            tickNum++;
            //System.err.println("Server pos: " +  this.getOnPos());
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        System.err.println("hurt server");
        if (this.isInvulnerableTo(level, damageSource)) return false;

        // only explosions hurt it — bullets, punches, fall damage etc. still do nothing
        if (!damageSource.is(DamageTypeTags.IS_EXPLOSION)) return false;

        float newHealth = this.getHealth() - amount;
        this.setHealth(newHealth);
        System.err.println(newHealth);
        if (newHealth <= 0.0F) {
            this.destroyDrone(damageSource);
        }
        return true;
    }


    private boolean isInvulnerableTo(ServerLevel level, DamageSource damageSource) {
        if (damageSource.is(DamageTypeTags.IS_EXPLOSION)) return false;
        return true;
    }

    private void destroyDrone(DamageSource cause) {
        this.level().broadcastEntityEvent(this, (byte) 60); // trigger a client-side particle/sound burst, see below
        this.stopOperating();
        this.discard();
    }
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            return InteractionResult.FAIL;
        }
        if (!level().isClientSide()){
            if (this.entityData.get(CONTROLLER_DATA) != "") return super.interact(player, hand);
            this.controllerId = player.getUUID();
            DroneManager.getInstance().addEntry(player, this);
            this.entityData.set(CONTROLLER_DATA, this.controllerId.toString());
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand);
    }
    @Override
    public boolean isPickable() {
        return true;
    }

    public void updatePosRot(DroneInputPayload data) {
        this.xRotO = getXRot();
        this.setXRot(data.xRot());
        this.yRotO = getYRot();
        this.setYRot(data.yRot());
        applyMovement(data.dz(), data.forward(), data.up(), data.down(), data.yRot());
    }
    public void stopOperating() {
        if (controllerId == null) return;
        Player player = this.level().getPlayerByUUID(controllerId);
        this.controllerId = null;
        this.entityData.set(CONTROLLER_DATA, "");
        if (player == null) return;
        DroneManager.getInstance().playerToDrone.remove(player.getUUID());
    }

    @Override
    public boolean isClientAuthoritative() {
        return level().isClientSide() && ClientDroneManager.PilotingClientState.drone == this;

    }
    // In AbstractFPVDrone — shared by both client prediction and server authority
    public void applyMovement(float forward, float strafe, boolean up, boolean down, float yRot) {
        if (!level().isClientSide()) {
            this.yRotO = this.getYRot();
            this.setYRot(yRot);
        }

        float speed = 0.3f;

        // build forward direction from BOTH yaw and pitch, so looking down
        // while moving forward naturally dives — same math as Entity#getViewVector / elytra flight
        float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
        float pitchRad = this.getXRot() * Mth.DEG_TO_RAD;

        float sinYaw = Mth.sin(-yawRad);
        float cosYaw = Mth.cos(-yawRad);
        float sinPitch = Mth.sin(-pitchRad);
        float cosPitch = Mth.cos(pitchRad);

        // forward vector tilted by pitch
        Vec3 forwardVec = new Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
        // strafe stays horizontal-only — sideways movement shouldn't dive/climb from pitch
        Vec3 strafeVec = new Vec3(cosYaw, 0, -sinYaw);

        Vec3 localMove = forwardVec.scale(forward * speed).add(strafeVec.scale(strafe * speed));

        // up/down is a separate boost added on top of whatever pitch-driven motion already gave us
        float vert = 0;
        if (up)   vert = 0.1f;
        if (down) vert = -0.1f;
        localMove = localMove.add(0, vert, 0);

        this.setDeltaMovement(localMove);
        this.move(MoverType.PLAYER, this.getDeltaMovement());

        if (!level().isClientSide()) {
            boolean hitBlock = this.horizontalCollision || this.verticalCollision;
            boolean hitEntity = !level().getEntities(this, this.getBoundingBox(), e -> e.isPickable() && e != this).isEmpty();

            if (hitBlock || hitEntity) {
                explode();
            }
        }
    }

    private void explode() {
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), 4f, Level.ExplosionInteraction.TNT);
        this.stopOperating();
        this.discard();
    }// Client-side, called every FRAME (e.g. from ClientTickEvent or a mouse-move hook),
    // NOT from Entity#tick()
    public void updateLookClientSide(double mouseYaw, double mousePitch) {
        if (ClientDroneManager.PilotingClientState.drone != this) return;

        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.setYRot((float) mousePitch);
        this.setXRot((float) mouseYaw);
    }
    public boolean isBeingPiloted(){
        return !this.entityData.get(CONTROLLER_DATA).isEmpty();
    }
    protected abstract float getMaxHealth();
    protected abstract float getVerticalSpeedModifier();
    protected abstract float getHorizontalSpeedModifier();
}
