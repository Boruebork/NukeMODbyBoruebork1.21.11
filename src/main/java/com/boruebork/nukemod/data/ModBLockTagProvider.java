package com.boruebork.nukemod.data;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

public class ModBLockTagProvider extends BlockTagsProvider {
    public ModBLockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, NukeModbyBoruebork.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.TITANIUM_BLOCK.get())
                .add(ModBlocks.TITANIUM_ORE.get())
                .add(ModBlocks.ENRICHED_URANIUM_BLOCK.get())
                .add(ModBlocks.TUNGSTEN_ORE.get())
                .add(ModBlocks.DEEPSLATE_URANIUM_ORE.get())
                .add(ModBlocks.DEEPSLATE_TITANIUM_ORE.get())
                .add(ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get())
                .add(ModBlocks.URANIUM_ORE.get())
                .add(ModBlocks.DEEPSLATE_RARE_ORE.get())
                .add(ModBlocks.RARE_ORE.get())
                .add(ModBlocks.RARE_DUST_BLOCK.get())
                .add(ModBlocks.URANIUM_BLOCK.get());


        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.TITANIUM_BLOCK.get())
                .add(ModBlocks.TITANIUM_ORE.get())
                .add(ModBlocks.TUNGSTEN_ORE.get())
                .add(ModBlocks.DEEPSLATE_TITANIUM_ORE.get())
                .add(ModBlocks.URANIUM_BLOCK.get())
                .add(ModBlocks.RARE_DUST_BLOCK.get())
                .add(ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get());
        tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(ModBlocks.DEEPSLATE_URANIUM_ORE.get())
                .add(ModBlocks.URANIUM_ORE.get())
                .add(ModBlocks.DEEPSLATE_RARE_ORE.get())
                .add(ModBlocks.ENRICHED_URANIUM_BLOCK.get())
                .add(ModBlocks.RARE_ORE.get());


    }
}
