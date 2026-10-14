import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Textures and models swapped from 1.12.2 (or older 1.7) art to 1.7 5771's. Places each machine whose OBJ or skin
 * changed, photographs the row from the front and from above, then shows the changed item icons in the inventory.
 * Each shot must show textures lying on their model (no smeared or misaligned UVs) and no missing-texture checkers.
 */
public final class Assets17 implements Scenario {

    private static final int X = 900;
    private static final int Y = 100;
    private static final int Z = 900;
    private static final int SPACING = 9;

    private static final List<String> MACHINES = List.of(
            "machine_cyclotron", "machine_catalytic_reformer", "machine_assembly_factory", "machine_chemical_factory",
            "machine_fluidtank", "machine_fel", "machine_compressor_compact", "machine_refinery", "machine_autosaw",
            "machine_rtg_grey", "steel_scaffold");

    private static final List<String> ITEMS = List.of(
            "wire_fine_gold", "wire_fine_copper", "wire_fine_aluminium", "wire_fine_red_copper", "wire_fine_tungsten",
            "wire_fine_carbon", "wire_fine_schrabidium", "wire_fine_magnetized_tungsten", "wire_fine_steel",
            "coil_copper", "coil_gold", "coil_tungsten", "coil_magnetized_tungsten", "coil_copper_torus",
            "coil_gold_torus", "niter", "biomass_compressed", "cap_star", "cap_sunset", "fluid_identifier_multi",
            "watz_pellet_schrabidium", "arc_electrode_desh", "billet_bismuth", "ams_lens", "ams_core_wormhole",
            "steel_scaffold", "machine_cyclotron", "machine_catalytic_reformer", "machine_assembly_factory",
            "machine_chemical_factory", "machine_fel", "machine_compressor_compact");

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        int length = MACHINES.size() * SPACING;
        String forceload = (X - 16) + " " + (Z - 16) + " " + (X + length + 16) + " " + (Z + 16);
        String hideGui = null;
        try {
            c.cmd("forceload add " + forceload);
            c.cmd("fill " + (X - 6) + " " + Y + " " + (Z - 8) + " " + (X + length) + " " + Y + " " + (Z + 8) + " minecraft:stone");
            c.cmd("fill " + (X - 6) + " " + (Y + 1) + " " + (Z - 8) + " " + (X + length) + " " + (Y + 12) + " " + (Z + 8) + " minecraft:air");
            Fixtures.platform(c, X, Y, Z, 6);
            c.cmd("time set 6000");

            // placed by hand so multiblocks build their dummies; the player stands 4 blocks south facing north
            List<String> missing = new ArrayList<>();
            for (int i = 0; i < MACHINES.size(); i++) {
                int x = X + i * SPACING;
                Fixtures.giveOnly(c, "hbm:" + MACHINES.get(i), 1);
                c.teleport(x + 0.5, Y + 1, Z - 3.5);
                c.waitTicks(2);
                c.lookAt(x + 0.5, Y + 0.98, Z + 0.5);
                c.waitTicks(1);
                c.use(2);
                c.waitTicks(4);
                JsonObject block = c.block(x, Y + 1, Z);
                if (block.get("block").getAsString().equals("minecraft:air")) missing.add(MACHINES.get(i));
            }
            Expect.that(missing.isEmpty(), "every machine placed: " + missing);
            c.cmd("clear");

            hideGui = c.setOption("hide_gui", "true");
            c.cmd("gamemode spectator");
            for (int i = 0; i < MACHINES.size(); i++) {
                int x = X + i * SPACING;
                c.teleport(x + 4.5, Y + 4.5, Z - 6.5);
                c.lookAt(x + 0.5, Y + 2, Z + 0.5);
                c.waitTicks(15);
                c.screenshot(String.format("%02d-%s", i, MACHINES.get(i)));
            }
            c.teleport(X + 3.5, Y + 12, Z - 3.5);
            c.lookAt(X + 18.5, Y + 1, Z + 0.5);
            c.waitTicks(15);
            c.screenshot("row-top");
            c.setOption("hide_gui", hideGui);
            hideGui = null;

            // survival, so E opens the plain inventory rather than a creative tab
            c.cmd("gamemode survival");
            c.cmd("clear");
            for (String item : ITEMS) c.cmd("give @s hbm:" + item + " 1");
            c.waitTicks(5);
            c.key("e");
            c.waitTicks(10);
            c.screenshot("inventory-icons");
            c.key("escape");
            Expect.logCleanSinceMark(c, "(Unknown item|Missing model|missing texture|Unable to load model|ERROR\\])");
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.cmd("forceload remove " + forceload);
        }
    }
}
