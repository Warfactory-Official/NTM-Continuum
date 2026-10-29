import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/** GitHub issue 91: diamond ore (and its deepslate twin) beside volcanic lava turns into the volcanic gem ore. */
public final class Gh91VolcanicGem implements Scenario {

    private static final int Y = 100, G = 101;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 8);
        c.cmd("fill -8 " + G + " -8 8 " + (Y + 6) + " 8 minecraft:air");
        c.cmd("fill 1 " + G + " -1 5 " + (G + 1) + " 1 minecraft:stone");
        c.cmd("setblock 3 " + (G + 1) + " 0 minecraft:air");
        c.cmd("setblock 3 " + G + " 0 hbm:volcanic_lava_block");
        c.waitTicks(2);
        // The lava reacts to a neighbour change, as when a player places the ore beside it.
        c.cmd("setblock 2 " + G + " 0 minecraft:diamond_ore");
        c.cmd("setblock 4 " + G + " 0 minecraft:deepslate_diamond_ore");
        c.teleport(3.5, G + 2, -3.5);
        c.lookAt(3, G, 0);
        c.waitTicks(10);
        String ore = c.block(2, G, 0).get("block").getAsString();
        String deep = c.block(4, G, 0).get("block").getAsString();
        c.screenshot("volcanic-gem");
        System.out.println("[Gh91VolcanicGem] " + c.node() + " diamond=" + ore + " deepslate=" + deep);
        Expect.that(ore.equals("hbm:ore_basalt_gem"), "diamond ore became " + ore);
        Expect.that(deep.equals("hbm:ore_basalt_gem"), "deepslate diamond ore became " + deep);
        c.cmd("fill -8 " + G + " -8 8 " + (Y + 6) + " 8 minecraft:air");
    }
}
