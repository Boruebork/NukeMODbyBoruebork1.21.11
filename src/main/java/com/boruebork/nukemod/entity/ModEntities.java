package com.boruebork.nukemod.entity;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
    public static ResourceKey<EntityType<?>> FPV_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace("fpv"));
    public static ResourceKey<EntityType<?>> FPV_INT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace("fpv_interceptor"));
    public static ResourceKey<EntityType<?>> GRENADE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace("grenade"));
    public static ResourceKey<EntityType<?>> GRENADE_DRONE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace("grenade_drone"));
    public static ResourceKey<EntityType<?>> ROCKET_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace("rocket"));
    public static ResourceKey<EntityType<?>> ROCKET_DRONE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace("rocket_drone"));
    public static final Supplier<EntityType<NukeEntity>> NUKE =
            ENTITY_TYPES.register("nuke", () -> EntityType.Builder.of(NukeEntity::new,
                            MobCategory.MISC)
                    .sized(0.75f, 0.35f).build(NUKE_KEY));
    public static final DeferredHolder<EntityType<?>, EntityType<GuidedMissile>> GUIDED_MISILE =
            ENTITY_TYPES.register("guided_nuclear_missile", () -> EntityType.Builder.of(GuidedMissile::new,
                            MobCategory.MISC)
                    .sized(3, 1).build(GUIDED_MISSILE_KEY));
    public static final DeferredHolder<EntityType<?>, EntityType<MushroomEntity>> MUSHROOM_ENTITY =
            ENTITY_TYPES.register("nuclear_mushroom", () -> EntityType.Builder.of(MushroomEntity::new,
                            MobCategory.MISC)
                    .sized(6, 7).noSave().build(MUSHROOM_KEY));
    public static final DeferredHolder<EntityType<?>, EntityType<FPVDrone>> FPV_DRONE =
            ENTITY_TYPES.register("fpv", () -> EntityType.Builder.of(FPVDrone::new,
                            MobCategory.MISC)
                    .sized(1, 0.5f).clientTrackingRange(64)   // blocks at which the server keeps sending updates
                    .updateInterval(1).build(FPV_KEY));
    public static final DeferredHolder<EntityType<?>, EntityType<FPVInterceptorDrone>> FPV_INTERCEPTOR_DRONE =
            ENTITY_TYPES.register("fpv_interceptor", () -> EntityType.Builder.of(FPVInterceptorDrone::new,
                            MobCategory.MISC)
                    .sized(1, 0.5f).clientTrackingRange(64)   // blocks at which the server keeps sending updates
                    .updateInterval(1).build(FPV_INT_KEY));

    public static final DeferredHolder<EntityType<?>, EntityType<Grenade>> GRENADE =
            ENTITY_TYPES.register("grenade", () -> EntityType.Builder.of(Grenade::new,
                            MobCategory.MISC)
                    .sized(0.25f, 0.25f)   // blocks at which the server keeps sending updates
                    .build(GRENADE_KEY));
    public static final DeferredHolder<EntityType<?>, EntityType<GrenadeDrone>> GRENADE_DRONE =
            ENTITY_TYPES.register("grenade_drone", () -> EntityType.Builder.of(GrenadeDrone::new,
                            MobCategory.MISC)
                    .sized(1, 0.5f)   // blocks at which the server keeps sending updates
                    .build(GRENADE_DRONE_KEY));
    public static final DeferredHolder<EntityType<?>, EntityType<Rocket>> ROCKET =
            ENTITY_TYPES.register("rocket", () -> EntityType.Builder.of(Rocket::new,
                            MobCategory.MISC)
                    .sized(0.25f, 0.25f)   // blocks at which the server keeps sending updates
                    .build(ROCKET_KEY));
    public static final DeferredHolder<EntityType<?>, EntityType<RocketDrone>> ROCKET_DRONE =
            ENTITY_TYPES.register("rocket_drone", () -> EntityType.Builder.of(RocketDrone::new,
                            MobCategory.MISC)
                    .sized(1, 0.5f)   // blocks at which the server keeps sending updates
                    .build(ROCKET_DRONE_KEY));



    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
