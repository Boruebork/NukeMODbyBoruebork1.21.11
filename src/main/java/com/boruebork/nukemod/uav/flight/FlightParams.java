package com.boruebork.nukemod.uav.flight;

public record FlightParams(float mass, float maxThrust, float wingArea, float throttleRate,
                           float maxPitchRate, float yawRate, float maxRoll, float rollFactor,
                           float rollStep, float maxSpeed) {}
