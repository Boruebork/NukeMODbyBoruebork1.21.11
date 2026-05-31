package com.boruebork.nukemod.explosion.client;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.explosion.client.packet.FlashPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
@EventBusSubscriber(value = Dist.CLIENT, modid = NukeModbyBoruebork.MODID)
public class ClientPacketRegistry {
    @SubscribeEvent // on the mod event bus only on the physical client
    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(
                FlashPacket.TYPE,
                FlashHandler::startFlash
        );
    }
}
