package com.boruebork.nukemod.explosion;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.*;

@EventBusSubscriber(modid = NukeModbyBoruebork.MODID, value = Dist.CLIENT)
public class ClientExplosionManager {
    private static ClientExplosionManager INSTANCE;
    public static ClientExplosionManager getInstance(){
        return INSTANCE;
    }

    public Map<UUID, NuclearExplosion> clientExplosions = new HashMap<>();
    public void tick(){
        for (NuclearExplosion explosion : clientExplosions.values()){
            explosion.tick();
        }
    }
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event){
        INSTANCE = new ClientExplosionManager();
    }
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event){
        if (INSTANCE == null) return;
        INSTANCE.tick();
    }
}
