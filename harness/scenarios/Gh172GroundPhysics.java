import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GitHub issue 172: spent casings and giblets come to rest as in 1.7. 1.7's {@code EntityFX} keeps 0.7 of its ground
 * speed each tick, so a landed giblet stops within a few ticks. A resting casing in 1.7 never turns: its axis moves only
 * when a block really clipped it, which vanilla's collision also reports for any move under 1E-7.
 */
public final class Gh172GroundPhysics implements Scenario {

    private static final int Y = 100;
    private static final double MAX_TURN = 0.5D;
    private static final double MAX_GROUND_SPEED = 0.002D;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        List<String> failures = new ArrayList<>();
        try {
            Fixtures.platform(c, 0, Y, 0, 10);
            c.cmd("gamemode survival");
            c.cmd("gamerule sendCommandFeedback false");
            c.cmd("time set 6000");
            c.teleport(0.5, Y + 1, 0.5);
            casings(c, failures);
            giblets(c, failures);
            Expect.that(failures.isEmpty(), "casings and giblets rest as in 1.7: " + failures);
            Expect.logCleanSinceMark(c, "Exception|/(ERROR|FATAL)[]].*hbm");
        } finally {
            c.releaseAll();
            c.cmd("clear @s");
            c.cmd("gamemode creative");
            c.cmd("kill @e[type=minecraft:pig]");
        }
    }

    /** Eight STG77 rounds, then each casing's yaw and pitch twice, 20 ticks apart, while it lies on the ground. */
    private static void casings(Client c, List<String> failures) throws Exception {
        c.cmd("clear @s");
        c.cmd("give @s hbm:gun_stg77");
        c.cmd("give @s hbm:ammo_standard_r556_sp 64");
        c.hotbar(1);
        c.waitTicks(40);
        c.key("r");
        c.waitTicks(100);
        c.look(90, 10);
        c.waitTicks(10);
        for (int i = 0; i < 8; i++) {
            c.attack(1);
            c.waitTicks(4);
        }
        c.waitTicks(80);
        double cx = 0, cy = 0, cz = 0;
        JsonArray resting = c.query("hbm_casing_states", new JsonObject()).getAsJsonArray("casings");
        for (JsonElement e : resting) {
            cx += e.getAsJsonArray().get(0).getAsDouble() / resting.size();
            cy += e.getAsJsonArray().get(1).getAsDouble() / resting.size();
            cz += e.getAsJsonArray().get(2).getAsDouble() / resting.size();
        }
        if (!resting.isEmpty()) c.lookAt(cx, cy, cz);
        c.waitTicks(5);
        c.screenshot("casings-resting");
        Map<Integer, double[]> first = casingAngles(c);
        c.waitTicks(20);
        Map<Integer, double[]> second = casingAngles(c);
        double turn = 0D;
        int compared = 0;
        for (Map.Entry<Integer, double[]> e : first.entrySet()) {
            double[] b = second.get(e.getKey());
            if (b == null) continue;
            compared++;
            turn = Math.max(turn, Math.max(Math.abs(b[0] - e.getValue()[0]), Math.abs(b[1] - e.getValue()[1])));
        }
        System.out.println("[Gh172] " + c.node() + " casings compared " + compared + ", largest turn at rest " + turn);
        if (compared < 4) failures.add("only " + compared + " casings to compare");
        if (turn > MAX_TURN) failures.add("a resting casing turned " + turn + " degrees in 20 ticks");
    }

    /** Casings by their age at the first sample, so the same casing is matched across samples. */
    private static Map<Integer, double[]> casingAngles(Client c) {
        Map<Integer, double[]> angles = new HashMap<>();
        JsonArray rows = c.query("hbm_casing_states", new JsonObject()).getAsJsonArray("casings");
        int index = 0;
        for (JsonElement e : rows) {
            JsonArray row = e.getAsJsonArray();
            angles.put(index++, new double[] {row.get(3).getAsDouble(), row.get(4).getAsDouble()});
        }
        return angles;
    }

    /**
     * A pig's giblets; 60 ticks later no giblet lying on the ground may still slide. One burst in 15 is thrown ten times
     * as hard (1.7's "stratosphere" roll) and is still in the air then, so such a burst is tried again.
     */
    private static void giblets(Client c, List<String> failures) throws Exception {
        for (int attempt = 0; attempt < 4; attempt++) {
            c.cmd("summon minecraft:pig 0.5 " + (Y + 1) + " 4.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}");
            c.waitTicks(10);
            JsonObject spawn = new JsonObject();
            spawn.addProperty("spawn", true);
            int count = c.query("hbm_giblets", spawn).getAsJsonArray("giblets").size();
            c.cmd("kill @e[type=minecraft:pig]");
            c.waitTicks(60);
            c.lookAt(0.5, Y + 1, 4.5);
            c.waitTicks(2);
            c.screenshot("giblets-resting-" + attempt);
            JsonArray rows = c.query("hbm_giblets", new JsonObject()).getAsJsonArray("giblets");
            double fastest = 0D;
            int grounded = 0;
            for (JsonElement e : rows) {
                JsonArray row = e.getAsJsonArray();
                if (Math.abs(row.get(4).getAsDouble()) > 1E-6) continue;
                grounded++;
                fastest = Math.max(fastest, row.get(3).getAsDouble());
            }
            System.out.println("[Gh172] " + c.node() + " giblets " + count + ", grounded " + grounded
                    + ", fastest grounded speed " + fastest);
            if (fastest > MAX_GROUND_SPEED) failures.add("a landed giblet still slides at " + fastest + " blocks/tick");
            if (grounded > 0) return;
            c.waitTicks(120);
        }
        failures.add("no giblet landed");
    }
}
