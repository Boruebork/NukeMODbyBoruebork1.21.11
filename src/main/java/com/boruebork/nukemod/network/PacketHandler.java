package com.boruebork.nukemod.network;

import com.boruebork.nukemod.network.packet.LaunchPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber
public class PacketHandler {
    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event){
        var registrar = event.registrar("1");
        registrar.playToServer(
                LaunchPacket.TYPE,
                LaunchPacket.STREAM_CODEC,
                ServerPayloadHandler::handleLaunch
        );
    }
}
