import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The crucible's attack input and its third-person look (crucible2, 2026-10-02):
 *
 * <ol>
 *   <li><b>slice confirm over a mob</b>: hold use, drag a cut through a sheep two blocks ahead so that the crosshair
 *       ends on a pig four blocks ahead (inside creative attack reach, outside the cut's three blocks), press attack
 *       while use is still held. The click must only confirm the cut: the sheep is cut, the pig keeps full health, no
 *       hurt time and no knockback; Core {@code AttackInput} consumed the click. A plain attack on the same pig
 *       afterwards kills it (it was in reach);</li>
 *   <li><b>F5 swing</b>: an attack at the air in third person back and front shows vanilla's arm swing (client and
 *       server swing state, mid-swing screenshots against a still baseline);</li>
 *   <li><b>first-person swing</b>: the 1.7 crucible swing's bus values against the 1.7 keyframes recomputed from its
 *       offset and elapsed time, with a screenshot per sample;</li>
 *   <li><b>third-person effects</b>: a charged crucible in F5 at night draws the translucent blade, lightning, bloom
 *       and distortion (draw counters and screenshots, a frozen bolt for a sure lightning shot); an empty crucible
 *       does not.</li>
 * </ol>
 */
public final class CrucibleSwingSlice implements Scenario {

    private static final int X = 300;
    private static final int Y = 100;
    private static final int Z = 300;

    private static String charged(Client c) {
        return c.node().startsWith("1.20") ? "hbm:crucible{\"hbm:charges\":16}" : "hbm:crucible[hbm:charges=16]";
    }

    private static JsonObject q(Client c) throws Exception {
        return c.query("hbm_crucible", new JsonObject());
    }

    private static JsonObject q(Client c, String control) throws Exception {
        JsonObject p = new JsonObject();
        p.addProperty(control, true);
        return c.query("hbm_crucible", p);
    }

    /** The server's view of the living entity of {@code type} nearest (x, z), or null. */
    private static JsonObject mob(Client c, String type, double x, double z) throws Exception {
        JsonObject p = new JsonObject();
        p.addProperty("radius", 12);
        p.addProperty("server", true);
        JsonArray ents = c.query("entities", p).getAsJsonArray("entities");
        JsonObject best = null;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < ents.size(); i++) {
            JsonObject e = ents.get(i).getAsJsonObject();
            if (!e.get("type").getAsString().contains(type)) continue;
            double d = Math.hypot(e.get("x").getAsDouble() - x, e.get("z").getAsDouble() - z);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    /** original-hbm ItemCrucible.getAnimation(SWING) at {@code ms}: SWING_ROT (3) then SWING_TRANS (3). */
    static double[] expected17(int offset, double ms) {
        double[][] keys = {
                {0, 0, 0, 0, 0, 0},
                {90 - offset, 90 - offset, 35, -3, 0, 0},
                {90 + offset, 90 - offset, -45, 8, 0, 0},
                {0, 0, 0, 0, 0, 0}};
        double[] ends = {0, 75, 225, 725};
        double[] out = new double[6];
        if (ms >= 725) return out;
        int k = ms < 75 ? 1 : ms < 225 ? 2 : 3;
        double t = (ms - ends[k - 1]) / (ends[k] - ends[k - 1]);
        for (int i = 0; i < 6; i++) out[i] = keys[k - 1][i] + (keys[k][i] - keys[k - 1][i]) * t;
        return out;
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 8);
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("weather clear");
        c.cmd("gamemode creative");
        c.cmd("kill @e[type=!player]");
        c.cmd("time set 6000");
        c.setOption("camera", "first_person");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, 10);
        c.cmd("clear");
        c.waitTicks(5);
        c.cmd("give @s " + charged(c) + " 1");
        c.waitTicks(40); // the deploy (1,060 ms) is over

