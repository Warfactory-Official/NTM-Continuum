import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * A power cable draws an arm toward a foreign energy block (the dev-only test_fe_cell, a plain FE store that knows
 * nothing of HBM's connector interfaces; Team Reborn Energy on Fabric) on exactly the sides where one stands, the arm
 * is on the client too, power reaches the cell, and the arm leaves with the block. Run for red_cable and
 * red_cable_classic.
 */
public final class FxCableForeignEnergy implements Scenario {

    private static final int Y = 100;
    private static final int B = Y + 1;
    private final List<String> failures = new ArrayList<>();

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 12);
        c.cmd("fill -10 " + B + " -10 10 " + (B + 6) + " 10 minecraft:air");
        c.waitTicks(5);

        run(c, "hbm:red_cable", 0, "cable");
        run(c, "hbm:red_cable_classic", 6, "classic");

        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(hbm|power|energy|cable)");
        Expect.that(failures.isEmpty(), "soft checks failed: " + failures);
    }

    private void run(Client c, String cable, int x, String tag) {
        // cable first, then the neighbours: the arms must come from the neighbour updates
        Fixtures.setBlock(c, x, B, 0, cable);
        Fixtures.setBlock(c, x - 1, B, 0, "hbm:creative_generator");
        Fixtures.setBlock(c, x + 1, B, 0, "hbm:test_fe_cell");
        Fixtures.setBlock(c, x, B, -1, "minecraft:stone");
        c.teleport(x + 3.5, B, 3.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, B + 0.5, 0.5);
        c.waitTicks(10);
        c.screenshot(tag + "-01-east");
        top(c, x, tag + "-01-top");

        check(arm(c, x, "east", true), tag + ": arm east toward the foreign cell (server)");
        check(arm(c, x, "east", false), tag + ": arm east toward the foreign cell (client)");
        check(arm(c, x, "north", true) == false, tag + ": no arm toward plain stone");
        check(arm(c, x, "west", true), tag + ": arm west toward the generator");
        check(exposes(c, x + 1, "west"), tag + ": the cell exposes energy on the cable's face");

        c.waitTicks(100);
        long stored = stored(c, x + 1);
        check(stored > 0, tag + ": power reached the foreign cell: " + stored);

        // a second cell on top
        Fixtures.setBlock(c, x, B + 1, 0, "hbm:test_fe_cell");
        c.waitTicks(10);
        c.screenshot(tag + "-02-east-up");
        check(arm(c, x, "up", true) && arm(c, x, "east", true), tag + ": arms east and up");
        c.waitTicks(60);
        check(stored(c, x, B + 1) > 0, tag + ": the second cell charges too: " + stored(c, x, B + 1));

        Fixtures.setBlock(c, x + 1, B, 0, "minecraft:air");
        Fixtures.setBlock(c, x, B + 1, 0, "minecraft:air");
        c.waitTicks(10);
        c.screenshot(tag + "-03-removed");
        top(c, x, tag + "-03-top");
        check(!arm(c, x, "east", true) && !arm(c, x, "east", false) && !arm(c, x, "up", true),
                tag + ": arms leave with the foreign blocks");
        check(arm(c, x, "west", true), tag + ": the generator arm stays");
    }

    /** Straight down over the cable, the one view in which a short arm is not hidden by the block it touches. */
    private void top(Client c, int x, String name) {
        c.teleport(x + 0.5, B + 3, 0.5);
        c.look(0, 90);
        c.waitTicks(3);
        c.screenshot(name);
    }

    private boolean arm(Client c, int x, String side, boolean server) {
        JsonObject block = c.block(x, B, 0);
        JsonObject view = server ? block : clientView(c, x);
        return view.has("properties") && "true".equals(view.getAsJsonObject("properties").get(side).getAsString());
    }

    private JsonObject clientView(Client c, int x) {
        JsonObject p = new JsonObject();
        p.addProperty("x", x);
        p.addProperty("y", B);
        p.addProperty("z", 0);
        p.addProperty("server", false);
        return c.query("block", p);
    }

    private long stored(Client c, int x) {
        return stored(c, x, B);
    }

    private long stored(Client c, int x, int y) {
        return fe(c, "stored", x, y, null).get("stored").getAsLong();
    }

    private boolean exposes(Client c, int x, String side) {
        return fe(c, "exposes", x, B, side).get("exposes").getAsBoolean();
    }

    private JsonObject fe(Client c, String op, int x, int y, String side) {
        JsonObject p = new JsonObject();
        p.addProperty("op", op);
        p.addProperty("x", x);
        p.addProperty("y", y);
        p.addProperty("z", 0);
        if (side != null) p.addProperty("side", side);
        return c.query("hbm_fecell", p);
    }

    private void check(boolean ok, String what) {
        if (!ok) failures.add(what);
        System.out.println((ok ? "ok   " : "FAIL ") + what);
    }
}
