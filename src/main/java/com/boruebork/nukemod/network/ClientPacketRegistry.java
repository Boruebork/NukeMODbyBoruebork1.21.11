package com.boruebork.nukemod.network;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.network.packet.DiscardNuclearExplPacket;
import com.boruebork.nukemod.network.packet.NuclearExplosionUpdateClientPacket;
import com.boruebork.nukemod.network.packet.SetTargetForClientBE;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
@EventBusSubscriber(modid = NukeModbyBoruebork.MODID, value = Dist.CLIENT)
public class ClientPacketRegistry {
    @SubscribeEvent // on the mod event bus only on the physical client
    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(
                NuclearExplosionUpdateClientPacket.TYPE,
                ClientPayloadHandler::handleFlash
        );
        event.register(
                DiscardNuclearExplPacket.TYPE,
                ClientPayloadHandler::discardNuclearExplosion
        );
        event.register(
                SetTargetForClientBE.TYPE,
                ClientPayloadHandler::handleSetTargeForBE
        );
    }
}
