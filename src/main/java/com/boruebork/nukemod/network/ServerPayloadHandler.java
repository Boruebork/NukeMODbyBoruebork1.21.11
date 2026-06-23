package com.boruebork.nukemod.network;

import com.boruebork.nukemod.Config;
import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.block.entity.GuidedMissileLauncherBE;
import com.boruebork.nukemod.entity.ModEntities;
import com.boruebork.nukemod.entity.custom.GuidedMissile;
import com.boruebork.nukemod.entity.custom.NukeEntity;
import com.boruebork.nukemod.gui.menu.GuidedMissileLauncherMenu;
import com.boruebork.nukemod.item.ModItems;
import com.boruebork.nukemod.missile.MissileManager;
import com.boruebork.nukemod.network.packet.LaunchGuidedPacket;
import com.boruebork.nukemod.network.packet.LaunchPacket;
import com.boruebork.nukemod.util.Util;
import com.boruebork.nukemod.util.VehiclesToItemsConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.AbstractCollection;
import java.util.Objects;

public class ServerPayloadHandler {
    public static void handleLaunch(LaunchPacket launchPacket, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (!player.level().isClientSide()) {
            if (context.player().getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.NUCLEAR_WARHEAD) || context.player().getItemInHand(InteractionHand.OFF_HAND).is(ModItems.NUCLEAR_WARHEAD)) {
                EquipmentSlot equipmentSlot;
                int count;
                if (context.player().getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.NUCLEAR_WARHEAD)){
                    equipmentSlot = EquipmentSlot.MAINHAND;
                    count = context.player().getMainHandItem().getCount();
                }else{
                    equipmentSlot = EquipmentSlot.OFFHAND;
                    count = context.player().getOffhandItem().getCount();
                }
                NukeEntity entity = new NukeEntity(ModEntities.NUKE.get(), player.level());
                entity.setTargetX(launchPacket.x());
                entity.setTargetY(launchPacket.y());
                entity.setTargetZ(launchPacket.z());
                entity.setPos(new Vec3(player.getX(), player.getY(), player.getZ()));
                entity.setRotation(NukeEntity.calculateBallisticRotation(new Vec3(entity.getX(), entity.getY(), entity.getZ()), new Vec3(entity.getTargetX(), entity.getTargetY(), entity.getTargetZ()), entity.speed, Config.GRAVITY));
                entity.senderName = context.player().getName().getString();
                context.player().level().addFreshEntity(entity);
                context.player().setItemSlot(equipmentSlot, new ItemStack(ModItems.NUCLEAR_WARHEAD.get(), count));
                ((ServerPlayer) context.player()).sendSystemMessage(Component.literal("Rocket Launch successful, you launched a Nuclear Warhead!"));
                System.out.println("Spawned Entity");
            }
        }

    }

    public static void handleGuidedlaunch(LaunchGuidedPacket launchPacket, IPayloadContext context) {
        AbstractContainerMenu menu = context.player().containerMenu;
        if (menu instanceof GuidedMissileLauncherMenu launchMenu){
            EntityType<?> entityType = (EntityType<?>) VehiclesToItemsConfig.getEntity(launchMenu.getSlot(36).getItem().getItem()).get();
            GuidedMissile toSpawn = (GuidedMissile) entityType.create(context.player().level(), EntitySpawnReason.TRIGGERED);
            assert toSpawn != null;
            if (context.player().containerMenu instanceof GuidedMissileLauncherMenu menu1)
                toSpawn.setPos(Util.BlockPosTooVec3(menu1.blockEntity.getBlockPos()));
            toSpawn.setTarget(Objects.requireNonNull(MissileManager.INSTANCE.server.getPlayerList().getPlayer(launchPacket.targetUUID())));
            MissileManager.spawnMissile(toSpawn);
            System.err.println("spawned missile!");
        }
    }
}
