import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A turbofan placed by hand on an iron deck, fuelled with kerosene and assembled into a Sable ship with
 * {@code /sable assemble}: the burning fan pushes the ship across the platform. Positions come from {@code /sable info}.
 * Needs Sable in the run ({@code ORG_GRADLE_PROJECT_sable.run=true}, 1.21.1 nodes).
 */
public final class SableTurbofanThrust implements Scenario {

    private static final int Y = 100, D = Y + 1;
    private static final Pattern POSITION = Pattern.compile("Position: (-?[0-9.]+) (-?[0-9.]+) (-?[0-9.]+)");

    @Override
    public void run(Client c) throws Exception {
        c.key("escape");
        c.waitTicks(2);
        c.resetWorld();
        Fixtures.platform(c, 0, Y, 0, 24);
        c.cmd("fill -5 " + D + " 3 5 " + D + " 13 minecraft:iron_block");
        Fixtures.giveOnly(c, "hbm:machine_turbofan", 1);
        c.teleport(0.5, D + 1, 5.5);
        c.waitTicks(3);
        c.lookAt(0.5, D + 0.98, 8.5);
        c.use(2);
        c.waitTicks(10);
        c.cmd("clear");

        int[] core = findCore(c);
        Expect.that(core != null, "the turbofan was placed on the deck");
        c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {tank:24000,tank_type:\"hbm:kerosene\"}");
        c.waitTicks(5);

        c.cmd("sable assemble area -5 " + D + " 3 5 " + (D + 4) + " 13");
        c.waitTicks(10);
        c.teleport(0.5, D + 10, -12.5);
        c.lookAt(0.5, D + 1, 8.5);
        c.waitTicks(5);
        c.screenshot("10-assembled");
        double[] start = position(c);

        c.waitTicks(80);
        double[] end = position(c);
        c.lookAt(end[0], end[1], end[2]);
        c.waitTicks(5);
        c.screenshot("20-after-thrust");
        double moved = Math.hypot(end[0] - start[0], end[2] - start[2]);
        System.out.printf("[SableTurbofanThrust] ship moved %.2f blocks (%s -> %s)%n", moved, str(start), str(end));
        Expect.that(moved > 2.0, "the burning turbofan pushed its ship " + moved + " blocks");
        c.cmd("sable remove @e");
    }

    /** The turbofan's core: the one block entity on the deck row that saves a {@code tank_type}. */
    private static int[] findCore(Client c) throws Exception {
        for (int x = -5; x <= 5; x++) {
            for (int z = 3; z <= 13; z++) {
                JsonObject be = c.blockEntity(x, D + 1, z);
                if (be.toString().contains("tank_type")) return new int[]{x, D + 1, z};
            }
        }
        return null;
    }

    /** The ship's position from {@code /sable info}, the newest line of its chat output. */
    private static double[] position(Client c) throws Exception {
        c.markLog();
        c.cmd("sable info @e");
        c.waitTicks(5);
        List<String> lines = c.logSinceMark("Position:");
        Expect.that(!lines.isEmpty(), "/sable info reported a position");
        Matcher m = POSITION.matcher(lines.get(lines.size() - 1));
        Expect.that(m.find(), "position line parses: " + lines.get(lines.size() - 1));
        return new double[]{Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3))};
    }

    private static String str(double[] p) {
        return String.format("%.2f %.2f %.2f", p[0], p[1], p[2]);
    }
}
