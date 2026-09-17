package com.boruebork.nukemod.entity;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

public class NukeMODEntityDataSerializers {
    private static final DeferredRegister<EntityDataSerializer<?>> DATA_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, NukeModbyBoruebork.MODID);

    public static final Supplier<EntityDataSerializer<List<Integer>>> INT_LIST =
            DATA_SERIALIZERS.register("int_list",
                    () -> EntityDataSerializer.forValueType(ByteBufCodecs.INT.apply(ByteBufCodecs.list())));
    public static final Supplier<EntityDataSerializer<List<Boolean>>> BOOL_LIST =
            DATA_SERIALIZERS.register("bool_list",
                    () -> EntityDataSerializer.forValueType(ByteBufCodecs.BOOL.apply(ByteBufCodecs.list())));

    public static void register(IEventBus eventBus){
        DATA_SERIALIZERS.register(eventBus);
    }
}
