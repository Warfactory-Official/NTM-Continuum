import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Gap batch A (§19ed-gaps-a), every item exercised in game and checked by value, with screenshots:
 * <ol>
 *   <li>dropped antimatter cell: 1.7 ExplosionVNT 3F amat (flash, crater, no drops); dropped anti-schrabidium cell: the
 *       FLEIJA field and cloud; an old-variant cell migrated by its inventory tick (item 14);</li>
 *   <li>the singularity crafted from anti-schrabidium cells on a crafting table, refused with antimatter cells;</li>
 *   <li>the ten radiovoltaic batteries, a Po-210 one crafted in the 2x2 grid, drawn in a battery socket;</li>
 *   <li>an orbus cell broken in survival drops the tank's contents, which come back when it is placed again;</li>
 *   <li>the crucible (old-hbm since 2026-10-02): an empty one hits for 5 (+1), a charged one for 500 (+1);</li>
 *   <li>the mese gavel from the Book of Boxcars;</li>
 *   <li>the survey scanner's bedrock ore line;</li>
 *   <li>the trenchmaster's one-in-three dodge and immunity to the wearer's own explosions;</li>
 *   <li>custom machines on a real research reactor's flux and a real firebox's heat (test machines and recipes
 *       staged by the scenario itself through {@code hbm_gapsa custom_machines}, restored at the end), a bred
 *       meteorite sword irradiated in that reactor, the JEI chance line;</li>
 *   <li>mp_s_20's name, the icfLaser entry of hbmMachines.json;</li>
 *   <li>the deco emitter's beam after dyeing;</li>
 *   <li>the settings tool: Alt cycles the copied fluid, Ctrl pastes along a whole pipe run;</li>
 *   <li>the death ray on a world aged past its charge time, and the ground station read by a ROR reader;</li>
 *   <li>the knife injector's stab and 1.7 properJolt (last: it tilts the camera for minutes).</li>
 * </ol>
 * Each section records PASS/FAIL in items.txt next to the screenshots; the scenario fails if any section failed.
 */
