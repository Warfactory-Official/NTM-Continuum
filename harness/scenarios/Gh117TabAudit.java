import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Scenario;

/** GitHub issue 117: items that 1.7's creative tabs show are listed by JEI, which reads the tabs, carts through vanilla's. */
public final class Gh117TabAudit implements Scenario {

    private static final String[][] SEARCHES = {
            {"Cart", "carts"}, {"Transport Drone", "drones"}, {"Grenade", "grenade-parts"}, {"Plutonic Quartz", "quartz"}};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        while (c.screen().get("open").getAsBoolean()) {
            c.key("escape");
            c.waitTicks(5);
        }
        c.cmd("gamemode survival");
        c.waitTicks(10);
        c.key("e");
        c.waitTicks(20);
        JsonObject screen = c.screen();
        double w = 2 * screen.get("left").getAsDouble() + screen.get("image_width").getAsDouble();
        double h = 2 * screen.get("top").getAsDouble() + screen.get("image_height").getAsDouble();
        for (String[] search : SEARCHES) {
            c.click(w - 85, h - 16);
            c.waitTicks(5);
            for (int i = 0; i < 20; i++) c.key("backspace");
            c.type(search[0]);
            c.waitTicks(20);
            c.mouseMove(2, 2);
            c.screenshot(search[1]);
        }
        c.key("escape");
        c.waitTicks(10);
        c.cmd("gamemode creative");
    }
}
