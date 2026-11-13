import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 177: tar sand draws 1.7's {@code ore_oil_sand_alt}, a sand texture a shade darker than vanilla sand,
 * not the grey {@code ore_oil_sand}. A patch of tar sand inside vanilla sand, seen from above, and one held.
 */
public final class Gh177TarSand implements Scenario {

    private static final int X = 1930;
    private static final int Y = 100;
    private static final int Z = 1930;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 10);
        c.cmd("time set 6000");
        c.cmd("fill " + (X - 6) + " " + (Y - 1) + " " + (Z - 6) + " " + (X + 6) + " " + (Y - 1) + " " + (Z + 6) + " minecraft:stone");
        c.cmd("fill " + (X - 6) + " " + Y + " " + (Z - 6) + " " + (X + 6) + " " + Y + " " + (Z + 6) + " minecraft:sand");
        c.cmd("fill " + (X - 2) + " " + Y + " " + (Z - 2) + " " + (X + 2) + " " + Y + " " + (Z + 2) + " hbm:ore_oil_sand");
        c.cmd("fill " + (X - 6) + " " + (Y + 1) + " " + (Z - 6) + " " + (X + 6) + " " + (Y + 6) + " " + (Z + 6) + " minecraft:air");
        Fixtures.giveOnly(c, "hbm:ore_oil_sand", 1);
        c.hotbar(1);
        c.teleport(X + 0.5, Y + 4, Z - 5.5);
        c.lookAt(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(20);
        c.screenshot("tar-sand");
        String placed = c.block(X, Y, Z).toString();
        Expect.that(placed.contains("hbm:ore_oil_sand"), "tar sand placed: " + placed);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Missing texture|missing.*ore_oil_sand");
    }
}
