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

        poseStack.translate(0, -0.84f, 0);

        // rotate around the model's own pivot instead of around (0,0,0)
        poseStack.translate(-0.25F / 16F, 15.5F / 16F, -0.1667F / 16F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-renderState.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(renderState.xRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.translate(0.25F / 16F, -15.5F / 16F, 0.1667F / 16F);

        nodeCollector.submitModel(
                model, renderState, poseStack,
                model.renderType(getTexture()),
                renderState.lightCoords, OverlayTexture.NO_OVERLAY,
                renderState.outlineColor, null
        );

        poseStack.popPose();
    }
    private Identifier getTexture() {
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
