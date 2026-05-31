package com.boruebork.nukemod.data;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.block.ModBlocks;
import com.boruebork.nukemod.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
        super(provider, recipeOutput);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> provider) {
            super(packOutput, provider);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
            return new ModRecipeProvider(provider, recipeOutput);
        }

        @Override
        public String getName() {
            return "My Recipes";
        }
    }


    @Override
    protected void buildRecipes() {
        List<ItemLike> TITANIUM_SMELTABLES = List.of(ModItems.RAW_TITANIUM,
                ModBlocks.TITANIUM_ORE, ModBlocks.DEEPSLATE_TITANIUM_ORE);
        List<ItemLike> TUNGSTEN_SMELTABLES = List.of(ModItems.RAW_TUNGSTEN,
                ModBlocks.TUNGSTEN_ORE, ModBlocks.DEEPSLATE_TUNGSTEN_ORE);
        List<ItemLike> RARE_SMELTABLES = List.of(
                ModBlocks.RARE_ORE, ModBlocks.DEEPSLATE_RARE_ORE);
        List<ItemLike> URANIUM_SMELTABLES = List.of(
                ModBlocks.URANIUM_ORE, ModBlocks.DEEPSLATE_URANIUM_ORE);

        shaped(RecipeCategory.MISC, ModBlocks.TITANIUM_BLOCK.get())
                .pattern("BBB")
                .pattern("BBB")
                .pattern("BBB")
                .define('B', ModItems.TITANIUM_INGOT.get())
                .unlockedBy("has_titanium", has(ModItems.TITANIUM_INGOT)).save(output);

        shapeless(RecipeCategory.MISC, ModItems.TITANIUM_INGOT.get(), 9)
                .requires(ModBlocks.TITANIUM_BLOCK)
                .unlockedBy("has_tungsten_block", has(ModBlocks.TUNGSTEN_BLOCK)).save(output);




        shaped(RecipeCategory.MISC, ModBlocks.TUNGSTEN_BLOCK.get())
                .pattern("BBB")
                .pattern("BBB")
                .pattern("BBB")
                .define('B', ModItems.TUNGSTEN_INGOT.get())
                .unlockedBy("has_tungsten", has(ModItems.TUNGSTEN_INGOT)).save(output);

        shapeless(RecipeCategory.MISC, ModItems.TUNGSTEN_INGOT.get(), 9)
                .requires(ModBlocks.TUNGSTEN_BLOCK)
                .unlockedBy("has_titanium_block", has(ModBlocks.TUNGSTEN_BLOCK)).save(output);




        shaped(RecipeCategory.MISC, ModBlocks.RARE_DUST_BLOCK.get())
                .pattern("BBB")
                .pattern("BBB")
                .pattern("BBB")
                .define('B', ModItems.RARE_DUST.get())
                .unlockedBy("has_titanium", has(ModItems.RARE_DUST)).save(output);

        shapeless(RecipeCategory.MISC, ModItems.RARE_DUST.get(), 9)
                .requires(ModBlocks.RARE_DUST_BLOCK)
                .unlockedBy("has_tungsten_block", has(ModBlocks.RARE_DUST_BLOCK)).save(output);




        shaped(RecipeCategory.MISC, ModBlocks.URANIUM_BLOCK.get())
                .pattern("BBB")
                .pattern("BBB")
                .pattern("BBB")
                .define('B', ModItems.URANIUM_DUST.get())
                .unlockedBy("has_tungsten", has(ModItems.URANIUM_DUST)).save(output);
        shapeless(RecipeCategory.MISC, ModItems.URANIUM_DUST.get(), 9)
                .requires(ModBlocks.URANIUM_BLOCK)
                .unlockedBy("has_tungsten_block", has(ModBlocks.URANIUM_BLOCK)).save(output);



        shaped(RecipeCategory.MISC, ModBlocks.ENRICHED_URANIUM_BLOCK.get())
                .pattern("BBB")
                .pattern("BBB")
                .pattern("BBB")
                .define('B', ModItems.ENRICHED_URANIUM_DUST.get())
                .unlockedBy("has_uranium", has(ModItems.ENRICHED_URANIUM_DUST)).save(output);


        shapeless(RecipeCategory.MISC, ModItems.ENRICHED_URANIUM_DUST.get(), 9)
                .requires(ModBlocks.ENRICHED_URANIUM_BLOCK)
                .unlockedBy("has_titanium_block", has(ModBlocks.ENRICHED_URANIUM_BLOCK)).save(output);
        shaped(RecipeCategory.MISC, ModItems.MODERN_ALLOY.get())
                .pattern("UUI")
                .pattern("UIT")
                .pattern("ITT")
                .define('I', Items.IRON_INGOT)
                .define('T', ModItems.TITANIUM_INGOT.get())
                .define('U', ModItems.TUNGSTEN_INGOT.get())
                .unlockedBy("has_uranium", has(ModItems.MODERN_ALLOY)).save(output);
        shaped(RecipeCategory.MISC, ModBlocks.MODERN_ALLOY_BLOCK.get())
                .pattern("BBB")
                .pattern("BBB")
                .pattern("BBB")
                .define('B', ModItems.MODERN_ALLOY.get())
                .unlockedBy("has_uranium", has(ModItems.MODERN_ALLOY)).save(output);
        shaped(RecipeCategory.MISC, ModItems.NUCLEAR_WARHEAD.get())
                .pattern("MMM")
                .pattern("MEM")
                .pattern("MBM")
                .define('M', ModItems.MODERN_ALLOY.get())
                .define('E', ModBlocks.ENRICHED_URANIUM_BLOCK.get())
                .define('B', ModBlocks.MODERN_ALLOY_BLOCK.get())
                .unlockedBy("has_enriched_uranium_block", has(ModItems.ENRICHED_URANIUM_DUST)).save(output);


        oreSmelting(output, TITANIUM_SMELTABLES, RecipeCategory.MISC, ModItems.TITANIUM_INGOT.get(), 0.25f, 200, "titanium");
        oreBlasting(output, TUNGSTEN_SMELTABLES, RecipeCategory.MISC, ModItems.TUNGSTEN_INGOT.get(), 0.25f, 100, "tungsten");
        oreSmelting(output, RARE_SMELTABLES, RecipeCategory.MISC, ModItems.RARE_DUST.get(), 0.25f, 200, "rare");
        oreBlasting(output, URANIUM_SMELTABLES, RecipeCategory.MISC, ModItems.URANIUM_DUST.get(), 0.25f, 100, "uranium");
        shaped(RecipeCategory.MISC, ModItems.IONIZER.get())
                .pattern("IQI")
                .pattern("RRR")
                .pattern("IQI")
                .define('I', Items.IRON_INGOT)
                .define('Q', Items.QUARTZ)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_redstone", has(Items.REDSTONE)).save(output);

        shaped(RecipeCategory.MISC, ModBlocks.WATER_IONIZER.get())
                .pattern("IRI")
                .pattern("IOI")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('O', ModItems.IONIZER)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_ionizer", has(ModItems.IONIZER)).save(output);
        shaped(RecipeCategory.MISC, ModBlocks.ENRICHER.get())
                .pattern("RDR")
                .pattern("IOI")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('D', Items.DISPENSER)
                .define('O', ModItems.IONIZER)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_ionizer", has(ModItems.IONIZER)).save(output);
    }

    protected void oreSmelting(RecipeOutput recipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                               float pExperience, int pCookingTIme, String pGroup) {
        oreCooking(recipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTIme, pGroup, "_from_smelting");
    }

    protected void oreBlasting(RecipeOutput recipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                               float pExperience, int pCookingTime, String pGroup) {
        oreCooking(recipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTime, pGroup, "_from_blasting");
    }

    protected <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput recipeOutput, RecipeSerializer<T> pCookingSerializer, AbstractCookingRecipe.Factory<T> factory,
                                                                List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup, String pRecipeName) {
        for(ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pCookingSerializer, factory).group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(recipeOutput, NukeModbyBoruebork.MODID + ":" + getItemName(pResult) + pRecipeName + "_" + getItemName(itemlike));
        }
    }
}