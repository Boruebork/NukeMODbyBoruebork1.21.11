package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.Drone;
import com.boruebork.nukemod.network.packet.DroneInputPayload;
import com.boruebork.nukemod.network.packet.ExitDronePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@EventBusSubscriber
public class DroneManager {
    private static DroneManager INSTANCE;

    public static DroneManager getInstance() {
        return INSTANCE;
    }
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event){
        INSTANCE = new DroneManager();
    }
    @SubscribeEvent
    public static void onPlayersDeath(LivingDeathEvent event){
        if (event.getEntity() instanceof Player player){
            if (getInstance().playerToDrone.containsKey(player.getUUID())){
                Entity ent = player.level().getEntity(getInstance().playerToDrone.get(player.getUUID()));
                if (ent instanceof Drone drone){
                    drone.stopOperating();
                }
            }
        }
    }

    public Map<UUID, UUID> playerToDrone = new HashMap<>();
    public static void updateDronePos(DroneInputPayload droneInputPayload, IPayloadContext context) {
        Entity ent = NukeModbyBoruebork.server.getLevel(Level.OVERWORLD).getEntity(droneInputPayload.droneId());
        if (ent != null)
        {
            if (ent instanceof Drone drone){
                drone.updatePosRot(droneInputPayload);
            }
        }
    }
    public void addEntry(Player player, Drone drone){
        playerToDrone.put(player.getUUID(), drone.getUUID());
    }
    public static void exitDrone(ExitDronePacket exitDronePacket, IPayloadContext context) {
        UUID droneId = DroneManager.getInstance().playerToDrone.get(context.player().getUUID());
        Entity ent = context.player().level().getEntity(droneId);
        if (ent == null) return;
        if (ent instanceof Drone drone){
            drone.stopOperating();
        }
    }
}
