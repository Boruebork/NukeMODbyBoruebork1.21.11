package com.boruebork.nukemod;

import com.boruebork.nukemod.block.ModBlocks;
import com.boruebork.nukemod.block.entity.ModBE;
import com.boruebork.nukemod.entity.ModEntities;
import com.boruebork.nukemod.explosion.ExpandingExplosion;
import com.boruebork.nukemod.explosion.NuclearExplosion;
import com.boruebork.nukemod.explosion.client.FlashHandler;
import com.boruebork.nukemod.explosion.client.packet.FlashPacket;
import com.boruebork.nukemod.gui.ModMenuTypes;
import com.boruebork.nukemod.item.ModCreativeModeTabs;
import com.boruebork.nukemod.item.ModItems;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.ArrayList;
import java.util.List;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(NukeModbyBoruebork.MODID)
public class NukeModbyBoruebork {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "nukemodbyboruebork";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static List<ExpandingExplosion> explosions = new ArrayList<>();
    public static List<NuclearExplosion> newExplosions = new ArrayList<>();
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public NukeModbyBoruebork(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModEntities.register(modEventBus);
        ModBE.register(modEventBus);
        ModMenuTypes.register(modEventBus);


        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (NukeModbyBoruebork) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        //modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Pre event){
        long start = System.nanoTime();
        while (
                ExpandingExplosion.currentShellGened <= ExpandingExplosion.MAX_RADIUS &&
                        System.nanoTime() - start < 2_000_000 // 2 ms
        ) {
            ExpandingExplosion.generateShells();
        }
        /*for (ExpandingExplosion explosion : explosions){
            explosion.tick1();
        }*/
        for (NuclearExplosion explosion: newExplosions){
            explosion.tick();
        }
    }
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ExpandingExplosion.generateShells();
    }
    
}
