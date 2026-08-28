package com.boruebork.nukemod.missile;

import com.boruebork.nukemod.entity.ModEntities;
import com.boruebork.nukemod.entity.custom.GuidedMissile;
import com.boruebork.nukemod.missile.GuidedMissileData;
import com.boruebork.nukemod.missile.MissileData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class MissileManager {

    public static MissileManager INSTANCE;

    public final MinecraftServer server;

    // ONE SOURCE OF TRUTH
    private Map<UUID, MissileData> dataMissiles = new HashMap<>();
    private final Map<UUID, GuidedMissile> physicalMissiles = new HashMap<>();

    private final Set<UUID> converting = new HashSet<>();

    public MissileManager(MinecraftServer server) {
        this.server = server;
        INSTANCE = this;
    }

    // -------------------------
    // SPAWN NEW MISSILE
    // -------------------------
    public static void spawnMissile(GuidedMissile missile) {
        INSTANCE.physicalMissiles.put(missile.getUUID(), missile);
        INSTANCE.server.getLevel(Level.OVERWORLD).addFreshEntity(missile);
    }

    // -------------------------
    // CONVERT PHYSICAL -> DATA
    // -------------------------
    public static void convertToData(GuidedMissile missile) {
        UUID id = missile.getUUID();

        if (INSTANCE.converting.contains(id)) return;
        INSTANCE.converting.add(id);

        INSTANCE.dataMissiles.put(id, new GuidedMissileData(missile));
        INSTANCE.physicalMissiles.remove(id);

        missile.discard();

        INSTANCE.converting.remove(id);

        System.err.println("converted to data");
    }

    // -------------------------
    // CONVERT DATA -> ENTITY
    // -------------------------
    public static void convertToEntity(GuidedMissileData data) {
        UUID id = data.id;

        if (INSTANCE.converting.contains(id)) return;
        INSTANCE.converting.add(id);

        ServerLevel level = INSTANCE.server.getLevel(Level.OVERWORLD);

        GuidedMissile missile = new GuidedMissile(ModEntities.GUIDED_MISILE.get(), level);

        missile.setPos(data.position);
        missile.setSpeed(data.speed);
        missile.setDirection(data.direction);
        missile.setCurrentState(data.missileState);
        missile.setState(data.lockingState);
        missile.setLastTargetPosition(data.lastTargetPosition);

        missile.setActive(true);

        LivingEntity target = INSTANCE.server.getPlayerList().getPlayer(data.targetUUID);
        if (target != null) {
            missile.setTarget(target); // IMPORTANT: not setTarget()
        }
        INSTANCE.physicalMissiles.put(missile.getUUID(), missile);
        INSTANCE.dataMissiles.remove(id);

        assert level != null;
        level.addFreshEntity(missile);

        INSTANCE.converting.remove(id);
        System.err.println("Convert to ent");
    }

    // -------------------------
    // TICK
    // -------------------------
    public void tick() {
        //System.out.println("Physical: " + INSTANCE.physicalMissiles.size());
        //System.out.println("Data: " + INSTANCE.dataMissiles.size());
        // tick data missiles
        for (MissileData data : dataMissiles.values()) {
            data.tick(server);
        }

        // physical missiles check unload
        for (GuidedMissile missile : physicalMissiles.values()) {
            ServerLevel level = server.getLevel(Level.OVERWORLD);

            boolean ticking = level.isPositionEntityTicking(missile.blockPosition());
            if (!ticking) {
                System.out.println("CONVERTING!");
                convertToData(missile);
            }
        }
    }

    // -------------------------
    // SAFE EXTERNAL CALLS
    // -------------------------
    public static void queuePhysicalRemoval(Entity entity) {
        if (entity instanceof GuidedMissile missile)
            INSTANCE.physicalMissiles.remove(missile.getUUID());
    }

    public static void init(MinecraftServer server) {
        INSTANCE = new MissileManager(server);
        List<GuidedMissile> temp = server.getLevel(Level.OVERWORLD).getEntitiesOfClass(GuidedMissile.class, AABB.INFINITE);
        for (GuidedMissile missile :  temp){
            INSTANCE.physicalMissiles.put(missile.getUUID(), missile);
        }

    }

    public Map<UUID, MissileData> getDataMissiles() {
        return this.dataMissiles;
    }

    public void setDataMissiles(Map<UUID, MissileData> uuidMissileDataMap) {
        this.dataMissiles = uuidMissileDataMap;
    }
}