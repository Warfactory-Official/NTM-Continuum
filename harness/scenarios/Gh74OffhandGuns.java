import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/** GitHub issue 74: guns held in the offhand, alone and beside a main-hand gun, at the hip and aimed. */
public final class Gh74OffhandGuns implements Scenario {

    private static final int Y = 100, G = 101;
    private static final String[] GUNS = {"gun_light_revolver", "gun_stg77", "gun_g3", "gun_amat", "gun_spas12",
            "gun_maresleg", "gun_minigun", "gun_fatman", "gun_panzerschreck", "gun_flamer", "gun_laser_pistol",
            "gun_coilgun", "gun_tau", "gun_double_barrel", "gun_liberator", "gun_quadro"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 12);
        c.cmd("fill -12 " + G + " -12 12 " + (Y + 8) + " 12 minecraft:air");
        c.cmd("fill -3 " + G + " 8 3 " + (G + 4) + " 8 minecraft:stone");
        c.cmd("gamemode survival");
        c.teleport(0.5, G, 0.5);
        c.setOption("camera", "first_person");
        c.look(0, 0);
        for (String gun : GUNS) {
            c.cmd("clear @s");
            c.cmd("item replace entity @s weapon.offhand with hbm:" + gun);
            c.hotbar(1);
            c.waitTicks(45);
            c.screenshot(gun + "-offhand");
            c.cmd("give @s hbm:" + gun);
            c.waitTicks(45);
            c.screenshot(gun + "-both");
            c.key("mouse.right");
            c.waitTicks(25);
            c.screenshot(gun + "-both-aimed");
            c.key("mouse.right");
            c.waitTicks(20);
        }
        c.releaseAll();
        c.cmd("clear @s");
        c.cmd("gamemode creative");
    }
}
