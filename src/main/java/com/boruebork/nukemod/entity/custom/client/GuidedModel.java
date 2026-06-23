package com.boruebork.nukemod.entity.custom.client;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.GuidedMissile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

public class GuidedModel extends EntityModel<GuidedMissileRenderstate> {
    // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "guidedmodel"), "main");


    public GuidedModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -16.0F, -22.0F, 10.0F, 8.0F, 50.0F, new CubeDeformation(0.0F))
                .texOffs(64, 76).addBox(-2.0F, -15.0F, -24.0F, 8.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(80, 68).addBox(-1.0F, -14.0F, 28.0F, 6.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition cube_r1 = bb_main.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(40, 68).addBox(-1.0F, -1.0F, 0.0F, 13.0F, 1.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.0F, -6.0F, 21.0F, 0.0607F, 0.116F, -0.3515F));

        PartDefinition cube_r2 = bb_main.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 68).addBox(-1.0F, -1.0F, 0.0F, 13.0F, 1.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, -10.0F, 20.0F, -0.0607F, -0.116F, 0.3515F));

        PartDefinition cube_r3 = bb_main.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(48, 76).addBox(-1.0F, -9.0F, -4.0F, 2.0F, 9.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, -14.0F, 21.0F, -0.3927F, 0.0F, 0.3054F));

        PartDefinition cube_r4 = bb_main.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(32, 76).addBox(-1.0F, -9.0F, -4.0F, 2.0F, 9.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -14.0F, 21.0F, -0.3927F, 0.0F, -0.3054F));

        PartDefinition cube_r5 = bb_main.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(16, 76).addBox(-1.0F, -9.0F, -4.0F, 2.0F, 9.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, -15.0F, 25.0F, 0.0F, 0.0F, 0.3054F));

        PartDefinition cube_r6 = bb_main.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 76).addBox(-1.0F, -9.0F, -4.0F, 2.0F, 9.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -15.0F, 25.0F, 0.0F, 0.0F, -0.3054F));

        PartDefinition cube_r7 = bb_main.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(60, 58).addBox(-11.0F, -2.0F, -3.9914F, 22.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.0466F, -9.0F, 4.9914F, 0.0F, 0.1278F, -0.088F));

        PartDefinition cube_r8 = bb_main.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(0, 58).addBox(-1.0F, -2.0F, 1.0F, 22.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.0F, -10.0F, -1.0F, -0.0285F, -0.1278F, 0.088F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }
}
