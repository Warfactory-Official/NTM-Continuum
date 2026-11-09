import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 126: fluorite filled all three chemical plant input slots, leaving no room for yellowcake. With the
 * uranium hexafluoride recipe selected, two stacks of fluorite shift-clicked in fill only slot 5, a stack clicked onto
 * the yellowcake slot stays on the cursor, spare slot 6 stays empty, and shift-clicked yellowcake lands in slot 4.
 */
public final class Gh126ChemPlantSlots implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 10);
        c.cmd("fill -6 " + G + " -6 6 " + (Y + 10) + " 6 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.markLog();

        Fixtures.giveOnly(c, "hbm:machine_chemical_plant", 1);
        c.teleport(0.5, G, -3.5);
        c.waitTicks(3);
        c.lookAt(0.5, Y + 0.98, 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(10);
        int[] core = findCore(c);
        Expect.that(core != null, "the chemical plant formed a core");
        c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {recipe0:\"chem.uf6\"}");

        c.cmd("clear");
        c.cmd("give @s hbm:fluorite 128");
        c.cmd("give @s hbm:fluorite 64");
        c.cmd("give @s hbm:powder_yellowcake 16");
        c.waitTicks(3);
        open(c, core);

        quickMove(c, "hbm:fluorite");
        quickMove(c, "hbm:fluorite");
        Expect.equal("hbm:fluorite", slotId(c, 5), "shift-clicked fluorite goes to slot 5");
        Expect.equal("minecraft:air", slotId(c, 4), "fluorite stays out of the yellowcake slot");
        Expect.equal("minecraft:air", slotId(c, 6), "fluorite stays out of spare slot 6");

        JsonObject stack = findSlot(c, "hbm:fluorite");
        Expect.that(stack != null, "fluorite left in the inventory");
        c.clickSlot(stack.get("index").getAsInt());
        c.waitTicks(2);
        c.clickSlot(4);
        c.waitTicks(2);
        Expect.equal("minecraft:air", slotId(c, 4), "a fluorite stack clicked onto slot 4 is refused");
        c.clickSlot(stack.get("index").getAsInt());
        c.waitTicks(2);

        quickMove(c, "hbm:powder_yellowcake");
        Expect.equal("hbm:powder_yellowcake", slotId(c, 4), "shift-clicked yellowcake goes to slot 4");
        c.screenshot("01-uf6-inputs");
        c.key("escape");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static void quickMove(Client c, String id) {
        JsonObject slot = findSlot(c, id);
        Expect.that(slot != null, "inventory slot holding " + id);
        c.slotClick(slot.get("index").getAsInt(), 0, "QUICK_MOVE");
        c.waitTicks(10);
    }

    private static void open(Client c, int[] core) {
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

    private static int[] findCore(Client c) {
        // on 1.20.1 Forge every cell has a proxy block entity, so match the core's own class
        for (int dx = -2; dx <= 2; dx++) for (int dz = -1; dz <= 3; dz++) {
            JsonObject be = c.blockEntity(dx, G, dz);
            if (be.has("block_entity") && !be.get("block_entity").isJsonNull()
                    && be.get("block_entity").getAsString().endsWith("TileEntityMachineChemicalPlant")) {
                return new int[]{dx, G, dz};
            }
        }
        return null;
    }

    private static String slotId(Client c, int index) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("index").getAsInt() == index) return s.has("id") ? s.get("id").getAsString() : "minecraft:air";
        }
        return "minecraft:air";
    }

    private static JsonObject findSlot(Client c, String id) {
        JsonObject screen = c.screen();
        if (!screen.has("slots")) return null;
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) return s;
        }
        return null;
    }
}
