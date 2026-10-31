package com.boruebork.nukemod.uav.flight;

import com.boruebork.nukemod.uav.collision.TerrainCollider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import static com.boruebork.nukemod.ooblib.FixedWingUtil.*;

public final class FlightModel {
    private FlightModel() {}

    @FunctionalInterface
    public interface Mover { TerrainCollider.Result move(Vec3 from, Vec3 desired, Quaternionf rot); }

    public record Result(FlightState state, boolean vertical, boolean horizontal,
                         boolean flagsValid, Vec3 moved) {}

    private static final float  COM_HEIGHT = 0.18f, ELEVATOR_PITCH_ACCEL = 3.0f,
            GROUND_PITCH_DAMPING = 0.80f, ELEVATOR_FULL_AUTHORITY_SPEED = 0.5f;
    private static final double CONTROL_FULL_SPEED = 0.3, BRAKE_DECEL = 0.02,
            TAIL_AREA_RATIO = 0.3, CN_BETA = 4.0, WORLD_LIMIT = 29_999_000;

    public static Result step(FlightState s, Sticks in, FlightParams p, Mover mover) {
        // 1. throttle integrator
        float throttle = Mth.clamp(s.throttle() + in.throttle() * p.throttleRate(), 0f, 1f);

        Vec3 vel = s.vel();
        double speed = vel.length();
        float gate = (float) Mth.clamp(speed / CONTROL_FULL_SPEED, 0.0, 1.0);

        // 2. attitude
        float pitch = s.pitch();
        float groundRate = s.groundPitchRate();
        if (s.onGround()) {
            float auth = Mth.clamp((float) (speed / ELEVATOR_FULL_AUTHORITY_SPEED), 0f, 1f);
            float alphaElevator = in.pitch() * ELEVATOR_PITCH_ACCEL * auth;
            double alphaGravRad = -GRAVITY * Math.sin(Math.toRadians(pitch)) / COM_HEIGHT;
            groundRate += alphaElevator + (float) Math.toDegrees(alphaGravRad);
            groundRate *= GROUND_PITCH_DAMPING;
            groundRate = Mth.clamp(groundRate, -15f, 15f);
            pitch = Mth.clamp(pitch + groundRate, -90f, 90f);
        } else {
            pitch = Mth.clamp(pitch + in.pitch() * p.maxPitchRate() * gate, -90f, 90f);
            groundRate = 0f;
        }
        float yaw = s.yaw() + in.yaw() * p.yawRate() * gate;

        float rollTarget = -Mth.clamp(-in.yaw() * p.rollFactor() + in.roll() * p.maxRoll(),
                -p.maxRoll(), p.maxRoll());
        float rollStep = Mth.clamp(rollTarget - s.roll(), -p.rollStep(), p.rollStep());
        float roll = s.roll() + rollStep * gate;

        // 3. body axes
        float yawRad = yaw * Mth.DEG_TO_RAD, pitchRad = pitch * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(Mth.sin(-yawRad) * Mth.cos(pitchRad), Mth.sin(-pitchRad), Mth.cos(-yawRad) * Mth.cos(pitchRad));
        Vec3 up  = new Vec3(Mth.sin(-yawRad) * Mth.sin(pitchRad), Mth.cos(pitchRad), Mth.cos(-yawRad) * Mth.sin(pitchRad));
        Vec3 right = fwd.cross(up).normalize();

        // 4. forces
        Vec3 brake = Vec3.ZERO;
        if (s.onGround() && in.throttle() < -0.05f) {
            Vec3 h = new Vec3(vel.x, 0, vel.z);
            double hs = h.length();
            if (hs > 1e-5) brake = h.scale(-Math.min(BRAKE_DECEL, hs) * p.mass() / hs);
        }

        Vec3 side = Vec3.ZERO, lift = Vec3.ZERO, drag = Vec3.ZERO;
        if (speed > 1e-4) {
            double q = 0.5 * AIR_DENSITY * speed * speed * p.wingArea();
            side = right.scale(-(q * TAIL_AREA_RATIO * CN_BETA * (vel.dot(right) / speed)));

            Vec3 vDir = vel.scale(1.0 / speed);
            double alpha = Math.asin(Mth.clamp(-vDir.dot(up), -1.0, 1.0));
            double alphaClamped = Mth.clamp(alpha, -STALL_ANGLE, STALL_ANGLE);

            Vec3 liftDir = up.subtract(vDir.scale(vDir.dot(up)));
            double len = liftDir.length();
            liftDir = len > 1e-4 ? liftDir.scale(1.0 / len) : Vec3.ZERO;

            lift = liftDir.scale(q * LIFT_SLOPE * alphaClamped);
            drag = vDir.scale(-q * (DRAG_BASE + DRAG_INDUCED * alpha * alpha));
        }
        Vec3 thrust = fwd.scale(p.maxThrust() * throttle);
        Vec3 weight = new Vec3(0, -p.mass() * GRAVITY, 0);

        // 5. integrate + cap
        Vec3 net = thrust.add(lift).add(drag).add(weight).add(side).add(brake);
        Vec3 newVel = vel.add(net.scale(1.0 / p.mass()));
        double spd = newVel.length();
        if (spd > p.maxSpeed()) newVel = newVel.scale(p.maxSpeed() / spd);

        // 6. move + resolve
        TerrainCollider.Result r = mover.move(s.pos(), newVel, FlightState.rotation(yaw, pitch, roll));
        Vec3 moved = r.newPos().subtract(s.pos());
        Vec3 blocked = newVel.subtract(moved);
        if (Math.abs(blocked.x) > 1e-4) newVel = new Vec3(0, newVel.y, newVel.z);
        if (Math.abs(blocked.y) > 1e-4) newVel = new Vec3(newVel.x, 0, newVel.z);
        if (Math.abs(blocked.z) > 1e-4) newVel = new Vec3(newVel.x, newVel.y, 0);
        if (Math.abs(r.newPos().x + newVel.x) > WORLD_LIMIT || Math.abs(r.newPos().z + newVel.z) > WORLD_LIMIT)
            newVel = new Vec3(0, newVel.y, 0);

        boolean onGround = r.flagsValid() ? r.onGround() : s.onGround();
        return new Result(new FlightState(r.newPos(), newVel, yaw, pitch, roll, throttle, groundRate, onGround),
                r.vertical(), r.horizontal(), r.flagsValid(), moved);
    }
}
