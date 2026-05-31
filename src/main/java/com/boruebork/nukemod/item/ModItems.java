package com.boruebork.nukemod.item;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NukeModbyBoruebork.MODID);

    public static final DeferredItem<Item> TITANIUM_INGOT = ITEMS.registerSimpleItem("titanium");

    public static final DeferredItem<Item> RAW_TITANIUM = ITEMS.registerSimpleItem("raw_titanium");

    public static final DeferredItem<Item> NEPTUNIUM = ITEMS.registerSimpleItem("neptunium");

    public static final DeferredItem<Item> DIAMOND_ROD = ITEMS.registerSimpleItem("diamond_rod");

    public static final DeferredItem<Item> TUNGSTEN_INGOT = ITEMS.registerSimpleItem("tungsten_ingot");

    public static final DeferredItem<Item> RAW_TUNGSTEN = ITEMS.registerSimpleItem("raw_tungsten");

    public static final DeferredItem<Item> RARE_DUST = ITEMS.registerSimpleItem("rare_dust");

    public static final DeferredItem<Item> DIRTY_RARE_DUST = ITEMS.registerSimpleItem("dirty_rare_dust");

    public static final DeferredItem<Item> URANIUM_DUST = ITEMS.registerSimpleItem("uranium_dust");

    public static final DeferredItem<Item> HEAVY_WATER = ITEMS.registerSimpleItem("heavy_water");

    public static final DeferredItem<Item> ENRICHED_URANIUM_DUST = ITEMS.registerSimpleItem("enriched_uranium_dust");

    public static final DeferredItem<Item> MODERN_ALLOY = ITEMS.registerSimpleItem("modern_alloy");

    public static final DeferredItem<Item> NUCLEAR_WARHEAD = ITEMS.registerSimpleItem("nuclear_warhead");

    public static final DeferredItem<Item> IONIZER = ITEMS.registerSimpleItem("ionizer");



    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
