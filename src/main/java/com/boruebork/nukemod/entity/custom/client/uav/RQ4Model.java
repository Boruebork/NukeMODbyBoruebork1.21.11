package com.boruebork.nukemod.entity.custom.client.uav;// Made with Blockbench 5.2.1
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.uav.RQ4;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

public class RQ4Model extends EntityModel<EntityRenderState> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "rq4"), "main");
	private final ModelPart bb_main;

	public RQ4Model(ModelPart root) {
        super(root);
        this.bb_main = root.getChild("bb_main");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(126, 0).addBox(-2.0F, -2.0F, -21.0F, 2.0F, 2.0F, 13.0F, new CubeDeformation(0.0F))
		.texOffs(50, 113).addBox(-5.0F, -12.0F, -22.0F, 8.0F, 10.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(78, 67).addBox(-5.0F, -10.0F, -8.0F, 8.0F, 8.0F, 26.0F, new CubeDeformation(0.0F))
		.texOffs(0, 26).addBox(-5.0F, -8.0F, 18.0F, 8.0F, 6.0F, 35.0F, new CubeDeformation(0.0F))
		.texOffs(0, 67).addBox(-7.0F, -16.0F, 12.0F, 12.0F, 12.0F, 27.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition cube_r1 = bb_main.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(78, 101).addBox(-24.0F, -2.0F, -1.0F, 25.0F, 2.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -6.0F, 50.0F, 0.047F, 0.0736F, 0.6126F));

		PartDefinition cube_r2 = bb_main.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(86, 53).addBox(-1.0F, -2.0F, -1.0F, 25.0F, 2.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -6.0F, 50.0F, 0.047F, -0.0736F, -0.6126F));

		PartDefinition cube_r3 = bb_main.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 125).addBox(-4.0F, -6.0F, -7.0F, 8.0F, 3.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -2.5F, 56.75F, 0.0873F, 0.0F, 0.0F));

		PartDefinition cube_r4 = bb_main.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(122, 113).addBox(-4.0F, -6.0F, -7.0F, 8.0F, 3.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -2.5F, 59.75F, 0.48F, 0.0F, 0.0F));

		PartDefinition cube_r5 = bb_main.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 13).addBox(-51.0F, -2.0F, -1.0F, 52.0F, 2.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -4.0F, 0.0F, 0.0F, 0.0873F, 0.0436F));

		PartDefinition cube_r6 = bb_main.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.0F, -1.0F, 52.0F, 2.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -4.0F, 0.0F, 0.0F, -0.0873F, -0.0436F));

		PartDefinition cube_r7 = bb_main.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(0, 106).addBox(-6.0F, -4.5F, -4.5F, 12.0F, 6.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -9.5F, 39.5F, -0.7418F, 0.0F, 0.0F));

		PartDefinition cube_r8 = bb_main.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(86, 26).addBox(-4.0F, -6.0F, -7.0F, 8.0F, 3.0F, 24.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -3.0F, 23.0F, -0.1745F, 0.0F, 0.0F));

		PartDefinition cube_r9 = bb_main.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(94, 113).addBox(-3.0F, -9.0F, 0.0F, 6.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -5.25F, -11.25F, 1.0908F, 0.0F, 0.0F));

		PartDefinition cube_r10 = bb_main.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(122, 126).addBox(-3.0F, -9.0F, -8.0F, 6.0F, 9.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -5.25F, -19.75F, -1.0908F, 0.0F, 0.0F));

		PartDefinition cube_r11 = bb_main.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(36, 137).addBox(-3.0F, -7.0F, -8.0F, 6.0F, 7.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -2.0F, -20.0F, -0.5236F, 0.0F, 0.0F));

		PartDefinition cube_r12 = bb_main.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(126, 15).addBox(-1.0F, -2.0F, -2.0F, 2.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -2.75F, -25.25F, -0.5236F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

}