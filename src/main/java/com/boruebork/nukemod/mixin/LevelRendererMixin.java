package com.boruebork.nukemod.mixin;

import com.boruebork.nukemod.entity.custom.AbstractFPVDrone;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Redirect(
            method = "extractVisibleEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;isDetached()Z"
            )
    )
    private boolean drone$forceRenderWhenPiloting(Camera camera) {
        if (camera.entity() instanceof AbstractFPVDrone) {
            return true; // pretend we're "detached" so the entity != camera.entity() OR passes
        }
        return camera.isDetached();
    }
}
