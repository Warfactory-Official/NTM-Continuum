import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * The RBMK dials as world rules: /gamerule accepts a double, an int and a boolean, tab completion lists them, the
 * client sees the values the server holds, and the values survive leaving and
 * rejoining the world.
 */
public final class RcNukeRbmkDials implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.teleport(0.5, 101, 0.5);
        c.waitTicks(20);
        c.markLog();
        try {
            c.cmd("gamerule dialControlSpeed 2.5");
            c.cmd("gamerule dialColumnHeight 6");
            c.cmd("gamerule dialDisableMeltdowns true");
            c.waitTicks(10);
            check(c, "after setting");

            JsonObject suggest = new JsonObject();
            suggest.addProperty("input", "gamerule dialCo");
            String names = c.query("hbm_suggest", suggest).get("suggestions").toString();
            Expect.that(names.contains("dialControlSpeed") && names.contains("dialControlSurgeMod") && names.contains("dialColumnHeight")
                    && names.contains("dialColumnHeatFlow"), "tab completion lists the dials: " + names);
            suggest.addProperty("input", "gamerule dialDisableMeltdowns ");
            String values = c.query("hbm_suggest", suggest).get("suggestions").toString();
            Expect.that(values.contains("true") && values.contains("false"), "a boolean dial suggests true and false: " + values);

            c.screenshot("01-dials-set");
            String world = c.currentWorld();
            c.leaveWorld(60_000L);
            Expect.that(!c.inWorld(), "the client left the world");
            c.joinWorld(world != null ? world : "harness", 180_000L);
            c.waitInWorld(120_000L);
            c.waitTicks(40);
            check(c, "after rejoining");
        } finally {
            c.cmd("gamerule dialControlSpeed 1");
            c.cmd("gamerule dialColumnHeight 4");
            c.cmd("gamerule dialDisableMeltdowns false");
        }
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static void check(Client c, String when) {
        JsonObject dials = c.query("hbm_rbmk_dials", new JsonObject());
        Expect.that(dials.get("controlSpeed").getAsDouble() == 2.5D, "client sees dialControlSpeed 2.5 " + when + ": " + dials);
        Expect.that(dials.get("columnHeight").getAsInt() == 6, "client sees dialColumnHeight 6 " + when + ": " + dials);
        Expect.that(dials.get("disableMeltdowns").getAsBoolean(), "client sees dialDisableMeltdowns true " + when + ": " + dials);
    }
}
