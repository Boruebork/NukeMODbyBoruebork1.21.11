package com.boruebork.nukemod.data;

import com.boruebork.nukemod.block.ModBlocks;
import com.boruebork.nukemod.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.RARE_DUST_BLOCK.get());
        dropSelf(ModBlocks.TITANIUM_BLOCK.get());
        dropSelf(ModBlocks.ENRICHED_URANIUM_BLOCK.get());
        dropSelf(ModBlocks.MODERN_ALLOY_BLOCK.get());
        dropSelf(ModBlocks.ENRICHER.get());
        dropSelf(ModBlocks.URANIUM_BLOCK.get());
        dropSelf(ModBlocks.TUNGSTEN_BLOCK.get());
        dropSelf(ModBlocks.LAUNCHER.get());
        dropSelf(ModBlocks.GUIDED_LAUNCHER.get());
        dropSelf(ModBlocks.WATER_IONIZER.get());
        add(ModBlocks.TITANIUM_ORE.get(),
                block -> createOreDrop(ModBlocks.TITANIUM_ORE.get(), ModItems.RAW_TITANIUM.get())); //ORE DROPS
        add(ModBlocks.TUNGSTEN_ORE.get(),
                block -> createOreDrop(ModBlocks.TUNGSTEN_ORE.get(), ModItems.RAW_TUNGSTEN.get()));
        add(ModBlocks.DEEPSLATE_URANIUM_ORE.get(),
                block -> createMultipleOreDrops(ModBlocks.DEEPSLATE_URANIUM_ORE.get(), ModItems.URANIUM_DUST.get(), 1, 3));
        add(ModBlocks.URANIUM_ORE.get(),
                block -> createMultipleOreDrops(ModBlocks.URANIUM_ORE.get(), ModItems.URANIUM_DUST.get(), 1, 3));
        add(ModBlocks.DEEPSLATE_TITANIUM_ORE.get(),
                block -> createOreDrop(ModBlocks.DEEPSLATE_TITANIUM_ORE.get(), ModItems.RAW_TITANIUM.get()));
        add(ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get(),
                block -> createOreDrop(ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get(), ModItems.RAW_TUNGSTEN.get()));
        add(ModBlocks.RARE_ORE.get(),
                block -> createMultipleOreDrops(ModBlocks.RARE_ORE.get(), ModItems.DIRTY_RARE_DUST.get(), 2, 4));
        add(ModBlocks.DEEPSLATE_RARE_ORE.get(),
                block -> createMultipleOreDrops(ModBlocks.DEEPSLATE_RARE_ORE.get(), ModItems.DIRTY_RARE_DUST.get(), 2, 4));


    }
    protected LootTable.Builder createMultipleOreDrops(Block pBlock, Item item, float minDrops, float maxDrops) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(pBlock,
                this.applyExplosionDecay(pBlock, LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(minDrops, maxDrops)))
                        .apply(ApplyBonusCount.addOreBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))));
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}