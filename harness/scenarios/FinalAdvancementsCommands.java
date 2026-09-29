import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Wave 2, advancements + commands: the 1.7 achievement page as advancements and the ntm* commands.
 *
 * <p>One representative in-game route per trigger type, each checked on the integrated server's advancement
 * progress (the {@code hbm_advancements} query), after the advancement was revoked:
 * vanilla {@code recipe_crafted} (a burner press crafted on a crafting table), {@code hbm:item_produced} (an ingot
 * taken from a machine output slot), vanilla {@code tick} + statistics (a slime ball picked up, gneiss mined for
 * Stratum's 500 experience), {@code tick} + equipment (Fiend), {@code hbm:radiation} (standing in chunk radiation set
 * with {@code /ntmrad set}), {@code hbm:room_opened} (a red key turned in a keyhole), {@code hbm:smoked} (a cigarette
 * under the No. 9 helmet), {@code hbm:consumed} (radium coffee), and the boltgun's melee half on a zombie. Then
 * {@code /advancement grant|revoke}, the advancements screen on the HBM tab and a toast are screenshotted, every
 * command runs with valid and invalid arguments with its chat feedback checked, and tab completion is checked for
 * {@code /ntmserver get}.
 */
public final class FinalAdvancementsCommands implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        // Statistics (picked_up, mined) and advancement progress live in the save: start from the clean snapshot,
        // or an earlier run's mined gneiss would grant Stratum before this run mines anything.
        c.resetWorld();
        c.waitInWorld(120_000L);
        ERRORS.clear();
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 12);
        c.cmd("ntmrad clear");
        c.cmd("advancement revoke @s everything");
        c.waitTicks(10);

        JsonObject adv = advancements(c);
        int loaded = adv.get("loaded").getAsInt();
        Expect.that(loaded >= 60, "the 1.7 tree is loaded: " + loaded + " of 63 hbm advancements");
        waitFor(c, "the tab root completes on the first tick", 100, () -> done(c, "root"));

        radiation(c);
        craftedOnTable(c);
        machineOutput(c);
        statistics(c);
        fiend(c);
        redRoom(c);
        smoking(c);
        boltgun(c);
        grantRevoke(c);
        screens(c);
        serverCommands(c);
        clientCommands(c);
        tabCompletion(c);

        c.cmd("ntmrad clear");
        c.cmd("gamemode creative");
        c.cmd("kill @e[type=!player]");
        c.waitTicks(5);
        checkpoint(c);
        Expect.that(ERRORS.isEmpty(), "log clean through the run: " + ERRORS);
    }

    private static final String BAD = "/(ERROR|FATAL)[]]|Unknown criterion|Couldn't load advancement|Parsing error";
    private static final List<String> ERRORS = new ArrayList<>();

    /** Collects error lines since the last mark, then marks again; every mark in this scenario goes through here. */
    private static void checkpoint(Client c) {
        ERRORS.addAll(c.logSinceMark(BAD));
        c.markLog();
    }

    // ------------------------------------------------------------------ advancement routes

    /** Vanilla recipe_crafted: a burner press from the crafting grid, shift-clicked out of the result slot. */
    private void craftedOnTable(Client c) {
        c.cmd("advancement revoke @s only hbm:burner_press");
        c.cmd("clear");
        Fixtures.setBlock(c, X + 2, Y + 1, Z, "minecraft:crafting_table");
        c.cmd("give @s minecraft:iron_ingot 6");
        c.cmd("give @s minecraft:furnace 1");
        c.cmd("give @s minecraft:piston 1");
        c.cmd("give @s minecraft:iron_block 1");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(4);
        Fixtures.interact(c, X + 2, Y + 1, Z);
        c.waitScreen("Crafting", 5_000L);
        // IRI / IPI / IBI: carry the six ingots and drop one on each I cell (right-click = one)
        c.clickSlot(slotWith(c, "minecraft:iron_ingot"));
        c.waitTicks(2);
        for (int cell : new int[] {1, 3, 4, 6, 7, 9}) {
            c.clickSlot(cell, 1);
            c.waitTicks(2);
        }
        put(c, "minecraft:furnace", 2);
        put(c, "minecraft:piston", 5);
        put(c, "minecraft:iron_block", 8);
        Expect.slotHolds(c, 0, "hbm:machine_press", 1);
        c.screenshot("01-crafting-burner-press");
        Expect.that(!done(c, "burner_press"), "Under Pressure is not granted before the result is taken");
        c.slotClick(0, 0, "QUICK_MOVE");
        c.waitTicks(4);
        c.key("escape");
        c.waitTicks(4);
        Expect.inventoryHolds(c, "hbm:machine_press", 1);
        waitFor(c, "recipe_crafted grants hbm:burner_press", 40, () -> done(c, "burner_press"));
        c.screenshot("02-toast-under-pressure");
    }

    private static void put(Client c, String item, int cell) {
        c.clickSlot(slotWith(c, item));
        c.waitTicks(2);
        c.clickSlot(cell);
        c.waitTicks(2);
    }

    /** hbm:item_produced (1.7 SlotCraftingOutput): an ingot taken out of the breeding reactor's output slot. */
    private void machineOutput(Client c) {
        c.cmd("advancement revoke @s only hbm:schrab");
        c.cmd("clear");
        Fixtures.placeOnTop(c, "hbm:machine_reactor", X - 3, Y, Z);
        Expect.block(c, X - 3, Y + 1, Z, "hbm:machine_reactor");
        c.cmd("item replace block " + (X - 3) + " " + (Y + 1) + " " + Z + " container.1 with hbm:ingot_schrabidium 2");
        c.waitTicks(3);
        c.teleport(X - 1.5, Y + 1, Z + 0.5);
        c.waitTicks(3);
        Fixtures.interact(c, X - 3, Y + 1, Z);
        c.waitTicks(10);
        String screen = c.screenClass();
        Expect.that(screen != null && screen.contains("Breeding"), "the breeding reactor menu opened: " + screen);
        Expect.slotHolds(c, 1, "hbm:ingot_schrabidium", 2);
        Expect.that(!done(c, "schrab"), "Island of Stability is not granted before the output is taken");
        c.screenshot("03-breeding-reactor-output");
        // A real pickup: 1.7's breeding reactor shift-click never called onPickupFromSlot, so only a click grants it
        c.clickSlot(1);
        c.waitTicks(3);
        c.clickSlot(2);
        c.waitTicks(4);
        c.key("escape");
        c.waitTicks(3);
        Expect.inventoryHolds(c, "hbm:ingot_schrabidium", 2);
        waitFor(c, "item_produced grants hbm:schrab", 40, () -> done(c, "schrab"));
    }

    /** Vanilla tick + player statistics: a slime ball picked up (1.7 onItemPickup), gneiss mined (Stratum). */
    private void statistics(Client c) {
        c.cmd("advancement revoke @s only hbm:slimeball");
        c.cmd("advancement revoke @s only hbm:stratum");
        c.cmd("clear");
        // /give inserts directly (its dropped copy is a fake); a real item entity at the feet is a real pickup.
        // Both count spellings: 1.20.1 reads Count, 1.20.5+ reads count.
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(2);
        c.cmd("summon minecraft:item " + X + ".5 " + (Y + 1) + " " + Z + ".5 {Item:{id:\"minecraft:slime_ball\",count:1,Count:1b},PickupDelay:0s}");
        waitFor(c, "a picked-up slime ball grants hbm:slimeball", 60, () -> done(c, "slimeball"));

        c.cmd("clear");
        c.cmd("gamemode survival");
        c.cmd("xp set @s 0 levels");
        c.cmd("xp set @s 0 points");
        Expect.that(!done(c, "stratum"), "Stratum is not granted before any gneiss is mined");
        Fixtures.setBlock(c, X, Y + 1, Z + 2, "hbm:stone_gneiss");
        c.cmd("give @s minecraft:diamond_pickaxe 1");
        // The harness never grabs the mouse, so a held attack does not keep digging; haste makes the first hit break.
        c.cmd("effect give @s minecraft:haste 30 255 true");
        c.hotbar(1);
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(4);
        c.lookAt(X + 0.5, Y + 1.5, Z + 2);
        c.waitTicks(2);
        c.attack(2);
        c.waitTicks(5);
        Expect.that(c.block(X, Y + 1, Z + 2).toString().contains("minecraft:air"), "the gneiss was mined: " + c.block(X, Y + 1, Z + 2));
        waitFor(c, "mined gneiss grants hbm:stratum", 60, () -> done(c, "stratum"));
        // the reward reaches the client with the next experience sync
        waitFor(c, "Stratum paid its 500 experience (level 19 = 493-611 points)", 40,
                () -> c.player().get("xp_level").getAsInt() == 19);
        c.cmd("gamemode creative");
        c.cmd("effect clear @s minecraft:haste");
        c.cmd("xp set @s 0 levels");
    }

    /** Vanilla tick + equipment: the jacket worn with the shimmer sledge in hand (1.7 checkForFiend). */
    private void fiend(Client c) {
        c.cmd("advancement revoke @s only hbm:fiend");
        c.cmd("clear");
        c.cmd("give @s hbm:shimmer_sledge 1");
        c.hotbar(1);
        c.waitTicks(10);
        Expect.that(!done(c, "fiend"), "the sledge alone does not make a Delinquent");
        c.cmd("item replace entity @s armor.chest with hbm:jackt");
        waitFor(c, "jacket + sledge grants hbm:fiend", 40, () -> done(c, "fiend"));
        c.cmd("clear");
    }

    /** hbm:radiation, "poison" band: stand in chunk radiation set with /ntmrad until 200 RAD. */
    private void radiation(Client c) {
        c.cmd("advancement revoke @s only hbm:rad_poison");
        c.cmd("advancement revoke @s only hbm:rad_death");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.cmd("gamemode survival"); // 1.7 ContaminationType.CREATIVE: creative players take no chunk radiation
        c.waitTicks(2);
        c.cmd("ntmrad set 150");
        waitFor(c, "standing in 150 RAD/s grants hbm:rad_poison", 20 * 40, () -> done(c, "rad_poison"));
        c.cmd("ntmrad clear");
        Expect.that(!done(c, "rad_death"), "Ouch, Radiation! needs the 1000 RAD death");
        respawn(c); // drop the accumulated dose before the cigarettes and the coffee add more
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }

    private static void respawn(Client c) {
        c.cmd("kill @s");
        c.waitScreen("Death", 10_000L);
        c.waitTicks(30); // the death screen enables its buttons after a second
        c.clickWidget("Respawn");
        c.waitScreen("none", 10_000L);
        c.waitTicks(10);
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(5);
    }

    /** hbm:room_opened: a red key turned in a stone keyhole's side. */
    private void redRoom(Client c) {
        c.cmd("advancement revoke @s only hbm:red_room");
        c.cmd("clear");
        int kx = X + 6;
        Fixtures.setBlock(c, kx, Y + 2, Z, "hbm:stone_keyhole");
        c.cmd("give @s hbm:key_red 1");
        c.hotbar(1);
        c.teleport(kx - 2 + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(3);
        c.lookAt(kx, Y + 2.5, Z + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(10);
        waitFor(c, "turning the red key grants hbm:red_room", 40, () -> done(c, "red_room"));
        c.screenshot("04-red-room-door");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
    }

    /** hbm:smoked (No. 9) and hbm:consumed (radium coffee). */
    private void smoking(Client c) {
        c.cmd("advancement revoke @s only hbm:no9");
        c.cmd("advancement revoke @s only hbm:radium");
        c.cmd("clear");
        c.cmd("give @s hbm:cigarette 2");
        c.hotbar(1);
        c.look(0, 60);
        c.use(40);
        c.waitTicks(5);
        Expect.that(!done(c, "no9"), "a cigarette without the No. 9 helmet grants nothing");
        c.cmd("item replace entity @s armor.head with hbm:no9");
        c.waitTicks(2);
        c.use(40);
        c.waitTicks(5);
        waitFor(c, "smoking under the No. 9 grants hbm:no9", 40, () -> done(c, "no9"));
        c.cmd("clear");
        c.cmd("give @s hbm:coffee_radium 1");
        c.hotbar(1);
        c.use(45);
        c.waitTicks(5);
        waitFor(c, "radium coffee grants hbm:radium", 40, () -> done(c, "radium"));
        respawn(c);
        c.cmd("clear");
    }

    /** The boltgun's melee half: a steel bolt spent for 10 armour-piercing damage on a zombie. */
    private void boltgun(Client c) {
        c.cmd("clear");
        c.cmd("difficulty easy");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.cmd("summon minecraft:zombie " + X + ".5 " + (Y + 1) + " " + (Z + 2) + ".5 {NoAI:1b}");
        c.cmd("effect give @e[type=minecraft:zombie] minecraft:fire_resistance 120 0 true");
        c.cmd("give @s hbm:boltgun 1");
        c.cmd("give @s hbm:bolt_steel 3");
        c.hotbar(1);
        c.waitTicks(5);
        double before = zombieHealth(c);
        c.lookAt(X + 0.5, Y + 2.0, Z + 2.5);
        c.waitTicks(2);
        c.attack(1);
        c.waitTicks(5);
        double after = zombieHealth(c);
        Expect.that(before - after >= 9.5, "the boltgun hit for 10 armour-piercing damage: " + before + " -> " + after);
        Expect.inventoryHolds(c, "hbm:bolt_steel", 2);
        c.cmd("kill @e[type=minecraft:zombie]");
        c.cmd("difficulty peaceful");
        c.cmd("clear");
    }

    private static double zombieHealth(Client c) {
        JsonObject living = c.query("hbm_living", new JsonObject());
        for (JsonElement e : living.getAsJsonArray("living")) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals("minecraft:zombie")) return o.get("health").getAsDouble();
        }
        return -1;
    }

    /** /advancement grant and revoke work on the tree, and a grant shows its toast. */
    private void grantRevoke(Client c) {
        c.cmd("advancement grant @s only hbm:impossible");
        c.waitTicks(12);
        c.screenshot("05-toast-literally-impossible");
        waitFor(c, "/advancement grant reaches hbm:impossible", 20, () -> done(c, "impossible"));
        c.cmd("advancement revoke @s only hbm:impossible");
        waitFor(c, "/advancement revoke takes hbm:impossible back", 20, () -> !done(c, "impossible"));
        c.cmd("advancement grant @s through hbm:manhattan"); // the Manhattan branch: root, chemplant... polymer, manhattan
        waitFor(c, "/advancement grant through reaches the parents", 20, () -> done(c, "polymer") && done(c, "manhattan"));
    }

    /** The advancements screen, opened with its key, on the HBM tab. */
    private void screens(Client c) {
        c.waitTicks(100); // let the toasts leave
        c.key("key.advancements");
        c.waitTicks(20);
        String screen = c.screenClass();
        Expect.that(screen != null && screen.contains("Advancement"), "the advancements screen opened: " + screen);
        c.screenshot("06-advancements-first-tab");
        JsonObject status = c.status();
        int x0 = (status.get("gui_width").getAsInt() - 252) / 2;
        int y0 = (status.get("gui_height").getAsInt() - 140) / 2;
        for (int tab = 0; tab < 8; tab++) {
            c.click(x0 + 32 * tab + 14, y0 - 14);
            c.waitTicks(8);
            c.screenshot("07-advancements-tab-" + tab);
        }
        c.key("escape");
        c.waitTicks(5);
    }

    // ------------------------------------------------------------------ commands

    private void serverCommands(Client c) {
        expectChat(c, "ntmreload", "Reload complete :)");
        expectChat(c, "ntmrad", "/ntmrad <set/clear>");
        expectChat(c, "ntmrad set 5", "Radiation set.");
        expectChat(c, "ntmrad set -1", "must not be less than");
        expectChat(c, "ntmrad clear", "Cleared radiation data!");
        expectChat(c, "ntmserver list", "SERVER VARIABLES:");
        expectChat(c, "ntmserver get MINE_AP_DAMAGE", "MINE_AP_DAMAGE: 10.0");
        expectChat(c, "ntmserver get NOPE", "Key does not exist.");
        expectChat(c, "ntmserver set AUTOCAL_MAX_CLOCK fast", "Error parsing type for Integer");
        expectChat(c, "ntmserver set AUTOCAL_MAX_CLOCK 21", "Value updated.");
        expectChat(c, "ntmserver get AUTOCAL_MAX_CLOCK", "AUTOCAL_MAX_CLOCK: 21");
        expectChat(c, "ntmserver set AUTOCAL_MAX_CLOCK 20", "Value updated.");
        expectChat(c, "ntmserver help", "/ntmserver list");
        expectChat(c, "ntmserver help reload", "Reads server variables from the config file.");
        expectChat(c, "ntmserver reload", "Variables loaded from config file.");
        expectChat(c, "ntmserver", "/ntmserver help");
        expectChat(c, "ntmlocate list", ",");
        expectChat(c, "ntmlocate structure no_such_thing", "No structures with that name!");
        c.cmd("clear");
        expectChat(c, "ntmcustomize", "You have to hold a customizable item to use this command!");
        expectChat(c, "ntmreapnetworks", "Nodespace cleared :)");
        expectChat(c, "ntmloadchunk 0 0", "Chunk currently loaded.");
        expectChat(c, "ntmloadchunk 4000000 4000000", "Tag list null");
        expectChat(c, "ntmloadchunk x 0", "integer");
        expectChat(c, "hbm rad here", "Radiation here");
    }

    /** Client commands go through the chat screen, as a player types them. */
    private void clientCommands(Client c) {
        typed(c, "/ntmclient list", "CLIENT VARIABLES:");
        typed(c, "/ntmclient get NUKE_HUD_FLASH", "NUKE_HUD_FLASH: true");
        typed(c, "/ntmclient set NUKE_HUD_FLASH false", "Value updated.");
        typed(c, "/ntmclient get NUKE_HUD_FLASH", "NUKE_HUD_FLASH: false");
        typed(c, "/ntmclient set NUKE_HUD_FLASH true", "Value updated.");
        typed(c, "/ntmclient get NOPE", "Key does not exist.");
        typed(c, "/dumpthreadsandcrashgame", "Requires argument");
        typed(c, "/dumpthreadsandcrashgame nope", "Requires argument");
        typed(c, "/dumpthreadsandcrashgame dump", "Thread: ");
        c.markLog(); // the dump itself is logged at error level on purpose
        typed(c, "/ntmwikirender ItemBoltgun", "Taking a screenshot of ItemBoltgun");
        c.waitTicks(40);
        Expect.that(!c.logSinceMark("Saved wiki render .*ItemBoltgun").isEmpty(), "the wiki render saved its PNG");
        Expect.noScreen(c);
    }

    private void tabCompletion(Client c) {
        JsonObject params = new JsonObject();
        params.addProperty("input", "ntmserver get MINE_");
        JsonObject result = c.query("hbm_suggest", params);
        JsonArray list = result.getAsJsonArray("suggestions");
        Expect.that(list != null && list.toString().contains("MINE_AP_DAMAGE") && list.toString().contains("MINE_NAVAL_DAMAGE"),
                "/ntmserver get MINE_ suggests the mine keys: " + result);
        params.addProperty("input", "ntmrad ");
        result = c.query("hbm_suggest", params);
        Expect.that(result.toString().contains("set") && result.toString().contains("clear"), "/ntmrad suggests set/clear: " + result);
        c.key("t");
        c.waitTicks(5);
        c.type("/ntmserver get ");
        c.waitTicks(10);
        c.screenshot("08-tab-completion-ntmserver-get");
        c.key("escape");
        c.waitTicks(3);
    }

    // ------------------------------------------------------------------ helpers

    private static JsonObject advancements(Client c) {
        return c.query("hbm_advancements", new JsonObject());
    }

    private static boolean done(Client c, String id) {
        JsonArray list = advancements(c).getAsJsonArray("done");
        if (list == null) return false;
        for (JsonElement e : list) if (e.getAsString().equals("hbm:" + id)) return true;
        return false;
    }

    private static void waitFor(Client c, String what, int ticks, BooleanSupplier condition) {
        for (int waited = 0; waited <= ticks; waited += 5) {
            if (condition.getAsBoolean()) {
                Expect.that(true, what);
                return;
            }
            c.waitTicks(5);
        }
        Expect.that(false, what + " (timed out after " + ticks + " ticks)");
    }

    private static int slotWith(Client c, String item) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean()
                    && s.get("id").getAsString().equals(item)) return s.get("index").getAsInt();
        }
        throw new IllegalStateException("no player slot holding " + item);
    }

    private static void expectChat(Client c, String command, String expected) {
        checkpoint(c);
        c.cmd(command);
        c.waitTicks(6);
        List<String> lines = c.logSinceMark("\\[CHAT\\]");
        boolean found = false;
        for (String line : lines) if (line.contains(expected)) found = true;
        Expect.that(found, "/" + command + " answers \"" + expected + "\": " + lines);
    }

    private static void typed(Client c, String line, String expected) {
        checkpoint(c);
        c.key("t");
        c.waitTicks(4);
        c.type(line);
        c.waitTicks(2);
        c.key("enter");
        c.waitTicks(8);
        List<String> lines = c.logSinceMark(java.util.regex.Pattern.quote(expected));
        Expect.that(!lines.isEmpty(), line + " (typed in chat) answers \"" + expected + "\"");
    }
}
