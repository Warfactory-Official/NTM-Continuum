import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Scenario;

/** Helper between scenarios: respawns a dead player, clears the open screen and returns to creative at the platform. */
public final class RcRespawn implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String screen = c.screenClass();
        if (screen != null && screen.contains("DeathScreen")) {
            c.waitTicks(30);
            clickRespawn(c);
            c.waitTicks(60);
        } else if (c.player().get("health").getAsDouble() <= 0.0) {
            c.waitTicks(40);
            clickRespawn(c);
            c.waitTicks(60);
        }
        if (c.screenClass() != null) c.key("escape");
        c.cmd("gamemode creative");
        c.cmd("effect clear @s");
        c.cmd("tp @s 0 120 0");
        c.waitTicks(20);
    }

    private static void clickRespawn(Client c) {
        c.clickWidget("Respawn");
        c.waitTicks(20);
        String screen = c.screenClass();
        if (screen == null || !screen.contains("DeathScreen")) return;
        for (JsonElement w : c.screen().getAsJsonArray("widgets")) {
            JsonObject o = w.getAsJsonObject();
            if (o.get("message").getAsString().equals("Respawn")) c.click(o.get("center_x").getAsInt(), o.get("center_y").getAsInt());
        }
    }
}
