package com.boruebork.nukemod.block;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.block.custom.EnricherBlock;
import com.boruebork.nukemod.block.custom.GuidedMissileLauncher;
import com.boruebork.nukemod.block.custom.LauncherBlock;
import com.boruebork.nukemod.block.custom.WaterIonizerBlock;
import com.boruebork.nukemod.item.ModItems;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(NukeModbyBoruebork.MODID);

    public static final DeferredBlock<Block> TITANIUM_BLOCK = registerBlock("titanium_block",
            (properties) -> new Block(properties
                    .strength(3f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK)));
    public static final DeferredBlock<Block> TITANIUM_ORE = registerBlock("titanium_ore",
            (properties) -> new Block(properties
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> RAW_TITANIUM_BLOCK = registerBlock("raw_titanium_block",
            (properties -> new Block(properties
                    .strength(3f).requiresCorrectToolForDrops().sound(SoundType.ANCIENT_DEBRIS))));
    public static final DeferredBlock<Block> TUNGSTEN_BLOCK = registerBlock("tungsten_block",
            (properties) -> new Block(properties
                    .strength(3f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK)));
    public static final DeferredBlock<Block> TUNGSTEN_ORE = registerBlock("tungsten_ore",
            (properties) -> new Block(properties
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> DEEPSLATE_URANIUM_ORE = registerBlock("deepslate_uranium_ore",
            (properties) -> new Block(properties
                    .strength(6f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<Block> DEEPSLATE_TITANIUM_ORE = registerBlock("deepslate_titanium_ore",
            (properties) -> new Block(properties
                    .strength(5f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<Block> DEEPSLATE_TUNGSTEN_ORE = registerBlock("deepslate_tungsten_ore",
            (properties) -> new Block(properties
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<Block> URANIUM_ORE = registerBlock("uranium_ore",
            (properties) -> new Block(properties
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> RARE_ORE = registerBlock("rare_ore",
            (properties) -> new DropExperienceBlock(UniformInt.of(5, 10),
                    properties
                            .strength(4f).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> DEEPSLATE_RARE_ORE = registerBlock("deepslate_rare_ore",
            (properties) -> new DropExperienceBlock(UniformInt.of(5, 10),
                    properties
                            .strength(5f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<Block> ENRICHED_URANIUM_BLOCK = registerBlock("enriched_uranium_block",
            (properties) -> new Block(properties
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> MODERN_ALLOY_BLOCK = registerBlock("modern_alloy_block",
            (properties) -> new Block(properties
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK)));
    public static final DeferredBlock<Block> URANIUM_BLOCK = registerBlock("uranium_block",
            (properties) -> new Block(properties
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK)));
    public static final DeferredBlock<Block> RARE_DUST_BLOCK = registerBlock("rare_dust_block",
            (properties) -> new Block(properties
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK)));
    public static final DeferredBlock<EnricherBlock> ENRICHER = registerBlock("enricher",
            (properties -> new EnricherBlock(properties.strength(3f).sound(SoundType.SOUL_SAND).noOcclusion())));
    public static final DeferredBlock<WaterIonizerBlock> WATER_IONIZER = registerBlock("water_ionizer",
            (properties) -> new WaterIonizerBlock(properties.strength(3f).sound(SoundType.SOUL_SAND).noOcclusion()));
    public static final DeferredBlock<LauncherBlock> LAUNCHER = registerBlock("launcher",
            (properties) -> new LauncherBlock(properties.strength(3f).sound(SoundType.NETHERITE_BLOCK).noOcclusion()));
    public static final DeferredBlock<GuidedMissileLauncher> GUIDED_LAUNCHER = registerBlock("guided_launcher",
            (properties) -> new GuidedMissileLauncher(properties.strength(3f).sound(SoundType.NETHERITE_BLOCK).noOcclusion()));
    public static final DeferredBlock<Block> RAW_TUNGSTEN_BLOCK = registerBlock("raw_tungsten_block",
            (properties -> new Block(properties
                    .strength(3f).requiresCorrectToolForDrops().sound(SoundType.ANCIENT_DEBRIS))));
    public static final DeferredBlock<Block> DIRTY_RARE_DUST_BLOCK = registerBlock("dirty_rare_dust_block",
            (properties -> new Block(properties
                    .strength(3f).requiresCorrectToolForDrops().sound(SoundType.RESIN))));
    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.registerItem(name, (properties) -> new BlockItem(block.get(), properties.useBlockDescriptionPrefix()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
