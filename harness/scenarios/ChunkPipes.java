import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * The network blocks 1.7 drew into the world mesh (red cable, fluid ducts, box ducts, box cables, pneumatic tubes),
 * placed by hand as a straight run, a T junction, a bend and a lone block, one row per kind, and shot row by row. The
 * oil-filled rows check the fluid tint (the neo duct's overlay, the white box duct's lightened colour). Checks: every
 * block is placed and connects; the shots are for the eye (shapes, sprites, tints, lighting).
 */
public class ChunkPipes implements Scenario {

    private static final int Y = 100, D = Y + 1;
    private static final String[] KINDS = {"hbm:red_cable", "hbm:fluid_duct_neo", "hbm:fluid_duct_neo_colored",
            "hbm:fluid_duct_box_silver_2", "hbm:fluid_duct_box_white_2", "hbm:fluid_duct_exhaust_2",
            "hbm:red_cable_box_2", "hbm:pneumatic_tube"};
    /** Cells relative to a row's origin: a run (0..2), a branch off its middle, a bend at its end, a lone block. */
    private static final int[][] CELLS = {{0, 0}, {1, 0}, {2, 0}, {1, 1}, {2, -1}, {5, 0}};

    @Override
    public void run(Client c) throws Exception {
        c.key("escape");
        c.waitTicks(2);
        c.resetWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 16, 20);
        c.cmd("fill -6 " + D + " -2 12 " + (D + 6) + " 40 minecraft:air");
        for (int k = 0; k < KINDS.length; k++) {
            int z = 2 + k * 4;
            for (int[] cell : CELLS) {
                JsonObject placed = Fixtures.placeOnTop(c, KINDS[k], cell[0], Y, z + cell[1]);
                Expect.equal(KINDS[k], placed.get("block").getAsString(), KINDS[k] + " placed at " + cell[0] + "," + (z + cell[1]));
            }
            if (KINDS[k].contains("neo") || KINDS[k].contains("white")) {
                for (int[] cell : CELLS) {
                    c.cmd("data merge block " + cell[0] + " " + D + " " + (z + cell[1]) + " {type:\"hbm:oil\"}");
                }
            }
        }
        c.cmd("clear");
        c.waitTicks(20);

        for (int k = 0; k < KINDS.length; k++) {
            int z = 2 + k * 4;
            JsonObject junction = c.block(1, D, z);
            Expect.that(junction.toString().contains("true"), KINDS[k] + " junction has connections: " + junction);
            c.teleport(1.5, D, z - 3.5);
            c.lookAt(2.0, D + 0.3, z);
            c.waitTicks(10);
            c.screenshot(String.format("%02d-%s", 10 + k, KINDS[k].substring(4)));
        }
        c.teleport(-5.5, D + 6, 2.5);
        c.lookAt(1.5, D, 16);
        c.waitTicks(10);
        c.screenshot("30-overview");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }
}
