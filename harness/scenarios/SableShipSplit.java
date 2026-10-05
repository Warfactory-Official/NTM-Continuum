import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * A Sable ship split in two with an instanced machine on one half. Two pads joined by a one-block bridge, a Fat Man
 * placed by hand on the east pad, assembled; the bridge is broken by hand (Sable splits the ship) and the newest piece
 * is teleported away. Reported: a copy of the bomb's model stayed at its old place relative to the other half until
 * relog. The scenario counts the live instanced machine visuals (Core's {@code gem} query) after each step: one Fat
 * Man must mean one visual, and screenshots show where the model is drawn.
 *
 * <p>1.21.1 with Sable: {@code ${env:ORG_GRADLE_PROJECT_sable.run}="true"} and {@code up --rebuild}.
 */
public class SableShipSplit implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    @Override
    public void run(Client c) throws Exception {
        c.key("escape");
        c.waitTicks(2);
        c.cmd("sable paused false");
        c.cmd("sable remove @e");
        c.waitTicks(5);
        // marked after the cleanup: removing an earlier run's ship makes Sable's client log an ERROR for movement
        // packets still in flight for it
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 16);

        // west pad, a one-block bridge, east pad
        c.cmd("fill " + (X - 6) + " " + (Y + 1) + " " + (Z + 3) + " " + (X - 2) + " " + (Y + 1) + " " + (Z + 7) + " minecraft:iron_block");
        c.cmd("fill " + (X - 1) + " " + (Y + 1) + " " + (Z + 5) + " " + (X + 1) + " " + (Y + 1) + " " + (Z + 5) + " minecraft:iron_block");
        c.cmd("fill " + (X + 2) + " " + (Y + 1) + " " + (Z + 3) + " " + (X + 6) + " " + (Y + 1) + " " + (Z + 7) + " minecraft:iron_block");
        c.waitTicks(2);
        JsonObject placed = Fixtures.placeOnTop(c, "hbm:nuke_man", X + 4, Y + 1, Z + 5);
        Expect.that(placed.get("block").getAsString().startsWith("hbm:nuke_man"), "Fat Man placed by hand: " + placed);
        c.cmd("clear");
        c.waitTicks(10);

        c.cmd("sable assemble area " + (X - 6) + " " + (Y + 1) + " " + (Z + 3) + " " + (X + 6) + " " + (Y + 3) + " " + (Z + 7));
        c.waitTicks(30);
        int assembled = visuals(c);

        // watch from the south, then break the middle of the bridge by hand
        c.teleport(X + 0.5, Y + 1, Z - 1.5);
        c.waitTicks(3);
        c.lookAt(X + 0.5, Y + 1.5, Z + 5.5);
        c.waitTicks(5);
        c.screenshot("10-assembled");
        // within reach of the bridge (it is 2.5 blocks ahead), break its middle block
        c.teleport(X + 0.5, Y + 1, Z + 2.5);
        c.waitTicks(3);
        c.lookAt(X + 0.5, Y + 1.5, Z + 5.0);
        c.waitTicks(2);
        c.attack(3);
        c.waitTicks(40);
        c.teleport(X + 0.5, Y + 1, Z - 1.5);
        c.waitTicks(3);
        c.lookAt(X + 0.5, Y + 1.5, Z + 5.5);
        c.waitTicks(5);
        int split = visuals(c);
        c.screenshot("20-split");

        // the newest sub-level (the piece split off) moves away, still over the platform (past its edge it falls)
        c.cmd("sable teleport @l " + (X + 0.5) + " " + (Y + 3) + " " + (Z + 12.5));
        c.waitTicks(40);
        int moved = visuals(c);
        c.lookAt(X + 0.5, Y + 2, Z + 10.5);
        c.waitTicks(5);
        c.screenshot("30-piece-moved-away");

        Expect.that(assembled <= 1 && split <= 1 && moved <= 1,
                "one Fat Man, at most one instanced visual: assembled " + assembled + ", split " + split + ", moved " + moved);
        // Sable's client logs this ERROR itself while a split hands blocks to the new sub-level; not HBM's or Core's
        Expect.logCleanSinceMark(c,
                "/(ERROR|FATAL)[]](?! \\[Sable\\]: Received a sub-level movement packet for a non-existent sub-level)");
    }

    private static int visuals(Client c) {
        JsonObject gem = c.query("gem", new JsonObject());
        return gem.has("visuals") ? gem.get("visuals").getAsInt() : -1;
    }
}
