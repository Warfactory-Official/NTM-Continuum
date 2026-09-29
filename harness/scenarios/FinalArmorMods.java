import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The armor-module family (ModArmorModItems + insert_era) and the PaA hazmat / Liquidator / Mask of
 * Infamy armor: every id given and shown in the inventory; the armor modification table opened with a
 * real right-click and modules installed by real clicks into a chestplate, helmet, leggings and boots,
 * with the table and the Shift-hover module listing captured; the installed effects checked by query
 * (heart container max health, servo haste/damage, iron-cladding knockback, Life Extender shield, spider
 * milk curing poison, slime pads fall damage, scrumpy revive via /damage, night vision on a full T-51b
 * set); and the three suits, the M65 attachment and the back tesla captured in third person.
 */
public final class FinalArmorMods implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;
    /** The armor table sits on the platform two blocks in front of the spawn. */
    private static final int TX = X;
    private static final int TZ = Z + 2;

    static final String[] IDS = {"attachment_mask", "attachment_mask_mono", "australium_iii", "back_tesla",
            "ballistic_gauntlet", "bandaid", "bathwater", "bathwater_mk2", "black_diamond", "heart_piece",
            "heart_container", "heart_booster", "heart_fab", "card_aos", "card_qos", "cladding_iron",
            "cladding_obsidian", "gas_tester", "lodestone", "horseshoe_magnet", "industrial_magnet", "injector_5htp",
            "injector_knife", "ink", "insert_era", "medal_liquidator", "meteor_charm", "protection_charm",
            "morning_glory", "night_vision", "neutrino_lens", "pads_rubber", "pads_slime", "pads_static",
            "servo_set", "servo_set_desh", "shackles", "wd40", "scrumpy", "wild_p", "serum", "spider_milk",
            "hazmat_paa_helmet", "hazmat_paa_plate", "hazmat_paa_legs", "hazmat_paa_boots", "liquidator_helmet",
            "liquidator_plate", "liquidator_legs", "liquidator_boots", "mask_of_infamy"};

    /** Table menu: 0..8 module slots, 9 the armor being modified, 10..13 the worn head..feet slots. */
    private static final int ARMOR = 9;
    private static final int HEAD = 10;
    private static final int CHEST = 11;
    private static final int LEGS = 12;
    private static final int FEET = 13;

    private boolean legacy;
    private double chestMaxShield;

    @Override
    public void run(Client c) throws Exception {
        legacy = c.node().startsWith("1.20");
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 10);
        c.cmd("gamemode survival");
        // Effects outlive a scenario in this world (another scenario leaves Resistance V, which makes the
        // player immune to the revive test's /damage); start clean.
        c.cmd("effect clear @s");
        c.cmd("effect give @s minecraft:saturation 600 4 true");
        c.waitTicks(2);

        inventoryPages(c);
        tables(c);
        effects(c);
        nightVision(c);
        thirdPerson(c);

        c.cmd("item replace entity @s armor.head with minecraft:air");
        c.cmd("item replace entity @s armor.chest with minecraft:air");
        c.cmd("item replace entity @s armor.legs with minecraft:air");
        c.cmd("item replace entity @s armor.feet with minecraft:air");
        c.cmd("effect clear @s");
        c.cmd("gamemode creative");
        c.cmd("kill @e[type=!player]");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Unable to play unknown soundEvent|Exception");
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
            // Hover the first item of the page for its module tooltip.
            JsonObject screen = c.screen();
            for (JsonElement e : screen.getAsJsonArray("slots")) {
                JsonObject s = e.getAsJsonObject();
                if (!s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(batch.get(0))) {
                    c.mouseMove(s.get("center_x").getAsDouble(), s.get("center_y").getAsDouble());
                    break;
                }
            }
            c.waitTicks(5);
            c.screenshot("0" + page + "-inventory-page-" + page + "-tooltip");
            c.key("escape");
            c.waitTicks(3);
        }
        c.cmd("clear");
        c.waitTicks(2);
    }

    // ------------------------------------------------------------------ armor table

    private void tables(Client c) {
        Fixtures.placeOnTop(c, "hbm:machine_armor_table", TX, Y, TZ);
        Expect.block(c, TX, Y + 1, TZ, "hbm:machine_armor_table");
        c.cmd("clear");
        for (String id : new String[]{"hbm:steel_plate", "hbm:servo_set", "hbm:heart_container", "hbm:australium_iii",
                "hbm:cladding_iron", "hbm:back_tesla", "hbm:steel_helmet", "hbm:attachment_mask",
                "hbm:medal_liquidator", "hbm:spider_milk", "hbm:cladding_obsidian", "hbm:steel_legs", "hbm:scrumpy",
                "hbm:hazmat_boots", "hbm:pads_slime"}) {
            c.cmd("give @s " + id + " 1");
        }
        c.waitTicks(5);
        openTable(c);

        // Chestplate: servos, special (heart), insert (life extender), cladding (iron), chest-only (tesla).
        moveInto(c, "hbm:steel_plate", ARMOR);
        moveInto(c, "hbm:servo_set", 4);
        moveInto(c, "hbm:heart_container", 7);
        moveInto(c, "hbm:australium_iii", 6);
        moveInto(c, "hbm:cladding_iron", 5);
        moveInto(c, "hbm:back_tesla", 1);
        c.waitTicks(5);
        c.screenshot("10-table-chestplate");
        hover(c, ARMOR);
        c.screenshot("11-table-chestplate-tooltip");
        moveSlot(c, ARMOR, CHEST);

        // Helmet: M65 attachment (helmet-only), medal (chest-only is allowed on helmets), milk, obsidian skin.
        moveInto(c, "hbm:steel_helmet", ARMOR);
        moveInto(c, "hbm:attachment_mask", 0);
        moveInto(c, "hbm:medal_liquidator", 1);
        moveInto(c, "hbm:spider_milk", 7);
        moveInto(c, "hbm:cladding_obsidian", 5);
        c.waitTicks(5);
        c.screenshot("12-table-helmet");
        hover(c, ARMOR);
        c.screenshot("13-table-helmet-tooltip");
        moveSlot(c, ARMOR, HEAD);

        moveInto(c, "hbm:steel_legs", ARMOR);
        moveInto(c, "hbm:scrumpy", 7);
        c.waitTicks(3);
        moveSlot(c, ARMOR, LEGS);
        moveInto(c, "hbm:hazmat_boots", ARMOR);
        moveInto(c, "hbm:pads_slime", 3);
        c.waitTicks(3);
        c.screenshot("14-table-boots");
        moveSlot(c, ARMOR, FEET);
        c.key("escape");
        c.waitTicks(10);

        JsonObject armor = armor(c);
        expectMods(armor, "chest", "4:hbm:servo_set", "7:hbm:heart_container", "6:hbm:australium_iii",
                "5:hbm:cladding_iron", "1:hbm:back_tesla");
        expectMods(armor, "head", "0:hbm:attachment_mask", "1:hbm:medal_liquidator", "7:hbm:spider_milk",
                "5:hbm:cladding_obsidian");
        expectMods(armor, "legs", "7:hbm:scrumpy");
        expectMods(armor, "feet", "3:hbm:pads_slime");

        // Shift-hover the worn chestplate in the survival inventory: the "Mods:" listing.
        c.key("e");
        c.waitTicks(10);
        c.keyDown("lshift");
        c.waitTicks(2);
        hover(c, 6);
        c.screenshot("15-inventory-shift-hover-chestplate");
        hover(c, 5);
        c.screenshot("16-inventory-shift-hover-helmet");
        c.keyUp("lshift");
        c.key("escape");
        c.waitTicks(3);
    }

    private void openTable(Client c) {
        c.teleport(TX + 0.5, Y + 1, TZ - 1.5);
        c.waitTicks(2);
        Fixtures.interact(c, TX, Y + 1, TZ);
        c.waitScreen("Armor Modification Table", 5000);
        Expect.screenStaysOpen(c, "Armor Modification Table", 10);
    }

    // ------------------------------------------------------------------ effects

    private void effects(Client c) {
        c.waitTicks(20);
        JsonObject a = armor(c);
        double max = a.get("max_health").getAsDouble();
        Expect.that(Math.abs(max - 40.0) < 0.01, "heart container: max health 20 + 20 = 40, got " + max);
        double kb = a.get("knockback_resistance").getAsDouble();
        Expect.that(Math.abs(kb - 0.5) < 0.01, "iron cladding: knockback resistance 0.5, got " + kb);
        double dmg = a.get("attack_damage").getAsDouble();
        Expect.that(Math.abs(dmg - 1.5) < 0.01, "servo set on a chestplate: attack damage 1 * 1.5, got " + dmg);
        Expect.that(hasEffect(a, "haste", 0), "servo set on a chestplate: Haste I, got " + a.get("effects"));

        // The Life Extender's shield regenerates once 60 ticks pass without damage.
        c.waitTicks(100);
        a = armor(c);
        double shield = a.get("shield").getAsDouble();
        double maxShield = a.get("max_shield").getAsDouble();
        Expect.that(shield > 1 && shield <= maxShield + 0.0001, "life extender: shield regenerating, got " + shield
                + " of " + maxShield);
        chestMaxShield = maxShield;
        shieldSoaks(c);

        // Spider milk strips harmful effects every tick.
        c.cmd("effect give @s minecraft:poison 30 0");
        c.waitTicks(5);
        a = armor(c);
        Expect.that(!hasEffect(a, "poison", -1), "spider milk removed poison, got " + a.get("effects"));

        // Scrumpy: /damage far beyond max health revives with full health and Resistance C, spending it.
        c.cmd("damage @s 1000");
        c.waitTicks(10);
        a = armor(c);
        Expect.that(a.get("alive").getAsBoolean(), "scrumpy revive kept the player alive");
        Expect.that(Math.abs(a.get("health").getAsDouble() - a.get("max_health").getAsDouble()) < 0.01,
                "scrumpy revive restores full health, got " + a.get("health"));
        Expect.that(hasEffect(a, "resistance", 99), "scrumpy revive grants Resistance 100, got " + a.get("effects"));
        Expect.that(!mods(a, "legs").contains("7:hbm:scrumpy:1") && mods(a, "legs").isEmpty(),
                "the single scrumpy revive is spent, legs mods " + mods(a, "legs"));
        Expect.that(c.screenClass() == null || !c.screenClass().contains("Death"), "no death screen");
        c.screenshot("21-after-scrumpy-revive");
        c.cmd("effect clear @s"); // the revive's Resistance 100 would cancel the falls below

        // Slime pads take 75% off fall damage. They sit in hazmat boots, and every other piece comes off
        // first: the steel pieces carry thresholds of their own, and the Life Extender's shield (which
        // regenerates within seconds) would soak the hit.
        c.cmd("item replace entity @s armor.head with minecraft:air");
        c.cmd("item replace entity @s armor.chest with minecraft:air");
        c.cmd("item replace entity @s armor.legs with minecraft:air");
        c.waitTicks(5);
        JsonObject unarmored = armor(c);
        Expect.that(Math.abs(chestMaxShield - unarmored.get("max_shield").getAsDouble() - 25.0) < 0.001,
                "the Life Extender adds 25 maximum shield: " + chestMaxShield + " with, " + unarmored.get("max_shield")
                        + " without");
        // A flask-infused base shield (from other scenarios in this world) would soak the hit too; skip the
        // pad check's shield by spending it first.
        while (armor(c).get("shield").getAsDouble() > 0) {
            c.cmd("damage @s 10");
            c.waitTicks(11);
        }
        // 16 points of fall damage with the pads, then the same in plain hazmat boots. The rest of the
        // damage chain does not scale linearly here (a bare 16-point /damage costs 10-11 health), so the
        // check is relative: the pads must at least halve the cost of the same hit.
        double padded = hit(c, "minecraft:fall");
        c.screenshot("20-after-padded-fall");
        c.cmd("item replace entity @s armor.feet with hbm:hazmat_boots");
        double bare = hit(c, "minecraft:fall");
        Expect.that(bare > 4 && padded <= bare * 0.5,
                "slime pads: 16 fall damage cost " + padded + " padded vs " + bare + " unpadded");
        c.cmd("effect clear @s");
    }

    /** Heals, deals 16 points of {@code type} and returns the health lost. */
    private double hit(Client c, String type) {
        c.cmd("effect give @s minecraft:instant_health 1 10 true");
        c.waitTicks(15);
        double before = armor(c).get("health").getAsDouble();
        c.cmd("damage @s 16 " + type);
        c.waitTicks(15);
        return before - armor(c).get("health").getAsDouble();
    }

    /** A hit the shield fully soaks leaves health untouched and the shield lower. */
    private void shieldSoaks(Client c) {
        JsonObject before = armor(c);
        c.cmd("damage @s 10");
        c.waitTicks(3);
        JsonObject after = armor(c);
        Expect.that(after.get("health").getAsDouble() == before.get("health").getAsDouble()
                        && after.get("shield").getAsDouble() < before.get("shield").getAsDouble() - 9.9,
                "life extender shield soaks a 10-point hit: " + before.get("shield") + " -> " + after.get("shield")
                        + ", health " + before.get("health") + " -> " + after.get("health"));
    }

    private void nightVision(Client c) {
        c.cmd("clear");
        c.cmd("give @s hbm:t51_helmet 1");
        c.cmd("give @s hbm:night_vision 1");
        c.cmd("item replace entity @s armor.chest with hbm:t51_plate");
        c.cmd("item replace entity @s armor.legs with hbm:t51_legs");
        c.cmd("item replace entity @s armor.feet with hbm:t51_boots");
        c.waitTicks(5);
        openTable(c);
        moveInto(c, "hbm:t51_helmet", ARMOR);
        moveInto(c, "hbm:night_vision", 0);
        c.waitTicks(3);
        c.screenshot("30-table-t51-night-vision");
        // Straight from the table onto the head: the full T-51b set is now worn.
        moveSlot(c, ARMOR, HEAD);
        c.key("escape");
        c.waitTicks(20);
        JsonObject a = armor(c);
        expectMods(a, "head", "0:hbm:night_vision");
        Expect.that(hasEffect(a, "night_vision", 0), "night vision goggles on a full T-51b set grant Night Vision, got "
                + a.get("effects"));
        c.screenshot("31-t51-night-vision");
    }

    // ------------------------------------------------------------------ third person

    private void thirdPerson(Client c) {
        c.cmd("clear");
        c.cmd("effect clear @s");
        c.cmd("gamemode creative");
        c.teleport(X + 0.5, Y + 1, Z - 4.5);
        c.look(0, 10);
        c.waitTicks(5);

        // The modded steel set was replaced by the T-51b test; rebuild the render pieces through the table:
        // an M65 attachment on a helmet and the back tesla on a chestplate.
        c.cmd("item replace entity @s armor.head with minecraft:air");
        c.cmd("item replace entity @s armor.chest with minecraft:air");
        c.cmd("item replace entity @s armor.legs with minecraft:air");
        c.cmd("item replace entity @s armor.feet with minecraft:air");
        c.cmd("give @s hbm:steel_helmet 1");
        c.cmd("give @s hbm:attachment_mask 1");
        c.cmd("give @s hbm:steel_plate 1");
        c.cmd("give @s hbm:back_tesla 1");
        c.cmd("gamemode survival");
        c.waitTicks(5);
        openTable(c);
        moveInto(c, "hbm:steel_helmet", ARMOR);
        moveInto(c, "hbm:attachment_mask", 0);
        moveSlot(c, ARMOR, HEAD);
        moveInto(c, "hbm:steel_plate", ARMOR);
        moveInto(c, "hbm:back_tesla", 1);
        moveSlot(c, ARMOR, CHEST);
        c.key("escape");
        c.waitTicks(5);
        c.cmd("gamemode creative");
        c.teleport(X + 0.5, Y + 1, Z - 4.5);
        c.look(0, 10);
        c.waitTicks(5);

        c.key("f5"); // third person, behind
        c.waitTicks(10);
        c.screenshot("40-f5-back-tesla");
        c.key("f5"); // third person, front
        c.waitTicks(10);
        c.screenshot("41-f5-front-m65-attachment");

        wear(c, "hbm:hazmat_paa_helmet", "hbm:hazmat_paa_plate", "hbm:hazmat_paa_legs", "hbm:hazmat_paa_boots");
        c.screenshot("42-f5-front-hazmat-paa");
        wear(c, "hbm:liquidator_helmet", "hbm:liquidator_plate", "hbm:liquidator_legs", "hbm:liquidator_boots");
        c.screenshot("43-f5-front-liquidator");
        JsonObject a = armor(c);
        Expect.that(a.get("knockback_resistance").getAsDouble() >= 1.0,
                "liquidator suit: knockback resistance capped at 1, got " + a.get("knockback_resistance"));
        // The pre-existing yellow hazmat suit: below 1.21.2 its worn texture comes from the build layer's
        // equipment-asset -> models/armor/<material>_layer_N copy (it drew solid magenta without it).
        wear(c, "hbm:hazmat_helmet", "hbm:hazmat_plate", "hbm:hazmat_legs", "hbm:hazmat_boots");
        c.screenshot("43b-f5-front-hazmat-yellow");
        wear(c, "hbm:mask_of_infamy", "minecraft:air", "minecraft:air", "minecraft:air");
        c.screenshot("44-f5-front-mask-of-infamy");
        c.key("f5"); // back to first person
        c.waitTicks(5);
        wear(c, "hbm:liquidator_helmet", "minecraft:air", "minecraft:air", "minecraft:air");
        c.screenshot("45-first-person-liquidator-overlay");
    }

    private void wear(Client c, String head, String chest, String legs, String feet) {
        c.cmd("item replace entity @s armor.head with " + head);
        c.cmd("item replace entity @s armor.chest with " + chest);
        c.cmd("item replace entity @s armor.legs with " + legs);
        c.cmd("item replace entity @s armor.feet with " + feet);
        c.waitTicks(10);
    }

    // ------------------------------------------------------------------ helpers

    /** Picks up {@code itemId} from the player inventory part of the open menu and drops it into {@code target}. */
    private void moveInto(Client c, String itemId, int target) {
        int from = find(c, itemId);
        Expect.that(from >= 0, itemId + " is in the open menu's inventory");
        moveSlot(c, from, target);
        JsonObject slot = c.slot(target);
        Expect.that(slot != null && !slot.get("empty").getAsBoolean() && slot.get("id").getAsString().equals(itemId),
                itemId + " landed in menu slot " + target + ", got " + slot);
    }

    private void moveSlot(Client c, int from, int to) {
        c.clickSlot(from);
        c.waitTicks(2);
        c.clickSlot(to);
        c.waitTicks(3);
    }

    private int find(Client c, String itemId) {
        JsonObject screen = c.screen();
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.get("index").getAsInt() > FEET
                    && !s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(itemId)) {
                return s.get("index").getAsInt();
            }
        }
        // The survival inventory screen numbers its slots differently; accept any inventory slot there.
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.get("index").getAsInt() >= 9
                    && !s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(itemId)) {
                return s.get("index").getAsInt();
            }
        }
        return -1;
    }

    private void hover(Client c, int slotIndex) {
        JsonObject s = c.slot(slotIndex);
        c.mouseMove(s.get("center_x").getAsDouble(), s.get("center_y").getAsDouble());
        c.waitTicks(5);
    }

    private JsonObject armor(Client c) {
        return c.query("hbm_armor", new JsonObject());
    }

    private static List<String> mods(JsonObject armor, String slot) {
        List<String> out = new ArrayList<>();
        JsonArray mods = armor.getAsJsonObject(slot).getAsJsonArray("mods");
        for (JsonElement e : mods) out.add(e.getAsString());
        return out;
    }

    private static void expectMods(JsonObject armor, String slot, String... expected) {
        List<String> mods = mods(armor, slot);
        for (String want : expected) {
            Expect.that(mods.stream().anyMatch(m -> m.startsWith(want + ":")), slot + " holds " + want + ", got " + mods);
        }
    }

    /** Effect by description-id suffix and amplifier (-1 = any). */
    private static boolean hasEffect(JsonObject armor, String name, int amplifier) {
        for (JsonElement e : armor.getAsJsonArray("effects")) {
            String s = e.getAsString();
            int at = s.lastIndexOf('@');
            String id = s.substring(0, at);
            int amp = Integer.parseInt(s.substring(at + 1));
            if (id.endsWith("." + name) && (amplifier < 0 || amp == amplifier)) return true;
        }
        return false;
    }

    private static int count(Client c, String itemId) {
        JsonObject inventory = c.inventory();
        int n = 0;
        for (JsonElement e : inventory.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("id").getAsString().equals(itemId)) n += s.get("count").getAsInt();
        }
        return n;
    }
}
