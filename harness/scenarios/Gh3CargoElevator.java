import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub issue 3: a sneak right-click on a placed cargo elevator with another elevator in hand. The scenario places
 * an elevator, sneak-clicks its top with a second one, then counts elevator cores and cells in the column for a
 * while to catch a stack that keeps growing. A plain click with an elevator must still add exactly one course (1.7
 * {@code onBlockActivated}).
 */
public final class Gh3CargoElevator implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 12);
        c.cmd("fill -6 " + G + " -6 6 " + (Y + 40) + " 6 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.markLog();

        // the first elevator, placed by hand from the south
        Fixtures.giveOnly(c, "hbm:cargo_elevator", 8);
        c.teleport(0.5, G, -2.5);
        c.waitTicks(3);
        c.lookAt(0.5, Y + 0.98, 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(10);
        int[] core = findCore(c, G);
        Expect.that(core != null, "the first elevator formed a core on y=" + G);
        System.out.println("[Gh3] first core " + core[0] + " " + core[1] + " " + core[2]);
        int before = countColumn(c, core);
        System.out.println("[Gh3] elevator blocks after placing: " + before);
        c.screenshot("01-placed");

        // the sneak-click on its top with another elevator
        // standing on the platform south of the lift, aimed down at the lift's top face (y = G + 1)
        c.cmd("setblock " + core[0] + " " + (G + 1) + " " + (core[2] - 3) + " minecraft:stone");
        c.teleport(core[0] + 0.5, G + 2, core[2] - 2.5);
        c.waitTicks(3);
        c.lookAt(core[0] + 0.5, G + 1.0, core[2] - 0.3);
        c.waitTicks(1);
        c.keyDown("key.sneak");
        c.waitTicks(3);
        c.use(1);
        c.waitTicks(3);
        c.keyUp("key.sneak");
        int[] counts = new int[6];
        int[] heights = new int[6];
        for (int i = 0; i < counts.length; i++) {
            c.waitTicks(20);
            counts[i] = countColumn(c, core);
            heights[i] = height(c, core);
        }
        System.out.println("[Gh3] lower core height each second after the sneak-click: " + Arrays.toString(heights));
        System.out.println("[Gh3] elevator blocks each second after the sneak-click: " + Arrays.toString(counts));
        c.screenshot("02-after-sneak-click");
        // 1.7 TileEntityCargoElevator: an elevator placed on top joins the one below once, which then counts two courses
        Expect.that(counts[0] == 2 * before, "the sneak-click placed one more elevator on top: " + counts[0]);
        for (int h : heights) Expect.that(h == 1, "the joined lift is two courses (height 1): " + Arrays.toString(heights));
        Expect.that(!c.blockEntity(core[0], G + 1, core[2]).toString().contains("TileEntityCargoElevator"),
                "the upper elevator's core became a plain cell: " + c.blockEntity(core[0], G + 1, core[2]));
        Expect.that(counts[counts.length - 1] == counts[0], "the column stops growing after one sneak-click: "
                + Arrays.toString(counts));
        JsonObject inv = c.inventory();
        System.out.println("[Gh3] inventory after: " + inv);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static int height(Client c, int[] core) {
        Matcher m = Pattern.compile("height: ?(\\d+)").matcher(c.blockEntity(core[0], core[1], core[2]).toString());
        Expect.that(m.find(), "the lower core has a height: " + c.blockEntity(core[0], core[1], core[2]));
        return Integer.parseInt(m.group(1));
    }

    /** Counts cargo-elevator blocks at the core column and its four neighbours, up to 30 courses. */
    private static int countColumn(Client c, int[] core) {
        int n = 0;
        int[][] cols = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int dy = 0; dy < 30; dy++) {
            for (int[] col : cols) {
                if (c.block(core[0] + col[0], core[1] + dy, core[2] + col[1]).toString().contains("cargo_elevator")) n++;
            }
        }
        return n;
    }

    private static int[] findCore(Client c, int y) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 3; dz++) {
            JsonObject be = c.blockEntity(dx, y, dz);
            if (be.has("block_entity") && !be.get("block_entity").isJsonNull() && be.toString().contains("cargo_elevator")) {
                return new int[]{dx, y, dz};
            }
        }
        return null;
    }
}
