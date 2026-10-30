package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PilotSessions extends SavedData {
    public static PilotSessions INSTANCE;

    private List<Session> sessions;

    public List<Session> getSessions() {
        return sessions;
    }

    public void setSessions(List<Session> sessions) {
        this.sessions = sessions;
        this.setDirty();
    }
    public static PilotSessions get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }


    private final Map<UUID, Session> byPlayer = new HashMap<>();
    public PilotSessions() {}
    private PilotSessions(List<Session> list) { list.forEach(s -> byPlayer.put(s.player(), s)); }

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
                    Codec.list(Session.CODEC)
                            .fieldOf("sessions")
                            .forGetter(PilotSessions::all)
            ).apply(instance, PilotSessions::new))
    );
    // byPlayer map, get/put/remove/byVehicle/updatePos, each mutator calls setDirty()
    public List<Session> all() { return List.copyOf(byPlayer.values()); }
    public @Nullable Session get(UUID player) { return byPlayer.get(player); }
    public void put(Session s) { byPlayer.put(s.player(), s); setDirty(); }
    public void remove(UUID player) { if (byPlayer.remove(player) != null) setDirty(); }

    /** Only dirties on a chunk change, so this is cheap to call every tick. */
    public void movedTo(UUID player, ResourceKey<Level> dim, BlockPos pos) {
        Session s = byPlayer.get(player);
        if (s == null) return;
        if (s.dim().equals(dim) && new ChunkPos(s.pos()).equals(new ChunkPos(pos))) return;
        byPlayer.put(player, new Session(player, s.vehicle(), dim, pos));
        setDirty();
    }
}
