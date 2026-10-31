package com.boruebork.nukemod.drone;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

// client package
@EventBusSubscriber(value = Dist.CLIENT)
public class ClientPilotHooks {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> PilotHooks.client = ClientPilotHooks::tick);
    }
    static void tick(AbstractPilotedEntity d) {
        var S = ClientDroneManager.PilotingClientState.class; // use the static fields directly
        boolean local = ClientDroneManager.PilotingClientState.drone == d;
        if (!local && d.pilotSeat().isOccupied()
                && ClientDroneManager.PilotingClientState.drone == null
                && ClientDroneManager.checkIfPilot(d)) {
            // ...your mounting block, with `d` instead of `this`...
            local = true;
        }
        if (local && d.isClientAuthoritative()) d.onClientPrediction();   // make it public
        else d.tickAsObserver();
    }
}