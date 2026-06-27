package com.boruebork.nukemod.entity.custom.client;

import com.boruebork.nukemod.entity.custom.GuidedMissile;
import com.boruebork.nukemod.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class MissileWarningSound extends AbstractTickableSoundInstance {

    private final GuidedMissile missile;

    public MissileWarningSound(GuidedMissile missile) {
        super(ModSounds.MISSILE_WARNING.get(),
                SoundSource.HOSTILE,
                RandomSource.create()
        );
        this.missile = missile;
    }

    @Override
    public void tick() {

        if (!missile.isAlive()) {
            stop();
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;

        if (player.distanceTo(missile) > 100) {
            stop();
        }

        this.x = missile.getX();
        this.y = missile.getY();
        this.z = missile.getZ();
    }
}
