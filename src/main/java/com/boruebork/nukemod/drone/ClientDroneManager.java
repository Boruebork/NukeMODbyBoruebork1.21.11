package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVDrone;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVProjectileLaunchingDrone;
import com.boruebork.nukemod.network.packet.*;
import com.boruebork.nukemod.ooblib.AbstractUAV;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

import static com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVDrone.CONTROLLER_DATA;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientDroneManager {
    private static UUID pendingMount;
    private static int pendingTicks;
    private static boolean confirmed;
    private static int unconfirmedTicks;
    static void enter(AbstractDrone d) {
        PilotingClientState.drone = d;
        PilotingClientState.mode = PilotingClientState.PilotingMode.FIXED;
        PilotingClientState.cameraYaw   = PilotingClientState.cameraYawO   = d.getYRot();
        PilotingClientState.cameraPitch = PilotingClientState.cameraPitchO = d.getXRot();
        PilotingClientState.roll = PilotingClientState.rollO = d.roll;
        PilotingClientState.resetZoom();
        confirmed = false; unconfirmedTicks = 0;
        Minecraft.getInstance().setCameraEntity(d);
    }

    static void exit(String reason, boolean tellServer) {
        NukeModbyBoruebork.LOGGER.info("[pilot] client exit: {}", reason);
        if (tellServer && PilotingClientState.drone != null)
            ClientPacketDistributor.sendToServer(new ExitDronePacket());   // adjust to your record's constructor
        PilotingClientState.drone = null;
        PilotingClientState.patrolCenter = null;
        PilotingClientState.resetZoom();
        pendingMount = null;
        var mc = Minecraft.getInstance();
        if (mc.player != null) mc.setCameraEntity(mc.player);
    }
    public static boolean checkIfPilot(AbstractDrone drone) {
        if (drone.getEntityData().get(CONTROLLER_DATA).isEmpty() || drone.getRemovalReason() == Entity.RemovalReason.DISCARDED) {
            return false;
        }
        if (drone.getEntityData().get(CONTROLLER_DATA).equals(Minecraft.getInstance().player.getUUID().toString())) {
            return true;
        }
        return false;
    }
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (Minecraft.getInstance().isPaused()) return;
        var player = Minecraft.getInstance().player;
        if (PilotingClientState.drone == null) return;
        if (player == null) {
            exit( "player is null", true);
            return;
        }
        ClientInput input = event.getInput();
        PilotingClientState.x = input.getMoveVector().x;
        PilotingClientState.z = input.getMoveVector().y;
        PilotingClientState.up = Minecraft.getInstance().options.keyJump.isDown();
        PilotingClientState.down = Minecraft.getInstance().options.keyShift.isDown();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused()) return;
        if (PilotingClientState.drone == null) return;
        if (Minecraft.getInstance().player == null) return;

        if (PilotingClientState.drone instanceof AbstractFPVDrone fpvDrone) {
            // FPV path unchanged
            ClientPacketDistributor.sendToServer(new DroneInputPayload(
                    fpvDrone.getId(),
                    PilotingClientState.x,
                    PilotingClientState.z,
                    PilotingClientState.up,
                    PilotingClientState.down,
                    PilotingClientState.cameraPitch,
                    PilotingClientState.cameraYaw
            ));
        } else if (PilotingClientState.drone instanceof AbstractUAV uav) {
            // Compute sticks from camera error (FIXED) or send zeros (FREE/PATROL)
            /*float stickPitch = 0f, stickYaw = 0f, stickRoll = 0f;

            if (PilotingClientState.mode == PilotingClientState.PilotingMode.FIXED) {
                float yawErr   = Mth.wrapDegrees(PilotingClientState.cameraYaw - uav.getYRot());
                // Trim: drone needs ~5° AoA to fly level. Camera level = drone 5° nose up.
                final float LEVEL_FLIGHT_AOA = 5.0f;
                float pitchErr = Mth.wrapDegrees(
                        PilotingClientState.cameraPitch - LEVEL_FLIGHT_AOA - uav.getXRot()
                );      stickYaw   = Mth.clamp(yawErr   / 30f, -1f, 1f);
                stickPitch = Mth.clamp(pitchErr / 30f, -1f, 1f);
                // bank into the turn: yaw error drives roll
                stickRoll  = Mth.clamp(PilotingClientState.x, -1f, 1f);
            }
            // FREE and PATROL send zero — drone holds heading, server drives patrol

            // throttle stick: keyboard forward/back
            float throttle = PilotingClientState.z;   // your existing mapping

            ClientPacketDistributor.sendToServer(new NewFixedWingPayload(
                    uav.getId(),
                    throttle,       // forward
                    stickPitch,
                    stickRoll,
                    stickYaw,
                    uav.getYRot(),
                    PilotingClientState.mode.getStreamCodec()// optional: last-known for server-side debug
            ));*/
            AbstractUAV.ClientSticks s = uav.computeClientSticks();
            ClientPacketDistributor.sendToServer(new NewFixedWingPayload(
                    uav.getId(),
                    s.throttle(),
                    s.pitch(),
                    s.roll(),
                    s.yaw(),
                    uav.getYRot(),
                    PilotingClientState.mode.getStreamCodec(),
                    uav.tickCount
                    ));

        }
    }
    public static class PilotingClientState{
        public static AbstractDrone drone;
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
        public static float cameraYaw;
        public static float cameraPitch;
        public static float cameraYawO;
        public static float cameraPitchO;
        public static PilotingMode mode = PilotingMode.FIXED;
        public static final float MIN_ZOOM = 1.0F;
        public static final float MAX_ZOOM = 10.0F;
        public static final float ZOOM_STEP = 1.15F;   // multiplier per scroll notch
        public static float zoom = 1.0F;
        public static float zoomO = 1.0F;
        public static float zoomTarget = 1.0F;
        public static BlockPos patrolCenter;
        public static boolean canZoom() {
            return drone instanceof AbstractUAV && mode != PilotingMode.FIXED;
        }

        public static void tickZoom() {
            if (!canZoom()) zoomTarget = 1.0F;
            zoomO = zoom;
            zoom += (zoomTarget - zoom) * 0.35F;
            if (Math.abs(zoomTarget - zoom) < 0.001F) zoom = zoomTarget;
        }

        public static void resetZoom() {
            zoom = zoomO = zoomTarget = 1.0F;
        }

        public static float getRenderZoom(float partialTick) {
            return Mth.lerp(partialTick, zoomO, zoom);
        }
        public static enum PilotingMode {
            FIXED(0),
            FREE(1),
            PATROL(2);
            int stream_codec = 0;
            PilotingMode(int stream_codec) {
                this.stream_codec = stream_codec;
            }
            public int getStreamCodec() {
                return stream_codec;
            }
        }

        public static void turn(double dyR, double dxR) {
            if (drone == null) return;
            if (drone instanceof AbstractFPVDrone) {
                float sens = 1.0F / Math.max(zoom, 1.0F);
                cameraYawO = cameraYaw;
                cameraPitchO = cameraPitch;
                float f  = (float) dxR * 0.15F * sens;
                float f1 = (float) dyR * 0.15F * sens;
                cameraPitch = Mth.clamp(cameraPitch + f, -90f, 90f);
                cameraYaw = cameraYaw + f1;
                // FPV path unchanged — it uses its own state, untouched by this refactor
                drone.updateLookClientSide(
                        Mth.clamp(f + drone.getXRot(), -90f, 90f),
                        drone.getYRot() + f1
                );
            } else if (drone instanceof AbstractUAV) {
                float sens = 1.0F / Math.max(zoom, 1.0F);
                cameraYawO = cameraYaw;
                cameraPitchO = cameraPitch;
                float f  = (float) dxR * 0.15F * sens;
                float f1 = (float) dyR * 0.15F * sens;

                if (mode == PilotingMode.FIXED) {
                    // Camera IS the wanted direction. Move it freely.
                    // No more setYawWanted / setPitchWanted — those methods are gone.
                    cameraPitch = Mth.clamp(cameraPitch + f, -90f, 90f);
                    cameraYaw = cameraYaw + f1;
                } else {
                    // FREE / PATROL: camera decoupled, drone does its own thing
                    cameraPitch = Mth.clamp(cameraPitch + f, -90f, 90f);
                    cameraYaw = cameraYaw + f1;
                }
            }
        }
        public static float getRenderPitch(float partialTick) {
            if (drone == null) return 0;
            return Mth.lerp(partialTick, xRotO, xRot);
        }
        public static float getRenderRoll(float partialTick) {
            if (drone == null) return 0;
            return Mth.lerp(partialTick, rollO, roll);
        }
        public static float getRenderYaw(float partialTick) {
            if (drone == null) return 0;
            return Mth.rotLerp(partialTick, yRotO, yRot);
        }
        public static void applyWantedRotation() {
            if (drone instanceof AbstractUAV uav) {
            float yawStep = Mth.clamp(
                    Mth.wrapDegrees(yW - yRot),
                    -uav.getYawRatePerTick(),
                    uav.getYawRatePerTick()
            );
            yRotO = yRot;
            yRot = yRot + yawStep;

            float pitchStep = Mth.clamp(
                    Mth.wrapDegrees(xW - xRot),
                    -uav.getPitchRatePerTick(),
                    uav.getPitchRatePerTick()
            );
            xRotO = xRot;
            xRot = Mth.clamp(xRot + pitchStep, -90f, 90f);

            float rollStep = Mth.clamp(
                    Mth.wrapDegrees(rollW - roll),
                    -uav.getRollStep(),
                    uav.getRollStep()
            );

            rollO = roll;
            roll += rollStep;
            }
        }

        public static boolean isPiloting() {
            return drone != null;
        }
    }
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (Minecraft.getInstance().isPaused()) return;
        if (!PilotingClientState.isPiloting()) return;

        if (PilotingClientState.drone instanceof AbstractUAV) {
            // Camera is fully client-local. Just use camera state.
            event.setYaw(PilotingClientState.cameraYaw);
            event.setPitch(PilotingClientState.cameraPitch);
            // Don't set roll — camera doesn't roll on UAV
        } else {
            // FPV path: camera is drone's rotation, unchanged
            // (this used PilotingClientState.yRot/xRot/roll which we just deleted)
            // Use the drone's own synced rotation instead:
            event.setYaw(PilotingClientState.drone.getYRot());
            event.setPitch(PilotingClientState.drone.getXRot());
            //System.out.printf("Ca yaw:" + reusedState.yRot + "pitch:" + reusedState.xRot);
            // roll: use drone's synced roll field if you have one
        }
    }
    public static PilotingClientState.PilotingMode next(PilotingClientState.PilotingMode mode) {
        if (mode == PilotingClientState.PilotingMode.FIXED) {
            return PilotingClientState.PilotingMode.FREE;
        } else if (mode == PilotingClientState.PilotingMode.FREE) {
            return PilotingClientState.PilotingMode.PATROL;
        } else {
            return PilotingClientState.PilotingMode.FIXED;
        }
    }
    static boolean prevShiftDown = false;
    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        if (Minecraft.getInstance().level == null) {
            exit("Client Level is null", false);
        }
        ///  if the player goes to another dimension, end piloting!
        if (PilotingClientState.drone != null) {
            if (PilotingClientState.drone.level() != Minecraft.getInstance().level) {
                exit("Drone is another dimension", true);
            }
        }
        if (pendingMount != null && PilotingClientState.drone == null) {
            Entity e = findEntity(pendingMount);               // your existing lookup from tryRegisterFromServer
            if (e instanceof AbstractDrone d) { enter(d); pendingMount = null; }
            else if (++pendingTicks > 100) pendingMount = null;
        }
        if (PilotingClientState.drone == null) { PilotingClientState.resetZoom(); return; }

        AbstractDrone d = PilotingClientState.drone;
        if (checkIfPilot(d)) { confirmed = true; unconfirmedTicks = 0; }
        else if (confirmed || ++unconfirmedTicks > 60 || d.isRemoved()) { exit("drone is removed", true); return; }
        if (PilotingClientState.drone == null) {
            PilotingClientState.resetZoom();          // new
            return;
        }
        PilotingClientState.tickZoom();
        if (Minecraft.getInstance().level == null) {
            exit("Client level is null", false);
            return;
        }
        if (PilotingClientState.drone == null) return;
        if (PilotingClientState.drone.getEntityData().get(CONTROLLER_DATA).isEmpty() || PilotingClientState.drone.getRemovalReason() == Entity.RemovalReason.DISCARDED){
            exit("The drones synced data doesn't contain the controller anymore", true);
            Minecraft.getInstance().setCameraEntity(Minecraft.getInstance().player);
        }
        if (Minecraft.getInstance().options.keyShift.isDown() && !prevShiftDown) {
            PilotingClientState.mode = next(PilotingClientState.mode);
        }
        if (PilotingClientState.mode != PilotingClientState.PilotingMode.PATROL) {
            PilotingClientState.patrolCenter = null;
        }
        prevShiftDown = Minecraft.getInstance().options.keyShift.isDown();
        handlePatrolClick();
        PilotingClientState.movementYRot = PilotingClientState.yRot;
    }
    public static void onDroneClientTick(AbstractDrone d) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (PilotingClientState.drone == null && checkIfPilot(d)) enter(d);
    }
    private static Entity findEntity(UUID pendingMount) {
        Level lvl = Minecraft.getInstance().level;
        if (lvl == null) return null;
        return lvl.getEntity(pendingMount);
    }

    static boolean wasAttackDown = false;
    public static void handlePatrolClick() {
        if (ClientDroneManager.PilotingClientState.drone == null) return;
        if (ClientDroneManager.PilotingClientState.mode != ClientDroneManager.PilotingClientState.PilotingMode.PATROL) return;
        boolean attackDown = Minecraft.getInstance().options.keyAttack.isDown();
        if (attackDown && !wasAttackDown) {
            net.minecraft.world.phys.HitResult hit = raycastFromCamera(1000.0);
            if (hit instanceof EntityHitResult ehr) {
                ClientPacketDistributor.sendToServer(new SetPatrolTargetPayload(
                        ClientDroneManager.PilotingClientState.drone.getId(), 1, Vec3.ZERO, ehr.getEntity().getUUID()));
            } else if (hit instanceof BlockHitResult bhr) {
                BlockPos p = bhr.getBlockPos();
                PilotingClientState.patrolCenter = p;
                ClientPacketDistributor.sendToServer(new SetPatrolTargetPayload(
                        ClientDroneManager.PilotingClientState.drone.getId(), 0, new Vec3(p.getX(), p.getY(), p.getZ()), UUID.randomUUID()));
            }
        }
        wasAttackDown = attackDown;
    }
    private static HitResult raycastFromCamera(double distance) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 start = camera.position();
        Vector3fc fwd = camera.forwardVector();
        Vec3 end = start.add(fwd.x() * distance, fwd.y() * distance, fwd.z() * distance);

        // Entities first, so aiming at a mob/player doesn't get swallowed by terrain behind it.
        AABB search = new AABB(start, end).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                mc.level,
                PilotingClientState.drone,
                start,
                end,
                search,
                e -> e.isPickable() && e != PilotingClientState.drone
                ,1
        );
        if (entityHit != null) return entityHit;

        // Fall back to block hit.
        return mc.level.clip(new ClipContext(
                start, end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                PilotingClientState.drone
        ));
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
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().isPaused()) return;
        if (Minecraft.getInstance().screen != null) return;
        if (!PilotingClientState.canZoom()) return;

        double delta = event.getScrollDeltaY();
        if (delta == 0) return;

        PilotingClientState.zoomTarget = Mth.clamp(
                PilotingClientState.zoomTarget * (float) Math.pow(PilotingClientState.ZOOM_STEP, delta),
                PilotingClientState.MIN_ZOOM,
                PilotingClientState.MAX_ZOOM
        );
        event.setCanceled(true); // stops the hotbar from scrolling
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!PilotingClientState.isPiloting()) return;
        float z = PilotingClientState.getRenderZoom((float) event.getPartialTick());
        if (z <= 1.001F) return;

        // tan-based scaling makes the image magnify by exactly z at the screen centre
        double half = Math.toRadians(event.getFOV()) * 0.5;
        event.setFOV((float) Math.toDegrees(2.0 * Math.atan(Math.tan(half) / z)));
    }
    public static void handleReconcile(ReconcilePayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(Minecraft.getInstance().level.getEntity(p.entityId()) instanceof AbstractUAV uav)) return;
            uav.reconcileFromServer(p);
        });
    }
    @SubscribeEvent
    public static void onClone(ClientPlayerNetworkEvent.Clone e) { exit("player cloned", false); }
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { exit("logout", false); }
}