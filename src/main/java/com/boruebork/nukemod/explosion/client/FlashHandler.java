package com.boruebork.nukemod.explosion.client;


import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(value = Dist.CLIENT)
public class FlashHandler {
    public static int TOTAL_FLASH_TIME = 40; //In ticks
    public static int currentFlashTime = 0;
    /*public static void startFlash(final FlashPacket packet, IPayloadContext context){
        context.enqueueWork(() -> {
            TOTAL_FLASH_TIME = packet.flashTime();
            currentFlashTime = TOTAL_FLASH_TIME;
            System.err.println("started flash");
        });

    }*/
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event){
        if (currentFlashTime > 0) currentFlashTime--;
    }
    @SubscribeEvent
    public static void overrideOverlayEvent(RenderGuiLayerEvent.Pre event) {
        if (currentFlashTime <= 0) return;
        System.err.println("flashing!");

        float progress = (float) currentFlashTime / TOTAL_FLASH_TIME;
        int alpha = (int)(255 * progress);

        int color = (alpha << 24) | 0xFFFFFF;
        event.getGuiGraphics().fill(
                0,
                0,
                Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                Minecraft.getInstance().getWindow().getGuiScaledHeight(),
                color
        );
    }
}
