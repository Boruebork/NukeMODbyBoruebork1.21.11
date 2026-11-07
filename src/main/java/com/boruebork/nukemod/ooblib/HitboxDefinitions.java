package com.boruebork.nukemod.ooblib;

import com.boruebork.nukemod.entity.ModEntities;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.common.Mod;
import org.joml.Vector3f;

import java.util.HashMap;
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
    private static final Map<EntityType<?>, Vec3> OFFSET = new HashMap<>();
    public static void register(Supplier<? extends EntityType<?>> entity, List<HitboxPart> parts) {
        register(entity.get(), parts);
    }
    public static void registerOffset(Supplier<? extends EntityType<?>> entity, Vec3 offset) {
        OFFSET.put(entity.get(), offset);
    }
    private static void register(EntityType<?> type, List<HitboxPart> parts) {
        PARTS.put(type, List.copyOf(parts));
    }

    public static List<HitboxPart> get(EntityType<?> type) {
        if (type == ModEntities.RQ4.get()){
            return List.of(
                    HitboxPart.of("body", 0,1,-2.4f,0.6f,0.6f,3,  new Vector3f(0,0,0)),
                    HitboxPart.of("left_wing", 2,1,-1.6f,1.38f,0.2f,0.5f,  new Vector3f(0,0,0)),
                    HitboxPart.of("right_wing", -2,1,-1.6f,1.38f,0.2f,0.5f,  new Vector3f(0,0,0))
            );
        } else if (type == ModEntities.BAYRAKTAR_TB2.get()) {
            return List.of(
                    HitboxPart.of("body", 0,1,-1.3f,0.6f,0.6f,2.1f,  new Vector3f(0,0,0)),
                    HitboxPart.withRotation("left_wing", 3.2f,1.5f,-2.2f,2.75f,0.075f,0.5f,  new Vector3f(0,0,0), 0, (float) Math.toRadians(2.5f), (float) Math.toRadians(3)),
                    HitboxPart.withRotation("right_wing", -3.2f,1.5f,-2.2f,2.75f,0.075f,0.5f,  new Vector3f(0,0,0), 0, (float) Math.toRadians(-2.5f), (float) Math.toRadians(-3)),
                    HitboxPart.withRotation("left_stick", 1.1f, 1.5f, -4f, 0.1f, 0.1f, 1.7f, new Vector3f(), (float) Math.toRadians(3), 0, 0),
                    HitboxPart.withRotation("right_stick", -1.1f, 1.5f, -4f, 0.1f, 0.1f, 1.7f, new Vector3f(), (float) Math.toRadians(3), 0, 0),
                    HitboxPart.withRotation("right_back_wing", -0.5f, 2.1f, -5.2f, 0.8f, 0.1f, 0.5f, new Vector3f(), (float) Math.toRadians(3), 0, (float) Math.toRadians(45)),
                    HitboxPart.withRotation("left_back_wing", 0.5f, 2.1f, -5.2f, 0.8f, 0.1f, 0.5f, new Vector3f(), (float) Math.toRadians(3), 0, (float) Math.toRadians(-45))
            );
        }
        return PARTS.getOrDefault(type, List.of());
    }
    public static Vec3 getoffset(EntityType<?> type){
        return OFFSET.get(type);
    }

    HitboxDefinitions() {
    }
}
