import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Release audit, server tick cost: 40 powered electric furnaces smelting off one cable network, 10 steel barrels on a
 * fluid duct, 5 more furnaces on a second network. Reads the integrated server's average ms per tick in an empty world and again with the machines running.
 */
public final class RcCompatTick implements Scenario {

    private static final int Y = 100;
    private static final int B = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        Fixtures.platform(c, 0, Y, 0, 30);
        c.cmd("fill -28 " + B + " -28 28 " + (B + 6) + " 28 minecraft:air");
        c.waitTicks(300);
        double idle = measure(c);

        Fixtures.setBlock(c, -26, B, 0, "hbm:creative_generator");
        c.cmd("fill -25 " + B + " 0 25 " + B + " 0 hbm:red_cable");
        for (int x = -24; x <= 14; x += 2) {
            for (int z : new int[] {-1, 1}) {
                Fixtures.setBlock(c, x, B, z, "hbm:machine_electric_furnace_off");
                c.cmd("item replace block " + x + " " + B + " " + z + " container.0 with minecraft:iron_ore 64");
            }
        }
        c.cmd("fill -25 " + B + " 10 -5 " + B + " 10 hbm:fluid_duct_neo");
        for (int x = -24; x <= -6; x += 2) {
            Fixtures.setBlock(c, x, B, 11, "hbm:barrel_steel");
            c.cmd("data merge block " + x + " " + B + " 11 {tank:16000,tank_type:\"hbm:oil\"}");
        }
        c.waitTicks(300);
        c.screenshot("01-base");
        double busy = measure(c);
        System.out.println("[RcCompatTick] " + c.node() + " idle " + idle + " ms, 50 machines + 2 networks " + busy + " ms per tick");
        Expect.that(busy < 15.0, "average tick with 50 working machines stays well under 50 ms: " + busy);
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(hbm|golem)");
    }

    private static double measure(Client c) {
        double sum = 0;
        for (int i = 0; i < 3; i++) {
            c.waitTicks(100);
            sum += c.status().get("mspt").getAsDouble();
        }
        return sum / 3;
    }
}
