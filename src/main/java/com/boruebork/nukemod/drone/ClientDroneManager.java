package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.entity.custom.AbstractFPVDrone;
import com.boruebork.nukemod.entity.custom.AbstractFPVProjectileLaunchingDrone;
import com.boruebork.nukemod.network.packet.DroneInputPayload;
import com.boruebork.nukemod.network.packet.NotifyClientDroneExit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lwjgl.glfw.GLFW;

import static com.boruebork.nukemod.entity.custom.AbstractFPVDrone.CONTROLLER_DATA;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientDroneManager {
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        // your own client-side tracking
        var player = Minecraft.getInstance().player;
        if (PilotingClientState.drone == null) return;
        if (player == null) {
            PilotingClientState.drone = null; // stop piloting, nothing to control it with
            return;
        }
        ClientInput input = event.getInput();
        // stash what the player pressed for the drone, then cancel their own movement
        PilotingClientState.x = input.getMoveVector().x;
        PilotingClientState.z = input.getMoveVector().y;
        PilotingClientState.up = Minecraft.getInstance().options.keyJump.isDown();
        PilotingClientState.down = Minecraft.getInstance().options.keyShift.isDown();

        //input.forwardImpulse = 0;
        //input.leftImpulse = 0;
    }
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event){
        if (PilotingClientState.drone == null)  return;
        if (Minecraft.getInstance().player == null) return;
        ClientPacketDistributor.sendToServer(new DroneInputPayload(
                PilotingClientState.drone.getId(),
                PilotingClientState.x,
                PilotingClientState.z,
                PilotingClientState.up,
                PilotingClientState.down,
                PilotingClientState.xRot,
                PilotingClientState.yRot

        ));
    }

    public static void exitDrone(NotifyClientDroneExit notifyClientDroneExit, IPayloadContext context) {
        PilotingClientState.drone = null;
        Minecraft.getInstance().setCameraEntity(Minecraft.getInstance().player);
    }

    public static class PilotingClientState{
        public static AbstractFPVDrone drone;
        public static float x;
        public static float z;
        public static boolean up;
        public static boolean down;
        public static float xRot;
        public static float xRotO;
        public static float yRot;
        public static float yRotO;
        public static float movementYRot;
        public static int currentPayloadMode = 0;

        public static void turn(double yR, double xR) {
            float f = (float)xR * 0.15F;
            float f1 = (float)yR * 0.15F;
            xRot =xRot + f;
            yRot =yRot + f1;
            xRot = Mth.clamp(xRot, -90.0F, 90.0F);
            xRotO += f;
            yRotO += f1;
            xRotO = Mth.clamp(xRotO, -90.0F, 90.0F);
        }
        public static boolean isPiloting() {
            return drone != null;
        }
        public void updateLookClientSide(double mouseYaw, double mousePitch) {

            yRotO = yRot;
            xRotO = xRot;
            yRot = (float) mouseYaw;
            xRot = (float) mousePitch;
        }
    }
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!ClientDroneManager.PilotingClientState.isPiloting())
            return;

        event.setYaw(PilotingClientState.yRot);
        event.setPitch(PilotingClientState.xRot);
        PilotingClientState.drone.updateLookClientSide(PilotingClientState.xRot, PilotingClientState.yRot);

    }
    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {

        if (Minecraft.getInstance().level == null) {
            PilotingClientState.drone = null;
            return;
        }
        if (PilotingClientState.drone == null) return;
        if (PilotingClientState.drone.getEntityData().get(CONTROLLER_DATA).isEmpty() || PilotingClientState.drone.getRemovalReason() == Entity.RemovalReason.DISCARDED){
            PilotingClientState.drone = null;
            Minecraft.getInstance().setCameraEntity(Minecraft.getInstance().player);
        }
        PilotingClientState.movementYRot = PilotingClientState.yRot;
    }
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (PilotingClientState.drone == null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        int key = event.getKey();
        System.err.println(1);
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            System.err.println(2);
            int number = key - GLFW.GLFW_KEY_1 + 1;
            if (PilotingClientState.drone instanceof AbstractFPVProjectileLaunchingDrone pDrone){
                System.err.println(3);
                pDrone.setMode(number - 1);
            }
        }
    }

}
