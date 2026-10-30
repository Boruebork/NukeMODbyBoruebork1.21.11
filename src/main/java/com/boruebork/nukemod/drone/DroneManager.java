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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@EventBusSubscriber
public class DroneManager {
    private static DroneManager INSTANCE;
    private DroneChunkStreamer chunkStreamer;
    public DroneManager() {
        INSTANCE = this;

        this.chunkStreamer = new DroneChunkStreamer();
    }
    private void generateData() {
        List<PilotSessions.Session> sessions = PilotSessions.getInstance().getSessions();
        for (PilotSessions.Session s : sessions) {
            this.playerToVehicle.put(s.player(), s.vehicle());
            Player pl = NukeModbyBoruebork.server.getPlayerList().getPlayer(s.player());
            Entity pilotable = pl.level().getEntity(s.vehicle());
            if (pilotable instanceof AbstractDrone d) {
                this.pilotedDrones.add(d);
            }
        }
    }
    public static DroneManager getInstance() {
        if (INSTANCE == null) {
            new DroneManager();
        }
        return INSTANCE;
    }
    @SubscribeEvent
    public static void onPlayersDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            Pilotable v = getInstance().getPiloted(p);
            if (v instanceof AbstractDrone d) d.stopOperating();
        }
    }
    public Set<AbstractDrone> pilotedDrones = new HashSet<>();
    public static void updateDronePos(DroneInputPayload droneInputPayload, IPayloadContext context) {
        Pilotable pilotable = getInstance().getPiloted((ServerPlayer) context.player());
        if (pilotable == null) return;
        Entity ent = pilotable.asEntity();
        if (ent != null)
        {
            if (ent instanceof AbstractFPVDrone drone){
                drone.updatePosRot(droneInputPayload);
            }
        }
    }
    public void register(Player player, Pilotable vehicle){
        playerToVehicle.remove(player.getUUID());
        playerToVehicle.put(player.getUUID(), vehicle.asEntity().getUUID());
        if (vehicle instanceof AbstractDrone drone) pilotedDrones.add(drone);
    }
    public static void exitDrone(ExitDronePacket $, IPayloadContext context) {
        Pilotable ent = DroneManager.getInstance().getPiloted((ServerPlayer) context.player());
        if (ent == null) return;
        if (ent instanceof AbstractDrone drone){
            drone.stopOperating();
            //getInstance().chunkStreamer.onExit((ServerPlayer) context.player());
        }
    }

    public static void setWeaponsMode(SetProjectileModePayload payload, IPayloadContext context) {
        Pilotable ent = DroneManager.getInstance().getPiloted((ServerPlayer) context.player());
        if (ent instanceof AbstractFPVProjectileLaunchingDrone drone){
            drone.setServerMode(payload.mode());
        }
    }

    public static void launchDroneProjectile(DroneLaucnhProjectilePayload $, IPayloadContext context) {
        Pilotable ent = DroneManager.getInstance().getPiloted((ServerPlayer) context.player());
        if (ent instanceof AbstractFPVProjectileLaunchingDrone drone){
            drone.tryFireWeapon();
        }
    }
    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Pre event) {
        DroneManager dm = getInstance();
        for (var it = dm.pilotedDrones.iterator(); it.hasNext();) {
            AbstractDrone d = it.next();
            UUID pid = d.pilotSeat().pilotId();
            ServerPlayer p = pid == null ? null : NukeModbyBoruebork.server.getPlayerList().getPlayer(pid);

            boolean invalid = d.isRemoved() || pid == null || p == null
                    || !p.isAlive() || p.hasDisconnected() || p.level() != d.level();   // dimension change lands here
            if (invalid) {
                d.stopOperating();            // -> release() -> notifies client
                dm.releaseTicket(d);
                it.remove();
            } else {
                dm.updateChunkTicket(d);
            }
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

    public static void onInput(NewFixedWingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Pilotable d = DroneManager.getInstance().getPiloted((ServerPlayer) context.player());
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
        Pilotable d = DroneManager.getInstance().getPiloted((ServerPlayer) context.player());
        if (d == null) return;
        if (d instanceof AbstractUAV uav) {
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
        if (e == null) return null;                          // not found now: keep the entry
        if (e instanceof Pilotable v && v.pilotSeat().isPilot(p)) return v;
        playerToVehicle.remove(p.getUUID());                 // entity exists but isn't ours
        return null;
    }
    public void unregister(UUID playerId) { playerToVehicle.remove(playerId); }
    @SubscribeEvent
    public static void onDimChange(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        for (AbstractDrone d : List.copyOf(getInstance().pilotedDrones))
            if (d.pilotSeat().isPilot(p)) d.stopOperating();
        getInstance().unregister(p.getUUID());
    }
    @SubscribeEvent
    public static void onDimChange(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        for (AbstractDrone d : List.copyOf(getInstance().pilotedDrones))
            if (d.pilotSeat().isPilot(p)) d.stopOperating();
        getInstance().unregister(p.getUUID());
    }
    @SubscribeEvent
    public static void onDimChange(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        for (AbstractDrone d : List.copyOf(getInstance().pilotedDrones))
            if (d.pilotSeat().isPilot(p)) d.stopOperating();
        getInstance().unregister(p.getUUID());
    }
    public void onSeatReleased(UUID playerId, UUID vehicleId) {
        // two-arg remove: don't wipe a newer session that was registered for another drone
        boolean wasCurrent = playerToVehicle.remove(playerId, vehicleId);
        ServerPlayer p = NukeModbyBoruebork.server.getPlayerList().getPlayer(playerId);
        if (p == null) return;
        //if (wasCurrent) chunkStreamer.onExit(p);
    }
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event){
        List<PilotSessions.Session> sessions = new ArrayList<>();
        for (UUID pl : getInstance().playerToVehicle.keySet()) {
            Player player = event.getServer().getPlayerList().getPlayer(pl);
            sessions.add(new PilotSessions.Session(pl, getInstance().playerToVehicle.get(pl), player.level().dimension(), getInstance().getPiloted((ServerPlayer) player).asEntity().blockPosition()));
        }
        PilotSessions.getInstance().setSessions(sessions);
    }
}
