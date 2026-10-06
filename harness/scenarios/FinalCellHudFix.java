import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * 2026-09-25 cell / HUD correction.
 * <ul>
 *   <li>Full cells: four SAS3 cells come from a real prototype kit (ItemCell.getFullCell), the six other full
 *       cells and the empty cell are given with exactly the components getFullCell writes. Every one must show its
 *       1.7 name (checked through the slot's hover name) and icon (screenshot), and the SAS3 / AMAT / ASCHRAB /
 *       tritium tooltips are captured by hovering.</li>
 *   <li>The AJR charge bars in first person, in survival, with the chat still showing the commands that dressed
 *       the player (the 1.20.1 fault: chat lines at z 50 hid the bars), then again once the chat has faded.</li>
 * </ul>
 */
public final class FinalCellHudFix implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    /** fluid id (the port's Fluids order), CellVariants index, expected English name. */
    private static final Object[][] CELLS = {
            {"hbm:uf6", 1, "Uranium Hexafluoride Cell"},
            {"hbm:puf6", 2, "Plutonium Hexafluoride Cell"},
            {"hbm:amat", 3, "Antimatter Cell"},
            {"hbm:deuterium", 4, "Deuterium Cell"},
            {"hbm:tritium", 5, "Tritium Cell"},
            {"hbm:aschrab", 7, "Antischrabidium Cell"},
    };

    private static boolean legacy(Client c) {
        return c.node().startsWith("1.20.");
    }

    /** A full cell; {@code variant} is the sprite its fluid selects (no longer stored, History §19es-itemvariant). */
    private static String cell(Client c, String fluid, int variant) {
        return legacy(c)
                ? "hbm:cell{\"hbm:fluid\":\"" + fluid + "\",\"hbm:fluid_amount\":1000}"
                : "hbm:cell[hbm:fluid=\"" + fluid + "\",hbm:fluid_amount=1000]";
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 10);
        c.cmd("gamemode survival");
        c.cmd("effect clear @s");
        c.cmd("effect give @s minecraft:saturation 600 4 true");
        c.cmd("clear");
        strip(c);
        c.waitTicks(2);

        cells(c);
        chargeBars(c);

        strip(c);
        c.cmd("clear");
        c.cmd("effect clear @s");
        c.cmd("gamemode creative");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Missing model|Failed to load texture|Exception");
    }

    // ------------------------------------------------------------------ cells

    private void cells(Client c) {
        // SAS3 through the real ItemCell.getFullCell path: the prototype kit's four cells.
        c.cmd("give @s hbm:prototype_kit 1");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, -80);
        c.hotbar(1);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(5);
        strip(c); // the kit dresses a grey hazmat suit
        Expect.that(count(c, "hbm:cell", "Schrabidium Trisulfide Cell") == 4,
                "prototype kit gave four cells named Schrabidium Trisulfide Cell: " + c.inventory());

        c.cmd("give @s hbm:cell 1");
        for (Object[] row : CELLS) c.cmd("give @s " + cell(c, (String) row[0], (Integer) row[1]) + " 1");
        c.waitTicks(5);
        Expect.that(count(c, "hbm:cell", "Empty Cell") == 1, "a bare cell is named Empty Cell");
        for (Object[] row : CELLS) {
            Expect.that(count(c, "hbm:cell", (String) row[2]) == 1, "full cell named " + row[2] + ": " + c.inventory());
        }

        c.key("e");
        c.waitTicks(10);
        c.screenshot("10-inventory-cells");
        String[] hovers = {"Schrabidium Trisulfide Cell", "Antimatter Cell", "Antischrabidium Cell", "Tritium Cell",
                "Uranium Hexafluoride Cell"};
        int n = 0;
        for (String name : hovers) {
            hoverNamed(c, name);
            c.screenshot("1" + (++n) + "-tooltip-" + name.toLowerCase().replace(' ', '_'));
        }
        c.key("escape");
        c.waitTicks(3);
        c.cmd("clear");
        c.waitTicks(2);
    }

    // ------------------------------------------------------------------ AJR charge bars

    private void chargeBars(Client c) {
        // The carried SAS3 cells blinded the player (1.7 hazard); clear it so no effect particles sit in front
        // of the camera in the HUD captures.
        c.cmd("effect clear @s");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, 10);
        c.cmd("item replace entity @s armor.head with hbm:ajr_helmet");
        c.cmd("item replace entity @s armor.chest with hbm:ajr_plate");
        c.cmd("item replace entity @s armor.legs with hbm:ajr_legs");
        c.cmd("item replace entity @s armor.feet with hbm:ajr_boots");
        c.waitTicks(20);
        JsonObject a = c.query("hbm_armor_set", new JsonObject());
        Expect.that(a.get("fsb").getAsBoolean(), "AJR full set worn: " + a);
        Expect.that(a.getAsJsonObject("chest").get("charge").getAsLong() > 0, "AJR has charge to show: " + a);
        c.screenshot("20-first-person-ajr-charge-bars-with-chat");
        c.waitTicks(260); // 1.20.1/26.1 chat lines fade after 200 ticks
        c.screenshot("21-first-person-ajr-charge-bars-chat-faded");
    }

    // ------------------------------------------------------------------ helpers

    private void strip(Client c) {
        c.cmd("item replace entity @s armor.head with minecraft:air");
        c.cmd("item replace entity @s armor.chest with minecraft:air");
        c.cmd("item replace entity @s armor.legs with minecraft:air");
        c.cmd("item replace entity @s armor.feet with minecraft:air");
        c.waitTicks(2);
    }

    private void hoverNamed(Client c, String name) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("empty").getAsBoolean() && s.has("name") && s.get("name").getAsString().equals(name)) {
                c.mouseMove(s.get("center_x").getAsDouble(), s.get("center_y").getAsDouble());
                c.waitTicks(5);
                return;
            }
        }
        Expect.that(false, name + " not found in the open screen");
    }

    private static int count(Client c, String itemId, String name) {
        int n = 0;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("empty").getAsBoolean()) continue;
            if (s.get("id").getAsString().equals(itemId) && s.get("name").getAsString().equals(name)) {
                n += s.get("count").getAsInt();
            }
        }
        return n;
    }
}
