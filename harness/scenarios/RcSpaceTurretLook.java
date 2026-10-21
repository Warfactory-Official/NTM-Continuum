import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Every 1.7 turret in the world, powered and on, with a mob to aim at, photographed from the front and from behind;
 * and the Chekhov at all four facings. Looked at by hand against 1.7: model, texture, aim animation, plugs.
 */
public final class RcSpaceTurretLook implements Scenario {

    private static final int Y = 100, L = Y + 1;
    private static final String[] TURRETS = {"chekhov", "friendly", "jeremy", "tauon", "richard", "howard",
            "howard_damaged", "maxwell", "fritz", "arty", "himars", "sentry", "sentry_damaged"};
    private static final String[] FACING = {"north", "east", "south", "west"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 70);
        c.cmd("fill -64 " + L + " -20 64 " + (Y + 6) + " 20 minecraft:air");
        c.cmd("time set 6000");
        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode spectator");
        c.waitTicks(10);

        for (int i = 0; i < TURRETS.length; i++) {
            int x = -60 + i * 10;
            place(c, TURRETS[i], x, 0, FACING[i % 4]);
        }
        for (int i = 0; i < 4; i++) place(c, "chekhov", -60 + i * 10, 40, FACING[i]);
        c.waitTicks(80);

        for (int i = 0; i < TURRETS.length; i++) {
            int x = -60 + i * 10;
            shot(c, TURRETS[i] + "-front", x + 1.0, L + 3, 7.0, x + 1.0, L + 1.4, 1.0);
            shot(c, TURRETS[i] + "-back", x + 1.0, L + 3, -5.0, x + 1.0, L + 1.4, 1.0);
        }
        for (int i = 0; i < 4; i++) {
            int x = -60 + i * 10;
            shot(c, "chekhov-facing-" + FACING[i], x + 1.0, L + 3, 47.0, x + 1.0, L + 1.4, 41.0);
        }
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointerException|ClassCastException|IllegalStateException|Ticking (entity|block entity)");
        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode creative");
    }

    private static void place(Client c, String turret, int x, int z, String facing) {
        Fixtures.setBlock(c, x, L, z, "hbm:turret_" + turret + (turret.startsWith("sentry") ? "" : "[facing=" + facing + "]"));
        c.cmd("data merge block " + x + " " + L + " " + z + " {power:1000000L,isOn:1b,targetMobs:1b,targetAnimals:1b}");
        // 6 blocks out and 4 to the side so the turret has to swing round
        c.cmd("summon minecraft:iron_golem " + (x + 6.5) + " " + L + " " + (z + 6.5) + " {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
    }

    private static void shot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.teleport(x, y, z);
        c.waitTicks(10);
        c.lookAt(tx, ty, tz);
        c.waitTicks(10);
        c.screenshot(name);
    }
}
