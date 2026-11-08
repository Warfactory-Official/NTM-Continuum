import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * 1.7's thrown sawblade: the sawmill's Blade model spinning in flight, a bounce off a plank wall that punches the
 * planks out, and the blade lying still once it has landed. Shots for reading, plus the wall check.
 */
public final class Gh150Sawblade implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        String hideGui = null;
        try {
            Fixtures.platform(c, 0, Y, 0, 8);
            c.cmd("gamemode creative");
            c.cmd("gamerule sendCommandFeedback false");
            c.cmd("time set 6000");
            c.cmd("fill -2 " + (Y + 1) + " -7 2 " + (Y + 3) + " -7 minecraft:oak_planks");
            hideGui = c.setOption("hide_gui", "true");
            c.teleport(4.5, Y + 1, 0.5);
            c.lookAt(0, Y + 2, -5);
            c.waitTicks(20);
            float rate = c.tickRate(4F);
            c.cmd("summon hbm:entity_sawblade 0.5 " + (Y + 2) + " 0.5 {Motion:[0.0d,0.15d,-1.0d]}");
            c.waitTicks(2);
            c.screenshot("sawblade-flight");
            c.waitTicks(5);
            c.screenshot("sawblade-bounce");
            c.tickRate(rate);
            c.waitTicks(60);
            c.screenshot("sawblade-landed");
            String wall = c.block(0, Y + 2, -7).toString();
            System.out.println("[Gh150] " + c.node() + " wall block after the hit: " + wall);
            Expect.that(wall.contains("air"), "the bounce punched the planks out: " + wall);
            Expect.logCleanSinceMark(c, "Exception|/(ERROR|FATAL)[]].*hbm");
        } finally {
            c.tickRate(20F);
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.cmd("kill @e[type=hbm:entity_sawblade]");
            c.cmd("fill -2 " + (Y + 1) + " -7 2 " + (Y + 3) + " -7 minecraft:air");
            c.teleport(0.5, Y + 1, 0.5);
        }
    }
}
