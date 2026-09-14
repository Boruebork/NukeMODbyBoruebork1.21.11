package com.boruebork.nukemod.entity.custom.client.nuke;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;

public class NukeModel extends EntityModel<NukeRenderState> {
    // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "nuke"), "main");
    private final ModelPart bb_main;

    public NukeModel(ModelPart root) {
        super(root);
        this.bb_main = root.getChild("main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bb_main = partdefinition.addOrReplaceChild("main", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.0F, -7.0F, 2.0F, 2.0F, 13.0F, new CubeDeformation(0.0F))
                .texOffs(0, 17).addBox(-1.0F, -2.0F, -8.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 15).addBox(-7.0F, 0.0F, 0.0F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(16, 15).addBox(1.0F, 0.0F, 0.0F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(6, 17).addBox(-0.5F, -1.5F, 6.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }
}
