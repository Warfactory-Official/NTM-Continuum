import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The final consumables wave (ModFinalConsumableItems): every id given and shown in the survival inventory, then
 * the items used for real - foods and drinks eaten with the use key (hunger and effects read back), a disc played
 * in a jukebox, a potato battery drained into an electric furnace through its GUI, cloth dropped into water turning
 * damp, industrial fertilizer growing wheat, the muffler fitted to the furnace, matches lighting fire and balefire,
 * a cigarette smoked and the canteen's cooldown bar.
 */
public final class FinalConsumables implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    static final String[] IDS = {
            "apple_lead", "apple_lead_ingot", "apple_lead_block", "apple_schrabidium", "apple_schrabidium_ingot",
            "apple_schrabidium_block", "apple_euphemium", "glowing_stew", "balefire_scrambled", "balefire_and_ham",
            "bomb_waffle", "schnitzel_vegan", "cotton_candy", "ingot_smore", "mucho_mango", "pancake", "tem_flakes",
            "tem_flakes_normal", "tem_flakes_expensive", "canned_conserve_beef", "canned_conserve_tuna",
            "canned_conserve_mystery", "canned_conserve_pashtet", "canned_conserve_cheese", "canned_conserve_slime",
            "canned_conserve_milk", "canned_conserve_ass", "canned_conserve_pizza", "canned_conserve_tube",
            "canned_conserve_tomato", "canned_conserve_asbestos", "canned_conserve_bhole",
            "canned_conserve_hotdogs", "canned_conserve_leftovers", "canned_conserve_yogurt",
            "canned_conserve_stew", "canned_conserve_chinese", "canned_conserve_oil", "canned_conserve_fist",
            "canned_conserve_spam", "canned_conserve_fried", "canned_conserve_napalm", "canned_conserve_diesel",
            "canned_conserve_kerosene", "canned_conserve_recursion", "canned_conserve_bark", "canteen_vodka",
            "chocolate_milk", "cigarette", "crackpipe", "flask_infusion_shield", "bdcl", "peas", "matchstick",
            "balefire_and_steel", "gun_kit_1", "gun_kit_2", "record_lc", "record_ss", "record_vc", "record_glass",
            "battery_potato", "battery_potatos", "cube_power", "fusion_core", "rag", "powder_fertilizer",
            "ingot_electronium", "laser_crystal_dnt", "chemistry_set", "chemistry_set_boron",
            "parts_legendary_tier1", "parts_legendary_tier2", "parts_legendary_tier3", "briquette_coal",
            "briquette_lignite", "briquette_wood", "item_secret_canister", "item_secret_controller",
            "item_secret_selenium_steel", "item_secret_aberrator", "item_secret_folly",
            "circuit_star_piece_board_blank", "circuit_star_piece_board_transistor",
            "circuit_star_piece_board_converter", "circuit_star_piece_bridge_north",
            "circuit_star_piece_bridge_south", "circuit_star_piece_bridge_io", "circuit_star_piece_bridge_bus",
            "circuit_star_piece_bridge_chipset", "circuit_star_piece_bridge_cmos", "circuit_star_piece_bridge_bios",
            "circuit_star_piece_cpu_register", "circuit_star_piece_cpu_clock", "circuit_star_piece_cpu_logic",
            "circuit_star_piece_cpu_cache", "circuit_star_piece_cpu_ext", "circuit_star_piece_cpu_socket",
            "circuit_star_piece_mem_socket", "circuit_star_piece_mem_16k_a", "circuit_star_piece_mem_16k_b",
            "circuit_star_piece_mem_16k_c", "circuit_star_piece_mem_16k_d", "circuit_star_piece_card_board",
            "circuit_star_piece_card_processor", "circuit_star_component_chipset", "circuit_star_component_cpu",
            "circuit_star_component_ram", "circuit_star_component_card", "page_of_page1", "page_of_page2",
            "page_of_page3", "page_of_page4", "page_of_page5", "page_of_page6", "page_of_page7", "page_of_page8",
            "stamp_book_printing1", "stamp_book_printing2", "stamp_book_printing3", "stamp_book_printing4",
            "stamp_book_printing5", "stamp_book_printing6", "stamp_book_printing7", "stamp_book_printing8",
            "upgrade_5g", "upgrade_muffler"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 16);
        c.cmd("gamemode survival");
        c.cmd("difficulty easy"); // peaceful refills the hunger bar, and the foods need a hungry player
        c.cmd("effect give @s minecraft:resistance 900 4 true");
        c.waitTicks(2);

        inventoryPages(c);
        food(c);
        drinks(c);
        record(c);
        battery(c);
        rag(c);
        fertilizer(c);
        muffler(c);
        matches(c);

        c.cmd("gamemode creative");
        c.cmd("difficulty peaceful");
        c.cmd("kill @e[type=!player]");
        c.waitTicks(5);
        List<String> bad = c.logSinceMark(
                "/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Unable to play unknown soundEvent|missing texture");
        Expect.that(bad.isEmpty(), "log clean since the mark: " + bad);
    }

    // ------------------------------------------------------------------ inventory

    private void inventoryPages(Client c) {
        for (int page = 0; page * 36 < IDS.length; page++) {
            c.cmd("clear");
            c.waitTicks(2);
            List<String> batch = new ArrayList<>();
            for (int i = page * 36; i < Math.min(IDS.length, (page + 1) * 36); i++) {
                c.cmd("give @s hbm:" + IDS[i] + " 1");
                batch.add("hbm:" + IDS[i]);
            }
            c.waitTicks(5);
            for (String id : batch) Expect.that(count(c, id) == 1, id + " was given and is in the inventory");
            c.key("e");
            c.waitTicks(10);
            c.screenshot("0" + page + "-inventory-page-" + page);
            c.key("escape");
            c.waitTicks(3);
        }
        c.cmd("clear");
        c.waitTicks(2);
    }

    // ------------------------------------------------------------------ food

    private static JsonObject player(Client c) {
        return c.player();
    }

    private static boolean hasEffect(Client c, String name) {
        for (JsonElement e : player(c).getAsJsonArray("effects")) if (e.getAsString().contains(name)) return true;
        return false;
    }

    private static int foodLevel(Client c) {
        return player(c).get("food").getAsInt();
    }

    /** Hold the item in slot 1 and keep the use key down long enough to finish eating. */
    private static void eat(Client c, String id, int ticks) {
        Fixtures.giveOnly(c, "hbm:" + id, 1);
        c.look(0, 60);
        c.waitTicks(2);
        c.use(ticks);
        c.waitTicks(5);
    }

    private void starve(Client c) {
        c.cmd("effect clear @s minecraft:saturation");
        c.cmd("effect give @s minecraft:hunger 5 255 true");
        c.waitTicks(110);
        c.cmd("effect clear @s minecraft:hunger");
        c.waitTicks(2);
    }

    private void food(Client c) {
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        starve(c);
        int before = foodLevel(c);
        Expect.that(before <= 12, "the player is hungry before eating, food " + before);
        eat(c, "canned_conserve_beef", 45);
        int after = foodLevel(c);
        Expect.that(after == Math.min(20, before + 8), "canned beef feeds 8: " + before + " -> " + after);
        Expect.that(count(c, "hbm:can_key") == 1, "the can hands back a winding key");
        Expect.that(count(c, "hbm:canned_conserve_beef") == 0, "the can was eaten");
        c.screenshot("10-after-canned-beef");

        // always edible (full or not): the schrabidium apple's regeneration, resistance and fire resistance
        c.cmd("effect clear @s");
        eat(c, "apple_schrabidium", 45);
        Expect.that(hasEffect(c, "regeneration") && hasEffect(c, "fire_resistance"),
                "the schrabidium apple applies its effects: " + player(c).get("effects"));
        eat(c, "apple_lead", 45);
        Expect.that(hasEffect(c, "lead"), "the lead apple poisons with lead: " + player(c).get("effects"));
        c.cmd("effect clear @s");
        eat(c, "cotton_candy", 45);
        Expect.that(hasEffect(c, "poison") && hasEffect(c, "wither") && hasEffect(c, "weakness"),
                "cotton candy: poison, wither, weakness, speed, resistance: " + player(c).get("effects"));
        c.screenshot("11-cotton-candy-effects");
        c.cmd("effect clear @s");
        c.cmd("effect give @s minecraft:resistance 900 4 true");

        // the glowing stew hands back its bowl
        starve(c);
        eat(c, "glowing_stew", 45);
        Expect.that(count(c, "minecraft:bowl") == 1 && count(c, "hbm:glowing_stew") == 0, "the stew leaves a bowl");

        // the pancake refuses anyone without the lunar set
        Fixtures.giveOnly(c, "hbm:pancake", 1);
        c.use(45);
        c.waitTicks(5);
        Expect.that(count(c, "hbm:pancake") == 1, "the pancake cannot be eaten without the BJ set");
        Expect.that(!c.logSinceMark("Your teeth are too soft to eat this").isEmpty(), "the teeth message appears in chat");
        c.screenshot("12-pancake-teeth");

        eat(c, "tem_flakes", 45);
        Expect.that(count(c, "hbm:tem_flakes") == 0, "tem flakes are always edible");
    }

    private void drinks(Client c) {
        c.cmd("effect clear @s");
        c.cmd("effect give @s minecraft:resistance 900 4 true");
        eat(c, "canteen_vodka", 20);
        Expect.that(hasEffect(c, "nausea") || hasEffect(c, "confusion"), "the flask nauseates: " + player(c).get("effects"));
        Expect.that(hasEffect(c, "strength"), "the flask gives strength III: " + player(c).get("effects"));
        JsonObject hand = player(c).getAsJsonObject("main_hand");
        Expect.that(hand.get("damage").getAsInt() > 150, "the canteen's bar is its 3-minute cooldown: " + hand);
        c.screenshot("13-canteen-cooldown");
        c.use(20);
        c.waitTicks(3);
        Expect.that(player(c).getAsJsonObject("main_hand").get("damage").getAsInt() > 150,
                "the canteen cannot be drunk again while cooling down");

        c.cmd("effect clear @s");
        c.cmd("effect give @s minecraft:resistance 900 4 true");
        Fixtures.giveOnly(c, "hbm:cigarette", 16);
        c.use(40);
        c.waitTicks(5);
        Expect.that(count(c, "hbm:cigarette") == 15, "one cigarette was smoked, " + count(c, "hbm:cigarette") + " left");

        eat(c, "bdcl", 45);
        Expect.that(count(c, "hbm:bdcl") == 0, "the BDCL was drunk");
        eat(c, "flask_infusion_shield", 45);
        Expect.that(count(c, "hbm:flask_infusion_shield") == 0, "the shield infusion was drunk");
        eat(c, "mucho_mango", 210);
        Expect.that(count(c, "hbm:mucho_mango") == 0 && hasEffect(c, "speed"), "Mucho Mango drunk over 200 ticks gives speed");
        Fixtures.giveOnly(c, "hbm:rag", 1);
        c.use(2);
        c.waitTicks(3);
        Expect.that(count(c, "hbm:rag_piss") == 1 && count(c, "hbm:rag") == 0, "right-clicking the cloth soils it");
        c.cmd("effect clear @s");
        c.cmd("effect give @s minecraft:resistance 900 4 true");
    }

    // ------------------------------------------------------------------ record

    private void record(Client c) {
        int jx = X + 3, jz = Z + 3;
        Fixtures.setBlock(c, jx, Y + 1, jz, "minecraft:jukebox");
        Fixtures.giveOnly(c, "hbm:record_lc", 1);
        c.teleport(jx + 0.5, Y + 1, jz - 1.5);
        c.waitTicks(2);
        c.lookAt(jx + 0.5, Y + 1.5, jz + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(10);
        String block = c.block(jx, Y + 1, jz).toString();
        Expect.that(block.contains("has_record=true") || block.contains("\"has_record\":\"true\""),
                "the jukebox holds the disc: " + block);
        String be = nbt(c, jx, Y + 1, jz);
        Expect.that(be.contains("record_lc"), "the jukebox block entity holds hbm:record_lc: " + be);
        Expect.that(count(c, "hbm:record_lc") == 0, "the disc left the hand");
        c.screenshot("20-jukebox-record-lc");
        Fixtures.setBlock(c, jx, Y + 1, jz, "minecraft:air");
    }

    // ------------------------------------------------------------------ battery

    private static final int FX = X - 4, FZ = Z + 4;

    private void battery(Client c) {
        c.cmd("gamemode creative");
        Fixtures.placeOnTop(c, "hbm:machine_electric_furnace_off", FX, Y, FZ);
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("give @s hbm:battery_potato 1");
        c.waitTicks(3);
        c.teleport(FX + 0.5, Y + 1, FZ - 2.5);
        c.waitTicks(2);
        Fixtures.interact(c, FX, Y + 1, FZ);
        JsonObject screen = c.waitScreen("", 5000);
        Expect.that(screen.get("open").getAsBoolean(), "the electric furnace GUI opened by a right-click: " + screen);
        clickFromInventory(c, "hbm:battery_potato", 0);
        c.waitTicks(20);
        c.screenshot("30-electric-furnace-potato-battery");
        c.key("escape");
        c.waitTicks(3);
        String nbt = nbt(c, FX, Y + 1, FZ);
        Expect.that(nbt.contains("battery_potato"), "the potato battery sits in the furnace: " + nbt);
        Expect.that(nbt.contains("power:1000L"), "the furnace drew the potato battery's whole 1000 HE: " + nbt);
        Expect.that(nbt.contains("battery_charge\":0L"), "the potato battery is drained to 0: " + nbt);
        Expect.that(nbt.contains("battery_charge"), "the battery's charge was written as it drained: " + nbt);
    }

    private static void clickFromInventory(Client c, String id, int machineSlot) {
        JsonObject slot = null;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) slot = s;
        }
        Expect.that(slot != null, "inventory slot holding " + id);
        c.clickSlot(slot.get("index").getAsInt());
        c.waitTicks(1);
        c.clickSlot(machineSlot);
        c.waitTicks(2);
    }

    // ------------------------------------------------------------------ rag

    private void rag(Client c) {
        int wx = X + 6, wz = Z - 6;
        Fixtures.setBlock(c, wx, Y + 1, wz, "minecraft:water");
        Fixtures.giveOnly(c, "hbm:rag", 3);
        c.teleport(wx + 0.5, Y + 1, wz + 0.5); // standing in the water: the dropped cloth falls into it
        c.waitTicks(3);
        c.look(0, 90);
        c.key("key.drop");
        c.waitTicks(15); // well inside the 40-tick pickup delay, so the player cannot take it back
        boolean damp = false;
        for (JsonElement e : c.entities(8)) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals("minecraft:item") && o.get("name").getAsString().contains("Damp Cloth")) damp = true;
        }
        Expect.that(damp, "the cloth dropped into water became damp cloth: " + c.entities(8));
        c.look(0, 30);
        c.waitTicks(2);
        c.screenshot("40-rag-in-water-damp");
        c.cmd("kill @e[type=minecraft:item]");
        Fixtures.setBlock(c, wx, Y + 1, wz, "minecraft:air");
    }

    // ------------------------------------------------------------------ fertilizer

    private void fertilizer(Client c) {
        int gx = X - 6, gz = Z - 6;
        for (int dx = -1; dx <= 1; dx++) {
            Fixtures.setBlock(c, gx + dx, Y, gz, "minecraft:farmland");
            Fixtures.setBlock(c, gx + dx, Y + 1, gz, "minecraft:wheat[age=0]");
        }
        Fixtures.giveOnly(c, "hbm:powder_fertilizer", 4);
        c.teleport(gx + 0.5, Y + 1, gz - 2.5);
        c.waitTicks(2);
        c.lookAt(gx + 0.5, Y + 1.1, gz + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
        String crop = c.block(gx, Y + 1, gz).toString();
        // 1.7 forces one bone-meal growth on the clicked crop: +2..5 stages, never a fail roll
        int age = Integer.parseInt(c.block(gx, Y + 1, gz).getAsJsonObject("properties").get("age").getAsString());
        Expect.that(age >= 2, "the clicked wheat was forced to grow: " + crop);
        Expect.that(count(c, "hbm:powder_fertilizer") == 3, "one fertilizer was spent");
        c.screenshot("50-fertilizer-wheat");
    }

    // ------------------------------------------------------------------ muffler

    private void muffler(Client c) {
        String before = nbt(c, FX, Y + 1, FZ);
        Expect.that(before.contains("muffled:0b") || before.contains("muffled:false"), "the furnace starts loud: " + before);
        Fixtures.giveOnly(c, "hbm:upgrade_muffler", 2);
        c.teleport(FX + 0.5, Y + 1, FZ - 2.5);
        c.waitTicks(2);
        c.lookAt(FX + 0.5, Y + 1.5, FZ + 0.5);
        c.waitTicks(1);
        // a plain right-click opens the machine; as in 1.7 the item is applied while sneaking
        c.keyDown("key.sneak");
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(5);
        String after = nbt(c, FX, Y + 1, FZ);
        Expect.that(after.contains("muffled:1b") || after.contains("muffled:true"), "the muffler muffled the furnace: " + after);
        Expect.that(count(c, "hbm:upgrade_muffler") == 1, "one muffler was fitted");
        c.use(2);
        c.waitTicks(5);
        c.keyUp("key.sneak");
        c.waitTicks(2);
        Expect.that(count(c, "hbm:upgrade_muffler") == 1, "a muffled machine takes no second muffler");
        c.screenshot("55-muffled-furnace");
    }

    // ------------------------------------------------------------------ matches

    private void matches(Client c) {
        int mx = X + 6, mz = Z + 6;
        Fixtures.giveOnly(c, "hbm:matchstick", 2);
        c.teleport(mx + 0.5, Y + 1, mz - 2.5);
        c.waitTicks(2);
        c.lookAt(mx + 0.5, Y + 0.98, mz + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(3);
        Expect.that(c.block(mx, Y + 1, mz).toString().contains("minecraft:fire"), "the matchstick lit a fire");
        Expect.that(count(c, "hbm:matchstick") == 1, "one matchstick was spent");
        c.screenshot("60-matchstick-fire");
        Fixtures.setBlock(c, mx, Y + 1, mz, "minecraft:air");

        int bx = X + 9;
        Fixtures.giveOnly(c, "hbm:balefire_and_steel", 1);
        c.teleport(bx + 0.5, Y + 1, mz - 2.5);
        c.waitTicks(2);
        c.lookAt(bx + 0.5, Y + 0.98, mz + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(3);
        Expect.that(c.block(bx, Y + 1, mz).toString().contains("hbm:balefire"), "balefire and steel placed balefire");
        Expect.that(player(c).getAsJsonObject("main_hand").get("damage").getAsInt() == 1, "balefire and steel wore by one");
        c.screenshot("61-balefire-and-steel");
        Fixtures.setBlock(c, bx, Y + 1, mz, "minecraft:air");
        c.cmd("clear");
    }

    /** Block-entity SNBT with all whitespace removed, so checks do not depend on the pretty printer. */
    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") ? be.get("nbt").getAsString().replaceAll("\\s", "") : be.toString();
    }

    private static int count(Client c, String itemId) {
        int count = 0;
        for (var e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.has("id") && itemId.equals(s.get("id").getAsString())) count += s.get("count").getAsInt();
        }
        return count;
    }
}
