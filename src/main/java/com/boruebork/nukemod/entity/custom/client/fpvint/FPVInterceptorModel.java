package com.boruebork.nukemod.entity.custom.client.fpvint;// Made with Blockbench 5.1.6
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class FPVInterceptorModel extends EntityModel<FPVInterceptorRenderState> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "fpvinterceptor"), "main");
	private final ModelPart bone;
	private final ModelPart left_back_rotor;
	private final ModelPart bone7;
	private final ModelPart left_front_rotor;
	private final ModelPart bone3;
	private final ModelPart right_back_rotor;
	private final ModelPart bone5;
	private final ModelPart right_front_rotor;
	private final ModelPart bb_main;

	public FPVInterceptorModel(ModelPart root) {
        super(root);
        this.bone = root.getChild("bone");
		this.left_back_rotor = this.bone.getChild("bone2");
		this.bone7 = root.getChild("bone7");
		this.left_front_rotor = this.bone7.getChild("bone8");
		this.bone3 = root.getChild("bone3");
		this.right_back_rotor = this.bone3.getChild("bone4");
		this.bone5 = root.getChild("bone5");
		this.right_front_rotor = this.bone5.getChild("bone6");
		this.bb_main = root.getChild("bb_main");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(-1.25F, 14.5F, 5.5F));

		PartDefinition cube_r1 = bone.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(40, 20).addBox(0.0F, -1.0F, -8.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(32, 36).addBox(0.0F, -1.0F, -8.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 0.0F, -1.0F, -2.8365F, -0.9733F, 2.7878F));

		PartDefinition bone2 = bone.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(20, 52).addBox(-0.5F, -0.3333F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(40, 30).addBox(-4.5F, -0.3333F, -0.5F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(11.25F, -1.6667F, 3.75F));

		PartDefinition cube_r2 = bone2.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(40, 31).addBox(-5.0F, 0.0F, 0.0F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -0.3333F, 0.5F, 0.0F, -1.5708F, 0.0F));

		PartDefinition bone7 = partdefinition.addOrReplaceChild("bone7", CubeListBuilder.create(), PartPose.offset(-1.25F, 14.5F, -5.5F));

		PartDefinition cube_r3 = bone7.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 49).addBox(0.0F, -1.0F, -1.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(32, 46).addBox(0.0F, -1.0F, -1.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 0.0F, 1.0F, 2.8365F, 0.9733F, 2.7878F));

		PartDefinition bone8 = bone7.addOrReplaceChild("bone8", CubeListBuilder.create().texOffs(52, 38).addBox(-0.5F, -0.3333F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(52, 36).addBox(-4.5F, -0.3333F, -0.5F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.25F, -1.6667F, -3.75F, 0.0F, -0.3927F, 0.0F));

		PartDefinition cube_r4 = bone8.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(52, 37).addBox(-5.0F, 0.0F, -1.0F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -0.3333F, -0.5F, 0.0F, 1.5708F, 0.0F));

		PartDefinition bone3 = partdefinition.addOrReplaceChild("bone3", CubeListBuilder.create(), PartPose.offset(1.25F, 14.5F, 5.5F));

		PartDefinition cube_r5 = bone3.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, -1.0F, -8.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, 0.0F, -1.0F, -2.8365F, 0.9733F, -2.7878F));

		PartDefinition bone4 = bone3.addOrReplaceChild("bone4", CubeListBuilder.create().texOffs(24, 52).addBox(-0.5F, -0.3333F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(40, 32).addBox(-4.5F, -0.3333F, -0.5F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-11.25F, -1.6667F, 3.75F));

		PartDefinition cube_r6 = bone4.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(40, 33).addBox(-4.0F, 0.0F, 0.0F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -0.3333F, 0.5F, 0.0F, 1.5708F, 0.0F));

		PartDefinition bone5 = partdefinition.addOrReplaceChild("bone5", CubeListBuilder.create(), PartPose.offset(1.25F, 14.5F, -5.5F));

		PartDefinition cube_r7 = bone5.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(40, 10).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, 0.0F, 1.0F, 2.8365F, -0.9733F, -2.7878F));

		PartDefinition bone6 = bone5.addOrReplaceChild("bone6", CubeListBuilder.create().texOffs(28, 52).addBox(-0.5F, -0.3333F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(40, 34).addBox(-4.5F, -0.3333F, -0.5F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.25F, -1.6667F, -3.75F, 0.0F, 0.3927F, 0.0F));

		PartDefinition cube_r8 = bone6.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(40, 35).addBox(-4.0F, 0.0F, -1.0F, 9.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -0.3333F, -0.5F, 0.0F, -1.5708F, 0.0F));

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 20).addBox(-3.0F, -11.0F, -7.0F, 6.0F, 2.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(20, 49).addBox(-2.0F, -10.5F, -8.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(26, 49).addBox(1.0F, -10.5F, -8.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 36).addBox(-2.0F, -12.0F, -6.0F, 4.0F, 1.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(FPVInterceptorRenderState renderState) {
		super.setupAnim(renderState);
		this.left_front_rotor.yRot  = renderState.rotorAngle * Mth.DEG_TO_RAD;
		this.right_front_rotor.yRot = -renderState.rotorAngle * Mth.DEG_TO_RAD; // opposite spin looks better
		this.left_back_rotor.yRot   = renderState.rotorAngle * Mth.DEG_TO_RAD;
		this.right_back_rotor.yRot  = -renderState.rotorAngle * Mth.DEG_TO_RAD;
	}
}