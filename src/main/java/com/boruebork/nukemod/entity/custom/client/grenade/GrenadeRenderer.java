package com.boruebork.nukemod.entity.custom.client.grenade;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.Grenade;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class GrenadeRenderer extends EntityRenderer<Grenade, GrenadeRenderState> {
    private GrenadeModel model;
    public GrenadeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new GrenadeModel(context.bakeLayer(GrenadeModel.LAYER_LOCATION));
    }

    @Override
    public void submit(GrenadeRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        poseStack.pushPose();
        poseStack.translate(0, 1.5f, 0);
        poseStack.rotateAround(Axis.XN.rotationDegrees(180), 0,0,0);
        poseStack.rotateAround(Axis.YN.rotationDegrees(renderState.yRot), 0,0,0);
        nodeCollector.submitModel(model, renderState, poseStack, model.renderType(getTexture()), renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.outlineColor, (ModelFeatureRenderer.CrumblingOverlay) null);
        poseStack.popPose();
    }

    private Identifier getTexture() {
        return Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID,
                "textures/entity/grenade.png");
    }

    @Override
    public void extractRenderState(Grenade entity, GrenadeRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.yRot = entity.getYRot();
        reusedState.xRot = entity.getXRot();

    }

    @Override
    public GrenadeRenderState createRenderState() {
        return new GrenadeRenderState();
    }
}
