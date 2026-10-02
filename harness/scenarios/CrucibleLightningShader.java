import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The crucible lightning through the ported {@code crucible_lightning} GPU shader against the CPU evaluation of the same
 * arithmetic, same frame state: the deploy held on its last frame, one fixed bolt (seed 4815162342, age 4) frozen, the
 * camera still. Shots: {@code gpu}, {@code cpu} (by day and by night), and {@code none} (no bolt) to locate the bolt's
 * pixels; compared offline. "isolated" leaves out the blade's bloom and the (wall-clock animated) heat distortion so
 * the two lightning draws are the only thing that can differ.
 */
public final class CrucibleLightningShader implements Scenario {

    private static final int X = 340;
    private static final int Y = 100;
    private static final int Z = 340;

    private static JsonObject q(Client c, String key, Object value) throws Exception {
        JsonObject p = new JsonObject();
        if (value instanceof Boolean b) p.addProperty(key, b);
        else if (value != null) p.addProperty(key, value.toString());
        return c.query("hbm_crucible", p);
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 6);
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("weather clear");
        c.cmd("gamemode creative");
        c.setOption("camera", "first_person");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, 10);
        c.cmd("clear");
        c.cmd("time set 6000");
        c.waitTicks(5);
        c.cmd("give @s " + (c.node().startsWith("1.20") ? "hbm:crucible{\"hbm:charges\":16}" : "hbm:crucible[hbm:charges=16]") + " 1");
        c.waitTicks(40);
        for (String mode : new String[]{"full", "isolated"}) {
        q(c, "isolate", mode.equals("isolated"));
        for (String time : new String[]{"6000", "18000"}) {
            String tag = mode + "-" + (time.equals("6000") ? "day" : "night");
            c.cmd("time set " + time);
            c.waitTicks(20); // let the sky and lightmap settle before the three shots
            q(c, "lightning_freeze", true);
            q(c, "lightning_mode", "gpu");
            c.waitTicks(4);
            JsonObject gpu = q(c, null, null);
            Expect.that(gpu.get("fp_lightning").getAsInt() == 1, "one frozen bolt: " + gpu);
            c.screenshot(tag + "-gpu");
            q(c, "lightning_mode", "cpu");
            c.waitTicks(4);
            c.screenshot(tag + "-cpu");
            q(c, "lightning_mode", "gpu");
            JsonObject none = new JsonObject();
            none.addProperty("lightning_freeze", true);
            c.query("hbm_crucible", none);
            // the same frozen state without the bolt: clear it by freezing with no particles
            JsonObject clear = new JsonObject();
            clear.addProperty("lightning_clear", true);
            c.query("hbm_crucible", clear);
            c.waitTicks(4);
            c.screenshot(tag + "-none");
        }
        }
        q(c, "isolate", false);
        q(c, "lightning_freeze", false);
        c.cmd("time set 6000");
        c.cmd("clear");
        List<String> bad = c.logSinceMark("/(ERROR|FATAL)[]]|Exception");
        List<String> relevant = new ArrayList<>();
        for (String line : bad) if (!line.contains("Loaded ")) relevant.add(line);
        Expect.that(relevant.isEmpty(), "log clean since the mark: " + relevant);
    }
}