        // ---- 1. slice confirm with a pig under the crosshair
        double ax = X - 0.5, az = Z + 2.6;   // the sheep: on the cut's sweep, two blocks out
        double bx = X + 0.5, bz = Z + 4.5;   // the pig: under the final crosshair, four blocks out
        c.cmd("summon minecraft:sheep " + ax + " " + (Y + 1) + " " + az + " {NoAI:1b,Color:14b}");
        c.cmd("summon minecraft:pig " + bx + " " + (Y + 1) + " " + bz + " {NoAI:1b}");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(10);
        JsonObject pigBefore = mob(c, "pig", bx, bz);
        Expect.that(pigBefore != null, "the pig is there");
        c.lookAt(X - 2.0, Y + 1.3, Z + 2.5);
        c.waitTicks(2);
        JsonObject start0 = q(c);
        long consumedBefore = start0.get("attack_clicks_consumed").getAsLong();
        int slicedBefore = start0.get("sliced_spawned").getAsInt();
        c.keyDown("key.use");
        c.waitTicks(3);
        JsonObject start = q(c);
        Expect.that(start.get("clicked").getAsBoolean(), "holding use began a cut: " + start);
        c.lookAt(bx, Y + 1.45, bz);
        c.waitTicks(3);
        JsonObject drawn = q(c);
        Expect.that(!drawn.get("plane_normal").getAsString().isEmpty(), "the plane normal is drawn: " + drawn);
        JsonObject target = c.query("player", new JsonObject());
        c.screenshot("01-cut-drawn-pig-under-crosshair");
        c.attack();            // left click while use is still held, crosshair on the pig
        c.waitTicks(1);
        c.screenshot("02-confirm");
        c.waitTicks(1);
        c.keyUp("key.use");
        c.waitTicks(2);
        JsonObject after = q(c);
        JsonObject pigAfter = mob(c, "pig", bx, bz);
        JsonObject sheepAfter = mob(c, "sheep", ax, az);
        c.screenshot("03-after-confirm");
        Expect.that(after.get("attack_clicks_consumed").getAsLong() == consumedBefore + 1,
                "AttackInput took the confirming click: " + after);
        Expect.that(after.get("sliced_spawned").getAsInt() - slicedBefore >= 2, "the sheep was cut: " + after);
        Expect.that(sheepAfter == null || !sheepAfter.get("alive").getAsBoolean(), "the sheep is gone: " + sheepAfter);
        Expect.that(pigAfter != null && pigAfter.get("alive").getAsBoolean(), "the pig is alive: " + pigAfter);
        Expect.that(pigAfter.get("health").getAsDouble() == pigAfter.get("max_health").getAsDouble(),
                "the pig took no damage: " + pigAfter);
        Expect.that(pigAfter.get("hurt_time").getAsInt() == 0, "the pig was not hurt: " + pigAfter);
        double moved = Math.hypot(pigAfter.get("x").getAsDouble() - pigBefore.get("x").getAsDouble(),
                pigAfter.get("z").getAsDouble() - pigBefore.get("z").getAsDouble());
        double vxz = Math.hypot(pigAfter.get("vx").getAsDouble(), pigAfter.get("vz").getAsDouble());
        Expect.that(moved < 0.001 && vxz < 0.001, "no knockback: moved " + moved + ", v " + vxz + " " + pigAfter);
        Expect.that(after.get("charges").getAsInt() == 15, "the cut spent one charge, the click none: " + after);
        System.out.println("[crucible2] player at confirm: " + target);

        // control: no cut drawn, the same click on the same pig attacks it (it was in reach)
        c.waitTicks(30);
        c.lookAt(bx, Y + 1.45, bz);
        c.waitTicks(2);
        c.attack();
        c.waitTicks(4);
        JsonObject pigHit = mob(c, "pig", bx, bz);
        JsonObject control = q(c);
        Expect.that(pigHit == null || !pigHit.get("alive").getAsBoolean() || pigHit.get("hurt_time").getAsInt() > 0,
                "control: a plain click on the pig attacks it: " + pigHit);
        Expect.that(control.get("attack_clicks_consumed").getAsLong() == consumedBefore + 1,
                "control: AttackInput left the plain click alone: " + control);
        c.cmd("kill @e[type=!player]");

        // ---- 2. F5 swing (back and front)
        c.cmd("clear");
        c.waitTicks(5);
        c.cmd("give @s " + charged(c) + " 1");
        c.waitTicks(40);
        c.look(0, 0);
        for (String camera : new String[]{"third_person_back", "third_person_front"}) {
            c.setOption("camera", camera);
            c.waitTicks(10);
            c.screenshot("10-" + camera + "-still", new int[]{200, 60, 400, 400});
            // the swing lasts six ticks: no waits between the click, the probes and the shot
            c.attack();
            JsonObject mid = q(c, "server_swing_probe");
            c.screenshot("11-" + camera + "-midswing", new int[]{200, 60, 400, 400});
            JsonObject probed = q(c);
            c.screenshot("12-" + camera + "-midswing-b", new int[]{200, 60, 400, 400});
            Expect.that(mid.get("swinging").getAsBoolean(), camera + ": the client swings the arm: " + mid);
            JsonObject server = probed.getAsJsonObject("server_swing");
            Expect.that(server.has("swinging") && server.get("swinging").getAsBoolean(),
                    camera + ": the server swings too (broadcast to trackers): " + probed);
            c.waitTicks(20);
        }

