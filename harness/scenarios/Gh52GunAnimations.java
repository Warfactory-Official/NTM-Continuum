import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issues 52, 53 and 58: the grenade machinegun's belt through a reload and an inspect, the Liberator's
 * reload and inspect, and the B92 held in 3D with its sprite in the inventory. Shots only; compare by eye with
 * 1.7's ItemRenderMK108 belt, the Liberator's break-open keyframes and ItemRenderGunAnim.
 */
public final class Gh52GunAnimations implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, 100, 0, 8);
        c.cmd("time set 6000");
        c.cmd("weather clear");
        c.cmd("gamemode survival");
        c.teleport(0.5, 101, 0.5);
        c.look(0, -10);
        try {
            mk108(c);
            liberator(c);
            b92(c);
        } finally {
            c.releaseAll();
            c.cmd("gamemode creative");
        }
    }

    private static void mk108(Client c) {
        equip(c, "gun_mk108", "ammo_standard_g40_he 64");
        c.screenshot("mk108-idle");
        c.key("r");
        int[] at = {12, 30, 50, 75, 100, 125};
        int waited = 0;
        for (int tick : at) {
            c.waitTicks(tick - waited);
            waited = tick;
            c.screenshot("mk108-reload-" + tick);
        }
        c.waitTicks(160);
        c.screenshot("mk108-loaded");
        c.key("r");
        c.waitTicks(9);
        c.screenshot("mk108-inspect-9");
        c.waitTicks(40);
        // a part-empty drum through a reload, then the third-person belt
        c.attack(25);
        c.waitTicks(30);
        c.screenshot("mk108-partial");
        c.key("r");
        c.waitTicks(6);
        c.screenshot("mk108-partial-reload-6");
        c.waitTicks(34);
        c.screenshot("mk108-partial-reload-40");
        c.waitTicks(150);
        c.key("f5");
        c.key("f5");
        c.waitTicks(10);
        c.screenshot("mk108-third-person");
        c.key("f5");
        c.waitTicks(5);
        c.look(0, -10);
    }

    private static void liberator(Client c) {
        equip(c, "gun_liberator", "ammo_standard_g12 64");
        c.screenshot("liberator-idle");
        c.key("r");
        c.waitTicks(8);
        c.screenshot("liberator-reload-8");
        c.waitTicks(10);
        c.screenshot("liberator-reload-18");
        c.waitTicks(100);
        c.key("r");
        c.waitTicks(12);
        c.screenshot("liberator-inspect-12");
        c.waitTicks(40);
    }

    private static void b92(Client c) {
        equip(c, "gun_b92", null);
        c.screenshot("b92-held");
        c.key("e");
        c.waitTicks(10);
        c.screenshot("b92-inventory");
        c.key("escape");
        c.waitTicks(5);
    }

    private static void equip(Client c, String gun, String ammo) {
        c.cmd("clear @s");
        c.cmd("give @s hbm:" + gun);
        if (ammo != null) c.cmd("give @s hbm:" + ammo);
        c.hotbar(1);
        c.waitTicks(60);
    }
}
