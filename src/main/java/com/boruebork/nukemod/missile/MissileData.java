package com.boruebork.nukemod.missile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class MissileData
{
    public static final Codec<MissileData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    UUIDUtil.CODEC.fieldOf("id")
                            .forGetter(data -> data.id),

                    Codec.DOUBLE.fieldOf("speed")
                            .forGetter(data -> data.speed),

                    Vec3.CODEC.fieldOf("position")
                            .forGetter(data -> data.position),

                    Vec3.CODEC.fieldOf("direction")
                            .forGetter(data -> data.direction),

                    Vec3.CODEC.fieldOf("target")
                            .forGetter(data -> data.target)

            ).apply(instance, MissileData::new));
    public MissileData(Vec3 current, Vec3 rotation, Vec3 target, double speed) {
        this.position = current;
        this.direction = rotation;
        this.target = target;
        this.speed = speed;
    }
    public UUID id;
    double speed;
    Vec3 position;
    Vec3 direction;
    Vec3 target = new Vec3(0,0,0);
    public void tick(MinecraftServer server){

    }

    public MissileData(UUID id, double speed, Vec3 position, Vec3 direction, Vec3 target) {
        this.id = id;
        this.speed = speed;
        this.position = position;
        this.direction = direction;
        this.target = target;
    }
}
