package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVDrone;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVProjectileLaunchingDrone;
import com.boruebork.nukemod.network.packet.DroneInputPayload;
import com.boruebork.nukemod.network.packet.FixedWingInputPayload;
import com.boruebork.nukemod.network.packet.NotifyClientDroneExit;
import com.boruebork.nukemod.ooblib.AbstractUAV;
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
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import static com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVDrone.CONTROLLER_DATA;
import static com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVDrone.MAX_ROLL_DEGREES;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientDroneManager {
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (Minecraft.getInstance().isPaused()) return;
        var player = Minecraft.getInstance().player;
        if (PilotingClientState.drone == null) return;
        if (player == null) {
            PilotingClientState.drone = null;
            return;
        }
        ClientInput input = event.getInput();
        PilotingClientState.x = input.getMoveVector().x;
        PilotingClientState.z = input.getMoveVector().y;
        PilotingClientState.up = Minecraft.getInstance().options.keyJump.isDown();
        PilotingClientState.down = Minecraft.getInstance().options.keyShift.isDown();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event){
        if (Minecraft.getInstance().isPaused()) return;
        if (PilotingClientState.drone == null)  return;
        if (Minecraft.getInstance().player == null) return;
        if (PilotingClientState.drone instanceof AbstractFPVDrone fpvDrone){
        ClientPacketDistributor.sendToServer(new DroneInputPayload(
                PilotingClientState.drone.getId(),
                PilotingClientState.x,
                PilotingClientState.z,
                PilotingClientState.up,
                PilotingClientState.down,
                PilotingClientState.xRot,
                PilotingClientState.yRot

        ));
        }else if (PilotingClientState.drone instanceof AbstractUAV uav) {
            System.out.println("PCS xRot: " + PilotingClientState.xRot + " yRot: " + PilotingClientState.yRot);
            ClientPacketDistributor.sendToServer(new FixedWingInputPayload(
                    uav.getId(),
                    PilotingClientState.x,
                    PilotingClientState.z,
                    PilotingClientState.up,
                    PilotingClientState.down,
                    PilotingClientState.xW,
                    PilotingClientState.yW,
                    uav.getXRot(),
                    uav.getYRot(),
                    uav.getRoll(),
                    uav.getRollWanted()
            ));
        }
    }

    public static void exitDrone(NotifyClientDroneExit notifyClientDroneExit, IPayloadContext context) {
        PilotingClientState.drone = null;
        Minecraft.getInstance().setCameraEntity(Minecraft.getInstance().player);
    }

    public static class PilotingClientState{
        public static com.boruebork.nukemod.entity.custom.fpvdrones.AbstractDrone drone;
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
        public static float roll;
        public static float rollO;
        public static float rollW;
        public static float yW;
        public static float xW;

        public static void turn(double dyR, double dxR) {
            if (drone == null) return;
            if (drone instanceof AbstractFPVDrone) {
                float f = (float)dxR * 0.15F;
                float f1 = (float)dyR * 0.15F;
                xRot =xRot + f;
                yRot =yRot + f1;
                xRot = Mth.clamp(xRot, -90.0F, 90.0F);
                xRotO += f;
                yRotO += f1;
                xRotO = Mth.clamp(xRotO, -90.0F, 90.0F);
            }else if (drone instanceof AbstractUAV uav) {
                float f = (float)dxR * 0.15F;
                float f1 = (float)dyR * 0.15F;
                xW = xW + f;
                yW = yW + f1;
                xW = Mth.clamp(xW, -90.0F, 90.0F);
                uav.setYawWanted(yW);
                uav.setPitchWanted(xW);

            }
        }
        public static boolean isPiloting() {
            return drone != null;
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (Minecraft.getInstance().isPaused()) return;
        if (!PilotingClientState.isPiloting())
            return;
        if (PilotingClientState.drone instanceof AbstractUAV uav) {
            float partialTick = (float) event.getPartialTick();
            event.setYaw(PilotingClientState.yW);
            event.setPitch(PilotingClientState.xW);
            //event.setRoll(uav.getRenderRoll(partialTick));
            /*uav.updateLookClientSide(
                    PilotingClientState.xRot,
                    PilotingClientState.yRot
            );*/
        } else {
            event.setYaw(PilotingClientState.yRot);
            event.setPitch(PilotingClientState.xRot);
            event.setRoll(
                    (float) Mth.lerp(
                            event.getPartialTick(),
                            PilotingClientState.rollO,
                            PilotingClientState.roll
                    )
            );

            PilotingClientState.drone.updateLookClientSide(
                    PilotingClientState.xRot,
                    PilotingClientState.yRot
            );
        }
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
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            int number = key - GLFW.GLFW_KEY_1 + 1;
            if (PilotingClientState.drone instanceof AbstractFPVProjectileLaunchingDrone pDrone){
                pDrone.setMode(number - 1);
            }
        }
    }

}