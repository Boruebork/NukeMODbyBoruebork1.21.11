package com.boruebork.nukemod.entity.custom.client.drone;

import com.boruebork.nukemod.entity.custom.Drone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public abstract class DroneRenderer <T extends Drone, M extends EntityModel<? super DroneRenderState>> extends EntityRenderer<T, DroneRenderState> {
    public abstract Identifier getTexture();
    protected DroneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void submit(DroneRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
    }
    public void renderModel(M model, DroneRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState){
        poseStack.pushPose();
        poseStack.rotateAround(Axis.XN.rotationDegrees(180), 0,0,0);
        nodeCollector.submitModel(model, renderState, poseStack, model.renderType(getTexture()), renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.outlineColor, (ModelFeatureRenderer.CrumblingOverlay) null);
        poseStack.popPose();

    }
    @Override
    public DroneRenderState createRenderState(){
        return new DroneRenderState();
    }

    @Override
    public void extractRenderState(T entity, DroneRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.xRot = entity.getXRot(partialTick);
        reusedState.yRot = entity.getYRot(partialTick);
    }
}
