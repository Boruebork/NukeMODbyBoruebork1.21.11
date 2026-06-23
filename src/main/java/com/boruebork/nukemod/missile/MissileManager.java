package com.boruebork.nukemod.missile;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.ModEntities;
import com.boruebork.nukemod.entity.custom.GuidedMissile;
import com.boruebork.nukemod.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
@EventBusSubscriber(modid = NukeModbyBoruebork.MODID)
public class MissileManager {
    public static MissileManager INSTANCE;
    public static MissileManager getInstance(){
        return INSTANCE;
    }
    public MinecraftServer server;
    public Set<MissileData> missiles = new HashSet<>();
    public Set<Entity> physicalMissiles = new HashSet<>();
    private Set<MissileData> missileToRemove = new HashSet<>();
    private Set<Entity> physicalMissilesToRemove = new HashSet<>();
    public static void turnToData(GuidedMissile missile){
        INSTANCE.missiles.add(new GuidedMissileData(missile));
    }
    /**this method is use to spawn any custom missile, because it feeds the new missiles directly into NukeMODs off-Chunk handling system***/
    public static void spawnMissile(GuidedMissile missile){
        INSTANCE.physicalMissiles.add(missile);
        INSTANCE.server.getLevel(Level.OVERWORLD).addFreshEntity(missile);
    }
    public MissileManager(MinecraftServer server) {
        this.server = server;
    }
    /**this method is use to convert an entity into a simple RAM object before it enters and uloaded chunk**/
    public static void convertToData(GuidedMissile missile){
        INSTANCE.missiles.add(new GuidedMissileData(missile));
        INSTANCE.physicalMissiles.remove(missile);
        System.err.println("Physical missiles: " + INSTANCE.physicalMissiles.size());
        System.err.println("Data missiles: " + INSTANCE.missiles.size());
        System.err.println("converted missile to data");

    }
    public static void queueRemoval(MissileData data){
        INSTANCE.missileToRemove.add(data);
    }
    public static void convertToEntity(GuidedMissileData data){
        GuidedMissile missile = new GuidedMissile(ModEntities.GUIDED_MISILE.get(), INSTANCE.server.getLevel(Level.OVERWORLD));
        missile.setSpeed(data.speed);
        missile.setDirection(data.direction);
        missile.setPos(data.position);
        missile.setCurrentState(data.missileState);
        missile.setState(data.lockingState);
        missile.setLastTargetPosition(data.lastTargetPosition);
        missile.setActive(true);
        missile.setTarget(Objects.requireNonNull(INSTANCE.server.getPlayerList().getPlayer(data.targetUUID)));
        INSTANCE.physicalMissiles.add(missile);
        INSTANCE.missiles.remove(data);

        INSTANCE.server.getLevel(Level.OVERWORLD).addFreshEntity(missile);
        System.err.println("converted missile to entity");
    }
    public void tick(){
        for (MissileData missile : this.missiles){
            missile.tick(this.server);
        }
        for (Entity e : this.physicalMissiles){
            if (e instanceof GuidedMissile guidedMissile){
                if (!((ServerLevel) server.getLevel(Level.OVERWORLD)).isPositionEntityTicking(BlockPos.containing(guidedMissile.position()))) {
                    MissileManager.convertToData(guidedMissile);
                    guidedMissile.discard();
                    this.physicalMissilesToRemove.add(guidedMissile);
                    System.err.println("badabim badabum!");
                    return;
                }
            }
        }
        for (MissileData data : this.missileToRemove){
            convertToEntity((GuidedMissileData) data);
        }
        if (!missileToRemove.isEmpty()) {
            this.missileToRemove.clear();
            System.err.println("Physical missiles: " + physicalMissiles.size());
            System.err.println("Data missiles: " + missiles.size());
        }
        for (Entity e : this.physicalMissilesToRemove){
            if (e instanceof GuidedMissile guidedMissile){
                convertToData(guidedMissile);
            }
        }
        if (!physicalMissilesToRemove.isEmpty()){
            this.physicalMissilesToRemove.clear();
            System.err.println("Physical missiles: " + physicalMissiles.size());
            System.err.println("Data missiles: " + missiles.size());
        }
    }
    public static void init(MinecraftServer server){
        INSTANCE = new MissileManager(server);
    }
    @SubscribeEvent
    private static void preTick(ServerTickEvent.Pre event){
        INSTANCE.tick();
    }
}