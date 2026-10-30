package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;
import java.util.UUID;

public class PilotSessions extends SavedData {
    public static PilotSessions INSTANCE;

    public static PilotSessions getInstance() {
        if (INSTANCE == null) {
            if (NukeModbyBoruebork.server == null) throw new IllegalStateException("Server not initialized");
            INSTANCE = NukeModbyBoruebork.server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(PilotSessions.TYPE);
        }
        return INSTANCE;
    }

    private List<Session> sessions;

    public List<Session> getSessions() {
        return sessions;
    }

    public void setSessions(List<Session> sessions) {
        this.sessions = sessions;
        this.setDirty();
    }

    public PilotSessions() {
        INSTANCE = this;
    }
    public PilotSessions(List<Session> sessions) {
        super();
        this.sessions = sessions;
    }
    public record Session(UUID player, UUID vehicle, ResourceKey<Level> dim, BlockPos pos) {
        static final Codec<Session> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(Session::player),
                UUIDUtil.CODEC.fieldOf("vehicle").forGetter(Session::vehicle),
                Level.RESOURCE_KEY_CODEC.fieldOf("dim").forGetter(Session::dim),
                BlockPos.CODEC.fieldOf("pos").forGetter(Session::pos)
        ).apply(i, Session::new));
    }
    public static final SavedDataType<PilotSessions> TYPE = new SavedDataType<>(
            "drone_sessions",
            PilotSessions::new,
            RecordCodecBuilder.create(instance -> instance.group(
                Codec.list(Session.CODEC).fieldOf("sessions").forGetter(PilotSessions::getSessions)
            ).apply(instance, PilotSessions::new))
    );
    // byPlayer map, get/put/remove/byVehicle/updatePos, each mutator calls setDirty()
}
