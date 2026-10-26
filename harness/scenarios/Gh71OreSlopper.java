import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 71, the Bedrock Ore Processor. Its JEI tab holds 1.7's one recipe with the machine as catalyst; it
 * places by hand looking along each of the four axes with the player three blocks from the clicked cell; and its
 * hovered outline lies along the machine (7 long, 3 wide, 3.25 high). Shots 1x show each facing's outline on the model.
 */
public final class Gh71OreSlopper implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final String[] NAMES = {"east", "west", "south", "north"};

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(180_000L);
        Fixtures.platform(c, 0, Y, 0, 16);
        c.cmd("gamemode creative");
        c.cmd("gamerule sendCommandFeedback false");
        clear(c);
        c.markLog();

        JsonObject category = new JsonObject();
        category.addProperty("category", "hbm:ore_slopper");
        JsonObject opened = c.query("jei", category);
        Expect.that(opened.has("opened") && opened.get("opened").getAsBoolean(), "JEI opened the ore slopper tab: " + opened);
        Expect.equal(1L, opened.get("recipes").getAsLong(), "the tab holds 1.7's one recipe");
        c.waitTicks(30);
        c.screenshot("01-jei-tab");
        c.key("escape");
        c.waitTicks(10);

        long catalysts = 0;
        for (JsonElement row : c.query("jei", new JsonObject()).getAsJsonArray("categories")) {
            JsonObject tab = row.getAsJsonObject();
            if (tab.get("id").getAsString().equals("hbm:ore_slopper")) catalysts = tab.get("catalysts").getAsLong();
        }
        // JEI's uses (U) of a catalyst open its tabs
        Expect.equal(1L, catalysts, "the machine is the tab's catalyst");

        for (int i = 0; i < DIRS.length; i++) {
            int dx = DIRS[i][0], dz = DIRS[i][1];
            Fixtures.giveOnly(c, "hbm:machine_ore_slopper", 1);
            c.teleport(0.5, G, 0.5);
            c.waitTicks(5);
            c.lookAt(0.5 + 3 * dx, Y + 0.98, 0.5 + 3 * dz);
            c.waitTicks(5);
            c.use(2);
            c.waitTicks(15);
            JsonObject core = c.block(6 * dx, G, 6 * dz);
            Expect.equal("hbm:machine_ore_slopper", core.get("block").getAsString(),
                    "placed looking " + NAMES[i] + ", core three blocks past the clicked cell: " + core);

            c.cmd("clear");
            c.teleport(6 * dx + 0.5 + 4 * dz, G + 1, 6 * dz + 0.5 + 4 * dx);
            c.waitTicks(5);
            c.lookAt(6 * dx + 0.5 + 1.4 * dz, G + 0.6, 6 * dz + 0.5 + 1.4 * dx);
            c.waitTicks(20);
            c.screenshot((10 + i) + "-outline-" + NAMES[i]);
            JsonObject outline = c.query("hbm_outline", new JsonObject());
            Expect.that(outline.has("bounds"), "an outline is drawn looking " + NAMES[i] + ": " + outline);
            double[] size = size(outline);
            double along = dx != 0 ? size[0] : size[2], across = dx != 0 ? size[2] : size[0];
            Expect.that(Math.abs(along - 7) < 1e-6 && Math.abs(across - 3) < 1e-6 && Math.abs(size[1] - 3.25) < 1e-6,
                    "the outline lies along the machine placed looking " + NAMES[i] + ": " + outline);
            clear(c);
        }
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[JEI\\].*error|/(ERROR|FATAL)[]]).*");
    }

    private static void clear(Client c) {
        c.cmd("fill -12 " + G + " -12 12 " + (Y + 8) + " 12 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.waitTicks(5);
    }

    private static double[] size(JsonObject outline) {
        String[] corners = outline.get("bounds").getAsString().split(" \\.\\. ");
        String[] min = corners[0].split(","), max = corners[1].split(",");
        double[] size = new double[3];
        for (int i = 0; i < 3; i++) size[i] = Double.parseDouble(max[i]) - Double.parseDouble(min[i]);
        return size;
    }
}
