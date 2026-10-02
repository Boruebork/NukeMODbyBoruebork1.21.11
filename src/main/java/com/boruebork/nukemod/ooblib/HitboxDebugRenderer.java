package com.boruebork.nukemod.ooblib;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws each vehicle's OBB parts as wireframe boxes via the Gizmos API,
 * whenever vanilla's F3+B hitbox toggle is on.
 *
 * Hooked on ClientTickEvent rather than a render-stage event: Minecraft#tick()
 * (and therefore ClientHooks.fireClientTickPre/Post, which fires this event)
 * runs INSIDE the try-with-resources window opened by
 * Minecraft#collectPerTickGizmos() -- see Minecraft#runTick():
 *   try (Gizmos.TemporaryCollection tc = this.collectPerTickGizmos()) { this.tick(); }
 * so Gizmos.line(...) calls made here have a live GizmoCollector on this
 * thread and won't throw. Submitted gizmos persist for the following
 * render frame(s) via Minecraft#perTickGizmos / getPerTickGizmos(),
 * exactly like vanilla's own debug renderers.
 */
@EventBusSubscriber(value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class HitboxDebugRenderer {

    private static final int COLOR = 0xFF33FF66; // ARGB, distinguishable from vanilla's own hitbox lines
    private static final float LINE_WIDTH = 2.0f;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES)) return;
        if (mc.level == null) return;

        drawAllVehicleHitboxes(mc.level.entitiesForRendering());
    }

    private static void drawAllVehicleHitboxes(Iterable<Entity> entities) {
        for (Entity entity : entities) {
            if (!(entity instanceof HasHitboxParts hitboxOwner)) continue;
            for (OBB obb : hitboxOwner.computeWorldHitboxes()) {
                drawObb(obb);
            }
        }
    }

    private static void drawObb(OBB obb) {
        Vector3f he = obb.halfExtents;
        float x = he.x, y = he.y, z = he.z;

        // 8 local-space corners, rotated + translated into world space via
        // the OBB's own rotation -- Gizmos.line() just wants world-space
        // Vec3 endpoints directly, no PoseStack involved.
        Vec3[] corners = new Vec3[8];
        float[][] local = {
                {-x,-y,-z}, { x,-y,-z}, { x,-y, z}, {-x,-y, z}, // bottom face
                {-x, y,-z}, { x, y,-z}, { x, y, z}, {-x, y, z}, // top face
        };
        Quaternionf rot = obb.rotation;
        for (int i = 0; i < 8; i++) {
            Vector3f p = new Vector3f(local[i][0], local[i][1], local[i][2]);
            rot.transform(p);
            p.add(obb.center);
            corners[i] = new Vec3(p.x, p.y, p.z);
        }

        int[][] edges = {
                {0,1},{1,2},{2,3},{3,0}, // bottom
                {4,5},{5,6},{6,7},{7,4}, // top
                {0,4},{1,5},{2,6},{3,7}, // verticals
        };
        for (int[] edge : edges) {
            Gizmos.line(corners[edge[0]], corners[edge[1]], COLOR, LINE_WIDTH);
        }
    }

    private HitboxDebugRenderer() {}
}