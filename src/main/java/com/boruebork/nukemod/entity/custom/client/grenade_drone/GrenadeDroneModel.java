package com.boruebork.nukemod.entity.custom.client.grenade_drone;// Made with Blockbench 5.1.6
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.client.fpvint.FPVInterceptorRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class GrenadeDroneModel extends EntityModel<GrenadeDroneRenderState> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "grendade"), "main");
	private final ModelPart bone;
	private final ModelPart left_back_rotor;
	private final ModelPart bone7;
	private final ModelPart left_front_rotor;
	private final ModelPart bone3;
	private final ModelPart right_back_rotor;
	private final ModelPart bone5;
	private final ModelPart right_front_rotor;
	private final ModelPart grenade1;
	private final ModelPart grenade2;
	private final ModelPart bb_main;

	public GrenadeDroneModel(ModelPart root) {
        super(root);
        this.bone = root.getChild("bone");
		this.left_back_rotor = this.bone.getChild("left_back_rotor");
		this.bone7 = root.getChild("bone7");
		this.left_front_rotor = this.bone7.getChild("left_front_rotor");
		this.bone3 = root.getChild("bone3");
		this.right_back_rotor = this.bone3.getChild("right_back_rotor");
		this.bone5 = root.getChild("bone5");
		this.right_front_rotor = this.bone5.getChild("right_front_rotor");
		this.grenade1 = root.getChild("grenade1");
		this.grenade2 = root.getChild("grenade2");
		this.bb_main = root.getChild("bb_main");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(-1.25F, 14.5F, 5.5F));

		PartDefinition cube_r1 = bone.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(20, 26).addBox(0.0F, -1.0F, -8.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(0, 16).addBox(0.0F, -1.0F, -8.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 0.0F, -1.0F, -2.8365F, -0.9733F, 2.7878F));

		PartDefinition left_back_rotor = bone.addOrReplaceChild("left_back_rotor", CubeListBuilder.create().texOffs(40, 34).addBox(-0.5F, -0.3333F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(40, 0).addBox(-4.5F, -0.3333F, -0.5F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(11.25F, -1.6667F, 3.75F));

		PartDefinition cube_r2 = left_back_rotor.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(40, 1).addBox(-5.0F, 0.0F, 0.0F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -0.3333F, 0.5F, 0.0F, -1.5708F, 0.0F));

		PartDefinition bone7 = partdefinition.addOrReplaceChild("bone7", CubeListBuilder.create(), PartPose.offset(-1.25F, 14.5F, -5.5F));

		PartDefinition cube_r3 = bone7.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(20, 36).addBox(0.0F, -1.0F, -1.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(0, 36).addBox(0.0F, -1.0F, -1.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 0.0F, 1.0F, 2.8365F, 0.9733F, 2.7878F));

		PartDefinition left_front_rotor = bone7.addOrReplaceChild("left_front_rotor", CubeListBuilder.create().texOffs(40, 43).addBox(-0.5F, -0.3333F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(40, 6).addBox(-4.5F, -0.3333F, -0.5F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.25F, -1.6667F, -3.75F, 0.0F, -0.3927F, 0.0F));

		PartDefinition cube_r4 = left_front_rotor.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(40, 7).addBox(-5.0F, 0.0F, -1.0F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -0.3333F, -0.5F, 0.0F, 1.5708F, 0.0F));

		PartDefinition bone3 = partdefinition.addOrReplaceChild("bone3", CubeListBuilder.create(), PartPose.offset(1.25F, 14.5F, 5.5F));

		PartDefinition cube_r5 = bone3.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(20, 16).addBox(-1.0F, -1.0F, -8.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, 0.0F, -1.0F, -2.8365F, 0.9733F, -2.7878F));

		PartDefinition right_back_rotor = bone3.addOrReplaceChild("right_back_rotor", CubeListBuilder.create().texOffs(40, 37).addBox(-0.5F, -0.3333F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(40, 2).addBox(-4.5F, -0.3333F, -0.5F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-11.25F, -1.6667F, 3.75F));

		PartDefinition cube_r6 = right_back_rotor.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(40, 3).addBox(-4.0F, 0.0F, 0.0F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -0.3333F, 0.5F, 0.0F, 1.5708F, 0.0F));

		PartDefinition bone5 = partdefinition.addOrReplaceChild("bone5", CubeListBuilder.create(), PartPose.offset(1.25F, 14.5F, -5.5F));

		PartDefinition cube_r7 = bone5.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(0, 26).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, 0.0F, 1.0F, 2.8365F, -0.9733F, -2.7878F));

		PartDefinition right_front_rotor = bone5.addOrReplaceChild("right_front_rotor", CubeListBuilder.create().texOffs(40, 40).addBox(-0.5F, -0.3333F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(40, 4).addBox(-4.5F, -0.3333F, -0.5F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.25F, -1.6667F, -3.75F, 0.0F, 0.3927F, 0.0F));

		PartDefinition cube_r8 = right_front_rotor.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(40, 5).addBox(-4.0F, 0.0F, -1.0F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -0.3333F, -0.5F, 0.0F, -1.5708F, 0.0F));

		PartDefinition grenade1 = partdefinition.addOrReplaceChild("grenade1", CubeListBuilder.create().texOffs(40, 8).addBox(-1.0F, -1.0F, -1.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(44, 34).addBox(0.0F, -2.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, 17.0F, 0.0F));

		PartDefinition grenade2 = partdefinition.addOrReplaceChild("grenade2", CubeListBuilder.create().texOffs(40, 14).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(44, 36).addBox(-0.5F, -1.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(4.5F, 16.5F, 0.5F));

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -11.0F, -7.0F, 6.0F, 2.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(40, 28).addBox(-2.0F, -10.5F, -8.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(40, 31).addBox(1.0F, -10.5F, -8.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(40, 20).addBox(3.0F, -10.0F, -1.0F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(40, 24).addBox(-6.0F, -10.0F, -1.0F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(GrenadeDroneRenderState renderState) {
		super.setupAnim(renderState);
		this.left_front_rotor.yRot  = renderState.rotorAngle * Mth.DEG_TO_RAD;
		this.right_front_rotor.yRot = -renderState.rotorAngle * Mth.DEG_TO_RAD; // opposite spin looks better
		this.left_back_rotor.yRot   = renderState.rotorAngle * Mth.DEG_TO_RAD;
		this.right_back_rotor.yRot  = -renderState.rotorAngle * Mth.DEG_TO_RAD;
		this.grenade1.visible = renderState.grenade1;
		this.grenade2.visible = renderState.grenade2;
	}
}