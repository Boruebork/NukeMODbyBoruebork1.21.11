package com.boruebork.nukemod.util;

import com.boruebork.nukemod.entity.ModEntities;
import com.boruebork.nukemod.item.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Map;
import java.util.function.Supplier;

public class VehiclesToItemsConfig {
    public static Supplier<? extends EntityType<?>> getEntity(Item item){
        for (DeferredItem<Item> it : DATA.keySet()){
            if (it.get() == item){
                return DATA.get(it);
            }
        }
        return null;
    }
    public static final Map<DeferredItem<Item>, Supplier<? extends EntityType<?>>> DATA = Map.of(
            ModItems.NUCLEAR_WARHEAD, ModEntities.GUIDED_MISILE
    );
}
