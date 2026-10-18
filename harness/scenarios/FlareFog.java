import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * 1.7 {@code LegoClient.renderFlare} draws with fog off. A flare round and a Lambda-98 flare are photographed in clear
 * air and under blindness, whose fog ends 5 blocks out. Fails only on an exception: read the pictures.
 */
public final class FlareFog implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        String hideGui = null;
        try {
            Fixtures.platform(c, 0, Y, 0, 12);
            c.cmd("fill -12 " + (Y + 1) + " -12 12 " + (Y + 30) + " 40 minecraft:air");
            c.cmd("difficulty peaceful");
            c.cmd("time set 18000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            c.cmd("kill @e[type=hbm:entity_lambda_rocket]");
            hideGui = c.setOption("hide_gui", "true");

            for (boolean fog : new boolean[] {false, true}) {
                String tag = fog ? "fog" : "clear";
                c.cmd("effect clear @s");
                if (fog) c.cmd("effect give @s minecraft:blindness 120 0 true");
                c.waitTicks(40);

                loadFlareGun(c);
                c.teleport(0.5, Y + 1, 0.5);
                c.look(0, -4);
                c.waitTicks(10);
                c.attack();
                c.waitTicks(5);
                c.screenshot("round-" + tag + "-a");
                c.waitTicks(4);
                c.screenshot("round-" + tag + "-b");
                c.cmd("kill @e[type=hbm:entity_bullet_mk4]");

                c.cmd("clear @s");
                c.cmd("summon hbm:entity_lambda_rocket 0.5 " + (Y + 1) + " 14.5");
                c.lookAt(0.5, Y + 2, 14.5);
                c.waitTicks(20);
                c.screenshot("rocket-" + tag);
                c.cmd("kill @e[type=hbm:entity_lambda_rocket]");
                c.waitTicks(5);
            }
            Expect.logCleanSinceMark(c, "Exception|/(ERROR|FATAL)[]].*hbm");
        } finally {
            c.cmd("effect clear @s");
            c.cmd("kill @e[type=hbm:entity_lambda_rocket]");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
        }
    }

    private static void loadFlareGun(Client c) {
        c.cmd("clear @s");
        c.cmd("give @s hbm:gun_flaregun");
        c.cmd("give @s hbm:ammo_standard_g26_flare 4");
        c.hotbar(1);
        c.waitTicks(30);
        c.key("r", 2);
        c.waitTicks(80);
    }
}
