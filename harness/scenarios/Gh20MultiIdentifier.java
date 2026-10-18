import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 20: the electrolyser (and the ore slopper, excavator and annihilator) took only the single fluid
 * identifier. The multi identifier must go into the electrolyser's identifier slot, retype the input tank and pass to
 * the slot below, as 1.7's {@code setType(3, 4, slots)} does. The other machines are covered by GameTests.
 */
public final class Gh20MultiIdentifier implements Scenario {

    private static final int Y = 100, G = 101;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.markLog();

        open(c, "hbm:machine_electrolyser");
        give(c);
        clickFromInventory(c, "hbm:fluid_identifier_multi", 3);
        c.waitTicks(10);
        c.mouseMove(2, 2);
        c.screenshot("01-electrolyser");
        String moved = slotItem(c, 4);
        Expect.that("hbm:fluid_identifier_multi".equals(moved),
                "the multi identifier retyped the tank and passed to slot 4, which holds " + moved + ", slot 3 " + slotItem(c, 3));
        String nbt = nbtNear(c);
        System.out.println("[Gh20] electrolyser: " + nbt);
        Expect.that(nbt.contains("heavywater"), "the electrolyser's input tank is heavy water: " + nbt);
        c.key("escape");
        c.waitTicks(5);

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static void open(Client c, String block) {
        c.cmd("fill -12 " + (Y - 1) + " -12 12 " + (Y + 14) + " 12 minecraft:air");
        Fixtures.platform(c, 0, Y, 0, 12);
        c.waitTicks(5);
        Fixtures.placeOnTop(c, block, 0, Y, 0);
        c.waitTicks(10);
        c.teleport(0.5, G, -3.5);
        Fixtures.interact(c, 0, G, 0);
        c.waitTicks(15);
        Expect.that(c.screen().has("slots"), block + " opened its screen");
    }

    private static void give(Client c) {
        String multi = c.node().startsWith("1.20.")
                ? "hbm:fluid_identifier_multi{\"hbm:fluid_multi\":{fluid1:\"hbm:heavywater\"}}"
                : "hbm:fluid_identifier_multi[hbm:fluid_multi={fluid1:\"hbm:heavywater\"}]";
        c.cmd("give @s " + multi + " 1");
        c.waitTicks(5);
    }

    /** The electrolyser's core block entity sits several blocks from the clicked dummy. */
    private static String nbtNear(Client c) {
        StringBuilder all = new StringBuilder();
        for (int dx = -7; dx <= 7; dx++) for (int dz = -4; dz <= 4; dz++) for (int dy = 0; dy <= 0; dy++) {
            JsonObject be = c.blockEntity(dx, G + dy, dz);
            if (be.has("nbt") && !be.get("nbt").isJsonNull()) all.append(be.get("nbt").getAsString());
        }
        return all.toString();
    }

    private static String slotItem(Client c, int index) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("index").getAsInt() == index) return s.has("id") ? s.get("id").getAsString() : "empty";
        }
        return "missing";
    }

    private static void clickFromInventory(Client c, String id, int machineSlot) {
        JsonObject slot = null;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) slot = s;
        }
        Expect.that(slot != null, "inventory slot holding " + id);
        c.clickSlot(slot.get("index").getAsInt());
        c.waitTicks(2);
        c.clickSlot(machineSlot);
        c.waitTicks(2);
    }
}
