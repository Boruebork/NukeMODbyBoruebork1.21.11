package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.NukeModbyBorueborkClient;
import com.boruebork.nukemod.network.packet.DroneLaucnhProjectilePayload;
import com.boruebork.nukemod.network.packet.ExitDronePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
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
        if (NukeModbyBorueborkClient.DRONE_ATTACK_MAPPING.get().consumeClick()){
            if (ClientDroneManager.PilotingClientState.drone != null){
                ClientPacketDistributor.sendToServer(new DroneLaucnhProjectilePayload());
            }
        }
    }
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        var player = Minecraft.getInstance().player;
        if (ClientDroneManager.PilotingClientState.drone == null) return;
        if (player == null) { ClientDroneManager.PilotingClientState.drone = null; return; }

        ClientInput input = event.getInput();
        ClientDroneManager.PilotingClientState.x = input.getMoveVector().x;
        ClientDroneManager.PilotingClientState.z = input.getMoveVector().y;
        ClientDroneManager.PilotingClientState.up = Minecraft.getInstance().options.keyJump.isDown();
        ClientDroneManager.PilotingClientState.down = Minecraft.getInstance().options.keyShift.isDown();
        player.setDeltaMovement(Vec3.ZERO);
        // same onClientTickPost as above
        //if (player.isCrouching()) player.setCro(false);
        if (player.getPose() != Pose.STANDING) player.setPose(Pose.STANDING);
        // actually stop the player entity itself from moving — this was commented out before
        // if MovementInputUpdateEvent supports cancellation in your version
    }
}
