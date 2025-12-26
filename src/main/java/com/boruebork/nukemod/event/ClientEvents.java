package com.boruebork.nukemod.event;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.gui.screen.LaunchScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = NukeModbyBoruebork.MODID)
public class ClientEvents {
    @SubscribeEvent
    private static void clTick(ClientTickEvent.Post event){
        if (Keymappings.LAUNCH_MAPPING.get().consumeClick()){
            if (!(Minecraft.getInstance().screen instanceof LaunchScreen)){
                Minecraft.getInstance().setScreen(new LaunchScreen());
            }
        }
    }
}
