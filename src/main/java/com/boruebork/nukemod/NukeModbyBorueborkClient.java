package com.boruebork.nukemod;

import com.boruebork.nukemod.block.ModBlocks;
import com.boruebork.nukemod.block.entity.ModBE;
import com.boruebork.nukemod.block.entity.renderer.GuidedMissileLauncherBER;
import com.boruebork.nukemod.entity.ModEntities;
import com.boruebork.nukemod.entity.custom.client.GuidedMissileRenderer;
import com.boruebork.nukemod.entity.custom.client.MushroomEntityRenderer;
import com.boruebork.nukemod.entity.custom.client.NukeEntityRenderer;
import com.boruebork.nukemod.explosion.NuclearExplosion;
import com.boruebork.nukemod.explosion.client.FlashHandler;
import com.boruebork.nukemod.gui.ModMenuTypes;
import com.boruebork.nukemod.gui.menu.EnricherScreen;
import com.boruebork.nukemod.gui.menu.GuidedMissileLauncherScreen;
import com.boruebork.nukemod.gui.menu.IonizerScreen;
import com.boruebork.nukemod.gui.menu.launcher.LauncherMenu;
import com.boruebork.nukemod.gui.menu.launcher.LauncherScreen;
import com.boruebork.nukemod.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = NukeModbyBoruebork.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = NukeModbyBoruebork.MODID, value = Dist.CLIENT)
public class NukeModbyBorueborkClient {
    public NukeModbyBorueborkClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }



    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code

    }
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ModEntities.NUKE.get(),
                NukeEntityRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.GUIDED_MISILE.get(),
                GuidedMissileRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.MUSHROOM_ENTITY.get(),
                MushroomEntityRenderer::new
        );
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ENRICHER_MENU.get(), EnricherScreen::new);
        event.register(ModMenuTypes.IONIZER_MENU.get(), IonizerScreen::new);
        event.register(ModMenuTypes.GUIDED_MISSILE_MENU.get(), GuidedMissileLauncherScreen::new);
        event.register(ModMenuTypes.LAUNCHER_MENU.get(), LauncherScreen::new);
    }
    @SubscribeEvent
    public static void play(ClientTickEvent.Pre event){
        if (Minecraft.getInstance().player == null) return;
        /*Minecraft.getInstance().level.playLocalSound(
                Minecraft.getInstance().player.blockPosition(),
                ModSounds.NUKE_CLOSE.get(),
                SoundSource.HOSTILE,
                1.0f,
                1.0f,
                false
        );*/

    }

    @SubscribeEvent
    public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBE.GUIDED_LAUNCHER_BE.get(), GuidedMissileLauncherBER::new);
    }


}
