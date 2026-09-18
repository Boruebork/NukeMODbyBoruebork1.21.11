package com.boruebork.nukemod.mixin;

import com.boruebork.nukemod.drone.ClientDroneManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class SwingSuppressMixin {
    @Inject(method = "swing*", at = @At("HEAD"), cancellable = true)
    private void drone$suppressSwing(InteractionHand hand, CallbackInfo ci) {
        if ((LivingEntity) (Object) this == Minecraft.getInstance().player
                && ClientDroneManager.PilotingClientState.drone != null) {
            ci.cancel();
        }
    }
}
