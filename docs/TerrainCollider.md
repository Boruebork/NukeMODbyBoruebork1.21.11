# Terrain Collision System

## Overview

`TerrainCollider` handles collision detection and movement resolution between an aircraft and Minecraft terrain.

Unlike ordinary Minecraft entities, aircraft use multiple oriented bounding boxes (OBBs) to represent their physical geometry. These hitboxes rotate with the aircraft.

The collider is responsible for:
- Detecting terrain intersections.
- Finding the maximum safe movement distance.
- Resolving movement separately along each world axis.
- Recovering aircraft that begin a movement tick inside terrain.

## Collision Detection

The main collision detection method is `scan()`.

It uses a two-phase collision detection process:

1. **Broad phase:** Convert every aircraft OBB into an AABB and calculate a combined AABB enclosing all aircraft hitboxes. Scan only Minecraft blocks inside this region.
2. **Narrow phase:** For each potentially intersecting block collision shape, perform an actual OBB-vs-OBB intersection test.

Minecraft block collision shapes can contain multiple AABBs, so each shape is processed individually.

`collidesAt()` checks whether a given aircraft pose overlaps terrain, using slightly shrunken hitboxes.

`findContact()` identifies the first actual terrain contact at a given pose, without shrinking hitboxes, and returns the aircraft hitbox part and block involved.

Unloaded chunks are treated as free space by the current implementation.

## Movement Resolution

`move()` receives:
- The current position.
- The desired displacement.
- The aircraft rotation.

Movement is resolved in Y → X → Z order.

Before normal movement, the collider checks whether the aircraft is already inside terrain. If so, it attempts to recover by moving the aircraft upward in increments of `UNSTICK_STEP`, up to `UNSTICK_MAX`.

If no safe position is found, the original position is returned and the aircraft is marked as stuck.

## Axis Clipping

`clip()` determines how far the aircraft can move along one axis without colliding.

If the destination is safe, the entire requested displacement is accepted.

Otherwise, binary search is used to find the largest safe fraction of the requested movement. The resulting displacement is reduced by `SKIN` to maintain a small separation from terrain.

The binary search performs 10 iterations, providing a movement fraction resolution of approximately 1/1024.

## Collision Result

`Result` contains the new position and collision flags:

- `vertical`: vertical movement was blocked.
- `horizontal`: horizontal movement was blocked.
- `onGround`: downward movement was blocked.
- `flagsValid`: indicates whether the collision flags represent a normal movement calculation.

## Important Assumptions

- `clip()` expects a non-colliding starting position.
- Normal movement uses axis-separated collision resolution.
- The unstick procedure only searches upward.
- Unloaded chunks are currently treated as free space.
- Collision detection uses OBBs for accurate geometry and AABBs for broad-phase optimization.