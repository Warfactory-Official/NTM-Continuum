import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * The final recipe/data gap wave, by hand in a real crafting table and a real furnace:
 * <ul>
 *   <li>chocolate milk (1.7 shapeless, getDict(1000) nitroglycerin): a full universal tank is accepted, the grid
 *       keeps the milk bucket's bucket and an empty tank;</li>
 *   <li>gun_kit_1 (rubber, woodoil tank, iron) and the holotape restoration (component-matched tape, the
 *       screwdriver staying in the grid one use more worn);</li>
 *   <li>a steel crate filled with diamonds through its GUI, broken, picked up and crafted into a tungsten crate,
 *       which is placed and opened again with the diamonds inside (1.7 ContainerUpgradeCraftingHandler);</li>
 *   <li>a coal briquette burning in a vanilla furnace (the Forge burn-time gap).</li>
 * </ul>
 * Every item is moved by real mouse clicks on the screen's slots.
 */
public final class FinalRecipeGaps implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;
    private static final String NITROGLYCERIN = "hbm:nitroglycerin";
    private static final String WOODOIL = "hbm:woodoil";

    /** Per client: one scenario instance runs on both nodes in parallel, so no per-node field. */
    private static boolean legacy(Client c) {
        return c.node().startsWith("1.20.");
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 12);
        c.cmd("setblock " + X + " " + (Y + 1) + " " + (Z + 3) + " minecraft:crafting_table");
        c.waitTicks(2);

        chocolateMilk(c);
        gunKit(c);
        holotape(c);
        tungstenCrate(c);
        furnace(c);

        c.cmd("gamemode creative");
        Expect.logCleanSinceMark(c, "(?i)(exception|Failed to parse|Couldn't parse).*(recipe|hbm)");
    }

    private static String tank(Client c, String fluid) {
        return legacy(c) ? "hbm:fluid_tank{\"hbm:fluid\":\"" + fluid + "\",\"hbm:fluid_amount\":1000}"
                : "hbm:fluid_tank[hbm:fluid=\"" + fluid + "\",hbm:fluid_amount=1000]";
    }

    private static String variant(Client c, String id, int variant) {
        return legacy(c) ? id + "{\"hbm:item_variant\":" + variant + "}" : id + "[hbm:item_variant=" + variant + "]";
    }

    // ------------------------------------------------------------------ crafting table by clicks

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

    /** An empty hotbar slot: the last nine player slots of any container menu are the hotbar. */
    private static int emptyHotbarSlot(Client c) {
        java.util.List<JsonObject> player = new java.util.ArrayList<>();
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean()) player.add(s);
        }
        for (JsonObject s : player.subList(player.size() - 9, player.size() - 1)) { // keep hotbar 9 empty
            if (!s.has("id") || s.get("id").isJsonNull()) return s.get("index").getAsInt();
        }
        throw new IllegalStateException("no empty hotbar slot");
    }

    /** Picks up the whole stack of {@code id} and drops one item into each grid slot (1..9), by right-clicks. */
    private static void place(Client c, String id, int... gridSlots) {
        JsonObject slot = invSlot(c, id);
        Expect.that(slot != null, "the inventory holds " + id + ": " + c.screen());
        c.clickSlot(slot.get("index").getAsInt());
        c.waitTicks(1);
        for (int grid : gridSlots) {
            c.clickSlot(grid, 1);
            c.waitTicks(1);
        }
        // put back whatever is left on the cursor
        JsonObject again = invSlot(c, id);
        c.clickSlot(again != null ? again.get("index").getAsInt() : emptyInvSlot(c));
        c.waitTicks(2);
    }

    private static String id(JsonObject slot) {
        return slot.has("id") && !slot.get("id").isJsonNull() ? slot.get("id").getAsString() : "";
    }

    /** Takes the result into the inventory by a click and returns the grid afterwards. */
    private static void take(Client c, String expected) {
        JsonObject result = c.slot(0);
        Expect.that(expected.equals(id(result)), "the table offers " + expected + ": " + result);
        c.clickSlot(0);
        c.waitTicks(2);
        c.clickSlot(emptyHotbarSlot(c));
        c.waitTicks(2);
        Expect.that(invSlot(c, expected) != null, expected + " arrived in the inventory");
    }

    private static void clearGridAndClose(Client c) {
        c.key("escape");
        c.waitTicks(3);
        c.cmd("clear");
        c.waitTicks(2);
    }

    // ------------------------------------------------------------------ recipes

    private void chocolateMilk(Client c) {
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("give @s minecraft:glass_pane 1");
        c.cmd("give @s minecraft:cocoa_beans 1");
        c.cmd("give @s minecraft:milk_bucket 1");
        c.cmd("give @s " + tank(c, NITROGLYCERIN) + " 1");
        c.waitTicks(3);
        openTable(c);
        place(c, "minecraft:glass_pane", 1);
        place(c, "minecraft:cocoa_beans", 2);
        place(c, "minecraft:milk_bucket", 4);
        place(c, "hbm:fluid_tank", 5);
        c.waitTicks(3);
        c.screenshot("10-chocolate-milk-grid");
        take(c, "hbm:chocolate_milk");
        JsonObject bucket = c.slot(4);
        JsonObject tank = c.slot(5);
        Expect.that("minecraft:bucket".equals(id(bucket)), "the milk bucket left a bucket: " + bucket);
        Expect.that("hbm:fluid_tank".equals(id(tank)) && !tank.get("name").getAsString().toLowerCase().contains("nitro"),
                "the tank stayed in the grid, empty: " + tank);
        c.screenshot("11-chocolate-milk-remainders");
        clearGridAndClose(c);
    }

    private void gunKit(Client c) {
        c.cmd("give @s hbm:ingot_rubber 1");
        c.cmd("give @s minecraft:iron_ingot 1");
        c.cmd("give @s " + tank(c, WOODOIL) + " 1");
        c.waitTicks(3);
        openTable(c);
        place(c, "hbm:ingot_rubber", 1);
        place(c, "hbm:fluid_tank", 5);
        place(c, "minecraft:iron_ingot", 9);
        c.waitTicks(3);
        c.screenshot("20-gun-kit-grid");
        take(c, "hbm:gun_kit_1");
        JsonObject tank = c.slot(5);
        Expect.that("hbm:fluid_tank".equals(id(tank)) && !tank.get("name").getAsString().toLowerCase().contains("wood"),
                "the woodoil tank came back empty: " + tank);
        c.screenshot("21-gun-kit-remainder");
        clearGridAndClose(c);
    }

    private void holotape(Client c) {
        c.cmd("give @s " + variant(c, "hbm:holotape_image", 0) + " 1");
        c.cmd("give @s hbm:screwdriver 1");
        c.cmd("give @s hbm:ducttape 1");
        c.cmd("give @s hbm:armor_polish 1");
        c.waitTicks(3);
        openTable(c);
        place(c, "hbm:holotape_image", 1);
        place(c, "hbm:screwdriver", 2);
        place(c, "hbm:ducttape", 3);
        place(c, "hbm:armor_polish", 4);
        c.waitTicks(3);
        c.screenshot("30-holotape-grid");
        take(c, "hbm:holotape_image");
        JsonObject tool = c.slot(2);
        Expect.that("hbm:screwdriver".equals(id(tool)) && tool.get("damage").getAsInt() == 1,
                "the screwdriver stayed, one use more worn: " + tool);
        c.screenshot("31-holotape-screwdriver-stays");
        clearGridAndClose(c);
    }

    private void tungstenCrate(Client c) {
        int cx = X + 4, cz = Z + 3;
        // a steel crate filled through its own GUI, broken and picked up with its contents
        c.cmd("gamemode creative");
        c.cmd("setblock " + cx + " " + (Y + 1) + " " + cz + " hbm:crate_steel");
        c.cmd("give @s minecraft:diamond 7");
        c.waitTicks(3);
        c.teleport(cx + 0.5, Y + 1, cz - 2.5);
        c.waitTicks(2);
        Fixtures.interact(c, cx, Y + 1, cz);
        c.waitScreen("Steel Crate", 5000);
        c.waitTicks(3);
        JsonObject diamonds = invSlot(c, "minecraft:diamond");
        Expect.that(diamonds != null, "diamonds in the player half of the steel crate");
        c.clickSlot(diamonds.get("index").getAsInt());
        c.waitTicks(1);
        c.clickSlot(0);
        c.waitTicks(2);
        c.screenshot("40-steel-crate-filled");
        c.key("escape");
        c.waitTicks(3);
        Expect.that(c.blockEntity(cx, Y + 1, cz).toString().contains("diamond"), "the steel crate holds the diamonds");
        c.cmd("clear");
        c.cmd("kill @e[type=item]");
        c.cmd("gamemode survival");
        c.cmd("setblock " + cx + " " + (Y + 1) + " " + cz + " air destroy");
        c.waitTicks(5);
        c.teleport(cx + 0.5, Y + 1, cz + 0.5);
        c.waitTicks(20);
        Expect.that(c.inventory().toString().contains("hbm:crate_steel"), "the broken steel crate was picked up");

        c.cmd("give @s hbm:block_tungsten 4");
        c.cmd("give @s hbm:plate_cast_copper 4");
        c.waitTicks(3);
        openTable(c);
        place(c, "hbm:block_tungsten", 1, 3, 7, 9);
        place(c, "hbm:plate_cast_copper", 2, 4, 6, 8);
        place(c, "hbm:crate_steel", 5);
        c.waitTicks(3);
        c.screenshot("41-tungsten-crate-grid");
        take(c, "hbm:crate_tungsten");
        c.key("escape");
        c.waitTicks(3);

        // put the crafted crate down and open it: the diamonds came along
        int tx = X - 4, tz = Z + 3;
        JsonObject inv = c.inventory();
        int hotbar = -1;
        for (JsonElement e : inv.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if ("hbm:crate_tungsten".equals(s.get("id").getAsString()) && s.get("slot").getAsInt() < 9) hotbar = s.get("slot").getAsInt();
        }
        Expect.that(hotbar >= 0, "the tungsten crate is in the hotbar: " + inv);
        c.teleport(tx + 2.5, Y + 1, tz + 0.5);
        c.waitTicks(2);
        c.hotbar(hotbar + 1);
        c.waitTicks(1);
        // 1.7 ItemBlockStorageCrate under CRATE_OPEN_HELD (on by default): only a sneak-click places a crate
        c.keyDown("key.sneak");
        c.waitTicks(5);
        Expect.that(c.player().get("sneaking").getAsBoolean(), "holding the sneak key makes the player sneak");
        c.lookAt(tx + 0.5, Y + 0.98, tz + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        c.keyUp("key.sneak");
        c.waitTicks(3);
        Expect.block(c, tx, Y + 1, tz, "hbm:crate_tungsten");
        String nbt = c.blockEntity(tx, Y + 1, tz).toString();
        Expect.that(nbt.contains("diamond"), "the crafted tungsten crate kept the steel crate's diamonds: " + nbt);
        Fixtures.interact(c, tx, Y + 1, tz);
        c.waitScreen("Tungsten Crate", 5000);
        c.waitTicks(5);
        c.screenshot("42-tungsten-crate-contents");
        c.key("escape");
        c.waitTicks(3);
        c.cmd("clear");
    }

    private void furnace(Client c) {
        int fx = X - 4, fz = Z - 3;
        c.cmd("gamemode survival");
        c.cmd("setblock " + fx + " " + (Y + 1) + " " + fz + " minecraft:furnace[facing=south]");
        c.cmd("give @s minecraft:raw_iron 1");
        c.cmd("give @s hbm:briquette_coal 1");
        c.waitTicks(3);
        c.teleport(fx + 0.5, Y + 1, fz + 2.5);
        c.waitTicks(2);
        Fixtures.interact(c, fx, Y + 1, fz);
        c.waitScreen("Furnace", 5000);
        c.waitTicks(3);
        place(c, "minecraft:raw_iron", 0);
        place(c, "hbm:briquette_coal", 1);
        c.waitTicks(40);
        JsonObject fuel = c.slot(1);
        Expect.that(id(fuel).isEmpty(), "the furnace burned the briquette as fuel: " + fuel);
        c.screenshot("50-furnace-briquette-burning");
        c.key("escape");
        c.waitTicks(3);
        JsonObject block = c.block(fx, Y + 1, fz);
        Expect.that("true".equals(block.getAsJsonObject("properties").get("lit").getAsString()),
                "the furnace is lit on a coal briquette: " + block);
    }
}
