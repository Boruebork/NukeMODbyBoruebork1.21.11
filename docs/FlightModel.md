# Flight Model

## Overview

`FlightModel` is the central aircraft physics simulation.

It is a stateless simulation class: it receives the current `FlightState`, pilot inputs, aircraft parameters and a movement implementation, then calculates the next state.

It does not directly depend on Minecraft entities or world access.

The simulation is divided into six stages:

1. Throttle integration.
2. Attitude calculation.
3. Body-axis calculation.
4. Aerodynamic force calculation.
5. Velocity integration.
6. Collision and movement resolution.

## Inputs and Outputs

`step(FlightState, Sticks, FlightParams, Mover)` receives:

- `FlightState`: current position, velocity, orientation, throttle and ground state.
- `Sticks`: pilot control inputs.
- `FlightParams`: aircraft-specific physical and control parameters.
- `Mover`: interface used to resolve movement and terrain collisions.

It returns a `FlightModel.Result` containing the new state and collision information.

## Attitude

Pitch, yaw and roll are calculated independently.

Ground pitch uses a simplified rotational model with elevator acceleration, gravity-related acceleration, damping and angular-rate limits.

Airborne pitch uses direct rate-based control.

Yaw and roll authority are scaled by a speed-dependent control gate.

Roll combines automatic banking from yaw input with direct roll input.

## Body Axes

The model calculates forward, up and right vectors using yaw and pitch.

These vectors define the aircraft's local orientation in world space and are used to calculate aerodynamic forces.

The current implementation does not include roll in the aerodynamic body axes.

## Aerodynamic Forces

The model calculates:

- Thrust: acts along the forward axis.
- Weight: acts downward in world space.
- Lift: acts perpendicular to the airflow, approximately along the aircraft's up direction.
- Drag: acts opposite the velocity.
- Side force: approximates a restoring force against sideslip.
- Brake force: opposes horizontal velocity while grounded and braking.

Dynamic pressure is calculated using air density, speed squared and wing area.

Lift uses a simplified linear angle-of-attack model with a stall-angle clamp. Drag includes base and induced components.

## Velocity Integration

All forces are summed to obtain net force.

Newton's second law is used to calculate acceleration, which is integrated into velocity.

The resulting velocity is capped by the aircraft's maximum speed.

The current simulation assumes velocity is expressed in blocks per tick, with one simulation step representing one tick.

## Movement and Collision

The model passes the desired displacement and aircraft rotation to the supplied `Mover`.

The returned position determines actual movement.

If movement is blocked along an axis, the corresponding velocity component is set to zero.

The model also enforces horizontal world boundaries and updates the grounded state when collision flags are valid.

## Design Principles

- The flight simulation is independent of Minecraft entity classes.
- The same simulation method can be used on both client and server.
- The simulation produces a new state instead of directly mutating the input state.
- Collision handling is delegated through the `Mover` interface.
- Aircraft-specific behavior is configured through `FlightParams`.