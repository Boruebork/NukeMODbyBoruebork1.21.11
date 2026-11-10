package com.boruebork.nukemod.uav.flight;

import com.boruebork.nukemod.uav.collision.TerrainCollider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import static com.boruebork.nukemod.ooblib.FixedWingUtil.*;

public final class FlightModel {
    private FlightModel() {}

    public interface Mover {
        /** Pushes the body out of terrain if it overlaps; returns the corrected position. */
        Vec3 depenetrate(Quaternionf rot, Vec3 pos);
        TerrainCollider.Sweep translate(Vec3 from, Vec3 desired, Quaternionf rot);
        TerrainCollider.RotResult rotate(Vec3 from, float[] fromAngles, float[] toAngles, boolean allowLift);
        /** True if terrain is a hair below the body (used to keep "on ground" steady while lift ~ weight at take-off). */
        boolean supported(Quaternionf rot, Vec3 pos);
    }

    /**
     * impact = speed (blocks/tick) that was driven into a surface this tick. A parked aircraft reports
     * about GRAVITY every tick, so use a threshold well above that (e.g. 0.3) for crash damage.
     */
    public record Result(FlightState state, boolean vertical, boolean horizontal,
                         boolean flagsValid, Vec3 moved, double impact) {}

    private static final float  COM_HEIGHT = 0.18f, ELEVATOR_PITCH_ACCEL = 3.0f,
            GROUND_PITCH_DAMPING = 0.80f, ELEVATOR_FULL_AUTHORITY_SPEED = 0.5f;
    private static final double CONTROL_FULL_SPEED = 0.3, BRAKE_DECEL = 0.02,
            TAIL_AREA_RATIO = 0.3, CN_BETA = 4.0, WORLD_LIMIT = 29_999_000;

    // ---- contact model: friction budgets are multiples of the normal speed removed in a tick (Coulomb: f <= mu * N) ----
    /** Tyres: sideways grip. Higher = the aircraft follows its nose on the ground more tightly. */
    private static final double MU_LATERAL = 1.5;
    /** Tyres: rolling resistance. */
    private static final double MU_ROLLING = 0.02;
    /** Scraping along a wall or ceiling. */
    private static final double MU_WALL = 0.35;
    /** Airborne: fraction of the sideways velocity (relative to the nose) bled off per tick. 0 disables. */
    private static final double AIR_SIDESLIP_DAMP = 0.03;
    /** Upward speed below which a body resting on the ground still counts as on the ground (take-off hand-over). */
    private static final double LIFTOFF_VY = 0.01;

    /** main movement method */
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

        // 4. forces. Gravity is ALWAYS applied: the ground answers through the contact response (step 8),
        //    so there is no "normal force" special case and no onGround-dependent branch in the force sum.
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
        Vec3 net = thrust.add(lift).add(drag).add(weight).add(side);
        Vec3 newVel = vel.add(net.scale(1.0 / p.mass()));
        // the velocity is a free world vector that the yaw does not drag along, so bleed the part that is
        // sideways relative to the nose (on the ground the tyres do this instead, see resolveContacts)
        if (!s.onGround()) newVel = newVel.subtract(right.scale(newVel.dot(right) * AIR_SIDESLIP_DAMP));
        if (!isFinite(newVel)) newVel = Vec3.ZERO;
        double spd = newVel.length();
        if (spd > p.maxSpeed()) newVel = newVel.scale(p.maxSpeed() / spd);

        // 6. rotation, resolved before translation, from a pose that is guaranteed to be free
        Vec3 pos = mover.depenetrate(FlightState.rotation(s.yaw(), s.pitch(), s.roll()), s.pos());

        // One axis per stage, each from the pose the previous stage left free. They must NOT share one
        // fraction: on the ground a roll digs a wheel in, and a shared fraction would freeze the yaw with it.
        // stage 1: yaw. Blocked by e.g. a wing tip against a wall.
        TerrainCollider.RotResult ya = mover.rotate(pos,
                new float[]{s.yaw(), s.pitch(), s.roll()}, new float[]{yaw, s.pitch(), s.roll()}, false);
        float yawN = Mth.lerp((float) ya.t(), s.yaw(), yaw);

        // stage 2: roll. Never lifts, so a wing on the ground blocks the roll.
        TerrainCollider.RotResult ra = mover.rotate(ya.pos(),
                new float[]{yawN, s.pitch(), s.roll()}, new float[]{yawN, s.pitch(), roll}, false);
        float rollN = Mth.lerp((float) ra.t(), s.roll(), roll);

