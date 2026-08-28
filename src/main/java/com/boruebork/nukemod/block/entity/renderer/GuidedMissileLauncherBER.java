package com.boruebork.nukemod.block.entity.renderer;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.block.entity.GuidedMissileLauncherBE;
import com.boruebork.nukemod.util.Colors;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class GuidedMissileLauncherBER implements BlockEntityRenderer<GuidedMissileLauncherBE, GuidedMissilelauncherBERenderstate> {
    private final GuidedlauncherModel model;
    public GuidedMissileLauncherBER(BlockEntityRendererProvider.Context context) {

        this.model = new GuidedlauncherModel(context.bakeLayer(GuidedlauncherModel.LAYER_LOCATION));
    }

    @Override
    public void extractRenderState(GuidedMissileLauncherBE blockEntity, GuidedMissilelauncherBERenderstate state, float partialTick, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        state.rotatorY = blockEntity.getYaw(partialTick);
        state.headX = blockEntity.getPitch(partialTick);

    }

    @Override
    public GuidedMissilelauncherBERenderstate createRenderState() {
        return new GuidedMissilelauncherBERenderstate();
    }

    @Override
    public void submit(GuidedMissilelauncherBERenderstate renderstate, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        submitNodeCollector.submitModel(this.model,renderstate, poseStack, RenderTypes.entityCutout(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID,"textures/block/guided_launcher_e.png")), renderstate.lightCoords, OverlayTexture.NO_OVERLAY, 0,(ModelFeatureRenderer.CrumblingOverlay) null);
        submitNodeCollector.submitText(poseStack, 0, 0, FormattedCharSequence.forward("S", Style.EMPTY), true, Font.DisplayMode.NORMAL,0,0,0,0);

        //Minecraft.getInstance().font.drawInBatch("fffffff", 0, 0, Colors.GREEN,false,poseStack.last().pose(), MultiBufferSource.immediate(), Font.DisplayMode.NORMAL, Colors.BLACK, renderstate.lightCoords);
        poseStack.popPose();
    }
}
