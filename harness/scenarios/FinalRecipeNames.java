import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * §19ed-recipe-names: 1.7's named GenericRecipes ({@code setupNamed}) shown under their en_US.lang names where 1.7
 * shows them.
 * <ul>
 *   <li>assembly machine, placed by hand and opened by a right-click: the recipe selector is searched for
 *       "to ammunition", which only 1.7's name of {@code ass.nitra} ("Nitra to Ammunition") contains - its icon is
 *       plain Nitra - the result is picked by a click, and the selector and machine tooltips are captured;</li>
 *   <li>chemical plant: the same with "desh from", only in {@code chem.deshcracked}'s "Desh from Cracked Oil"
 *       (icon: desh powder; the other named chemical recipes are pooled behind blueprints or share a fluid's name);</li>
 *   <li>JEI: the atomic clock's recipes, whose second assembly recipe is the restored alt-pool
 *       {@code ass.atomicClockAlt}; hovering its blueprint lists the pool by its 1.7 names ("Simple Analog
 *       Circuit", "Minimalist Circuits", "Caesium Atomic Clock").</li>
 * </ul>
 */
public final class FinalRecipeNames implements Scenario {

    private static final int X = 0, Y = 100, Z = 0, L = 101;
    private static final int ASM_X = 0, CHEM_X = 8;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 16);
        c.cmd("gamerule sendCommandFeedback false");

        placeSouth(c, "hbm:machine_assembly_machine", ASM_X, Z);
        Expect.block(c, ASM_X, L, Z, "hbm:machine_assembly_machine");
        pickByName(c, ASM_X, "GUIMachineAssemblyMachine", "to ammunition", "ass.nitra", "10-assembly");

        placeSouth(c, "hbm:machine_chemical_plant", CHEM_X, Z);
        Expect.block(c, CHEM_X, L, Z, "hbm:machine_chemical_plant");
        pickByName(c, CHEM_X, "GUIMachineChemicalPlant", "desh from", "chem.deshcracked", "20-chemplant");

        jei(c);

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|unregistered hbm:|duplicate ID|translation key).*");
    }

    /**
     * Opens the machine, its recipe selector, searches {@code term} (a substring of the recipe's 1.7 name only),
     * captures the filtered grid and the hovered tooltip, picks the recipe by clicks and captures the machine's own
     * selector-button tooltip.
     */
    private void pickByName(Client c, int coreX, String screen, String term, String recipe, String shot) {
        open(c, coreX, Z + 1, screen);
        JsonObject s = c.screen();
        int left = s.get("left").getAsInt(), top = s.get("top").getAsInt();
        c.click(left + 16, top + 134);
        c.waitScreen("RecipeSelector", 5_000L);
        JsonObject search = editBox(c);
        int gl = search.get("x").getAsInt() - 28, gt = search.get("y").getAsInt() - 111;
        c.click(search.get("center_x").getAsDouble(), search.get("center_y").getAsDouble());
        c.waitTicks(2);
        c.type(term);
        c.waitTicks(4);
        c.mouseMove(gl + 16, gt + 26);
        c.waitTicks(4);
        c.screenshot(shot + "-selector-search-tooltip");
        c.click(gl + 16, gt + 26);             // the only match, first in the filtered grid
        c.waitTicks(3);
        c.mouseMove(gl + 160, gt + 98);        // the "current selection" slot
        c.waitTicks(4);
        c.screenshot(shot + "-selector-selected-tooltip");
        c.click(gl + 160, gt + 98);            // confirm and close
        c.waitTicks(6);
        if (c.screenClass() != null && c.screenClass().contains("RecipeSelector")) close(c);
        String nbt = nbt(c, coreX, L, Z);
        Expect.that(nbt.contains("recipe0:\"" + recipe + "\""),
                "searching '" + term + "' (1.7 name only) found and picked " + recipe + "; nbt " + nbt);
        if (c.screenClass() == null) open(c, coreX, Z + 1, screen);
        s = c.screen();
        left = s.get("left").getAsInt();
        top = s.get("top").getAsInt();
        c.mouseMove(left + 16, top + 134);     // the machine's selector button shows the active recipe's print()
        c.waitTicks(4);
        c.screenshot(shot + "-machine-tooltip");
        close(c);
    }

    /** JEI: R on the atomic clock, then a hover sweep over the recipe column for the alt blueprint. */
    private void jei(Client c) {
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("give @s hbm:circuit_atomic_clock 1");
        c.waitTicks(4);
        c.key("key.inventory");
        c.waitScreen("Inventory", 5_000L);
        c.waitTicks(10);
        JsonObject slot = c.slot(playerSlot(c, "hbm:circuit_atomic_clock"));
        c.mouseMove(slot.get("center_x").getAsDouble(), slot.get("center_y").getAsDouble());
        c.waitTicks(5);
        c.key("r");
        c.waitTicks(20);
        String screen = c.screenClass();
        c.screenshot("30-jei-atomic-clock");
        Expect.that(screen != null && screen.toLowerCase().contains("recipe"), "JEI opened the atomic clock's recipes: " + screen);
        JsonObject status = c.status();
        int w = status.get("gui_width").getAsInt(), h = status.get("gui_height").getAsInt();
        // the blueprint sits in the machine pill at local (75, 10) of a 166-wide layout centred on the screen
        int n = 0;
        for (int dy = -110; dy <= 90; dy += 10) {
            c.mouseMove(w / 2.0, h / 2.0 + dy);
            c.waitTicks(3);
            c.screenshot(String.format("3%02d-jei-hover-%+d", ++n, dy));
        }
        closeScreens(c);
        c.cmd("clear");
        c.cmd("gamemode creative");
    }

    // ------------------------------------------------------------------ helpers

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
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

    private static void open(Client c, int x, int z, String screen) {
        c.hotbar(9);
        standAt(c, x + 0.5, L + 1, z + 0.5 + 2.5);
        Fixtures.interact(c, x, L + 1, z);
        c.waitScreen(screen, 5_000L);
        Expect.screenStaysOpen(c, screen, 5);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(2);
    }

    /** Escape only while a screen is open: an escape in the world opens the pause menu. */
    private static void closeScreens(Client c) {
        for (int i = 0; i < 4 && c.screenClass() != null; i++) {
            c.key("escape");
            c.waitTicks(5);
        }
    }

    private static JsonObject editBox(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("widgets")) {
            JsonObject w = e.getAsJsonObject();
            if (w.get("class").getAsString().contains("EditBox")) return w;
        }
        throw new IllegalStateException("no text field on " + c.screenClass());
    }
}
