package com.boruebork.nukemod.entity.custom.client.uav;

import com.boruebork.nukemod.entity.NukeMODEntityDataSerializers;
import com.boruebork.nukemod.entity.custom.fpvdrones.DroneProjectile;
import com.boruebork.nukemod.network.packet.SetProjectileModePayload;
import com.boruebork.nukemod.uav.AbstractUAV;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractWeaponEquipedUAV extends AbstractUAV {

    public static final EntityDataAccessor<Integer> WEAPONS_MODE =
            SynchedEntityData.defineId(AbstractWeaponEquipedUAV.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<List<Boolean>> LOADED_BAYS =
            SynchedEntityData.defineId(AbstractWeaponEquipedUAV.class, NukeMODEntityDataSerializers.BOOL_LIST.get());

    public List<EntityType<?>> modes = new ArrayList<>();
    EntityType<?> thisEntity;
    protected List<BayEntry> flattenedBays = new ArrayList<>();

    public AbstractWeaponEquipedUAV(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.thisEntity = entityType;
        defineProjectileOffsets();
        List<Boolean> initialLoaded = new ArrayList<>(Collections.nCopies(flattenedBays.size(), true));
        this.entityData.set(LOADED_BAYS, initialLoaded);
    }

    /// define the `projectileBays` array
    protected abstract void defineProjectileOffsets();

    protected void addProjectileBay(EntityType<?> entity, Vec3 relativeOffsetFromCenter) {
        flattenedBays.add(new BayEntry(entity, relativeOffsetFromCenter.scale(thisEntity.getWidth())));
        if (!modes.contains(entity)) modes.add(entity);
    }

    /// method called when the number key is pressed, if `[1]` was pressed `i = 0`
    public void setMode(int i) {
        ClientPacketDistributor.sendToServer(new SetProjectileModePayload(i));
    }

    public List<BayEntry> getFlattenedBays() {
        return flattenedBays;
    }

    public void setServerMode(int mode) {
        System.err.println("Server Mode: " + mode);
        if (mode >= modes.size()) return;
        this.entityData.set(WEAPONS_MODE, mode);
    }

    public void tryFireWeapon() {
        int rt = getNextBay(this.modes.get(this.entityData.get(WEAPONS_MODE)));
        if (rt == -1) return;
        fireBay(rt);
    }

    public int getAmountOf(EntityType<?> type) {
        int cnt = 0;
        for (BayEntry el : this.flattenedBays) {
            if (el.type() == type) cnt++;
        }
        return cnt;
    }

    public int getAmountOfLoaded(EntityType<?> type) {
        int cnt = 0;
        int i = 0;
        for (BayEntry el : this.flattenedBays) {
            if (el.type() == type && this.entityData.get(LOADED_BAYS).get(i)) cnt++;
            i++;
        }
        return cnt;
    }

    protected record BayEntry(EntityType<?> type, Vec3 offset) {}

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LOADED_BAYS, new ArrayList<>());
        builder.define(WEAPONS_MODE, 0);
    }

    public boolean isBayLoaded(int index) {
        return this.entityData.get(LOADED_BAYS).get(index);
    }

    protected int getNextBay(EntityType<?> type) {
        for (int i = 0; i < flattenedBays.size(); ++i) {
            if (flattenedBays.get(i).type() == type) {
                if (this.entityData.get(LOADED_BAYS).get(i)) return i;
            }
        }
        return -1;
    }

    // server-side: call when firing/dropping from a specific bay
    protected void setBayLoaded(int index, boolean loaded) {
        List<Boolean> current = new ArrayList<>(this.entityData.get(LOADED_BAYS));
        current.set(index, loaded);
        this.entityData.set(LOADED_BAYS, current);
    }

    protected void fireProjectile(EntityType<?> type) {
        fireBay(getNextBay(type));
    }

    protected void fireBay(int index) {
        if (level().isClientSide() || !isBayLoaded(index)) return;
        BayEntry bay = flattenedBays.get(index);
        EntityType<?> projType = bay.type();

        Vec3 rotatedOffset = bay.offset()
                .yRot(-this.getYRot() * ((float) Math.PI / 180F));

        Vec3 worldPos = this.position().add(rotatedOffset);

        Entity projectile = projType.create(level(), EntitySpawnReason.TRIGGERED);
        assert projectile != null;
        if (projectile instanceof DroneProjectile dp) {
            dp.setStartingSpeed(this.getDeltaMovement(), this.getXRot(), this.getYRot(), this);
        }
        projectile.setPos(worldPos.x, worldPos.y, worldPos.z);
        level().addFreshEntity(projectile);
        setBayLoaded(index, false);
    }
}