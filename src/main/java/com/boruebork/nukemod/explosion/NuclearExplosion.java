package com.boruebork.nukemod.explosion;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.ModEntities;
import com.boruebork.nukemod.entity.custom.MushroomEntity;
import com.boruebork.nukemod.explosion.client.packet.FlashPacket;
import com.boruebork.nukemod.network.ServerPayloadHandler;
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

import java.util.List;

public class NuclearExplosion {
    public NuclearExplosionPhase phase;
    public static final int MAX_MUSHROOM_SIZE = 30;
    public static final int NUCLEAR_EXPLOSION_RADIUS = 20;
    public Vec3i position;
    public ServerLevel level;
    public MushroomEntity mushroom = null;
    public ExpandingExplosion explosion;
    public NuclearExplosion(ServerLevel level,Vec3i position){
        this.phase = NuclearExplosionPhase.FLASH;
        this.level = level;
        this.position = position;
    }
    public static NuclearExplosion createExplosion(ServerLevel lvl, Vec3i pos){
        NukeModbyBoruebork.newExplosions.add(new NuclearExplosion(lvl, pos));
        return NukeModbyBoruebork.newExplosions.getLast();
    }
    public void tick(){
        if (phase == NuclearExplosionPhase.FLASH){
            AABB box = new AABB(
                    position.getX() + NUCLEAR_EXPLOSION_RADIUS,
                    position.getY() + NUCLEAR_EXPLOSION_RADIUS,
                    position.getZ() + NUCLEAR_EXPLOSION_RADIUS,
                    position.getX() - NUCLEAR_EXPLOSION_RADIUS,
                    position.getY() - NUCLEAR_EXPLOSION_RADIUS,
                    position.getZ() - NUCLEAR_EXPLOSION_RADIUS
            );
            List<Player> entitiesInRange = level.getEntitiesOfClass(Player.class, box);
            for (Player player : entitiesInRange){
                System.err.println("Sending packet to Player:"+player.getName().getString());
                PacketDistributor.sendToPlayer((ServerPlayer) player, new FlashPacket(20));
            }
            this.phase = NuclearExplosionPhase.SHOCKWAVE;
        }
        if (phase == NuclearExplosionPhase.SHOCKWAVE){
            if (explosion == null){
                explosion = ExpandingExplosion.createExplosion(level, new BlockPos(position), NUCLEAR_EXPLOSION_RADIUS);
            }
            if (mushroom == null) {
                mushroom = new MushroomEntity(ModEntities.MUSHROOM_ENTITY.get(), level);
               ModEntities.MUSHROOM_ENTITY.get().spawn(level,
                        new BlockPos(new Vec3i(position.getX(), position.getY() - NUCLEAR_EXPLOSION_RADIUS, position.getZ())),
                        EntitySpawnReason.TRIGGERED);

            }
            explosion.tick1();
        }
    }

}
