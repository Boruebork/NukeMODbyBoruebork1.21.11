package com.boruebork.nukemod.mixin;

import com.boruebork.nukemod.drone.ClientDroneManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class PlayerCrouchSuppressMixin {
    @Inject(method = "isShiftKeyDown", at = @At("HEAD"), cancellable = true)
    private void drone$suppressShift(CallbackInfoReturnable<Boolean> cir) {
        if ((Entity)(Object) this == Minecraft.getInstance().player
                && ClientDroneManager.PilotingClientState.drone != null) {
            cir.setReturnValue(false);
        }
    }
}
