package com.boruebork.nukemod.entity.custom.client.drone;

import com.boruebork.nukemod.drone.ClientDroneManager;
import com.boruebork.nukemod.entity.custom.AbstractFPVDrone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public abstract class AbstractFPVRenderer<T extends AbstractFPVDrone, S extends AbstractFPVDroneRenderState, M extends EntityModel<? super S>> extends EntityRenderer<T, S> {
    public abstract Identifier getTexture();
    protected AbstractFPVRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void submit(S renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
    }
    public void renderModel(M model, S renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState){
        poseStack.pushPose();
        poseStack.rotateAround(Axis.XN.rotationDegrees(180), 0,0,0);
        nodeCollector.submitModel(model, renderState, poseStack, model.renderType(getTexture()), renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.outlineColor, (ModelFeatureRenderer.CrumblingOverlay) null);
        poseStack.popPose();

    }
    protected abstract S createDroneRenderState();
    @Override
    public S createRenderState(){
        return createDroneRenderState();
    }

    @Override
    public void extractRenderState(T entity, S reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        if (ClientDroneManager.PilotingClientState.drone == entity){
            reusedState.xRot = ClientDroneManager.PilotingClientState.xRot;
            reusedState.yRot = ClientDroneManager.PilotingClientState.yRot;

        }else{
            reusedState.xRot = entity.getXRot(partialTick);
            reusedState.yRot = entity.getYRot(partialTick);
        }
        reusedState.rotorAngle = entity.getRotorAngle() + entity.getRotorSpeed() * partialTick;
    }
}
