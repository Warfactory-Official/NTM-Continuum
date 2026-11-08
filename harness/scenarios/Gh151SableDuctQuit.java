import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 151, minimal: a Sable ship with oil barrels and oil ducts touching them. Leaving the world unloads the
 * ship after Sable has dropped its physics body; a barrel's port used to rewrite the ducts' arms from setRemoved, and
 * Sable's block-change hook then asked Rapier for the missing body and aborted the game. Leaves and rejoins three times.
 * Needs Sable ({@code ORG_GRADLE_PROJECT_sable.run=true}, 1.21.1 nodes).
 */
public final class Gh151SableDuctQuit implements Scenario {

    private static final int Y = 100, D = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.key("escape");
        c.waitTicks(2);
        c.resetWorld();
        c.cmd("sable remove @e");
        Fixtures.platform(c, 0, Y, 0, 16);
        c.cmd("gamemode creative");
        c.cmd("fill -6 " + D + " 2 6 " + D + " 10 minecraft:iron_block");
        for (int x = -4; x <= 4; x += 4) {
            c.cmd("setblock " + x + " " + (D + 1) + " 6 hbm:barrel_steel{tank_type:\"hbm:oil\",tank:8000}");
            c.cmd("setblock " + (x + 1) + " " + (D + 1) + " 6 hbm:fluid_duct_neo{type:\"hbm:oil\"}");
            c.cmd("setblock " + x + " " + (D + 1) + " 5 hbm:fluid_duct_neo{type:\"hbm:oil\"}");
        }
        c.teleport(0.5, D + 6, -6.5);
        c.lookAt(0.5, D + 1, 6.5);
        c.waitTicks(40);
        c.screenshot("10-deck");
        c.cmd("sable assemble area -6 " + D + " 2 6 " + (D + 4) + " 10");
        c.waitTicks(60);
        c.screenshot("20-ship");

        for (int round = 1; round <= 3; round++) {
            String world = c.currentWorld();
            c.markLog();
            c.leaveWorld(120_000);
            Expect.that(c.ping() != null, "the client survived leaving the world, round " + round);
            System.out.println("[Gh151] " + c.node() + " quit " + round + " survived");
            c.joinWorld(world, 180_000);
            c.waitTicks(60);
            c.markLog();
            c.cmd("sable info @e");
            c.waitTicks(5);
            Expect.that(!c.logSinceMark("Position:").isEmpty(), "the ship came back, round " + round);
        }
        c.screenshot("30-after");
        c.cmd("sable remove @e");
    }
}
