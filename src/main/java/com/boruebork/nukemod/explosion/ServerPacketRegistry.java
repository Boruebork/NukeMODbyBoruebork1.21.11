package com.boruebork.nukemod.explosion;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.explosion.client.packet.FlashPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
@EventBusSubscriber(modid = NukeModbyBoruebork.MODID)
public class ServerPacketRegistry {
    @SubscribeEvent // on the mod event bus
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                FlashPacket.TYPE,
                FlashPacket.STREAM_CODEC
        );
    }
}
