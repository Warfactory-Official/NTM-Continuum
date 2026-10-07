import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Frame cost of the port-only black hole lens (History §19ey-lensbench): frame ms with no field, with the field
 * drawn the 1.7 way (lens off through {@code hbm_lens_bench}) and with the lens, alternated lens/1.7/lens/1.7 so
 * drift cancels. Views at 8, 20 and 60 blocks (size 1.5) and a screen-filling size 5 at 20 blocks.
 *
 * <p>Not a pass/fail check, beyond requiring that the intended path actually drew in every window. Run on one client
 * with {@code up --show} (an iconified window is frame-capped) so no second client shares the GPU.
 */
public final class BlackHoleLensFps implements Scenario {

    private static final String[][] FIELDS = {
            {"black_hole", "hbm:entity_black_hole"},
            {"quasar", "hbm:entity_digamma_quasar"},
            {"vortex", "hbm:entity_vortex"}};
    /** name, camera z, field size */
    private static final String[][] VIEWS = {
            {"8m", "-1.5", "1.5"}, {"20m", "-13.5", "1.5"}, {"60m", "-53.5", "1.5"}, {"20m-size5", "-13.5", "5"}};
    private static final int WINDOW_TICKS = 60;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String hideGui = null, renderDistance = null, fov = null, maxFps = null, vsync = null, inactivity = null;
        List<String> rows = new ArrayList<>();
        boolean allDrew = true;
        try {
            c.cmd("gamemode creative");
            Fixtures.platform(c, 0, 100, 0, 24);
            c.cmd("time set 6000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            String[] wool = {"white", "black", "red", "yellow", "lime", "blue"};
            for (int x = -24; x <= 24; x += 2) {
                String colour = wool[Math.floorMod(x / 2, wool.length)];
                c.cmd("fill " + x + " 101 24 " + (x + 1) + " 140 24 minecraft:" + colour + "_wool");
            }
            clear(c);
            hideGui = c.setOption("hide_gui", "true");
            renderDistance = c.setOption("render_distance", "12");
            fov = c.setOption("fov", "70");
            maxFps = c.setOption("max_fps", "260");
            vsync = c.setOption("vsync", "false");
            inactivity = c.setOption("inactivity_fps", "minimized");
            // creative, not spectator: the field pulls every non-creative entity, spectators included, and an
            // earlier run's camera drifted through the hole (2026-10-07)
            lens(c, true);

            for (String[] view : VIEWS) {
                double z = Double.parseDouble(view[1]);
                c.cmd("setblock 0 111 " + (int) Math.floor(z) + " minecraft:glass");
                c.teleport(0.5, 112.0, z);
                c.lookAt(0.5, 114.0, 6.5);
                c.waitTicks(40);
                JsonObject none1 = measure(c);
                for (String[] field : FIELDS) {
                    clear(c);
                    c.cmd("summon " + field[1] + " 0.5 114 6.5 {size:" + view[2] + "f,breaksBlocks:0b}");
                    c.waitTicks(20);
                    // warm-up: first use compiles/links the shader and fills caches
                    reset(c, field, view[2]);
                    measure(c);
                    lens(c, false);
                    measure(c);
                    c.screenshot(view[0] + "-" + field[0] + "-1.7");
                    lens(c, true);
                    c.screenshot(view[0] + "-" + field[0]);
                    JsonObject where = c.player();
                    rows.add(view[0] + " " + field[0] + " camera " + where.get("x") + "," + where.get("y") + ","
                            + where.get("z"));
                    double[] on = new double[4], off = new double[4];
                    int onFrames = 0, offFrames = 0;
                    for (int rep = 0; rep < 2; rep++) {
                        reset(c, field, view[2]);
                        JsonObject a = measure(c);
                        allDrew &= a.get("lens_draws").getAsLong() > 0 && a.get("fallback_draws").getAsLong() == 0;
                        add(on, a);
                        onFrames += a.get("frames").getAsInt();
                        lens(c, false);
                        reset(c, field, view[2]);
                        JsonObject b = measure(c);
                        allDrew &= b.get("fallback_draws").getAsLong() > 0 && b.get("lens_draws").getAsLong() == 0;
                        add(off, b);
                        offFrames += b.get("frames").getAsInt();
                        lens(c, true);
                    }
                    rows.add(String.format(Locale.ROOT,
                            "%s %s | lens p50/p95/p99/mean %.3f/%.3f/%.3f/%.3f (%d fr) | 1.7 %.3f/%.3f/%.3f/%.3f (%d fr)"
                                    + " | lens - 1.7 mean %+.3f ms",
                            view[0], field[0], on[0] / 2, on[1] / 2, on[2] / 2, on[3] / 2, onFrames,
                            off[0] / 2, off[1] / 2, off[2] / 2, off[3] / 2, offFrames, (on[3] - off[3]) / 2));
                }
                clear(c);
                c.waitTicks(10);
                JsonObject none2 = measure(c);
                rows.add(String.format(Locale.ROOT, "%s none | p50/p95/p99/mean %s/%s/%s/%s then %s/%s/%s/%s | %sx%s",
                        view[0], none1.get("p50").getAsString(), none1.get("p95").getAsString(),
                        none1.get("p99").getAsString(), none1.get("mean").getAsString(),
                        none2.get("p50").getAsString(), none2.get("p95").getAsString(),
                        none2.get("p99").getAsString(), none2.get("mean").getAsString(),
                        none2.get("width").getAsString(), none2.get("height").getAsString()));
            }
            for (String r : rows) c.chat("lens-fps: " + r);
            Expect.that(allDrew, "every window drew through the intended path | " + String.join(" | ", rows));
        } finally {
            lens(c, true);
            clear(c);
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (renderDistance != null) c.setOption("render_distance", renderDistance);
            if (fov != null) c.setOption("fov", fov);
            if (maxFps != null) c.setOption("max_fps", maxFps);
            if (vsync != null) c.setOption("vsync", vsync);
            if (inactivity != null) c.setOption("inactivity_fps", inactivity);
            for (String r : rows) System.out.println("[lens-fps] " + c.node() + " " + r);
        }
    }

    /** Vortices shrink every tick; put the size back before each window. */
    private static void reset(Client c, String[] field, String size) {
        c.cmd("data merge entity @e[type=" + field[1] + ",limit=1] {size:" + size + "f}");
        c.waitTicks(5);
    }

    private static JsonObject measure(Client c) {
        JsonObject start = new JsonObject();
        start.addProperty("action", "start");
        c.query("hbm_lens_bench", start);
        c.waitTicks(WINDOW_TICKS);
        JsonObject stop = new JsonObject();
        stop.addProperty("action", "stop");
        return c.query("hbm_lens_bench", stop);
    }

    private static void lens(Client c, boolean on) {
        JsonObject p = new JsonObject();
        p.addProperty("action", "lens");
        p.addProperty("on", on);
        c.query("hbm_lens_bench", p);
        c.waitTicks(2);
    }

    private static void add(double[] sum, JsonObject r) {
        sum[0] += r.get("p50").getAsDouble();
        sum[1] += r.get("p95").getAsDouble();
        sum[2] += r.get("p99").getAsDouble();
        sum[3] += r.get("mean").getAsDouble();
    }

    private static void clear(Client c) {
        for (String[] f : FIELDS) c.cmd("kill @e[type=" + f[1] + "]");
    }
}
