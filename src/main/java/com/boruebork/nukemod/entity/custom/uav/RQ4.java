package com.boruebork.nukemod.entity.custom.uav;

import com.boruebork.nukemod.entity.ModEntities;
import com.boruebork.nukemod.entity.custom.client.uav.AbstractWeaponEquipedUAV;
import com.boruebork.nukemod.ooblib.AbstractUAV;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static com.boruebork.nukemod.ooblib.FixedWingUtil.GRAVITY;

public class RQ4 extends AbstractWeaponEquipedUAV {
    @Override protected float getMass()                 { return 1.0f;    }   // kg
    @Override protected float getWingArea()             { return 0.32f;   }   // m²  (~0.6m × 0.55m)
    @Override protected float getMaxThrust()            { return 0.8f * getMass() * GRAVITY; } // ≈ 0.0196
    @Override protected float getThrottleRate()         { return 0.020f;  }   // full range in 2.5 s
    @Override protected float getMaxPitchRatePerTick()  { return 1.0f;    }   // 20°/s
    @Override public float getRollStep()             { return 2.0f;    }   // 40°/s
    @Override protected float getMaxRoll()              { return 45f;     }
    @Override protected float getRollFactor()           { return 3.0f;    }
    public RQ4(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected float getMaxSpeed() {
        return 1.2f;
    }

    @Override
    public float getMaxHealth() {
        return 15;
    }

    @Override
    public float getDeathExplosionPower() {
        return 3;
    }

    @Override
    protected void defineProjectileOffsets() {
        addProjectileBay(ModEntities.ROCKET.get(), new Vec3(0, 0, 0.));
    }

    @Override
    public Entity self() {
        return this;
    }

}