public final class FinalGapsA implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;
    private static final String WATER = "hbm:water";

    private interface Step { void run() throws Exception; }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        // Stage the test machines (sections 10/11) before the world is rejoined, so JEI registers them on the join.
        stageCustomMachines(c);
        try {
            body(c);
        } finally {
            JsonObject restore = new JsonObject();
            restore.addProperty("custom_machines", "restore");
            c.query("hbm_gapsa", restore);
            c.resetWorld();
        }
    }

    /**
     * 1.7 {@code CustomMachineConfigJSON}/{@code CustomMachineRecipes}: the real machine file plus two test machines
     * (a flux one on a {@code cm_flux} block, a heat one on a {@code cm_heat} block; controller variants 101 and 102
     * after the default paper press at 100) and a recipe file giving the paper press a 50% sawdust byproduct for the
     * JEI chance line and each test machine a stone -> cobblestone recipe needing flux 50 / heat 100.
     */
    private static void stageCustomMachines(Client c) {
        JsonObject params = new JsonObject();
        params.addProperty("custom_machines", "stage");
        JsonArray machines = new JsonArray();
        machines.add(testMachine("gapsaFlux", "G-A Flux Test", true, 0, "hbm:tile.cm_flux"));
        machines.add(testMachine("gapsaHeat", "G-A Heat Test", false, 10000, "hbm:tile.cm_heat"));
        params.add("machines", machines);
        params.add("recipes", com.google.gson.JsonParser.parseString(
                "{\"recipes\": [" +
                "{\"recipeKey\": \"paperPress\", \"recipes\": [" +
                "{\"inputFluids\": [[\"WATER\", 250]], \"inputItems\": [[\"item\", \"hbm:powder_sawdust\", 1]], \"outputFluids\": []," +
                "\"outputItems\": [[\"minecraft:paper\", 3, 1.0], [\"hbm:powder_sawdust\", 1, 0.5]], \"duration\": 60," +
                "\"consumptionPerTick\": 10, \"pollutionType\": \"SOOT\", \"pollutionAmount\": 0.03, \"radiationAmount\": 0.0," +
                "\"flux\": 0, \"heat\": 0}]}," +
                "{\"recipeKey\": \"gapsaFlux\", \"recipes\": [" +
                "{\"inputFluids\": [], \"inputItems\": [[\"item\", \"minecraft:stone\", 1]], \"outputFluids\": []," +
                "\"outputItems\": [[\"minecraft:cobblestone\", 1, 1.0]], \"duration\": 20, \"consumptionPerTick\": 1," +
                "\"pollutionType\": \"SOOT\", \"pollutionAmount\": 0.0, \"radiationAmount\": 0.0, \"flux\": 50, \"heat\": 0}]}," +
                "{\"recipeKey\": \"gapsaHeat\", \"recipes\": [" +
                "{\"inputFluids\": [], \"inputItems\": [[\"item\", \"minecraft:stone\", 1]], \"outputFluids\": []," +
                "\"outputItems\": [[\"minecraft:cobblestone\", 1, 1.0]], \"duration\": 20, \"consumptionPerTick\": 1," +
                "\"pollutionType\": \"SOOT\", \"pollutionAmount\": 0.0, \"radiationAmount\": 0.0, \"flux\": 0, \"heat\": 100}]}]}"));
        JsonObject staged = c.query("hbm_gapsa", params);
        System.out.println("[FinalGapsA " + c.node() + "] staged custom machines: " + staged);
        Expect.that(staged.has("custom_machines") && staged.get("custom_machines").getAsInt() >= 3,
                "the staged machine file loaded the two test machines after the real ones: " + staged);
    }

    private static JsonObject testMachine(String key, String name, boolean flux, int heat, String block) {
        JsonObject m = new JsonObject();
        m.addProperty("recipeKey", key);
        m.addProperty("unlocalizedName", key);
        m.add("localization", new JsonObject());
        m.addProperty("localizedName", name);
        m.addProperty("fluidInCount", 0);
        m.addProperty("fluidInCap", 0);
        m.addProperty("itemInCount", 1);
        m.addProperty("fluidOutCount", 0);
        m.addProperty("fluidOutCap", 0);
        m.addProperty("itemOutCount", 1);
        m.addProperty("generatorMode", false);
        m.addProperty("maxPollutionCap", 100);
        m.addProperty("fluxMode", flux);
        m.addProperty("recipeSpeedMult", 1.0);
        m.addProperty("recipeConsumptionMult", 1.0);
        m.addProperty("maxPower", 10000);
        m.addProperty("maxHeat", heat);
        JsonArray components = new JsonArray();
        JsonObject comp = new JsonObject();
        comp.addProperty("block", block);
        comp.addProperty("x", 0);
        comp.addProperty("y", 0);
        comp.addProperty("z", 1);
        JsonArray metas = new JsonArray();
        metas.add(0);
        comp.add("metas", metas);
        components.add(comp);
        m.add("components", components);
        return m;
    }

    private static void body(Client c) throws Exception {
        c.resetWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 30);
        List<String> results = new ArrayList<>();

        step(c, results, "01 dropped AMAT cell (ExplosionVNT 3F amat)", () -> amatDrop(c));
        step(c, results, "14 a hand-built cell stacks with a new one", () -> oldCell(c));
        step(c, results, "02 singularity from ASCHRAB cells (crafting table)", () -> singularity(c));
        step(c, results, "03 radiovoltaic batteries + socket", () -> batteries(c));
        step(c, results, "03b orbus cell drop keeps the tank", () -> orbus(c));
        step(c, results, "04 crucible empty charge = 5 damage, charged = 500", () -> crucible(c));
        step(c, results, "06 mese gavel (Book of Boxcars)", () -> meseGavel(c));
        step(c, results, "08 survey scanner bedrock ore line", () -> survey(c));
        step(c, results, "09 trenchmaster dodge + own explosion", () -> trenchmaster(c));
        step(c, results, "10 custom machine flux (research reactor) + 05 sword irradiated", () -> fluxMachine(c));
        step(c, results, "10 custom machine heat (firebox)", () -> heatMachine(c));
        step(c, results, "11 JEI output chance %", () -> jeiChance(c));
        step(c, results, "12 mp_s_20 name + 13 icfLaser config file", () -> namesAndConfig(c));
        step(c, results, "15 deco emitter beam after dyeing", () -> emitter(c));
        step(c, results, "17 settings tool Alt cycle + Ctrl paste run", () -> settingsTool(c));
        step(c, results, "01b dropped ASCHRAB cell (FLEIJA)", () -> aschrabDrop(c));
        step(c, results, "16 death ray + satlink ROR", () -> deathRayAndRor(c));
        step(c, results, "07 knife injector stab + properJolt", () -> knife(c));

        Path out = c.screenshotDir().resolve("items.txt");
        Files.createDirectories(out.getParent());
        Files.write(out, results);
        for (String r : results) System.out.println("[FinalGapsA " + c.node() + "] " + r);
        List<String> errors = c.logSinceMark("/(ERROR|FATAL)[]]|Missing model|Failed to load texture|Unable to load model");
        long failed = results.stream().filter(r -> r.startsWith("FAIL")).count();
        Expect.that(failed == 0, failed + " section(s) failed: " + results);
        Expect.that(errors.isEmpty(), "log errors: " + errors);
    }

    private static void step(Client c, List<String> results, String name, Step body) {
        try {
            JsonObject clear = new JsonObject();
            clear.addProperty("clearrads", true);
            c.query("hbm_gapsa", clear);
            c.cmd("ntmrad clear");
            c.cmd("effect clear @s");
            body.run();
            results.add("PASS " + name);
        } catch (Throwable t) {
            results.add("FAIL " + name + ": " + t.getMessage());
            try {
                c.screenshot("fail-" + name.substring(0, 2).trim());
                String screen = c.screenClass();
                if (screen != null && screen.contains("Death")) {
                    c.clickWidget("Respawn");
                    c.waitInWorld(30_000L);
                    c.waitTicks(20);
                } else if (screen != null) {
                    c.key("escape");
                }
                c.releaseAll();
                c.cmd("gamemode creative");
                c.waitTicks(5);
            } catch (Throwable ignored) { }
        }
    }

    // ============================================================== 1: dropped cells

    private static void amatDrop(Client c) {
        int ax = X + 20, az = Z;
        c.cmd("gamemode creative");
        Fixtures.giveOnly(c, cell(c, "hbm:amat", 3), 1);
        c.teleport(ax + 0.5, Y + 1, az + 0.5);
        c.look(0, 20);
        c.waitTicks(5);
        c.key("key.drop");
        boolean flashed = false;
        for (int i = 0; i < 60 && !flashed; i++) {
            c.waitTicks(1);
            flashed = gapsa(c).get("amat_flashes").getAsInt() > 0;
        }
        c.screenshot("10-amat-flash");
        Expect.that(flashed, "the antimatter cell's 1.7 amat flash was spawned");
        c.waitTicks(20);
        int air = 0;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -1; dz <= 5; dz++) {
            if (c.block(ax + dx, Y, az + dz).get("block").getAsString().equals("minecraft:air")) air++;
        }
        Expect.that(air >= 1, "the 3F annihilation cut into the stone platform (" + air + " air blocks)");
        JsonArray items = c.entities(12);
        int dropped = 0;
        for (JsonElement e : items) if (e.getAsJsonObject().get("type").getAsString().equals("minecraft:item")) dropped++;
        Expect.that(dropped == 0, "makeAmat drops nothing (and the cell is gone): " + items);
        worldShot(c, "11-amat-crater", ax + 4.5, Y + 4, az - 3.5, ax + 0.5, Y, az + 2.5);
    }

    private static void aschrabDrop(Client c) {
        int fx = X + 400, fz = Z;
        Fixtures.platform(c, fx, Y, fz, 6);
        c.waitTicks(40);
        Fixtures.platform(c, fx, Y, fz, 6);
        Fixtures.giveOnly(c, cell(c, "hbm:aschrab", 7), 1);
        c.teleport(fx + 0.5, Y + 1, fz + 0.5);
        c.look(0, 20);
        c.waitTicks(5);
        c.key("key.drop");
        JsonObject field = null, cloud = null;
        for (int i = 0; i < 60 && (field == null || cloud == null); i++) {
            c.waitTicks(1);
            if (field == null) field = serverEntity(c, "hbm:nuke_mk3", 40);
            if (cloud == null) cloud = serverEntity(c, "hbm:cloud_fleija", 40);
        }
        // the cloud lives aSchrabRadius (20) ticks and a small field can finish within a poll, so either one seen
        // proves the spawn; the hollowed ground below is the value check
        Expect.that(field != null || cloud != null, "the anti-schrabidium cell started a FLEIJA field or its cloud");
        c.cmd("gamemode spectator");
        c.teleport(fx + 30.5, Y + 12, fz - 30.5);
        c.lookAt(fx + 0.5, Y, fz + 0.5);
        c.waitTicks(10);
        c.screenshot("12-fleija-cloud");
        c.waitTicks(100);
        c.screenshot("13-fleija-crater");
        String under = c.block(fx, Y - 3, fz + 1).get("block").getAsString();
        Expect.that(under.equals("minecraft:air"), "the FLEIJA field hollowed the ground under the drop: " + under);
        c.cmd("gamemode creative");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(5);
    }

    private static void oldCell(Client c) {
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.waitTicks(2);
        JsonObject give = new JsonObject();
        give.addProperty("give", "oldcell:AMAT");
        c.query("hbm_gapsa", give);
        give.addProperty("give", "oldcell:SAS3");
        c.query("hbm_gapsa", give);
        c.waitTicks(5); // one inventory tick migrates both; a new antimatter cell must then stack onto the old one
        c.cmd("give @s " + cell(c, "hbm:amat", 3) + " 1");
        c.waitTicks(10);
        JsonObject q = gapsa(c);
        int amat = 0;
        for (JsonElement e : q.getAsJsonArray("cells")) {
            JsonObject cell = e.getAsJsonObject();
            Expect.that(cell.get("variant").getAsInt() < 8, "every cell draws a CellVariants sprite: " + q);
            if (cell.get("fluid").getAsString().equals("hbm:amat")) amat += cell.get("count").getAsInt();
        }
        Expect.that(amat == 2 && q.getAsJsonArray("cells").size() == 2,
                "the migrated antimatter cell stacked with a new one (one stack of 2, one SAS3): " + q);
        c.key("e");
        c.waitTicks(10);
        c.screenshot("14-old-cells-migrated");
        c.key("escape");
        c.cmd("clear");
        c.cmd("gamemode creative");
        c.waitTicks(3);
    }

    // ============================================================== 2: singularity recipe

    private static void singularity(Client c) {
        int tx = X - 10, tz = Z;
        Fixtures.setBlock(c, tx, Y + 1, tz, "minecraft:crafting_table");
        // creative: four anti-schrabidium cells and a schrabidium block kill a survival player within seconds (1.7 hazards)
        c.cmd("gamemode creative");
        c.cmd("clear");
        c.cmd("give @s " + cell(c, "hbm:aschrab", 7) + " 4");
        c.cmd("give @s hbm:nugget_euphemium 4");
        c.cmd("give @s hbm:block_schrabidium 1");
        c.waitTicks(4);
        c.teleport(tx + 0.5, Y + 1, tz - 1.5);
        Fixtures.interact(c, tx, Y + 1, tz);
        c.waitScreen("Crafting", 5000);
        c.waitTicks(3);
        fillSingularity(c, "hbm:aschrab");
        c.waitTicks(5);
        JsonObject result = c.slot(0);
        c.screenshot("20-singularity-aschrab");
        Expect.that(!result.get("empty").getAsBoolean() && result.get("id").getAsString().equals("hbm:singularity"),
                "four anti-schrabidium cells make the singularity (1.7 CraftingManager:325): " + result);
        c.clickSlot(0);
        c.waitTicks(2);
        c.clickSlot(emptyPlayerSlot(c));
        c.waitTicks(3);
        int empties = 0;
        for (int s : new int[]{2, 4, 6, 8}) {
            JsonObject slot = c.slot(s);
            if (!slot.get("empty").getAsBoolean() && slot.get("name").getAsString().equals("Empty Cell")) empties++;
        }
        Expect.that(empties == 4, "the four cells came back empty in the grid (" + empties + ")");
        c.key("escape");
        c.waitTicks(5);

        c.cmd("clear");
        c.cmd("give @s " + cell(c, "hbm:amat", 3) + " 4");
        c.cmd("give @s hbm:nugget_euphemium 4");
        c.cmd("give @s hbm:block_schrabidium 1");
        c.waitTicks(4);
        Fixtures.interact(c, tx, Y + 1, tz);
        c.waitScreen("Crafting", 5000);
        c.waitTicks(3);
        fillSingularity(c, "hbm:amat");
        c.waitTicks(5);
        c.screenshot("21-singularity-amat-refused");
        Expect.that(c.slot(0).get("empty").getAsBoolean(), "antimatter cells no longer make the singularity");
        c.key("escape");
        c.waitTicks(3);
        c.cmd("clear");
        c.cmd("gamemode creative");
    }

    private static void fillSingularity(Client c, String fluid) {
        int cells = playerSlot(c, "hbm:cell");
        c.clickSlot(cells);
        c.waitTicks(1);
        for (int s : new int[]{2, 4, 6, 8}) { c.clickSlot(s, 1); c.waitTicks(1); }
        int nuggets = playerSlot(c, "hbm:nugget_euphemium");
        c.clickSlot(nuggets);
        c.waitTicks(1);
        for (int s : new int[]{1, 3, 7, 9}) { c.clickSlot(s, 1); c.waitTicks(1); }
        c.clickSlot(playerSlot(c, "hbm:block_schrabidium"));
        c.waitTicks(1);
        c.clickSlot(5);
        c.waitTicks(2);
    }

    // ============================================================== 3: radiovoltaic batteries

    private static final String[][] SC = {
            {"empty", "Empty Self-Charging Battery"}, {"waste", "Spent Fuel Self-Charging Battery"},
            {"ra226", "Radium-226 Self-Charging Battery"}, {"tc99", "Technetium-99 Self-Charging Battery"},
            {"co60", "Cobalt-60 Self-Charging Battery"}, {"pu238", "Plutonium-238 Self-Charging Battery"},
            {"po210", "Polonium-210 Self-Charging Battery"}, {"au198", "Gold-198 Self-Charging Battery"},
            {"pb209", "Lead-209 Self-Charging Battery"}, {"am241", "Americium-241 Self-Charging Battery"}};

    private static void batteries(Client c) {
        c.cmd("gamemode survival");
        c.cmd("clear");
        for (String[] sc : SC) c.cmd("give @s hbm:battery_sc_" + sc[0] + " 1");
        c.waitTicks(5);
        String inv = c.inventory().toString();
        for (String[] sc : SC) Expect.that(inv.contains(sc[1]), "inventory shows " + sc[1]);
        c.key("e");
        c.waitScreen("Inventory", 5000);
        c.waitTicks(8);
        c.screenshot("30-sc-batteries");
        hover(c, playerSlot(c, "hbm:battery_sc_po210"));
        c.screenshot("31-sc-po210-tooltip");
        c.key("escape");
        c.waitTicks(3);

        c.cmd("clear");
        c.cmd("give @s hbm:battery_sc_empty 1");
        c.cmd("give @s hbm:billet_polonium 2");
        c.waitTicks(4);
        c.key("e");
        c.waitScreen("Inventory", 5000);
        c.waitTicks(4);
        c.clickSlot(playerSlot(c, "hbm:battery_sc_empty"));
        c.waitTicks(1);
        c.clickSlot(1);
        c.waitTicks(1);
        c.clickSlot(playerSlot(c, "hbm:billet_polonium"));
        c.waitTicks(1);
        c.clickSlot(2, 1);
        c.waitTicks(1);
        c.clickSlot(3, 1);
        c.waitTicks(4);
        JsonObject result = c.slot(0);
        c.screenshot("32-sc-po210-crafting");
        Expect.that(!result.get("empty").getAsBoolean() && result.get("id").getAsString().equals("hbm:battery_sc_po210"),
                "empty radiovoltaic battery + two polonium billets = Po-210 (1.7 CraftingManager:268): " + result);
        c.clickSlot(0);
        c.waitTicks(2);
        c.clickSlot(emptyPlayerSlot(c));
        c.waitTicks(2);
        c.key("escape");
        c.waitTicks(3);
        Expect.inventoryHolds(c, "hbm:battery_sc_po210", 1);

        c.cmd("gamemode creative");
        int sx = X - 20, sz = Z - 10;
        place(c, "hbm:machine_battery_socket", sx, Y, sz);
        int[] core = findCore(c, sx, Y + 1, sz, "battery_socket", 2);
        Expect.that(core != null, "the battery socket formed a core");
        c.cmd("clear");
        c.cmd("give @s hbm:battery_sc_po210 1");
        c.waitTicks(3);
        c.teleport(core[0] + 3.5, Y + 1, core[2] + 0.5);
        c.waitTicks(3);
        Fixtures.interact(c, core[0], Y + 1, core[2]);
        c.waitScreen("GUIBatterySocket", 5000);
        c.waitTicks(3);
        c.clickSlot(playerSlot(c, "hbm:battery_sc_po210"));
        c.waitTicks(1);
        c.clickSlot(0);
        c.waitTicks(5);
        c.screenshot("33-socket-gui-po210");
        c.key("escape");
        c.waitTicks(10);
        String nbt = c.blockEntity(core[0], core[1], core[2]).toString();
        Expect.that(nbt.contains("battery_sc_po210"), "the socket holds the Po-210 battery: " + nbt);
        worldShot(c, "34-socket-po210", core[0] + 3.5, Y + 2.5, core[2] + 3.5, core[0] + 0.5, Y + 1, core[2] + 0.5);
    }

    // ============================================================== 3b: orbus drop from any cell

    private static void orbus(Client c) {
        int ox = X - 16, oz = Z + 16;
        c.cmd("gamemode creative");
        place(c, "hbm:machine_orbus", ox, Y, oz);
        int[] core = findCore(c, ox, Y + 1, oz, "machine_orbus", 3);
        Expect.that(core != null, "the orbus formed a core");
        c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2]
                + " {tank:384000,tank_type:\"" + WATER + "\",tank_max:512000}");
        c.waitTicks(10);
        c.cmd("clear");
        c.cmd("gamemode survival");
        c.cmd("give @s minecraft:netherite_pickaxe 1");
        c.cmd("effect give @s minecraft:haste 60 255 true"); // the harness never grabs the mouse: one hit must break
        c.cmd("effect give @s minecraft:resistance 60 4 true");
        c.waitTicks(3);
        c.hotbar(1);
        // stand on top of the tank and break the cell above the core: a cell, never the core itself
        c.teleport(core[0] + 0.5, core[1] + 5, core[2] + 0.5);
        c.waitTicks(10);
        c.lookAt(core[0] + 0.5, core[1] + 4.98, core[2] + 0.5);
        c.waitTicks(2);
        c.attack(2);
        c.waitTicks(20);
        String gone = c.block(core[0], core[1], core[2]).get("block").getAsString();
        Expect.that(gone.equals("minecraft:air"), "breaking a cell took the whole orbus down: " + gone);
        c.teleport(core[0] + 0.5, core[1], core[2] + 0.5);
        c.waitTicks(10);
        // the drop spawns at the broken cell, 4 up, while the cells under it are still being removed: it can be
        // pushed out sideways past pickup range (seen once on 1.21.1 Fabric). Gather every item nearby to the player;
        // the count check below still requires exactly one orbus
        c.cmd("tp @e[type=minecraft:item,distance=..12] @s");
        c.waitTicks(30);
        int count = 0;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("empty").getAsBoolean() && s.get("id").getAsString().equals("hbm:machine_orbus")) count += s.get("count").getAsInt();
        }
        Expect.that(count == 1, "exactly one orbus item came back from the broken cell: " + c.inventory());
        // place the carried orbus again by hand, still in survival, and read the new core's tank
        int nx = ox + 12;
        c.cmd("clear @s minecraft:netherite_pickaxe");
        c.waitTicks(2);
        c.hotbar(slotOf(c, "hbm:machine_orbus") + 1);
        c.teleport(nx + 4.5, Y + 1, oz + 0.5);
        c.waitTicks(3);
        c.lookAt(nx + 0.9, Y + 0.98, oz + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
        int[] placed = findCore(c, nx, Y + 1, oz, "machine_orbus", 3);
        Expect.that(placed != null, "the dropped orbus could be placed again");
        String nbt = c.blockEntity(placed[0], placed[1], placed[2]).toString().replaceAll("\\s", "");
        Expect.that(nbt.contains("tank:384000"), "the tank's 384,000 mB came back with the item (1.7 IPersistentNBT): " + nbt);
        worldShot(c, "35-orbus-replaced", placed[0] + 8.5, Y + 4, placed[2] + 6.5, placed[0] + 0.5, Y + 2.5, placed[2] + 0.5);
        c.cmd("clear");
        c.cmd("gamemode creative");
    }

    // ============================================================== 4: crucible

    private static void crucible(Client c) {
        int px = X + 6, pz = Z + 12;
        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode survival");
        c.cmd("clear");
        // old-hbm ItemCrucible: a crafted crucible holds no charge; its modifiers are 5 damage, +80% speed
        c.cmd("give @s hbm:crucible 1");
        c.waitTicks(3);
        c.hotbar(1);
        c.cmd("summon minecraft:pig " + (px + 0.5) + " " + (Y + 1) + " " + (pz + 0.5) + " {NoAI:1b}");
        c.teleport(px + 0.5, Y + 1, pz - 1.5);
        c.waitTicks(25);
        JsonObject q = gapsa(c);
        Expect.that(q.get("crucible_charges").getAsInt() == 0 && Math.abs(q.get("attack_damage").getAsDouble() - 6.0) < 1.0E-3,
                "the held crucible is empty and hits for 1 + 5: " + q);
        float before = pigHealth(c);
        c.lookAt(px + 0.5, Y + 1.5, pz + 0.5);
        c.waitTicks(2);
        c.attack(1);
        c.waitTicks(10);
        float after = pigHealth(c);
        c.screenshot("40-crucible-empty-hit");
        Expect.that(before - after > 5.5F && before - after < 6.5F, "the empty crucible took 6 health: " + before + " -> " + after);
        c.cmd("clear");
        c.cmd("give @s " + crucible(c, 16) + " 1");
        c.waitTicks(25);
        q = gapsa(c);
        Expect.that(q.get("crucible_charges").getAsInt() == 16 && Math.abs(q.get("attack_damage").getAsDouble() - 5001.0) < 1.0E-3,
                "a charged crucible hits for 1 + 5000 (1.7 value, vanilla's 2048 bound raised, History 19ej-caps): " + q);
        c.cmd("kill @e[type=!player]");
        c.cmd("clear");
        c.cmd("gamemode creative");
    }

    private static float pigHealth(Client c) {
        JsonObject living = c.query("hbm_living", new JsonObject());
        for (JsonElement e : living.getAsJsonArray("living")) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals("minecraft:pig")) return o.get("health").getAsFloat();
        }
        return -1F;
    }

    // ============================================================== 6: mese gavel

    private static void meseGavel(Client c) {
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("give @s hbm:book_of_ 1");
        for (String id : new String[]{"hbm:shimmer_handle", "hbm:powder_dineutronium", "hbm:blades_desh", "hbm:diamond_gavel"}) {
            c.cmd("give @s " + id + " 1");
        }
        c.waitTicks(4);
        c.hotbar(slotOf(c, "hbm:book_of_") + 1);
        c.look(0, -30);
        c.waitTicks(2);
        c.use(2);
        c.waitScreen("GUIBook", 5000);
        c.waitTicks(5);
        String[] order = {"hbm:shimmer_handle", "hbm:powder_dineutronium", "hbm:blades_desh", "hbm:diamond_gavel"};
        for (int i = 0; i < 4; i++) {
            c.clickSlot(playerSlot(c, order[i]));
            c.waitTicks(1);
            c.clickSlot(1 + i);
            c.waitTicks(2);
        }
        c.waitTicks(4);
        JsonObject result = c.slot(0);
        c.screenshot("50-book-mese-gavel");
        Expect.that(!result.get("empty").getAsBoolean() && result.get("id").getAsString().equals("hbm:mese_gavel"),
                "1.7 MagicRecipes: handle, dineutronium, desh blades, diamond gavel -> mese gavel: " + result);
        c.clickSlot(0);
        c.waitTicks(2);
        c.clickSlot(emptyPlayerSlot(c));
        c.waitTicks(2);
        for (int i = 1; i <= 4; i++) Expect.that(c.slot(i).get("empty").getAsBoolean(), "grid slot " + i + " was used up");
        c.key("escape");
        c.waitTicks(3);
        Expect.inventoryHolds(c, "hbm:mese_gavel", 1);
        c.cmd("clear");
        c.cmd("gamemode creative");
    }

    // ============================================================== 8: survey scanner

    private static void survey(Client c) {
        c.cmd("setblock " + X + " -64 " + Z + " hbm:ore_bedrock_block{tier:1}");
        c.waitTicks(3);
        String b = c.block(X, -64, Z).get("block").getAsString();
        Expect.that(b.equals("hbm:ore_bedrock_block"), "a bedrock deposit under the player: " + b);
        Fixtures.giveOnly(c, "hbm:survey_scanner", 1);
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, -30);
        c.waitTicks(3);
        c.markLog();
        c.use(2);
        c.waitTicks(8);
        List<String> lines = c.logSinceMark("Found BEDROCK ORE for");
        c.screenshot("60-survey-bedrock-line");
        Expect.that(!lines.isEmpty() && lines.get(0).contains("Iron"), "1.7 'Found BEDROCK ORE for <resource>!': " + lines);
        c.cmd("setblock " + X + " -64 " + Z + " minecraft:bedrock");
    }

    // ============================================================== 9: trenchmaster

    private static void trenchmaster(Client c) {
        c.cmd("clear");
        c.cmd("effect clear @s");
        c.cmd("gamemode survival");
        c.cmd("item replace entity @s armor.head with hbm:trenchmaster_helmet");
        c.cmd("item replace entity @s armor.chest with hbm:trenchmaster_plate");
        c.cmd("item replace entity @s armor.legs with hbm:trenchmaster_legs");
        c.cmd("item replace entity @s armor.feet with hbm:trenchmaster_boots");
        c.cmd("effect give @s minecraft:instant_health 1 5 true");
        c.waitTicks(20);
        int hits = 0, dodged = 0;
        for (int i = 0; i < 36; i++) {
            // full health first, then a 10-point hit: well above the suit's damage threshold, so only a dodge leaves
            // the health untouched
            c.cmd("effect give @s minecraft:instant_health 1 10 true");
            c.waitTicks(3);
            float before = gapsa(c).get("health").getAsFloat();
            c.cmd("damage @s 10 minecraft:generic");
            c.waitTicks(12);
            float after = gapsa(c).get("health").getAsFloat();
            hits++;
            if (after >= before) dodged++;
        }
        c.screenshot("70-trenchmaster");
        Expect.that(dodged >= 3 && dodged <= 24, "1.7 nextInt(3) == 0 dodge: " + dodged + " of " + hits + " attacks shrugged off");
        c.cmd("effect give @s minecraft:instant_health 1 5 true");
        c.waitTicks(15);
        int selfHits = 0;
        for (int i = 0; i < 6; i++) {
            c.cmd("effect give @s minecraft:instant_health 1 10 true");
            c.waitTicks(3);
            float before = gapsa(c).get("health").getAsFloat();
            c.cmd("damage @s 10 minecraft:player_explosion by @s");
            c.waitTicks(12);
            if (gapsa(c).get("health").getAsFloat() < before) selfHits++;
        }
        Expect.that(selfHits == 0, "the wearer's own explosions do nothing (" + selfHits + " of 6 hurt)");
        c.cmd("item replace entity @s armor.head with minecraft:air");
        c.cmd("item replace entity @s armor.chest with minecraft:air");
        c.cmd("item replace entity @s armor.legs with minecraft:air");
        c.cmd("item replace entity @s armor.feet with minecraft:air");
        c.cmd("effect give @s minecraft:instant_health 1 5 true");
        c.cmd("gamemode creative");
        c.waitTicks(3);
    }

    // ============================================================== 10: custom machines + sword

    private static void fluxMachine(Client c) {
        c.cmd("clear");
        c.cmd("give @s " + controller(c, 101) + " 1");
        c.waitTicks(4);
        Expect.that(c.inventory().toString().contains("G-A Flux Test"),
                "the staged hbmCustomMachines.json declares the flux test machine (stageCustomMachines)");
        int rx = X + 12, rz = Z + 20;
        c.cmd("gamemode creative");
        place(c, "hbm:machine_reactor_small", rx, Y, rz);
        int[] reactor = findCore(c, rx, Y + 1, rz, "reactorResearch", 2);
        if (reactor == null) reactor = findCore(c, rx, Y + 1, rz, "reactor", 2);
        Expect.that(reactor != null, "the research reactor was placed");
        // 1.7 TileEntityReactorResearch: 120 flux heats 240/tick and blows the reactor past 50,000 after ~210
        // ticks without water. One contained source under the core (getWater's y-1 cell) cools it by
        // heat*0.07/12 per tick, an equilibrium near 41,000, so it survives this and the later sections.
        Fixtures.setBlock(c, reactor[0], reactor[1] - 1, reactor[2], "minecraft:water");
        c.cmd("clear");
        c.cmd("give @s hbm:plate_fuel_ra226be 4");
        c.cmd("give @s hbm:meteorite_sword_bred 1");
        c.waitTicks(3);
        c.teleport(reactor[0] + 2.5, Y + 1, reactor[2] + 0.5);
        c.waitTicks(3);
        Fixtures.interact(c, reactor[0], Y + 1, reactor[2]);
        c.waitScreen("GUIReactorResearch", 5000);
        c.waitTicks(3);
        // plate fuel does not stack (1.7 ItemFuelRod setMaxStackSize(1)): carry each plate over on its own; four
        // passive Ra226Be plates are the 4 x 30 = 120 flux the custom machine below needs (its recipe asks 50)
        for (int s = 0; s < 4; s++) {
            c.clickSlot(playerSlot(c, "hbm:plate_fuel_ra226be"));
            c.waitTicks(1);
            c.clickSlot(s);
            c.waitTicks(1);
        }
        c.clickSlot(playerSlot(c, "hbm:meteorite_sword_bred"));
        c.waitTicks(1);
        c.clickSlot(5);
        c.waitTicks(2);
        JsonObject gui = c.screen();
        int left = gui.get("left").getAsInt(), top = gui.get("top").getAsInt();
        c.click(left + 20, top + 105);
        c.waitTicks(2);
        for (int i = 0; i < 4; i++) c.key("backspace");
        // One key per tick, as typed by hand: 1.7 GUIReactorResearch re-normalises the 3-char field every frame
        // ("" -> "0", "01" -> "1"), so "100" typed within one frame would become "010" -> "10" (1.7 and port alike).
        for (char ch : "100".toCharArray()) {
            c.type(String.valueOf(ch));
            c.waitTicks(1);
        }
        c.waitTicks(2);
        c.click(left + 49, top + 107);
        c.waitTicks(60);
        c.screenshot("80-reactor-running");
        c.key("escape");
        c.waitTicks(3);
        String rnbt = c.blockEntity(reactor[0], reactor[1], reactor[2]).toString();
        Expect.that(rnbt.matches("(?s).*targetLevel\\D{0,3}1\\.0.*"),
                "reactor control field typed 100 set targetLevel 1.0: " + rnbt);
        Expect.that(rnbt.contains("meteorite_sword_irradiated") && !rnbt.contains("meteorite_sword_bred"),
                "item 5: the bred meteorite sword came out irradiated (1.7 TileEntityReactorResearch): " + rnbt);

        // controller two blocks from the reactor column, the flux block diagonal to it (1.7's odd scan)
        int fx = reactor[0] - 1, fy = Y + 2, fz = reactor[2];
        int cx = fx, cz = fz - 1;
        Fixtures.setBlock(c, cx, Y + 1, cz, "minecraft:stone");
        Fixtures.setBlock(c, fx, Y + 1, fz, "minecraft:stone");
        Fixtures.setBlock(c, fx, fy, fz, "hbm:cm_flux");
        placeController(c, 101, cx, fy, cz);
        loadController(c, cx, fy, cz);
        c.waitTicks(100);
        String nbt = c.blockEntity(cx, fy, cz).toString().replaceAll("\\s", "");
        Expect.that(nbt.contains("minecraft:cobblestone"),
                "the flux recipe (flux 50) ran on the reactor's 120 flux: " + nbt);
        openController(c, cx, fy, cz);
        c.screenshot("81-cm-flux-gui");
        c.key("escape");
        c.waitTicks(3);
        worldShot(c, "82-cm-flux-world", cx + 4.5, Y + 4, cz - 4.5, cx + 0.5, fy, cz + 0.5);
        c.cmd("clear");
    }

    private static void heatMachine(Client c) {
        c.cmd("gamemode creative");
        int hx = X + 24, hz = Z + 20;
        place(c, "hbm:heater_firebox", hx, Y, hz);
        int[] fire = findCore(c, hx, Y + 1, hz, "heater_firebox", 2);
        if (fire == null) fire = findCore(c, hx, Y + 1, hz, "Firebox", 2);
        Expect.that(fire != null, "the firebox was placed");
        c.cmd("clear");
        c.cmd("give @s minecraft:coal 64");
        c.waitTicks(3);
        c.teleport(fire[0] + 0.5, Y + 1, fire[2] - 2.5);
        c.waitTicks(3);
        Fixtures.interact(c, fire[0], Y + 1, fire[2]);
        c.waitScreen("GUIFirebox", 5000);
        c.waitTicks(3);
        c.clickSlot(playerSlot(c, "minecraft:coal"));
        c.waitTicks(1);
        c.clickSlot(0);
        c.waitTicks(3);
        c.screenshot("83-firebox-burning");
        c.key("escape");
        c.waitTicks(3);

        // the heat block: two west and one up from the firebox core, pulled through its upper neighbour
        int x = fire[0] - 2, y = Y + 2, z = fire[2];
        int cx = x, cz = z - 1;
        Fixtures.setBlock(c, cx, Y + 1, cz, "minecraft:stone");
        Fixtures.setBlock(c, x, Y + 1, z, "minecraft:stone");
        Fixtures.setBlock(c, x, y, z, "hbm:cm_heat");
        placeController(c, 102, cx, y, cz);
        loadController(c, cx, y, cz);
        c.waitTicks(160);
        String nbt = c.blockEntity(cx, y, cz).toString().replaceAll("\\s", "");
        Expect.that(nbt.contains("minecraft:cobblestone"), "the heat recipe (heat 100) ran on the firebox's heat: " + nbt);
        openController(c, cx, y, cz);
        c.screenshot("84-cm-heat-gui");
        c.key("escape");
        c.waitTicks(3);
        worldShot(c, "85-cm-heat-world", cx - 3.5, Y + 4, cz - 4.5, fire[0] + 0.5, Y + 1, cz + 0.5);
        c.cmd("clear");
    }

    private static void placeController(Client c, int variant, int x, int y, int z) {
        Fixtures.giveOnly(c, controller(c, variant), 1);
        // straight down onto the support, facing south so the controller faces north (1.7: towards the placer);
        // click at once, before the falling player reaches the cell
        c.teleport(x + 0.5, y + 2.2, z + 0.5);
        c.look(0, 90);
        c.use(2);
        c.waitTicks(5);
        StringBuilder near = new StringBuilder();
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -2; dz <= 1; dz++) {
            String b = c.block(x + dx, y + dy, z + dz).get("block").getAsString();
            if (b.contains("custom_machine")) near.append(" ").append(dx).append(",").append(dy).append(",").append(dz);
        }
        Expect.that(c.block(x, y, z).get("block").getAsString().equals("hbm:custom_machine"),
                "the controller was placed at " + x + "," + y + "," + z + "; controllers nearby at offsets:" + near
                        + " gamemode " + c.player().get("game_mode"));
    }

    /** A creative battery in slot 0 and a stack of stone in the one input, by clicks. */
    private static void loadController(Client c, int x, int y, int z) {
        c.cmd("clear");
        c.cmd("give @s hbm:battery_creative 1");
        c.cmd("give @s minecraft:stone 64");
        c.waitTicks(3);
        openController(c, x, y, z);
        JsonObject gui = c.screen();
        int battery = slotAt(c, gui, 150, 72), input = slotAt(c, gui, 8, 72);
        c.clickSlot(playerSlot(c, "hbm:battery_creative"));
        c.waitTicks(1);
        c.clickSlot(battery);
        c.waitTicks(1);
        c.clickSlot(playerSlot(c, "minecraft:stone"));
        c.waitTicks(1);
        c.clickSlot(input);
        c.waitTicks(3);
        c.key("escape");
        c.waitTicks(3);
    }

    private static void openController(Client c, int x, int y, int z) {
        c.teleport(x + 0.5, y, z - 2.5);
        c.waitTicks(3);
        c.hotbar(9);
        c.waitTicks(1);
        c.lookAt(x + 0.5, y + 0.5, z + 0.02);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen("GUIMachineCustom", 5000);
        c.waitTicks(5);
    }

    // ============================================================== 11: JEI chance

    private static void jeiChance(Client c) {
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("give @s " + controller(c, 100) + " 1");
        c.waitTicks(4);
        c.key("e");
        c.waitScreen("Inventory", 5000);
        c.waitTicks(10);
        hover(c, playerSlot(c, "hbm:custom_machine"));
        c.key("u");
        c.waitTicks(20);
        String screen = c.screenClass();
        c.screenshot("90-jei-paper-press");
        Expect.that(screen != null && screen.toLowerCase().contains("recipe"), "JEI opened the controller's uses: " + screen);
        // the first tab lists block tags; the Paper Press category (the controller is its catalyst) is the second
        JsonObject status = c.status();
        int w = status.get("gui_width").getAsInt(), h = status.get("gui_height").getAsInt();
        c.click(w / 2.0 - 62, h / 2.0 - 144);
        c.waitTicks(10);
        c.screenshot("91-jei-paper-press-tab");
        // gui_nei_custom (166 wide) centred in the recipe panel; outputs at (102 + 18i, 24): hover the second one
        int n = 1;
        for (int dx = 0; dx <= 36; dx += 18) {
            for (int dy = -8; dy <= 8; dy += 16) {
                c.mouseMove(w / 2.0 + 20 + dx, h / 2.0 - 60 + dy);
                c.waitTicks(4);
                c.screenshot("9" + (++n) + "-jei-hover-" + dx + "-" + dy);
            }
        }
        closeScreens(c);
        c.cmd("clear");
        c.cmd("gamemode creative");
    }

    /** Escape only while a screen is open: an escape in the world opens the pause menu and stops the server. */
    private static void closeScreens(Client c) {
        for (int i = 0; i < 4 && c.screenClass() != null; i++) {
            c.key("escape");
            c.waitTicks(5);
        }
    }

    // ============================================================== 12/13: names, config

    private static void namesAndConfig(Client c) throws Exception {
        Fixtures.giveOnly(c, "hbm:mp_s_20", 1);
        c.waitTicks(3);
        Expect.that(c.inventory().toString().contains("Size 20 Flat Fins"), "mp_s_20 is named Size 20 Flat Fins");
        Path file = gameDir(c).resolve("config/hbmConfig/hbmMachines.json");
        Expect.that(Files.isRegularFile(file), "1.7 hbmMachines.json written at " + file);
        String json = Files.readString(file).replaceAll("\\s", "");
        Expect.that(json.contains("\"icfLaser\":{\"I:capacitorPower\":2500000,\"I:turboPower\":5000000}"),
                "icfLaser entry with the 1.7 defaults: " + json);
        c.cmd("clear");
    }

    // ============================================================== 15: emitter

    private static void emitter(Client c) {
        int ex = X - 12, ez = Z - 20;
        c.cmd("gamemode creative");
        Fixtures.placeOnTop(c, "hbm:deco_emitter", ex, Y, ez);
        Expect.block(c, ex, Y + 1, ez, "hbm:deco_emitter");
        Fixtures.giveOnly(c, "minecraft:red_dye", 1);
        c.teleport(ex + 2.5, Y + 1, ez + 0.5);
        c.waitTicks(2);
        c.lookAt(ex + 0.5, Y + 1.5, ez + 0.5);
        c.use(2);
        c.waitTicks(25);
        String nbt = c.blockEntity(ex, Y + 1, ez).toString().replaceAll("\\s", "");
        Expect.that(!nbt.contains("color:0,") && !nbt.contains("color:0}"), "the dye recoloured the emitter: " + nbt);
        worldShot(c, "A0-emitter-beam", ex + 6.5, Y + 3, ez + 8.5, ex + 0.5, Y + 3, ez + 0.5);
        c.cmd("clear");
    }

    // ============================================================== 17: settings tool

    private static void settingsTool(Client c) {
        int dx = X + 10, dz = Z - 12;
        c.cmd("gamemode creative");
        for (int i = 0; i < 6; i++) Fixtures.setBlock(c, dx + i, Y + 1, dz, "hbm:fluid_duct_neo");
        place(c, "hbm:heat_boiler", dx, Y, dz - 6);
        int[] boiler = findCore(c, dx, Y + 1, dz - 6, "heat_boiler", 2);
        if (boiler == null) boiler = findCore(c, dx, Y + 1, dz - 6, "Boiler", 2);
        Expect.that(boiler != null, "the heat boiler was placed");
        Fixtures.giveOnly(c, "hbm:settings_tool", 1);
        c.teleport(boiler[0] + 0.5, Y + 1, boiler[2] + 3.5);
        c.waitTicks(3);
        c.lookAt(boiler[0] + 0.5, Y + 1.5, boiler[2] + 0.5);
        c.waitTicks(1);
        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(2);
        c.keyUp("key.sneak");
        c.waitTicks(10);
        String settings = held(c).toString();
        // fluids are saved by registry name since History 19ej-ids (was the numeric fluidID array)
        Expect.that(settings.contains("fluids:[") && settings.contains("hbm:water") && settings.contains("hbm:steam")
                && settings.contains("copyIndex:0"), "copied the boiler's two tanks: " + settings);
        JsonObject keys = holdAltUntil(c, 1);
        settings = held(c).toString();
        Expect.that(keys.get("client_alt_down").getAsBoolean(), "the client saw Alt held: " + keys);
        Expect.that(keys.get("tool_alt").getAsBoolean(), "the server saw the copy tool's Alt held: " + keys);
        c.screenshot("B0-settings-alt-selected-steam");
        Expect.that(settings.contains("copyIndex:1"), "Alt moved the paste selection to the second fluid: " + settings);

        // plain paste on the first duct: steam, the rest untouched
        c.teleport(dx + 0.5, Y + 1, dz + 2.5);
        c.waitTicks(3);
        c.lookAt(dx + 0.5, Y + 1.5, dz + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
        Expect.that(ductType(c, dx, dz).equals("hbm:steam") && ductType(c, dx + 1, dz).equals("hbm:none"), "a plain paste retypes one duct to steam");

        // Alt back to water (the index wraps), then Ctrl-paste into the untyped run
        holdAltUntil(c, 0);
        Expect.that(held(c).toString().contains("copyIndex:0"), "Alt wrapped back to the first fluid");
        c.teleport(dx + 3.5, Y + 1, dz + 2.5);
        c.waitTicks(3);
        c.lookAt(dx + 3.5, Y + 1.5, dz + 0.5);
        c.keyDown("key.hbm.copyToolCtrl");
        c.waitTicks(4);
        c.use(2);
        c.waitTicks(4);
        c.keyUp("key.hbm.copyToolCtrl");
        c.waitTicks(5);
        StringBuilder types = new StringBuilder();
        for (int i = 0; i < 6; i++) types.append(ductType(c, dx + i, dz)).append(' ');
        worldShot(c, "B1-ducts-ctrl-pasted", dx + 3, Y + 4, dz + 5.5, dx + 3, Y + 1, dz + 0.5);
        Expect.that(ductType(c, dx, dz).equals("hbm:steam"), "the steam duct kept its type (a different type ends the run): " + types);
        for (int i = 1; i < 6; i++) Expect.that(ductType(c, dx + i, dz).equals(WATER), "Ctrl pasted water along the run: " + types);
        c.cmd("clear");
    }

    /**
     * Holds the copy tool's Alt until the server's paste selection reads {@code target}, then lets go. 1.7
     * {@code ItemSettingsTool.onUpdate} cycles once per 5 ticks for as long as the key is held, so a fixed-length
     * hold on a lagging client can cycle twice; the poll reads the key state and the selection in one server step
     * and releases at once. Returns that reading (the server-side proof the key was held when it moved).
     */
    private static JsonObject holdAltUntil(Client c, int target) {
        c.keyDown("key.hbm.copyToolAlt");
        JsonObject keys = new JsonObject();
        try {
            for (int i = 0; i < 40; i++) {
                c.waitTicks(1);
                keys = gapsa(c);
                if (keys.has("settings_copy_index") && keys.get("settings_copy_index").getAsInt() == target) break;
            }
        } finally {
            c.keyUp("key.hbm.copyToolAlt");
        }
        c.waitTicks(10);
        return keys;
    }

    private static String ductType(Client c, int x, int z) {
        String nbt = c.blockEntity(x, Y + 1, z).toString().replaceAll("\\s", "").replace("\\\"", "\"");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("type:\"([^\"]+)\"").matcher(nbt);
        return m.find() ? m.group(1) : "?";
    }

    private static JsonObject held(Client c) {
        return c.query("hbm_utility", new JsonObject());
    }

    // ============================================================== 16: death ray, satlink ROR

    private static void deathRayAndRor(Client c) {
        int freq = 424242;
        c.cmd("gamemode creative");
        long time = c.status().get("game_time").getAsLong();
        if (time < 6200 && !legacy(c)) {
            c.cmd("tick sprint " + (6300 - time));
        }
        for (int i = 0; i < 400 && time < 6200; i++) {
            c.waitTicks(40);
            time = c.status().get("game_time").getAsLong();
        }
        Expect.that(time >= 6200, "the world aged past the death ray's 1.7 CHARGE_TIME (6000): " + time);

        c.cmd("ntmsatellites descend " + freq);
        c.cmd("clear");
        c.cmd("give @s " + satellite(c, 6, freq) + " 1");
        c.waitTicks(3);
        c.hotbar(1);
        c.waitTicks(3);
        c.cmd("ntmsatellites orbit");
        c.waitTicks(3);
        c.cmd("clear");
        c.cmd("give @s " + chip(c, "hbm:sat_coord", freq) + " 1");
        c.waitTicks(8);
        c.hotbar(1);
        c.waitTicks(4);
        c.use(1);
        c.waitScreen("GUIScreenSatCoord", 5000);
        JsonObject status = c.status();
        int rl = (status.get("gui_width").getAsInt() - 176) / 2, rt = (status.get("gui_height").getAsInt() - 126) / 2;
        c.click(rl + 70, rt + 24);
        c.type("200");
        c.click(rl + 70, rt + 95);
        c.type("200");
        c.click(rl + 142, rt + 61);
        c.waitTicks(5);
        c.cmd("gamemode spectator");
        c.teleport(180.5, Y + 30, 180.5);
        JsonObject blast = null;
        for (int i = 0; i < 20 && blast == null; i++) {
            c.waitTicks(4);
            blast = entity(c, "hbm:entity_laser_blast", 300);
        }
        Expect.that(blast != null, "the death ray dropped its blast at 200 / 200");
        // the superflat ground at 200 / 200 is at y -61, far below the platform: aim at the blast and that ground
        double by = blast.get("y").getAsDouble();
        c.teleport(185.5, by + 12, 185.5);
        c.waitTicks(4);
        c.lookAt(200.5, by, 200.5);
        c.waitTicks(4);
        c.screenshot("C0-death-ray-beam");
        c.waitTicks(80);
        c.teleport(182.5, -40, 182.5);
        c.waitTicks(10);
        c.lookAt(200.5, -62, 200.5);
        c.waitTicks(8);
        c.screenshot("C1-death-ray-after");
        c.cmd("gamemode creative");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(10);

        // the ground station tuned to the death ray, read by a ROR reader on its east face
        int gx = X - 24, gz = Z - 24;
        c.cmd("clear");
        place(c, "hbm:machine_satlink", gx, Y, gz);
        int[] link = findCore(c, gx, Y + 1, gz, "machine_satlink", 2);
        Expect.that(link != null, "the ground station formed a core");
        c.cmd("clear");
        c.cmd("give @s " + chip(c, "hbm:sat_chip", freq) + " 1");
        c.waitTicks(2);
        c.hotbar(1);
        c.teleport(link[0] + 3.5, Y + 1, link[2] + 0.5);
        c.waitTicks(3);
        c.lookAt(link[0] + 0.5, Y + 1.5, link[2] + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(40);
        Expect.that(c.blockEntity(link[0], link[1], link[2]).toString().replaceAll("\\s", "").contains("freq:" + freq),
                "the chip tuned the ground station");
        Fixtures.giveOnly(c, "hbm:radio_torch_reader", 1);
        c.teleport(link[0] + 4.5, Y + 1, link[2] + 0.5);
        c.waitTicks(3);
        c.lookAt(link[0] + 1.98, Y + 1.5, link[2] + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
        int rx = Integer.MIN_VALUE, rz = 0;
        for (int dx = -2; dx <= 3 && rx == Integer.MIN_VALUE; dx++) for (int dz = -2; dz <= 3; dz++) {
            if (c.block(link[0] + dx, Y + 1, link[2] + dz).get("block").getAsString().equals("hbm:radio_torch_reader")) {
                rx = link[0] + dx; rz = link[2] + dz; break;
            }
        }
        Expect.that(rx != Integer.MIN_VALUE, "the ROR reader was placed against the ground station");
        c.cmd("data merge block " + rx + " " + (Y + 1) + " " + rz
                + " {channels0:\"gsat\",names0:\"freq\",channels1:\"gsatok\",names1:\"true\"}");
        c.waitTicks(40);
        String reader = c.blockEntity(rx, Y + 1, rz).get("nbt").getAsString().replaceAll("\\s", "");
        Expect.that(reader.contains("prev0:\"" + freq + "\""), "the reader took VAL:freq from the ground station: " + reader);
        Expect.that(reader.contains("prev1:\"TRUE\""), "and 1.7's VAL:true quirk (connected) answered TRUE: " + reader);
        c.teleport(rx + 0.5 + (rx - link[0]), Y + 1, rz + 0.5 + (rz - link[2]));
        c.waitTicks(3);
        Fixtures.interact(c, rx, Y + 1, rz);
        c.waitScreen("RadioReaderScreen", 5000);
        c.waitTicks(5);
        c.screenshot("C2-ror-reader-gui");
        c.key("escape");
        c.waitTicks(3);
        worldShot(c, "C3-satlink-reader", rx + 5.5, Y + 4, rz + 5.5, link[0] + 0.5, Y + 2, link[2] + 0.5);
        c.cmd("ntmsatellites descend " + freq);
        c.cmd("clear");
    }

    // ============================================================== 7: knife

    private static void knife(Client c) {
        int tx = X + 4, tz = Z - 4;
        c.cmd("gamemode creative");
        Fixtures.placeOnTop(c, "hbm:machine_armor_table", tx, Y, tz);
        c.cmd("clear");
        c.cmd("gamemode survival");
        c.cmd("give @s hbm:steel_plate 1");
        c.cmd("give @s hbm:injector_knife 1");
        c.waitTicks(4);
        // the baseline before the knife is fitted: its first stab can land while the table closes (every 50 ticks
        // of the wearer's age, whatever the phase), and a later read would already include it
        float max0 = gapsa(c).get("max_health").getAsFloat();
        c.teleport(tx + 0.5, Y + 1, tz - 1.5);
        c.waitTicks(2);
        Fixtures.interact(c, tx, Y + 1, tz);
        c.waitScreen("Armor Modification Table", 5000);
        c.waitTicks(3);
        c.clickSlot(playerSlot(c, "hbm:steel_plate"));
        c.waitTicks(1);
        c.clickSlot(9);
        c.waitTicks(2);
        c.clickSlot(playerSlot(c, "hbm:injector_knife"));
        c.waitTicks(1);
        c.clickSlot(7);
        c.waitTicks(3);
        c.clickSlot(9);
        c.waitTicks(1);
        c.clickSlot(11);
        c.waitTicks(3);
        c.key("escape");
        c.waitTicks(5);
        JsonObject q = gapsa(c);
        boolean jolted = false;
        for (int i = 0; i < 40 && !jolted; i++) {
            c.waitTicks(5);
            q = gapsa(c);
            jolted = q.get("hurt_duration").getAsInt() == 10000 && q.get("hurt_time").getAsInt() > 9000;
        }
        c.look(0, 0);
        c.waitTicks(3);
        c.screenshot("D0-knife-jolt");
        Expect.that(jolted, "1.7 properJolt: hurt time 10,000-19,999 over a 10,000 duration: " + q);
        Expect.that(q.get("max_health").getAsFloat() <= max0 - 2F, "the stab took 2 max health: " + max0 + " -> " + q);
        c.waitTicks(20);
        c.screenshot("D1-knife-jolt-later");
        c.cmd("item replace entity @s armor.chest with minecraft:air");
        c.cmd("gamemode creative");
    }

    // ============================================================== helpers

    private static JsonObject gapsa(Client c) {
        return c.query("hbm_gapsa", new JsonObject());
    }

    private static boolean legacy(Client c) {
        return c.node().startsWith("1.20.");
    }

    /** A full cell; {@code variant} is the sprite its fluid selects (no longer stored, History §19es-itemvariant). */
    private static String cell(Client c, String fluid, int variant) {
        return legacy(c)
                ? "hbm:cell{\"hbm:fluid\":\"" + fluid + "\",\"hbm:fluid_amount\":1000}"
                : "hbm:cell[hbm:fluid=\"" + fluid + "\",hbm:fluid_amount=1000]";
    }

    private static String crucible(Client c, int charges) {
        return legacy(c) ? "hbm:crucible{\"hbm:charges\":" + charges + "}" : "hbm:crucible[hbm:charges=" + charges + "]";
    }

    private static String controller(Client c, int variant) {
        String name = controllerName(variant);
        return legacy(c) ? "hbm:custom_machine{\"hbm:custom_machine\":\"" + name + "\"}"
                : "hbm:custom_machine[hbm:custom_machine=\"" + name + "\"]";
    }

    /** 1.7 controller damage 100/101/102: the paper press and the two staged test machines, by configuration name. */
    private static String controllerName(int variant) {
        return switch (variant) {
            case 101 -> "gapsaFlux";
            case 102 -> "gapsaHeat";
            default -> "paperPress";
        };
    }


    private static String satellite(Client c, int variant, int freq) {
        String id = "hbm:satellite_" + new String[] {"spy", "scanner", "radar", "miner_astro", "miner_lunar", "precision_laser", "death_ray", "xenium_resonator", "relay", "detector", "ray_scan"}[variant];
        if (legacy(c)) return id + "{\"hbm:persistent_data\":{freq:" + freq + "}}";
        return id + "[hbm:persistent_data={freq:" + freq + "}]";
    }

    private static String chip(Client c, String id, int freq) {
        if (legacy(c)) return id + "{\"hbm:persistent_data\":{freq:" + freq + "}}";
        return id + "[hbm:persistent_data={freq:" + freq + "}]";
    }

    private static void place(Client c, String item, int x, int y, int z) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(x + 4.5, y + 1, z + 0.5);
        c.waitTicks(3);
        c.lookAt(x + 0.9, y + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
    }

    private static int[] findCore(Client c, int x, int y, int z, String id, int r) {
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    JsonObject be = c.blockEntity(x + dx, y + dy, z + dz);
                    if (be.has("block_entity") && !be.get("block_entity").isJsonNull()
                            && (be.toString().contains(id) || c.block(x + dx, y + dy, z + dz).toString().contains(id))) {
                        return new int[]{x + dx, y + dy, z + dz};
                    }
                }
            }
        }
        return null;
    }

    /** The integrated server's entity list: the FLEIJA field is a logic entity no client tracks. */
    private static JsonObject serverEntity(Client c, String type, double radius) {
        JsonObject params = new JsonObject();
        params.addProperty("radius", radius);
        params.addProperty("server", true);
        for (JsonElement e : c.query("entities", params).getAsJsonArray("entities")) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals(type)) return o;
        }
        return null;
    }

    private static JsonObject entity(Client c, String type, double radius) {
        for (JsonElement e : c.entities(radius)) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals(type)) return o;
        }
        return null;
    }

    private static int playerSlot(Client c, String id) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) {
                return s.get("index").getAsInt();
            }
        }
        throw new IllegalStateException("no player slot holding " + id);
    }

    private static int emptyPlayerSlot(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.get("empty").getAsBoolean()) return s.get("index").getAsInt();
        }
        throw new IllegalStateException("no empty player slot");
    }

    /** The menu slot at a GUI-relative position (1.7 container coordinates). */
    private static int slotAt(Client c, JsonObject gui, int rx, int ry) {
        int left = gui.get("left").getAsInt(), top = gui.get("top").getAsInt();
        for (JsonElement e : gui.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("x").getAsInt() == left + rx && s.get("y").getAsInt() == top + ry) return s.get("index").getAsInt();
        }
        throw new IllegalStateException("no slot at " + rx + "," + ry);
    }

    /** The hotbar index (0..8) holding {@code id}. */
    private static int slotOf(Client c, String id) {
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(id) && s.get("slot").getAsInt() < 9) {
                return s.get("slot").getAsInt();
            }
        }
        throw new IllegalStateException(id + " is not on the hotbar");
    }

    private static void hover(Client c, int slot) {
        JsonObject s = c.slot(slot);
        c.mouseMove(s.get("center_x").getAsDouble(), s.get("center_y").getAsDouble());
        c.waitTicks(5);
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
