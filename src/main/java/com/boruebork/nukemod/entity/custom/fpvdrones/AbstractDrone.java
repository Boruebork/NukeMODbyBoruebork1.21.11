package com.boruebork.nukemod.entity.custom.fpvdrones;
// NOTE: adjust this package to wherever you actually want the generic base
// to live -- I've put it one level up from .fpvdrones since the whole point
// is that other drone types (non-FPV) could extend this too. Update the
// import in AbstractFPVDrone.java to match if you move it elsewhere.

import com.boruebork.nukemod.drone.ClientDroneManager;
import com.boruebork.nukemod.drone.DroneManager;
import com.boruebork.nukemod.network.packet.DroneInputPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

/**
 * Generic base for any entity that can be piloted via
 * Minecraft#setCameraEntity, with synced roll for banking/tilting while
 * flown. Deliberately has no opinion on health, propulsion, or how
 * movement itself is computed -- that's left to subclasses (see
 * {@link #applyMovement}), since a non-FPV drone or a different vehicle
 * entirely might fly very differently while still wanting the same
 * piloting handshake and roll-banking behavior.
 */
public abstract class AbstractDrone extends Entity {

    // --- piloting / camera ---
    private UUID controllerId;
    public static final EntityDataAccessor<String> CONTROLLER_DATA =
            SynchedEntityData.defineId(AbstractDrone.class, EntityDataSerializers.STRING);

    // --- roll ---
    public float roll = 0f;
    public float rollO = 0f; // for interpolation, like xRotO/yRotO
    public static final EntityDataAccessor<Float> ROLL_DATA =
            SynchedEntityData.defineId(AbstractDrone.class, EntityDataSerializers.FLOAT);

    public static final float MAX_ROLL_DEGREES = 35f; // full bank angle, clamps any target passed to updateRollTowards
    private static final float ROLL_RESPONSE = 0.15f;  // how fast roll chases its target while actively flown
    private static final float ROLL_RECOVERY = 0.10f;  // how fast roll returns to level when idle/unpiloted

    public AbstractDrone(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CONTROLLER_DATA, this.controllerId == null ? "" : this.controllerId.toString());
        builder.define(ROLL_DATA, 0f);
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
                            ClientDroneManager.PilotingClientState.movementYRot,
                            ClientDroneManager.PilotingClientState.xRot
                    );
                    // applyMovement() just recomputed this.roll via updateRollTowards() --
                    // mirror it every tick so the pilot's camera (onCameraAngles) sees it
                    // change, not just at mount time.
                    ClientDroneManager.PilotingClientState.roll = this.roll;
                    ClientDroneManager.PilotingClientState.rollO = this.rollO;
                }
            }
        }

        // --- roll sync/auto-level ---
        // applyMovement() (implemented by subclasses) is the only place roll is actively
        // *targeted*; it runs on whichever side is authoritative for this tick (the piloting
        // client, or the server when it receives an input packet via updatePosRot). Everyone
        // else just needs to mirror or settle it here.
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

    /**
     * Eases roll toward {@code targetRoll} (clamped to +/-MAX_ROLL_DEGREES) and syncs it
     * when server-authoritative. Call this from your {@link #applyMovement} implementation
     * once you've computed whatever target bank angle your flight model wants -- this method
     * owns the actual state transition (lerp rate, clamping, sync), your movement model just
     * decides what angle to chase.
     */
    protected void updateRollTowards(float targetRoll) {
        float clamped = Mth.clamp(targetRoll, -MAX_ROLL_DEGREES, MAX_ROLL_DEGREES);
        this.rollO = this.roll;
        this.roll += (clamped - this.roll) * ROLL_RESPONSE;
        if (!level().isClientSide()) {
            this.entityData.set(ROLL_DATA, this.roll);
        }
    }

    /**
     * Implement your flight model here: read whatever input the caller gives you, move the
     * entity, and call {@link #updateRollTowards} with your computed bank target. Called both
     * from the piloting client's own tick (predicted movement) and from
     * {@link #updatePosRot} on the server (authoritative movement from a network packet) --
     * same method, same semantics, different caller.
     */
    protected abstract void applyMovement(float forward, float strafe, boolean up, boolean down, float yRot, float xRot);

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            return InteractionResult.FAIL;
        }
        if (this.entityData.get(CONTROLLER_DATA) != "") return super.interact(player, hand);
        this.controllerId = player.getUUID();
        DroneManager.getInstance().addEntry(player, this);
        this.entityData.set(CONTROLLER_DATA, this.controllerId.toString());
        return InteractionResult.SUCCESS;
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
        applyMovement(data.dz(), data.forward(), data.up(), data.down(), data.yRot(), data.xRot());
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

    public boolean isBeingPiloted() {
        return !this.entityData.get(CONTROLLER_DATA).isEmpty();
    }

    // Client-side, called every FRAME (e.g. from ClientTickEvent or a mouse-move hook),
    // NOT from Entity#tick()
    public void updateLookClientSide(double mouseYaw, double mousePitch) {
        if (ClientDroneManager.PilotingClientState.drone != this) return;

        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.setYRot((float) mousePitch);
        this.setXRot((float) mouseYaw);
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
}