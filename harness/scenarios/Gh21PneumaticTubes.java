import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 21: pneumatic tubes drawn with faces missing in the world and in the hand. A straight run, a corner,
 * a junction and a vertical run, seen from four sides and close up with a tube held. The item draws 1.7's inventory box, the straight
 * sprite's band along its length. Each arm must run from the
 * core to the block face, as 1.7's {@code setRenderBounds(upper, ..., 1, ...)}; arms spanning the whole block crossed
 * through each other as thin fins.
 */
public final class Gh21PneumaticTubes implements Scenario {

    private static final int Y = 100, G = 101;
    private static final String[][] SIDES = {{"down", "0", "-1", "0"}, {"up", "0", "1", "0"}, {"north", "0", "0", "-1"},
            {"south", "0", "0", "1"}, {"west", "-1", "0", "0"}, {"east", "1", "0", "0"}};

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 10);
        c.cmd("fill -10 " + G + " -10 10 " + (Y + 8) + " 10 minecraft:air");
        c.cmd("time set 6000");
        c.markLog();

        String tube = "hbm:pneumatic_tube";
        List<int[]> tubes = new ArrayList<>();
        for (int x = -3; x <= 1; x++) tubes.add(new int[]{x, G, 0});
        for (int z = 1; z <= 3; z++) tubes.add(new int[]{1, G, z});
        for (int y = G + 1; y <= G + 2; y++) tubes.add(new int[]{-1, y, 0});
        tubes.add(new int[]{-1, G, -1});
        tubes.add(new int[]{3, G, -2});
        // setblock does not run the placement shape update, so each state names its connections
        for (int[] t : tubes) {
            StringBuilder state = new StringBuilder();
            for (String[] side : SIDES) {
                int[] n = {t[0] + Integer.parseInt(side[1]), t[1] + Integer.parseInt(side[2]), t[2] + Integer.parseInt(side[3])};
                boolean on = tubes.stream().anyMatch(o -> o[0] == n[0] && o[1] == n[1] && o[2] == n[2]);
                state.append(state.length() == 0 ? "" : ",").append(side[0]).append('=').append(on);
            }
            c.cmd("setblock " + t[0] + " " + t[1] + " " + t[2] + " " + tube + "[" + state + "]");
        }
        Fixtures.giveOnly(c, tube, 1);
        c.hotbar(1);
        c.waitTicks(20);

        // with the instancing engine, then the conventional renderer a client without Flywheel uses
        shoot(c, "engine");
        c.cmd("flywheel backend off");
        c.waitTicks(20);
        shoot(c, "fallback");
        c.cmd("flywheel backend DEFAULT");
        // the item: held, dropped on the ground, and in the inventory
        c.teleport(4.5, G, 4.5);
        c.waitTicks(3);
        c.lookAt(4.5, G + 1.0, 8.5);
        c.cmd("summon minecraft:item 4.5 " + G + " 6.5 {Item:{id:\"" + tube + "\",Count:1b,count:1},PickupDelay:32767s,NoGravity:1b}");
        c.waitTicks(10);
        c.screenshot("item-held-dropped");
        c.key("key.inventory");
        c.waitTicks(10);
        c.screenshot("item-inventory");
        c.key("escape");
        c.waitTicks(3);
        c.cmd("kill @e[type=minecraft:item]");
        Expect.that(c.block(-1, G, 0).toString().contains("pneumatic_tube"), "the junction tube is there");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static void shoot(Client c, String label) {
        // four views of the whole layout, then close views of the junction and of the corner, where arms meet
        double[][] eyes = {{-0.5, G + 2, -5.5, -0.5, 0.5}, {5.5, G + 2, 1.5, -0.5, 0.5}, {-0.5, G + 2, 6.5, -0.5, 0.5},
                {-6.5, G + 2, 1.5, -0.5, 0.5}, {0.8, G, -1.8, -0.5, 0.5}, {3.3, G, -1.3, 1.5, 0.5}};
        for (int i = 0; i < eyes.length; i++) {
            c.teleport(eyes[i][0], eyes[i][1], eyes[i][2]);
            c.waitTicks(5);
            c.lookAt(eyes[i][3], G + 0.5, eyes[i][4]);
            c.waitTicks(15);
            c.screenshot(label + "-" + (i + 1));
        }
    }
}
