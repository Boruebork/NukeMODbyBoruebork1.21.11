package com.boruebork.nukemod.network;

import com.boruebork.nukemod.network.packet.NuclearExplosionStartedPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

public class ClientPacketRegistry {
    @SubscribeEvent // on the mod event bus only on the physical client
    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(
                NuclearExplosionStartedPacket.TYPE,
                ClientPayloadHandler::handleFlash
        );
    }
}
