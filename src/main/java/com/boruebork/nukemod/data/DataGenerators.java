package com.boruebork.nukemod.data;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = NukeModbyBoruebork.MODID)
public class DataGenerators {
    @SubscribeEvent
    private static void gatherDataEvent(GatherDataEvent.Client event){
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        BlockTagsProvider blockTagsProvider = new ModBLockTagProvider(packOutput, lookupProvider);
        generator.addProvider(true, blockTagsProvider);
        generator.addProvider(true, new ModModelProvider(packOutput));
        generator.addProvider(true, new ModDataPackProvider(packOutput, lookupProvider));
        generator.addProvider(true, new ModRecipeProvider.Runner(packOutput, lookupProvider));



    }
    @SubscribeEvent
    private static void gatherDataEvent(GatherDataEvent.Server event){
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        BlockTagsProvider blockTagsProvider = new ModBLockTagProvider(packOutput, lookupProvider);
        generator.addProvider(true, blockTagsProvider);
        generator.addProvider(true, new ModModelProvider(packOutput));
        generator.addProvider(true, new ModDataPackProvider(packOutput, lookupProvider));
        generator.addProvider(true, new ModRecipeProvider.Runner(packOutput, lookupProvider));



    }
}
