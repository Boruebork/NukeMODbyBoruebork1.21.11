package com.boruebork.nukemod.entity.custom.client.uav;

import com.boruebork.nukemod.NukeModbyBoruebork;

import com.boruebork.nukemod.entity.custom.uav.RQ4;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class RQ4Renderer extends EntityRenderer<RQ4, EntityRenderState> {
    private RQ4Model model;
    public RQ4Renderer(EntityRendererProvider.Context context) {
        super(context);
        model = new RQ4Model(context.bakeLayer(RQ4Model.LAYER_LOCATION));
    }
    @Override
    public void submit(EntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        poseStack.translate(0,5,0);
        renderModel(this.model, renderState, poseStack, nodeCollector, cameraRenderState);
    }
    public void renderModel(RQ4Model model, EntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState){
        poseStack.pushPose();
        poseStack.rotateAround(Axis.XN.rotationDegrees(180), 0,0,0);
        nodeCollector.submitModel(model, renderState, poseStack, model.renderType(getTexture()), renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.outlineColor, (ModelFeatureRenderer.CrumblingOverlay) null);
        poseStack.popPose();

    }

    private Identifier getTexture() {
        return Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID,
                "textures/entity/rq4.png");
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
