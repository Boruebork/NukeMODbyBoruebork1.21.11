package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.ModEntities;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractDrone;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVDrone;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVProjectileLaunchingDrone;
import com.boruebork.nukemod.ooblib.AbstractUAV;
import com.boruebork.nukemod.util.Colors;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.joml.Vector3f;

import java.util.Map;

@EventBusSubscriber(value = Dist.CLIENT)
public class DroneHudRegistration {

    // --- horizon line tuning ---
    private static final int HORIZON_GAP = 16;     // px from center to the start of each segment
    private static final int HORIZON_LENGTH = 22;  // px length of each segment

    // --- boresight marker tuning ---
    private static final float BORESIGHT_PROJECTION_DISTANCE = 50f; // arbitrary -- just needs to be far
    private static final int BORESIGHT_SIZE = 6; // px half-length of each crosshair arm

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "drone_hud"),
                DroneHudRegistration::renderDroneHud
        );
    }

    private static void renderDroneHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (ClientDroneManager.PilotingClientState.drone == null) return;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);

        AbstractDrone drone = ClientDroneManager.PilotingClientState.drone;
        if (drone instanceof AbstractUAV uav) {
            // Nose-direction marker: where the UAV's BODY is actually pointing, independent of
            // where the camera (mouse look) is currently aimed. Reuses the exact partialTick
            // this whole HUD layer already has -- no separate event/API needed for it.
            renderCenterSquare(graphics, centerX, centerY);
            renderBoresight(graphics, uav, partialTick, width, height);
            return;
        }
        // Interpolate the same way the camera roll itself is interpolated (see onCameraAngles) —
        // reading the raw un-interpolated roll here would reintroduce the 20Hz stair-step jitter
        // we fixed on the camera earlier, just on the HUD instead.
            float roll = Mth.lerp(partialTick, drone.getRollO(), drone.getRoll());

        renderHorizonLines(graphics, centerX, centerY, roll);



        // Example: crosshair-style reticle — stays screen-fixed, drawn OUTSIDE the rotated block above
        graphics.hLine(centerX - 10, centerX + 10, centerY, Colors.GREEN);
        graphics.vLine(centerX, centerY - 10, centerY + 10, Colors.GREEN);
        // Example: corner brackets, letterbox bars, telemetry text, etc.
        graphics.drawString(
                Minecraft.getInstance().font,
                "ALT: " + (int) ClientDroneManager.PilotingClientState.drone.getY(),
                10, height - 20, Colors.GREEN
        );
        if (ClientDroneManager.PilotingClientState.drone instanceof AbstractFPVDrone)
            graphics.drawString(
                    Minecraft.getInstance().font,
                    "HEALTH: " + ClientDroneManager.PilotingClientState.drone.getEntityData().get(AbstractFPVDrone.HEALTH_DATA),
                    width -100, height - 20, Colors.GREEN
            );
        if (ClientDroneManager.PilotingClientState.drone instanceof AbstractFPVProjectileLaunchingDrone pDrone){
            // add weapons preview
            int i = 0;
            EntityType<?> type = pDrone.modes.get(pDrone.getEntityData().get(AbstractFPVProjectileLaunchingDrone.WEAPONS_MODE));
            //graphics.drawString(Minecraft.getInstance().font, "Weapon: " + type + "[" + pDrone.getAmountOfLoaded(type) +"/" + pDrone.getAmountOf(type) + "]", 0, 0, Colors.GREEN);
            for (EntityType<?> type1 : pDrone.modes){
                graphics.drawString(Minecraft.getInstance().font, (type == type1 ? "> " : "") + "Weapon: " + type1 + "[" + pDrone.getAmountOfLoaded(type1) +"/" + pDrone.getAmountOf(type1) + "]", 0, i*10, Colors.GREEN);

                graphics.blit(RenderPipelines.GUI_TEXTURED, textures().get(type1), 40 + i*10, 50, 0, 0, 10,10,32,32);
                i++;
            }
        }
    }

    /**
     * Where the UAV's body is ACTUALLY pointing, projected onto screen space via the
     * camera's own basis vectors and FOV -- independent of where the camera is aimed.
     * Hidden entirely once the nose points behind the camera or off-screen; ask if you
     * want a clamped-to-edge arrow instead of just disappearing in that case.
     */
    private static void renderBoresight(GuiGraphics graphics, AbstractUAV uav, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();

        // Same interpolated values the body model itself renders with -- not a raw tick-end
        // value, or the marker will visibly lag/stutter relative to the visible aircraft.
        float yaw = uav.getRenderYaw(partialTick);
        float pitch = uav.getRenderPitch(partialTick);
        float yawRad = yaw * Mth.DEG_TO_RAD;
        float pitchRad = pitch * Mth.DEG_TO_RAD;

        Vector3f bodyForward = new Vector3f(
                -Mth.sin(yawRad) * Mth.cos(pitchRad),
                -Mth.sin(pitchRad),
                Mth.cos(yawRad) * Mth.cos(pitchRad)
        );

        Vector3f camPos = uav.getCameraPosition(partialTick).toVector3f();
        Vector3f targetWorld = new Vector3f(bodyForward).mul(BORESIGHT_PROJECTION_DISTANCE).add(camPos);
        Vector3f relative = new Vector3f(targetWorld).sub(camPos);

        Vector3f look = new Vector3f(camera.forwardVector());
        Vector3f up = new Vector3f(camera.upVector());
        Vector3f right = new Vector3f(camera.leftVector()).negate(); // left -> right

        float forwardComp = relative.dot(look);
        if (forwardComp <= 0.01f) return; // nose pointing behind the camera entirely

        float rightComp = relative.dot(right);
        float upComp = relative.dot(up);

        float fovDeg = mc.options.fov().get(); // base FOV; doesn't account for zoom/sprint FOV effects
        float fovRad = fovDeg * Mth.DEG_TO_RAD;
        float aspect = (float) width / height;

        float tanHalfFovY = (float) Math.tan(fovRad / 2f);
        float tanHalfFovX = tanHalfFovY * aspect;

        float ndcX = (rightComp / forwardComp) / tanHalfFovX;
        float ndcY = (upComp / forwardComp) / tanHalfFovY;

        if (Math.abs(ndcX) > 1f || Math.abs(ndcY) > 1f) return; // off-screen

        int px = Math.round((ndcX * 0.5f + 0.5f) * width);
        int py = Math.round((0.5f - ndcY * 0.5f) * height);

        graphics.hLine(px - BORESIGHT_SIZE, px + BORESIGHT_SIZE, py, Colors.GREEN);
        graphics.vLine(px, py - BORESIGHT_SIZE, py + BORESIGHT_SIZE, Colors.GREEN);
    }

    /**
     * The two side segments FPV OSDs draw to represent the true horizon. The crosshair stays
     * fixed to the screen; these rotate so the pilot can read bank angle at a glance even when
     * the real horizon in the footage is obscured.
     *
     * Sign note: whether this should be `roll` or `-roll` depends on which direction your
     * ViewportEvent#setRoll(...) call actually rotates the rendered scene on screen — that's
     * engine/version-specific and I can't verify it from here. Test it: roll right (D) and
     * watch these lines. If they visually tilt the WRONG way relative to the real in-game
     * horizon behind them, flip the sign below.
     */
    private static void renderHorizonLines(GuiGraphics graphics, int centerX, int centerY, float roll) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        // rotate() takes radians — roll is in degrees, hence the conversion. Without this,
        // easing from 0 deg to ~35 deg actually sweeps ~5.6 full turns, which is the "spinning" you saw.
        graphics.pose().rotate((float) Math.toRadians(-roll));

        graphics.hLine(- HORIZON_GAP - HORIZON_LENGTH, - HORIZON_GAP, 0, Colors.GREEN);
        graphics.hLine(HORIZON_GAP, HORIZON_GAP + HORIZON_LENGTH, 0, Colors.GREEN);

        graphics.pose().popMatrix();
    }

    private static Map<EntityType<?>, Identifier> texturesCache;

    private static Map<EntityType<?>, Identifier> textures() {
        if (texturesCache == null) {
            texturesCache = Map.of(
                    ModEntities.GRENADE.get(), Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "textures/item/grenade_item.png"),
                    ModEntities.ROCKET.get(), Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "textures/item/rocket_item.png")
            );
        }
        return texturesCache;
    }
    private static void renderCenterSquare(GuiGraphics graphics, int centerX, int centerY) {
        int size = 4;

        graphics.hLine(
                centerX - size,
                centerX + size,
                centerY - size,
                Colors.GREEN
        );

        graphics.hLine(
                centerX - size,
                centerX + size,
                centerY + size,
                Colors.GREEN
        );

        graphics.vLine(
                centerX - size,
                centerY - size,
                centerY + size,
                Colors.GREEN
        );

        graphics.vLine(
                centerX + size,
                centerY - size,
                centerY + size,
                Colors.GREEN
        );
    }
}