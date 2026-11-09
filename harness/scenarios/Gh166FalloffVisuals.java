import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.Locale;

/**
 * GitHub issue 166. Stereo machine sounds (radar ping, Stirling overspeed) were heard at full volume at any distance:
 * OpenAL does not attenuate stereo sources, while 1.7's sound library scaled every source by 1 - d / (16 x volume).
 * A silent channel set up the way vanilla plays a sound shows the gain at several distances. The rest is filmed: the liquefactor and
 * solidifier glass, the burning plasma forge tube, the arc welder's flashes and the radiation fog.
 */
public class Gh166FalloffVisuals implements Scenario {

    private static final int Y = 100, G = 101;
    private static final int FORGE_X = -13;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 26);
        clear(c);
        c.cmd("kill @e[type=item]");
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("time set 6000");
        c.waitTicks(5);

        // the radar ping (volume 5) and Stirling overspeed (volume 2) are stereo; a mono sound keeps OpenAL's own falloff
        falloff(c, 2, 80F, new float[]{8, 40, 70, 90});
        falloff(c, 2, 32F, new float[]{24});
        falloff(c, 1, 16F, new float[]{8});

        tanks(c);
        welder(c);
        arcFurnace(c);
        fog(c);
        forge(c);

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[ERROR\\]).*(hbm|golem|sound|particle)");
        System.out.println("[Gh166] PASS");
    }

    /** A silent channel at each distance, set up as vanilla plays a sound; its OpenAL gain against 1.7's 1 - d / max. */
    private static void falloff(Client c, int channels, float max, float[] distances) {
        for (float d : distances) {
            JsonObject params = new JsonObject();
            params.addProperty("channels", channels);
            params.addProperty("distance", d);
            params.addProperty("max", max);
            JsonObject result = c.query("stereo_falloff", params);
            float gain = result.get("gain").getAsFloat();
            double expected = channels == 2 ? Math.max(0D, 1D - d / max) : 1D;
            System.out.printf(Locale.ROOT, "[Gh166] %d channel(s), max %.0f, %.0f blocks: gain %.3f, 1.7 %.3f%n",
                    channels, max, d, gain, expected);
            Expect.equal(channels, result.get("channels").getAsInt(), "channel count");
            Expect.that(Math.abs(gain - expected) < 0.01, "gain " + gain + " at " + d + " blocks, 1.7 " + expected);
        }
    }

    private static void tanks(Client c) throws Exception {
        Fixtures.setBlock(c, -3, G, -2, "hbm:machine_liquefactor");
        Fixtures.setBlock(c, 3, G, -2, "hbm:machine_solidifier");
        c.waitTicks(5);
        merge(c, -3, -2, "{tank:12000,tank_type:\"hbm:oil\"}");
        merge(c, 3, -2, "{tank:8000,tank_type:\"minecraft:lava\"}");
        c.teleport(0.5, G + 2, -8.5);
        c.lookAt(0.5, G + 2, -2);
        c.send("clear_chat");
        c.waitTicks(20);
        c.screenshot("10-liquefactor-solidifier");
        c.teleport(0.5, G + 1, -6.5);
        c.lookAt(-3, G + 2, -2);
        c.waitTicks(10);
        c.screenshot("11-liquefactor-close");
        c.lookAt(3, G + 2, -2);
        c.waitTicks(10);
        c.screenshot("12-solidifier-close");
        clear(c);
    }

    /** The welder flashes on a black night sky: a 1.7 flash fades out at its largest, so no frame shows a large white disc. */
    private static void welder(Client c) throws Exception {
        c.cmd("time set 18000");
        Fixtures.giveOnly(c, "hbm:machine_arc_welder", 1);
        c.teleport(0.5, G, -3.5);
        c.waitTicks(3);
        c.lookAt(0.5, Y + 0.98, 0.5);
        c.use(2);
        c.waitTicks(6);
        c.cmd("clear");
        c.teleport(0.5, G, -3.5);
        Fixtures.interact(c, 0, G, 0);
        c.waitTicks(20);
        insert(c, "hbm:wire_fine_copper", 64, 0);
        insert(c, "hbm:battery_creative", 1, 4);
        c.key("escape");
        c.waitTicks(5);
        c.cmd("setblock 0 " + G + " -3 minecraft:stone");
        c.teleport(0.5, G + 1, -2.5);
        c.waitTicks(6);
        c.lookAt(0.5, G + 1.25, 0.5);
        c.send("clear_chat");
        c.waitTicks(5);
        for (int i = 0; i < 24; i++) {
            c.screenshot(String.format("20-welder-%02d", i));
            c.waitTicks(1);
        }
        c.cmd("time set 6000");
        clear(c);
    }

    /** The engine decays 500 RAD below fogRad within seconds, so it is held there for 30 s (about 13 fog effects). */
    private static void fog(Client c) throws Exception {
        c.teleport(0.5, G, 0.5);
        c.waitTicks(5);
        for (int i = 0; i < 30; i++) {
            small(c, "set_rad", 500);
            c.waitTicks(20);
        }
        System.out.println("[Gh166] fog " + small(c));
        c.teleport(0.5, G + 12, -20.5);
        c.lookAt(0.5, G, 0.5);
        c.waitTicks(10);
        c.screenshot("30-radiation-fog");
        small(c, "set_rad", 0);
    }

    private static JsonObject small(Client c, Object... kv) {
        JsonObject p = new JsonObject();
        p.addProperty("mode", "fog");
        p.addProperty("x", 4);
        p.addProperty("y", G);
        p.addProperty("z", 4);
        for (int i = 0; i + 1 < kv.length; i += 2) p.addProperty((String) kv[i], (Number) kv[i + 1]);
        for (int i = 0; i < 60; i++) {
            JsonObject r = c.query("hbm_small", p);
            if (!r.has("pending")) return r;
            c.waitTicks(10);
        }
        Expect.that(false, "hbm_small fog stayed pending");
        return p;
    }

    /** Used electrodes (worn, not burnt out) are drawn opaque and full-bright, as 1.7's fullbright(true) block. */
    private static void arcFurnace(Client c) throws Exception {
        clear(c);
        Fixtures.setBlock(c, 0, G, 0, "hbm:machine_arc_furnace");
        c.waitTicks(5);
        String electrode = c.node().startsWith("1.20") ? "hbm:arc_electrode_graphite{Damage:5}" : "hbm:arc_electrode_graphite[damage=5]";
        for (int slot = 0; slot < 3; slot++) {
            c.cmd("item replace block 0 " + G + " 0 container." + slot + " with " + electrode);
        }
        c.cmd("time set 18000");
        Expect.equal("hbm:machine_arc_furnace", c.block(0, G, 0).get("block").getAsString(), "arc furnace core");
        c.teleport(6.5, G + 4, -6.5);
        c.lookAt(0.5, G + 3, 0.5);
        c.send("clear_chat");
        c.waitTicks(30);
        c.screenshot("35-arc-furnace-electrodes-night");
        c.cmd("time set 6000");
        c.waitTicks(10);
        c.screenshot("36-arc-furnace-electrodes-day");
        clear(c);
    }

    private static void forge(Client c) throws Exception {
        place(c, "hbm:fusion_torus", 0.5, G, -9.5, 0, Y, -7);
        place(c, "hbm:fusion_klystron_creative", 18.5, G, 0.5, 15, Y, 0);
        place(c, "hbm:fusion_plasma_forge", FORGE_X + 0.5, G, -7.5, FORGE_X, Y, -5);
        c.cmd("data merge block " + FORGE_X + " " + G + " 0 {recipe0:\"plsm.exsteelplating\"}");
        long tu = 0;
        for (int i = 0; i < 40 && tu == 0; i++) {
            burn(c);
            c.waitTicks(10);
            tu = forgeTu(c);
        }
        System.out.println("[Gh166] forge reads " + tu + " TU");
        Expect.that(tu > 0, "the forge receives plasma");
        double[][] views = {{FORGE_X - 9.5, G + 4, 0.5}, {FORGE_X + 0.5, G + 4, -9.5}, {FORGE_X + 0.5, G + 9, 0.5}};
        for (int i = 0; i < views.length; i++) {
            c.teleport(views[i][0], views[i][1], views[i][2]);
            c.lookAt(FORGE_X + 0.5, G + 2.5, 0.5);
            c.send("clear_chat");
            burn(c);
            c.waitTicks(10);
            burn(c);
            c.screenshot("4" + i + "-forge-tube");
        }
    }

    private static long forgeTu(Client c) {
        JsonObject params = new JsonObject();
        params.addProperty("x", FORGE_X);
        params.addProperty("y", G);
        params.addProperty("z", 0);
        JsonObject result = c.query("hbm_block_info", params);
        if (!result.has("gauges")) return 0;
        for (JsonElement gauge : result.getAsJsonArray("gauges")) {
            JsonObject g = gauge.getAsJsonObject();
            if ("TU".equals(g.get("unit").getAsString())) return g.get("value").getAsLong();
        }
        return 0;
    }

    /** Two fills: one box of the whole site is over vanilla's 32,768-block limit. */
    private static void clear(Client c) {
        c.cmd("fill -26 " + G + " -26 26 " + (G + 5) + " 26 minecraft:air");
        c.cmd("fill -26 " + (G + 6) + " -26 26 " + (Y + 12) + " 26 minecraft:air");
        c.cmd("kill @e[type=item]");
    }

    private static void burn(Client c) {
        c.cmd("data merge block 0 " + G + " 0 {recipe0:\"fus.dd\",power:10000000L,temperature:123F,t0:4000,t1:0,"
                + "ft0:4000,ft0_type:\"hbm:deuterium\",ft3:0}");
    }

    private static void merge(Client c, int x, int z, String nbt) {
        c.cmd("data merge block " + x + " " + G + " " + z + " " + nbt);
    }

    private static void place(Client c, String item, double px, double py, double pz, int bx, int by, int bz) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(px, py, pz);
        c.waitTicks(3);
        c.lookAt(bx + 0.5, by + 0.98, bz + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        Expect.equal(item, c.block(bx, by + 1, bz).get("block").getAsString(), item + " placed by hand");
    }

    private static void insert(Client c, String item, int count, int slot) {
        c.cmd("give @s " + item + " " + count);
        c.waitTicks(10);
        int source = -1;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(item)) {
                source = s.get("index").getAsInt();
                break;
            }
        }
        Expect.that(source >= 0, "no player slot holding " + item);
        c.clickSlot(source);
        c.waitTicks(2);
        c.clickSlot(slot);
        c.waitTicks(5);
    }
}
