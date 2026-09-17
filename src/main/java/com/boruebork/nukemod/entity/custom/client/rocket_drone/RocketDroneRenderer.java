package com.boruebork.nukemod.entity.custom.client.rocket_drone;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.RocketDrone;
import com.boruebork.nukemod.entity.custom.client.drone.AbstractFPVRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;

public class RocketDroneRenderer extends AbstractFPVRenderer<RocketDrone, RocketDroneRenderState, RocketDroneModel> {
    private RocketDroneModel model;
    public RocketDroneRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RocketDroneModel(context.bakeLayer(RocketDroneModel.LAYER_LOCATION));
    }

    @Override
    public void submit(RocketDroneRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        this.model.setupAnim(renderState);
        poseStack.translate(0, 1.6, 0);
        poseStack.rotateAround(Axis.YN.rotationDegrees(renderState.yRot), 0, -1.6f+renderState.eyeHeight, 0);
        poseStack.rotateAround(Axis.XN.rotationDegrees(-renderState.xRot), 0 , -1.6f+renderState.eyeHeight, 0);
        renderModel(model, renderState, poseStack, nodeCollector,cameraRenderState);
    }

    @Override
    public Identifier getTexture() {
        return Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "textures/entity/rocket_drone.png");
    }

    @Override
    protected RocketDroneRenderState createDroneRenderState() {
        return new RocketDroneRenderState();
    }

    @Override
    public void extractRenderState(RocketDrone entity, RocketDroneRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.nextRocket = entity.getNextRocket();
    }
}
