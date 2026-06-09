package com.boruebork.nukemod.network.packet;

import com.boruebork.nukemod.block.entity.GuidedMissileLauncherBE;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

public record LaunchGuidedPacket(GameProfile target, Player entity) {
    void o(){
        
    }
}
