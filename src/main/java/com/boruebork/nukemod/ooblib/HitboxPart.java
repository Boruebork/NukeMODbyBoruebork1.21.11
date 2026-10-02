package com.boruebork.nukemod.ooblib;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A single named part of a vehicle's hitbox, defined once in the
 * vehicle's OWN local space (its unrotated, "identity orientation" frame).
 *
 * These never change at runtime and never need to be synced -- only the
 * entity's base position/rotation does, and you already sync that.
 */
public record HitboxPart(
        String name,              // "hull", "turret", "left_wing", ...
        Vector3f localOffset,     // center of this part, relative to entity origin
        Vector3f localHalfExtents,
        Quaternionf localRotation // usually identity; non-identity for e.g. an angled turret mantlet
) {
    public static HitboxPart of(String name, float ox, float oy, float oz, float hx, float hy, float hz) {
        return new HitboxPart(name, new Vector3f(ox, oy, oz), new Vector3f(hx, hy, hz), new Quaternionf());
    }
}

