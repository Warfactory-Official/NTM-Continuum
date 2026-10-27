import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * In-game acceptance for the 1.7 NBT structure system and its six structure-editing blocks.
 *
 * <p>Every wand block in the inventory (with the loot/logic tooltips) and placed by hand; the look overlays; the
 * jigsaw, tandem, save and load screens opened by a real right-click (the jigsaw pool typed in and checked on the
 * server); wand_k's air mode filling air with wand_air; a small build saved with wand_s + the saving block's SAVE
 * button and loaded elsewhere with a loading block (compared block by block); a bundled 1.7 structure
 * (desert_shack_1) loaded in structure-debug mode; and wand_d pasting the 1.7 crane_mod prefab, with its palette
 * mapping and its loot/logic placeholders replacing themselves checked at known cells.
 */
public final class FinalStructures implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    private static boolean legacy(Client c) {
        return c.node().startsWith("1.20.");
    }

    private static String loadItem(Client c) {
        return "hbm:wand_structure_load"; // 1.7 wand_structure metadata 1 (History §19es-itemvariant)
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, X, Y, Z, 24);
        // No reach attribute: 1.20.1 Fabric has none (vanilla's creative pick range is a fixed 5.0 there), so every
        // click in this scenario is aimed within 5 blocks of the eye on all six nodes.
        c.markLog();

        inventory(c);
        placeAll(c);
        screens(c);
        airWand(c);
        saveAndLoad(c);
        bundledDebugLoad(c);
        crane(c);

        // 1.7 StructureManager names structures/excavator.nbt, which 1.7 never shipped; its "not found" error is 1.7's own
        List<String> bad = c.logSinceMark("(ERROR|FATAL)[]]|Failed to load texture|Missing model|Unable to load model|Exception reading NBT");
        bad.removeIf(line -> line.contains("NBT Structure not found: structures/excavator.nbt"));
        Expect.that(bad.isEmpty(), "log clean since the mark: " + bad);
    }

    // --- inventory ----------------------------------------------------------------------------------------------

    private static final String[] BLOCKS = {"hbm:wand_air", "hbm:wand_loot", "hbm:wand_jigsaw", "hbm:wand_logic",
            "hbm:wand_tandem", "hbm:wand_structure"};

    private static void inventory(Client c) {
        c.cmd("clear");
        c.waitTicks(2);
        for (String id : BLOCKS) c.cmd("give @s " + id + " 1");
        c.cmd("give @s " + loadItem(c) + " 1");
        c.waitTicks(4);
        JsonObject inv = c.inventory();
        for (String id : BLOCKS) Expect.that(inv.toString().contains("\"" + id + "\""), "inventory holds " + id);
        Expect.that(inv.toString().contains("Structure Loading Block"), "the load variant is named Structure Loading Block: " + inv);
        c.cmd("gamemode survival");
        c.waitTicks(2);
        c.key("key.inventory");
        c.waitScreen("", 5000);
        c.waitTicks(10);
        c.screenshot("01-inventory");
        for (String id : new String[]{"hbm:wand_loot", "hbm:wand_logic"}) {
            JsonObject slot = findSlot(c, id);
            Expect.that(slot != null, "slot of " + id);
            c.mouseMove(slot.get("x").getAsDouble() + 8, slot.get("y").getAsDouble() + 8);
            c.waitTicks(4);
            c.screenshot("02-tooltip-" + id.substring(4));
        }
        close(c);
        c.cmd("gamemode creative");
        c.cmd("clear");
        c.waitTicks(2);
    }

    // --- placing ------------------------------------------------------------------------------------------------

    private static final int ROW_Z = Z + 4;

    private static int rowX(int i) {
        return X - 6 + i * 2;
    }

    private static void placeAll(Client c) {
        for (int i = 0; i < BLOCKS.length; i++) {
            JsonObject placed = Fixtures.placeOnTop(c, BLOCKS[i], rowX(i), Y, ROW_Z);
            Expect.equal(BLOCKS[i], placed.get("block").getAsString(), "placed " + BLOCKS[i]);
        }
        JsonObject load = Fixtures.placeOnTop(c, loadItem(c), rowX(6), Y, ROW_Z);
        Expect.that(load.get("state").getAsString().contains("mode=load"), "the load variant places a loading block: " + load);
        Expect.that(c.block(rowX(5), Y + 1, ROW_Z).get("state").getAsString().contains("mode=save"), "wand_structure places a saving block");
        c.cmd("clear");
        worldShot(c, "10-placed-row", X + 0.5, Y + 3.5, ROW_Z - 5.5, X + 0.5, Y + 1, ROW_Z + 0.5);
    }

    // --- screens ------------------------------------------------------------------------------------------------

    private static void standBefore(Client c, int x) {
        c.hotbar(9);
        c.teleport(x + 0.5, Y + 1, ROW_Z - 2.5);
        c.waitTicks(3);
    }

    private static void screens(Client c) throws Exception {
        // look overlays (HUD) of the placeholders
        String[] overlay = {"wand_loot", "wand_jigsaw", "wand_logic", "wand_tandem", "wand_structure"};
        for (int i = 1; i <= 5; i++) {
            standBefore(c, rowX(i));
            c.lookAt(rowX(i) + 0.5, Y + 1.5, ROW_Z + 0.02);
            c.waitTicks(10);
            c.screenshot("2" + i + "-overlay-" + overlay[i - 1]);
        }

        // jigsaw: open by right-click, type a pool, close, the server has it
        standBefore(c, rowX(2));
        Fixtures.interact(c, rowX(2), Y + 1, ROW_Z);
        c.waitScreen("GUIWandJigsaw", 5000);
        c.waitTicks(5);
        c.screenshot("30-gui-jigsaw");
        JsonObject status = c.status();
        int w = status.get("gui_width").getAsInt();
        c.click(w / 2.0 - 100, 60);
        c.waitTicks(2);
        c.type("halls");
        c.waitTicks(2);
        c.click(w / 2.0 + 105, 160); // joint type toggle
        c.waitTicks(3);
        c.screenshot("31-gui-jigsaw-edited");
        close(c);
        c.waitTicks(5);
        String jigsawNbt = snbt(c, rowX(2), Y + 1, ROW_Z);
        Expect.that(jigsawNbt.contains("halls"), "the jigsaw took the typed pool: " + jigsawNbt);
        Expect.that(jigsawNbt.contains("roll:0b"), "the joint toggle turned it to Aligned: " + jigsawNbt);

        standBefore(c, rowX(4));
        Fixtures.interact(c, rowX(4), Y + 1, ROW_Z);
        c.waitScreen("GUIWandTandem", 5000);
        c.waitTicks(5);
        c.screenshot("32-gui-tandem");
        close(c);

        standBefore(c, rowX(5));
        Fixtures.interact(c, rowX(5), Y + 1, ROW_Z);
        c.waitScreen("GUIStructureSave", 5000);
        c.waitTicks(5);
        c.screenshot("33-gui-structure-save");
        close(c);

        standBefore(c, rowX(6));
        Fixtures.interact(c, rowX(6), Y + 1, ROW_Z);
        c.waitScreen("GUIStructureLoad", 5000);
        c.waitTicks(5);
        c.screenshot("34-gui-structure-load");
        close(c);

        // loot: a container block in hand sets the replacement (no screen in 1.7)
        Fixtures.giveOnly(c, "minecraft:chest", 1);
        standBefore(c, rowX(1));
        c.hotbar(1);
        c.lookAt(rowX(1) + 0.5, Y + 1.5, ROW_Z + 0.02);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(5);
        String lootNbt = c.blockEntity(rowX(1), Y + 1, ROW_Z).toString();
        Expect.that(lootNbt.contains("minecraft:chest"), "wand_loot now turns into a chest: " + lootNbt);
        c.hotbar(9);
        c.lookAt(rowX(1) + 0.5, Y + 1.5, ROW_Z + 0.02);
        c.waitTicks(10);
        c.screenshot("35-overlay-loot-chest");
        c.cmd("clear");
    }

    // --- wand_k air mode ----------------------------------------------------------------------------------------

    private static void airWand(Client c) throws Exception {
        int ax = X - 10, az = Z - 8;
        Fixtures.giveOnly(c, "hbm:wand_k", 1);
        Fixtures.setBlock(c, ax, Y + 1, az, "hbm:wand_air");
        c.teleport(ax + 0.5, Y + 1, az - 2.5); // the far corner (ax+2, az+1) stays within 5 blocks
        c.waitTicks(3);
        c.markLog();
        c.keyDown("key.sneak");
        c.waitTicks(5);
        c.lookAt(ax + 0.5, Y + 1.5, az + 0.2);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        c.keyUp("key.sneak");
        c.waitTicks(3);
        expectChat(c, "Set block block.hbm.wand_air");
        // a stone post inside the box must survive: air mode only fills air. Box: platform corner (ax+1, Y, az) to
        // the top of the post (ax+2, Y+1, az+1); the platform layer is stone and stays, the air layer fills.
        Fixtures.setBlock(c, ax + 2, Y + 1, az + 1, "minecraft:stone");
        c.lookAt(ax + 1 + 0.5, Y + 0.98, az + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        expectChat(c, "Position set!");
        c.lookAt(ax + 2 + 0.5, Y + 1.98, az + 1.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        expectChat(c, "Selection filled!");
        Expect.block(c, ax + 1, Y + 1, az, "hbm:wand_air");
        Expect.block(c, ax + 2, Y + 1, az, "hbm:wand_air");
        Expect.block(c, ax + 1, Y + 1, az + 1, "hbm:wand_air");
        Expect.block(c, ax + 2, Y + 1, az + 1, "minecraft:stone");
        Expect.block(c, ax + 1, Y, az, "minecraft:stone");
        c.cmd("clear");
        worldShot(c, "40-wand_k-air", ax + 2.5, Y + 3, az - 3.5, ax + 2, Y + 0.5, az + 1);
    }

    // --- save with wand_s + saving block, load with a loading block -----------------------------------------------

    private static final int SX = X + 8, SZ = Z - 12;   // original build, lowest corner (SX, Y+1, SZ)
    private static final int LX = X + 16, LZ = Z - 12;  // loading block

    private static final String[][][] BUILD = {
            // [dy][dz][dx], 3x3x3 above the platform
            {{"minecraft:oak_planks", "minecraft:oak_stairs[facing=east]", "minecraft:glass"},
                    {"minecraft:iron_block", "minecraft:air", "minecraft:oak_stairs[facing=north,half=top]"},
                    {"minecraft:bricks", "minecraft:chest[facing=south]", "hbm:concrete_smooth"}},
            {{"minecraft:oak_planks", "minecraft:air", "minecraft:glass"},
                    {"minecraft:air", "minecraft:gold_block", "minecraft:air"},
                    {"minecraft:bricks", "minecraft:air", "hbm:reinforced_brick"}},
            {{"minecraft:oak_planks", "minecraft:oak_slab[type=top]", "minecraft:glass"},
                    {"minecraft:air", "minecraft:gold_block", "minecraft:air"},
                    {"minecraft:bricks", "minecraft:air", "minecraft:gold_block"}}
    };

    private static void saveAndLoad(Client c) throws Exception {
        for (int dy = 0; dy < 3; dy++)
            for (int dz = 0; dz < 3; dz++)
                for (int dx = 0; dx < 3; dx++)
                    Fixtures.setBlock(c, SX + dx, Y + 1 + dy, SZ + dz, BUILD[dy][dz][dx]);

        // wand_s: first corner the lowest block of the build, second the top far corner; the saving block goes
        // one below the lower corner, i.e. into the platform
        Fixtures.giveOnly(c, "hbm:wand_s", 1);
        c.teleport(SX - 2.5, Y + 1, SZ + 0.5);
        c.waitTicks(3);
        c.markLog();
        c.lookAt(SX + 0.02, Y + 1.5, SZ + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        expectChat(c, "First position set!");
        c.teleport(SX + 5.5, Y + 4, SZ + 4.5);
        c.waitTicks(3);
        c.lookAt(SX + 2.5, Y + 3.98, SZ + 2.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        expectChat(c, "Structure block configured and added at: " + SX + ", " + Y + ", " + SZ);
        Expect.block(c, SX, Y, SZ, "hbm:wand_structure");
        String saving = snbt(c, SX, Y, SZ);
        Expect.that(saving.contains("sizeX:3") && saving.contains("sizeY:3") && saving.contains("sizeZ:3"), "the wand sized the box 3x3x3: " + saving);
        worldShot(c, "50-save-box", SX - 3.5, Y + 5, SZ - 4.5, SX + 1.5, Y + 1.5, SZ + 1.5);

        // open the saving block through its exposed west face, name the file, press SAVE
        Fixtures.setBlock(c, SX - 1, Y, SZ, "minecraft:air");
        c.cmd("clear");
        c.hotbar(9);
        c.teleport(SX - 3.5, Y + 1, SZ + 0.5);
        c.waitTicks(3);
        c.markLog();
        c.lookAt(SX + 0.02, Y + 0.5, SZ + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitScreen("GUIStructureSave", 5000);
        c.waitTicks(5);
        int w = c.status().get("gui_width").getAsInt();
        c.click(w / 2.0, 60);
        c.waitTicks(2);
        c.type("harness_final_structures");
        c.waitTicks(2);
        c.screenshot("51-gui-save-named");
        c.click(w / 2.0, 160); // SAVE
        c.waitTicks(10);
        expectChat(c, "Saved structure as harness_final_structures.nbt");
        Path file = runDir(c).resolve("structures/harness_final_structures.nbt");
        Expect.that(Files.isRegularFile(file) && Files.size(file) > 0, "the structure file was written: " + file);

        // a loading block elsewhere, opened through its exposed west face
        Fixtures.setBlock(c, LX, Y, LZ, "hbm:wand_structure[mode=load]");
        Fixtures.setBlock(c, LX - 1, Y, LZ, "minecraft:air");
        // litter inside the target box: the load wipes it where the file has no block
        Fixtures.setBlock(c, LX + 1, Y + 2, LZ, "minecraft:diamond_block");
        c.teleport(LX - 3.5, Y + 1, LZ + 0.5);
        c.waitTicks(3);
        c.markLog();
        c.lookAt(LX + 0.02, Y + 0.5, LZ + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitScreen("GUIStructureLoad", 5000);
        c.waitTicks(5);
        c.click(w / 2.0, 60);
        c.waitTicks(2);
        c.type("harness_final_structures");
        c.waitTicks(4);
        c.screenshot("52-gui-load-listed");
        int h = c.status().get("gui_height").getAsInt();
        c.click(w / 2.0, h - 60); // LOAD
        c.waitTicks(10);
        expectChat(c, "Structure loaded");
        Expect.that(c.block(LX, Y, LZ).get("state").getAsString().contains("mode=save"), "the loading block became a saving block");

        int compared = 0;
        for (int dy = 0; dy < 3; dy++) {
            for (int dz = 0; dz < 3; dz++) {
                for (int dx = 0; dx < 3; dx++) {
                    JsonObject a = c.block(SX + dx, Y + 1 + dy, SZ + dz);
                    JsonObject b = c.block(LX + dx, Y + 1 + dy, LZ + dz);
                    Expect.equal(a.get("state").getAsString(), b.get("state").getAsString(), "loaded copy at " + dx + "," + dy + "," + dz);
                    compared++;
                }
            }
        }
        Expect.that(compared == 27, "compared 27 cells");
        worldShot(c, "53-save-and-load", SX + 6.5, Y + 6, SZ - 6.5, SX + 6.5, Y + 1.5, SZ + 1.5);
    }

    // --- a bundled 1.7 structure through the loading block (structure debug: placeholders stay visible) ------------

    private static void bundledDebugLoad(Client c) throws Exception {
        Path root = projectRoot();
        Path source = root.resolve("NTM-Continuum/src/main/resources/assets/hbm/structures/desert_shack_1.nbt");
        Path target = runDir(c).resolve("structures/desert_shack_1.nbt");
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        int bx = X - 20, bz = Z + 8;
        c.cmd("fill " + (bx - 2) + " " + (Y + 1) + " " + (bz - 2) + " " + (bx + 20) + " " + (Y + 16) + " " + (bz + 18) + " minecraft:air");
        Fixtures.setBlock(c, bx, Y, bz, "hbm:wand_structure[mode=load]");
        Fixtures.setBlock(c, bx - 1, Y, bz, "minecraft:air");
        c.cmd("clear");
        c.hotbar(9);
        c.teleport(bx - 3.5, Y + 1, bz + 0.5);
        c.waitTicks(3);
        c.markLog();
        c.lookAt(bx + 0.02, Y + 0.5, bz + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitScreen("GUIStructureLoad", 5000);
        c.waitTicks(3);
        int w = c.status().get("gui_width").getAsInt();
        int h = c.status().get("gui_height").getAsInt();
        c.click(w / 2.0, 60);
        c.type("desert_shack_1");
        c.waitTicks(3);
        c.click(w / 2.0, h - 60);
        c.waitTicks(20);
        expectChat(c, "Structure loaded");
        String saving = snbt(c, bx, Y, bz);
        Expect.that(saving.contains("sizeX:18") && saving.contains("sizeY:13") && saving.contains("sizeZ:16"),
                "the loading block took desert_shack_1's 18x13x16 size: " + saving);
        worldShot(c, "60-desert-shack-debug", bx - 8.5, Y + 12, bz - 10.5, bx + 9, Y + 3, bz + 8);
    }

    // --- wand_d: the crane_mod prefab ------------------------------------------------------------------------------

    private static final int CX = 200, CZ = 0;

    private static void crane(Client c) throws Exception {
        Fixtures.platform(c, CX, Y, CZ, 30);
        // a crane from an earlier run stays in the saved world and would catch the ray trace: clear the whole volume
        for (int y = Y + 1; y <= Y + 180; y += 8) {
            c.cmd("fill " + (CX - 30) + " " + y + " " + (CZ - 12) + " " + (CX + 30) + " " + Math.min(y + 7, Y + 180) + " " + (CZ + 12) + " minecraft:air");
        }
        c.cmd("kill @e[type=!player]");
        c.waitTicks(5);
        Fixtures.giveOnly(c, "hbm:wand_d", 1);
        c.teleport(CX + 0.5, Y + 1, CZ - 30.5);
        c.waitTicks(5);
        c.markLog();
        c.lookAt(CX + 0.5, Y + 0.98, CZ + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(60); // the loot/logic placeholders replace themselves on their first tick

        String line = findLog(c, "[wand_d] crane build requested at " + CX + " " + (Y + 1) + " " + CZ);
        Expect.that(line != null, "the debug wand ray-traced to the column and pasted the crane");
        Expect.that(findLog(c, "NBTStructure is invalid") == null, "the crane structure loaded");

        // 52x69x17, centred: origin = (x - 26, y, z - 8)
        int ox = CX - 26, oy = Y + 1, oz = CZ - 8;
        expectAt(c, ox, oy, oz, 4, 31, 8, "hbm:reinforced_brick", null);
        expectAt(c, ox, oy, oz, 0, 32, 7, "hbm:deco_rusty_steel", null);
        expectAt(c, ox, oy, oz, 7, 10, 7, "hbm:concrete_smooth", null);
        expectAt(c, ox, oy, oz, 3, 35, 7, "minecraft:iron_bars", null);
        expectAt(c, ox, oy, oz, 1, 31, 6, "hbm:reinforced_brick_slab", "type=top");          // brick_slab:9
        expectAt(c, ox, oy, oz, 3, 31, 8, "hbm:reinforced_brick_stairs", "half=top");        // stairs:4
        expectAt(c, ox, oy, oz, 1, 32, 6, "hbm:concrete_colored_ext", "type=machine");       // meta 0
        expectAt(c, ox, oy, oz, 6, 16, 7, "hbm:concrete_colored_ext", null);                  // meta 7
        // loot placeholders became their containers, logic placeholders their logic blocks
        expectAt(c, ox, oy, oz, 2, 33, 8, "minecraft:chest", null);
        expectAt(c, ox, oy, oz, 6, 33, 1, "hbm:crate_iron", null);
        expectAt(c, ox, oy, oz, 12, 11, 10, "hbm:deco_loot", null);
        expectAt(c, ox, oy, oz, 2, 34, 8, "hbm:logic_block_invis", null);
        expectAt(c, ox, oy, oz, 17, 25, 7, "hbm:logic_block", null);
        String chest = c.blockEntity(ox + 2, oy + 33, oz + 8).toString();
        Expect.that(chest.contains("Items") || chest.contains("items"), "the chest was filled from POOL_MACHINE_PARTS: " + chest);

        worldShot(c, "70-crane-front", CX + 0.5, Y + 40, CZ - 60.5, CX + 0.5, Y + 30, CZ + 0.5);
        worldShot(c, "71-crane-side", CX + 60.5, Y + 40, CZ + 0.5, CX + 0.5, Y + 30, CZ + 0.5);
        worldShot(c, "72-crane-cab", ox + 8.5, oy + 38, oz - 6.5, ox + 6.5, oy + 32, oz + 7.5);
        c.cmd("kill @e[type=!player]");
    }

    /** The block entity's SNBT with all whitespace removed, so key:value checks do not depend on the printer. */
    private static String snbt(Client c, int x, int y, int z) {
        return c.blockEntity(x, y, z).toString().replaceAll("\\s", "");
    }

    private static void expectAt(Client c, int ox, int oy, int oz, int dx, int dy, int dz, String id, String stateFragment) {
        JsonObject b = Expect.block(c, ox + dx, oy + dy, oz + dz, id);
        if (stateFragment != null) {
            Expect.that(b.get("state").getAsString().contains(stateFragment),
                    "crane cell " + dx + "," + dy + "," + dz + " has " + stateFragment + ": " + b.get("state"));
        }
    }

    // --- helpers ------------------------------------------------------------------------------------------------

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

    private static Path projectRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("NTM-Continuum"))) dir = dir.getParent();
        Expect.that(dir != null, "project root above " + Path.of("").toAbsolutePath());
        return dir;
    }

    private static Path runDir(Client c) {
        Path runs = projectRoot().resolve("NTM-Continuum/versions/" + c.node() + "/runs");
        return Files.isDirectory(runs.resolve("harness")) ? runs.resolve("harness") : runs.resolve("client");
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitTicks(5);
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
        c.waitTicks(10);
        c.lookAt(tx, ty, tz);
        c.waitTicks(10);
        c.screenshot(name);
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }
}
