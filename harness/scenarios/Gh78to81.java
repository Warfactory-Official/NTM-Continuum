import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issues 78, 79 and 81: the blinking CRT placed at all four facings (two shots half a second apart) and held,
 * the conveyor splitter held and in the inventory, and a held gun at several view angles without a shader pack.
 */
public final class Gh78to81 implements Scenario {

    private static final int Y = 100;
    private static final String[] FACINGS = {"north", "east", "south", "west"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("gamemode creative");
        c.cmd("time set noon");
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("clear");
        Fixtures.platform(c, 0, Y, 0, 8);
        c.cmd("fill -8 " + (Y + 1) + " -8 8 " + (Y + 10) + " 8 minecraft:air");
        for (int f = 0; f < 4; f++) {
            Fixtures.setBlock(c, -3 + 2 * f, Y + 1, 3, "hbm:deco_crt_blinking[facing=" + FACINGS[f] + "]");
            Fixtures.setBlock(c, -3 + 2 * f, Y + 1, 5, "hbm:deco_crt_clean[facing=" + FACINGS[f] + "]");
        }
        c.teleport(0.5, Y + 1, -1.5);
        c.lookAt(0.5, Y + 1.5, 4);
        c.waitTicks(20);
        c.screenshot("crt-placed-a");
        c.waitTicks(10);
        c.screenshot("crt-placed-b");

        for (String id : new String[] {"crane_splitter", "deco_crt_blinking"}) {
            c.cmd("clear");
            c.cmd("give @s hbm:" + id + " 1");
            c.hotbar(1);
            c.look(180, 10);
            c.waitTicks(6);
            c.screenshot(id + "-first");
            c.key("e");
            c.waitTicks(6);
            c.screenshot(id + "-inventory");
            c.key("escape");
            c.waitTicks(4);
        }

        c.cmd("clear");
        c.cmd("give @s hbm:gun_light_revolver 1");
        c.hotbar(1);
        int[][] views = {{0, 0}, {90, 0}, {180, 30}, {270, -30}};
        for (int[] v : views) {
            c.look(v[0], v[1]);
            c.waitTicks(6);
            c.screenshot("gun-yaw" + v[0] + "-pitch" + v[1]);
        }
        c.cmd("clear");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointer|ClassCast|IllegalState");
    }
}
