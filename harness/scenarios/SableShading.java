import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Player report: with GemRender installed, blocks on a Sable sub-level lose their per-face shading. Builds a stepped
 * gold deck, screenshots it in the world, assembles it and screenshots it again from the same spot. Run once with and
 * once without GemRender ({@code ORG_GRADLE_PROJECT_gemrender.run=false}) and compare. Needs Sable (1.21.1 nodes).
 */
public final class SableShading implements Scenario {

    private static final int Y = 100, D = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.key("escape");
        c.waitTicks(2);
        c.cmd("sable remove @e");
        c.waitTicks(5);
        Fixtures.platform(c, 0, Y, 0, 16);
        c.cmd("gamemode creative");
        c.cmd("time set noon");
        c.cmd("fill -8 " + D + " -4 8 " + (D + 8) + " 12 minecraft:air");
        c.cmd("fill -4 " + D + " 4 4 " + D + " 10 minecraft:gold_block");
        c.cmd("fill -4 " + (D + 1) + " 4 0 " + (D + 1) + " 5 minecraft:gold_block");
        c.cmd("fill 2 " + (D + 1) + " 8 4 " + (D + 1) + " 10 minecraft:gold_block");
        view(c);
        c.screenshot("10-world");

        c.cmd("sable assemble area -4 " + D + " 4 4 " + (D + 1) + " 10");
        c.waitTicks(60);
        c.cmd("sable paused true");
        view(c);
        c.screenshot("20-sublevel");
        c.cmd("sable paused false");
    }

    private static void view(Client c) {
        c.teleport(-6.5, D + 5, 0.5);
        c.lookAt(0.5, D + 0.5, 7.5);
        c.waitTicks(40);
    }
}
