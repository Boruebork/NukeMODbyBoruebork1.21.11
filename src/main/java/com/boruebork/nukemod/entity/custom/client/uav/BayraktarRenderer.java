package com.boruebork.nukemod.entity.custom.client.uav;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.uav.BayraktarTB2;
import com.boruebork.nukemod.entity.custom.uav.RQ4;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BayraktarRenderer extends EntityRenderer<BayraktarTB2, BayraktarRenderState> {
    ItemModelResolver itemModelResolver;
    ItemStack BODY = make("bayraktar_tb2");
    public BayraktarRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }
    private static ItemStack make(String id) {
        ItemStack s = new ItemStack(Items.STICK);
        s.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, id));
        return s;
    }

    @Override
    public void submit(BayraktarRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        poseStack.mulPose(Axis.YP.rotationDegrees(-renderState.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(renderState.xRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(renderState.zRot));
        poseStack.translate(0.5f, 0.91f, -2);
        renderState.body.submit(poseStack, nodeCollector, renderState.lightCoords,
                OverlayTexture.NO_OVERLAY, renderState.outlineColor);        // Body-space only: model origin → CG. Retune after this change.

    }
    @Override
    public void extractRenderState(BayraktarTB2 entity, BayraktarRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.xRot = entity.getXRot(partialTick);
        reusedState.yRot = entity.getYRot(partialTick);
        reusedState.zRot = entity.getRenderRoll(partialTick);
        this.itemModelResolver.updateForTopItem(
                reusedState.body, BODY, ItemDisplayContext.NONE, entity.level(), entity, entity.getId());
    }
    @Override
    public BayraktarRenderState createRenderState() {
        return new BayraktarRenderState();
    }
}
