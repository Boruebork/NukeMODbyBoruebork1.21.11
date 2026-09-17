package com.boruebork.nukemod;

import com.boruebork.nukemod.block.entity.ModBE;
import com.boruebork.nukemod.block.entity.renderer.GuidedMissileLauncherBER;
import com.boruebork.nukemod.entity.ModEntities;
import com.boruebork.nukemod.entity.custom.client.fpv.FPVRenderer;
import com.boruebork.nukemod.entity.custom.client.fpvint.FPVInterceptorRenderer;
import com.boruebork.nukemod.entity.custom.client.grenade.GrenadeRenderer;
import com.boruebork.nukemod.entity.custom.client.grenade_drone.GrenadeDroneRenderer;
import com.boruebork.nukemod.entity.custom.client.guided.GuidedMissileRenderer;
import com.boruebork.nukemod.entity.custom.client.mushroom.MushroomEntityRenderer;
import com.boruebork.nukemod.entity.custom.client.nuke.NukeEntityRenderer;
import com.boruebork.nukemod.entity.custom.client.rocket.RocketRenderer;
import com.boruebork.nukemod.entity.custom.client.rocket_drone.RocketDroneRenderer;
import com.boruebork.nukemod.gui.ModMenuTypes;
import com.boruebork.nukemod.gui.menu.EnricherScreen;
import com.boruebork.nukemod.gui.menu.GuidedMissileLauncherScreen;
import com.boruebork.nukemod.gui.menu.IonizerScreen;
import com.boruebork.nukemod.gui.menu.launcher.LauncherScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

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
        event.registerEntityRenderer(
                ModEntities.FPV_DRONE.get(),
                FPVRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.FPV_INTERCEPTOR_DRONE.get(),
                FPVInterceptorRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.GRENADE.get(),
                GrenadeRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.GRENADE_DRONE.get(),
                GrenadeDroneRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.ROCKET_DRONE.get(),
                RocketDroneRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.ROCKET.get(),
                RocketRenderer::new
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
    public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBE.GUIDED_LAUNCHER_BE.get(), GuidedMissileLauncherBER::new);
    }
    // In some physical client only class

    // Key mapping is lazily initialized so it doesn't exist until it is registered
    public static final Lazy<KeyMapping> EXIT_DRONE_KEY = Lazy.of(() -> new KeyMapping(
            "key.nukemodbyboruebork.exitdrone", // Will be localized using this translation key
            InputConstants.Type.KEYSYM, // Default mapping is on the keyboard
            GLFW.GLFW_KEY_X, // Default key is P
            KeyMapping.Category.MISC // Mapping will be in the misc category
    ));
    public static final Lazy<KeyMapping> DRONE_ATTACK_MAPPING = Lazy.of(() -> new KeyMapping(
            "key.nukemodbyboruebork.drone_attack", // Will be localized using this translation key
            InputConstants.Type.KEYSYM, // Default mapping is on the keyboard
            GLFW.GLFW_KEY_B, // Default key is P
            KeyMapping.Category.MISC // Mapping will be in the misc category
    ));

    @SubscribeEvent // on the mod event bus only on the physical client
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(EXIT_DRONE_KEY.get());
        event.register(DRONE_ATTACK_MAPPING.get());
    }


}
