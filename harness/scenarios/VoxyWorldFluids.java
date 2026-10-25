import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * Voxy bakes every world fluid as level-of-detail terrain without a model error (it fails on a fluid block that is
 * not a LiquidBlock). 26.1 Fabric only, with ORG_GRADLE_PROJECT_voxy.run=true and `up --rebuild`.
 */
public final class VoxyWorldFluids implements Scenario {

    private static final int X = 2600;
    private static final int Y = 100;
    private static final int Z = 2600;
    private static final String[] FLUIDS = {
            "mud_block", "acid_block", "toxic_block", "schrabidic_block", "sulfuric_acid_block", "corium_block",
            "volcanic_lava_block", "rad_lava_block",
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Expect.that(!c.log(4000, "(?i)\\bvoxy\\b").isEmpty(), "Voxy is not in this run (set ORG_GRADLE_PROJECT_voxy.run=true, up --rebuild)");
        c.markLog();
        String renderDistance = c.setOption("render_distance", "4");
        String hideGui = c.setOption("hide_gui", "true");
        try {
            Fixtures.platform(c, X, Y, Z, 12, "minecraft:obsidian");
            c.cmd("gamemode spectator");
            for (int i = 0; i < FLUIDS.length; i++) {
                int x = X - 10 + i * 3;
                c.cmd("setblock " + x + " " + (Y + 1) + " " + Z + " hbm:" + FLUIDS[i]);
                c.cmd("setblock " + x + " " + (Y + 1) + " " + (Z + 2) + " hbm:" + FLUIDS[i] + "[level=2]");
            }
            c.teleport(X, Y + 12, Z - 16);
            c.lookAt(X, Y + 1, Z);
            c.waitTicks(100);
            c.screenshot("near");

            // past the render distance, where Voxy draws the row
            c.teleport(X, Y + 60, Z - 220);
            c.lookAt(X, Y + 1, Z);
            c.waitTicks(600);
            c.screenshot("far-lod");

            c.teleport(X, Y + 12, Z - 16);
            c.lookAt(X, Y + 1, Z);
            c.waitTicks(200);
            c.screenshot("back");

            List<String> bake = c.logSinceMark("not already baked|ModelFactory|StackOverflowError|voxy.*(Exception|ERROR)");
            Expect.that(bake.isEmpty(), "Voxy reported a model bake problem: " + bake);
            Expect.logCleanSinceMark(c, "IllegalStateException");
        } finally {
            c.cmd("fill " + (X - 12) + " " + (Y + 1) + " " + (Z - 3) + " " + (X + 14) + " " + (Y + 3) + " " + (Z + 5) + " minecraft:air");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (renderDistance != null) c.setOption("render_distance", renderDistance);
        }
    }
}
