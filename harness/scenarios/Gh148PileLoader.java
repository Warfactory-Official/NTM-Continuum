import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 148: the Chicago Pile fuel loader's lever and ram never moved on the client, and pile rods did not stack.
 * Places a loader on a wall by hand, clicks a natural uranium rod in (two rods must share one slot), fires a stroke
 * empty-handed and follows the client's ram travel: 1.7 runs 0 to 1 in seven ticks, holds five and returns.
 */
public final class Gh148PileLoader implements Scenario {

    private static final int X = 1580;
    private static final int Y = 100;
    private static final int Z = 1580;
    private static final int L = Y + 1;

    private final List<String> fails = new ArrayList<>();

    private void check(boolean ok, String message) {
        if (!ok) fails.add(message);
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, X, Y, Z, 10);
        c.cmd("time set 6000");
        c.markLog();
        c.cmd("fill " + (X - 1) + " " + L + " " + Z + " " + (X + 1) + " " + (L + 2) + " " + Z + " minecraft:stone");

        // the loader goes on the wall's south face
        Fixtures.giveOnly(c, "hbm:pile_loader", 1);
        c.teleport(X + 0.5, L, Z + 2.5);
        c.waitTicks(2);
        c.lookAt(X + 0.5, L + 0.5, Z + 0.99);
        Fixtures.waitForTarget(c, X, L, Z);
        c.use(2);
        c.waitTicks(4);
        check(c.block(X, L, Z + 1).get("block").getAsString().equals("hbm:pile_device"), "the loader must be placed");

        Fixtures.giveOnly(c, "hbm:pile_rod_mk2_nu", 2);
        int count = c.player().getAsJsonObject("main_hand").get("count").getAsInt();
        check(count == 2, "two natural uranium pile rods must stack in one slot as in 1.7, held " + count);
        aim(c);
        c.use(2);
        c.waitTicks(6);
        check(pile(c).get("loaded").getAsBoolean(), "the rod must be loaded: " + pile(c));
        c.screenshot("01-loaded");

        c.tickRate(4);
        double max = 0;
        boolean shotMid = false, shotOut = false;
        try {
            c.hotbar(9);
            c.waitTicks(1);
            c.use(2);
            for (int i = 0; i < 60; i++) {
                double travel = pile(c).get("travel").getAsDouble();
                max = Math.max(max, travel);
                if (!shotMid && travel > 0.3 && travel < 0.7) {
                    c.screenshot("02-stroke-mid");
                    shotMid = true;
                }
                if (!shotOut && travel >= 0.99) {
                    c.screenshot("03-stroke-out");
                    shotOut = true;
                }
                if (max >= 0.99 && travel == 0) break;
                c.waitTicks(1);
            }
        } finally {
            c.tickRate(20);
        }
        double end = pile(c).get("travel").getAsDouble();
        c.screenshot("04-returned");
        System.out.println("[Gh148] client ram travel max " + max + ", end " + end);
        check(max >= 0.99, "the client's ram must reach the end of its stroke, max " + max);
        check(end == 0, "the ram must return, at " + end);
        check(shotMid, "a mid-stroke frame was seen");
        check(c.logSinceMark("/(ERROR|FATAL)[]]").isEmpty(), "log clean since the mark");
        Expect.that(fails.isEmpty(), "issue 148: " + fails);
    }

    private static void aim(Client c) {
        c.teleport(X + 0.5, L, Z + 3.5);
        c.waitTicks(2);
        c.lookAt(X + 0.5, L + 0.4, Z + 1.5);
        Fixtures.waitForTarget(c, X, L, Z + 1);
    }

    private static JsonObject pile(Client c) {
        JsonObject params = new JsonObject();
        params.addProperty("mode", "pile");
        params.addProperty("x", X);
        params.addProperty("y", L);
        params.addProperty("z", Z + 1);
        return c.query("hbm_small", params);
    }
}
