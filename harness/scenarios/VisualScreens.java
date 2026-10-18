import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Wave 2 VISUAL (History §19ek-visual): opens every screen whose texture, draw code or labels moved to the 1.7.10
 * spec, by a real right-click on a placed block, and screenshots it for comparison with the 1.7 texture layout.
 * Set the environment variable {@code VISUAL_ONLY=<id,id>} for the harness CLI to open a subset.
 *
 * <p>Fails if any listed block does not open a screen; the per-screen class and image size are printed.
 */
public final class VisualScreens implements Scenario {

    private static final int Y = 100;

    /** Covered by {@code VisualScreensLarge}; skipped here unless named in {@code VISUAL_ONLY}. */
    static final List<String> LARGE = List.of("pa_detector", "soyuz_launcher", "launch_pad_large", "machine_mining_laser");

    static final String[] BLOCKS = {
            // batch 1: DFC, particle accelerator, fusion, reactors, launchers, bombs
            "dfc_core", "dfc_emitter", "dfc_injector", "dfc_receiver", "dfc_stabilizer",
            "pa_source", "pa_dipole", "pa_quadrupole", "pa_rfc", "pa_detector",
            "fusion_breeder", "fusion_klystron", "fusion_torus", "machine_zirnox", "machine_fel",
            "soyuz_launcher", "launch_pad", "launch_pad_large", "nuke_boy", "nuke_n2", "nuke_man",
            // batch 2: turrets, machines
            "turret_chekhov", "turret_friendly", "turret_jeremy", "turret_tauon", "turret_richard", "turret_howard",
            "turret_maxwell", "turret_fritz", "turret_arty", "turret_himars", "turret_sentry",
            "machine_rtg_grey", "machine_press", "machine_epress", "machine_electric_furnace_off", "machine_shredder",
            "machine_refinery", "machine_well", "machine_centrifuge", "machine_gascent", "machine_cyclotron",
            "machine_mining_laser", "anvil_iron", "machine_storage_drum", "machine_waste_drum",
            // batch 3: generators, processing
            "machine_combustion_engine", "machine_compressor", "machine_pyrooven", "machine_catalytic_reformer",
            "machine_crystallizer", "machine_liquefactor", "machine_solidifier", "machine_arc_furnace",
            "machine_arc_welder", "machine_soldering_station", "machine_assembly_factory", "machine_chemical_factory",
            "machine_exposure_chamber", "machine_mixer", "machine_purex", "machine_precass", "machine_electrolyser",
            "machine_flare", "machine_large_turbine", "machine_turbine_gas", "machine_turbofan",
            "machine_wood_burner", "heater_oilburner", "heater_firebox", "heater_oven", "machine_battery_redd",
            "machine_battery_socket", "radio_autocal",
            // batch 4: labels
            "machine_ashpit", "barrel_steel", "machine_blast_furnace", "machine_uf6_tank", "machine_furnace_brick_off",
            "machine_furnace_combination", "furnace_iron", "furnace_steel", "machine_chemical_plant", "machine_coker",
            "machine_fluidtank", "machine_bat9000", "radio_telex", "rbmk_outgasser",
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("gamemode creative");
        c.cmd("time set 6000");
        c.cmd("gamerule doDaylightCycle false");
        String only = System.getenv("VISUAL_ONLY") == null ? "" : System.getenv("VISUAL_ONLY");
        List<String> failed = new ArrayList<>();
        int n = 0;
        for (String id : BLOCKS) {
            if (!only.isEmpty() && !("," + only + ",").contains("," + id + ",")) continue;
            // These four cannot be opened from this scenario's stock placement (the player stands inside or beside the
            // wrong part of a wide multiblock); VisualScreensLarge opens them from outside their footprint (§19ek-final).
            if (only.isEmpty() && LARGE.contains(id)) continue;
            int x = 0, z = 0;
            c.cmd("fill -12 " + (Y - 1) + " -12 12 " + (Y + 14) + " 12 minecraft:air");
            Fixtures.platform(c, 0, Y, 0, 12);
            c.waitTicks(5);
            Fixtures.placeOnTop(c, "hbm:" + id, x, Y, z);
            c.waitTicks(10);
            c.teleport(x + 0.5, Y + 1, z - 3.5);
            Fixtures.interact(c, x, Y + 1, z);
            c.waitTicks(15);
            String screen = c.screenClass();
            if (screen == null) {
                failed.add(id);
                System.out.println("[visual] NO SCREEN " + id);
                c.screenshot(String.format("%03d-%s-NOSCREEN", n++, id));
                continue;
            }
            JsonObject s = c.screen();
            System.out.println("[visual] " + id + " -> " + screen + " " + s.get("image_width") + "x" + s.get("image_height"));
            c.mouseMove(2, 2);
            c.waitTicks(5);
            c.screenshot(String.format("%03d-%s", n++, id));
            c.key("escape");
            c.waitTicks(5);
        }
        c.cmd("fill -12 " + (Y - 1) + " -12 12 " + (Y + 14) + " 12 minecraft:air");
        System.out.println("[visual] screens failed to open: " + failed);
        Expect.that(failed.isEmpty(), "screens that did not open: " + failed);
        Expect.logCleanSinceMark(c, "(?i)exception.*(gui|screen|render|hbm)");
    }
}
