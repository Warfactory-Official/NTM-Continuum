import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 126, part 2 of 2, on the new code: the plant {@code Gh126LegacySetup} saved with fluorite in the
 * yellowcake slot and yellowcake in the fluorite slot keeps them, refuses new fluorite, and runs both operations.
 */
public final class Gh126LegacyCheck implements Scenario {

    static final int Y = 100, G = Y + 1, X = 30;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        int[] core = findCore(c);
        Expect.that(core != null, "the saved chemical plant is there");
        c.cmd("clear");
        c.cmd("give @s hbm:fluorite 16");
        c.waitTicks(20);
        open(c, core);
        String layout = slot(c, 4) + ", " + slot(c, 5) + ", " + slot(c, 6);
        System.out.println("[Gh126] after loading: " + layout);
        Expect.that(layout.equals("hbm:fluorite x8, hbm:powder_yellowcake x2, empty"), "the saved layout is kept: " + layout);
        quickMove(c, "hbm:fluorite");
        Expect.that(slot(c, 6).equals("empty") && slot(c, 4).equals("hbm:fluorite x8"), "new fluorite finds no slot: "
                + slot(c, 4) + ", " + slot(c, 6));
        c.screenshot("01-old-layout-kept");

        c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {power:1000000L,i0_type:\"minecraft:water\",i0:24000}");
        for (int i = 0; i < 60 && !slot(c, 5).equals("empty"); i++) c.waitTicks(20);
        c.waitTicks(10);
        layout = slot(c, 4) + ", " + slot(c, 5) + " -> " + slot(c, 7);
        System.out.println("[Gh126] after running: " + layout);
        Expect.that(layout.equals("empty, empty -> hbm:sulfur x4"), "two operations use the old stacks up: " + layout);
        c.screenshot("02-used-up");
        c.key("escape");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    static int[] findCore(Client c) {
        // on 1.20.1 Forge every cell has a proxy block entity, so match the core's own class
        for (int dx = X - 2; dx <= X + 2; dx++) for (int dz = -1; dz <= 3; dz++) {
            JsonObject be = c.blockEntity(dx, G, dz);
            if (be.has("block_entity") && !be.get("block_entity").isJsonNull()
                    && be.get("block_entity").getAsString().endsWith("TileEntityMachineChemicalPlant")) {
                return new int[]{dx, G, dz};
            }
        }
        return null;
    }

    static void open(Client c, int[] core) {
        c.teleport(core[0] + 0.5, G, core[2] - 3.5);
        c.waitTicks(3);
        c.hotbar(9);
        c.waitTicks(1);
        c.lookAt(core[0] + 0.5, G + 0.5, core[2] - 1.0);
        c.waitTicks(1);
        c.use(1);
        c.waitScreen("", 5000);
        c.waitTicks(5);
        Expect.that(c.screen().has("slots"), "the chemical plant menu opened");
    }

    static String slot(Client c, int index) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("index").getAsInt() == index) {
                return s.has("id") ? s.get("id").getAsString() + " x" + s.get("count").getAsInt() : "empty";
            }
        }
        return "empty";
    }

    static void quickMove(Client c, String id) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) {
                c.slotClick(s.get("index").getAsInt(), 0, "QUICK_MOVE");
                c.waitTicks(10);
                return;
            }
        }
        throw new AssertionError("no " + id + " in the inventory");
    }
}
