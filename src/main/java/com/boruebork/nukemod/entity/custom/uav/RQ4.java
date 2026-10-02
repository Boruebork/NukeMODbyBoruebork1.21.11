package com.boruebork.nukemod.entity.custom.uav;

import com.boruebork.nukemod.ooblib.AbstractUAV;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class RQ4 extends AbstractUAV {
    public RQ4(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected float getMaxHealth() {
        return 6;
    }

    @Override
    protected float getVerticalSpeedModifier() {
        return 1;
    }

    @Override
    protected float getHorizontalSpeedModifier() {
        return 0.3f;
    }

    @Override
    protected float getOnHitExplosionRadius() {
        return 0;
    }
}
