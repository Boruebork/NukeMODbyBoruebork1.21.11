package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.ModEntities;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVDrone;
import com.boruebork.nukemod.entity.custom.fpvdrones.AbstractFPVProjectileLaunchingDrone;
import com.boruebork.nukemod.util.Colors;
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

import java.util.Map;

@EventBusSubscriber(value = Dist.CLIENT)
public class DroneHudRegistration {

    // --- horizon line tuning ---
    private static final int HORIZON_GAP = 16;     // px from center to the start of each segment
    private static final int HORIZON_LENGTH = 22;  // px length of each segment

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

        AbstractFPVDrone drone = ClientDroneManager.PilotingClientState.drone;

        // Interpolate the same way the camera roll itself is interpolated (see onCameraAngles) —
        // reading the raw un-interpolated roll here would reintroduce the 20Hz stair-step jitter
        // we fixed on the camera earlier, just on the HUD instead.
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
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
}