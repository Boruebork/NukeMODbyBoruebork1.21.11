package com.boruebork.nukemod.entity.custom.client.rocket;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.Grenade;
import com.boruebork.nukemod.entity.custom.Rocket;
import com.boruebork.nukemod.entity.custom.client.grenade.GrenadeRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class RocketRenderer extends EntityRenderer<Rocket, RocketRenderState> {
    private RocketModel model;
    public RocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RocketModel(context.bakeLayer(RocketModel.LAYER_LOCATION));
    }

    @Override
    public void submit(RocketRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        poseStack.pushPose();
        poseStack.translate(0, 1.5f, 0);
        poseStack.rotateAround(Axis.XN.rotationDegrees(180), 0,0,0);
        poseStack.rotateAround(Axis.YP.rotationDegrees(renderState.yRot), 0,0,0);
        poseStack.rotateAround(Axis.XP.rotationDegrees(renderState.xRot), 0,0,0);
        nodeCollector.submitModel(model, renderState, poseStack, model.renderType(getTexture()), renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.outlineColor, (ModelFeatureRenderer.CrumblingOverlay) null);
        poseStack.popPose();

    }private Identifier getTexture() {
        return Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID,
                "textures/entity/rocket.png");
    }

    @Override
    public void extractRenderState(Rocket entity, RocketRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.yRot = entity.getYRot();
        reusedState.xRot = entity.getXRot();

    }

    @Override
    public RocketRenderState createRenderState() {
        return new RocketRenderState();
    }
}
