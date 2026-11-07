package com.boruebork.nukemod.aircraft;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.drone.AbstractPilotedEntity;
import com.boruebork.nukemod.drone.ClientDroneManager;
import com.boruebork.nukemod.network.packet.ReconcilePayload;
import com.boruebork.nukemod.ooblib.*;
import com.boruebork.nukemod.uav.collision.TerrainCollider;
import com.boruebork.nukemod.uav.flight.FlightModel;
import com.boruebork.nukemod.uav.flight.FlightParams;
import com.boruebork.nukemod.uav.flight.FlightState;
import com.boruebork.nukemod.uav.flight.Sticks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.List;

import static com.boruebork.nukemod.ooblib.FixedWingUtil.*;

public abstract class AbstractFixedWingAircraft extends AbstractPilotedEntity implements HasHitboxParts {
    private final TerrainCollider terrain = new TerrainCollider(this);
    protected float throttle;
    private float groundPitchRate;
    private Sticks lastInput = Sticks.ZERO;

    public AbstractFixedWingAircraft(EntityType<?> type, Level level) {
        super(type, level);
        this.interpolation = new InterpolationHandler(this, 1);
    }
    private Sticks lastPredicted = Sticks.ZERO;
    public Sticks lastPredicted() { return lastPredicted; }
    // ---- what a concrete aircraft supplies ----
    protected abstract FlightParams flightParams();
    public abstract Sticks computeClientSticks();            // client: input devices -> sticks

    // ---- hooks with sensible defaults ----
    protected boolean predictsLocally() { return true; }
    protected void onModeReceived(int mode) {}
    protected Sticks serverSticks() { return isBeingPiloted() ? lastInput : Sticks.ZERO; }

