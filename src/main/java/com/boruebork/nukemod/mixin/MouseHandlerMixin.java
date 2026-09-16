package com.boruebork.nukemod.mixin;

import com.boruebork.nukemod.drone.ClientDroneManager;
import com.boruebork.nukemod.entity.custom.AbstractFPVDrone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.SmoothDouble;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow
    private SmoothDouble smoothTurnX;
    @Shadow
    private SmoothDouble smoothTurnY;
    @Shadow
    private double accumulatedDX;
    @Shadow
    private double accumulatedDY;
    @Shadow
    private Minecraft minecraft;
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void redirectToDrone(double movementTime, CallbackInfo ci) {
        AbstractFPVDrone drone = ClientDroneManager.PilotingClientState.drone;
        if (drone != null) {
            CalculatePlayerTurnEvent event = ClientHooks.getTurnPlayerValues(
                    (Double) this.minecraft.options.sensitivity().get(), this.minecraft.options.smoothCamera);
            double d2 = event.getMouseSensitivity() * 0.6F + 0.2F;
            double d3 = d2 * d2 * d2;
            double d4 = d3 * 8.0F;
            double d0;
            double d1;
            if (event.getCinematicCameraEnabled()) {
                d0 = this.smoothTurnX.getNewDeltaValue(this.accumulatedDX * d4, movementTime * d4);
                d1 = this.smoothTurnY.getNewDeltaValue(this.accumulatedDY * d4, movementTime * d4);
            } else if (minecraft.options.getCameraType().isFirstPerson() && this.minecraft.player.isScoping()) {
                this.smoothTurnX.reset();
                this.smoothTurnY.reset();
                d0 = this.accumulatedDX * d3;
                d1 = this.accumulatedDY * d3;
            } else {
                this.smoothTurnX.reset();
                this.smoothTurnY.reset();
                d0 = this.accumulatedDX * d4;
                d1 = this.accumulatedDY * d4;
            }

            minecraft.getTutorial().onMouse(d0, d1);
            ClientDroneManager.PilotingClientState.turn(
                    (Boolean) this.minecraft.options.invertMouseX().get() ? -d0 : d0,
                    (Boolean) this.minecraft.options.invertMouseY().get() ? -d1 : d1
            );

            // MUST reset — vanilla does this at the end of turnPlayer, and cancelling skips it
            this.accumulatedDX = 0.0D;
            this.accumulatedDY = 0.0D;

            ci.cancel();
        }
    }

}
