package com.boruebork.nukemod.network;

import com.boruebork.nukemod.Config;
import com.boruebork.nukemod.entity.ModEntities;
import com.boruebork.nukemod.entity.custom.NukeEntity;
import com.boruebork.nukemod.network.packet.LaunchPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {
    public static void handleLaunch(LaunchPacket launchPacket, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer)  context.player();
        if (!player.level().isClientSide()){
        NukeEntity entity = new NukeEntity(ModEntities.NUKE.get(), player.level());
        entity.setTargetX(launchPacket.x());
        entity.setTargetY(launchPacket.y());
        entity.setTargetZ(launchPacket.z());
        entity.setPos(new Vec3(player.getX(), player.getY(), player.getZ()));
        entity.setRotation(NukeEntity.calculateBallisticRotation(new Vec3(entity.getX(), entity.getY(), entity.getZ()), new Vec3(entity.getTargetX(), entity.getTargetY(), entity.getTargetZ()), entity.speed, Config.GRAVITY));
        entity.senderName = context.player().getName().getString();
        context.player().level().addFreshEntity(entity);
        ((ServerPlayer) context.player()).sendSystemMessage(Component.literal("Rocket Launch successful, you launched a Nuclear Warhead!"));
        System.out.println("Spawned Entity");}
    }
}
