import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * In-game acceptance for the last four registered 1.7 items: the three creative wands (wand_k construction wand,
 * wand_s structure wand, wand_d debug wand) and missile_test.
 *
 * <p>All four in the inventory with each wand's tooltip; the construction wand saving a block by crouch-click,
 * two corner clicks filling the box, and crouch-clicking air saving air; the structure wand's blacklist toggle,
 * its two-click box (which places a configured wand_structure saving block)
 * and blacklist clear; the structure wand building the default Paper Press around a custom machine controller,
 * after which the controller's menu opens; the debug wand's ray trace and crane paste (checked in detail by FinalStructures); and missile_test standing on a launch pad,
 * launched by redstone at a designator's target and climbing as the hbm:missile_test entity.
 */
public final class FinalWands implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    private static boolean legacy(Client c) {
        return c.node().startsWith("1.20.");
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, X, Y, Z, 24);
        // No reach attribute: 1.20.1 Fabric has none (vanilla's creative pick range is a fixed 5.0 there), so every
        // click in this scenario is aimed within 5 blocks of the eye on all six nodes.
        c.markLog();

        inventory(c);
        constructionWand(c);
        structureWand(c);
        customMachine(c);
        debugWand(c);
        missileTest(c);

        // wand_d builds StructureManager.crane, and loading StructureManager logs 1.7's own error for
        // structures/excavator.nbt, which 1.7 named but never shipped (FinalStructures accepts the same line). Only
        // logged by the first scenario in a JVM that touches StructureManager, so it depends on run order.
        List<String> bad = c.logSinceMark("/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Unable to load model");
        bad.removeIf(line -> line.contains("NBT Structure not found: structures/excavator.nbt"));
        Expect.that(bad.isEmpty(), "log clean since the mark: " + bad);
    }

    // --- inventory ----------------------------------------------------------------------------------------------

    private static final String[] ITEMS = {"hbm:wand_k", "hbm:wand_s", "hbm:wand_d", "hbm:missile_test"};

    private static void inventory(Client c) {
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.waitTicks(2);
        for (String id : ITEMS) c.cmd("give @s " + id + " 1");
        c.waitTicks(4);
        JsonObject inv = c.inventory();
        for (String id : ITEMS) Expect.that(inv.toString().contains("\"" + id + "\""), "inventory holds " + id);
        c.key("key.inventory");
        c.waitScreen("", 5000);
        c.waitTicks(10);
        c.screenshot("01-inventory");
        String[] names = {"wand_k", "wand_s", "wand_d", "missile_test"};
        for (int i = 0; i < ITEMS.length; i++) {
            JsonObject slot = findSlot(c, ITEMS[i]);
            Expect.that(slot != null, "slot of " + ITEMS[i]);
            c.mouseMove(slot.get("x").getAsDouble() + 8, slot.get("y").getAsDouble() + 8);
            c.waitTicks(4);
            c.screenshot("0" + (i + 2) + "-tooltip-" + names[i]);
        }
        close(c);
        c.cmd("clear");
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }

    // --- wand_k -------------------------------------------------------------------------------------------------

    private static void constructionWand(Client c) throws Exception {
        int sx = X + 6, sz = Z + 6;
        Fixtures.setBlock(c, sx, Y + 1, sz, "minecraft:gold_block");
        Fixtures.giveOnly(c, "hbm:wand_k", 1);
        c.teleport(sx - 1.5, Y + 1, sz - 2.5); // between the gold block and the far corner: both within 5 blocks
        c.waitTicks(3);

        c.markLog();
        sneakUse(c, sx + 0.5, Y + 1.5, sz + 0.02);
        expectChat(c, "Set block block.minecraft.gold_block");

        // corners: two platform blocks; the box between them (inclusive) becomes gold
        c.lookAt(sx - 2 + 0.5, Y + 0.98, sz + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        expectChat(c, "Position set!");
        c.lookAt(sx - 4 + 0.5, Y + 0.98, sz + 1.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        expectChat(c, "Selection filled!");
        for (int x = sx - 4; x <= sx - 2; x++) {
            for (int z = sz; z <= sz + 1; z++) Expect.block(c, x, Y, z, "minecraft:gold_block");
        }
        Expect.block(c, sx - 5, Y, sz, "minecraft:stone");
        worldShot(c, "10-wand_k-filled", sx + 1.5, Y + 4, sz - 4.5, sx - 3, Y, sz + 0.5);

        // crouch-click air saves air (1.7 block id 0)
        Fixtures.giveOnly(c, "hbm:wand_k", 1);
        c.teleport(sx - 1.5, Y + 1, sz - 2.5);
        c.waitTicks(3);
        c.markLog();
        sneakUse(c, sx + 0.5, Y + 30, sz + 0.5);
        expectChat(c, "Set block block.minecraft.air");
        hoverHeld(c, "hbm:wand_k", "11-wand_k-tooltip-after-use");
    }

    // --- wand_s -------------------------------------------------------------------------------------------------

    private static void structureWand(Client c) throws Exception {
        int sx = X + 6, sz = Z + 6;
        Fixtures.giveOnly(c, "hbm:wand_s", 1);
        c.teleport(sx + 1.5, Y + 1, sz - 2.5); // the far corner (sx+3, sz+1) stays within 5 blocks
        c.waitTicks(3);

        c.markLog();
        sneakUse(c, sx + 0.5, Y + 1.5, sz + 0.02);
        expectChat(c, "Added to blacklist block.minecraft.gold_block");
        hoverHeld(c, "hbm:wand_s", "20-wand_s-tooltip-blacklist");

        c.lookAt(sx + 2 + 0.5, Y + 0.98, sz + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        expectChat(c, "First position set!");
        c.lookAt(sx + 3 + 0.5, Y + 0.98, sz + 1.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        // the second click places a configured wand_structure saving block one below the box's lower corner
        expectChat(c, "Structure block configured and added at: " + (sx + 2) + ", " + (Y - 1) + ", " + sz);
        Expect.block(c, sx + 2, Y - 1, sz, "hbm:wand_structure");

        sneakUse(c, sx + 0.5, Y + 30, sz + 0.5);
        expectChat(c, "Cleared blacklist");
        hoverHeld(c, "hbm:wand_s", "21-wand_s-tooltip-cleared");
    }

    // --- wand_s on a custom machine controller -------------------------------------------------------------------

    private static final int CX = X, CY = Y + 2, CZ = Z - 16;

    private static void customMachine(Client c) throws Exception {
        // a stand for the controller: the Paper Press has components one below it, so it sits in the air on glass
        Fixtures.setBlock(c, CX, CY - 1, CZ, "minecraft:glass");
        String controller = legacy(c) ? "hbm:custom_machine{\"hbm:item_variant\":100}" : "hbm:custom_machine[hbm:item_variant=100]";
        Fixtures.giveOnly(c, controller, 1);
        c.teleport(CX + 0.5, Y + 1, CZ - 2.5);
        c.waitTicks(3);
        c.lookAt(CX + 0.5, CY - 1 + 0.98, CZ + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
        JsonObject core = Expect.block(c, CX, CY, CZ, "hbm:custom_machine");
        Expect.that(core.toString().contains("north"), "the controller faces north: " + core);
        Expect.that(c.blockEntity(CX, CY, CZ).toString().contains("paperPress"), "the controller took the Paper Press");
        Expect.block(c, CX + 1, CY + 1, CZ + 2, "minecraft:air");
        worldShot(c, "30-cm-before", CX + 5.5, CY + 3, CZ - 4.5, CX + 0.5, CY, CZ + 1);

        // a plain right-click with the structure wand on the incomplete machine builds it (1.7 BlockCustomMachine)
        Fixtures.giveOnly(c, "hbm:wand_s", 1);
        c.teleport(CX + 0.5, Y + 1, CZ - 3.5);
        c.waitTicks(3);
        c.markLog();
        c.lookAt(CX + 0.5, CY + 0.5, CZ + 0.02);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(6);
        Expect.block(c, CX - 1, CY - 1, CZ + 2, "hbm:cm_block_steel");
        Expect.block(c, CX + 1, CY, CZ + 2, "hbm:cm_sheet_steel");
        Expect.block(c, CX - 1, CY + 1, CZ + 2, "hbm:cm_block_steel");
        // the wand did not also act as an item (the controller consumed the click, as in 1.7)
        Expect.that(c.logSinceMark("First position set").isEmpty(), "the wand's own click was not run on the controller");
        worldShot(c, "31-cm-built-by-wand", CX + 5.5, CY + 3, CZ - 4.5, CX + 0.5, CY, CZ + 1);

        // now the structure is complete, so the same click opens the menu
        c.teleport(CX + 0.5, Y + 1, CZ - 3.5);
        c.waitTicks(3);
        c.hotbar(9);
        c.lookAt(CX + 0.5, CY + 0.5, CZ + 0.02);
        c.waitTicks(2);
        c.use(2);
        c.waitScreen("GUIMachineCustom", 5000);
        Expect.screenStaysOpen(c, "GUIMachineCustom", 10);
        c.waitTicks(5);
        c.screenshot("32-cm-gui-after-build");
        close(c);
    }

    // --- wand_d -------------------------------------------------------------------------------------------------

    private static void debugWand(Client c) throws Exception {
        Fixtures.giveOnly(c, "hbm:wand_d", 1);
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(3);
        c.markLog();
        c.lookAt(X + 10.5, Y + 0.98, Z - 4.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(6);
        // the server logs it; the integrated server's lines are in latest.log even when not in the harness buffer
        String line = findLog(c, "[wand_d] crane build requested at " + (X + 10) + " " + (Y + 1) + " " + (Z - 5));
        Expect.that(line != null, "the debug wand ray-traced to the platform column and asked for the crane build");
        Expect.that(findLog(c, "NBTStructure is invalid") == null, "the crane NBTStructure loaded and was pasted");
        c.cmd("kill @e[type=!player]");
        c.screenshot("40-wand_d-used");
    }

    // --- missile_test -------------------------------------------------------------------------------------------

    private static void missileTest(Client c) throws Exception {
        int px = X - 12, pz = Z + 12;          // pad core
        // place the pad by hand from the south: the core lands one block past the clicked cell
        Fixtures.giveOnly(c, "hbm:launch_pad", 1);
        c.teleport(px + 0.5, Y + 1, pz + 3.5);
        c.waitTicks(3);
        c.lookAt(px + 0.5, Y + 0.98, pz + 1.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(6);
        Expect.block(c, px, Y + 1, pz, "hbm:launch_pad");

        // the designator aimed at a block 24 east of the pad
        c.cmd("clear");
        c.cmd("give @s hbm:designator 1");
        c.waitTicks(2);
        c.hotbar(1);
        c.teleport(px + 22.5, Y + 1, pz + 0.5);
        c.waitTicks(3);
        c.lookAt(px + 24.5, Y + 0.9, pz + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        c.cmd("give @s hbm:missile_test 1");
        c.cmd("give @s hbm:battery_creative 1");
        c.waitTicks(3);

        // missile, designator and battery into the pad by real clicks
        c.teleport(px + 0.5, Y + 1, pz - 3.5);
        c.waitTicks(3);
        c.hotbar(9);
        c.lookAt(px + 0.5, Y + 1.9, pz - 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen("LaunchPad", 5000);
        Expect.screenStaysOpen(c, "LaunchPad", 10);
        clickFromInventory(c, "hbm:missile_test", 0);
        clickFromInventory(c, "hbm:designator", 1);
        clickFromInventory(c, "hbm:battery_creative", 2);
        c.waitTicks(20);
        Expect.slotHolds(c, 0, "hbm:missile_test", 1);
        c.screenshot("50-pad-loaded");
        close(c);
        worldShot(c, "51-missile_test-on-pad", px + 5.5, Y + 5, pz - 5.5, px + 0.5, Y + 3, pz + 0.5);

        c.waitTicks(110);                       // the pad counts 100 ticks of readiness
        Fixtures.setBlock(c, px + 2, Y + 1, pz, "minecraft:redstone_block");
        c.waitTicks(4);
        JsonObject flying = missile(c);
        Expect.that(flying != null, "a hbm:missile_test entity was launched from the pad: " + c.entities(64));
        c.waitTicks(15);
        JsonObject later = missile(c);
        Expect.that(later != null && later.get("y").getAsDouble() > flying.get("y").getAsDouble(),
                "the test missile climbs: " + flying + " -> " + later);
        Expect.that(!c.blockEntity(px, Y + 1, pz).toString().contains("missile_test"), "the pad consumed the missile");
        worldShot(c, "52-missile_test-flying", px + 12.5, Y + 6, pz - 8.5,
                later.get("x").getAsDouble(), later.get("y").getAsDouble(), later.get("z").getAsDouble());
        // its impact chars a 50-block sphere; this scenario only checks the launch
        c.cmd("kill @e[type=hbm:missile_test]");
        Fixtures.setBlock(c, px + 2, Y + 1, pz, "minecraft:air");
        c.waitTicks(5);
    }

    // --- helpers ------------------------------------------------------------------------------------------------

    private static void sneakUse(Client c, double tx, double ty, double tz) {
        c.keyDown("key.sneak");
        c.waitTicks(5);
        c.lookAt(tx, ty, tz);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        c.keyUp("key.sneak");
        c.waitTicks(3);
    }

    private static void expectChat(Client c, String text) throws Exception {
        for (int i = 0; i < 10; i++) {
            if (!c.logSinceMark(java.util.regex.Pattern.quote(text)).isEmpty()) return;
            c.waitTicks(2);
        }
        Expect.that(false, "chat shows \"" + text + "\": " + c.logSinceMark("CHAT"));
    }

    private static String findLog(Client c, String text) throws Exception {
        List<String> hit = c.logSinceMark(java.util.regex.Pattern.quote(text));
        if (!hit.isEmpty()) return hit.get(0);
        Path log = runDir(c).resolve("logs/latest.log");
        if (Files.isRegularFile(log)) {
            for (String line : Files.readAllLines(log)) if (line.contains(text)) return line;
        }
        return null;
    }

    private static Path runDir(Client c) {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("new-hbm"))) dir = dir.getParent();
        Expect.that(dir != null, "project root above " + Path.of("").toAbsolutePath());
        Path runs = dir.resolve("new-hbm/versions/" + c.node() + "/runs");
        return Files.isDirectory(runs.resolve("harness")) ? runs.resolve("harness") : runs.resolve("client");
    }

    /** Opens the inventory and hovers the one held stack of {@code id}. */
    private static void hoverHeld(Client c, String id, String shot) {
        c.key("key.inventory");
        c.waitScreen("", 5000);
        c.waitTicks(8);
        JsonObject slot = findSlot(c, id);
        Expect.that(slot != null, "slot of " + id);
        c.mouseMove(slot.get("x").getAsDouble() + 8, slot.get("y").getAsDouble() + 8);
        c.waitTicks(4);
        c.screenshot(shot);
        close(c);
    }

    private static JsonObject missile(Client c) {
        for (JsonElement e : c.entities(200)) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals("hbm:missile_test")) return o;
        }
        return null;
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitTicks(5);
    }

    private static void clickFromInventory(Client c, String id, int machineSlot) {
        JsonObject slot = findSlot(c, id);
        Expect.that(slot != null, "inventory slot holding " + id);
        c.clickSlot(slot.get("index").getAsInt());
        c.waitTicks(1);
        c.clickSlot(machineSlot);
        c.waitTicks(1);
        JsonObject carried = c.screen().getAsJsonObject("carried");
        if (carried != null && carried.has("id") && !carried.get("id").getAsString().contains("air")) {
            c.clickSlot(slot.get("index").getAsInt());
            c.waitTicks(1);
        }
    }

    private static JsonObject findSlot(Client c, String id) {
        JsonObject screen = c.screen();
        if (!screen.has("slots")) return null;
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) return s;
        }
        return null;
    }

    private static void worldShot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.hotbar(9);
        c.waitTicks(1);
        c.cmd("gamemode spectator");
        c.waitTicks(2);
        c.teleport(x, y, z);
        c.waitTicks(8);
        c.lookAt(tx, ty, tz);
        c.waitTicks(8);
        c.screenshot(name);
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }
}
