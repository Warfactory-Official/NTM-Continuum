import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/** In-game acceptance for the 300-block rangefinder and the manual designator's coordinate payload. */
public final class TargetingTools implements Scenario {
    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 12);

        // A wall eight blocks down +Z makes the source's eye-to-hit range legible in the action bar.
        c.cmd("fill -2 101 8 2 104 8 minecraft:stone");
        Fixtures.giveOnly(c, "hbm:rangefinder", 1);
        c.teleport(0.5, Y + 1, 0.5);
        c.look(0, 0);
        c.waitTicks(2);
        c.use(1);
        c.waitTicks(2);
        c.screenshot("10-rangefinder-hit-distance");

        // The source coordinate editor starts at (0,0). Drive its real +/- and set buttons, then
        // close/reopen to force a server round trip before capturing the resulting coordinates.
        Fixtures.giveOnly(c, "hbm:designator_manual", 1);
        c.use(1);
        c.waitScreen("GUIScreenDesignator", 5_000L);
        JsonObject screen = c.screen();
        Expect.that(screen.get("class").getAsString().endsWith("GUIScreenDesignator"),
                "manual designator opened its coordinate screen: " + screen);
        c.screenshot("20-manual-designator-open");

        JsonObject status = c.status();
        double left = (status.get("gui_width").getAsDouble() - 176D) / 2D;
        double top = (status.get("gui_height").getAsDouble() - 178D) / 2D;
        c.click(left + 34, top + 35);    // X += 1
        c.waitTicks(3);
        c.click(left + 142, top + 45);   // set X to rounded player X
        c.waitTicks(5);
        c.screenshot("30-manual-designator-edited");
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(5);
        c.use(1);
        c.waitScreen("GUIScreenDesignator", 5_000L);
        c.screenshot("40-manual-designator-server-roundtrip");
        c.key("escape");
        c.waitScreen("none", 5_000L);

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture");
    }
}
