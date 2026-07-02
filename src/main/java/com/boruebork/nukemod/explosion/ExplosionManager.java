package com.boruebork.nukemod.explosion;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

@EventBusSubscriber(modid = NukeModbyBoruebork.MODID)
public class ExplosionManager {
    private Map<UUID, NuclearExplosion> explosions = new HashMap<>();
    private static ExplosionManager INSTANCE;
    public static ExplosionManager getInstance(){
        return INSTANCE;
    }
    public void tick(){
        long start = System.nanoTime();
        while (
                ExpandingExplosion.currentShellGened <= ExpandingExplosion.MAX_RADIUS &&
                        System.nanoTime() - start < 2_000_000 // 2 ms
        ) {
            ExpandingExplosion.generateShells();
        }
        for (NuclearExplosion explosion: explosions.values()){
            explosion.tick();
        }
        //System.err.println("Server: " + explosions.size());
    }

    public static NuclearExplosion addExplosion(ServerLevel level, Vec3i pos){
        NuclearExplosion tmp = new NuclearExplosion(level, pos);
        INSTANCE.explosions.put(tmp.id, tmp);
        return INSTANCE.explosions.get(tmp.id);
    }
    public static void removeExplosion(NuclearExplosion explosion){
        try {
            INSTANCE.explosions.remove(explosion.id);
        }catch (NullPointerException e){
            System.err.println("UUUh got an NPE while trying to remove explosion???");
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event){
        INSTANCE = new ExplosionManager();
    }
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event){
        if (INSTANCE == null) return;
        INSTANCE.tick();
    }

}
