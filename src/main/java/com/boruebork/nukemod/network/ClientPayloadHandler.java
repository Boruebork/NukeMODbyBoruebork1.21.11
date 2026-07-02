package com.boruebork.nukemod.network;

import com.boruebork.nukemod.explosion.ClientExplosionManager;
import com.boruebork.nukemod.network.packet.DiscardNuclearExplPacket;
import com.boruebork.nukemod.network.packet.NuclearExplosionUpdateClientPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handleFlash(NuclearExplosionUpdateClientPacket data, IPayloadContext context) {
        ClientExplosionManager.getInstance().addClientExplosion(data);
    }

    public static void discardNuclearExplosion(DiscardNuclearExplPacket discardNuclearExplPacket, IPayloadContext context) {
        ClientExplosionManager.getInstance().discardExplosion(discardNuclearExplPacket);
    }
}
