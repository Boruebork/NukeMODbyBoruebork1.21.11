package com.boruebork.nukemod.entity.custom.client.radar.basic_radar;

import com.boruebork.nukemod.entity.custom.radar.BasicRadar;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class BasicRadarRenderer extends EntityRenderer<BasicRadar, EntityRenderState> {
    public BasicRadarRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