        // stage 3: pitch. On the ground the wheels are allowed to pivot, so lift is allowed here only.
        TerrainCollider.RotResult b = mover.rotate(ra.pos(),
                new float[]{yawN, s.pitch(), rollN}, new float[]{yawN, pitch, rollN}, s.onGround());
        float pitchN = Mth.lerp((float) b.t(), s.pitch(), pitch);
        if (b.t() < 1.0) groundRate = 0f;           // pitch was blocked: kill the angular rate
        pos = b.pos();

        // 7. translation
        Quaternionf rot = FlightState.rotation(yawN, pitchN, rollN);
        TerrainCollider.Sweep sw = mover.translate(pos, newVel, rot);
        Vec3 moved = sw.pos().subtract(pos);

        // 8. contact response
        Resolved c = resolveContacts(newVel, sw, yawN, in.throttle() < -0.05f);
        newVel = c.vel();
        Vec3 end = sw.pos();
        if (Math.abs(end.x + newVel.x) > WORLD_LIMIT || Math.abs(end.z + newVel.z) > WORLD_LIMIT)
            newVel = new Vec3(0, newVel.y, 0);

        // Around lift-off lift ~ weight, nothing pushes into the floor and the sweep is no longer stopped.
        // Keep the flag steady until the aircraft is really climbing, instead of blinking for a few ticks.
        boolean onGround = c.onGround() || (s.onGround() && newVel.y <= LIFTOFF_VY && mover.supported(rot, end));

        return new Result(
                new FlightState(end, newVel, yawN, pitchN, rollN, throttle, groundRate, onGround),
                sw.blockedY(), sw.blockedX() || sw.blockedZ(), true, moved, c.impact());
    }

    private record Resolved(Vec3 vel, boolean onGround, double impact) {}

    /**
     * Velocity response after a sweep. It is inelastic: it only ever REMOVES velocity, never adds any.
     *  1. the part of the velocity that points into a surface the sweep was stopped by is deleted;
     *     the size of that part is the "normal impulse" N for this tick
     *  2. friction removes at most mu * N from what is left
     * Block faces are axis aligned, so "the blocked axis" is the contact normal.
     *
     * Because gravity is applied every tick, a parked aircraft is pushed into the floor at speed GRAVITY each
     * tick, which gives exactly mu * GRAVITY of friction budget. That is also why onGround is simply
     * "the floor stopped a downward move" (plus the take-off hand-over in step()), instead of a flag
     * derived from last tick's velocity, which is what made it blink.
     */
    private static Resolved resolveContacts(Vec3 v, TerrainCollider.Sweep c, float yawDeg, boolean braking) {
        double vx = v.x, vy = v.y, vz = v.z;
        double nFloor = 0, nWall = 0;

        if (c.blockedY()) { if (vy < 0) nFloor = -vy; else nWall += vy; vy = 0; }
        if (c.blockedX()) { nWall += Math.abs(vx); vx = 0; }
        if (c.blockedZ()) { nWall += Math.abs(vz); vz = 0; }

        if (nFloor > 0) {
            // Wheels: anisotropic friction on the ground plane. Hard to push sideways, easy to roll forwards.
            // |v| <= budget is removed completely (static friction, no jitter); above the budget it skids.
            float yawRad = yawDeg * Mth.DEG_TO_RAD;
            double fx = Mth.sin(-yawRad), fz = Mth.cos(-yawRad);     // nose direction on the ground plane
            double rx = -fz, rz = fx;                                // wing direction on the ground plane
            double vLat = vx * rx + vz * rz, vFwd = vx * fx + vz * fz;
            double dLat = Math.copySign(Math.min(Math.abs(vLat), MU_LATERAL * nFloor), vLat);
            double dFwd = Math.copySign(Math.min(Math.abs(vFwd),
                    MU_ROLLING * nFloor + (braking ? BRAKE_DECEL : 0)), vFwd);
            vx -= rx * dLat + fx * dFwd;
            vz -= rz * dLat + fz * dFwd;
        }
        if (nWall > 0) {
            // Scraping along a wall / ceiling: lose at most mu * N of the sliding speed.
            double sp = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (sp > 1e-9) {
                double k = Math.max(0, sp - MU_WALL * nWall) / sp;
                vx *= k; vy *= k; vz *= k;
            }
        }
        return new Resolved(new Vec3(vx, vy, vz), nFloor > 0, Math.max(nFloor, nWall));
    }
}
