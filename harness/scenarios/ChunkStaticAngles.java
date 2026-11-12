import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * Each static OBJ chunk model floating in the air, shot close up from eight compass angles at three heights (above,
 * level, below), with the GUI hidden. The shots are for the eye: no face may be missing or see-through from any side.
 * One facing per kind is enough, the other facings are the same mesh turned.
 */
public class ChunkStaticAngles implements Scenario {

    private static final int Y = 100, UP = Y + 6, GAP = 12;
    private static final double R = 2.6;

    private static final List<String> BLOCKS = List.of(
            "steel_scaffold[orient=horizontal_north_south]", "steel_scaffold[orient=vertical_east_west]",
            "steel_wall[facing=north]", "steel_corner[facing=north]", "steel_roof[facing=north]",
            "steel_poles[facing=north]", "tape_recorder[facing=north]", "deco_toaster_iron[facing=north]",
            "deco_computer_ibm_300pl[facing=north]", "pole_top", "barbed_wire[facing=north]", "spikes", "red_barrel",
            "crate_can", "hev_battery", "anvil_iron[facing=north]", "det_cord");

    @Override
    public void run(Client c) throws Exception {
        c.key("escape");
        c.waitTicks(2);
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("time set 6000");
        c.cmd("gamerule doDaylightCycle false");
        Fixtures.platform(c, 0, Y, BLOCKS.size() * GAP / 2, BLOCKS.size() * GAP / 2 + 8);
        c.cmd("fill -6 " + (Y + 1) + " -6 6 " + (UP + 4) + " " + (BLOCKS.size() * GAP + 12) + " minecraft:air");
        for (int k = 0; k < BLOCKS.size(); k++) Fixtures.setBlock(c, 0, UP, k * GAP, "hbm:" + BLOCKS.get(k));
        int lz = BLOCKS.size() * GAP;
        c.cmd("fill 0 " + (UP + 1) + " " + lz + " 2 " + (UP + 1) + " " + lz + " minecraft:stone");
        for (int x = 0; x <= 2; x++) Fixtures.setBlock(c, x, UP, lz, "hbm:spotlight_fluoro[facing=down]");
        c.cmd("setblock 0 " + UP + " " + (lz + 1) + " minecraft:stone");
        Fixtures.setBlock(c, 0, UP, lz + 2, "hbm:spotlight_incandescent[facing=south]");
        c.cmd("clear");
        c.key("f1");
        c.cmd("gamemode spectator");
        c.waitTicks(60);
        try {
            for (int k = 0; k < BLOCKS.size(); k++) {
                orbit(c, 0.5, UP + 0.5, k * GAP + 0.5, String.format("%02d-%s", k, BLOCKS.get(k).replaceAll("\\[.*", "")));
            }
            orbit(c, 1.5, UP + 0.5, lz + 0.5, "17-fluoro_run");
            orbit(c, 0.5, UP + 0.5, lz + 2.5, "18-cage_lamp_wall");
        } finally {
            c.cmd("gamemode creative");
            c.key("f1");
        }
    }

    private static void orbit(Client c, double x, double y, double z, String label) throws Exception {
        int[] pitches = {35, 0, -35};
        for (int p : pitches) {
            for (int a = 0; a < 8; a++) {
                double yaw = Math.toRadians(a * 45), pitch = Math.toRadians(p);
                c.teleport(x + Math.sin(yaw) * Math.cos(pitch) * R, y + Math.sin(pitch) * R - 1.62,
                        z - Math.cos(yaw) * Math.cos(pitch) * R);
                c.lookAt(x, y, z);
                c.waitTicks(3);
                c.screenshot(label + "-p" + (p + 100) + "-a" + (a * 45 + 1000));
            }
        }
    }
}
