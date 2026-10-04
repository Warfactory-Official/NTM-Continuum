import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * §19ed-recipe-content in the real machines, every machine placed by hand and opened by a real right-click:
 * <ul>
 *   <li>assembly machine: {@code ass.watzcasing} (1.7 ANY_RESISTANTALLOY welded plate, satisfied by a welded
 *       TC-alloy plate through the aggregate tag) and {@code ass.capnuka} (the bottle-cap block), both picked in
 *       the recipe selector by clicks and typing;</li>
 *   <li>chemical plant: {@code chem.meatprocessing} (1.7 KEY_GLYPHID_MEAT, now {@code c:glyphid_meats}) from
 *       glyphid meat and water into sulfur, niter and salient;</li>
 *   <li>soldering station: the 1.7 control unit (CONTROLLER circuit);</li>
 *   <li>anvil (murky, tier 7): the recycling block, an RBMK moderator column and an old CRT screen dismantled
 *       through the GUI's search box, recipe grid and craft button;</li>
 *   <li>storage drum: Au-198 ingots decaying into mercury droplets at 1.7's 45-second mean (15 min / 20).</li>
 * </ul>
 * Every product is then shown in the player inventory screen.
 */
public final class FinalRecipeContent implements Scenario {

    private static final int X = 0, Y = 100, Z = 0, L = 101;
    private static final int ASM_X = 0, CHEM_X = 8, SOLDER_X = 16, ANVIL_X = -6, DRUM_X = -11;
    private static final String WATER = "hbm:water", PERFLUOROMETHYL = "hbm:perfluoromethyl";

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 20);
        c.cmd("gamerule sendCommandFeedback false");

        assembly(c);
        chemicalPlant(c);
        soldering(c);
        anvil(c);
        drum(c);
        products(c);

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|unregistered hbm:|duplicate ID).*");
    }

    // ------------------------------------------------------------------ assembly machine

    private void assembly(Client c) {
        placeSouth(c, "hbm:machine_assembly_machine", ASM_X, Z);
        Expect.block(c, ASM_X, L, Z, "hbm:machine_assembly_machine");
        selectRecipe(c, ASM_X, Z + 1, 0, 2.5, "GUIMachineAssemblyMachine", "stability", "ass.watzcasing", ASM_X, Z);
        c.cmd("data merge block " + ASM_X + " " + L + " " + Z + " {power:100000L}");
        put(c, ASM_X, L, Z, 4, "hbm:plate_welded_tcalloy", 1);
        put(c, ASM_X, L, Z, 5, "hbm:ingot_boron", 3);
        put(c, ASM_X, L, Z, 6, "hbm:plate_welded_steel", 2);
        open(c, ASM_X, L + 1, Z + 1, 0, 2.5, "GUIMachineAssemblyMachine");
        c.waitTicks(20);
        c.screenshot("10-assembly-watzcasing-running");
        int made = waitFor(c, ASM_X, L, Z, "hbm:watz_casing", 3, 300);
        c.screenshot("11-assembly-watzcasing-done");
        close(c);
        Expect.that(made >= 3, "ass.watzcasing made 3 watz casings from a welded TC-alloy plate; nbt " + nbt(c, ASM_X, L, Z));
        Expect.that(count(nbt(c, ASM_X, L, Z), "hbm:plate_welded_tcalloy") == 0, "the TC-alloy plate was consumed");
        c.cmd("item replace block " + ASM_X + " " + L + " " + Z + " container.16 with minecraft:air");

        selectRecipe(c, ASM_X, Z + 1, 0, 2.5, "GUIMachineAssemblyMachine", "nuka", "ass.capnuka", ASM_X, Z);
        c.cmd("data merge block " + ASM_X + " " + L + " " + Z + " {power:100000L}");
        put(c, ASM_X, L, Z, 4, "hbm:cap_nuka", 64);
        put(c, ASM_X, L, Z, 5, "hbm:cap_nuka", 64);
        int caps = waitFor(c, ASM_X, L, Z, "hbm:block_cap_nuka", 1, 200);
        open(c, ASM_X, L + 1, Z + 1, 0, 2.5, "GUIMachineAssemblyMachine");
        c.waitTicks(5);
        c.screenshot("12-assembly-capnuka-done");
        close(c);
        Expect.that(caps >= 1, "ass.capnuka pressed 128 Nuka caps into a cap block; nbt " + nbt(c, ASM_X, L, Z));
    }

    // ------------------------------------------------------------------ chemical plant

    private void chemicalPlant(Client c) {
        placeSouth(c, "hbm:machine_chemical_plant", CHEM_X, Z);
        Expect.block(c, CHEM_X, L, Z, "hbm:machine_chemical_plant");
        selectRecipe(c, CHEM_X, Z + 1, 0, 2.5, "GUIMachineChemicalPlant", "glyphid", "chem.meatprocessing", CHEM_X, Z);
        c.cmd("data merge block " + CHEM_X + " " + L + " " + Z + " {power:100000L,i0:4000,i0_type:\"" + WATER + "\"}");
        put(c, CHEM_X, L, Z, 4, "hbm:glyphid_meat", 3);
        open(c, CHEM_X, L + 1, Z + 1, 0, 2.5, "GUIMachineChemicalPlant");
        c.waitTicks(20);
        c.screenshot("20-chemplant-meat-running");
        int sulfur = waitFor(c, CHEM_X, L, Z, "hbm:sulfur", 4, 400);
        c.screenshot("21-chemplant-meat-done");
        close(c);
        String n = nbt(c, CHEM_X, L, Z);
        Expect.that(sulfur >= 4 && count(n, "hbm:niter") >= 3,
                "chem.meatprocessing turned 3 glyphid meat + water into 4 sulfur and 3 niter; nbt " + n);
    }

    // ------------------------------------------------------------------ soldering station

    private void soldering(Client c) {
        Fixtures.placeOnTop(c, "hbm:machine_soldering_station", SOLDER_X, Y, Z);
        int[] core = findCore(c, SOLDER_X, L, Z, "maxPower");
        int sx = core[0], sy = core[1], sz = core[2];
        c.cmd("data merge block " + sx + " " + sy + " " + sz + " {tank:4000,tank_type:\"" + PERFLUOROMETHYL + "\"}");
        put(c, sx, sy, sz, 0, "hbm:circuit_chip", 32);
        put(c, sx, sy, sz, 1, "hbm:circuit_capacitor", 32);
        put(c, sx, sy, sz, 2, "hbm:circuit_capacitor_tantalium", 16);
        put(c, sx, sy, sz, 3, "hbm:circuit_controller_chassis", 1);
        put(c, sx, sy, sz, 4, "hbm:ingot_rubber", 4);
        put(c, sx, sy, sz, 5, "hbm:wire_fine_lead", 16);
        put(c, sx, sy, sz, 7, "hbm:battery_creative", 1);
        c.hotbar(9);
        c.teleport(SOLDER_X + 2.5, L, Z + 0.5);
        c.waitTicks(3);
        Fixtures.interact(c, sx, sy, sz);
        c.waitScreen("GUIMachineSolderingStation", 5_000L);
        Expect.screenStaysOpen(c, "GUIMachineSolderingStation", 5);
        c.waitTicks(20);
        c.screenshot("30-soldering-controller-running");
        int made = waitFor(c, sx, sy, sz, "hbm:circuit_controller", 1, 600);
        c.screenshot("31-soldering-controller-done");
        close(c);
        Expect.that(made >= 1, "the soldering station made a control unit (1.7 CONTROLLER); nbt " + nbt(c, sx, sy, sz));
    }

    // ------------------------------------------------------------------ anvil recycling

    private void anvil(Client c) {
        Fixtures.placeOnTop(c, "hbm:anvil_murky", ANVIL_X, Y, Z);
        Expect.block(c, ANVIL_X, L, Z, "hbm:anvil_murky");
        c.cmd("clear");
        Fixtures.give(c, "hbm:rbmk_moderator", 1);
        Fixtures.give(c, "hbm:deco_crt_clean", 1);
        dismantle(c, "moderator", "40-anvil-moderator");
        Expect.that(inventoryCount(c, "hbm:rbmk_blank") == 1 && inventoryCount(c, "hbm:block_graphite") == 4
                        && inventoryCount(c, "hbm:rbmk_moderator") == 0,
                "the moderator column came apart into a blank column and 4 graphite blocks: " + c.inventory());
        dismantle(c, "old crt", "41-anvil-crt");
        Expect.that(inventoryCount(c, "hbm:crt_display") == 1 && inventoryCount(c, "hbm:scrap") == 2
                        && inventoryCount(c, "hbm:deco_crt_clean") == 0,
                "the old CRT came apart into a CRT display and 2 scrap: " + c.inventory());
    }

    private void dismantle(Client c, String term, String shot) {
        c.hotbar(9);
        c.teleport(ANVIL_X + 2.5, L, Z + 0.5);
        c.waitTicks(3);
        Fixtures.interact(c, ANVIL_X, L, Z);
        c.waitScreen("GUIAnvil", 5_000L);
        Expect.screenStaysOpen(c, "GUIAnvil", 5);
        JsonObject s = c.screen();
        int left = s.get("left").getAsInt(), top = s.get("top").getAsInt();
        JsonObject box = editBox(c);
        c.click(box.get("center_x").getAsDouble(), box.get("center_y").getAsDouble());
        c.waitTicks(2);
        c.type(term);
        c.waitTicks(3);
        c.click(left + 24, top + 79);          // the first recipe of the filtered grid
        c.waitTicks(3);
        c.screenshot(shot + "-selected");
        c.click(left + 61, top + 62);          // the craft button
        c.waitTicks(6);
        c.screenshot(shot + "-crafted");
        close(c);
    }

    // ------------------------------------------------------------------ storage drum

    private void drum(Client c) {
        Fixtures.placeOnTop(c, "hbm:machine_storage_drum", DRUM_X, Y, Z);
        Expect.block(c, DRUM_X, L, Z, "hbm:machine_storage_drum");
        for (int slot = 0; slot < 24; slot++) put(c, DRUM_X, L, Z, slot, "hbm:ingot_au198", 1);
        int mercury = 0;
        for (int t = 0; t < 900; t += 20) {
            c.waitTicks(20);
            mercury = count(nbt(c, DRUM_X, L, Z), "hbm:nugget_mercury");
        }
        c.hotbar(9);
        c.teleport(DRUM_X + 2.5, L, Z + 0.5);
        c.waitTicks(3);
        Fixtures.interact(c, DRUM_X, L, Z);
        c.waitScreen("GUIStorageDrum", 5_000L);
        c.waitTicks(3);
        c.screenshot("50-drum-au198-decayed");
        close(c);
        // 1.7: nextInt(15 min / 20 = 900 ticks) per slot and tick, so after 900 ticks 1 - (899/900)^900 = 63 % of the
        // slots (~15 of 24) hold a mercury droplet; the old table (2,160 ticks, into bottle_mercury) gave ~8.6 bottles.
        Expect.that(mercury >= 10, "Au-198 in the drum decayed into mercury droplets (" + mercury + "/24 slots); nbt "
                + nbt(c, DRUM_X, L, Z));
    }

    // ------------------------------------------------------------------ products

    private void products(Client c) {
        c.cmd("clear");
        for (String id : new String[]{"hbm:watz_casing", "hbm:block_cap_nuka", "hbm:circuit_controller", "hbm:sulfur",
                "hbm:niter", "hbm:rbmk_blank", "hbm:crt_display", "hbm:nugget_mercury", "hbm:fusion_component_blanket",
                "hbm:machine_condenser_powered", "hbm:piston_set_steel", "hbm:missile_shuttle"}) {
            Fixtures.give(c, id, 1);
        }
        c.cmd("gamemode survival");   // the plain inventory screen, not a creative tab
        c.waitTicks(3);
        c.key("key.inventory");
        c.waitScreen("Inventory", 5_000L);
        c.waitTicks(5);
        c.screenshot("60-products");
        close(c);
        c.cmd("gamemode creative");
        c.cmd("clear");
    }

    // ------------------------------------------------------------------ helpers

    private static void put(Client c, int x, int y, int z, int slot, String item, int n) {
        c.cmd("item replace block " + x + " " + y + " " + z + " container." + slot + " with " + item + " " + n);
        c.waitTicks(1);
    }

    /** Polls the block entity until {@code id} reaches {@code want} items, at most {@code ticks}. */
    private static int waitFor(Client c, int x, int y, int z, String id, int want, int ticks) {
        int have = 0;
        for (int t = 0; t <= ticks; t += 10) {
            have = count(nbt(c, x, y, z), id);
            if (have >= want) break;
            c.waitTicks(10);
        }
        return have;
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }

    /** Items of exactly {@code id} in an SNBT inventory (26.1 {@code count:3}, 1.20.1 {@code Count:3b}). */
    private static int count(String nbt, String id) {
        int total = 0;
        Matcher m = Pattern.compile("\\{[^{}]*id:\"" + Pattern.quote(id) + "\"[^{}]*}").matcher(nbt);
        while (m.find()) {
            Matcher n = Pattern.compile("(?i)count:([0-9]+)").matcher(m.group());
            total += n.find() ? Integer.parseInt(n.group(1)) : 1;
        }
        return total;
    }

    private static int inventoryCount(Client c, String id) {
        int total = 0;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.has("id") && id.equals(s.get("id").getAsString())) total += s.has("count") ? s.get("count").getAsInt() : 1;
        }
        return total;
    }

    /** The multiblock core near a placed position: the one block entity whose NBT carries {@code key}. */
    private static int[] findCore(Client c, int x, int y, int z, String key) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 1; dy++) {
            if (nbt(c, x + dx, y + dy, z + dz).contains(key + ":")) return new int[]{x + dx, y + dy, z + dz};
        }
        throw new IllegalStateException("no block entity with " + key + " near " + x + " " + y + " " + z);
    }

    private static void placeSouth(Client c, String item, int coreX, int coreZ) {
        Fixtures.giveOnly(c, item, 1);
        standAt(c, coreX + 0.5, L, coreZ + 1 + 2 + 0.5);
        c.lookAt(coreX + 0.5, Y + 0.98, coreZ + 1 + 0.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(4);
    }

    private static void standAt(Client c, double x, double y, double z) {
        c.cmd("attribute @s minecraft:player.block_interaction_range base set 4.5");
        c.teleport(x, y, z);
        c.waitTicks(4);
    }

    private static void open(Client c, int x, int y, int z, double dx, double dz, String screen) {
        c.hotbar(9);
        standAt(c, x + 0.5 + dx, L + 1, z + 0.5 + dz);
        Fixtures.interact(c, x, y, z);
        c.waitScreen(screen, 5_000L);
        Expect.screenStaysOpen(c, screen, 5);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(2);
    }

    private static JsonObject editBox(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("widgets")) {
            JsonObject w = e.getAsJsonObject();
            if (w.get("class").getAsString().contains("EditBox")) return w;
        }
        throw new IllegalStateException("no text field on " + c.screenClass());
    }

    /** The machine's recipe selector by clicks: open, search, click matching icons until the core holds it. */
    private static void selectRecipe(Client c, int x, int z, double dx, double dz, String screen, String term, String recipe,
                                     int coreX, int coreZ) {
        open(c, x, L + 1, z, dx, dz, screen);
        JsonObject s = c.screen();
        int left = s.get("left").getAsInt(), top = s.get("top").getAsInt();
        c.click(left + 16, top + 134);
        c.waitScreen("RecipeSelector", 5_000L);
        JsonObject search = editBox(c);
        int gl = search.get("x").getAsInt() - 28, gt = search.get("y").getAsInt() - 111;
        c.click(search.get("center_x").getAsDouble(), search.get("center_y").getAsDouble());
        c.waitTicks(2);
        c.type(term);
        c.waitTicks(3);
        c.screenshot("05-search-" + recipe);
        String want = "recipe0:\"" + recipe + "\"";
        for (int i = 0; i < 8; i++) {
            c.click(gl + 16 + 18 * i, gt + 26);
            c.waitTicks(2);
            c.click(gl + 160, gt + 98);
            c.waitTicks(4);
            if (nbt(c, coreX, L, coreZ).contains(want)) break;
            if (c.screenClass() == null) open(c, x, L + 1, z, dx, dz, screen);
            if (!c.screenClass().contains("RecipeSelector")) {
                c.click(left + 16, top + 134);
                c.waitScreen("RecipeSelector", 5_000L);
            }
            JsonObject box = editBox(c);
            c.click(box.get("center_x").getAsDouble(), box.get("center_y").getAsDouble());
            c.type(term);
            c.waitTicks(2);
        }
        if (c.screenClass() != null) close(c);
        Expect.that(nbt(c, coreX, L, coreZ).contains(want), recipe + " picked in the selector; " + nbt(c, coreX, L, coreZ));
    }
}
