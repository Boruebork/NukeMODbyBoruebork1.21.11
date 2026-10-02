package com.boruebork.nukemod.ooblib;

import com.boruebork.nukemod.entity.ModEntities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.common.Mod;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry mapping EntityType -> its list of hitbox parts, same shape as
 * your bay-offset map. Register these once at mod init, next to wherever
 * you register bay layouts.
 */
public final class HitboxDefinitions {

    private static final Map<EntityType<?>, List<HitboxPart>> PARTS = new IdentityHashMap<>();
    public static void register(Supplier<? extends EntityType<?>> entity, List<HitboxPart> parts) {
        register(entity.get(), parts);
    }
    private static void register(EntityType<?> type, List<HitboxPart> parts) {
        PARTS.put(type, List.copyOf(parts));
    }

    public static List<HitboxPart> get(EntityType<?> type) {
        if (type == ModEntities.RQ4.get()){
            return List.of(
                    HitboxPart.of("body", 0,4,-1,0.6f,0.6f,3),
                    HitboxPart.of("left_wing", 2,4,-0.5f,1.38f,0.2f,0.5f),
                    HitboxPart.of("left_wing", -2,4,-0.5f,1.38f,0.2f,0.5f)
            );
        }
        return PARTS.getOrDefault(type, List.of());
    }

    HitboxDefinitions() {
    }
}
