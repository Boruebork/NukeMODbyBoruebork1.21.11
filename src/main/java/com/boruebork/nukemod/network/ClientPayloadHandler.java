package com.boruebork.nukemod.network;

import com.boruebork.nukemod.block.entity.GuidedMissileLauncherBE;
import com.boruebork.nukemod.explosion.ClientExplosionManager;
import com.boruebork.nukemod.network.packet.DiscardNuclearExplPacket;
import com.boruebork.nukemod.network.packet.NuclearExplosionUpdateClientPacket;
import com.boruebork.nukemod.network.packet.SetTargetForClientBE;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handleFlash(NuclearExplosionUpdateClientPacket data, IPayloadContext context) {
        ClientExplosionManager.getInstance().addClientExplosion(data);
    }

    public static void discardNuclearExplosion(DiscardNuclearExplPacket discardNuclearExplPacket, IPayloadContext context) {
        ClientExplosionManager.getInstance().discardExplosion(discardNuclearExplPacket);
    }

    public static void handleSetTargeForBE(SetTargetForClientBE packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            Level level = player.level();
            BlockEntity be = level.getBlockEntity(packet.pos());

            if (be instanceof GuidedMissileLauncherBE launcher) {
                launcher.setClientTarget(packet.id());
            }
        });
    }
}
