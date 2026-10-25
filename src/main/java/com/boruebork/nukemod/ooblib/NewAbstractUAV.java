package com.boruebork.nukemod.ooblib;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public abstract class NewAbstractUAV extends AbstractFixedWingAircraft {
    public NewAbstractUAV(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
}
