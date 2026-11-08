import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 151: leaving a world with a Sable ship of HBM blocks aborted the game while Sable saved the overworld's
 * sub-levels (a Rapier panic, "no entry found for key"). Builds a deck with machines, cables, ducts and a turbofan,
 * assembles it and lifts it, then leaves and rejoins the world three times, once while the
 * ship's chunk is unloaded. The client must survive
 * each save and find the ships again. Needs Sable ({@code ORG_GRADLE_PROJECT_sable.run=true}, 1.21.1 nodes).
 */
public final class Gh151SableQuit implements Scenario {

    private static final int Y = 100, D = Y + 1;
    private static final String[] MACHINES = {"hbm:machine_turbofan", "hbm:machine_tower_small", "hbm:machine_assembly_machine",
            "hbm:machine_chemical_plant", "hbm:machine_press"};

    @Override
    public void run(Client c) throws Exception {
        c.key("escape");
        c.waitTicks(2);
        c.resetWorld();
        c.cmd("sable remove @e");
        Fixtures.platform(c, 0, Y, 0, 24);
        c.cmd("gamemode creative");
        c.cmd("fill -12 " + D + " 2 12 " + D + " 18 minecraft:iron_block");
        for (int i = 0; i < MACHINES.length; i++) {
            int x = -9 + i * 5;
            Fixtures.giveOnly(c, MACHINES[i], 1);
            c.teleport(x + 0.5, D + 1, 3.5);
            c.waitTicks(3);
            c.lookAt(x + 0.5, D + 0.98, 7.5);
            c.use(2);
            c.waitTicks(10);
        }
        c.cmd("fill -12 " + (D + 1) + " 16 12 " + (D + 1) + " 16 hbm:red_cable");
        c.cmd("fill -12 " + (D + 1) + " 17 12 " + (D + 1) + " 17 hbm:fluid_duct_neo");
        c.cmd("fill -12 " + (D + 1) + " 18 12 " + (D + 1) + " 18 hbm:concrete_smooth");
        c.cmd("clear");
        c.waitTicks(20);

        c.cmd("sable assemble area -12 " + D + " 2 12 " + (D + 20) + " 18");
        c.waitTicks(40);
        c.teleport(0.5, D + 12, -14.5);
        c.lookAt(0.5, D + 2, 10.5);
        c.waitTicks(20);
        c.screenshot("10-ship");
        c.cmd("sable teleport @e 0.5 " + (D + 30) + " 10.5 15 0");
        c.waitTicks(40);
        c.screenshot("20-flying");

        for (int round = 1; round <= 3; round++) {
            String world = c.currentWorld();
            // round 2 leaves while the ship sits in Sable's holding chunks, round 3 right after it loaded again
            if (round == 2) {
                c.cmd("forceload remove all");
                c.teleport(600.5, D + 30, 600.5);
                c.waitTicks(200);
            }
            c.markLog();
            c.leaveWorld(120_000);
            Expect.that(c.ping() != null, "the client survived leaving the world, round " + round);
            c.joinWorld(world, 180_000);
            c.waitTicks(60);
            if (round == 2) {
                c.teleport(0.5, D + 12, -14.5);
                c.waitTicks(100);
            }
            c.markLog();
            c.cmd("sable info @e");
            c.waitTicks(5);
            Expect.that(!c.logSinceMark("Position:").isEmpty(), "the ship came back, round " + round);
            c.screenshot(String.format("3%d-rejoined", round));
        }
        System.out.println("[Gh151] " + c.query("gem", new JsonObject()));
        c.cmd("sable remove @e");
    }
}
