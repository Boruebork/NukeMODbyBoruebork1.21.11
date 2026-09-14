package com.boruebork.nukemod.entity.custom.client.mushroom;// Made with Blockbench 5.1.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

public class MushroomModel<T extends EntityRenderState> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "mushroom_model"), "main");
	private final ModelPart stem;
	private final ModelPart hat;

	public MushroomModel(ModelPart root) {
        super(root);
        this.stem = root.getChild("stem");
		this.hat = root.getChild("hat");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition stem = partdefinition.addOrReplaceChild("stem", CubeListBuilder.create().texOffs(216, 214).addBox(-18.0F, -63.0F, -4.0F, 22.0F, 63.0F, 22.0F, new CubeDeformation(0.0F))
		.texOffs(216, 170).addBox(-26.0F, -40.0F, -13.0F, 39.0F, 2.0F, 42.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition cube_r1 = stem.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 248).addBox(-18.0F, -63.0F, -4.0F, 22.0F, 63.0F, 22.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 0.0F, 8.0F, 0.0F, -0.8727F, 0.0F));

		PartDefinition hat = partdefinition.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 0).addBox(-39.0F, -87.0F, -21.0F, 67.0F, 24.0F, 67.0F, new CubeDeformation(3.0F))
		.texOffs(0, 91).addBox(-35.0F, -94.0F, -18.0F, 60.0F, 20.0F, 59.0F, new CubeDeformation(3.0F))
		.texOffs(0, 170).addBox(-33.0F, -82.0F, -14.0F, 54.0F, 24.0F, 54.0F, new CubeDeformation(3.0F))
		.texOffs(238, 91).addBox(-33.0F, -84.0F, -24.0F, 56.0F, 20.0F, 3.0F, new CubeDeformation(3.0F))
		.texOffs(238, 114).addBox(-33.0F, -84.0F, 46.0F, 56.0F, 20.0F, 3.0F, new CubeDeformation(3.0F)), PartPose.offset(-1.25F, 19.0F, -6.0F));

		PartDefinition cube_r2 = hat.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(238, 137).addBox(-24.0F, -20.0F, 23.0F, 54.0F, 20.0F, 3.0F, new CubeDeformation(3.0F)), PartPose.offsetAndRotation(-64.0F, -64.0F, 15.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r3 = hat.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(88, 248).addBox(-24.0F, -20.0F, 23.0F, 53.0F, 20.0F, 3.0F, new CubeDeformation(3.0F)), PartPose.offsetAndRotation(4.0F, -64.0F, 14.0F, 0.0F, 1.5708F, 0.0F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}
}