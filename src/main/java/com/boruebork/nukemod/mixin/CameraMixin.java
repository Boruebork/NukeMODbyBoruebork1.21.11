package com.boruebork.nukemod.mixin;

import com.boruebork.nukemod.drone.ClientDroneManager;
import com.boruebork.nukemod.ooblib.AbstractUAV;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(Vec3 pos);

    // setRotation is no longer called from here -- ClientDroneManager.onCameraAngles
    // (ViewportEvent.ComputeCameraAngles) is now the SOLE place UAV camera rotation gets
    // set. Having both set rotation was a race between two different data sources; this
    // mixin now only ever handles the position OFFSET (nose-camera placement), which
    // ComputeCameraAngles has no hook for.

    @Inject(method = "setup", at = @At("TAIL"))
    private void nukemod$offsetUAVCamera(
            Level level,
            Entity entity,
            boolean detached,
            boolean mirror,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!(ClientDroneManager.PilotingClientState.drone instanceof AbstractUAV uav))
            return;

        Vector3f local = new Vector3f(0f, 3f, -10f); // 3rd person
        if (detached) {
            this.setPosition(uav.camPosFrom(local, partialTick));
            return;
        }
        this.setPosition(uav.getRenderPosition(partialTick));
    }
}