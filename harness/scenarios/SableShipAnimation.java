import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Animated block textures on a Sable sub-level under Sodium. Sodium's "animate only visible textures" advances an
 * animated sprite only when a renderer marks it drawn that frame; Sable draws sub-levels from its own meshes and does
 * not, so a ship's conveyor belts froze unless the same sprite was also drawn elsewhere (a held conveyor, a belt in the
 * world). The scenario takes screenshot pairs a few ticks apart, empty-handed and then holding a conveyor; compare the
 * belt region of each pair (identical = frozen). Golem Core's Sable root marks the sprites of visible sub-levels.
 *
 * <p>1.21.1 NeoForge with Sable and Sodium: {@code ${env:ORG_GRADLE_PROJECT_sable.run}="true"} and {@code up --rebuild}.
 */
public class SableShipAnimation implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    @Override
    public void run(Client c) throws Exception {
        c.key("escape");
        c.waitTicks(2);
        c.cmd("sable paused false");
        c.cmd("sable remove @e");
        c.waitTicks(5);
        // marked after the cleanup: removing an earlier run's ship makes Sable's client log an ERROR for movement
        // packets still in flight for it
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 10);
        // nothing animated in the world grid: the only belts are the ship's
        c.cmd("fill " + (X - 3) + " " + (Y + 1) + " " + (Z + 3) + " " + (X + 3) + " " + (Y + 1) + " " + (Z + 7) + " minecraft:iron_block");
        c.cmd("fill " + (X - 3) + " " + (Y + 2) + " " + (Z + 3) + " " + (X + 3) + " " + (Y + 2) + " " + (Z + 7)
                + " hbm:conveyor[facing=east]");
        c.waitTicks(4);
        c.cmd("sable assemble area " + (X - 3) + " " + (Y + 1) + " " + (Z + 3) + " " + (X + 3) + " " + (Y + 2) + " " + (Z + 7));
        c.waitTicks(20);
        Expect.equal("minecraft:air", c.block(X, Y + 2, Z + 5).get("block").getAsString(), "the belts are on the ship");

        c.cmd("fill " + X + " " + (Y + 1) + " " + (Z - 2) + " " + X + " " + (Y + 5) + " " + (Z - 2) + " minecraft:stone");
        c.teleport(X + 0.5, Y + 6, Z - 1.5);
        c.waitTicks(3);
        c.lookAt(X + 0.5, Y + 2.5, Z + 5.5);
        c.cmd("clear");
        c.waitTicks(20);

        c.screenshot("10-empty-hand-a");
        c.waitTicks(3);
        c.screenshot("11-empty-hand-b");

        Fixtures.giveOnly(c, "hbm:conveyor", 1);
        c.waitTicks(10);
        c.screenshot("20-holding-belt-a");
        c.waitTicks(3);
        c.screenshot("21-holding-belt-b");

        c.cmd("clear");
        c.cmd("fill " + X + " " + (Y + 1) + " " + (Z - 2) + " " + X + " " + (Y + 5) + " " + (Z - 2) + " minecraft:air");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }
}
