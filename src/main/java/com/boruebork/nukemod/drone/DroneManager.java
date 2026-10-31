package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVDrone;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVProjectileLaunchingDrone;
import com.boruebork.nukemod.network.packet.*;
import com.boruebork.nukemod.uav.AbstractUAV;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
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
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@EventBusSubscriber
public class DroneManager {
    private static DroneManager INSTANCE;
    public DroneManager() {
        INSTANCE = this;
    }
    public static DroneManager getInstance() {
        if (INSTANCE == null) {
            new DroneManager();
        }
        return INSTANCE;
    }
    private final Map<UUID, AbstractPilotedEntity> live = new HashMap<>();   // player -> bound drone (runtime only)
    private final Map<UUID, Pin> pins = new HashMap<>();
    private final Map<UUID, Integer> waiting = new HashMap<>();
    private record Pin(ResourceKey<Level> dim, ChunkPos chunk) {}

    public Collection<AbstractPilotedEntity> pilotedDrones() { return live.values(); }  // mixins: .pilotedDrones().iterator()

    public @Nullable Pilotable getPiloted(ServerPlayer p) {          // O(1), no level lookup
        AbstractPilotedEntity d = live.get(p.getUUID());
        return d != null && !d.isRemoved() && d.pilotSeat().isPilot(p) ? d : null;
    }

    /** Called from PilotSeat.tryEnter. */
    public void register(Player player, Pilotable v) {
        if (!(v instanceof AbstractPilotedEntity d) || !(player instanceof ServerPlayer p)) return;
        live.put(p.getUUID(), d);
        PilotSessions.get(NukeModbyBoruebork.server).put(new PilotSessions.Session(
                p.getUUID(), d.getUUID(), d.level().dimension(), d.blockPosition()));
    }

    public void endSession(UUID player) {
        live.remove(player); waiting.remove(player); unpin(player);
        if (NukeModbyBoruebork.server != null) PilotSessions.get(NukeModbyBoruebork.server).remove(player);
    }

    private void pin(UUID player, ResourceKey<Level> dim, ChunkPos c) {
        Pin old = pins.get(player);
        if (old != null && old.dim().equals(dim) && old.chunk().equals(c)) return;
        unpin(player);
        MinecraftServer server = NukeModbyBoruebork.server;
        ServerLevel lvl = server.getLevel(dim);
        if (lvl == null) return;
        int r = server.getPlayerList().getSimulationDistance();
        lvl.getChunkSource().addTicketWithRadius(ModTickets.DRONE_LOADING.get(), c, r);
        lvl.getChunkSource().addTicketWithRadius(ModTickets.DRONE_SIMULATION.get(), c, r);
        pins.put(player, new Pin(dim, c));
    }

    private void unpin(UUID player) {
        Pin p = pins.remove(player);
        if (p == null) return;
        MinecraftServer server = NukeModbyBoruebork.server;
        ServerLevel lvl = server.getLevel(p.dim());
        if (lvl == null) return;
        int r = server.getPlayerList().getSimulationDistance();
        lvl.getChunkSource().removeTicketWithRadius(ModTickets.DRONE_LOADING.get(), p.chunk(), r);
        lvl.getChunkSource().removeTicketWithRadius(ModTickets.DRONE_SIMULATION.get(), p.chunk(), r);
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Pre event) {
        MinecraftServer server = NukeModbyBoruebork.server;
        if (server == null) return;
        DroneManager dm = getInstance();
        PilotSessions store = PilotSessions.get(server);

        for (PilotSessions.Session s : store.all()) {
            UUID pid = s.player();
            ServerPlayer p = server.getPlayerList().getPlayer(pid);
            if (p == null) { dm.unpin(pid); continue; }               // offline: session kept, nothing loaded

            ServerLevel lvl = server.getLevel(s.dim());
            Entity e = lvl == null ? null : lvl.getEntity(s.vehicle());

            if (e instanceof AbstractPilotedEntity d && !d.isRemoved()) {
                dm.waiting.remove(pid);
                if (!p.isAlive()) { d.stopOperating(); dm.endSession(pid); continue; }
                if (dm.live.get(pid) != d) {                           // first sight after login, restart or chunk reload
                    d.pilotSeat().bind(p);
                    dm.live.put(pid, d);
                }
                dm.pin(pid, s.dim(), d.chunkPosition());
                store.movedTo(pid, s.dim(), d.blockPosition());
            } else {
                dm.live.remove(pid);                                   // entity not loaded (yet)
                dm.pin(pid, s.dim(), new ChunkPos(s.pos()));           // load its last chunks so it can come back
                if (dm.waiting.merge(pid, 1, Integer::sum) > 600) dm.endSession(pid);  // gone for good
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        DroneManager dm = getInstance();
        AbstractPilotedEntity d = dm.live.remove(p.getUUID());
        if (d != null) d.pilotSeat().detach();                          // suspend: free the seat, KEEP the session
        dm.waiting.remove(p.getUUID());
        dm.unpin(p.getUUID());
    }

    @SubscribeEvent
    public static void onDimChange(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        AbstractPilotedEntity d = getInstance().live.get(p.getUUID());
        if (d != null) d.stopOperating();
        getInstance().endSession(p.getUUID());
    }

    @SubscribeEvent public static void onServerStarting(ServerStartingEvent e) { INSTANCE = new DroneManager(); }
    @SubscribeEvent public static void onServerStopped(ServerStoppedEvent e)   { INSTANCE = null; }
    @SubscribeEvent
    public static void onPlayersDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            Pilotable v = getInstance().getPiloted(p);
            if (v instanceof AbstractPilotedEntity d) d.stopOperating();
        }
    }
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
    public static void exitDrone(ExitDronePacket $, IPayloadContext context) {
        Pilotable ent = DroneManager.getInstance().getPiloted((ServerPlayer) context.player());
        if (ent == null) return;
        if (ent instanceof AbstractPilotedEntity drone){
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
}
