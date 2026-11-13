import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 172: 1.7 spawns a beam with its measured length and exact angles, so its first frame is already where
 * it ends. Vanilla's spawn packet carries the angles in 1/256 turns and no length; a beam drawn from that alone points
 * up to 0.7 degrees off and runs 250 blocks for its first tick. Each laser rifle shot at a slow tick rate records its
 * first rendered frame.
 */
public final class Gh172BeamFirstFrame implements Scenario {

    private static final int Y = 100;
    private static final double[][] AIMS = {{13, -7}, {101, 3}, {-77, -21}, {170, 11}, {44, 0.3}, {-133, 8}, {61, -12},
            {-12, 2}, {150, -4}, {-170, 6}};

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        List<String> failures = new ArrayList<>();
        try {
            Fixtures.platform(c, 0, Y, 0, 8);
            c.cmd("gamemode creative");
            c.cmd("gamerule sendCommandFeedback false");
            c.cmd("time set 18000");
            c.cmd("clear @s");
            c.cmd("give @s hbm:gun_lasrifle");
            c.cmd("give @s hbm:ammo_standard_capacitor 64");
            c.hotbar(1);
            c.teleport(0.5, Y + 1, 0.5);
            c.waitTicks(50);
            c.key("r", 2);
            c.waitTicks(100);
            JsonObject reset = new JsonObject();
            reset.addProperty("reset", true);
            c.query("hbm_beam_frames", reset);
            for (double[] aim : AIMS) {
                c.look(aim[0], aim[1]);
                c.waitTicks(10);
                float rate = c.tickRate(2F);
                c.attack(1);
                c.screenshot("beam-" + (int) aim[0]);
                c.waitTicks(4);
                c.tickRate(rate);
                c.waitTicks(20);
            }
            int seen = 0;
            for (JsonElement e : c.query("hbm_beam_frames", new JsonObject()).getAsJsonArray("beams")) {
                JsonObject beam = e.getAsJsonObject();
                seen++;
                System.out.println("[Gh172] " + c.node() + " beam " + beam);
                if (!beam.get("first_ready").getAsBoolean()) failures.add("beam " + beam.get("id")
                        + " drew its first frame without its measured length");
                if (beam.get("turn").getAsDouble() > 0.01D) failures.add("beam " + beam.get("id") + " turned "
                        + beam.get("turn") + " degrees after its first frame");
            }
            if (seen < AIMS.length) failures.add("only " + seen + " beams seen");
            Expect.that(failures.isEmpty(), "every beam's first frame is its final one: " + failures);
            Expect.logCleanSinceMark(c, "Exception|/(ERROR|FATAL)[]].*hbm");
        } finally {
            c.tickRate(20F);
            c.cmd("clear @s");
        }
    }
}
