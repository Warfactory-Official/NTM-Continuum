import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Foundry molds beside a crucible: empty, with an installed mold, with a cast output, and part filled with copper.
 * The melt surface must cover the mold item beneath it, as 1.7 drew it.
 */
public final class FoundryMoldMelt implements Scenario {

    private static final int X = 640, Y = 100, Z = 640, L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("forceload add " + (X - 16) + " " + (Z - 16) + " " + (X + 16) + " " + (Z + 16));
        c.waitTicks(40);
        c.cmd("gamemode creative");
        c.cmd("fill " + (X - 8) + " " + Y + " " + (Z - 8) + " " + (X + 8) + " " + Y + " " + (Z + 8) + " minecraft:sand");
        c.cmd("fill " + (X - 8) + " " + L + " " + (Z - 8) + " " + (X + 8) + " " + (L + 6) + " " + (Z + 8) + " minecraft:air");
        c.cmd("time set 6000");
        c.cmd("gamerule doDaylightCycle false");
        Fixtures.setBlock(c, X, L, Z - 2, "hbm:machine_crucible[facing=south]");
        Fixtures.setBlock(c, X - 3, L, Z + 1, "hbm:foundry_mold");
        Fixtures.setBlock(c, X, L, Z + 1, "hbm:foundry_mold");
        Fixtures.setBlock(c, X + 3, L, Z + 1, "hbm:foundry_mold");
        c.waitTicks(20);
        c.cmd("item replace block " + X + " " + L + " " + (Z + 1) + " container.0 with hbm:mold_ingot");
        c.cmd("item replace block " + (X + 3) + " " + L + " " + (Z + 1) + " container.0 with hbm:mold_plate");
        c.cmd("item replace block " + (X + 3) + " " + L + " " + (Z + 1) + " container.1 with minecraft:iron_ingot");
        c.cmd("item replace block " + (X - 3) + " " + L + " " + (Z + 1) + " container.0 with hbm:mold_plate");
        c.waitTicks(10);
        c.cmd("data merge block " + (X - 3) + " " + L + " " + (Z + 1) + " {type:\"copper\",amount:40}");
        c.waitTicks(30);
        System.out.println("[FoundryMoldMelt] melt mold: " + c.block(X - 3, L, Z + 1));
        c.cmd("gamemode spectator");
        c.teleport(X + 2.5, L + 2.2, Z + 4.5);
        c.waitTicks(2);
        c.lookAt(X + 0.5, L + 0.2, Z + 1.5);
        c.waitTicks(20);
        c.screenshot("mold-angle");
        c.teleport(X + 0.5, L + 3.5, Z + 1.5);
        c.waitTicks(2);
        c.lookAt(X + 0.5, L, Z + 1.5001);
        c.waitTicks(20);
        c.screenshot("mold-above");
        c.teleport(X + 3.5, L + 3.5, Z + 1.5);
        c.waitTicks(2);
        c.lookAt(X + 3.5, L, Z + 1.5001);
        c.waitTicks(20);
        c.screenshot("mold-output-above");
        c.teleport(X - 1.5, L + 1.6, Z + 3.5);
        c.waitTicks(2);
        c.lookAt(X - 2.5, L + 0.3, Z + 1.5);
        c.waitTicks(20);
        c.screenshot("melt-angle");
        c.teleport(X - 2.5, L + 3.5, Z + 1.5);
        c.waitTicks(2);
        c.lookAt(X - 2.5, L, Z + 1.5001);
        c.waitTicks(20);
        c.screenshot("melt-above");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointer|ClassCast|IllegalState");
    }
}
