import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The first-person crucible blade with 1.12's shader effects (Golem Core {@code ScreenEffects}): translucent blade,
 * bloom composited after the hand, and the heat-distortion shell. A striped wool wall stands behind the blade so the
 * distortion is visible; shots are taken by day and by night (bloom reads best on a dark background), a few frames
 * apart so the animated noise can be compared, plus the spent crucible (no blade, no layers) as the reference.
 */
public final class CrucibleShaders implements Scenario {

    private static final int X = 260;
    private static final int Y = 100;
    private static final int Z = 260;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 8);
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("weather clear");
        c.cmd("gamemode creative");
        String[] wool = {"white", "black", "red", "yellow", "blue", "lime"};
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = 1; dy <= 5; dy++) {
                Fixtures.setBlock(c, X + dx, Y + dy, Z + 3, "minecraft:" + wool[Math.floorMod(dx + dy, wool.length)] + "_wool");
            }
        }
        c.setOption("camera", "first_person");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, 10);

        c.cmd("time set 6000");
        Fixtures.giveOnly(c, c.node().startsWith("1.20") ? "hbm:crucible{\"hbm:crucible_spent\":3}"
                : "hbm:crucible[hbm:crucible_spent=3]", 1);
        c.waitTicks(30);
        c.screenshot("00-day-spent");

        Fixtures.giveOnly(c, "hbm:crucible", 1);
        c.waitTicks(40);
        c.screenshot("01-day-blade-a");
        c.waitTicks(7);
        c.screenshot("02-day-blade-b");
        c.screenshot("03-day-blade-crop", new int[]{280, 60, 480, 400});

        c.cmd("time set 18000");
        c.waitTicks(10);
        c.screenshot("04-night-blade");
        Fixtures.giveOnly(c, c.node().startsWith("1.20") ? "hbm:crucible{\"hbm:crucible_spent\":3}"
                : "hbm:crucible[hbm:crucible_spent=3]", 1);
        c.waitTicks(30);
        c.screenshot("05-night-spent");

        c.cmd("time set 6000");
        c.cmd("clear");
        // positive signal: Core logs the first composite of each layer
        List<String> composited = c.logSinceMark("Screen effect composited after the hand");
        Expect.that(composited.stream().anyMatch(l -> l.contains("bloom")), "bloom composited: " + composited);
        Expect.that(composited.stream().anyMatch(l -> l.contains("distortion")), "distortion composited: " + composited);
        List<String> bad = c.logSinceMark("/(ERROR|FATAL)[]]|Exception|screen_(bloom|distortion|blur|downsample)");
        List<String> relevant = new ArrayList<>();
        for (String line : bad) if (!line.contains("Loaded ") && !line.contains("composited")) relevant.add(line);
        Expect.that(relevant.isEmpty(), "log clean since the mark: " + relevant);
    }
}
