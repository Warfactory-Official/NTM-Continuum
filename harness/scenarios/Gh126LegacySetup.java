import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 126, part 1 of 2, run on the old any-slot code: a chemical plant set to uranium hexafluoride gets 8
 * fluorite, then 2 yellowcake shift-clicked in, which the old rule puts the wrong way round in slots 4 and 5, and the
 * world is saved. {@code Gh126LegacyCheck} then opens it on the new code.
 */
public final class Gh126LegacySetup implements Scenario {

    static final int Y = 100, G = Y + 1, X = 30;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, X, Y, 0, 10);
        c.cmd("fill " + (X - 6) + " " + G + " -6 " + (X + 6) + " " + (Y + 10) + " 6 minecraft:air");
        Fixtures.giveOnly(c, "hbm:machine_chemical_plant", 1);
        c.teleport(X + 0.5, G, -3.5);
        c.waitTicks(3);
        c.lookAt(X + 0.5, Y + 0.98, 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(10);
        int[] core = findCore(c);
        Expect.that(core != null, "the chemical plant formed a core");
        c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {recipe0:\"chem.uf6\"}");
        c.cmd("clear");
        c.cmd("give @s hbm:fluorite 8");
        c.cmd("give @s hbm:powder_yellowcake 2");
        c.waitTicks(3);
        open(c, core);
        quickMove(c, "hbm:fluorite");
        quickMove(c, "hbm:powder_yellowcake");
        String layout = slot(c, 4) + ", " + slot(c, 5) + ", " + slot(c, 6);
        System.out.println("[Gh126] old layout: " + layout);
        Expect.that(layout.equals("hbm:fluorite x8, hbm:powder_yellowcake x2, empty"), "the old code puts them the wrong way round: " + layout);
        c.screenshot("01-old-layout");
        c.key("escape");
        c.waitTicks(5);
        c.leaveWorld(60_000L);
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
