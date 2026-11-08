import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 127: over the arc furnace's molten-material gauge, which is also JEI's recipe click area, only the
 * furnace's own tooltip shows (1.7's NEI let the GUI's hover win over its transfer-rect hint). Next to it, over JEI's
 * other area where the furnace draws nothing, JEI's "Show Recipes" still shows. Look at the two crops.
 */
public final class Gh127ArcFurnaceTooltip implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 10);
        c.cmd("gamemode creative");
        c.cmd("setblock 0 " + (Y + 1) + " 0 hbm:machine_arc_furnace[facing=north]");
        c.waitTicks(10);
        int[] core = core(c);
        c.teleport(core[0] + 0.5, Y + 1, core[2] - 3.5);
        c.waitTicks(5);
        Fixtures.interact(c, core[0], core[1], core[2]);
        c.waitScreen("rc", 8000);
        c.waitTicks(10);
        JsonObject s = c.screen();
        int left = s.get("left").getAsInt(), top = s.get("top").getAsInt();

        // the molten-material gauge (152,36 16x70) is also the fluid recipes' click area
        c.mouseMove(left + 160, top + 70);
        c.waitTicks(10);
        c.screenshot("gauge", new int[] {left + 100, top + 20, 260, 90});
        // JEI's solid-recipe area (17,36 7x70) beside the energy bar: the furnace draws no tooltip there
        c.mouseMove(left + 20, top + 70);
        c.waitTicks(10);
        c.screenshot("solid-area", new int[] {left, top + 20, 260, 90});
        c.key("escape");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception.*hbm");
    }

    private static int[] core(Client c) {
        for (int dy = 0; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            JsonObject be = c.blockEntity(dx, Y + 1 + dy, dz);
            if (be.has("block_entity") && !be.get("block_entity").isJsonNull()
                    && be.get("block_entity").getAsString().endsWith("TileEntityMachineArcFurnaceLarge")) {
                return new int[] {dx, Y + 1 + dy, dz};
            }
        }
        throw new AssertionError("no arc furnace core");
    }
}
