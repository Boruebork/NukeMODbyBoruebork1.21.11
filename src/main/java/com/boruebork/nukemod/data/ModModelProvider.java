package com.boruebork.nukemod.data;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.block.ModBlocks;
import com.boruebork.nukemod.item.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.stream.Stream;

public class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output, NukeModbyBoruebork.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        //ITEMS
        itemModels.generateFlatItem(ModItems.TITANIUM_INGOT.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.RAW_TITANIUM.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.URANIUM_DUST.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.TUNGSTEN_INGOT.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.RAW_TUNGSTEN.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.RARE_DUST.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.DIRTY_RARE_DUST.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.DIAMOND_ROD.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.NEPTUNIUM.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.HEAVY_WATER.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.ENRICHED_URANIUM_DUST.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.MODERN_ALLOY.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.NUCLEAR_WARHEAD.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.IONIZER.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.ELECTRONICS.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        //BLOCKS
        blockModels.createTrivialCube(ModBlocks.RAW_TUNGSTEN_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.RAW_TITANIUM_BLOCK.get());
        //blockModels.createTrivialCube(ModBlocks.TUNGSTEN_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.TITANIUM_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.TITANIUM_ORE.get());
        blockModels.createTrivialCube(ModBlocks.TUNGSTEN_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.TUNGSTEN_ORE.get());
        blockModels.createTrivialCube(ModBlocks.DEEPSLATE_URANIUM_ORE.get());
        blockModels.createTrivialCube(ModBlocks.DEEPSLATE_TITANIUM_ORE.get());
        blockModels.createTrivialCube(ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get());
        blockModels.createTrivialCube(ModBlocks.URANIUM_ORE.get());
        blockModels.createTrivialCube(ModBlocks.RARE_ORE.get());
        blockModels.createTrivialCube(ModBlocks.DEEPSLATE_RARE_ORE.get());
        blockModels.createTrivialCube(ModBlocks.ENRICHED_URANIUM_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.MODERN_ALLOY_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.URANIUM_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.DIRTY_RARE_DUST_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.RARE_DUST_BLOCK.get());
        //blockModels.createTrivialCube(ModBlocks.ENRICHER.get());
        //blockModels.createTrivialCube(ModBlocks.WATER_IONIZER.get());
        //blockModels.createTrivialCube(ModBlocks.GUIDED_LAUNCHER.get());
        blockModels.createTrivialCube(ModBlocks.LAUNCHER.get());

    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream()
                .filter(blockDeferredHolder -> blockDeferredHolder == ModBlocks.ENRICHER)
                .filter(blockDeferredHolder -> blockDeferredHolder == ModBlocks.WATER_IONIZER)
                .filter(blockDeferredHolder -> blockDeferredHolder == ModBlocks.GUIDED_LAUNCHER);
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return ModItems.ITEMS.getEntries().stream();
    }
}
