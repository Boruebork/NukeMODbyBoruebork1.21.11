package com.boruebork.nukemod.block.entity;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBE {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, NukeModbyBoruebork.MODID);

    public static final Supplier<BlockEntityType<EnricherBE>> ENRICHER_BE =
            BLOCK_ENTITIES.register("growth_chamber_be", () -> new BlockEntityType<>(
                    EnricherBE::new, ModBlocks.ENRICHER.get()));
    public static final Supplier<BlockEntityType<WaterIonizerBE>> WIBE =
            BLOCK_ENTITIES.register("water_ionizer_be", () -> new BlockEntityType<>(
                    WaterIonizerBE::new, ModBlocks.WATER_IONIZER.get()
            ));
    public static final Supplier<BlockEntityType<LauncherBE>> LAUNCHER_BE =
            BLOCK_ENTITIES.register("launcher_be", () -> new BlockEntityType<>(
                    LauncherBE::new, ModBlocks.LAUNCHER.get()
            ));
    public static final Supplier<BlockEntityType<GuidedMissileLauncherBE>> GUIDED_LAUNCHER_BE =
            BLOCK_ENTITIES.register("guided_missile_launcher_be", () -> new BlockEntityType<>(
                    GuidedMissileLauncherBE::new, ModBlocks.GUIDED_LAUNCHER.get()
            ));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }

}
