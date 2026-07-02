package com.boruebork.nukemod.entity.custom.sound;

import com.boruebork.nukemod.entity.custom.GuidedMissile;
import com.boruebork.nukemod.explosion.NukeConfig;
import com.boruebork.nukemod.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class MissileWarningSound extends AbstractTickableSoundInstance {

    private final GuidedMissile missile;
    public MissileWarningSound(GuidedMissile missile) {
        super(
                ModSounds.MISSILE_WARNING.get(),
                SoundSource.NEUTRAL,
                RandomSource.create()
        );

        this.missile = missile;

        this.looping = true;
        this.delay = 0;
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
            this.missile.setWarningSound(null);
        }

        this.x = missile.getX();
        this.y = missile.getY();
        this.z = missile.getZ();
    }

}
