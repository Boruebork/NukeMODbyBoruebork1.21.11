package com.boruebork.nukemod.block.entity.renderer;// Made with Blockbench 5.1.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class GuidedlauncherModel extends Model<GuidedMissilelauncherBERenderstate> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(NukeModbyBoruebork.identifierFromPath("guided_launcher_converted"), "main");
	private final ModelPart rotator;
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart bone;

	public GuidedlauncherModel(ModelPart root) {
        super(root, RenderTypes::entitySolid);
        this.rotator = root.getChild("rotator");
		this.head = this.rotator.getChild("head");
		this.body = root.getChild("body");
		this.bone = root.getChild("bone");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition rotator = partdefinition.addOrReplaceChild("rotator", CubeListBuilder.create().texOffs(0, 41).addBox(-4.0F, 1.5F, -5.25F, 10.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
				.texOffs(44, 23).addBox(5.0F, -5.5F, -1.25F, 1.0F, 7.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(40, 45).addBox(-4.0F, -5.5F, -1.25F, 1.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 12.5F, 0.25F));

		PartDefinition head = rotator.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 23).addBox(-4.0F, -3.0F, -8.5F, 8.0F, 4.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, -3.5F, 0.25F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -8.0F, 0.0F, 16.0F, 8.0F, 15.0F, new CubeDeformation(0.0F))
				.texOffs(48, 45).addBox(-2.0F, -8.0F, 15.0F, 2.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(52, 23).addBox(-16.0F, -8.0F, 15.0F, 2.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(40, 41).addBox(-14.0F, -8.0F, 15.0F, 12.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(40, 43).addBox(-14.0F, -1.0F, 15.0F, 12.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(8.0F, 24.0F, -8.0F));

		PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(44, 33).addBox(-1.5F, -4.0F, 16.0F, 1.0F, 1.0F, 0.5F, new CubeDeformation(0.0F)), PartPose.offset(8.0F, 24.0F, -8.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	public void setupAnim(GuidedMissilelauncherBERenderstate state) {

		rotator.yRot = (float)Math.toRadians(state.rotatorY);

		head.xRot = (float)Math.toRadians(state.headX);
	}
}