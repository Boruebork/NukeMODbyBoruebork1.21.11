package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.NukeModbyBorueborkClient;
import com.boruebork.nukemod.network.packet.ExitDronePacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(value = Dist.CLIENT)
public class DroneInputSuppressor {
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (ClientDroneManager.PilotingClientState.drone != null) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (ClientDroneManager.PilotingClientState.drone != null) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void exitDrone(ClientTickEvent.Pre event){
        if (Minecraft.getInstance().player == null) return;
        if (NukeModbyBorueborkClient.EXIT_DRONE_KEY.get().consumeClick()){
            if (ClientDroneManager.PilotingClientState.drone != null){
                ClientPacketDistributor.sendToServer(new ExitDronePacket());
            }
        }
    }
}
