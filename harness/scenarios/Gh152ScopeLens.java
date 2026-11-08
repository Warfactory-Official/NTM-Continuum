import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 152 follow-up: the scope lenses at the hip, for comparison with 1.7. The STG77 (the reporter's gun) has
 * a black lens in its texture, the AMAT a brown one; both models and textures are byte-identical to 1.7's.
 */
public final class Gh152ScopeLens implements Scenario {

    private static final int X = 1580;
    private static final int Y = 100;
    private static final int Z = 1580;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, X, Y, Z, 12);
        c.cmd("time set 6000");
        c.cmd("gamemode survival");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.setOption("camera", "first_person");
        c.raw("{\"cmd\":\"clear_chat\"}");
        for (String gun : new String[] {"gun_stg77", "gun_amat", "gun_g3_zebra", "gun_heavy_revolver_lilmac"}) {
            c.cmd("clear @s");
            c.cmd("give @s hbm:" + gun);
            c.hotbar(1);
            c.look(0, 10);
            c.waitTicks(50);
            c.screenshot(gun + "-hip");
        }
        c.cmd("gamemode creative");
    }
}
