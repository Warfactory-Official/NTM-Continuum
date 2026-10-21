import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Scenario;

/** Digamma of a survival player carrying a damaged holotape (DIGAMMA 1000) for 20 s, read every 5 s. */
public final class RcWorldDigamma implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        c.cmd("clear");
        c.cmd("gamemode survival");
        c.cmd("give @s hbm:holotape_damaged 1");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            c.waitTicks(100);
            JsonObject pl = c.query("hbm_props", new JsonObject()).getAsJsonObject("player");
            sb.append(String.format("digamma=%.4f ", pl.get("digamma").getAsDouble()));
        }
        System.out.println("[RcWorldDigamma] " + c.node() + " " + sb + "effects=" + c.player().getAsJsonArray("effects"));
        c.screenshot("digamma-hud");
        c.cmd("clear");
        c.cmd("gamemode creative");
    }
}
