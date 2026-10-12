import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Playtest 2026-10-12 (task "armor"):
 * <ol>
 * <li>vanilla armour takes HBM armour modules (1.7 accepted any {@code ItemArmor}): a diamond chestplate in the armour
 * table by real clicks, servos installed, worn from the table's equipment slot, the servo effect checked by query,
 * the Shift "Mods:" tooltip captured;</li>
 * <li>port: the elytra as a chestplate module - installed in a golden chestplate, worn, a jump in mid-air starts
 * vanilla elytra flight, the stored elytra wears, the wings are drawn (third-person screenshots), and the table
 * hands the elytra back;</li>
 * <li>the glider jetpack refuels in a kerosene-filled fluid tank's fill bay (1.7 {@code FluidLoaderFillableItem}).</li>
 * </ol>
 */
public final class Pt1012Armor implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;
    private static final int TX = X;
    private static final int TZ = Z + 2;
    /** The fluid tank core, away from the table. */
    private static final int KX = X + 8;
    private static final int KZ = Z + 2;

    /** Table menu: 0..8 module slots, 9 the armor being modified, 10..13 the worn head..feet slots. */
    private static final int ARMOR = 9;
    private static final int CHEST = 11;
    private static final int FEET = 13;

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 14);
        c.cmd("fill -14 " + (Y + 1) + " -14 14 " + (Y + 12) + " 14 minecraft:air");
        c.cmd("gamemode survival");
        c.cmd("effect clear @s");
        c.cmd("effect give @s minecraft:saturation 600 4 true");
        c.cmd("effect give @s minecraft:resistance 600 4 true");
        clearArmor(c);
        c.waitTicks(2);

        vanillaChestplate(c);
        elytraModule(c);
        tankRefuelsGlider(c);

        clearArmor(c);
        c.cmd("effect clear @s");
        c.cmd("gamemode creative");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }

    // ------------------------------------------------------------------ 1. vanilla armour + modules

    private void vanillaChestplate(Client c) {
        Fixtures.placeOnTop(c, "hbm:machine_armor_table", TX, Y, TZ);
        Expect.block(c, TX, Y + 1, TZ, "hbm:machine_armor_table");
        c.cmd("clear");
        c.cmd("give @s minecraft:diamond_chestplate 1");
        c.cmd("give @s hbm:servo_set 1");
        c.cmd("give @s hbm:cladding_iron 1");
        c.waitTicks(5);
        openTable(c);
        moveInto(c, "minecraft:diamond_chestplate", ARMOR);
        moveInto(c, "hbm:servo_set", 4);
        moveInto(c, "hbm:cladding_iron", 5);
        c.waitTicks(5);
        c.screenshot("10-table-diamond-chestplate");
        moveSlot(c, ARMOR, CHEST);
        c.key("escape");
        c.waitTicks(20);

        JsonObject a = armor(c);
        Expect.that(a.getAsJsonObject("chest").get("id").getAsString().equals("minecraft:diamond_chestplate"),
                "the diamond chestplate is worn: " + a.get("chest"));
        expectMod(a, "chest", "4:hbm:servo_set");
        expectMod(a, "chest", "5:hbm:cladding_iron");
        double dmg = a.get("attack_damage").getAsDouble();
        Expect.that(Math.abs(dmg - 1.5) < 0.01, "servos on a diamond chestplate: attack damage 1 * 1.5, got " + dmg);
        double kb = a.get("knockback_resistance").getAsDouble();
        // diamond armour has no knockback resistance of its own; the iron cladding adds 0.5
        Expect.that(Math.abs(kb - 0.5) < 0.01, "iron cladding on diamond: knockback resistance 0.5, got " + kb);

        // the Shift "Mods:" listing on the vanilla chestplate
        c.key("e");
        c.waitTicks(10);
        c.keyDown("lshift");
        c.waitTicks(2);
        hover(c, 6);
        c.screenshot("11-inventory-shift-hover-diamond-chestplate");
        c.keyUp("lshift");
        c.key("escape");
        c.waitTicks(3);
        clearArmor(c);
    }

    // ------------------------------------------------------------------ 2. elytra module (port-only)

    private void elytraModule(Client c) {
        c.cmd("clear");
        c.cmd("give @s minecraft:golden_chestplate 1");
        c.cmd("give @s minecraft:elytra 1");
        c.cmd("give @s minecraft:iron_helmet 1");
        c.waitTicks(5);
        openTable(c);
        // a helmet takes no elytra
        moveInto(c, "minecraft:iron_helmet", ARMOR);
        int elytra = find(c, "minecraft:elytra");
        moveSlot(c, elytra, 1);
        Expect.that(c.slot(1).get("empty").getAsBoolean(), "the elytra went into a helmet's chestplate slot");
        // put the carried elytra back where it came from, take the helmet out
        c.clickSlot(elytra);
        c.waitTicks(3);
        int spare = find(c, "minecraft:air");
        moveSlot(c, ARMOR, spare >= 0 ? spare : FEET + 1);

        moveInto(c, "minecraft:golden_chestplate", ARMOR);
        moveInto(c, "minecraft:elytra", 1);
        c.waitTicks(5);
        c.screenshot("20-table-elytra-in-golden-chestplate");
        moveSlot(c, ARMOR, CHEST);
        c.key("escape");
        c.waitTicks(10);
        JsonObject a = armor(c);
        expectMod(a, "chest", "1:minecraft:elytra");

        // jump in mid-air: vanilla's own start
        c.teleport(X + 0.5, Y + 60, Z - 10.5);
        c.look(0, 0);
        c.waitTicks(4);
        c.key("space");
        c.waitTicks(6);
        a = armor(c);
        Expect.that(a.get("fall_flying").getAsBoolean() && a.get("client_fall_flying").getAsBoolean(),
                "jumping in mid-air started elytra flight (server " + a.get("fall_flying") + ", client "
                        + a.get("client_fall_flying") + ")");
        c.key("f5");
        c.waitTicks(10);
        c.screenshot("21-f5-back-elytra-wings-gliding");
        c.key("f5");
        c.waitTicks(10);
        c.screenshot("22-f5-front-elytra-wings-gliding");
        c.key("f5");
        c.waitTicks(30);
        a = armor(c);
        Expect.that(a.get("fall_flying").getAsBoolean(), "still gliding after 50 ticks");
        int damage = elytraDamage(a);
        Expect.that(damage >= 1, "the stored elytra wears while gliding (1 per 20 flight ticks), damage " + damage);

        // land, stand still: the wings fold, the cape stays hidden
        c.teleport(X + 0.5, Y + 1, Z - 6.5);
        c.waitTicks(10);
        a = armor(c);
        Expect.that(!a.get("fall_flying").getAsBoolean(), "landing ended the flight");
        c.look(0, 20);
        c.key("f5"); // behind
        c.waitTicks(10);
        c.screenshot("23-f5-back-standing-elytra-chestplate");
        c.key("f5"); // front
        c.waitTicks(10);
        c.screenshot("23b-f5-front-standing-elytra-chestplate");
        c.key("f5"); // first person
        c.waitTicks(5);

        // The dev player wears a 1.7 cape, and vanilla draws a caped player's elytra with the cape texture, whose
        // 1.7 layout has no elytra part: on this player even a worn vanilla elytra is invisible (shot 26). Armor
        // stands have no cape: a copy of the worn chestplate (components included) next to a vanilla elytra and a
        // plain golden chestplate shows the wings layer itself.
        c.cmd("kill @e[type=armor_stand]");
        c.cmd("summon minecraft:armor_stand " + (X + 2.5) + " " + (Y + 1) + " " + (Z - 2.5) + " {Rotation:[0f,0f],Tags:[\"pt_mod\"]}");
        c.cmd("summon minecraft:armor_stand " + (X + 4.5) + " " + (Y + 1) + " " + (Z - 2.5) + " {Rotation:[0f,0f],Tags:[\"pt_ely\"]}");
        c.cmd("summon minecraft:armor_stand " + (X + 0.5) + " " + (Y + 1) + " " + (Z - 2.5) + " {Rotation:[0f,0f],Tags:[\"pt_gold\"]}");
        c.waitTicks(3);
        c.cmd("item replace entity @e[type=armor_stand,tag=pt_mod] armor.chest from entity @s armor.chest");
        c.cmd("item replace entity @e[type=armor_stand,tag=pt_ely] armor.chest with minecraft:elytra");
        c.cmd("item replace entity @e[type=armor_stand,tag=pt_gold] armor.chest with minecraft:golden_chestplate");
        c.teleport(X + 2.5, Y + 1, Z - 6.5);
        c.look(0, 15);
        c.waitTicks(15);
        c.screenshot("23c-armor-stands-backs-gold-modded-elytra");


        // the table hands the elytra back, with its wear, when it is taken out of the chestplate
        openTable(c);
        moveSlot(c, CHEST, ARMOR);
        JsonObject module = c.slot(1);
        Expect.that(!module.get("empty").getAsBoolean() && module.get("id").getAsString().equals("minecraft:elytra"),
                "the worn chestplate's elytra shows in the table: " + module);
        int emptySlot = find(c, "minecraft:air");
        moveSlot(c, 1, emptySlot);
        c.waitTicks(3);
        c.screenshot("24-table-elytra-removed");
        c.key("escape");
        c.waitTicks(5);
        JsonObject inv = c.inventory();
        boolean back = false;
        for (JsonElement e : inv.getAsJsonArray("slots")) {
            back |= e.getAsJsonObject().get("id").getAsString().equals("minecraft:elytra");
        }
        Expect.that(back, "the removed elytra is back in the inventory");

        // references from the same spot: a plain iron chestplate (no wings) and a worn vanilla elytra
        c.teleport(X + 0.5, Y + 1, Z - 6.5);
        c.look(0, 20);
        c.cmd("item replace entity @s armor.chest with minecraft:golden_chestplate");
        c.key("f5");
        c.waitTicks(10);
        c.screenshot("25-f5-back-standing-plain-golden-chestplate");
        c.cmd("item replace entity @s armor.chest with minecraft:elytra");
        c.waitTicks(10);
        c.screenshot("26-f5-back-standing-vanilla-elytra");
        c.key("f5");
        c.key("f5");
        c.waitTicks(5);
        clearArmor(c);
        c.cmd("clear");
    }



    // ------------------------------------------------------------------ 3. tank refuels the glider

    private void tankRefuelsGlider(Client c) {
        c.cmd("clear");
        Fixtures.giveOnly(c, "hbm:machine_fluidtank", 1);
        c.teleport(KX + 4.5, Y + 1, KZ + 0.5);
        c.waitTicks(3);
        c.lookAt(KX + 0.5, Y + 0.98, KZ + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(5);
        JsonObject tank = gapsB(c, "machine", KX, Y + 1, KZ, "fillWith", "KEROSENE", "amount", 100000);
        Expect.that(tank.get("present").getAsBoolean(), "fluid tank placed: " + tank);
        c.waitTicks(5);
        tank = gapsB(c, "machine", KX, Y + 1, KZ);
        Expect.that(tank.get("fill").getAsInt() == 100000 && tank.get("fluid").getAsString().equalsIgnoreCase("KEROSENE"),
                "tank holds 100,000 mB kerosene: " + tank);

        c.cmd("give @s hbm:jetpack_glider 1");
        c.waitTicks(5);
        c.lookAt(KX + 0.5, Y + 1.5, KZ + 0.5);
        c.waitTicks(2);
        c.use();
        c.waitScreen("", 5000);
        c.waitTicks(5);
        JsonObject screen = c.screen();
        Expect.that(screen.has("menu") && screen.get("menu").getAsString().contains("FluidTank"),
                "the fluid tank menu opened: " + (screen.has("menu") ? screen.get("menu") : screen.get("class")));
        int from = find(c, "hbm:jetpack_glider");
        Expect.that(from >= 0, "the glider is in the tank menu's inventory");
        c.clickSlot(from);
        c.waitTicks(2);
        c.clickSlot(4); // the fill bay
        c.waitTicks(10);
        c.screenshot("30-tank-fill-bay-glider");
        JsonObject out = c.slot(5);
        JsonObject in = c.slot(4);
        boolean moved = !out.get("empty").getAsBoolean() && out.get("id").getAsString().equals("hbm:jetpack_glider");
        boolean stayed = !in.get("empty").getAsBoolean() && in.get("id").getAsString().equals("hbm:jetpack_glider");
        Expect.that(moved || stayed, "the fill bay accepted the glider (in " + in + ", out " + out + ")");
        c.key("escape");
        c.waitTicks(3);
        tank = gapsB(c, "machine", KX, Y + 1, KZ);
        int left = tank.get("fill").getAsInt();
        Expect.that(left == 80000, "the glider took its 20,000 mB of kerosene from the tank, tank left " + left);
    }

    // ------------------------------------------------------------------ helpers

    private void openTable(Client c) {
        c.teleport(TX + 0.5, Y + 1, TZ - 1.5);
        c.waitTicks(2);
        Fixtures.interact(c, TX, Y + 1, TZ);
        c.waitScreen("Armor Modification Table", 5000);
        Expect.screenStaysOpen(c, "Armor Modification Table", 10);
    }

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

    /** A player-inventory slot of the open menu holding {@code itemId} ("minecraft:air" = an empty one), or -1. */
    private int find(Client c, String itemId) {
        boolean air = itemId.equals("minecraft:air");
        JsonObject screen = c.screen();
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("player_inventory").getAsBoolean() || s.get("index").getAsInt() <= FEET) continue;
            if (air ? s.get("empty").getAsBoolean()
                    : !s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(itemId)) {
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

    private static void clearArmor(Client c) {
        for (String slot : new String[]{"head", "chest", "legs", "feet"}) {
            c.cmd("item replace entity @s armor." + slot + " with minecraft:air");
        }
    }

    private JsonObject armor(Client c) {
        return c.query("hbm_armor", new JsonObject());
    }

    private static void expectMod(JsonObject armor, String slot, String want) {
        JsonArray mods = armor.getAsJsonObject(slot).getAsJsonArray("mods");
        boolean found = false;
        for (JsonElement e : mods) found |= e.getAsString().startsWith(want + ":");
        Expect.that(found, slot + " holds " + want + ", got " + mods);
    }

    private static int elytraDamage(JsonObject armor) {
        for (JsonElement e : armor.getAsJsonObject("chest").getAsJsonArray("mods")) {
            String s = e.getAsString();
            if (s.startsWith("1:minecraft:elytra:")) return Integer.parseInt(s.substring(s.lastIndexOf(':') + 1));
        }
        return -1;
    }

    private static JsonObject gapsB(Client c, String op, int x, int y, int z, Object... kv) {
        JsonObject p = new JsonObject();
        p.addProperty("op", op);
        p.addProperty("x", x);
        p.addProperty("y", y);
        p.addProperty("z", z);
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Number n) p.addProperty((String) kv[i], n);
            else p.addProperty((String) kv[i], String.valueOf(v));
        }
        JsonObject r = c.query("hbm_gapsb", p);
        Expect.that(!r.has("error"), "query " + op + " failed: " + r);
        return r;
    }
}
