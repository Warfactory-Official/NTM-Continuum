import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game acceptance for the energy extras and the bedrock ore grade chain:
 *
 * <ol>
 *   <li>every new id in the (survival) inventory, in pages of 36, and a trait tooltip;</li>
 *   <li>a creative generator feeding a battery block through a hand-placed boxcable, coated wire and a thinner
 *       boxcable, read back from the battery's NBT; the cable run photographed;</li>
 *   <li>the HE -> RF -> HE round trip: generator, coated wire, HE->RF converter, RF->HE converter, coated wire,
 *       battery; the converters' RF stores and the far battery's charge read back;</li>
 *   <li>the charging station on a coated wire: a held lead-acid battery fills while the player stands at it, the
 *       clamps photographed extended;</li>
 *   <li>two chain steps in the real machines, through their menus: raw light-metal bedrock ore washed in the ore
 *       acidizer with water, then the washed ore centrifuged into two primary fractions and gravel.</li>
 * </ol>
 */
public final class FinalEnergyBedrock implements dev.golem.harness.api.Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;

    static final String[] BLOCK_IDS = {"hbm:red_wire_coated", "hbm:red_cable_box_0", "hbm:red_cable_box_1",
            "hbm:red_cable_box_2", "hbm:red_cable_box_3", "hbm:red_cable_box_4", "hbm:charger",
            "hbm:machine_converter_he_rf", "hbm:machine_converter_rf_he"};
    static final String[] GRADES = {"base_roasted", "base_washed", "primary", "primary_roasted", "primary_sulfuric",
            "primary_nosulfuric", "primary_solvent", "primary_nosolvent", "primary_rad", "primary_norad", "primary_first",
            "primary_second", "crumbs", "sulfuric_byproduct", "sulfuric_roasted", "sulfuric_arc", "sulfuric_washed",
            "solvent_byproduct", "solvent_roasted", "solvent_arc", "solvent_washed", "rad_byproduct", "rad_roasted",
            "rad_arc", "rad_washed"};
    static final String[] TYPES = {"light", "heavy", "rare", "actinide", "nonmetal", "crystal"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);

        inventory(c);
        cableRun(c);
        converters(c);
        charger(c);
        chain(c);

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model|missing texture");
    }

    // --- 1. inventory -------------------------------------------------------------------------------------------

    private static void inventory(Client c) {
        List<String> ids = new ArrayList<>(List.of(BLOCK_IDS));
        for (String type : TYPES) {
            for (String grade : GRADES) ids.add("hbm:bedrock_ore_" + grade + "_" + type);
        }
        int page = 0;
        for (int from = 0; from < ids.size(); from += 36, page++) {
            List<String> batch = ids.subList(from, Math.min(from + 36, ids.size()));
            c.cmd("gamemode survival");
            c.cmd("clear");
            c.waitTicks(2);
            for (String id : batch) c.cmd("give @s " + id + " 1");
            c.waitTicks(4);
            String inv = c.inventory().toString();
            for (String id : batch) Expect.that(inv.contains("\"" + id + "\""), "inventory holds " + id);
            c.key("key.inventory");
            c.waitScreen("", 5000);
            c.waitTicks(10);
            if (page == 1) hover(c, "hbm:bedrock_ore_primary_nosulfuric_heavy");
            c.screenshot(String.format("0%d-inventory-%d", page, page));
            close(c);
        }
        c.cmd("clear");
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }

    // --- 2. generator -> boxcable -> coated wire -> boxcable -> battery -------------------------------------------

    private static void cableRun(Client c) {
        int z = 4;
        Fixtures.setBlock(c, 0, G, z, "hbm:creative_generator");
        placeByHand(c, "hbm:red_cable_box_2", 1, z);
        placeByHand(c, "hbm:red_cable_box_2", 2, z);
        placeByHand(c, "hbm:red_wire_coated", 3, z);
        placeByHand(c, "hbm:red_cable_box_0", 4, z);
        placeByHand(c, "hbm:red_cable_box_4", 5, z);
        Fixtures.setBlock(c, 6, G, z, "hbm:machine_battery_potato");
        // a bend and a junction, so the curve and junction plates are drawn too
        placeFromWest(c, "hbm:red_cable_box_1", 5, z + 1);
        placeFromWest(c, "hbm:red_cable_box_1", 5, z + 2);
        placeByHand(c, "hbm:red_cable_box_3", 2, z - 1);
        c.waitTicks(40);
        long power = longOf(nbt(c, 6, G, z), "power");
        Expect.that(power > 0, "the battery behind the boxcable and coated wire charged: " + power + " HE");
        Expect.that(c.block(1, G, z).toString().contains("east=true") && c.block(1, G, z).toString().contains("west=true"),
                "the boxcable connected both ways: " + c.block(1, G, z));
        worldShot(c, "10-cable-run", 3.5, G + 3, z - 4.5, 3.5, G + 0.5, z + 0.5);
        worldShot(c, "11-cable-run-close", 5.5, G + 1.6, z - 2.2, 4.5, G + 0.5, z + 1);
        System.out.println("[FinalEnergyBedrock] battery after cable run: " + power + " HE");
    }

    // --- 3. HE -> RF -> HE -------------------------------------------------------------------------------------

    private static void converters(Client c) {
        int z = 9, h = G + 1;
        // 1.7 tryProvide hands HE to adjacent receivers in DOWN, UP, NORTH, SOUTH, WEST, EAST order, and the HE->RF
        // converter beside it is one: so the far battery sits UNDER the RF->HE converter, where it is served first.
        Fixtures.setBlock(c, 3, G, z, "hbm:machine_battery_potato");
        Fixtures.setBlock(c, 0, h, z, "hbm:creative_generator");
        Fixtures.setBlock(c, 1, h, z, "hbm:red_wire_coated");
        Fixtures.setBlock(c, 2, h, z, "hbm:machine_converter_he_rf");
        Fixtures.setBlock(c, 3, h, z, "hbm:machine_converter_rf_he");
        placeByHand(c, "hbm:machine_converter_he_rf", 6, z); // the item places by hand too
        placeByHand(c, "hbm:machine_converter_rf_he", 7, z);
        c.waitTicks(60);
        String heRf = nbt(c, 2, h, z), rfHe = nbt(c, 3, h, z), battery = nbt(c, 3, G, z);
        System.out.println("[FinalEnergyBedrock] he_rf " + heRf + " | rf_he " + rfHe + " | battery " + battery);
        Expect.that(longOf(heRf, "power") > 0, "the HE->RF converter took HE from the net: " + heRf);
        // RF is the platform energy API on every node: Forge Energy on Forge/NeoForge, Team Reborn Energy on Fabric
        // (optional there; §19ed-gaps-b item 8, loaded by the Fabric dev runs). So the round trip closes everywhere.
        Expect.that(longOf(battery, "power") > 0, "HE came back out of the RF->HE converter: " + battery);
        c.teleport(2.5, G, z - 2.5);
        c.waitTicks(2);
        c.lookAt(2.5, h + 0.5, z + 0.5);
        c.waitTicks(4);
        c.screenshot("20-converter-he-rf-overlay");
        c.lookAt(3.5, h + 0.5, z + 0.5);
        c.waitTicks(4);
        c.screenshot("21-converter-rf-he-overlay");
    }

    // --- 4. the charging station ------------------------------------------------------------------------------

    private static void charger(Client c) {
        int x = 0, z = 14;
        Fixtures.setBlock(c, x, G, z + 2, "hbm:creative_generator");
        Fixtures.setBlock(c, x, G, z + 1, "hbm:red_wire_coated");
        Fixtures.setBlock(c, x, G, z, "hbm:charger[facing=north]");
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("give @s hbm:battery_lead 1");
        c.waitTicks(3);
        c.hotbar(1);
        JsonObject before = c.query("hbm_battery", new JsonObject());
        System.out.println("[FinalEnergyBedrock] battery before: " + before);
        c.teleport(x + 0.5, G, z + 0.3);
        c.waitTicks(2);
        c.lookAt(x + 0.5, G + 0.45, z + 0.95);
        c.waitTicks(50);
        c.teleport(x + 0.5, G, z + 0.3);
        c.lookAt(x + 0.5, G + 0.45, z + 0.95);
        c.waitTicks(2);
        c.screenshot("30-charger-clamps-out");
        JsonObject after = c.query("hbm_battery", new JsonObject());
        System.out.println("[FinalEnergyBedrock] battery after: " + after);
        Expect.that(after.has("charge") && after.get("charge").getAsLong() > (before.has("charge") ? before.get("charge").getAsLong() : 0),
                "the charger charged the held battery: " + before + " -> " + after);
        c.teleport(x + 2.5, G, z - 1.5);
        c.waitTicks(40);
        worldShot(c, "31-charger-retracted", x + 2.2, G + 1.2, z - 1.2, x + 0.5, G + 0.5, z + 0.8);
        c.cmd("clear");
        c.cmd("gamemode creative");
    }

    // --- 5. acidizer then centrifuge ---------------------------------------------------------------------------

    private static void chain(Client c) {
        int ax = 12, az = 0, cx = 12, cz = 10;
        placeMachine(c, "hbm:machine_crystallizer", ax, az);
        int[] acid = findCore(c, ax, G, az, "TileEntityMachineCrystallizer");
        Expect.that(acid != null, "the ore acidizer was placed");
        // 1.7 WATER is fluid id 1
        c.cmd("data merge block " + acid[0] + " " + acid[1] + " " + acid[2] + " {power:1000000L,tankNew:8000,tankNew_type:1}");
        placeMachine(c, "hbm:machine_centrifuge", cx, cz);
        int[] cent = findCore(c, cx, G, cz, "TileEntityMachineCentrifuge");
        Expect.that(cent != null, "the centrifuge was placed");
        c.cmd("data merge block " + cent[0] + " " + cent[1] + " " + cent[2] + " {power:100000L}");

        c.cmd("clear");
        c.cmd("give @s hbm:bedrock_ore_base_light 1");
        c.waitTicks(3);
        c.teleport(ax + 0.5, G, az - 3.5);
        c.waitTicks(2);
        Fixtures.interact(c, acid[0], acid[1], acid[2]);
        c.waitScreen("GUICrystallizer", 5000);
        Expect.screenStaysOpen(c, "GUICrystallizer", 5);
        clickFromInventory(c, "hbm:bedrock_ore_base_light", 0);
        c.screenshot("40-acidizer-loaded");
        c.waitTicks(140);
        c.screenshot("41-acidizer-done");
        Expect.slotHolds(c, 2, "hbm:bedrock_ore_base_washed_light", 1);
        takeToInventory(c, 2);
        close(c);

        c.teleport(cx + 0.5, G, cz - 2.5);
        c.waitTicks(2);
        Fixtures.interact(c, cent[0], cent[1], cent[2]);
        c.waitScreen("GUICentrifuge", 5000);
        Expect.screenStaysOpen(c, "GUICentrifuge", 5);
        clickFromInventory(c, "hbm:bedrock_ore_base_washed_light", 0);
        c.screenshot("42-centrifuge-loaded");
        c.waitTicks(240);
        c.screenshot("43-centrifuge-done");
        Expect.slotHolds(c, 2, "hbm:bedrock_ore_primary_light", 1);
        Expect.slotHolds(c, 3, "hbm:bedrock_ore_primary_light", 1);
        Expect.slotHolds(c, 4, "minecraft:gravel", 1);
        close(c);
    }

    // --- helpers -----------------------------------------------------------------------------------------------

    /** Places {@code id} on top of the platform stone at (x, Y, z) by a real right-click from the south-west. */
    private static void placeByHand(Client c, String id, int x, int z) {
        Fixtures.giveOnly(c, id, 1);
        c.teleport(x + 0.5, G, z - 2.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, Y + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(3);
        Expect.block(c, x, G, z, id);
    }

    /** As {@link #placeByHand}, standing to the west and facing +X. */
    private static void placeFromWest(Client c, String id, int x, int z) {
        Fixtures.giveOnly(c, id, 1);
        c.teleport(x - 2.0, G, z + 0.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, Y + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(3);
        Expect.block(c, x, G, z, id);
    }

    /** A multiblock placed by hand from four blocks north, clear of its footprint. */
    private static void placeMachine(Client c, String id, int x, int z) {
        Fixtures.giveOnly(c, id, 1);
        c.teleport(x + 0.5, G, z - 4.0);
        c.waitTicks(2);
        c.lookAt(x + 0.5, Y + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
    }

    private static int[] findCore(Client c, int x, int y, int z, String cls) {
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    JsonObject be = c.blockEntity(x + dx, y + dy, z + dz);
                    if (be.has("block_entity") && !be.get("block_entity").isJsonNull()
                            && be.get("block_entity").getAsString().endsWith(cls)) {
                        return new int[]{x + dx, y + dy, z + dz};
                    }
                }
            }
        }
        return null;
    }

    private static void hover(Client c, String id) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(id)) {
                c.mouseMove(s.get("center_x").getAsInt(), s.get("center_y").getAsInt());
                c.waitTicks(4);
                return;
            }
        }
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(2);
    }

    private static void clickFromInventory(Client c, String id, int machineSlot) {
        JsonObject from = null;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean()
                    && s.get("id").getAsString().equals(id)) from = s;
        }
        Expect.that(from != null, "a player slot holds " + id);
        c.clickSlot(from.get("index").getAsInt());
        c.waitTicks(2);
        c.clickSlot(machineSlot);
        c.waitTicks(3);
    }

    private static void takeToInventory(Client c, int machineSlot) {
        c.clickSlot(machineSlot);
        c.waitTicks(2);
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.get("empty").getAsBoolean()) {
                c.clickSlot(s.get("index").getAsInt());
                c.waitTicks(2);
                return;
            }
        }
        throw new IllegalStateException("no empty player slot");
    }

    private static String nbt(Client c, int x, int y, int z) {
        return c.blockEntity(x, y, z).get("nbt").getAsString().replaceAll("\\s+", "");
    }

    private static long longOf(String nbt, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?<![A-Za-z0-9_])" + key + ":(-?[0-9]+)").matcher(nbt);
        Expect.that(m.find(), key + " present in " + nbt);
        return Long.parseLong(m.group(1));
    }

    private static void worldShot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.teleport(x, y, z);
        c.lookAt(tx, ty, tz);
        c.waitTicks(8);
        c.teleport(x, y, z);
        c.lookAt(tx, ty, tz);
        c.waitTicks(1);
        c.screenshot(name);
    }
}
