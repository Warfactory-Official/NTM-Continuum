package com.hbm.client.render;

import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.client.Minecraft;

/**
 * Harness query {@code hbm_lens_bench}: the frame cost of the port-only black hole lens (History §19ey-lensbench).
 * Inert unless the harness listener is running.
 *
 * <ul>
 *   <li>{@code {"action":"lens","on":false}}: draws the fields the 1.7 way (sphere, rings, jets) instead of through
 *       the lens, so the two can be compared in one session; {@code true} restores the lens.</li>
 *   <li>{@code {"action":"start"}}: clears and starts recording the interval between world frames.</li>
 *   <li>{@code {"action":"stop"}}: stops and reports frame ms p50/p95/p99/mean, frames, and how many lens and 1.7
 *       field draws happened while recording (the positive signal that the intended path drew).</li>
 * </ul>
 */
public final class LensBenchmark {

    private static final long[] FRAME_NANOS = new long[65_536];
    private static volatile boolean lensDisabled;
    private static boolean recording;
    private static int frames;
    private static long lastFrame;
    private static long lensDraws;
    private static long fallbackDraws;
    private static boolean hooked;

    private LensBenchmark() {}

    public static void register() {
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_lens_bench", LensBenchmark::query);
    }

    /** False only while the harness compares against the 1.7 look. */
    static boolean lensAllowed() {
        return !lensDisabled;
    }

    static void lensDrawn() {
        if (recording) lensDraws++;
    }

    static void fallbackDrawn() {
        if (recording) fallbackDraws++;
    }

    private static void frame() {
        if (!recording) return;
        long now = System.nanoTime();
        if (lastFrame != 0L && frames < FRAME_NANOS.length) FRAME_NANOS[frames++] = now - lastFrame;
        lastFrame = now;
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        if (!hooked) {
            hooked = true;
            dev.golem.core.render.WorldEffects.onBegin(camera -> frame());
        }
        JsonObject json = new JsonObject();
        String action = params.has("action") ? params.get("action").getAsString() : "";
        switch (action) {
            case "lens" -> lensDisabled = !params.get("on").getAsBoolean();
            case "start" -> {
                frames = 0;
                lastFrame = 0L;
                lensDraws = 0L;
                fallbackDraws = 0L;
                recording = true;
            }
            case "stop" -> {
                recording = false;
                long[] sorted = Arrays.copyOf(FRAME_NANOS, frames);
                Arrays.sort(sorted);
                long sum = 0L;
                for (long v : sorted) sum += v;
                json.addProperty("frames", frames);
                json.addProperty("p50", ms(percentile(sorted, 0.50)));
                json.addProperty("p95", ms(percentile(sorted, 0.95)));
                json.addProperty("p99", ms(percentile(sorted, 0.99)));
                json.addProperty("mean", ms(frames == 0 ? 0L : sum / frames));
                json.addProperty("lens_draws", lensDraws);
                json.addProperty("fallback_draws", fallbackDraws);
                json.addProperty("width", mc.getWindow().getWidth());
                json.addProperty("height", mc.getWindow().getHeight());
            }
            default -> {
            }
        }
        json.addProperty("lens", !lensDisabled);
        return json;
    }

    private static long percentile(long[] sorted, double p) {
        if (sorted.length == 0) return 0L;
        return sorted[Math.min(sorted.length - 1, (int) Math.ceil(sorted.length * p) - 1)];
    }

    private static String ms(long nanos) {
        return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000D);
    }
}
