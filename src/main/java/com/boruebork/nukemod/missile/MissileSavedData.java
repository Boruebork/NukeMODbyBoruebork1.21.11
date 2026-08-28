package com.boruebork.nukemod.missile;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MissileSavedData extends SavedData {
    private static MissileSavedData INSTANCE;
    public static MissileSavedData getInstance(){
        return INSTANCE;
    }
    public Map<UUID, MissileData> missiles = new HashMap<>();

    public static final String DATA_NAME = "nukemod_missiles";


    public MissileSavedData() {
        INSTANCE = this;
    }
    public static final Codec<Map<UUID, MissileData>> MISSILES_CODEC =
            Codec.unboundedMap(
                    UUIDUtil.CODEC,
                    MissileData.CODEC
            );

    public static final SavedDataType<MissileSavedData> ID =
            new SavedDataType<>(
                    "logical_missiles",

                    MissileSavedData::new,

                    RecordCodecBuilder.create(instance ->
                            instance.group(
                                    MISSILES_CODEC.fieldOf("data")
                                            .forGetter((MissileSavedData sd) -> sd.missiles)
                            ).apply(
                                    instance,
                                    MissileSavedData::new
                            )
                    )
            );

    public MissileSavedData(Map<UUID, MissileData> uuidMissileDataMap) {
        this.missiles = uuidMissileDataMap;
        MissileManager.INSTANCE.setDataMissiles(uuidMissileDataMap);
    }
    public void foo() {
        this.missiles = MissileManager.INSTANCE.getDataMissiles();
        this.setDirty();
    }
}