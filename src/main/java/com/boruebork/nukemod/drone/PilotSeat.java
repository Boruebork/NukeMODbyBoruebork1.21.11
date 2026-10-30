package com.boruebork.nukemod.drone;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class PilotSeat {
    private final Entity owner;
    private final EntityDataAccessor<String> data;   // each base class defines its own accessor
    private UUID pilotId;

    public PilotSeat(Entity owner, EntityDataAccessor<String> data) {
        this.owner = owner; this.data = data;
    }

    public void define(SynchedEntityData.Builder b) { b.define(data, ""); }

    public boolean isOccupied() {
        System.err.println(owner.getEntityData().get(data));
        return !owner.getEntityData().get(data).isEmpty();
    }
    // PilotSeat
    public void release() {
        if (pilotId == null) return;
        UUID pid = pilotId;
        pilotId = null;
        owner.getEntityData().set(data, "");
        DroneManager.getInstance().onSeatReleased(pid, owner.getUUID());
    }
    public @Nullable UUID pilotId() { return pilotId; }
    /** Server. */
    public boolean tryEnter(ServerPlayer p) {
        System.err.println(1);
        if (isOccupied()) return false;
        System.err.println(2);
        pilotId = p.getUUID();
        owner.getEntityData().set(data, pilotId.toString());
        DroneManager.getInstance().register(p, (Pilotable) owner);
        return true;
    }
    public boolean isPilot(Player p) {                 // works on both sides
        return owner.getEntityData().get(data).equals(p.getUUID().toString());
    }

    public void save(ValueOutput out) { out.putString("Controller", pilotId == null ? "" : pilotId.toString()); }

    public void load(ValueInput in) {
        String s = in.getStringOr("Controller", "");
        try { pilotId = s.isEmpty() ? null : UUID.fromString(s); }
        catch (IllegalArgumentException e) { pilotId = null; }
        owner.getEntityData().set(data, pilotId == null ? "" : pilotId.toString());
    }

    /** Call once the entity is in a server level, so a reloaded pilot gets re-registered. */
    public void onAddedToLevel() {
        if (pilotId != null && owner.level() instanceof ServerLevel sl) {
            Player p = sl.getPlayerByUUID(pilotId);
            if (p instanceof ServerPlayer sp) DroneManager.getInstance().register(sp, (Pilotable) owner);
            else release();       // pilot is offline: free the seat
        }
    }
}
