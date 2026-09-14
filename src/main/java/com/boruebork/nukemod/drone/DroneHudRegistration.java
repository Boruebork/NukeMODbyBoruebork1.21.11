package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.util.Colors;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class DroneHudRegistration {
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

        // Example: crosshair-style reticle
        graphics.hLine(centerX - 10, centerX + 10, centerY, Colors.GREEN);
        graphics.vLine(centerX, centerY - 10, centerY + 10, Colors.GREEN);

        // Example: corner brackets, letterbox bars, telemetry text, etc.
        graphics.drawString(
                Minecraft.getInstance().font,
                "ALT: " + (int) ClientDroneManager.PilotingClientState.drone.getY(),
                10, height - 20, Colors.GREEN
        );
    }
}
