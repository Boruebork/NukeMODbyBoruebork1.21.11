package com.boruebork.nukemod.debug;

import com.boruebork.nukemod.uav.flight.FlightState;
import com.boruebork.nukemod.uav.flight.Sticks;
import net.minecraft.util.Mth;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAccumulator;

/** -Dnukemod.netdebug=events (anomalies only) | trace (every tick). Writes <gamedir>/logs/netdebug-*.log */
public final class NetDebug {
    private NetDebug() {}

    private static final String MODE = System.getProperty("nukemod.netdebug", "off").toLowerCase(Locale.ROOT);
    public static final boolean ON = !MODE.equals("off");
    public static final boolean TRACE = MODE.equals("trace");

    // ------------------------------------------------------------------ output
    private static final Object LOCK = new Object();
    private static final long T0 = System.nanoTime();
    private static BufferedWriter out;
    private static long lastFlushMs;

    public static void note(String side, String tag, String msg) { if (ON) log(side, tag, msg); }

    private static void log(String side, String tag, String msg) {
        long ms = (System.nanoTime() - T0) / 1_000_000L;
        synchronized (LOCK) {
            try {
                if (out == null) open();
                out.write(String.format(Locale.ROOT, "%8d %s %-10s %s", ms, side, tag, msg));
                out.newLine();
                if (ms - lastFlushMs > 500) { out.flush(); lastFlushMs = ms; }
            } catch (IOException ignored) {}
        }
    }

