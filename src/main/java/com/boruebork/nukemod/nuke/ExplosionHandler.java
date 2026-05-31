package com.boruebork.nukemod.nuke;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**The explosion handler class, the bacbone behind explosion logic*/
@EventBusSubscriber(modid = NukeModbyBoruebork.MODID)
public class ExplosionHandler {
    private static List<NuclearExplosion> explosions;
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event){
        explosions = new ArrayList<>();
    }




    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Pre event){
        for (NuclearExplosion explosion : explosions){
            explosion.tick();
        }
    }





    public static void shockwaveHandler(Shockwave shockwave) {
        shockwave.tick();

    }
    /// Add an new Nuclear Explosion to the handler, effectively starting it
    /// @param level  the level, use this.level() in entities
    /// @param blockPos the position of the explosion
    public static void addExplosion(Level level, BlockPos blockPos){
        explosions.add(new NuclearExplosion(level, blockPos));
    }
    /// Add an new Nuclear Explosion to the handler, effectively starting it
    /// @param level  the level, use this.level() in entities
    /// @param vec3 the position of the explosion
    public static void addExplosion(Level level, Vec3 vec3){
       addExplosion(level, new BlockPos((int) vec3.x, (int) vec3.y, (int) vec3.z));
    }
    public static void discardExplosion(NuclearExplosion explosion){
        explosions.remove(explosion);
    }
}
