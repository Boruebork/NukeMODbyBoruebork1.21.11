package com.boruebork.nukemod.explosion;

import com.boruebork.nukemod.entity.ModEntities;
import com.boruebork.nukemod.entity.custom.MushroomEntity;
import com.boruebork.nukemod.explosion.client.FlashHandler;
import com.boruebork.nukemod.network.packet.NuclearExplosionUpdateClientPacket;
import com.boruebork.nukemod.sound.ModSounds;
import com.boruebork.nukemod.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.boruebork.nukemod.explosion.NukeConfig.LOUD_HEARING_DISTANCE;
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
    public List<Player> affectedPlayers = new ArrayList<>();
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
        this.tickSinceLastPhase = -10;
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
    ///  A server side method used to send an update to the client
    public void sendUpdate(){
        for (Player player : this.affectedPlayers){
            PacketDistributor.sendToPlayer((ServerPlayer) player, new NuclearExplosionUpdateClientPacket(this.id,this.position, this.phase, this.tickSinceLastPhase));
        }
    }
    private int boomDelayTicks = 0;
    /// Client-side method to sync the client `NuclearExplosion` with a server one, the equality is defined based on `NuclearExplosion.id`, if the ids don't match the method will throw a `RuntimeException`
    public void update(NuclearExplosionUpdateClientPacket packet){
        if (!this.id.equals(packet.id())){
            throw new RuntimeException("Couldn't update an Explosion because the data didn't belong to it!");
        }
        if (this.phase != NuclearExplosionPhase.SHOCKWAVE && packet.phase() == NuclearExplosionPhase.SHOCKWAVE){
            double distance = Minecraft.getInstance().player.position().distanceTo(Util.BlockPosTooVec3(new BlockPos(packet.pos())));
            boomDelayTicks = (int)(distance / 340.0 * 20.0);
        }
        this.phase = packet.phase();
        this.tickSinceLastPhase = packet.ticksSinceStartOfPhase();
        this.level = Minecraft.getInstance().level;
    }
    /** A serverSide static method to create an new explosion**/
    /**A tick method for both sides**/
    public void tick(){
        if (level.isClientSide()) clientTick();
        else serverTick();
        this.tickSinceLastPhase++;

    }
    private void clientTick(){
        if (phase == NuclearExplosionPhase.FLASH){
            if (this.tickSinceLastPhase == 0)
                FlashHandler.startNuclearFlash();
            if (this.tickSinceLastPhase >= NukeConfig.FLASH_DURATION_IN_TICKS){
                changeStage(NuclearExplosionPhase.SHOCKWAVE);
            }
        }else if (phase == NuclearExplosionPhase.SHOCKWAVE){
            if (this.boomDelayTicks <= 0 && !this.boomed){
                double distance = Minecraft.getInstance().player.position().distanceTo(Vec3.atLowerCornerOf(this.position));

                double t = Mth.clamp(distance / LOUD_HEARING_DISTANCE, 0.0, 1.0);

                //float volume = (float) Math.pow(1.0 - t, 2.0)*4;
                float volume = 4;
                System.err.println("BOOOOM");
                /*Minecraft.getInstance().level.playLocalSound(
                        position.getX(),
                        position.getY(),
                        position.getZ(),
                        ModSounds.NUKE_CLOSE.get(),
                        SoundSource.HOSTILE,
                        volume,
                        1.0f,
                        false
                );*/
                if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().level.playLocalSound(
                            Minecraft.getInstance().player.blockPosition(),
                            ModSounds.NUKE_CLOSE.get(),
                            SoundSource.MASTER,
                            10.0f,
                            1.0f,
                            false
                    );
                }
                this.boomed = true;
            }
            if (this.tickSinceLastPhase >= NukeConfig.EXPLOSION_DURATION_IN_TICKS){
                changeStage(NuclearExplosionPhase.FALLOUT);
            }
        }

    }
    boolean boomed = false;
    private void serverTick(){
        updatePlayers();
        //System.err.println("Server Tick!");
        if (phase == NuclearExplosionPhase.FLASH){
            if (this.tickSinceLastPhase >= NukeConfig.FLASH_DURATION_IN_TICKS){
                changeStage(NuclearExplosionPhase.SHOCKWAVE);
            }
        }
        else if (phase == NuclearExplosionPhase.SHOCKWAVE){

            if (explosion == null){
                ///TODO: fix this expanding explosion
                explosion = new ExpandingExplosion((ServerLevel) level, new BlockPos(position), NukeConfig.DESTRUCTION_RADIUS);
            }
            if (mushroom == null) {
                mushroom = new MushroomEntity(ModEntities.MUSHROOM_ENTITY.get(), level);
                ModEntities.MUSHROOM_ENTITY.get().spawn((ServerLevel) level,
                        new BlockPos(new Vec3i(position.getX(), position.getY() - NUCLEAR_EXPLOSION_RADIUS, position.getZ())),
                        EntitySpawnReason.TRIGGERED);

            }
            explosion.tick();
        }
        this.tickSinceLastPhase++;
    }
    public void changeStage(NuclearExplosionPhase phase){
        this.phase = phase;
        this.tickSinceLastPhase = 0;
        this.sendUpdate();
    }
    private void updatePlayers(){
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
            if (!this.affectedPlayers.contains(player)){
                //if a player went in middle explosion we need to provide necessary detail to create one on his client
                PacketDistributor.sendToPlayer((ServerPlayer) player, new NuclearExplosionUpdateClientPacket(this.id, this.position, this.phase, this.tickSinceLastPhase));
            }

        }
    }
}
