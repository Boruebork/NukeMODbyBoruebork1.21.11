package com.boruebork.nukemod.worldgen;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.List;

public class ModPlacedFeatures {

    public static final ResourceKey<PlacedFeature> TITANIUM_ORE_PLACED_KEY = registerKey("titanium_ore_place_key");
    public static final ResourceKey<PlacedFeature> TUNGSTEN_ORE_PLACED_KEY = registerKey("tungsten_ore_place_key");
    public static final ResourceKey<PlacedFeature> URANIUM_ORE_PLACED_KEY = registerKey("uranium_ore_place_key");
    public static final ResourceKey<PlacedFeature> RARE_ORE_PLACED_KEY = registerKey("rare_ore_place_key");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        var configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);


        register(context, TITANIUM_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.TITANIUM_ORE_KEY),
                ModOrePlacement.commonOrePlacement(49, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(20))));
        register(context, TUNGSTEN_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.TUNGSTEN_ORE_KEY),
                ModOrePlacement.commonOrePlacement(40, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(30))));
        register(context, URANIUM_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.URANIUM_ORE_KEY),
                ModOrePlacement.commonOrePlacement(35, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-12))));
        register(context, RARE_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.RARE_ORE_KEY),
                ModOrePlacement.commonOrePlacement(25, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(10))));

    }

    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, name));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}
