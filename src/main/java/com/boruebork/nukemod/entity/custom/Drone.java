package com.boruebork.nukemod.entity.custom;

import com.boruebork.nukemod.drone.ClientDroneManager;
import com.boruebork.nukemod.drone.DroneManager;
import com.boruebork.nukemod.network.packet.DroneInputPayload;
import com.boruebork.nukemod.network.packet.NotifyClientDroneExit;
import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public abstract class Drone extends Entity {
    private UUID controllerId;
    private int tickNum = 0;
    public static final EntityDataAccessor<String> CONTROLLER_DATA =
            SynchedEntityData.defineId(
                    // The class of the entity.
                    Drone.class,
                    // The entity data accessor type.
                    EntityDataSerializers.STRING
            );
    public Drone(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CONTROLLER_DATA, this.controllerId == null ? "" : this.controllerId.toString());
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
                    //System.err.println("CLIENT pos=" + this.position() + " delta=" + this.getDeltaMovement());
                }
            }
        }else{
            System.out.println(isBeingPiloted());
            System.out.println();
            if (this.tickNum == 0){
                this.move(MoverType.SELF, new Vec3(0, 0.5, 0));
            }
            tickNum++;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float v) {
        return false;
    }
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        //System.err.println("interact");
        if (this.level().isClientSide()) {
            return InteractionResult.FAIL;
        }
        if (!level().isClientSide()){
            if (this.entityData.get(CONTROLLER_DATA) != "") return super.interact(player, hand);
            this.controllerId = player.getUUID();
            DroneManager.getInstance().addEntry(player, this);
            //System.err.println("Set Controller");
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
        //System.err.println("SERVER pos=" + this.position() + " delta=" + this.getDeltaMovement());
    }
    public void stopOperating() {
        if (controllerId == null) return;
        Player player = this.level().getPlayerByUUID(controllerId);
        this.controllerId = null;
        this.entityData.set(CONTROLLER_DATA, "");
        if (player == null) return;
        DroneManager.getInstance().playerToDrone.remove(player.getUUID());
        System.err.println(player.getGameProfile().name());
        //PacketDistributor.sendToPlayer((ServerPlayer) player, new NotifyClientDroneExit());

    }

    @Override
    public boolean isClientAuthoritative() {
        return level().isClientSide() && ClientDroneManager.PilotingClientState.drone == this;

    }
    // In Drone — shared by both client prediction and server authority
    public void applyMovement(float strafe, float forward, boolean up, boolean down, float yRot) {
        if (!level().isClientSide()){
            this.yRotO = this.getYRot();
            this.setYRot(yRot);
        }else {
           /* this.yRotO = ClientDroneManager.PilotingClientState.yRotO;
            this.xRotO = ClientDroneManager.PilotingClientState.xRotO;
            this.setYRot(ClientDroneManager.PilotingClientState.xRot);
            this.setXRot(ClientDroneManager.PilotingClientState.yRot);*/

        }
        float vert = 0;
        if (up)   vert = 0.1f;
        if (down) vert = -0.1f;
        float speed = 0.3f;
        Vec3 localMove = new Vec3(forward * speed, vert, strafe * speed)
                .yRot(-this.getYRot() * ((float) Math.PI / 180F));

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
}
