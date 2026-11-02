import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 105: an RBMK rod alone in a crafting table gives eight pellets once it is below 99% fuel and both its
 * hull and core are under 50 C, as in 1.7. A fresh rod and a hot rod give nothing.
 */
public final class Gh105RbmkDisassembly implements Scenario {

    private static final int X = 0, Y = 100, Z = 0;
    private static final String ROD = "hbm:rbmk_fuel_hep";

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 8);

        craft(c, "", "", "fresh");
        craft(c, rod(c, 20), "hbm:rbmk_pellet_hep", "half-cool");
        craft(c, rod(c, 60), "", "half-hot");

        Expect.logCleanSinceMark(c, "(?i)exception.*(recipe|hbm)");
    }

    /** A HEP-239 rod at half its 1.7 yield of 1e8 with the given hull heat. */
    private static String rod(Client c, int hull) {
        String data = "yield:5.0E7d,xenon:0.0d,core:20.0d,hull:" + hull + ".0d";
        return c.node().startsWith("1.20.") ? "{\"hbm:rbmk_fuel\":{" + data + "}}" : "[hbm:rbmk_fuel={" + data + "}]";
    }

    private static void craft(Client c, String components, String expected, String shot) {
        // Visual Workbench keeps a closed table's grid, so each case gets a new table.
        c.cmd("setblock " + X + " " + (Y + 1) + " " + (Z + 3) + " minecraft:air");
        c.cmd("kill @e[type=item]");
        c.cmd("setblock " + X + " " + (Y + 1) + " " + (Z + 3) + " minecraft:crafting_table");
        c.cmd("clear");
        c.cmd("give @s " + ROD + components + " 1");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.hotbar(9);
        c.waitTicks(10);
        c.lookAt(X + 0.5, Y + 1.5, Z + 3.5);
        c.waitTicks(1);
        openTable(c);
        c.waitTicks(3);
        c.clickSlot(rodSlot(c));
        c.waitTicks(1);
        c.clickSlot(5, 1);
        c.waitTicks(4);
        c.screenshot(shot);
        JsonObject result = c.slot(0);
        Expect.that(expected.equals(id(result)),shot + " rod offers '" + expected + "': " + result);
        if (!expected.isEmpty()) {
            Expect.that(result.get("count").getAsInt() == 8, "eight pellets: " + result);
            c.clickSlot(0);
            c.waitTicks(3);
            Expect.that(id(c.slot(5)).isEmpty(), "taking the pellets uses up the rod: " + c.slot(5));
            c.clickSlot(emptySlot(c));
            c.waitTicks(2);
        }
        c.key("escape");
        c.waitTicks(3);
    }

    /** On a dedicated server the first right-click after a teleport can be lost, so it is retried once. */
    private static void openTable(Client c) {
        c.use(2);
        try {
            c.waitScreen("Crafting", 3000);
        } catch (RuntimeException lost) {
            c.use(2);
            c.waitScreen("Crafting", 5000);
        }
    }

    private static String id(JsonObject slot) {
        return slot.has("id") && !slot.get("id").isJsonNull() ? slot.get("id").getAsString() : "";
    }

    private static int emptySlot(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && id(s).isEmpty()) return s.get("index").getAsInt();
        }
        throw new IllegalStateException("no empty inventory slot");
    }

    private static int rodSlot(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && ROD.equals(s.get("id").getAsString())) {
                return s.get("index").getAsInt();
            }
        }
        throw new IllegalStateException("no rod in the inventory: " + c.screen());
    }
}
