package com.boruebork.nukemod.entity.custom.client.fpv;// Made with Blockbench 5.1.6
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.Drone;
import com.boruebork.nukemod.entity.custom.client.drone.DroneRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

public class FPVModel extends EntityModel<DroneRenderState> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "fpvdrone"), "main");
	private final ModelPart bone;
	private final ModelPart bone2;
	private final ModelPart bone7;
	private final ModelPart bone8;
	private final ModelPart bone3;
	private final ModelPart bone4;
	private final ModelPart bone5;
	private final ModelPart bone6;
	private final ModelPart bb_main;

	public FPVModel(ModelPart root) {
        super(root);
        this.bone = root.getChild("bone");
		this.bone2 = this.bone.getChild("bone2");
		this.bone7 = root.getChild("bone7");
		this.bone8 = this.bone7.getChild("bone8");
		this.bone3 = root.getChild("bone3");
		this.bone4 = this.bone3.getChild("bone4");
		this.bone5 = root.getChild("bone5");
		this.bone6 = this.bone5.getChild("bone6");
		this.bb_main = root.getChild("bb_main");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(-1.25F, 14.5F, 5.5F));

		PartDefinition cube_r1 = bone.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(40, 0).addBox(0.0F, -1.0F, -8.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(0, 32).addBox(0.0F, -1.0F, -8.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 0.0F, -1.0F, -2.8365F, -0.9733F, 2.7878F));

		PartDefinition bone2 = bone.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(32, 44).addBox(-0.5F, -0.3333F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(40, 10).addBox(-4.5F, -0.3333F, -0.5F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(11.25F, -1.6667F, 3.75F));

		PartDefinition cube_r2 = bone2.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(40, 11).addBox(-5.0F, 0.0F, 0.0F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -0.3333F, 0.5F, 0.0F, -1.5708F, 0.0F));

		PartDefinition bone7 = partdefinition.addOrReplaceChild("bone7", CubeListBuilder.create(), PartPose.offset(-1.25F, 14.5F, -5.5F));

		PartDefinition cube_r3 = bone7.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(40, 36).addBox(0.0F, -1.0F, -1.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(40, 26).addBox(0.0F, -1.0F, -1.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 0.0F, 1.0F, 2.8365F, 0.9733F, 2.7878F));

		PartDefinition bone8 = bone7.addOrReplaceChild("bone8", CubeListBuilder.create().texOffs(44, 46).addBox(-0.5F, -0.3333F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(18, 42).addBox(-4.5F, -0.3333F, -0.5F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.25F, -1.6667F, -3.75F, 0.0F, -0.3927F, 0.0F));

		PartDefinition cube_r4 = bone8.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(18, 43).addBox(-5.0F, 0.0F, -1.0F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -0.3333F, -0.5F, 0.0F, 1.5708F, 0.0F));

		PartDefinition bone3 = partdefinition.addOrReplaceChild("bone3", CubeListBuilder.create(), PartPose.offset(1.25F, 14.5F, 5.5F));

		PartDefinition cube_r5 = bone3.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(32, 16).addBox(-1.0F, -1.0F, -8.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, 0.0F, -1.0F, -2.8365F, 0.9733F, -2.7878F));

		PartDefinition bone4 = bone3.addOrReplaceChild("bone4", CubeListBuilder.create().texOffs(36, 44).addBox(-0.5F, -0.3333F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(40, 12).addBox(-4.5F, -0.3333F, -0.5F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-11.25F, -1.6667F, 3.75F));

		PartDefinition cube_r6 = bone4.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(40, 13).addBox(-4.0F, 0.0F, 0.0F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -0.3333F, 0.5F, 0.0F, 1.5708F, 0.0F));

		PartDefinition bone5 = partdefinition.addOrReplaceChild("bone5", CubeListBuilder.create(), PartPose.offset(1.25F, 14.5F, -5.5F));

		PartDefinition cube_r7 = bone5.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(20, 32).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, 0.0F, 1.0F, 2.8365F, -0.9733F, -2.7878F));

		PartDefinition bone6 = bone5.addOrReplaceChild("bone6", CubeListBuilder.create().texOffs(40, 46).addBox(-0.5F, -0.3333F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(40, 14).addBox(-4.5F, -0.3333F, -0.5F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.25F, -1.6667F, -3.75F, 0.0F, 0.3927F, 0.0F));

		PartDefinition cube_r8 = bone6.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(40, 15).addBox(-4.0F, 0.0F, -1.0F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -0.3333F, -0.5F, 0.0F, -1.5708F, 0.0F));

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -11.0F, -7.0F, 6.0F, 2.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(32, 47).addBox(-0.5F, -9.0F, -4.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(36, 47).addBox(-0.5F, -9.0F, 4.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 16).addBox(-0.5F, -8.0F, -8.0F, 1.0F, 1.0F, 15.0F, new CubeDeformation(0.0F))
		.texOffs(0, 42).addBox(-1.5F, -9.0F, -14.0F, 3.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(32, 26).addBox(-2.0F, -10.5F, -8.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(32, 29).addBox(1.0F, -10.5F, -8.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(18, 44).addBox(-0.5F, -8.0F, -15.25F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}
}