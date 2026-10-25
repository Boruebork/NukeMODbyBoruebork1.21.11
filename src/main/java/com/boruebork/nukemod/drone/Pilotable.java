package com.boruebork.nukemod.drone;

import net.minecraft.world.entity.Entity;

public interface Pilotable {
    PilotSeat pilotSeat();
    Entity asEntity();
    default boolean isBeingPiloted() { return pilotSeat().isOccupied(); }
}
