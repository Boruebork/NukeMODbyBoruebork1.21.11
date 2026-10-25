package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVDrone;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVProjectileLaunchingDrone;
import com.boruebork.nukemod.network.packet.*;
import com.boruebork.nukemod.ooblib.AbstractUAV;
import net.minecraft.core.BlockPos;
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
import org.jetbrains.annotations.Nullable;

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
                if (ent instanceof AbstractDrone drone){
                    drone.stopOperating();
                }
            }
        }
    }

    public Map<UUID, UUID> playerToDrone = new HashMap<>();
    public Map<AbstractDrone, UUID> drones = new HashMap<>();
    public static void updateDronePos(DroneInputPayload droneInputPayload, IPayloadContext context) {
        Entity ent = NukeModbyBoruebork.server.getLevel(Level.OVERWORLD).getEntity(droneInputPayload.droneId());
        if (ent != null)
        {
            if (ent instanceof AbstractFPVDrone drone){
                drone.updatePosRot(droneInputPayload);
            }
        }
    }
    public void addEntry(Player player, AbstractDrone drone){
        playerToDrone.put(player.getUUID(), drone.getUUID());
        drones.put(drone, drone.getUUID());
    }
    public static void exitDrone(ExitDronePacket $, IPayloadContext context) {
        UUID droneId = DroneManager.getInstance().playerToDrone.get(context.player().getUUID());
        Entity ent = context.player().level().getEntity(droneId);
        if (ent == null) return;
        if (ent instanceof AbstractDrone drone){
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
        for (AbstractDrone drone : getInstance().drones.keySet()){
            getInstance().updateChunkTicket(drone);
        }
    }
    private final Map<AbstractDrone, ChunkPos> lastTicketPositions = new HashMap<>();

    private void updateChunkTicket(AbstractDrone drone) {
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

    private void releaseTicket(AbstractDrone drone) {
        ChunkPos lastPos = lastTicketPositions.remove(drone);
        if (lastPos != null && drone.level() instanceof ServerLevel serverLevel) {
            int radius = serverLevel.getServer().getPlayerList().getSimulationDistance();
            ServerChunkCache chunkSource = serverLevel.getChunkSource();
            chunkSource.removeTicketWithRadius(ModTickets.DRONE_LOADING.get(), lastPos, radius);
            chunkSource.removeTicketWithRadius(ModTickets.DRONE_SIMULATION.get(), lastPos, radius);
        }
    }

    public AbstractDrone getPilotedDrone(ServerPlayer player) {
        return (AbstractDrone) player.level().getEntity(playerToDrone.get(player.getUUID()));
    }

    /*public static void onFixedWingInputUpdate(FixedWingInputPayload payload, IPayloadContext context) {
        Entity ent = NukeModbyBoruebork.server.getLevel(Level.OVERWORLD).getEntity(payload.droneId());
        if (ent == null) return;
        if (ent instanceof AbstractUAV uav) {
            uav.updateFixedWingRot(payload);
        }
    }*/

    public static void onInput(NewFixedWingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            AbstractDrone d = DroneManager.getInstance().getPilotedDrone((ServerPlayer) context.player());
            if (d instanceof AbstractUAV uav) {
                // payload must carry an int clientTick
                uav.setLastInputTick(payload.clientTick());
                uav.onInput(
                        payload.forward(),
                        payload.pitch(),
                        payload.roll(),
                        payload.yaw(),
                        payload.throttle(),
                        payload.mode()
                );
            }
        });
    }

    public static void onPatrolTargetSet(SetPatrolTargetPayload payload, IPayloadContext context) {
        if (context.player().level().getEntity(payload.droneId()) instanceof AbstractUAV uav) {
            if (payload.kind() == 0) uav.setPatrolBlockTarget(new BlockPos((int) payload.pos().x, (int) payload.pos().y, (int) payload.pos().z));
            else uav.setPatrolEntityTarget(((ServerLevel) context.player().level()).getEntity(payload.entityId()).getUUID());
        }
    }
    //===new
    private final Map<UUID, UUID> playerToVehicle = new HashMap<>();

    public @Nullable Pilotable getPiloted(ServerPlayer p) {
        UUID id = playerToVehicle.get(p.getUUID());
        if (id == null) return null;
        Entity e = ((ServerLevel) p.level()).getEntity(id);
        if (e instanceof Pilotable v && v.pilotSeat().isPilot(p)) return v;
        playerToVehicle.remove(p.getUUID());     // stale entry
        return null;
    }
}
