import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * Close-up screenshots of duct connection shapes (straight, bend, T, cross, up and down, joined to barrels and a
 * refinery port) and of each pump, valve and gauge block at all four facings with a duct on each side.
 */
public final class RcFluidCloseups implements Scenario {

    private static final int X = 500;
    private static final int Y = 100;
    private static final int Z = 500;
    private static final int L = Y + 1;
    private static final String DUCT = "hbm:fluid_duct_neo{type:\"hbm:water\"}";
    private static final String[] PARTS = {"fluid_pump", "flow_control_pump", "fluid_valve", "fluid_switch",
            "fluid_counter_valve", "fluid_duct_gauge", "pipe_anchor", "fluid_duct_paintable"};
    private static final String[] FACINGS = {"north", "east", "south", "west"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("gamemode creative");
        c.cmd("forceload add " + (X - 20) + " " + (Z - 20) + " " + (X + 60) + " " + (Z + 60));
        c.waitTicks(40);
        c.cmd("fill " + (X - 10) + " " + Y + " " + (Z - 10) + " " + (X + 50) + " " + Y + " " + (Z + 50) + " minecraft:stone");
        c.cmd("fill " + (X - 10) + " " + L + " " + (Z - 10) + " " + (X + 50) + " " + (L + 8) + " " + (Z + 50) + " minecraft:air");
        c.cmd("time set 6000");

        // duct shapes, one cluster each, 6 apart along x at z = Z
        shape(c, X + 0, Z, new int[][]{{-1, 0, 0}, {0, 0, 0}, {1, 0, 0}});                          // straight
        shape(c, X + 6, Z, new int[][]{{-1, 0, 0}, {0, 0, 0}, {0, 0, 1}});                          // bend
        shape(c, X + 12, Z, new int[][]{{-1, 0, 0}, {0, 0, 0}, {1, 0, 0}, {0, 0, 1}});              // T
        shape(c, X + 18, Z, new int[][]{{-1, 0, 0}, {0, 0, 0}, {1, 0, 0}, {0, 0, 1}, {0, 0, -1}});  // cross
        shape(c, X + 24, Z, new int[][]{{0, -1, 0}, {0, 0, 0}, {0, 1, 0}, {1, 0, 0}});              // vertical + side
        shape(c, X + 30, Z, new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {2, 0, 1}, {2, 0, 2}, {3, 0, 2}}); // staircase
        // ducts into barrels, and into a refinery port
        c.cmd("setblock " + (X + 36) + " " + L + " " + Z + " " + DUCT);
        c.cmd("setblock " + (X + 35) + " " + L + " " + Z + " hbm:barrel_steel");
        c.cmd("setblock " + (X + 37) + " " + L + " " + Z + " hbm:barrel_plastic");
        c.cmd("setblock " + (X + 36) + " " + L + " " + (Z + 1) + " hbm:barrel_tcalloy");
        c.cmd("setblock " + (X + 36) + " " + (L + 1) + " " + Z + " hbm:red_barrel");
        c.cmd("setblock " + (X + 44) + " " + L + " " + (Z + 2) + " hbm:machine_refinery[facing=north]");
        c.cmd("setblock " + (X + 42) + " " + L + " " + (Z + 1) + " " + DUCT);
        c.cmd("setblock " + (X + 41) + " " + L + " " + (Z + 1) + " " + DUCT);
        c.waitTicks(30);
        for (int i = 0; i <= 6; i++) {
            shoot(c, "shape-" + i, X + i * 6 + (i == 6 ? 0 : 0), Z, 5.0);
        }
        shoot(c, "shape-machine", X + 41, Z + 1, 7.0);

        // parts at each facing: duct, part, duct along x
        int row = 0;
        for (String part : PARTS) {
            int z = Z + 8 + row * 5;
            for (int f = 0; f < 4; f++) {
                int x = X + f * 6;
                c.cmd("setblock " + (x - 1) + " " + L + " " + z + " " + DUCT);
                c.cmd("setblock " + (x + 1) + " " + L + " " + z + " " + DUCT);
                c.cmd("setblock " + x + " " + L + " " + z + " hbm:" + part + "[facing=" + FACINGS[f] + "]");
                String placed = c.block(x, L, z).get("block").getAsString();
                if (placed.equals("minecraft:air")) c.cmd("setblock " + x + " " + L + " " + z + " hbm:" + part);
            }
            row++;
        }
        c.waitTicks(30);
        row = 0;
        for (String part : PARTS) {
            int z = Z + 8 + row * 5;
            Expect.that(!c.block(X, L, z).get("block").getAsString().equals("minecraft:air"), part + " placed");
            shoot(c, "part-" + part + "-ns-es", X + 3, z, 9.0);
            shoot(c, "part-" + part + "-sw", X + 15, z, 5.0);
            row++;
        }
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }

    private static void shape(Client c, int x, int z, int[][] cells) {
        for (int[] d : cells) c.cmd("setblock " + (x + d[0]) + " " + (L + d[1]) + " " + (z + d[2]) + " " + DUCT);
    }

    private static void shoot(Client c, String name, int x, int z, double dist) {
        c.cmd("gamemode spectator");
        c.teleport(x + 0.5 + dist * 0.4, L + 1.8, z + 0.5 + dist);
        c.waitTicks(2);
        c.lookAt(x + 0.5, L + 0.4, z + 0.5);
        c.waitTicks(15);
        c.screenshot(name);
    }
}
