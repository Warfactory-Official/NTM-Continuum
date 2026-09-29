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
 * The special melee / kit family (ModMeleeKitItems): every id given and shown in the inventory; each special
 * weapon hitting a NoAI iron golem with its effect checked on the integrated server (hbm_living: health, motion,
 * effects); the caber's blast; the desh screwdriver, desh hand drill and acetylene torch on their blocks; the Rod of
 * Discord's teleport; kits unpacked and their give-lists checked; the toolbox rotating the hotbar and storing and
 * retrieving through its GUI; the Book of Boxcars crafting the diamond gavel; Bobmazon buying 64 torches for 2 caps
 * and the drone delivering them; the missile loot crates; the Brittle Spade digging ntm_dirt; fishing dynamite in a
 * pool; the rigged star blaster cell smoking on the floor.
 */
public final class FinalMeleeKits implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    static final String[] IDS = {"big_sword", "schrabidium_hammer", "shimmer_sledge", "shimmer_axe", "wrench_flipped",
            "memespoon", "wood_gavel", "lead_gavel", "diamond_gavel", "weapon_pipe_lead", "reer_graar", "stopsign",
            "sopsign", "chernobylsign", "ullapool_caber", "wrench", "wrench_archineer", "screwdriver_desh",
            "hand_drill_desh", "acetylene_torch", "weaponized_starblaster_cell", "rod_of_discord", "mysteryshovel",
            "stick_dynamite_fishing", "book_of_", "stealth_boy", "nuke_starter_kit", "nuke_advanced_kit",
            "nuke_commercially_kit", "nuke_electric_kit", "gadget_kit", "boy_kit", "man_kit", "mike_kit", "tsar_kit",
            "multi_kit", "custom_kit", "fleija_kit", "prototype_kit", "missile_kit", "euphemium_kit", "solinium_kit",
            "hazmat_kit", "hazmat_red_kit", "hazmat_grey_kit", "kit_custom", "toolbox", "loot_10", "loot_15",
            "loot_misc", "bobmazon", "bobmazon_hidden"};

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 14);
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:resistance 900 4 true");
        c.cmd("effect give @s minecraft:saturation 900 4 true");
        c.cmd("gamerule doTileDrops true");
        c.waitTicks(2);

        inventoryPages(c);
        hits(c);
        caber(c);
        tools(c);
        discord(c);
        kits(c);
        toolbox(c);
        book(c);
        bobmazon(c);
        loot(c);
        spade(c);
        fishing(c);
        starblaster(c);

        c.cmd("gamemode creative");
        c.cmd("kill @e[type=!player]");
        c.waitTicks(5);
        // The hazmat suits the kits dress the player in have no legacy armor-layer textures on 1.20.1
        // (hbm:textures/models/armor/hazmat_layer_*.png): a pre-existing armor-family gap, reported, not this family's.
        List<String> bad = new ArrayList<>();
        for (String line : c.logSinceMark("/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Unable to play unknown soundEvent")) {
            if (!line.contains("textures/models/armor/hazmat_layer_")) bad.add(line);
        }
        Expect.that(bad.isEmpty(), "log clean since the mark (bar the known hazmat layer gap): " + bad);
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

    // ------------------------------------------------------------------ melee

    private JsonObject golem(Client c) {
        for (JsonElement e : c.query("hbm_living", new JsonObject()).getAsJsonArray("living")) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals("minecraft:iron_golem")) return o;
        }
        return null;
    }

    /** A fresh 100 HP NoAI iron golem three blocks in front of the player, then one full-cooldown hit. */
    private JsonObject hit(Client c, String weapon) {
        return hit(c, weapon, false);
    }

    /**
     * {@code free}: a golem with its AI on (a NoAI mob never applies its own motion, so a launch would not show)
     * and slowness 255 so it cannot walk off by itself.
     */
    private JsonObject hit(Client c, String weapon, boolean free) {
        c.cmd("kill @e[type=minecraft:iron_golem]");
        c.cmd("kill @e[type=minecraft:item]");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.cmd("summon minecraft:iron_golem " + (X + 0.5) + " " + (Y + 1) + " " + (Z + 3.5)
                + (free ? " {PersistenceRequired:1b}" : " {NoAI:1b,PersistenceRequired:1b}"));
        if (free) c.cmd("effect give @e[type=minecraft:iron_golem] minecraft:slowness 60 255 true");
        Fixtures.giveOnly(c, "hbm:" + weapon, 1);
        c.waitTicks(25); // attack cooldown
        c.lookAt(X + 0.5, Y + 2.2, Z + 3.5);
        c.waitTicks(2);
        c.attack(1);
        c.waitTicks(4);
        JsonObject g = golem(c);
        Expect.that(g != null || weapon.equals("schrabidium_hammer"), weapon + ": the golem is still queryable");
        return g;
    }

    private static double moved(JsonObject g) {
        double dx = g.get("x").getAsDouble() - (X + 0.5);
        double dz = g.get("z").getAsDouble() - (Z + 3.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static boolean hasEffect(JsonObject g, String name) {
        for (JsonElement e : g.getAsJsonArray("effects")) if (e.getAsString().contains(name)) return true;
        return false;
    }

    private void hits(Client c) {
        JsonObject g = hit(c, "diamond_gavel");
        double h = g.get("health").getAsDouble();
        // 8 (attack) + 100/3: the gavel takes a third of max health on every hit
        Expect.that(h < 62 && h > 54, "diamond gavel leaves ~58.7 HP, got " + h);
        c.screenshot("10-diamond-gavel-hit");

        g = hit(c, "lead_gavel");
        Expect.that(hasEffect(g, "lead"), "lead gavel applies lead poisoning, got " + g.get("effects"));

        g = hit(c, "shimmer_axe");
        h = g.get("health").getAsDouble();
        Expect.that(h < 36 && h > 33, "shimmer axe halves what the 31 damage left (34.5), got " + h);

        g = hit(c, "schrabidium_hammer");
        Expect.that(g == null || g.get("health").getAsDouble() <= 0, "schrabidium hammer one-hits the golem");
        c.screenshot("11-schrabidium-hammer-hit");

        g = hit(c, "shimmer_sledge", true);
        c.waitTicks(10);
        g = golem(c);
        Expect.that(g != null && moved(g) > 3, "shimmer sledge launches the golem 5x along the look, moved "
                + (g == null ? -1 : moved(g)));
        c.screenshot("12-shimmer-sledge-launch");

        g = hit(c, "wrench", true);
        c.waitTicks(10);
        g = golem(c);
        Expect.that(g != null && moved(g) > 0.2, "pipe wrench nudges the golem, moved " + (g == null ? -1 : moved(g)));

        for (String plain : new String[]{"big_sword", "wood_gavel", "memespoon", "stopsign", "sopsign", "chernobylsign",
                "wrench_flipped", "reer_graar", "weapon_pipe_lead", "wrench_archineer"}) {
            g = hit(c, plain);
            h = g.get("health").getAsDouble();
            Expect.that(h < 100, plain + " deals damage, golem at " + h);
        }
        c.screenshot("13-archineer-hit");
        c.cmd("kill @e[type=minecraft:iron_golem]");
    }

    /** The Ullapool caber blows up on the hit: the platform under the golem is gone. */
    private void caber(Client c) {
        int cx = X + 8;
        c.cmd("kill @e[type=minecraft:iron_golem]");
        c.teleport(cx + 0.5, Y + 1, Z + 0.5);
        c.cmd("summon minecraft:iron_golem " + (cx + 0.5) + " " + (Y + 1) + " " + (Z + 3.5) + " {NoAI:1b}");
        Fixtures.giveOnly(c, "hbm:ullapool_caber", 1);
        c.waitTicks(25);
        c.lookAt(cx + 0.5, Y + 2.2, Z + 3.5);
        c.waitTicks(2);
        c.attack(1);
        c.waitTicks(10);
        Expect.block(c, cx, Y, Z + 3, "minecraft:air");
        c.screenshot("14-caber-crater");
        JsonObject inv = c.inventory();
        int damage = -1;
        for (JsonElement e : inv.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if ("hbm:ullapool_caber".equals(s.get("id").getAsString())) damage = s.get("damage").getAsInt();
        }
        Expect.that(damage == 505, "the caber took 505 durability, took " + damage);
        c.cmd("kill @e[type=minecraft:iron_golem]");
        c.cmd("fill " + (cx - 8) + " " + (Y - 8) + " " + (Z - 6) + " " + (cx + 8) + " " + (Y - 1) + " " + (Z + 12) + " minecraft:stone");
        c.cmd("fill " + (cx - 8) + " " + Y + " " + (Z - 6) + " " + (cx + 8) + " " + Y + " " + (Z + 12) + " minecraft:smooth_stone");
        c.cmd("fill " + (cx - 8) + " " + (Y + 1) + " " + (Z - 6) + " " + (cx + 8) + " " + (Y + 8) + " " + (Z + 12) + " minecraft:air");
        c.cmd("kill @e[type=minecraft:item]");
        c.waitTicks(3);
    }

    // ------------------------------------------------------------------ tools

    private void useOn(Client c, String item, int x, int y, int z) {
        c.teleport(x + 2.5, Y + 1, z + 0.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, y + 0.5, z + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(5);
    }

    private void tools(Client c) {
        int tx = X - 6;
        // desh screwdriver: the fan turns round
        Fixtures.setBlock(c, tx, Y + 1, Z + 3, "hbm:fan");
        String before = c.block(tx, Y + 1, Z + 3).getAsJsonObject("properties").toString();
        Fixtures.giveOnly(c, "hbm:screwdriver_desh", 1);
        useOn(c, "screwdriver_desh", tx, Y + 1, Z + 3);
        String after = c.block(tx, Y + 1, Z + 3).getAsJsonObject("properties").toString();
        Expect.that(!before.equals(after), "desh screwdriver flips the fan: " + before + " -> " + after);
        c.screenshot("20-screwdriver-fan");

        // desh hand drill: graphite is drilled
        Fixtures.setBlock(c, tx, Y + 1, Z + 5, "hbm:block_graphite");
        Fixtures.giveOnly(c, "hbm:hand_drill_desh", 1);
        useOn(c, "hand_drill_desh", tx, Y + 1, Z + 5);
        Expect.block(c, tx, Y + 1, Z + 5, "hbm:block_graphite_drilled");
        c.screenshot("21-hand-drill-graphite");

        // acetylene torch: a raw fusion component welds into the BSCCO coil (a steel cast plate is used up)
        Fixtures.setBlock(c, tx, Y + 1, Z + 7, "hbm:fusion_component");
        Fixtures.giveOnly(c, "hbm:acetylene_torch", 1);
        Fixtures.give(c, "hbm:plate_cast_steel", 4);
        useOn(c, "acetylene_torch", tx, Y + 1, Z + 7);
        Expect.block(c, tx, Y + 1, Z + 7, "hbm:fusion_component_bscco_welded");
        c.screenshot("22-acetylene-weld");
        c.cmd("fill " + tx + " " + (Y + 1) + " " + (Z + 3) + " " + tx + " " + (Y + 1) + " " + (Z + 7) + " minecraft:air");
        c.cmd("kill @e[type=minecraft:item]");
    }

    /** Rod of Discord: aim at a wall ten blocks away, arrive in front of it. */
    private void discord(Client c) {
        Fixtures.giveOnly(c, "hbm:rod_of_discord", 1);
        c.cmd("fill " + (X - 1) + " " + (Y + 1) + " " + (Z + 11) + " " + (X + 1) + " " + (Y + 3) + " " + (Z + 11) + " minecraft:stone");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(3);
        c.lookAt(X + 0.5, Y + 2.0, Z + 11.0);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(5);
        double pz = c.player().get("z").getAsDouble();
        Expect.that(pz > Z + 8.5 && pz < Z + 11, "rod of discord moved the player in front of the wall, z " + pz);
        c.screenshot("30-rod-of-discord");
        c.cmd("fill " + (X - 1) + " " + (Y + 1) + " " + (Z + 11) + " " + (X + 1) + " " + (Y + 3) + " " + (Z + 11) + " minecraft:air");
    }

    // ------------------------------------------------------------------ kits

    private void openKit(Client c, String kit) {
        Fixtures.giveOnly(c, "hbm:" + kit, 1);
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, -80);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(5);
        Expect.that(count(c, "hbm:" + kit) == 0, kit + " is used up");
    }

    private void kits(Client c) {
        openKit(c, "boy_kit");
        for (String id : new String[]{"hbm:nuke_boy", "hbm:boy_shielding", "hbm:boy_target", "hbm:boy_bullet",
                "hbm:boy_propellant", "hbm:boy_igniter", "hbm:hazmat_helmet", "hbm:hazmat_plate", "hbm:hazmat_legs",
                "hbm:hazmat_boots"}) {
            Expect.that(count(c, id) == 1, "boy kit gave " + id);
        }
        c.key("e");
        c.waitTicks(10);
        c.screenshot("40-boy-kit-unpacked");
        c.key("escape");

        openKit(c, "hazmat_red_kit");
        Expect.that(count(c, "hbm:hazmat_helmet_red") == 1 && count(c, "hbm:hazmat_boots_red") == 1,
                "advanced hazmat kit dresses the red suit");

        openKit(c, "multi_kit");
        Expect.that(count(c, "minecraft:tnt") == 26 && count(c, "hbm:bomb_multi") == 6
                && count(c, "minecraft:gunpowder") == 2, "multi kit gives 26 TNT, 6 multi bombs, 2 gunpowder");

        openKit(c, "missile_kit");
        Expect.that(count(c, "hbm:launch_pad") == 1 && count(c, "hbm:missile_generic") == 1,
                "missile kit gives the pad and the missiles");
        c.key("e");
        c.waitTicks(10);
        c.screenshot("41-missile-kit-unpacked");
        c.key("escape");

        openKit(c, "nuke_starter_kit");
        Expect.that(count(c, "hbm:ingot_uranium") == 32 && count(c, "hbm:stamp_titanium_flat") == 3,
                "starter kit gives 32 uranium and 3 flat stamps");

        openKit(c, "stealth_boy");
        JsonArray effects = c.player().getAsJsonArray("effects");
        Expect.that(effects.toString().contains("invisibility"), "stealth device makes invisible: " + effects);
        c.cmd("effect clear @s minecraft:invisibility");
        c.cmd("clear");
    }

    // ------------------------------------------------------------------ toolbox

    private void toolbox(Client c) {
        Fixtures.giveOnly(c, "hbm:toolbox", 1);
        c.cmd("give @s minecraft:dirt 5");
        c.cmd("give @s minecraft:cobblestone 7");
        c.waitTicks(3);
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, -80);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(5);
        Expect.that(count(c, "minecraft:dirt") == 0 && count(c, "minecraft:cobblestone") == 0,
                "the toolbox swallowed the hotbar");

        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(2);
        c.keyUp("key.sneak");
        c.waitScreen("GUIToolBox", 5000);
        c.waitTicks(5);
        JsonObject slot0 = c.slot(0);
        c.screenshot("50-toolbox-gui");
        Expect.that(slot0.toString().contains("minecraft:dirt"), "toolbox slot 0 holds the dirt: " + slot0);
        // retrieve: pick up the dirt and put it into the player's first inventory row (menu slot 24)
        c.clickSlot(0);
        c.waitTicks(3);
        c.clickSlot(24);
        c.waitTicks(3);
        // store: put the cobblestone back? pick it from box slot 1 and drop it into box slot 23
        c.clickSlot(1);
        c.waitTicks(3);
        c.clickSlot(23);
        c.waitTicks(3);
        c.screenshot("51-toolbox-moved");
        Expect.that(c.slot(23).toString().contains("minecraft:cobblestone") && !c.slot(0).toString().contains("dirt"),
                "the cobblestone is stored in the last box slot and the dirt is out: " + c.slot(23));
        c.key("escape");
        c.waitTicks(5);
        Expect.that(count(c, "minecraft:dirt") == 5, "the dirt came out of the toolbox");
        c.cmd("clear");
    }

    // ------------------------------------------------------------------ book of boxcars

    private void book(Client c) {
        Fixtures.giveOnly(c, "hbm:book_of_", 1);
        c.cmd("give @s hbm:gravel_diamond 3");
        c.cmd("give @s hbm:lead_gavel 1");
        c.waitTicks(3);
        c.look(0, -80);
        c.use(2);
        c.waitScreen("GUIBook", 5000);
        c.waitTicks(5);
        // menu: 0 result, 1-4 grid, 5-31 inventory, 32-40 hotbar (book 32, gravel 33, gavel 34)
        c.clickSlot(33);
        c.waitTicks(2);
        c.clickSlot(1, 1);
        c.waitTicks(2);
        c.clickSlot(2, 1);
        c.waitTicks(2);
        c.clickSlot(3, 1);
        c.waitTicks(2);
        c.clickSlot(34);
        c.waitTicks(2);
        c.clickSlot(4);
        c.waitTicks(4);
        c.screenshot("60-book-diamond-gavel");
        Expect.that(c.slot(0).toString().contains("hbm:diamond_gavel"), "the book offers the diamond gavel: " + c.slot(0));
        c.slotClick(0, 0, "QUICK_MOVE");
        c.waitTicks(4);
        c.key("escape");
        c.waitTicks(4);
        Expect.that(count(c, "hbm:diamond_gavel") == 1 && count(c, "hbm:lead_gavel") == 0,
                "crafted the diamond gavel from the lead gavel");
        c.cmd("clear");
    }

    // ------------------------------------------------------------------ bobmazon

    private void bobmazon(Client c) {
        // a wide catch floor: the drone lands a gaussian ten blocks from the buyer
        c.cmd("fill " + (X - 30) + " " + Y + " " + (Z - 30) + " " + (X + 30) + " " + Y + " " + (Z + 30) + " minecraft:stone replace minecraft:air");
        Fixtures.giveOnly(c, "hbm:bobmazon", 1);
        c.cmd("give @s hbm:cap_nuka 5");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, -80);
        c.waitTicks(3);
        c.use(2);
        c.waitScreen("GUIScreenBobmazon", 5000);
        c.waitTicks(5);
        c.screenshot("70-bobmazon");
        JsonObject status = c.status();
        int left = (status.get("gui_width").getAsInt() - 217) / 2;
        int top = (status.get("gui_height").getAsInt() - 229) / 2;
        c.click(left + 34 + 9, top + 35 + 9); // offer 0: 64 torches, 2 caps, no requirement
        c.waitTicks(10);
        c.key("escape");
        c.waitTicks(3);
        Expect.that(count(c, "hbm:cap_nuka") == 3, "two caps paid, " + count(c, "hbm:cap_nuka") + " left");

        // the drone falls from y 300 at two blocks a tick and drops its payload where it lands
        JsonObject torch = null;
        for (int i = 0; i < 20 && torch == null; i++) {
            c.waitTicks(20);
            for (JsonElement e : c.entities(64).getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                if (o.get("type").getAsString().equals("minecraft:item") && o.get("name").getAsString().contains("Torch")) torch = o;
            }
        }
        Expect.that(torch != null, "the Bobmazon drone delivered the torches");
        c.teleport(torch.get("x").getAsDouble(), torch.get("y").getAsDouble() + 0.2, torch.get("z").getAsDouble());
        c.waitTicks(20);
        c.screenshot("71-bobmazon-delivered");
        Expect.that(count(c, "minecraft:torch") == 64, "picked up 64 torches, have " + count(c, "minecraft:torch"));
        c.teleport(X + 0.5, Y + 1, Z + 0.5);

        // the hidden catalogue opens too (special offers)
        Fixtures.giveOnly(c, "hbm:bobmazon_hidden", 1);
        c.look(0, -80);
        c.waitTicks(3);
        c.use(2);
        c.waitScreen("GUIScreenBobmazon", 5000);
        c.waitTicks(5);
        c.screenshot("72-bobmazon-hidden");
        c.key("escape");
        c.cmd("clear");
    }

    // ------------------------------------------------------------------ loot, spade, dynamite, cell

    private void loot(Client c) {
        for (String crate : new String[]{"loot_10", "loot_15", "loot_misc"}) {
            openKit(c, crate);
            int parts = 0;
            for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
                if (e.getAsJsonObject().get("id").getAsString().startsWith("hbm:mp_")) parts++;
            }
            Expect.that(parts == 1, crate + " gave one missile part");
        }
        c.key("e");
        c.waitTicks(10);
        c.screenshot("80-loot-crate-part");
        c.key("escape");
        c.cmd("clear");
    }

    private void spade(Client c) {
        int sx = X + 4;
        Fixtures.setBlock(c, sx, Y + 1, Z + 3, "hbm:ntm_dirt");
        Fixtures.giveOnly(c, "hbm:mysteryshovel", 1);
        useOn(c, "mysteryshovel", sx, Y + 1, Z + 3);
        Expect.block(c, sx, Y + 1, Z + 3, "minecraft:air");
        c.waitTicks(5);
        c.screenshot("81-brittle-spade");
        c.teleport(sx + 0.5, Y + 1, Z + 3.5);
        c.waitTicks(40);
        int eggs = 0;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            String name = e.getAsJsonObject().get("name").getAsString();
            if (name.equals("ELEMENTS") || name.equals("ARSENIC") || name.equals("VAULT")) eggs++;
        }
        Expect.that(eggs == 3, "the brittle spade dug up ELEMENTS, ARSENIC and VAULT, picked up " + eggs);
        c.key("e");
        c.waitTicks(10);
        c.screenshot("82-brittle-spade-eggs");
        c.key("escape");
        c.cmd("kill @e[type=minecraft:item]");
    }

    private void fishing(Client c) {
        int px = X - 30;
        c.cmd("fill " + (px - 7) + " " + (Y - 12) + " " + (Z - 7) + " " + (px + 7) + " " + (Y + 2) + " " + (Z + 7) + " minecraft:water");
        c.teleport(px + 0.5, Y + 6, Z - 9.5);
        c.cmd("setblock " + px + " " + (Y + 5) + " " + (Z - 10) + " minecraft:stone");
        c.teleport(px + 0.5, Y + 6, Z - 9.5);
        Fixtures.giveOnly(c, "hbm:stick_dynamite_fishing", 1);
        c.lookAt(px + 0.5, Y + 2.0, Z - 5.0);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(80);
        int fish = 0;
        for (JsonElement e : c.entities(32).getAsJsonArray()) {
            String type = e.getAsJsonObject().get("type").getAsString();
            if (type.contains("buoyant")) fish++;
        }
        c.screenshot("83-fishing-dynamite");
        Expect.that(fish > 0, "fishing dynamite threw up buoyant fish, found " + fish);
        c.cmd("kill @e[type=!player]");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
    }

    private void starblaster(Client c) {
        Fixtures.giveOnly(c, "hbm:weaponized_starblaster_cell", 1);
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.lookAt(X + 0.5, Y + 1.0, Z + 3.5);
        c.waitTicks(2);
        c.key("key.drop");
        c.waitTicks(60);
        boolean dropped = false;
        for (JsonElement e : c.entities(16).getAsJsonArray()) {
            if (e.getAsJsonObject().get("type").getAsString().equals("minecraft:item")) dropped = true;
        }
        Expect.that(dropped, "the rigged cell lies on the floor");
        c.screenshot("84-starblaster-cell-smoking");
        // gone well before its 50 second fuse
        c.cmd("kill @e[type=minecraft:item]");
        c.cmd("clear");
    }

    private static int count(Client c, String itemId) {
        JsonObject inventory = c.inventory();
        int count = 0;
        for (var e : inventory.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.has("id") && itemId.equals(s.get("id").getAsString())) count += s.get("count").getAsInt();
        }
        return count;
    }
}
