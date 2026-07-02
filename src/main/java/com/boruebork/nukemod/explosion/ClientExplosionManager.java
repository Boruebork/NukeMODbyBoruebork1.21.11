package com.boruebork.nukemod.explosion;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.network.packet.DiscardNuclearExplPacket;
import com.boruebork.nukemod.network.packet.NuclearExplosionUpdateClientPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.*;

@EventBusSubscriber(modid = NukeModbyBoruebork.MODID, value = Dist.CLIENT)
public class ClientExplosionManager {
    private static ClientExplosionManager INSTANCE;
    public static ClientExplosionManager getInstance(){
        return INSTANCE;
    }

    public Map<UUID, NuclearExplosion> clientExplosions = new HashMap<>();
    public void clear(){
        this.clientExplosions.clear();
    }
    public void discardExplosion(DiscardNuclearExplPacket data) {
        if (this.clientExplosions.containsKey(data.id())){
            this.clientExplosions.remove(data.id());
        }
    }

    public void tick(){
        for (NuclearExplosion explosion : clientExplosions.values()){
            explosion.tick();
        }
        //System.err.println("Client: " + clientExplosions.size());
    }
    public void addClientExplosion(NuclearExplosionUpdateClientPacket data) {

        System.err.println("Packet ID: " + data.id());

        NuclearExplosion explosion = clientExplosions.get(data.id());

        if (explosion != null) {
            System.err.println("Found explosion with ID: " + explosion.id);
            explosion.update(data);
            return;
        }

        System.err.println("Creating explosion " + data.id());

        clientExplosions.put(
                data.id(),
                new NuclearExplosion(
                        data.id(),
                        Minecraft.getInstance().level,
                        data.pos(),
                        data.phase(),
                        data.ticksSinceStartOfPhase()
                )
        );
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
    @SubscribeEvent
    public static void onPlayerLeaving(ClientPlayerNetworkEvent.LoggingOut event){
        ClientExplosionManager.getInstance().clear();
    }
}
