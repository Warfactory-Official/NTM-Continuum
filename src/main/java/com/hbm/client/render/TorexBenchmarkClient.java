package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.entity.effect.EntityNukeTorex;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.platform.Platform;
import dev.golem.core.render.Billboards;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;

/** Development-only deterministic benchmark driver for the Torex cloudlet renderer. */
public final class TorexBenchmarkClient {

    private static final boolean ENABLED = System.getenv("HBM_TOREX_BENCHMARK") != null;
    private static final int FIXTURE_AGE = fixtureAge();
    private static final int SAMPLE_START_AGE = FIXTURE_AGE + 50;
    private static final int SAMPLE_END_AGE = FIXTURE_AGE + 250;
    private static final int TIMEOUT_TICKS = 20 * 60;
    private static final long[] FRAME_NANOS = new long[32_768];
    private static final long[] FALLBACK_NANOS = new long[32_768];
    private static final long[] INSTANCED_NANOS = new long[32_768];
    private static boolean sent;
    private static boolean finished;
    private static boolean captured;
    private static int stopDelay;
    private static int elapsedTicks;
    private static int frameSamples;
    private static int fallbackSamples;
    private static int instancedSamples;
    private static int lastFrameId = Integer.MIN_VALUE;
    private static long cloudletSum;
    private static int cloudletMin = Integer.MAX_VALUE;
    private static int cloudletMax;
    private static long fallbackVertices;
    private static long instancedRows;
    private static Integer previousFrameLimit;
    private static Integer previousFov;
    private static Boolean previousVsync;
    private static CameraType previousCameraType;
    private static Float previousYaw;
    private static Float previousPitch;
    private static LocalPlayer benchmarkPlayer;
    private static boolean restored;
    private static boolean shutdownPending;

    private TorexBenchmarkClient() {}

