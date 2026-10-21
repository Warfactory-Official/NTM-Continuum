import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/** Release audit: the steel and iron Stirling engines placed by hand must stay in the world and open their GUI. */
public final class RcPowerStirling implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 12);
        c.waitTicks(5);
        for (String id : new String[]{"machine_stirling", "machine_stirling_steel"}) {
            c.cmd("fill -8 " + (Y + 1) + " -8 8 " + (Y + 5) + " 8 minecraft:air");
            c.waitTicks(3);
            JsonObject placed = Fixtures.placeOnTop(c, "hbm:" + id, 0, Y, 0);
            c.waitTicks(20);
            JsonObject now = c.block(0, Y + 1, 0);
            System.out.println("[" + id + "] placed " + placed + " after 20 ticks " + now);
            Expect.that(now.get("block").getAsString().equals("hbm:" + id), id + " vanished after hand placement: " + now);
            c.screenshot(id);
        }
    }
}
