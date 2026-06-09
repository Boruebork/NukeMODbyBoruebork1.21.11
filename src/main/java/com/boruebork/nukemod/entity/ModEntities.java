package com.boruebork.nukemod.entity;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.GuidedMissile;
import com.boruebork.nukemod.entity.custom.MushroomEntity;
import com.boruebork.nukemod.entity.custom.NukeEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, NukeModbyBoruebork.MODID);

    public static ResourceKey<EntityType<?>> NUKE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace("nuke"));
    public static ResourceKey<EntityType<?>> GUIDED_MISSILE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace("guided_missile"));
    public static ResourceKey<EntityType<?>> MUSHROOM_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace("mushroom_key"));

    public static final Supplier<EntityType<NukeEntity>> NUKE =
            ENTITY_TYPES.register("nuke", () -> EntityType.Builder.of(NukeEntity::new,
                            MobCategory.MISC)
                    .sized(0.75f, 0.35f).build(NUKE_KEY));
    public static final DeferredHolder<EntityType<?>, EntityType<GuidedMissile>> GUIDED_MISILE =
            ENTITY_TYPES.register("guided_nuclear_missile", () -> EntityType.Builder.of(GuidedMissile::new,
                            MobCategory.MISC)
                    .sized(0.75f, 0.35f).build(GUIDED_MISSILE_KEY));
    public static final DeferredHolder<EntityType<?>, EntityType<MushroomEntity>> MUSHROOM_ENTITY =
            ENTITY_TYPES.register("nuclear_mushroom", () -> EntityType.Builder.of(MushroomEntity::new,
                            MobCategory.MISC)
                    .sized(6, 7).build(MUSHROOM_KEY));


    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