    private static void open() throws IOException {
        Path dir = FMLPaths.GAMEDIR.get().resolve("logs");
        Files.createDirectories(dir);
        String name = "netdebug-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".log";
        out = Files.newBufferedWriter(dir.resolve(name));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            synchronized (LOCK) { try { out.flush(); } catch (IOException ignored) {} }
        }));
    }

    // ------------------------------------------------------------------ formatting / diff
    private static String f(@Nullable Sticks s) {
        return s == null ? "n/a" : String.format(Locale.ROOT, "(t%+.2f p%+.2f r%+.2f y%+.2f)", s.throttle(), s.pitch(), s.roll(), s.yaw());
    }

    private static String f(FlightState s) {
        return String.format(Locale.ROOT, "p=(%.4f,%.4f,%.4f) v=(%.5f,%.5f,%.5f) ypr=(%.3f,%.3f,%.3f) thr=%.3f gpr=%.3f gnd=%b",
                s.pos().x, s.pos().y, s.pos().z, s.vel().x, s.vel().y, s.vel().z,
                s.yaw(), s.pitch(), s.roll(), s.throttle(), s.groundPitchRate(), s.onGround());
    }

    private static boolean same(Sticks a, Sticks b) {
        return Math.abs(a.throttle() - b.throttle()) < 1e-5 && Math.abs(a.pitch() - b.pitch()) < 1e-5
                && Math.abs(a.roll() - b.roll()) < 1e-5 && Math.abs(a.yaw() - b.yaw()) < 1e-5;
    }

    private record Diff(double pos, double vel, float yaw, float pitch, float roll, float thr, float gpr, boolean ground) {
        static Diff of(FlightState a, FlightState b) {
            return new Diff(a.pos().distanceTo(b.pos()), a.vel().distanceTo(b.vel()),
                    Math.abs(Mth.wrapDegrees(a.yaw() - b.yaw())), Math.abs(a.pitch() - b.pitch()), Math.abs(a.roll() - b.roll()),
                    Math.abs(a.throttle() - b.throttle()), Math.abs(a.groundPitchRate() - b.groundPitchRate()),
                    a.onGround() != b.onGround());
        }
        /** Anything at all (used for "something outside moved the entity"). */
        boolean any() { return pos > 1e-6 || vel > 1e-9 || yaw > 1e-4 || pitch > 1e-4 || roll > 1e-4 || thr > 1e-6 || gpr > 1e-6 || ground; }
        /** Root-cause order: an earlier field explains the later ones. */
        String root() {
            StringBuilder sb = new StringBuilder();
            if (ground) sb.append("GROUND ");
            if (thr > 1e-3) sb.append("THROTTLE ");
            if (gpr > 1e-3) sb.append("GROUND_PITCH ");
            if (yaw > 0.01 || pitch > 0.01 || roll > 0.01) sb.append("ATTITUDE ");
            if (vel > 1e-3) sb.append("VELOCITY ");
            if (pos > 1e-2) sb.append("POSITION ");
            return sb.isEmpty() ? "none" : sb.toString().trim();
        }
        String text() {
            return String.format(Locale.ROOT, "dPos=%.5f dVel=%.6f dYaw=%.4f dPitch=%.4f dRoll=%.4f dThr=%.5f dGpr=%.4f dGnd=%b",
                    pos, vel, yaw, pitch, roll, thr, gpr, ground);
        }
    }

    // ------------------------------------------------------------------ counters
    private static final Map<String, AtomicInteger> COUNTS = new ConcurrentHashMap<>();
    private static final DoubleAccumulator MAX_CORR = new DoubleAccumulator(Math::max, 0.0);
    private static int lastSummaryTick;

    private static void count(String k) { COUNTS.computeIfAbsent(k, x -> new AtomicInteger()).incrementAndGet(); }

    public static void chunkMiss(boolean clientSide) { if (ON) count(clientSide ? "chunkMiss.C" : "chunkMiss.S"); }

    private static void summary(int tick) {
        if (tick >= lastSummaryTick && tick - lastSummaryTick < 100) return;     // every 5 s
        lastSummaryTick = tick;
        StringBuilder sb = new StringBuilder("last 5s:");
        new TreeMap<>(COUNTS).forEach((k, v) -> { int n = v.getAndSet(0); if (n > 0) sb.append(' ').append(k).append('=').append(n); });
        sb.append(String.format(Locale.ROOT, " maxCorr=%.3f", MAX_CORR.getThenReset()));
        log("C", "SUMMARY", sb.toString());
    }

    // ------------------------------------------------------------------ server side
    private record ServerRec(Sticks used, FlightState after) {}
    private static final Map<Integer, ServerRec> SERVER_BY_ACK = new ConcurrentHashMap<>();   // integrated server only
    private static final Map<Integer, FlightState> SERVER_EXPECTED = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> SERVER_LAST_ACK = new ConcurrentHashMap<>();

    public static void serverPreTick(int id, int tick, FlightState now) {
        FlightState exp = SERVER_EXPECTED.get(id);
        if (exp == null) return;
        Diff d = Diff.of(exp, now);
        if (d.any()) {
            count("extMove.S");
            log("S", "EXT_MOVE", "t=" + tick + " root=[" + d.root() + "] " + d.text() + "  (changed between our last step and this one)");
        }
    }

    public static void serverStep(int id, int tick, int ack, Sticks used, boolean starved, int inboxLeft, FlightState after) {
        SERVER_EXPECTED.put(id, after);
        Integer last = SERVER_LAST_ACK.put(id, ack);
        if (starved) {
            count("starve");
            log("S", "STARVE", "t=" + tick + " ack stays " + ack + ": no input arrived, stepped on a REPEATED input -> server is now one step ahead of the client history");
        } else {
            if (ack >= 0) {
                SERVER_BY_ACK.put(ack, new ServerRec(used, after));
                SERVER_BY_ACK.remove(ack - 120);
            }
            if (last != null && last >= 0 && ack != last + 1) {
                count("ackJump");
                log("S", "ACK_JUMP", "t=" + tick + " ack " + last + " -> " + ack + " (delta " + (ack - last) + "): inputs skipped or client ticks missing");
            }
        }
        if (TRACE) log("S", "STEP", "t=" + tick + " ack=" + ack + " inboxLeft=" + inboxLeft + " in=" + f(used) + " " + f(after));
    }

    public static void serverDrop(int tick, int droppedClientTick) {
        count("drop");
        log("S", "DROP", "t=" + tick + " inbox overflow, dropped input for clientTick=" + droppedClientTick + " -> server never steps it, permanent divergence until corrected");
    }

    // ------------------------------------------------------------------ client side
    public enum Action { MATCHED, REPLAY_KEPT, REPLAY_APPLIED, HARD_SNAP_LAG }
    public record ClientRec(FlightState state, Sticks sticks) {}

    private static volatile FlightState clientExpected;
    private static volatile int clientExpectedId = -1;

    /** Call after the reconcile code itself changed the entity, so it isn't reported as an outside move. */
    public static void clientExpect(int id, FlightState s) { clientExpected = s; clientExpectedId = id; }

    public static void clientPreTick(int id, int tick, FlightState now) {
        FlightState exp = clientExpected;
        if (exp == null || clientExpectedId != id) return;
        Diff d = Diff.of(exp, now);
        if (d.any()) {
            count("extMove.C");
            log("C", "EXT_MOVE", "t=" + tick + " root=[" + d.root() + "] " + d.text()
                    + "  (the entity was changed by something other than prediction/reconcile: tracker packet? other code?)");
            if (TRACE) { log("C", "  expected", f(exp)); log("C", "  found", f(now)); }
        }
    }

    public static void clientPostTick(int id, int tick, Sticks sticks, FlightState after) {
        clientExpect(id, after);
        if (TRACE) log("C", "PRED", "t=" + tick + " in=" + f(sticks) + " " + f(after));
        summary(tick);
    }

    public static void clientReconcile(int ack, int now, int histLeft, @Nullable ClientRec pred, FlightState auth,
                                       @Nullable FlightState replayed, FlightState current, Action action) {
        count("reconcile");
        count(action.name());

        double corr = switch (action) {
            case REPLAY_APPLIED -> replayed == null ? 0 : replayed.pos().distanceTo(current.pos());
            case HARD_SNAP_LAG -> auth.pos().distanceTo(current.pos());
            default -> 0;
        };
        MAX_CORR.accumulate(corr);
        boolean snap = corr > 0.05;
        if (snap) count("SNAP>5cm");

        ServerRec sr = SERVER_BY_ACK.get(ack);                       // null on a dedicated server
        boolean inputMismatch = pred != null && sr != null && !same(pred.sticks(), sr.used());
        Diff payload = sr == null ? null : Diff.of(sr.after(), auth);
        boolean payloadBad = payload != null && !payload.root().equals("none");
        Diff predVsAuth = pred == null ? null : Diff.of(pred.state(), auth);
        if (inputMismatch) count("inputMismatch");
        if (payloadBad) count("payloadMismatch");

        if (action == Action.MATCHED && !inputMismatch && !payloadBad && !TRACE) return;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "ack=%d now=%d behind=%d histLeft=%d %s", ack, now, now - ack, histLeft, action));
        if (snap) sb.append(String.format(Locale.ROOT, "  *** SNAP corr=%.3f ***", corr));
        sb.append(pred == null ? "  NO_HISTORY_FOR_ACK" : "  root=[" + predVsAuth.root() + "] " + predVsAuth.text());
        if (sr != null) sb.append("  sticks client=").append(f(pred == null ? null : pred.sticks())).append(" server=").append(f(sr.used()));
        if (inputMismatch) sb.append("  INPUT_MISMATCH");
        if (payloadBad) sb.append("  PAYLOAD!=SERVER_STATE[").append(payload.root()).append("] ").append(payload.text());
        log("C", "RECONCILE", sb.toString());

        if (action != Action.MATCHED || payloadBad || TRACE) {
            if (pred != null) log("C", "  pred", f(pred.state()));
            log("C", "  auth", f(auth));
            if (sr != null) log("C", "  srvTrue", f(sr.after()));
            if (replayed != null) log("C", "  replayed", f(replayed));
            log("C", "  current", f(current));
        }
    }
}