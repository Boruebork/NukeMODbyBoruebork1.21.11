package com.boruebork.nukemod.entity.custom.radar;

import com.boruebork.nukemod.entity.NukeMODEntityDataSerializers;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractRadar extends Entity {
    public static final float DEFAULT_RADIUS = 15;
    public static final float DEFAULT_HEALTH = 5;
    private static final int SCAN_INTERVAL_TICKS = 10; // don't scan every single tick — no need for detection to be frame-perfect
    private int scanCooldown = 0;
    public static final EntityDataAccessor<Float> HEALTH =
            SynchedEntityData.defineId(AbstractRadar.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Float> RADIUS =
            SynchedEntityData.defineId(AbstractRadar.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<List<UUID>> CONTACTS =
            SynchedEntityData.defineId(AbstractRadar.class, NukeMODEntityDataSerializers.UUID_LSIT.get());

    public AbstractRadar(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
    public void setRadius(float radius){
        this.entityData.set(RADIUS, radius);
    }
    public List<UUID> getContacts() {
        return this.entityData.get(CONTACTS);
    }
    public void clearContents() {
        this.entityData.set(CONTACTS, new ArrayList<>());
    }
    public void addContact(Entity ent) {
        List<UUID> tmp = new ArrayList<>(getContacts()); // fresh copy
        tmp.add(ent.getUUID());
        this.entityData.set(CONTACTS, tmp);
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, DEFAULT_RADIUS);
        builder.define(HEALTH, DEFAULT_HEALTH);
        builder.define(CONTACTS, new ArrayList<>());
    }
    public float getHealth(){
        return this.entityData.get(HEALTH);
    }
    public float getRange(){
        return this.entityData.get(RADIUS);
    }

    @Override
    public void tick() {
        super.tick();
        if (scanCooldown <= 0){
            if (level().isClientSide()){
                if (Minecraft.getInstance().screen instanceof RadarScreen screen){
                    screen.update(this);
                }
            }else {
                scan();
            }
            scanCooldown = SCAN_INTERVAL_TICKS;
        }else {
            scanCooldown--;
        }

    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float amount) {
        if (!damageSource.is(DamageTypeTags.IS_EXPLOSION)) return false;

        float newHealth = getHealth() - amount;
        setHealth(newHealth);

        if (newHealth <= 0.0F) {
            destroyRadar();
        }
        return true;
    }

    public void setHealth(float health) {
        this.entityData.set(HEALTH, Mth.clamp(health, 0.0F, DEFAULT_HEALTH)); // or a per-instance max if you ever vary it
    }
    public Optional<Vec3> getContactPos(int i) {
        List<UUID> contacts = this.entityData.get(CONTACTS);
        if (i < 0 || i >= contacts.size()) return Optional.empty();
        Entity e = this.level().getEntity(contacts.get(i));
        return (e != null && e.isAlive()) ? Optional.of(e.position()) : Optional.empty();
    }
    private void destroyRadar() {
        this.level().broadcastEntityEvent(this, (byte) 60); // reuse the same destruction-feedback event id pattern as Drone
        this.discard();
    }
    public abstract boolean canBeDetected(Entity entity);
    private void scan() {
        clearContents();
        float RANGE = getRange();
        AABB searchBox = AABB.ofSize(this.position(), RANGE * 2, RANGE * 2, RANGE * 2);
        for (Entity entity : level().getEntities(this, searchBox)) {
            if (entity.isAlive()
                    && entity.position().distanceToSqr(this.position()) <= RANGE * RANGE && this.canBeDetected(entity)) {
                addContact(entity);
            }
        }
    }


    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        this.entityData.set(HEALTH, valueInput.getFloatOr("health", DEFAULT_HEALTH));
        this.entityData.set(RADIUS, valueInput.getFloatOr("radius", DEFAULT_RADIUS));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {
        valueOutput.putFloat("health", this.entityData.get(HEALTH));
        valueOutput.putFloat("radius", this.entityData.get(RADIUS));
    }
    public boolean canOpenGUI(){return true;}
    // AbstractRadar or wherever right-click is handled
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide() && canOpenGUI()) {
            Minecraft.getInstance().setScreen(new RadarScreen(this));
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand);
    }

    @Override
    public boolean isPickable() {
        return true;
    }
}
