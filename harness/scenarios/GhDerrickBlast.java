import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1.7 {@code MachineOilWell.onBlockExploded}: a derrick with oil or gas caught in an explosion empties both tanks into a
 * strength 15 blast at its core with the small large-explosion effect (plume, shock ring, flying ground). Two derricks
 * are hit by the same TNT, one full and one empty; the ground crater is counted by refilling it.
 */
public final class GhDerrickBlast implements Scenario {

    private static final int X = 1800;
    private static final int Y = 100;
    private static final int Z = 1800;
    private static final int APART = 40;
    private static final Pattern FILLED = Pattern.compile("filled (\\d+)");

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X + APART / 2, Y, Z, APART / 2 + 16);
        Fixtures.fill(c, X - 16, Y - 3, Z - 16, X + APART + 16, Y - 1, Z + 16, "minecraft:stone");
        c.cmd("time set 6000");
        c.cmd("gamemode creative");
        c.setOption("camera", "first_person");
        int full = shot(c, X, true);
        int empty = shot(c, X + APART, false);
        System.out.println("[GhDerrickBlast] " + c.node() + " crater blocks: full derrick " + full + ", empty derrick " + empty);
        Expect.that(empty < 60, "TNT alone on an empty derrick makes only its own small crater: " + empty);
        Expect.that(full > 3 * Math.max(empty, 20), "the full derrick's strength 15 blast dwarfs the TNT's crater: "
                + full + " against " + empty);
        Expect.logCleanSinceMark(c, "hbm.*Exception|Exception.*hbm|/(ERROR|FATAL)[]].*hbm");
    }

    private static int shot(Client c, int x, boolean full) throws Exception {
        String tag = full ? "full" : "empty";
        Fixtures.setBlock(c, x, Y + 1, Z, "hbm:machine_well[facing=north]");
        if (full) c.cmd("data merge block " + x + " " + (Y + 1) + " " + Z + " {t0:64000,t1:64000}");
        c.teleport(x + 0.5, Y + 1, Z - 24.5);
        c.waitTicks(10);
        c.lookAt(x + 0.5, Y + 4, Z + 0.5);
        c.waitTicks(5);
        c.screenshot(tag + "-before");
        c.cmd("summon minecraft:tnt " + (x + 0.5) + " " + (Y + 1) + " " + (Z + 0.5) + " {fuse:1,Fuse:1}");
        c.waitTicks(4);
        c.screenshot(tag + "-blast");
        c.waitTicks(8);
        c.screenshot(tag + "-plume");
        c.waitTicks(40);
        c.screenshot(tag + "-after");
        c.markLog();
        c.cmd("fill " + (x - 12) + " " + (Y - 3) + " " + (Z - 12) + " " + (x + 12) + " " + Y + " " + (Z + 12)
                + " minecraft:glass replace minecraft:air");
        c.waitTicks(5);
        List<String> lines = c.logSinceMark("filled");
        for (String line : lines) {
            Matcher m = FILLED.matcher(line);
            if (m.find()) return Integer.parseInt(m.group(1));
        }
        return 0;
    }
}
