package com.boruebork.nukemod.entity.custom;

import com.boruebork.nukemod.drone.ClientDroneManager;
import com.boruebork.nukemod.drone.DroneManager;
import com.boruebork.nukemod.network.packet.DroneInputPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.*;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static net.minecraft.world.entity.player.Player.MAX_HEALTH;

public abstract class AbstractFPVDrone extends Entity {
    public float roll = 0f;
    public float rollO = 0f; // for interpolation, like xRotO/yRotO

    // --- roll tuning ---
    public static final float MAX_ROLL_DEGREES = 35f;  // full bank angle at max strafe input
    private static final float ROLL_RESPONSE = 0.15f;   // how fast roll chases its target while actively strafing
    private static final float ROLL_RECOVERY = 0.10f;   // how fast roll returns to level when idle/unpiloted
    private static final float TURN_BANK_FACTOR = 2.0f; // degrees of extra bank per degree/tick of yaw rate
    // tracks yaw independently of vanilla's yRotO, which client-side prediction never touches
    // inside applyMovement — needed to compute yaw *rate* for the coordinated-turn bank below.
    private float prevYawForBank = 0f;
    private boolean prevYawForBankInit = false;

    private UUID controllerId;
    private int tickNum = 0;
    private float rotorSpeed = 0f;
    private float rotorAngle = 0f;
    private static final int TICKET_RADIUS = 3; // chunks; ~48 blocks
    private static final int TICKET_LEVEL = 31; // see note below on what this controls
    public static final EntityDataAccessor<String> CONTROLLER_DATA =
            SynchedEntityData.defineId(
                    // The class of the entity.
                    AbstractFPVDrone.class,
                    // The entity data accessor type.
                    EntityDataSerializers.STRING
            );
    public static final EntityDataAccessor<Float> HEALTH_DATA =
            SynchedEntityData.defineId(AbstractFPVDrone.class, EntityDataSerializers.FLOAT);
    // Synced so third-party observers (anyone not piloting this drone) see it bank too,
    // not just the pilot's own client.
    public static final EntityDataAccessor<Float> ROLL_DATA =
            SynchedEntityData.defineId(AbstractFPVDrone.class, EntityDataSerializers.FLOAT);
    private long ticketTimer = 0;

    public AbstractFPVDrone(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CONTROLLER_DATA, this.controllerId == null ? "" : this.controllerId.toString());
        builder.define(HEALTH_DATA, getMaxHealth());
        builder.define(ROLL_DATA, 0f);
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
                    // applyMovement() just recomputed this.roll — mirror it every tick so the
                    // pilot's camera (onCameraAngles) actually sees it change, not just at mount time.
                    ClientDroneManager.PilotingClientState.roll = this.roll;
                    ClientDroneManager.PilotingClientState.rollO = this.rollO;
                }
            }
        }else{
            if (level() instanceof ServerLevel sl) {
                if (isBeingPiloted()) {
                    if (this.ticketTimer > 0L) {
                        this.ticketTimer--;
                    }
                } else {
                    this.ticketTimer = 0L;   // will re-arm instantly on next piloting
                }
            }

            if (this.tickNum == 0){
                this.move(MoverType.SELF, new Vec3(0, 0.5, 0));
            }
            tickNum++;
        }
        float targetSpeed = isBeingPiloted() ? 45f : 0f; // degrees/tick at full spin
        // ease toward target so rotors spin up/down instead of snapping
        this.rotorSpeed += (targetSpeed - this.rotorSpeed) * 0.1f;
        this.rotorAngle = (this.rotorAngle + this.rotorSpeed) % 360f;

        // --- roll sync/auto-level ---
        // applyMovement() is the only place roll is actively *computed* (it runs on whichever
        // side is authoritative for this tick: the piloting client, or the server when it
        // receives an input packet). Everyone else just needs to mirror or settle it here.
        if (level().isClientSide()) {
            if (ClientDroneManager.PilotingClientState.drone != this) {
                // third-party viewer (or nobody piloting): mirror the server-synced value
                this.rollO = this.roll;
                this.roll = this.entityData.get(ROLL_DATA);
            }
        } else if (!isBeingPiloted()) {
            // server-authoritative auto-level: nobody's sending input packets right now
            this.rollO = this.roll;
            this.roll += (0f - this.roll) * ROLL_RECOVERY;
            this.entityData.set(ROLL_DATA, this.roll);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        if (this.isInvulnerableTo(level, damageSource)) return false;

        // only explosions hurt it — bullets, punches, fall damage etc. still do nothing
        if (!damageSource.is(DamageTypeTags.IS_EXPLOSION)) return false;

        float newHealth = this.getHealth() - amount;
        this.setHealth(newHealth);
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

        Vec3 localMove = forwardVec.scale(forward * getHorizontalSpeedModifier()).add(strafeVec.scale(strafe * getHorizontalSpeedModifier()));

        // up/down is a separate boost added on top of whatever pitch-driven motion already gave us
        float vert = 0;
        if (up)   vert = 0.1f;
        if (down) vert = -0.1f;
        localMove = localMove.add(0, vert * getVerticalSpeedModifier(), 0);

        this.setDeltaMovement(localMove);
        this.move(MoverType.PLAYER, this.getDeltaMovement());

        // --- roll ---
        // Two contributions, combined and clamped:
        //  1) strafe input  -> "banking" the same way a plane rolls to slide sideways
        //  2) yaw *rate*    -> "coordinated turn" bank, same convention flight/space sims use:
        //     the faster you're turning right now, the more you bank into that turn.
        //     This is an arcade approximation (real turn-coordination depends on turn radius
        //     and airspeed too), but it reads correctly and costs almost nothing to compute.
        float currentYaw = this.getYRot();
        float yawRate = 0f; // degrees turned this tick, signed
        if (prevYawForBankInit) {
            yawRate = Mth.wrapDegrees(currentYaw - prevYawForBank);
        }
        prevYawForBank = currentYaw;
        prevYawForBankInit = true;

        float strafeRollTarget = -strafe * MAX_ROLL_DEGREES;
        float turnRollTarget = -yawRate * TURN_BANK_FACTOR;
        float targetRoll = Mth.clamp(strafeRollTarget + turnRollTarget, -MAX_ROLL_DEGREES, MAX_ROLL_DEGREES);

        this.rollO = this.roll;
        this.roll += (targetRoll - this.roll) * ROLL_RESPONSE;
        if (!level().isClientSide()) {
            this.entityData.set(ROLL_DATA, this.roll);
        }

        if (!level().isClientSide()) {
            boolean hitBlock = this.horizontalCollision || this.verticalCollision;
            boolean hitEntity = !level().getEntities(this, this.getBoundingBox(), e -> e.isPickable() && e != this).isEmpty();

            if (hitBlock || hitEntity) {
                explode();
            }
        }
    }

    private void explode() {
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), getOnHitExplosionRadius(), Level.ExplosionInteraction.TNT);
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
    }public float getRotorAngle() {
        return rotorAngle;
    }
    public float getRotorSpeed() {
        return rotorSpeed;
    }
    public float getRoll() {
        return roll;
    }
    public float getRollO() {
        return rollO;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {

    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {

    }

    protected abstract float getMaxHealth();
    protected abstract float getVerticalSpeedModifier();
    protected abstract float getHorizontalSpeedModifier();
    protected abstract float getOnHitExplosionRadius();
}
