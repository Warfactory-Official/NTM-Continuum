import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Fix phase 1B (History §19ej-1b): the 1.7 IRecipe handlers, crafted by real mouse clicks in a real crafting table.
 * <ul>
 *   <li>GrenadeCraftingHandler: frag shell + HE filling + 3 s fuze give a universal grenade;</li>
 *   <li>CargoShellCraftingHandler: an empty cargo shell (ammo_arty_cargo, 1.7 ammo_arty meta 8) and a diamond give the shell carrying
 *       one diamond;</li>
 *   <li>ScrapsCraftingHandler: steel scraps worth three ingots split into two halves;</li>
 *   <li>a restored declared recipe with a component result: the SEDNA scope (weapon_mod_special_scope).</li>
 * </ul>
 * Custom recipe types are registered per loader, so this runs on every node.
 */
public final class Fix1BCustomCrafting implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    private static boolean legacy(Client c) {
        return c.node().startsWith("1.20.");
    }

    private static String withComponents(Client c, String id, String modern, String legacyNbt) {
        return legacy(c) ? id + legacyNbt : id + modern;
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 12);
        c.cmd("setblock " + X + " " + (Y + 1) + " " + (Z + 3) + " minecraft:crafting_table");
        c.cmd("gamemode survival");
        c.waitTicks(2);

        grenade(c);
        cargo(c);
        scraps(c);
        scope(c);

        c.cmd("gamemode creative");
        Expect.logCleanSinceMark(c, "(?i)(exception|Failed to parse|Couldn't parse).*(recipe|hbm)");
    }

    private void openTable(Client c) {
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(2);
        c.hotbar(9);
        c.lookAt(X + 0.5, Y + 1.5, Z + 3.5);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen("Crafting", 5000);
        Expect.screenStaysOpen(c, "Crafting", 10);
        c.waitTicks(3);
    }

    private static JsonObject invSlot(Client c, String id) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) return s;
        }
        return null;
    }

    private static int emptyInvSlot(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && (!s.has("id") || s.get("id").isJsonNull())) {
                return s.get("index").getAsInt();
            }
        }
        throw new IllegalStateException("no empty inventory slot");
    }

    private static void place(Client c, String id, int... gridSlots) {
        JsonObject slot = invSlot(c, id);
        Expect.that(slot != null, "the inventory holds " + id + ": " + c.screen());
        c.clickSlot(slot.get("index").getAsInt());
        c.waitTicks(1);
        for (int grid : gridSlots) {
            c.clickSlot(grid, 1);
            c.waitTicks(1);
        }
        JsonObject again = invSlot(c, id);
        c.clickSlot(again != null ? again.get("index").getAsInt() : emptyInvSlot(c));
        c.waitTicks(2);
    }

    private static String id(JsonObject slot) {
        return slot.has("id") && !slot.get("id").isJsonNull() ? slot.get("id").getAsString() : "";
    }

    /** Takes the result by a click, drops it into an empty slot, and returns the result slot as it was offered. */
    private static JsonObject take(Client c, String expected, int count) {
        JsonObject result = c.slot(0);
        Expect.that(expected.equals(id(result)), "the table offers " + expected + ": " + result);
        Expect.that(result.get("count").getAsInt() == count, "the table offers " + count + ": " + result);
        c.clickSlot(0);
        c.waitTicks(2);
        c.clickSlot(emptyInvSlot(c));
        c.waitTicks(2);
        return result;
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitTicks(3);
        c.cmd("clear");
        c.waitTicks(2);
    }

    private void grenade(Client c) {
        c.cmd("clear");
        c.cmd("give @s hbm:grenade_shell_frag 1");
        c.cmd("give @s hbm:grenade_filling_he 1");
        c.cmd("give @s hbm:grenade_fuze_s3 1");
        c.waitTicks(3);
        openTable(c);
        place(c, "hbm:grenade_shell_frag", 2);
        place(c, "hbm:grenade_filling_he", 5);
        place(c, "hbm:grenade_fuze_s3", 8);
        c.waitTicks(3);
        c.screenshot("10-grenade-grid");
        JsonObject result = take(c, "hbm:grenade_universal", 1);
        c.screenshot("11-grenade-taken");
        Expect.that(invSlot(c, "hbm:grenade_universal") != null, "the universal grenade arrived: " + result);
        Expect.that(invSlot(c, "hbm:grenade_shell_frag") == null && id(c.slot(2)).isEmpty(), "the parts were used up");
        close(c);
    }

    private void cargo(Client c) {
        c.cmd("give @s hbm:ammo_arty_cargo 1");
        c.cmd("give @s minecraft:diamond 3");
        c.waitTicks(3);
        openTable(c);
        place(c, "hbm:ammo_arty_cargo", 1);
        place(c, "minecraft:diamond", 9);
        c.waitTicks(3);
        c.screenshot("20-cargo-grid");
        take(c, "hbm:ammo_arty_cargo", 1);
        Expect.that(id(c.slot(1)).isEmpty() && id(c.slot(9)).isEmpty(), "the shell and the one diamond were used up");
        JsonObject diamonds = invSlot(c, "minecraft:diamond");
        Expect.that(diamonds != null && diamonds.get("count").getAsInt() == 2, "two diamonds stayed in the inventory");
        c.screenshot("21-cargo-taken");
        close(c);
    }

    private void scraps(Client c) {
        c.cmd("give @s " + withComponents(c, "hbm:scraps", "[hbm:material=\"steel\",hbm:mat_amount=216]",
                "{\"hbm:material\":\"steel\",\"hbm:mat_amount\":216}") + " 1");
        c.waitTicks(3);
        openTable(c);
        place(c, "hbm:scraps", 5);
        c.waitTicks(3);
        c.screenshot("30-scraps-grid");
        JsonObject result = take(c, "hbm:scraps", 2);
        c.screenshot("31-scraps-split");
        Expect.that(result.toString().toLowerCase().contains("steel"), "steel scraps came out: " + result);
        close(c);
    }

    private void scope(Client c) {
        c.cmd("give @s hbm:plate_steel 4");
        c.cmd("give @s hbm:ingot_polymer 2");
        c.cmd("give @s minecraft:glass_pane 2");
        c.waitTicks(3);
        openTable(c);
        place(c, "hbm:plate_steel", 1, 3, 7, 9);
        place(c, "hbm:ingot_polymer", 2, 8);
        place(c, "minecraft:glass_pane", 4, 6);
        c.waitTicks(3);
        c.screenshot("40-scope-grid");
        take(c, "hbm:weapon_mod_special_scope", 1);
        c.screenshot("41-scope-taken");
        close(c);
    }
}
