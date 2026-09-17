package com.boruebork.nukemod.entity.custom.client.grenade_drone;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.GrenadeDrone;
import com.boruebork.nukemod.entity.custom.client.drone.AbstractFPVRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;

public class GrenadeDroneRenderer extends AbstractFPVRenderer<GrenadeDrone, GrenadeDroneRenderState, GrenadeDroneModel> {
    private GrenadeDroneModel model;
    public GrenadeDroneRenderer(EntityRendererProvider.Context context) {
        super(context);

        this.model = new GrenadeDroneModel(context.bakeLayer(GrenadeDroneModel.LAYER_LOCATION));
    }
    @Override
    public Identifier getTexture() {
        return Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID,
                "textures/entity/grenade_drone.png");
    }

    @Override
    public void submit(GrenadeDroneRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        this.model.setupAnim(renderState);
        poseStack.translate(0, 1.6, 0);
        poseStack.rotateAround(Axis.YN.rotationDegrees(renderState.yRot), 0, -1.6f+renderState.eyeHeight, 0);
        poseStack.rotateAround(Axis.XN.rotationDegrees(-renderState.xRot), 0 , -1.6f+renderState.eyeHeight, 0);
        renderModel(model, renderState, poseStack, nodeCollector,cameraRenderState);
    }

    @Override
    public void extractRenderState(GrenadeDrone entity, GrenadeDroneRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.grenade1 = entity.grenade1();
        reusedState.grenade2 = entity.grenade2();
    }

    @Override
    protected GrenadeDroneRenderState createDroneRenderState() {
        return new GrenadeDroneRenderState();
    }
}
