package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.ModEntities;
import com.boruebork.nukemod.entity.custom.AbstractFPVDrone;
import com.boruebork.nukemod.entity.custom.AbstractFPVProjectileLaunchingDrone;
import com.boruebork.nukemod.entity.custom.DroneProjectile;
import com.boruebork.nukemod.util.Colors;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Map;

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
