package com.boruebork.nukemod.network;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.drone.DroneManager;
import com.boruebork.nukemod.network.packet.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = NukeModbyBoruebork.MODID)
public class ServerPacketRegistry {
    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event){
        var registrar = event.registrar("1");
        registrar.playToServer(
                LaunchPacket.TYPE,
                LaunchPacket.STREAM_CODEC,
                ServerPayloadHandler::handleLaunch
        );
        registrar.playToServer(
                LaunchGuidedPacket.TYPE,
                LaunchGuidedPacket.STREAM_CODEC,
                ServerPayloadHandler::handleGuidedlaunch
        );
        registrar.playToClient(
                NuclearExplosionUpdateClientPacket.TYPE,
                NuclearExplosionUpdateClientPacket.STREAM_CODEC
        );
        registrar.playToClient(
                DiscardNuclearExplPacket.TYPE,
                DiscardNuclearExplPacket.STREAM_CODEC
        );
        registrar.playToServer(
                TargetSelectedPacket.TYPE,
                TargetSelectedPacket.STREAM_CODEC,
                ServerPayloadHandler::handleTargetSelected
        );
        registrar.playToClient(
                SetTargetForClientBE.TYPE,
                SetTargetForClientBE.STREAM_CODEC
        );
        registrar.playToServer(
                DroneInputPayload.TYPE,
                DroneInputPayload.STREAM_CODEC,
                DroneManager::updateDronePos
        );

        registrar.playToServer(
                ExitDronePacket.TYPE,
                ExitDronePacket.STREAM_CODEC,
                DroneManager::exitDrone
        );
        registrar.playToClient(
                NotifyClientDroneExit.TYPE,
                NotifyClientDroneExit.STREAM_CODEC
        );
        registrar.playToServer(
                SetProjectileModePayload.TYPE,
                SetProjectileModePayload.STREAM_CODEC,
                DroneManager::setWeaponsMode
        );
        registrar.playToServer(
                DroneLaucnhProjectilePayload.TYPE,
                DroneLaucnhProjectilePayload.STREAM_CODEC,
                DroneManager::launchDroneProjectile
        );
    }
}
