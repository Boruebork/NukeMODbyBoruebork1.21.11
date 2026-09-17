package com.boruebork.nukemod.entity.custom.client.fpvint;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.FPVInterceptorDrone;
import com.boruebork.nukemod.entity.custom.client.drone.AbstractFPVRenderer;
import com.boruebork.nukemod.entity.custom.client.fpv.FPVModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;

public class FPVInterceptorRenderer extends AbstractFPVRenderer<FPVInterceptorDrone,FPVInterceptorRenderState, FPVInterceptorModel> {
    private FPVInterceptorModel model;
    public FPVInterceptorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new FPVInterceptorModel(context.bakeLayer(FPVInterceptorModel.LAYER_LOCATION));
    }

    @Override
    public void submit(FPVInterceptorRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        this.model.setupAnim(renderState);
        poseStack.translate(0, 1.6, 0);
        poseStack.rotateAround(Axis.YN.rotationDegrees(renderState.yRot), 0, -1.6f+renderState.eyeHeight, 0);
        poseStack.rotateAround(Axis.XN.rotationDegrees(-renderState.xRot), 0 , -1.6f+renderState.eyeHeight, 0);
        renderModel(this.model, renderState, poseStack, nodeCollector, cameraRenderState);

    }

    @Override
    public void extractRenderState(FPVInterceptorDrone entity, FPVInterceptorRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
    }

    @Override
    public Identifier getTexture() {
        return Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID,
                "textures/entity/fpv_interceptor.png");
    }

    @Override
    protected FPVInterceptorRenderState createDroneRenderState() {
        return new FPVInterceptorRenderState();
    }
}
