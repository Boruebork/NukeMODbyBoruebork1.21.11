package com.boruebork.nukemod.entity.custom.client.fpv;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.Drone;
import com.boruebork.nukemod.entity.custom.client.drone.DroneRenderState;
import com.boruebork.nukemod.entity.custom.client.drone.DroneRenderer;
import com.boruebork.nukemod.entity.custom.client.guided.GuidedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;

public class FPVRenderer extends DroneRenderer<Drone, FPVModel> {
    private FPVModel model;
    public FPVRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new FPVModel(context.bakeLayer(FPVModel.LAYER_LOCATION));
    }

    @Override
    public void submit(DroneRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        poseStack.translate(0, 1.6, 0);
        poseStack.rotateAround(Axis.YN.rotationDegrees(renderState.yRot), 0,-1.5f,0);
        poseStack.rotateAround(Axis.XN.rotationDegrees(-renderState.xRot), 0 , -1.5f, 0);
        renderModel(this.model, renderState, poseStack, nodeCollector, cameraRenderState);
    }

    @Override
    public Identifier getTexture() {
        return Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID,
                "textures/entity/fpv.png");
    }

}
