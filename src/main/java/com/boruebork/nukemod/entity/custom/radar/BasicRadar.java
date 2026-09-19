package com.boruebork.nukemod.entity.custom.radar;

import com.boruebork.nukemod.entity.custom.AbstractFPVDrone;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;

public class BasicRadar extends AbstractRadar{
    public BasicRadar(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean canBeDetected(Entity entity) {
        return entity instanceof AbstractFPVDrone || entity instanceof Cow;
    }
}
