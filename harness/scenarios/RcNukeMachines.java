import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Every reactor and high-energy machine placed by hand from each of the four sides, a screenshot of the four
 * copies, and the menu of the first one opened. Block entities are read back for errors.
 */
public final class RcNukeMachines implements Scenario {

    private static final int Y = 100;
    private static final int X0 = 400;
    private static final int Z0 = 400;
    private static final int STEP = 26;

    static final String[] MACHINES = {"hbm:machine_waste_drum", "hbm:pwr_controller", "hbm:fusion_boiler", "hbm:fusion_collector",
            "hbm:dfc_core", "hbm:dfc_emitter", "hbm:dfc_receiver", "hbm:dfc_injector", "hbm:dfc_stabilizer"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("gamemode creative");
        c.waitTicks(3);
        for (int m = 0; m < MACHINES.length; m++) {
            String id = MACHINES[m];
            int z = Z0 + m * STEP;
            c.teleport(X0 + 0.5, Y + 1, z + 0.5);   // fill only works in loaded chunks
            c.waitTicks(100);
            c.cmd("fill " + (X0 - 12) + " " + Y + " " + (z - 12) + " " + (X0 + 4 * STEP) + " " + Y + " " + (z + 12) + " minecraft:stone");
            for (int f = 0; f < 4; f++) {
                c.cmd("fill " + (X0 + f * STEP - 12) + " " + (Y + 1) + " " + (z - 12) + " " + (X0 + f * STEP + 12) + " " + (Y + 20) + " " + (z + 12) + " minecraft:air");
            }
            c.waitTicks(3);
            int[][] side = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};
            int placed = 0;
            for (int f = 0; f < 4; f++) {
                int x = X0 + f * STEP;
                Fixtures.giveOnly(c, id, 1);
                c.teleport(x + 0.5 + side[f][0] * 1.25, Y + 1, z + 0.5 + side[f][1] * 1.25);
                c.waitTicks(2);
                c.lookAt(x + 0.5, Y + 0.98, z + 0.5);
                c.waitTicks(1);
                c.use(2);
                c.waitTicks(4);
                boolean found = false;
                for (int dx = -3; dx <= 3 && !found; dx++) {
                    for (int dz = -3; dz <= 3 && !found; dz++) {
                        for (int dy = 1; dy <= 3 && !found; dy++) {
                            found = c.block(x + dx, Y + dy, z + dz).get("block").getAsString().equals(id);
                        }
                    }
                }
                if (found) placed++;
                else System.out.println("PLACE " + id + " facing " + f + " not found");
            }
            System.out.println("machine " + id + " placed " + placed + "/4");
            Expect.that(placed >= 1, id + " could not be placed by hand");
            // menu of the first copy
            c.teleport(X0 + 0.5, Y + 1, z + 3.5);
            c.waitTicks(3);
            Fixtures.interact(c, X0, Y + 2, z);
            c.waitTicks(4);
            String screen = c.screenClass();
            if (screen == null || screen.equals("null")) {
                c.teleport(X0 + 0.5, Y + 1, z + 2.5);
                c.waitTicks(3);
                Fixtures.interact(c, X0, Y + 1, z);
                c.waitTicks(4);
                screen = c.screenClass();
            }
            System.out.println("menu " + id + " -> " + screen);
            if (screen != null && !screen.equals("null")) {
                c.screenshot("m" + String.format("%02d", m) + "-gui-" + id.substring(4));
                c.key("escape");
                c.waitTicks(4);
            }
            view(c, "m" + String.format("%02d", m) + "-" + id.substring(4), X0 + 1.5 * STEP, Y + 12, z - 22, X0 + 1.5 * STEP, Y + 3, z);
        }
        for (String error : c.logSinceMark("(ERROR|FATAL|Exception in thread|ReportedException)")) {
            Expect.that(error.contains("Realms"), "unexpected error: " + error);
        }
    }

    private static void view(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.hotbar(8);
        c.waitTicks(2);
        c.cmd("gamemode spectator");
        c.waitTicks(2);
        c.teleport(x, y, z);
        c.waitTicks(14);
        c.lookAt(tx, ty, tz);
        c.waitTicks(12);
        c.screenshot(name);
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }
}
