import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Player report: multiblock machines (turbofan, ZIRNOX, derrick) are invisible on a Create Aeronautics ship while
 * single blocks draw. Places them by hand on an iron deck, assembles the deck with {@code /sable assemble} and screenshots
 * before and after, then held in the air and turned, for comparison. Needs Sable in the run ({@code ORG_GRADLE_PROJECT_sable.run=true}, 1.21.1 nodes).
 */
public final class SableShipMachines implements Scenario {

    private static final int Y = 100, D = Y + 1;
    private static final String[] MACHINES = {"hbm:machine_turbofan", "hbm:machine_zirnox", "hbm:machine_well"};

    @Override
    public void run(Client c) throws Exception {
        c.key("escape");
        c.waitTicks(2);
        c.cmd("sable remove @e");
        c.waitTicks(5);
        Fixtures.platform(c, 0, Y, 0, 16);
        c.cmd("gamemode creative");
        c.cmd("fill -12 " + D + " -4 12 " + (D + 12) + " 16 minecraft:air");
        c.cmd("fill -10 " + D + " 4 10 " + D + " 14 minecraft:iron_block");
        for (int i = 0; i < MACHINES.length; i++) {
            int x = -6 + i * 6;
            Fixtures.giveOnly(c, MACHINES[i], 1);
            c.teleport(x + 0.5, D + 1, 5.5);
            c.waitTicks(3);
            c.lookAt(x + 0.5, D + 0.98, 8.5);
            c.use(2);
            c.waitTicks(10);
        }
        c.cmd("clear");
        c.teleport(0.5, D + 8, -8.5);
        c.lookAt(0.5, D + 2, 9.5);
        c.waitTicks(40);
        c.screenshot("10-before");

        c.cmd("sable assemble area -10 " + D + " 4 10 " + (D + 12) + " 14");
        c.waitTicks(60);
        c.teleport(0.5, D + 8, -8.5);
        c.lookAt(0.5, D + 2, 9.5);
        c.waitTicks(40);
        c.screenshot("20-on-ship");

        // held in the air and turned, then turned again every few ticks, filmed from the side
        c.cmd("sable paused true");
        c.teleport(0.5, D + 6, -10.5);
        for (int i = 0; i < 20; i++) {
            int angle = (i / 4) * 15;
            c.cmd("sable teleport @e 0.5 " + (D + 4) + " 9.5 " + angle + " 0");
            c.lookAt(0.5, D + 5, 9.5);
            c.waitTicks(1);
            c.screenshot(String.format("3%02d-turned-%d", i, angle));
        }
        c.cmd("sable paused false");
        c.query("gem", new JsonObject());
        System.out.println("[SableShip] gem " + c.query("gem", new JsonObject()));
    }
}