    // ---- server input ----
    public void onInput(Sticks s, int clientTick) {
        if (!Float.isFinite(s.throttle() + s.pitch() + s.roll() + s.yaw())) return;
        lastInput = new Sticks(Mth.clamp(s.throttle(), -1f, 1f), Mth.clamp(s.pitch(), -1f, 1f),
                Mth.clamp(s.roll(), -1f, 1f), Mth.clamp(s.yaw(), -1f, 1f));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            stepWith(serverSticks());
            checkOBBCollisions();
            checkGroundCollision();
        }
        sanitizeState();
    }

    @Override
    public boolean isClientAuthoritative() {
        if (level().isClientSide())
            return ClientDroneManager.PilotingClientState.drone == this && predictsLocally();
        return isBeingPiloted();
    }
    private FlightState captureState() {
        return new FlightState(position(), getDeltaMovement(), getYRot(), getXRot(), roll,
                throttle, groundPitchRate, onGround());
    }
    private void stepWith(Sticks in) {
        FlightState before = captureState();
        FlightModel.Result r = FlightModel.step(before, in, flightParams(), terrain::move);
        FlightState s = r.state();

        xRotO = getXRot(); yRotO = getYRot(); rollO = roll;       // interpolation anchors
        setYRot(s.yaw()); setXRot(s.pitch()); this.roll = s.roll(); // rotation first: setPos rebuilds the bounding box from it
        this.throttle = s.throttle();
        this.groundPitchRate = s.groundPitchRate();
        if (r.flagsValid()) {
            verticalCollision = r.vertical();
            horizontalCollision = r.horizontal();
            setOnGround(s.onGround());
        }
        if (!s.pos().equals(before.pos())) setPos(s.pos().x, s.pos().y, s.pos().z);
        setDeltaMovement(s.vel());
        if (!level().isClientSide()) entityData.set(ROLL_DATA, roll);   // the old code set this on the client too
    }
    @Override
    protected void applyMovement(float fwd, float roll, boolean up, boolean down, float yaw, float pitch) {
        boolean pilot = level().isClientSide() && ClientDroneManager.PilotingClientState.drone == this;
        if (level().isClientSide() && !pilot) return;
        stepWith(new Sticks(fwd, pitch, roll, yaw));
    }
    private int lastInputTick = -1;
    public void setLastInputTick(int t) { this.lastInputTick = t; }
    public int getLastInputTick()      { return this.lastInputTick; }
    private final ArrayDeque<StateTick> history = new ArrayDeque<>();
    private record InputTick(Sticks sticks) {}
    private record StateTick(int tick, FlightState state, InputTick input) {}   // state AFTER the step
    private static final int HISTORY_SIZE = 40;
    private static final double RECONCILE_THRESHOLD = 0.15;   // blocks
    private static final int MAX_REPLAY_TICKS = 8;
    @Override
    protected void onClientPrediction() {
        Sticks s = computeClientSticks();                        // change its return type to Sticks, delete ClientSticks
        stepWith(s);
        ClientDroneManager.PilotingClientState.roll = roll;
        ClientDroneManager.PilotingClientState.rollO = rollO;
        if (!predictsLocally()) history.clear();
        history.addLast(new StateTick(tickCount, captureState(),
                new InputTick(s)));
        while (history.size() > HISTORY_SIZE) history.removeFirst();
    }
    public void reconcileFromServer(ReconcilePayload p) {
        FlightState auth = new FlightState(new Vec3(p.motion().x(), p.motion().y(), p.motion().z()), new Vec3(p.motion().dx(), p.motion().dy(), p.motion().dz()),
                p.attitude().yRot(), p.attitude().xRot(), p.attitude().roll(), p.throttle(), p.groundPitchRate(), p.onGround());
        if (!predictsLocally()) { applyState(auth); history.clear(); return; }

        while (!history.isEmpty() && history.peekFirst().tick() <= p.inputTick()) history.removeFirst();

        if (history.size() > MAX_REPLAY_TICKS) {          // lag spike: too old to replay
            applyState(auth); history.clear(); return;
        }

        FlightState s = auth;
        ArrayDeque<StateTick> replayed = new ArrayDeque<>();
        for (StateTick h : history) {
            s = FlightModel.step(s, h.input().sticks(), flightParams(), terrain::move).state();
            replayed.addLast(new StateTick(h.tick(), s, h.input()));   // keep history consistent with the correction
        }
        history.clear();
        history.addAll(replayed);

        if (s.pos().distanceTo(position()) > RECONCILE_THRESHOLD) applyState(s);
    }
    // COLLISIONS
    protected boolean checkGroundCollision() {
        TerrainCollider.Contact c = terrain.findContact(getEntityRotation(), position());
        if (c == null) return false;
        onGroundCollision(c.part(), c.pos(), c.state());
        return true;
    }
    protected void onGroundCollision(HitboxPart part, BlockPos pos, BlockState state) {}
    protected void checkOBBCollisions() {
        AABB broadphase = getBroadphaseAABB();
        List<Entity> entities = level().getEntitiesOfClass(
                Entity.class,
                broadphase,
                entity -> entity != this && entity.isPickable()
        );
        for (Entity entity : entities) {
            checkOBBCollision(entity);
        }
    }
    private void applyState(FlightState s) {
        setYRot(s.yaw()); setXRot(s.pitch());
        this.roll = this.rollO = s.roll();
        this.throttle = s.throttle();
        this.groundPitchRate = s.groundPitchRate();
        setPos(s.pos().x, s.pos().y, s.pos().z);
        setDeltaMovement(s.vel());
        setOnGround(s.onGround());
        if (!level().isClientSide()) entityData.set(ROLL_DATA, roll);
    }
    protected void checkOBBCollision(Entity other) {
        if (!(other instanceof AbstractFixedWingAircraft otherUav)) return;
        for (HitboxPart thisPart : getHitboxParts()) {
            for (HitboxPart otherPart : otherUav.getHitboxParts()) {
                if (thisOBBsIntersect(getWorldOBB(thisPart), otherUav.getWorldOBB(otherPart))) {
                    onOBBCollision(thisPart, otherUav, otherPart);
                }
            }
        }
    }
    @Override
    public Quaternionf getEntityRotation() {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-this.getYRot()))
                .rotateX((float) Math.toRadians(this.getXRot()))
                .rotateZ((float) Math.toRadians(this.roll));
    }
    public Quaternionf getClientEntityRotation(float partialTick) {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-this.getYRot(partialTick)))
                .rotateX((float) Math.toRadians(this.getXRot(partialTick)))
                .rotateZ((float) Math.toRadians(this.getRenderRoll(partialTick)));
    }
    @Override
    protected @Nullable AABB makeBoundingBox(Vec3 pos) {
        return computeEnclosingAABB(pos);
    }
    public Vec3 getRenderPosition(float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, this.xo, this.getX()),
                Mth.lerp(partialTick, this.yo, this.getY()),
                Mth.lerp(partialTick, this.zo, this.getZ())
        );
    }
    public float getRenderYaw(float partialTick) { return Mth.rotLerp(partialTick, yRotO, getYRot()); }
    public float getRenderPitch(float partialTick) { return Mth.lerp(partialTick, xRotO, getXRot()); }
    public float getRenderRoll(float partialTick) {
        boolean isPilot = level().isClientSide()
                && ClientDroneManager.PilotingClientState.drone == this;
        float current = isPilot ? this.roll : this.entityData.get(ROLL_DATA);
        return Mth.lerp(partialTick, rollO, current);
    }
    protected void onOBBCollision(HitboxPart thisPart, AbstractFixedWingAircraft other, HitboxPart otherPart) {}
    private Vec3 lastGoodPos = null;
    private void sanitizeState() {
        if (lastGoodPos == null) lastGoodPos = position();

        if (!isFinite(getDeltaMovement())) setDeltaMovement(Vec3.ZERO);

        if (isFinite(position())) {
            lastGoodPos = position();
        } else {
            setDeltaMovement(Vec3.ZERO);
            this.throttle = 0f;
            setPos(lastGoodPos.x, lastGoodPos.y, lastGoodPos.z);
            NukeModbyBoruebork.LOGGER.warn("Recovered drone from NaN position → {}", lastGoodPos);
        }

        if (!Float.isFinite(this.throttle)) this.throttle = 0f;
        if (!Float.isFinite(this.roll))     this.roll = 0f;
        if (!Float.isFinite(getYRot()))     setYRot(0f);
        if (!Float.isFinite(getXRot()))     setXRot(0f);
    }
    protected void resolveInitialPenetration() {
        double x = getX(), y = getY(), z = getZ();
        boolean stuck = hasTerrainCollisionAt(x, y, z);
        for (int i = 0; i < 32 && hasTerrainCollisionAt(x, y, z); i++) {
            y += 1.0 / 16.0;
        }
        if (y != getY()) {
            setPos(x, y, z);

        }
    }
    protected boolean hasTerrainCollisionAt(double x, double y, double z) {   // kept for resolveInitialPenetration
        return terrain.collidesAt(getEntityRotation(), x, y, z);
    }
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        resolveInitialPenetration();
    }
    @Override
    public boolean isPickable() {
        return true;
    }
    public static final EntityDataAccessor<Float> HEALTH_DATA =
            SynchedEntityData.defineId(AbstractFixedWingAircraft.class, EntityDataSerializers.FLOAT);
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);   // AbstractPilotedEntity's ROLL_DATA and CONTROLLER_DATA
        builder.define(HEALTH_DATA, getMaxHealth());
    }
    public float getHealth() { return this.entityData.get(HEALTH_DATA); }
    public void setHealth(float h) { this.entityData.set(HEALTH_DATA, Mth.clamp(h, 0f, getMaxHealth())); }
    public abstract float getMaxHealth();
    public abstract float getDeathExplosionPower();
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isRemoved()) return false;
        if (!this.damageActuallyHitsHitbox(source)) return false;
        if (isInvulnerableTo(level, source)) return false;

        // Vanilla-style invulnerability frames: ignore rapid re-hits.
        if (this.invulnerableTime > 10) return false;

        float scaled = scaleDamage(source, amount);
        if (scaled <= 0f) return false;

        float newHealth = getHealth() - scaled;
        setHealth(newHealth);
        this.invulnerableTime = 10;

        if (newHealth <= 0f) {
            destroy(source);
        }
        return true;
    }
    protected float scaleDamage(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return amount;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return amount * 0.25f;
        if (source.getDirectEntity() instanceof Player || source.getDirectEntity() instanceof Mob) {
            return amount * 0.5f;
        }
        return 0f;
    }
    protected boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (source.is(DamageTypeTags.IS_FALL))    return true;
        if (source.is(DamageTypeTags.IS_FIRE))    return true;
        if (source.is(DamageTypeTags.IS_DROWNING)) return true;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return false;
    }
    protected void destroy(DamageSource source) {
        if (!(level() instanceof ServerLevel sl)) return;

        sl.explode(this,
                getX(), getY(), getZ(),
                getDeathExplosionPower(),
                Level.ExplosionInteraction.NONE);   // NONE = no terrain damage

        this.stopOperating();   // clears controller, drops camera on the pilot's client
        this.discard();
    }
    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putFloat("Throttle", this.throttle);
    }
    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.throttle  = in.getFloatOr("Throttle", 0f);

    }
}
