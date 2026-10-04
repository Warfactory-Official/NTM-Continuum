import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Fix wave 1C (History §19ej-1C): a RadAway bag drunk for real removes 1.7's 1 RAD a tick (RadAway I for 140 ticks),
 * and carrying a Pu-239 ingot (5 RAD/s, 1.7) raises the player's rads by that much in survival.
 */
public final class Fix1CRadAway implements Scenario {

    private static double rads(Client c) {
        return c.query("hbm_props", new JsonObject()).getAsJsonObject("player").get("rads").getAsDouble();
    }

    private static void setRads(Client c, double rads) {
        JsonObject params = new JsonObject();
        params.addProperty("set_rads", rads);
        c.query("hbm_props", params);
    }

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        Fixtures.platform(c, 0, 100, 0, 6);
        c.teleport(0.5, 101, 0.5);
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:resistance 900 4 true");
        c.cmd("clear");
        c.waitTicks(5);

        // RadAway: 1 RAD per tick for 140 ticks
        setRads(c, 500);
        Fixtures.giveOnly(c, "hbm:radaway", 1);
        c.look(0, 60);
        c.waitTicks(2);
        long t0 = c.status().get("game_time").getAsLong();
        c.use(2);
        c.waitTicks(60);
        long t1 = c.status().get("game_time").getAsLong();
        double after = rads(c);
        double expected = 500 - (t1 - t0);
        c.screenshot("radaway-drunk");
        Expect.that(Math.abs(after - expected) <= 8,
                "RadAway removed 1 RAD a tick: 500 -> " + after + " after " + (t1 - t0) + " ticks (expected ~" + expected + ")");
        Expect.that(after < 470 && after > 420, "RadAway is 1.7 strength, not 0.05 RAD/t nor 25 RAD/t: " + after);

        // carrying a Pu-239 ingot: 5 RAD/s
        c.cmd("effect clear @s hbm:radaway");
        Fixtures.giveOnly(c, "hbm:ingot_pu239", 1);
        c.waitTicks(40); // let the carried-hazard scan resolve the new slot before measuring
        setRads(c, 0);
        long t2 = c.status().get("game_time").getAsLong();
        c.waitTicks(100);
        long t3 = c.status().get("game_time").getAsLong();
        double carried = rads(c);
        double expectedCarry = 5.0 * (t3 - t2) / 20.0;
        c.screenshot("pu239-carried");
        Expect.that(carried > expectedCarry * 0.7 && carried < expectedCarry * 1.3,
                "a carried Pu-239 ingot gives 5 RAD/s: " + carried + " in " + (t3 - t2) + " ticks (expected ~" + expectedCarry + ")");

        c.cmd("clear");
        setRads(c, 0);
        c.cmd("gamemode creative");
    }
}
