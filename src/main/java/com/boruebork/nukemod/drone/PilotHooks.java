package com.boruebork.nukemod.drone;

// common
public final class PilotHooks {
    public interface ClientHook { void tick(AbstractPilotedEntity d); }
    public static volatile ClientHook client = ClientDroneManager::onDroneClientTick;
}
