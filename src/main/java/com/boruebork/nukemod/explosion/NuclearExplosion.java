package com.boruebork.nukemod.explosion;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.ModEntities;
import com.boruebork.nukemod.entity.custom.MushroomEntity;
import com.boruebork.nukemod.network.ServerPayloadHandler;
import com.boruebork.nukemod.network.packet.NuclearExplosionStartedPacket;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.boruebork.nukemod.explosion.NukeConfig.NUCLEAR_EXPLOSION_VISIBILITY_RADIUS;

public class NuclearExplosion {
    public NuclearExplosionPhase phase;
    public static final int MAX_MUSHROOM_SIZE = 30;
    public static final int NUCLEAR_EXPLOSION_RADIUS = 20;
    public Vec3i position;
    public Level level;
    /// the unique id of an explosion
    public UUID id;
    /// A server side field, to keep record of all players affected by the explosion
    public List<Player> effectedPlayers = new ArrayList<>();
    public MushroomEntity mushroom = null;
    public ExpandingExplosion explosion;
    /// Is used when a player gets into an area mid-explosion
    public int tickSinceLastPhase;
    /**Server side constructor**/
    public NuclearExplosion(ServerLevel level,Vec3i position){
        this.phase = NuclearExplosionPhase.FLASH;
        this.level = level;
        this.position = position;
        this.id = UUID.randomUUID();
    }
    /**Client side constructor**/
    public NuclearExplosion(UUID id, ClientLevel level, Vec3i position, NuclearExplosionPhase phase, int timeSinceLastPhaseChange){
        this.phase = NuclearExplosionPhase.FLASH;
        this.level = level;
        this.position = position;
        this.phase = phase;
        this.tickSinceLastPhase = timeSinceLastPhaseChange;
        this.id = id;

    }
    /** A serverSide static method to create an new explosion**/
    /**A tick method for both sides**/
    public void tick(){
        if (!level.isClientSide()){
            // if on server we should update all the players
            AABB box = new AABB(
                    position.getX() + NUCLEAR_EXPLOSION_VISIBILITY_RADIUS,
                    position.getY() + NUCLEAR_EXPLOSION_VISIBILITY_RADIUS,
                    position.getZ() + NUCLEAR_EXPLOSION_VISIBILITY_RADIUS,
                    position.getX() - NUCLEAR_EXPLOSION_VISIBILITY_RADIUS,
                    position.getY() - NUCLEAR_EXPLOSION_VISIBILITY_RADIUS,
                    position.getZ() - NUCLEAR_EXPLOSION_VISIBILITY_RADIUS
            );
            List<Player> playersInRange = level.getEntitiesOfClass(Player.class, box);
            for (Player player : playersInRange){
                if (!this.effectedPlayers.contains(player)){
                    //if a player went in middle explosion we need to provide necessary detail to create one on his client
                    PacketDistributor.sendToPlayer((ServerPlayer) player, new NuclearExplosionStartedPacket(this.id, this.position, this.phase, this.tickSinceLastPhase));
                }
            }
        }
        if (phase == NuclearExplosionPhase.FLASH){

        }
        if (phase == NuclearExplosionPhase.SHOCKWAVE){
            if (explosion == null){
                //TODO: fix this expanding explosion
                //explosion = ExplosionManager.addExplosion((ServerLevel) level, new BlockPos(position));
            }
            if (mushroom == null) {
                mushroom = new MushroomEntity(ModEntities.MUSHROOM_ENTITY.get(), level);
               ModEntities.MUSHROOM_ENTITY.get().spawn((ServerLevel) level,
                        new BlockPos(new Vec3i(position.getX(), position.getY() - NUCLEAR_EXPLOSION_RADIUS, position.getZ())),
                        EntitySpawnReason.TRIGGERED);

            }
            explosion.tick1();
        }
    }
    public void flashTick(){}

}
