import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Scenario;

/** Rads of a survival player carrying one Pu-mix ingot (6.25 RAD/s), read every 40 ticks for 400 ticks. */
public final class RcWorldDoseTimeline implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        c.cmd("clear");
        c.cmd("gamemode survival");
        JsonObject p = new JsonObject();
        p.addProperty("set_rads", 0.0);
        c.query("hbm_props", p);
        c.cmd("give @s hbm:ingot_pu_mix 1");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            c.waitTicks(40);
            sb.append(String.format("%.1f ", c.query("hbm_props", new JsonObject()).getAsJsonObject("player").get("rads").getAsDouble()));
        }
        System.out.println("[RcWorldDoseTimeline] " + c.node() + " " + sb);
        c.cmd("clear");
        c.cmd("gamemode creative");
        c.query("hbm_props", p);
    }
}
