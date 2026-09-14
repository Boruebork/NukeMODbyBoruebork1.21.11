package com.boruebork.nukemod.entity.custom.client.mushroom;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.MushroomEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class MushroomEntityRenderer extends EntityRenderer<MushroomEntity, MushroomRenderState> {
    protected final MushroomModel<MushroomRenderState> model;
    public static final int MAX_MUSHROOM_SCALE = 10;
    private static final Identifier TEXTURE_LOCATION = Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "textures/effect/mushroom.png");
    public MushroomEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new MushroomModel<>(context.bakeLayer(MushroomModel.LAYER_LOCATION));
    }

    @Override
    public void submit(MushroomRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        poseStack.pushPose();

        // Flip upside down
        poseStack.rotateAround(
                com.mojang.math.Axis.ZP.rotationDegrees(180),
                0, 0, 0
        );
        poseStack.translate(0, -1.5f, 0);
        float scale = 1 + renderState.ageInTicks/20 < MAX_MUSHROOM_SCALE ? 1 + renderState.ageInTicks/20 : MAX_MUSHROOM_SCALE;
        poseStack.scale(scale, scale, scale);
        nodeCollector.submitModel(this.model, renderState, poseStack, this.model.renderType(TEXTURE_LOCATION),renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.outlineColor,(ModelFeatureRenderer.CrumblingOverlay)null);
        poseStack.popPose();
    }

    @Override
    public MushroomRenderState createRenderState() {
        return new MushroomRenderState();
    }
}
