package com.boruebork.nukemod.entity.custom.fpvdrones;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.world.entity.player.Player.MAX_HEALTH;

/**
 * FPV combat drone: explosion-only health, spinning rotor animation, and a
 * strafe/yaw-rate-banking flight model. Piloting and roll are handled by
 * {@link com.boruebork.nukemod.entity.custom.fpvdrones.AbstractDrone}; this class only supplies what's specific to this
 * particular kind of drone.
 */
public abstract class AbstractFPVDrone extends com.boruebork.nukemod.entity.custom.fpvdrones.AbstractDrone {

    // --- health ---
    public static final EntityDataAccessor<Float> HEALTH_DATA =
            SynchedEntityData.defineId(AbstractFPVDrone.class, EntityDataSerializers.FLOAT);

    // --- rotor animation ---
    private float rotorSpeed = 0f;
    private float rotorAngle = 0f;

    // --- lift-off / chunk ticketing ---
    private int tickNum = 0;
    private long ticketTimer = 0;
    private static final int TICKET_RADIUS = 3; // chunks; ~48 blocks
    private static final int TICKET_LEVEL = 31; // see note below on what this controls
    // (TICKET_RADIUS/TICKET_LEVEL aren't consumed anywhere in this excerpt -- if you've got
    // a chunk-ticket method elsewhere that reads them, it should move here too.)

    // --- flight model: coordinated-turn banking ---
    private static final float TURN_BANK_FACTOR = 2.0f; // degrees of extra bank per degree/tick of yaw rate
    // tracks yaw independently of vanilla's yRotO, which client-side prediction never touches
    // inside applyMovement -- needed to compute yaw *rate* for the coordinated-turn bank below.
    private float prevYawForBank = 0f;
    private boolean prevYawForBankInit = false;

    public AbstractFPVDrone(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
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
        super.tick(); // AbstractDrone: Entity#tick() + camera piloting + roll sync/auto-level
        // NOTE: this reorders things slightly vs. the original single-class version -- roll
        // sync/auto-level now runs *before* the ticketTimer/tickNum/rotor block below rather
        // than after. Harmless: none of these touch roll, and roll's auto-level doesn't
        // depend on them either, so the two blocks are independent either way.

        if (!level().isClientSide()) {
            if (isBeingPiloted()) {
                if (this.ticketTimer > 0L) {
                    this.ticketTimer--;
                }
            } else {
                this.ticketTimer = 0L; // will re-arm instantly on next piloting
            }

            if (this.tickNum == 0) {
                this.move(MoverType.SELF, new Vec3(0, 0.5, 0));
            }
            tickNum++;
        }

        float targetSpeed = isBeingPiloted() ? 45f : 0f; // degrees/tick at full spin
        // ease toward target so rotors spin up/down instead of snapping
        this.rotorSpeed += (targetSpeed - this.rotorSpeed) * 0.1f;
        this.rotorAngle = (this.rotorAngle + this.rotorSpeed) % 360f;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        if (this.isInvulnerableTo(level, damageSource)) return false;

        // only explosions hurt it -- bullets, punches, fall damage etc. still do nothing
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
    protected void applyMovement(float forward, float strafe, boolean up, boolean down, float yRot, float xRot) {
        if (!level().isClientSide()) {
            this.yRotO = this.getYRot();
            this.setYRot(yRot);
        }
        // build forward direction from BOTH yaw and pitch, so looking down
        // while moving forward naturally dives -- same math as Entity#getViewVector / elytra flight
        float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
        float pitchRad = this.getXRot() * Mth.DEG_TO_RAD;

        float sinYaw = Mth.sin(-yawRad);
        float cosYaw = Mth.cos(-yawRad);
        float sinPitch = Mth.sin(-pitchRad);
        float cosPitch = Mth.cos(pitchRad);

        // forward vector tilted by pitch
        Vec3 forwardVec = new Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
        // strafe stays horizontal-only -- sideways movement shouldn't dive/climb from pitch
        Vec3 strafeVec = new Vec3(cosYaw, 0, -sinYaw);

        Vec3 localMove = forwardVec.scale(forward * getHorizontalSpeedModifier()).add(strafeVec.scale(strafe * getHorizontalSpeedModifier()));

        // up/down is a separate boost added on top of whatever pitch-driven motion already gave us
        float vert = 0;
        if (up)   vert = 0.1f;
        if (down) vert = -0.1f;
        localMove = localMove.add(0, vert * getVerticalSpeedModifier(), 0);

        this.setDeltaMovement(localMove);
        this.move(MoverType.PLAYER, this.getDeltaMovement());

        // --- roll target ---
        // Two contributions, combined and left for updateRollTowards() to clamp:
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
        updateRollTowards(strafeRollTarget + turnRollTarget);

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
    }

    public float getRotorAngle() {
        return rotorAngle;
    }

    public float getRotorSpeed() {
        return rotorSpeed;
    }

    protected abstract float getMaxHealth();
    protected abstract float getVerticalSpeedModifier();
    protected abstract float getHorizontalSpeedModifier();
    protected abstract float getOnHitExplosionRadius();
}