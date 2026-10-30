import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 94: lids on structural, storage, boiler and heater columns sat inside the column's top cell instead of
 * on top of it. Places a row of columns with concrete and glass lids beside a lidded fuel rod and an open boiler, and
 * shoots them from above and from the side: every lid must sit on the column top at the fuel rod's height.
 */
public final class Gh94RbmkLids implements Scenario {

    private static final int X = 1300;
    private static final int Y = 100;
    private static final int Z = 1300;
    private static final int L = Y + 1;

    private static final String[] COLUMNS = {
            "hbm:rbmk_blank", "hbm:rbmk_storage", "hbm:rbmk_boiler", "hbm:rbmk_heater", "hbm:rbmk_fuel_rod",
            "hbm:rbmk_reflector", "hbm:rbmk_boiler"
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        String hideGui = c.setOption("hide_gui", "true");
        try {
            Fixtures.platform(c, X, Y, Z, 20);
            c.cmd("time set 6000");
            // two blocks apart, so each column shows its sides
            for (int i = 0; i < COLUMNS.length; i++) {
                int x = X - 6 + i * 2;
                placeFrom(c, COLUMNS[i], x, Z, x + 0.5, Z - 2.5);
            }
            List<String> missing = new ArrayList<>();
            // the last boiler stays open to show its pipe stubs
            for (int i = 0; i < COLUMNS.length - 1; i++) {
                int x = X - 6 + i * 2;
                lid(c, i % 2 == 0 ? "hbm:rbmk_lid" : "hbm:rbmk_lid_glass", x, Z);
                if (!c.block(x, L, Z).get("state").getAsString().contains("lid=")
                        || c.block(x, L, Z).get("state").getAsString().contains("lid=none")) missing.add(COLUMNS[i]);
            }
            Expect.that(missing.isEmpty(), "lids placed on every column: missing on " + missing);
            c.cmd("clear");
            c.cmd("gamemode spectator");

            c.teleport(X + 0.5, L + 7, Z - 7.5);
            c.lookAt(X + 0.5, L + 4, Z + 0.5);
            c.waitTicks(30);
            c.screenshot("01-angle");
            c.teleport(X + 0.5, L + 4.6, Z - 9.5);
            c.lookAt(X + 0.5, L + 4.1, Z + 0.5);
            c.waitTicks(20);
            c.screenshot("02-side");
            StringBuilder stack = new StringBuilder();
            for (int y = L; y <= L + 5; y++) stack.append(y - L).append('=').append(c.block(X, y, Z).get("block").getAsString()).append(' ');
            c.chat("gh94: column at core+n " + stack);
            c.teleport(X + 0.5, L + 4.1 - 1.62, Z - 3.5);
            c.lookAt(X + 0.5, L + 4.1, Z + 0.5);
            c.waitTicks(20);
            c.screenshot("03-level");

            List<String> bad = new ArrayList<>();
            for (String line : c.logSinceMark("/(ERROR|FATAL)[]]|Missing model|Gem visual failed")) bad.add(line);
            Expect.that(bad.isEmpty(), "log clean since the mark: " + bad);
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
        }
    }

    /** Stands at (px, pz) on the platform, aims at the top of the floor block (x, Y, z) and places the item. */
    private static void placeFrom(Client c, String item, int x, int z, double px, double pz) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(px, L, pz);
        c.waitTicks(2);
        c.lookAt(x + 0.5, Y + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
    }

    /** Clicks the top of the column at (x, z) with the lid item from above. */
    private static void lid(Client c, String item, int x, int z) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(x + 0.5, L + 5, z - 1.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, L + 3.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        if (c.screenClass() != null) c.key("escape");
    }
}
