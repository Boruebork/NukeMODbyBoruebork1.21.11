package com.boruebork.nukemod.drone;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
    public void bind(ServerPlayer p) {      // runtime only, used on restore
        pilotId = p.getUUID();
        owner.getEntityData().set(data, pilotId.toString());
    }
    public void detach() {                  // runtime only: free the seat, keep the stored session
        pilotId = null;
        owner.getEntityData().set(data, "");
    }
    public void release() {                 // the real exit: also ends the stored session
        UUID pid = pilotId;
        detach();
        if (pid != null) DroneManager.getInstance().endSession(pid);
    }
}
