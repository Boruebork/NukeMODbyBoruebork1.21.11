package com.boruebork.nukemod.drone;
// NOTE: adjust this package to wherever you actually want the generic base
// to live -- I've put it one level up from .fpvdrones since the whole point
// is that other drone types (non-FPV) could extend this too. Update the
// import in AbstractFPVDrone.java to match if you move it elsewhere.

import com.boruebork.nukemod.network.packet.DroneInputPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

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
public abstract class AbstractDrone extends Entity implements Pilotable {
    protected InterpolationHandler interpolation = new InterpolationHandler(this);
    protected PilotSeat pilotSeat;
    @Override
    public PilotSeat pilotSeat() {
        return pilotSeat;
    }

    // --- piloting / camera ---
    public static final EntityDataAccessor<String> CONTROLLER_DATA =
            SynchedEntityData.defineId(AbstractDrone.class, EntityDataSerializers.STRING);

    @Override
    public Entity asEntity() {
        return this;
    }

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
        this.pilotSeat = new PilotSeat(this, CONTROLLER_DATA);
        if (level.isClientSide()) {
            /*PilotHooks.client = (drone, isLocalPilot) -> {
                // Mount the camera when the server tells us we're the controller.
                if (!isLocalPilot && !this.entityData.get(CONTROLLER_DATA).isEmpty()) {
                    if (ClientDroneManager.PilotingClientState.drone == null
                            && ClientDroneManager.checkIfPilot(this)) {
                        ClientDroneManager.PilotingClientState.drone = this;
                        ClientDroneManager.PilotingClientState.yRot = this.getYRot();
                        ClientDroneManager.PilotingClientState.xRot = this.getXRot();
                        ClientDroneManager.PilotingClientState.cameraYaw = this.getYRot();
                        ClientDroneManager.PilotingClientState.cameraPitch = this.getXRot();
                        ClientDroneManager.PilotingClientState.roll = this.roll;
                        ClientDroneManager.PilotingClientState.rollO = this.rollO;
                        ClientDroneManager.PilotingClientState.mode = ClientDroneManager.PilotingClientState.PilotingMode.FIXED;
                        Minecraft.getInstance().setCameraEntity(this);
                        isLocalPilot = true;
                    }
                }

                if (isLocalPilot && isClientAuthoritative()) {
                    onClientPrediction();
                    ClientDroneManager.PilotingClientState.roll  = this.roll;
                    ClientDroneManager.PilotingClientState.rollO = this.rollO;
                } else {
                    // Observer: server-authoritative, smooth between packets.
                    interpolation.interpolate();
                    this.rollO = this.roll;
                    this.roll  = this.entityData.get(ROLL_DATA);
                }
            };*/
        }
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CONTROLLER_DATA, "");
        builder.define(ROLL_DATA, 0f);
    }
    @Override
    public @Nullable InterpolationHandler getInterpolation() {
        return this.interpolation;
    }
    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            PilotHooks.client.tick(this);
        } else if (!isBeingPiloted()) {
            // Server auto-level when nobody is flying.
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
        if (level().isClientSide()) return InteractionResult.FAIL;
        System.err.println("Server");
        if (!(player instanceof ServerPlayer sp) || !pilotSeat.tryEnter(sp)) return super.interact(player, hand);
        System.err.println("player entitern");
        onPlayerEnter(player);
        return InteractionResult.SUCCESS;
    }

    public void stopOperating() {
        pilotSeat.release();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        pilotSeat.save(out);
        out.putFloat("Roll", roll);
    }
    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        pilotSeat.load(in);
        roll = in.getFloatOr("Roll", 0f); rollO = roll;
    }

    protected void onPlayerEnter(Player player) {
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


    @Override
    public boolean isClientAuthoritative() {
        if (level().isClientSide()) {
            return ClientDroneManager.PilotingClientState.drone == this;
        }
        return !this.getEntityData().get(CONTROLLER_DATA).isEmpty();
    }

    public boolean isBeingPiloted() {
        return !this.entityData.get(CONTROLLER_DATA).isEmpty();
    }

    // Client-side, called every FRAME (e.g., from ClientTickEvent or a mouse-move hook),
    // NOT from Entity#tick()
    public void updateLookClientSide(double mousePitch, double mouseYaw) {
        if (ClientDroneManager.PilotingClientState.drone != this) return;

        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.setYRot((float) mouseYaw);
        this.setXRot((float) mousePitch);
    }

    public float getRoll() {
        return roll;
    }

    public float getRollO() {
        return rollO;
    }
    /** Called each tick on the pilot's own client when this drone is client-authoritative.
     *  Implementations should run their local flight model here. */
    protected void onClientPrediction() {
        applyMovement(
                ClientDroneManager.PilotingClientState.z,
                ClientDroneManager.PilotingClientState.x,
                ClientDroneManager.PilotingClientState.up,
                ClientDroneManager.PilotingClientState.down,
                ClientDroneManager.PilotingClientState.movementYRot,
                ClientDroneManager.PilotingClientState.xRot);
        ClientDroneManager.PilotingClientState.roll  = this.roll;
        ClientDroneManager.PilotingClientState.rollO = this.rollO;
    }
    public void tickAsObserver() {
        interpolation.interpolate();
        this.rollO = this.roll;
        this.roll = this.entityData.get(ROLL_DATA);
    }
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide()) pilotSeat.release();
        super.remove(reason);
    }
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key == CONTROLLER_DATA && level().isClientSide()) ClientDroneManager.onDroneClientTick(this);
    }
}