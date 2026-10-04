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
 * In-game acceptance for the custom machine system: every block and item of the family in the inventory and in a
 * row in the world; the default Paper Press (hbmConfig/hbmCustomMachines.json, written on first start) built by
 * hand around its controller, with the ghost of the missing components shown while it is incomplete; power from a
 * creative generator over a cable into the lower cm_port, water from a creative source into the upper cm_port,
 * sawdust from a hopper into the upper port; the controller's menu opened by a real right-click, a water
 * identifier moved in by clicks, and paper produced; and the structure output wand recording a box around the
 * positioning anchor into hbmConfig/CMstructureOutput.txt.
 */
public final class FinalCustomMachine implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    static final String[] BLOCKS = {
            "hbm:cm_block_steel", "hbm:cm_block_alloy", "hbm:cm_block_desh", "hbm:cm_block_tcalloy",
            "hbm:cm_sheet_steel", "hbm:cm_sheet_alloy", "hbm:cm_sheet_desh", "hbm:cm_sheet_tcalloy",
            "hbm:cm_engine_standard", "hbm:cm_engine_desh", "hbm:cm_engine_bismuth",
            "hbm:cm_tank_steel", "hbm:cm_tank_alloy", "hbm:cm_tank_desh", "hbm:cm_tank_tcalloy",
            "hbm:cm_circuit_aluminium", "hbm:cm_circuit_copper", "hbm:cm_circuit_red_copper", "hbm:cm_circuit_gold",
            "hbm:cm_circuit_schrabidium",
            "hbm:cm_port_steel", "hbm:cm_port_alloy", "hbm:cm_port_desh", "hbm:cm_port_tcalloy",
            "hbm:cm_flux", "hbm:cm_heat", "hbm:custom_machine_anchor"};

    // the Paper Press: controller at (CX, CY, CZ) facing north, components at core + (x, y, z), z 0..2 behind it
    private static final int CX = X, CY = Y + 2, CZ = Z;

    /** Per client: one scenario class runs on every node in parallel, so no static node state. */
    private static boolean legacy(Client c) {
        return c.node().startsWith("1.20.");
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 16);
        // No reach attribute: 1.20.1 Fabric has none (vanilla's creative pick range is a fixed 5.0 there), so every
        // click in this scenario is aimed within 5 blocks of the eye on all six nodes (placements from 1-3 above).

        configFileWritten(c);
        inventory(c);
        row(c);

        // --- the Paper Press by hand ------------------------------------------------------------------------
        for (int x = -1; x <= 1; x++) {
            for (int z = 0; z <= 2; z++) {
                if (x == 0 && z == 0) continue;
                placeFromAbove(c, "hbm:cm_block_steel", CX + x, CY - 1, CZ + z);
            }
        }
        placeFromAbove(c, "hbm:cm_port_steel", CX, CY - 1, CZ);
        // the controller, placed from the north so it faces north (1.7: the face turned to the placer)
        Fixtures.giveOnly(c, controller(c), 1);
        c.teleport(CX + 0.5, Y + 4, CZ - 1.5);
        c.waitTicks(2);
        c.lookAt(CX + 0.5, CY - 1 + 0.98, CZ + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
        JsonObject core = Expect.block(c, CX, CY, CZ, "hbm:custom_machine");
        Expect.that(core.toString().contains("north"), "the controller faces north: " + core);
        String coreNbt = c.blockEntity(CX, CY, CZ).toString();
        Expect.that(coreNbt.contains("paperPress"), "the placed controller took the Paper Press config: " + coreNbt);
        worldShot(c, "10-ghost-incomplete", CX + 4.5, CY + 3, CZ - 3.5, CX + 0.5, CY, CZ + 1.5);

        // middle layer: steel sheets; the hollow at (0, 0, 1) is not a component, a glass block holds the roof
        for (int x = -1; x <= 1; x++) {
            for (int z = 0; z <= 2; z++) {
                if (x == 0 && (z == 0 || z == 1)) continue;
                placeFromAbove(c, "hbm:cm_sheet_steel", CX + x, CY, CZ + z);
            }
        }
        placeFromAbove(c, "minecraft:glass", CX, CY, CZ + 1);
        for (int x = -1; x <= 1; x++) {
            for (int z = 0; z <= 2; z++) {
                if (x == 0 && z == 0) continue;
                placeFromAbove(c, "hbm:cm_block_steel", CX + x, CY + 1, CZ + z);
            }
        }
        // the upper port sits on the controller: sneak, or the right-click goes to the controller as in 1.7
        Fixtures.giveOnly(c, "hbm:cm_port_steel", 1);
        c.teleport(CX + 0.5, Y + 5, CZ - 0.5);
        c.waitTicks(2);
        c.lookAt(CX + 0.5, CY + 0.98, CZ + 0.5);
        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(3);
        c.keyUp("key.sneak");
        c.waitTicks(2);
        Expect.block(c, CX, CY + 1, CZ, "hbm:cm_port_steel");
        Expect.block(c, CX, CY - 1, CZ, "hbm:cm_port_steel");
        Expect.block(c, CX + 1, CY, CZ + 2, "hbm:cm_sheet_steel");
        Expect.block(c, CX - 1, CY + 1, CZ + 2, "hbm:cm_block_steel");

        // --- supply: cable + generator on the lower port, a fluid source on top of the upper port ----------
        placeFromAbove(c, "hbm:red_cable", CX, CY - 1, CZ - 1);
        placeFromAbove(c, "hbm:creative_generator", CX, CY - 1, CZ - 2);
        placeFromAbove(c, "hbm:creative_fluid_source", CX, CY + 2, CZ);
        // a hopper against the upper port's north face, pointing into it
        Fixtures.giveOnly(c, "minecraft:hopper", 1);
        c.teleport(CX + 0.5, Y + 1, CZ - 3.5);
        c.waitTicks(3);
        c.lookAt(CX + 0.5, CY + 1.5, CZ + 0.02);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        Expect.block(c, CX, CY + 1, CZ - 1, "minecraft:hopper");
        worldShot(c, "11-paper-press-built", CX + 5.5, CY + 3, CZ - 4.5, CX + 0.5, CY, CZ + 1);
        worldShot(c, "12-paper-press-back", CX - 3.5, CY + 3, CZ + 6.5, CX + 0.5, CY, CZ + 1);

        // --- the controller's menu by a real right-click, the water identifier moved in by clicks --------------
        c.cmd("clear");
        c.cmd("give @s " + identifier(c) + " 1");
        c.cmd("give @s hbm:powder_sawdust 64");
        c.waitTicks(3);
        openController(c);
        c.screenshot("20-gui-empty");
        JsonObject screen = c.screen();
        Expect.that(screen.toString().contains("hbm:fluid_identifier") || findSlot(c, "hbm:fluid_identifier") != null,
                "the menu shows the player's identifier");
        clickFromInventory(c, "hbm:fluid_identifier", 1);
        c.waitTicks(40);
        c.screenshot("21-gui-identifier-water");
        close(c);

        // --- sawdust through the hopper ---------------------------------------------------------------------
        c.teleport(CX + 0.5, Y + 1, CZ - 3.5);
        c.waitTicks(3);
        c.hotbar(9);
        c.waitTicks(1);
        c.lookAt(CX + 0.5, CY + 1.5, CZ - 1.02);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen("Hopper", 5000);
        c.waitTicks(3);
        clickFromInventory(c, "hbm:powder_sawdust", 0);
        c.waitTicks(3);
        close(c);

        // --- the run: 60 ticks per 250 mB + 1 sawdust -> 3 paper ----------------------------------------------
        c.waitTicks(260);
        String nbt = c.blockEntity(CX, CY, CZ).toString();
        Expect.that(nbt.contains("minecraft:paper"), "the Paper Press made paper: " + nbt);
        Expect.that(nbt.contains("powder_sawdust"), "sawdust reached the input slot through the port: " + nbt);
        openController(c);
        c.waitTicks(20);
        c.screenshot("22-gui-running");
        JsonObject out = c.screen();
        String outStr = out.toString();
        Expect.that(outStr.contains("minecraft:paper"), "the output slot shows paper: " + outStr);
        close(c);
        worldShot(c, "23-paper-press-running", CX + 5.5, CY + 3, CZ - 4.5, CX + 0.5, CY, CZ + 1);

        // --- the structure output wand ------------------------------------------------------------------------
        structureWand(c);

        List<String> errors = c.logSinceMark("(?i)(custom.?machine|cm_port|CMstructure|CustomMachine).*(Exception|ERROR)|(ERROR|Exception).*(custom.?machine|CustomMachine)");
        Expect.that(errors.isEmpty(), "no custom machine errors in the log: " + errors);
    }

    private static String controller(Client c) {
        return legacy(c) ? "hbm:custom_machine{\"hbm:item_variant\":100}" : "hbm:custom_machine[hbm:item_variant=100]";
    }

    private static String identifier(Client c) {
        return legacy(c) ? "hbm:fluid_identifier{\"hbm:fluid\":\"hbm:water\"}" : "hbm:fluid_identifier[hbm:fluid=\"hbm:water\"]";
    }

    /** 1.7 wrote hbmConfig/hbmCustomMachines.json with the Paper Press on first start; the port does too. */
    private static void configFileWritten(Client c) throws Exception {
        List<String> loaded = c.log(4000, "Loaded \\d+ custom machine");
        Expect.that(loaded.isEmpty() || loaded.get(loaded.size() - 1).contains("Loaded 1 custom machine"), "one default machine (the line predates the log buffer on some nodes): " + loaded);
        Path file = gameDir(c).resolve("config/hbmConfig/hbmCustomMachines.json");
        Expect.that(Files.isRegularFile(file), "default config written at " + file);
        String json = Files.readString(file);
        Expect.that(json.contains("\"paperPress\"") && json.contains("hbm:tile.cm_sheet") && json.contains("Papierpresse"),
                "default config content: " + json.substring(0, Math.min(400, json.length())));
    }

    private static Path gameDir(Client c) {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("new-hbm"))) dir = dir.getParent();
        Expect.that(dir != null, "project root above " + Path.of("").toAbsolutePath());
        Path runs = dir.resolve("new-hbm/versions/" + c.node() + "/runs");
        for (String name : new String[]{"harness", "client"}) {
            if (Files.isDirectory(runs.resolve(name).resolve("config/hbmConfig"))) return runs.resolve(name);
        }
        return runs.resolve("client");
    }

    private static void inventory(Client c) {
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.waitTicks(2);
        for (String id : BLOCKS) c.cmd("give @s " + id + " 1");
        c.cmd("give @s hbm:structure_custommachine 1");
        c.cmd("give @s " + controller(c) + " 1");
        c.waitTicks(4);
        JsonObject inv = c.inventory();
        for (int i = 0; i < 20 && !inv.toString().contains("hbm:custom_machine\""); i++) {
            c.waitTicks(5);
            inv = c.inventory();
        }
        for (String id : BLOCKS) Expect.that(inv.toString().contains("\"" + id + "\""), "inventory holds " + id);
        Expect.that(inv.toString().contains("hbm:structure_custommachine"), "inventory holds the wand");
        Expect.that(inv.toString().contains("hbm:custom_machine\"") && inv.toString().contains("Paper Press"),
                "inventory holds the controller, named from the config: " + inv);
        c.key("key.inventory");
        c.waitScreen("", 5000);
        c.waitTicks(10);
        c.screenshot("01-inventory");
        // the controller's name comes from the config
        JsonObject slot = findSlot(c, "hbm:custom_machine");
        Expect.that(slot != null, "controller slot");
        c.mouseMove(slot.get("x").getAsDouble() + 8, slot.get("y").getAsDouble() + 8);
        c.waitTicks(4);
        c.screenshot("02-inventory-controller-tooltip");
        JsonObject wand = findSlot(c, "hbm:structure_custommachine");
        c.mouseMove(wand.get("x").getAsDouble() + 8, wand.get("y").getAsDouble() + 8);
        c.waitTicks(4);
        c.screenshot("03-inventory-wand-tooltip");
        close(c);
        c.cmd("clear");
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }

    /** Every block of the family in a row south of the machine, plus the controller. */
    private static void row(Client c) {
        int z = Z + 10;
        for (int i = 0; i < BLOCKS.length; i++) {
            int x = X - 13 + i;
            Fixtures.setBlock(c, x, Y + 1, z, BLOCKS[i]);
        }
        c.waitTicks(5);
        worldShot(c, "05-row-west", X - 7, Y + 3.5, z - 5.5, X - 7, Y + 1, z);
        worldShot(c, "06-row-east", X + 7, Y + 3.5, z - 5.5, X + 7, Y + 1, z);
    }

    private static void placeFromAbove(Client c, String item, int x, int y, int z) {
        Fixtures.giveOnly(c, item, 1);
        // three above the new block (4.6 from the face, inside vanilla's 5-block creative reach) and aimed at once:
        // the player falls after the teleport, and from two above it landed in the new block's cell before the click
        c.teleport(x + 0.5, y + 3, z + 0.5);
        c.lookAt(x + 0.5, y - 1 + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(3);
        Expect.block(c, x, y, z, item);
    }

    private static void openController(Client c) {
        c.teleport(CX + 0.5, Y + 1, CZ - 3.5);
        c.waitTicks(3);
        c.hotbar(9);
        c.waitTicks(1);
        c.lookAt(CX + 0.5, CY + 0.5, CZ + 0.02);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen("GUIMachineCustom", 5000);
        Expect.screenStaysOpen(c, "GUIMachineCustom", 10);
        c.waitTicks(5);
    }

    private static void structureWand(Client c) throws Exception {
        int ax = X + 8, az = Z - 8;
        Fixtures.giveOnly(c, "hbm:custom_machine_anchor", 1);
        c.teleport(ax + 0.5, Y + 1, az - 2.5);
        c.waitTicks(3);
        c.lookAt(ax + 0.5, Y + 0.98, az + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(3);
        Expect.block(c, ax, Y + 1, az, "hbm:custom_machine_anchor");
        Fixtures.setBlock(c, ax, Y + 1, az + 1, "hbm:cm_block_steel");
        Fixtures.setBlock(c, ax + 1, Y + 1, az + 1, "hbm:cm_engine_standard");
        Fixtures.setBlock(c, ax, Y + 2, az + 1, "hbm:cm_tank_desh");

        Fixtures.giveOnly(c, "hbm:structure_custommachine", 1);
        c.teleport(ax + 0.5, Y + 1, az - 2.5);
        c.waitTicks(3);
        c.lookAt(ax + 0.5, Y + 0.98, az + 0.5);
        c.waitTicks(4);
        c.screenshot("30-wand-no-anchor");
        c.lookAt(ax + 0.5, Y + 1.5, az + 0.02);
        c.waitTicks(1);
        c.use(2);             // the anchor
        c.waitTicks(3);
        c.lookAt(ax + 1.5, Y + 0.98, az + 1.5);
        c.waitTicks(1);
        c.use(2);             // position 1: the platform under the engine
        c.waitTicks(3);
        c.lookAt(ax + 0.5, Y + 2.5, az + 1.02);
        c.waitTicks(1);
        c.use(2);             // position 2: the tank
        c.waitTicks(4);
        c.screenshot("31-wand-overlay-anchor-and-corners");
        long before = System.currentTimeMillis() - 2000;
        c.markLog();
        c.use(2);             // third click writes the file
        c.waitTicks(10);
        Path out = gameDir(c).resolve("config/hbmConfig/CMstructureOutput.txt");
        Expect.that(Files.isRegularFile(out) && Files.getLastModifiedTime(out).toMillis() >= before,
                "the third click wrote CMstructureOutput.txt at " + out);
        // the server-thread log line is not in every node's harness log buffer; when it is, it names the anchor
        List<String> wrote = c.logSinceMark("Wrote custom machine structure");
        Expect.that(wrote.isEmpty() || wrote.get(0).contains("around anchor " + ax + " " + (Y + 1) + " " + az), "the wand kept the anchor: " + wrote);
        String json = Files.readString(out);
        Expect.that(json.contains("hbm:cm_engine_standard") && json.contains("hbm:cm_tank_desh")
                && !json.contains("custom_machine_anchor"), "structure output content: " + json);
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
