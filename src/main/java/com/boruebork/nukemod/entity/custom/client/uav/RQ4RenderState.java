package com.boruebork.nukemod.entity.custom.client.uav;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.phys.Vec3;

public class RQ4RenderState extends EntityRenderState {
    public float yRot;
    public float xRot;
    public float zRot;
    public final ItemStackRenderState body = new ItemStackRenderState();
}
