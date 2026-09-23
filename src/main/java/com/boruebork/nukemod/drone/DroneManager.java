package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.AbstractFPVDrone;
import com.boruebork.nukemod.entity.custom.AbstractFPVProjectileLaunchingDrone;
import com.boruebork.nukemod.network.packet.DroneInputPayload;
import com.boruebork.nukemod.network.packet.DroneLaucnhProjectilePayload;
import com.boruebork.nukemod.network.packet.ExitDronePacket;
import com.boruebork.nukemod.network.packet.SetProjectileModePayload;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.*;

@EventBusSubscriber
public class DroneManager {
    private static DroneManager INSTANCE;
    private DroneChunkStreamer chunkStreamer;
    public static DroneManager getInstance() {
        return INSTANCE;
    }
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event){
        INSTANCE = new DroneManager();
        INSTANCE.chunkStreamer = new DroneChunkStreamer();
    }
    @SubscribeEvent
    public static void onPlayersDeath(LivingDeathEvent event){
        if (event.getEntity() instanceof Player player){
            if (getInstance().playerToDrone.containsKey(player.getUUID())){
                Entity ent = player.level().getEntity(getInstance().playerToDrone.get(player.getUUID()));
                if (ent instanceof AbstractFPVDrone drone){
                    drone.stopOperating();
                }
            }
        }
    }

    public Map<UUID, UUID> playerToDrone = new HashMap<>();
    public Map<AbstractFPVDrone, UUID> drones = new HashMap<>();
    public static void updateDronePos(DroneInputPayload droneInputPayload, IPayloadContext context) {
        Entity ent = NukeModbyBoruebork.server.getLevel(Level.OVERWORLD).getEntity(droneInputPayload.droneId());
        if (ent != null)
        {
            if (ent instanceof AbstractFPVDrone drone){
                drone.updatePosRot(droneInputPayload);
            }
        }
    }
    public void addEntry(Player player, AbstractFPVDrone drone){
        playerToDrone.put(player.getUUID(), drone.getUUID());
        drones.put(drone, drone.getUUID());
    }
    public static void exitDrone(ExitDronePacket $, IPayloadContext context) {
        UUID droneId = DroneManager.getInstance().playerToDrone.get(context.player().getUUID());
        Entity ent = context.player().level().getEntity(droneId);
        if (ent == null) return;
        if (ent instanceof AbstractFPVDrone drone){
            drone.stopOperating();
            getInstance().chunkStreamer.onExit((ServerPlayer) context.player());
        }
    }

    public static void setWeaponsMode(SetProjectileModePayload payload, IPayloadContext context) {
        UUID id =  getInstance().playerToDrone.get(context.player().getUUID());
        Entity ent = context.player().level().getEntity(id);
        if (ent instanceof AbstractFPVProjectileLaunchingDrone drone){
            drone.setServerMode(payload.mode());
        }
    }

    public static void launchDroneProjectile(DroneLaucnhProjectilePayload $, IPayloadContext context) {
        UUID id =  getInstance().playerToDrone.get(context.player().getUUID());
        Entity ent = context.player().level().getEntity(id);
        if (ent instanceof AbstractFPVProjectileLaunchingDrone drone){
            drone.tryFireWeapon();
        }
    }
    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Pre event) {
        for (AbstractFPVDrone drone : getInstance().drones.keySet()){
            getInstance().updateChunkTicket(drone);
        }
    }
    private final Map<AbstractFPVDrone, ChunkPos> lastTicketPositions = new HashMap<>();

    private void updateChunkTicket(AbstractFPVDrone drone) {
        if (!drone.isBeingPiloted()) {
            releaseTicket(drone);
            return;
        }

        ChunkPos currentPos = new ChunkPos(drone.blockPosition());
        ChunkPos lastPos = lastTicketPositions.get(drone);
        if (currentPos.equals(lastPos)) return;

        ServerChunkCache chunkSource = ((ServerLevel) drone.level()).getChunkSource();
        int radius = ((ServerLevel) drone.level()).getServer().getPlayerList().getSimulationDistance();
        if (lastPos != null) {
            chunkSource.removeTicketWithRadius(ModTickets.DRONE_LOADING.get(), lastPos, radius);
            chunkSource.removeTicketWithRadius(ModTickets.DRONE_SIMULATION.get(), lastPos, radius);
        }
        chunkSource.addTicketWithRadius(ModTickets.DRONE_LOADING.get(), currentPos, radius);
        chunkSource.addTicketWithRadius(ModTickets.DRONE_SIMULATION.get(), currentPos, radius);
        lastTicketPositions.put(drone, currentPos);
    }

    private void releaseTicket(AbstractFPVDrone drone) {
        ChunkPos lastPos = lastTicketPositions.remove(drone);
        if (lastPos != null && drone.level() instanceof ServerLevel serverLevel) {
            int radius = serverLevel.getServer().getPlayerList().getSimulationDistance();
            ServerChunkCache chunkSource = serverLevel.getChunkSource();
            chunkSource.removeTicketWithRadius(ModTickets.DRONE_LOADING.get(), lastPos, radius);
            chunkSource.removeTicketWithRadius(ModTickets.DRONE_SIMULATION.get(), lastPos, radius);
        }
    }

    public AbstractFPVDrone getPilotedDrone(ServerPlayer player) {
        return (AbstractFPVDrone) player.level().getEntity(playerToDrone.get(player.getUUID()));
    }
}
