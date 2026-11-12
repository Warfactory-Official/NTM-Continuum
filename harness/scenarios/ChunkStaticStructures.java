import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Chunk-model blocks against each other, where boundary faces cull: 1.7's crane (pasted by the debug wand, the same
 * {@code NBTStructure} worldgen uses) and an oil-rig-like cluster (a scaffold cube, a walled room with corners and a
 * roof, spotlights on walls and under the roof, a pole column), shot from eight angles at two heights. For the eye only.
 */
public class ChunkStaticStructures implements Scenario {

    private static final int Y = 100, D = Y + 1;
    private static final int CRANE_X = 2000, RIG_X = 2200;

    @Override
    public void run(Client c) throws Exception {
        c.key("escape");
        c.waitTicks(2);
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("time set 6000");
        c.cmd("gamerule doDaylightCycle false");

        Fixtures.platform(c, CRANE_X, Y, 0, 40);
        c.cmd("gamemode creative");
        c.teleport(CRANE_X + 0.5, D + 6, 20.5);
        c.lookAt(CRANE_X + 0.5, Y, 0.5);
        Fixtures.giveOnly(c, "hbm:wand_d", 1);
        c.waitTicks(10);
        c.use();
        c.waitTicks(60);

        Fixtures.platform(c, RIG_X, Y, 0, 24);
        rig(c);
        c.cmd("clear");
        c.cmd("gamemode spectator");
        c.key("f1");
        try {
            orbit(c, CRANE_X + 0.5, D + 30, 0.5, 48, "00-crane");
            orbit(c, RIG_X + 0.5, D + 3, 0.5, 14, "01-rig");
        } finally {
            c.key("f1");
            c.cmd("gamemode creative");
        }
    }

    private static void rig(Client c) {
        int x0 = RIG_X - 8, z0 = -2;
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    String orient = y == 1 ? "vertical_north_south" : "horizontal_north_south";
                    c.cmd("setblock " + (x0 + x) + " " + (D + y) + " " + (z0 + z) + " hbm:steel_scaffold[orient=" + orient + "]");
                }
            }
        }
        int rx = RIG_X + 2, rz = -3;
        for (int y = 0; y < 3; y++) {
            for (int i = 1; i < 4; i++) {
                c.cmd("setblock " + (rx + i) + " " + (D + y) + " " + rz + " hbm:steel_wall[facing=south]");
                c.cmd("setblock " + (rx + i) + " " + (D + y) + " " + (rz + 4) + " hbm:steel_wall[facing=north]");
                c.cmd("setblock " + rx + " " + (D + y) + " " + (rz + i) + " hbm:steel_wall[facing=east]");
                c.cmd("setblock " + (rx + 4) + " " + (D + y) + " " + (rz + i) + " hbm:steel_wall[facing=west]");
            }
            c.cmd("setblock " + rx + " " + (D + y) + " " + rz + " hbm:steel_corner[facing=south]");
            c.cmd("setblock " + (rx + 4) + " " + (D + y) + " " + rz + " hbm:steel_corner[facing=west]");
            c.cmd("setblock " + (rx + 4) + " " + (D + y) + " " + (rz + 4) + " hbm:steel_corner[facing=north]");
            c.cmd("setblock " + rx + " " + (D + y) + " " + (rz + 4) + " hbm:steel_corner[facing=east]");
        }
        c.cmd("fill " + rx + " " + (D + 3) + " " + rz + " " + (rx + 4) + " " + (D + 3) + " " + (rz + 4) + " hbm:steel_roof[facing=north]");
        c.cmd("fill " + (rx + 6) + " " + D + " -1 " + (rx + 6) + " " + (D + 4) + " -1 minecraft:iron_block");
        c.cmd("setblock " + (rx + 6) + " " + (D + 2) + " 0 hbm:spotlight_halogen[facing=south]");
        c.cmd("setblock " + (rx + 7) + " " + (D + 2) + " -1 hbm:spotlight_incandescent[facing=east]");
        c.cmd("fill " + (rx + 1) + " " + (D + 2) + " " + (rz + 1) + " " + (rx + 3) + " " + (D + 2) + " " + (rz + 1) + " hbm:spotlight_fluoro[facing=down]");
        c.cmd("fill " + (RIG_X - 3) + " " + D + " 5 " + (RIG_X - 3) + " " + (D + 5) + " 5 hbm:steel_poles[facing=north]");
        c.cmd("setblock " + (RIG_X - 3) + " " + (D + 6) + " 5 hbm:pole_top");
        c.cmd("fill " + (RIG_X - 6) + " " + D + " 6 " + RIG_X + " " + D + " 6 hbm:barbed_wire[facing=east]");
    }

    private static void orbit(Client c, double x, double y, double z, double r, String label) throws Exception {
        for (int p : new int[]{30, 5}) {
            for (int a = 0; a < 8; a++) {
                double yaw = Math.toRadians(a * 45), pitch = Math.toRadians(p);
                c.teleport(x + Math.sin(yaw) * Math.cos(pitch) * r, y + Math.sin(pitch) * r - 1.62,
                        z - Math.cos(yaw) * Math.cos(pitch) * r);
                c.lookAt(x, y, z);
                c.waitTicks(15);
                c.screenshot(String.format("%s-p%d-a%d", label, p + 100, a * 45 + 1000));
            }
        }
    }
}
