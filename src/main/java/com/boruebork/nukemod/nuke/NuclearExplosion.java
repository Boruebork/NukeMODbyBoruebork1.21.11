package com.boruebork.nukemod.nuke;

import com.boruebork.nukemod.network.packet.FlashPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.boruebork.nukemod.nuke.NukeConfig.FLASH_RADIUS;


public class NuclearExplosion {
    private final Level level;
    private final BlockPos center;
    private int tick;
    private ExplosionPhase phase;
    private Shockwave shockwave;

    public NuclearExplosion(Level level, BlockPos center) {
        this.level = level;
        this.center = center;
        this.tick = 0;
        this.phase = ExplosionPhase.SHOCKWAVE;
    }
    public void tick(){
        /*if (tick < 100){
            this.phase = ExplosionPhase.FLASH;
        } else if (tick < 240) {
            this.phase = ExplosionPhase.SHOCKWAVE;
            
        }*/
        if (phase == ExplosionPhase.FLASH && tick == 0){
            startFlash();
        }
        if (phase == ExplosionPhase.SHOCKWAVE && shockwave == null){
            shockwave = new Shockwave(new Vec3(center.getX(), center.getY(), center.getZ()), 2, NukeConfig.SHOCKWAVE_RADIUS, this.level);
        }else{
            shockwave.tick();
        }
        this.tick++;
    }

    private void startFlash() {
        PacketDistributor.sendToPlayersNear((ServerLevel) level, null, center.getX(), center.getY(), center.getZ(), FLASH_RADIUS, new FlashPacket(center.getX(), center.getY(), center.getZ()));
    }



    public void discard(){
        ExplosionHandler.discardExplosion(this);
    }
}
