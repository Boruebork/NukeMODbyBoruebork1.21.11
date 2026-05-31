package com.boruebork.nukemod.nuke.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class FlashHandler {
    @SubscribeEvent
    public static void foo(RenderLevelStageEvent.AfterLevel event){

    }
}
