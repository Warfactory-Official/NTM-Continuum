import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 22: a Balefire Bomb set off below y = 0 left no crater and its mushroom cloud had no stem. 1.7 clamps
 * the crater floor and the cloud's stem source to the world bottom, which was y = 0 there. On a stone slab at y -50..-41
 * the crater must reach the slab and the stem must rise from the ground (screenshots).
 */
public final class Gh22BalefireBelowZero implements Scenario {

    private static final int TOP = -41;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.cmd("gamemode spectator");
        // load the slab's chunks before filling: fill and setblock skip unloaded positions
        c.teleport(0.5, TOP + 20, 0.5);
        c.waitTicks(80);
        c.cmd("kill @e[type=!player]");
        c.cmd("fill -30 -50 -30 30 -46 30 minecraft:stone");
        c.cmd("fill -30 -45 -30 30 " + TOP + " 30 minecraft:stone");
        c.cmd("fill -30 " + (TOP + 1) + " -30 30 " + (TOP + 8) + " 30 minecraft:air");
        c.cmd("setblock 0 " + (TOP + 1) + " 0 hbm:nuke_fstbmb");
        c.waitTicks(5);
        Expect.that(block(c, 6, TOP, 6).contains("minecraft:stone"), "the slab is there: " + block(c, 6, TOP, 6));
        Expect.that(block(c, 0, TOP + 1, 0).contains("nuke_fstbmb"), "the bomb is there: " + block(c, 0, TOP + 1, 0));
        c.teleport(0.5, TOP + 20, -60.5);
        c.cmd("item replace block 0 " + (TOP + 1) + " 0 container.0 with hbm:egg_balefire");
        c.cmd("item replace block 0 " + (TOP + 1) + " 0 container.1 with hbm:battery_spark");
        c.cmd("data merge block 0 " + (TOP + 1) + " 0 {started:1b,timer:40}");
        c.markLog();
        c.lookAt(0.5, TOP + 30, 0.5);

        c.waitTicks(80);
        Expect.that(!block(c, 0, TOP + 1, 0).contains("nuke_fstbmb"), "the bomb went off: " + block(c, 0, TOP + 1, 0));
        for (int s = 1; s <= 4; s++) {
            c.waitTicks(100);
            c.lookAt(0.5, TOP + 40, 0.5);
            c.waitTicks(2);
            c.screenshot("0" + s + "-cloud");
        }

        int air = 0;
        for (int x = -6; x <= 6; x += 3) for (int z = -6; z <= 6; z += 3) {
            if (block(c, x, TOP, z).contains("minecraft:air")) air++;
        }
        System.out.println("[Gh22] slab columns dug at the top: " + air + "/25");
        c.teleport(0.5, TOP + 15, -20.5);
        c.lookAt(0.5, TOP - 10, 0.5);
        c.waitTicks(20);
        c.screenshot("05-crater");
        Expect.that(air >= 20, "the crater reached the slab below y = 0: " + air + "/25 columns open");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode creative");
        c.teleport(0.5, 101, 0.5);
    }

    private static String block(Client c, int x, int y, int z) {
        JsonObject b = c.block(x, y, z);
        return b.has("block") ? b.get("block").getAsString() : b.toString();
    }
}
