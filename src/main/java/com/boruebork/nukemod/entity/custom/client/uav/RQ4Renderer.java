package com.boruebork.nukemod.entity.custom.client.uav;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.uav.RQ4;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class RQ4Renderer extends EntityRenderer<RQ4, RQ4RenderState> {
    private RQ4Model model;

    public RQ4Renderer(EntityRendererProvider.Context context) {
        super(context);
        model = new RQ4Model(context.bakeLayer(RQ4Model.LAYER_LOCATION));
    }

    @Override
    public void submit(RQ4RenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);

        // Same as AbstractUAV.getEntityRotation(): Ry(-yaw) * Rx(pitch) * Rz(roll)
        // Pivot is the entity/CG. Do NOT translate before this.
        poseStack.mulPose(Axis.YP.rotationDegrees(-renderState.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(renderState.xRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(renderState.zRot));

        // Body-space only: model origin → CG. Retune after this change.
        poseStack.translate(0.0F, 2.0F, -1.2F);

        renderModel(this.model, renderState, poseStack, nodeCollector, cameraRenderState);
    }

    public void renderModel(RQ4Model model, RQ4RenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        nodeCollector.submitModel(
                model,
                renderState,
                poseStack,
                model.renderType(getTexture()),
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                renderState.outlineColor,
                (ModelFeatureRenderer.CrumblingOverlay) null
        );
        poseStack.popPose();
    }

    private Identifier getTexture() {
        return Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "textures/entity/rq4.png");
    }

    @Override
    public RQ4RenderState createRenderState() {
        return new RQ4RenderState();
    }

    @Override
    public void extractRenderState(RQ4 entity, RQ4RenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        // Same orientation as OBBs / camera. PilotingClientState will desync the mesh.
        reusedState.xRot = entity.getRenderPitch(partialTick);
        reusedState.yRot = entity.getRenderYaw(partialTick);
        reusedState.zRot = entity.getRenderRoll(partialTick);
        reusedState.cameraPos = entity.getCameraPosition(partialTick);
    }
}