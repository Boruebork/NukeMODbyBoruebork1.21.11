package com.boruebork.nukemod.network;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.NukeModbyBorueborkClient;
import com.boruebork.nukemod.explosion.ClientExplosionManager;
import com.boruebork.nukemod.explosion.NuclearExplosion;
import com.boruebork.nukemod.network.packet.NuclearExplosionStartedPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handleFlash(NuclearExplosionStartedPacket data, IPayloadContext context) {
        ClientExplosionManager.getInstance().clientExplosions.add(
                new NuclearExplosion(
                        data.id(),
                        Minecraft.getInstance().level,
                        data.pos(),
                        data.phase(),
                        data.ticksSinceStartOfPhase()
                )
        );
    }
}