        // ---- 3. first-person 1.7 swing against its keyframes
        c.setOption("camera", "first_person");
        c.look(0, -60);
        c.waitTicks(30);
        List<String> samples = new ArrayList<>();
        int checked = 0;
        // one sample per swing (a screenshot takes longer than the swing's keyframes): swing, wait i ticks, read and shoot
        int[] delays = {0, 1, 2, 3, 4, 6, 8, 10};
        for (int i = 0; i < delays.length; i++) {
            c.waitTicks(20);
            c.attack();
            if (delays[i] > 0) c.waitTicks(delays[i]);
            JsonObject s = q(c);
            long ms = s.get("swing17_ms").getAsLong();
            JsonArray v = s.getAsJsonArray("swing17");
            int offset = s.get("swing17_offset").getAsInt();
            if (ms >= 0) {
                c.screenshot(String.format("20-fp-swing-%02d-%dms", i, ms));
                double[] e = expected17(offset, ms);
                StringBuilder line = new StringBuilder("t=" + ms + " offset=" + offset + " got");
                for (int k = 0; k < 6; k++) line.append(' ').append(String.format("%.1f", v.get(k).getAsDouble()));
                line.append(" expected");
                for (int k = 0; k < 6; k++) line.append(' ').append(String.format("%.1f", e[k]));
                samples.add(line.toString());
                // the query reads the bus a moment after it reads the clock: allow ~3 ms of motion
                double tolRot = 6, tolTrans = 0.3;
                for (int k = 0; k < 6; k++) {
                    double tol = k < 3 ? tolRot : tolTrans;
                    Expect.that(Math.abs(v.get(k).getAsDouble() - e[k]) <= tol, "1.7 keyframes: " + line);
                }
                Expect.that(!s.get("swinging").getAsBoolean() || delays[i] == 0, "first person: no vanilla arm swing: " + s);
                checked++;
            }
        }
        for (String s : samples) System.out.println("[crucible2] " + s);
        Expect.that(checked >= 5, "sampled the 1.7 swing at least four times: " + samples);

        // pinned keyframes, offset 20: the old-hbm scene with the mapped swing next to the 1.7 renderer's own crucible
        c.look(0, 0);
        c.waitTicks(20);
        for (int ms : new int[]{0, 40, 75, 150, 225, 350, 475, 600}) {
            for (boolean ref : new boolean[]{false, true}) {
                JsonObject pin = new JsonObject();
                pin.addProperty("swing17_ms", ms);
                pin.addProperty("swing17_offset", 20);
                pin.addProperty("reference17", ref);
                c.query("hbm_crucible", pin);
                c.waitTicks(2);
                c.screenshot(String.format("21-pinned-%03dms-%s", ms, ref ? "1.7-reference" : "old-hbm-mapped"));
            }
        }
        JsonObject unpin = new JsonObject();
        unpin.addProperty("swing17_release", true);
        unpin.addProperty("reference17", false);
        c.query("hbm_crucible", unpin);

        // ---- 4. third-person effects at night
        c.cmd("time set 18000");
        c.look(30, 10);
        c.waitTicks(20);
        int bolts = 0;
        for (String camera : new String[]{"third_person_back", "third_person_front"}) {
            c.setOption("camera", camera);
            c.waitTicks(5);
            q(c); // reset the draw counters
            c.waitTicks(3);
            JsonObject fx = q(c);
            c.screenshot("30-" + camera + "-effects", new int[]{200, 60, 400, 400});
            Expect.that(fx.get("third_person_draws").getAsInt() > 0, camera + ": the third-person effects drew: " + fx);
            for (int i = 0; i < 120 && bolts == 0; i++) {
                c.waitTicks(1);
                JsonObject b = q(c);
                bolts = b.get("third_person_bolts").getAsInt();
                if (bolts > 0) c.screenshot("31-" + camera + "-lightning", new int[]{200, 60, 400, 400});
            }
            Expect.that(bolts > 0, camera + ": lightning drew on the third-person blade");
            bolts = 0;
        }
        // a frozen bolt (the harness's fixed one) for a sure lightning frame, front view (from behind the body hides the blade)
        c.setOption("camera", "third_person_front");
        JsonObject freeze = new JsonObject();
        freeze.addProperty("lightning_freeze", true);
        c.query("hbm_crucible", freeze);
        c.waitTicks(3);
        q(c);
        c.waitTicks(2);
        JsonObject frozen = q(c);
        c.screenshot("32-third-person-frozen-bolt", new int[]{200, 60, 400, 400});
        Expect.that(frozen.get("third_person_bolts").getAsInt() > 0, "the frozen bolt drew in third person: " + frozen);
        JsonObject thaw = new JsonObject();
        thaw.addProperty("lightning_freeze", false);
        c.query("hbm_crucible", thaw);

        // an empty crucible: the old-hbm cutout blade, no effects
        c.cmd("clear");
        c.waitTicks(3);
        c.cmd("give @s hbm:crucible 1");
        c.waitTicks(20);
        q(c);
        c.waitTicks(3);
        JsonObject empty = q(c);
        c.screenshot("33-third-person-empty", new int[]{200, 60, 400, 400});
        Expect.that(empty.get("third_person_draws").getAsInt() == 0, "an empty crucible draws no effects: " + empty);

        c.setOption("camera", "first_person");
        c.cmd("time set 6000");
        c.cmd("clear");
        c.cmd("kill @e[type=!player]");
        List<String> bad = c.logSinceMark("/(ERROR|FATAL)[]]|Exception");
        List<String> relevant = new ArrayList<>();
        for (String line : bad) if (!line.contains("Loaded ")) relevant.add(line);
        Expect.that(relevant.isEmpty(), "log clean since the mark: " + relevant);
    }
}