    public static void register() {
        if (ENABLED && Platform.isDevelopment()) {
            ClientTicks.onEndTick(TorexBenchmarkClient::tick);
        }
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (shutdownPending) {
            minecraft.stop();
            return;
        }
        if (restored) return;
        if (sent && ++elapsedTicks >= TIMEOUT_TICKS) {
            Constants.LOG.error("[TOREX-BENCH] timed out before the fixture completed");
            finished = true;
            if (minecraft.player != null) {
                minecraft.player.connection.sendCommand("hbm torex_benchmark stop");
            }
            restoreAndStop(minecraft);
            return;
        }
        if (sent && (minecraft.player == null || minecraft.level == null)) {
            Constants.LOG.error("[TOREX-BENCH] client left the benchmark level before completion");
            finished = true;
            restoreAndStop(minecraft);
            return;
        }
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
                || minecraft.getOverlay() != null) return;
        if (!sent) {
            previousFrameLimit = minecraft.options.framerateLimit().get();
            previousFov = minecraft.options.fov().get();
            previousVsync = minecraft.options.enableVsync().get();
            previousCameraType = minecraft.options.getCameraType();
            previousYaw = minecraft.player.getYRot();
            previousPitch = minecraft.player.getXRot();
            benchmarkPlayer = minecraft.player;
        }
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        minecraft.player.setYRot(0F);
        minecraft.player.setXRot(-28F);
        if (!sent) {
            // Vanilla's slider sentinel for unlimited on every supported client.
            minecraft.options.framerateLimit().set(260);
            minecraft.options.fov().set(90);
            minecraft.options.enableVsync().set(false);
            Constants.LOG.info("[TOREX-BENCH] requesting deterministic scale-5 fixture at age {}", FIXTURE_AGE);
            minecraft.player.connection.sendCommand("hbm torex_benchmark " + FIXTURE_AGE);
            sent = true;
            return;
        }
        if (finished && !captured) {
            Screenshot.grab(minecraft.gameDirectory, minecraft.getMainRenderTarget(),
                    message -> Constants.LOG.info("[TOREX-BENCH] capture {}", message.getString()));
            captured = true;
        }
        if (finished && ++stopDelay >= 20) {
            minecraft.player.connection.sendCommand("hbm torex_benchmark stop");
            restoreAndStop(minecraft);
        }
    }

    private static void restoreAndStop(Minecraft minecraft) {
        if (restored) return;
        restored = true;
        if (previousFrameLimit != null) minecraft.options.framerateLimit().set(previousFrameLimit);
        if (previousFov != null) minecraft.options.fov().set(previousFov);
        if (previousVsync != null) minecraft.options.enableVsync().set(previousVsync);
        if (previousCameraType != null) minecraft.options.setCameraType(previousCameraType);
        if (benchmarkPlayer != null) {
            if (previousYaw != null) benchmarkPlayer.setYRot(previousYaw);
            if (previousPitch != null) benchmarkPlayer.setXRot(previousPitch);
        }
        minecraft.options.save();
        // Leave one full client tick for the restored rotation to reach the integrated server.
        shutdownPending = true;
    }

    public static void frame(EntityNukeTorex cloud) {
        if (!ENABLED || finished) return;
        int age = cloud.tickCount;
        if (age < SAMPLE_START_AGE) {
            lastFrameId = Integer.MIN_VALUE;
            return;
        }
        if (age >= SAMPLE_END_AGE) {
            finish();
            return;
        }
        int frameId = Billboards.frameId();
        if (frameId == lastFrameId) return;
        lastFrameId = frameId;
        if (frameSamples < FRAME_NANOS.length) {
            FRAME_NANOS[frameSamples++] = Minecraft.getInstance().getFrameTimeNs();
            int count = cloud.cloudlets.size();
            cloudletSum += count;
            cloudletMin = Math.min(cloudletMin, count);
            cloudletMax = Math.max(cloudletMax, count);
        }
    }

    public static long fallbackStart(EntityNukeTorex cloud) {
        return ENABLED && !finished
                && cloud.tickCount >= SAMPLE_START_AGE && cloud.tickCount < SAMPLE_END_AGE
                ? System.nanoTime() : 0L;
    }

    public static void fallbackEnd(long started, int cloudlets) {
        if (started == 0L) return;
        if (fallbackSamples < FALLBACK_NANOS.length) {
            FALLBACK_NANOS[fallbackSamples++] = System.nanoTime() - started;
        }
        fallbackVertices += (long) cloudlets * 4L;
    }

    public static long instancedStart(EntityNukeTorex cloud) {
        return ENABLED && !finished && cloud.tickCount >= SAMPLE_START_AGE
                && cloud.tickCount < SAMPLE_END_AGE ? System.nanoTime() : 0L;
    }

    public static void instancedEnd(long started, int cloudlets, boolean active) {
        if (started == 0L || !active) return;
        if (instancedSamples < INSTANCED_NANOS.length) {
            INSTANCED_NANOS[instancedSamples++] = System.nanoTime() - started;
        }
        instancedRows += cloudlets;
    }

    private static void finish() {
        if (finished) return;
        finished = true;
        String mode = instancedSamples > 0 ? "instanced" : "fallback";
        Constants.LOG.info("[TOREX-BENCH] mode={} frames={} frame_ms[p50={},p95={},p99={}] "
                        + "cloudlets[min={},mean={},max={}] fallback_us[p50={},p95={},p99={}] vertices={} "
                        + "instance_us[p50={},p95={},p99={}] rows={}", mode,
                frameSamples, millis(FRAME_NANOS, frameSamples, 0.50), millis(FRAME_NANOS, frameSamples, 0.95),
                millis(FRAME_NANOS, frameSamples, 0.99), cloudletMin,
                frameSamples == 0 ? 0 : cloudletSum / frameSamples, cloudletMax,
                micros(FALLBACK_NANOS, fallbackSamples, 0.50), micros(FALLBACK_NANOS, fallbackSamples, 0.95),
                micros(FALLBACK_NANOS, fallbackSamples, 0.99), fallbackVertices,
                micros(INSTANCED_NANOS, instancedSamples, 0.50), micros(INSTANCED_NANOS, instancedSamples, 0.95),
                micros(INSTANCED_NANOS, instancedSamples, 0.99), instancedRows);
    }

    private static String millis(long[] samples, int count, double percentile) {
        return String.format(Locale.ROOT, "%.3f", percentile(samples, count, percentile) / 1_000_000D);
    }

    private static String micros(long[] samples, int count, double percentile) {
        return String.format(Locale.ROOT, "%.1f", percentile(samples, count, percentile) / 1_000D);
    }

    private static long percentile(long[] samples, int count, double percentile) {
        if (count == 0) return 0L;
        long[] sorted = Arrays.copyOf(samples, count);
        Arrays.sort(sorted);
        return sorted[Math.min(count - 1, (int) Math.ceil(count * percentile) - 1)];
    }

    private static int fixtureAge() {
        String value = System.getenv("HBM_TOREX_BENCHMARK_AGE");
        if (value == null || value.isBlank()) return 1_200;
        try {
            return Math.max(0, Math.min(4_000, Integer.parseInt(value)));
        } catch (NumberFormatException ignored) {
            return 1_200;
        }
    }
}
