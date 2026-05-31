package com.boruebork.nukemod.worldgen;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

public class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> TITANIUM_ORE_KEY = registerKey("titanium_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TUNGSTEN_ORE_KEY = registerKey("tungsten_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> URANIUM_ORE_KEY = registerKey("uranium_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> RARE_ORE_KEY = registerKey("rare_ore");



    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest stoneReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        RuleTest netherrackReplaceables = new BlockMatchTest(Blocks.NETHERRACK);
        RuleTest endReplaceables = new BlockMatchTest(Blocks.END_STONE);

        List<OreConfiguration.TargetBlockState> overworldTitaniumOres = List.of(
                OreConfiguration.target(stoneReplaceables, ModBlocks.TITANIUM_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceables, ModBlocks.DEEPSLATE_TITANIUM_ORE.get().defaultBlockState())

        );
        List<OreConfiguration.TargetBlockState> overworldTungstenOres = List.of(
                OreConfiguration.target(stoneReplaceables, ModBlocks.TUNGSTEN_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceables, ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get().defaultBlockState())
        );
        List<OreConfiguration.TargetBlockState> overworldUraniumOres = List.of(
                OreConfiguration.target(stoneReplaceables, ModBlocks.URANIUM_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceables, ModBlocks.DEEPSLATE_URANIUM_ORE.get().defaultBlockState())
        );
        List<OreConfiguration.TargetBlockState> overworldRareOres = List.of(
                OreConfiguration.target(stoneReplaceables, ModBlocks.RARE_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceables, ModBlocks.DEEPSLATE_RARE_ORE.get().defaultBlockState())
        );
        register(context, TITANIUM_ORE_KEY, Feature.ORE, new OreConfiguration(overworldTitaniumOres, 8));
        register(context, TUNGSTEN_ORE_KEY, Feature.ORE, new OreConfiguration(overworldTungstenOres, 5));
        register(context, URANIUM_ORE_KEY, Feature.ORE, new OreConfiguration(overworldUraniumOres, 4));
        register(context, RARE_ORE_KEY, Feature.ORE, new OreConfiguration(overworldRareOres, 4));


    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
