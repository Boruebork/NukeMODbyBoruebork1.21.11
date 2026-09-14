package com.boruebork.nukemod.entity.custom.client.nuke;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.NukeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class NukeEntityRenderer extends EntityRenderer<NukeEntity, NukeRenderState> {
    private final NukeModel model;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "textures/entity/nuke.png");
    public NukeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new NukeModel(context.bakeLayer(NukeModel.LAYER_LOCATION));
    }

    @Override
    public void submit(NukeRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        nodeCollector.submitModel(this.model, renderState, poseStack, this.model.renderType(TEXTURE), renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.outlineColor, (ModelFeatureRenderer.CrumblingOverlay) null);
    }

    @Override
    public boolean shouldRender(NukeEntity livingEntity, Frustum camera, double camX, double camY, double camZ) {
        return true;
        //TODO optimize rendering
    }

    @Override
    public NukeRenderState createRenderState() {
        return new NukeRenderState();
    }
}
