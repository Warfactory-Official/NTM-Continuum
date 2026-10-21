import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Release audit, item processing machines: places each machine by hand from all four sides (so each facing is
 * photographed from the side it faces), opens the GUI of the first one with a real right-click and screenshots both.
 * Soft checks, one entry per machine. Optional environment RC_PROC_FILTER=regex on the block id.
 */
public final class RcProcGui implements Scenario {

    private static final int Y = 100;
    private static final String[] BLOCKS = {
            "machine_assembly_machine", "machine_assembly_factory", "machine_chemical_plant", "machine_chemical_factory",
            "machine_press", "machine_epress", "machine_conveyor_press", "machine_shredder", "machine_centrifuge",
            "machine_gascent", "machine_electric_furnace_off", "machine_furnace_combination", "machine_pyrooven",
            "machine_rotary_furnace", "machine_crystallizer", "machine_silex", "machine_fel", "machine_purex",
            "machine_mixer", "machine_electrolyser", "machine_precass", "machine_ore_slopper", "machine_rockmill",
            "machine_autosaw", "machine_thresher", "machine_compressor", "machine_compressor_compact",
            "machine_difurnace_off", "machine_difurnace_extension", "machine_difurnace_rtg_off", "machine_rtg_furnace_off",
            "machine_furnace_brick_off", "furnace_iron", "furnace_steel", "machine_ashpit", "machine_waste_drum",
            "machine_storage_drum", "custom_machine", "machine_ammo_press", "machine_radiolysis", "machine_autocrafter",
            "machine_sawmill", "machine_microwave", "machine_blast_furnace", "machine_exposure_chamber",
            "machine_keyforge", "machine_funnel", "machine_stirling", "cm_block_alloy", "cm_block_steel", "cm_engine_bismuth",
            "cm_port_steel", "cm_tank_steel", "cm_circuit_basic", "cm_flux", "cm_heat", "cm_anchor"
    };
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private final List<String> failures = new ArrayList<>();

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);
        c.waitTicks(5);
        String filter = System.getenv("RC_PROC_FILTER");
        for (String id : BLOCKS) {
            if (filter != null && !filter.isBlank() && !id.matches(filter)) continue;
            try {
                one(c, id);
            } catch (RuntimeException e) {
                failures.add(id + ": " + e.getMessage());
                System.out.println("[FAIL] " + id + " " + e.getMessage());
                c.key("escape");
                c.waitTicks(3);
            }
        }
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(hbm|golem|render|screen|gui)");
        Expect.that(failures.isEmpty(), "soft checks failed: " + failures);
    }

    private void one(Client c, String id) {
        for (int side = 0; side < 4; side++) {
            c.cmd("fill -14 " + (Y + 1) + " -14 14 " + (Y + 12) + " 14 minecraft:air");
            c.waitTicks(3);
            Fixtures.giveOnly(c, "hbm:" + id, 1);
            int[] d = DIRS[side];
            c.teleport(d[0] * 2.5 + 0.5, Y + 1, d[1] * 2.5 + 0.5);
            c.waitTicks(2);
            c.lookAt(0.5, Y + 0.98, 0.5);
            c.waitTicks(1);
            c.use(2);
            c.waitTicks(4);
            int[] core = findCore(c);
            JsonObject be = core == null ? null : c.blockEntity(core[0], core[1], core[2]);
            String klass = be != null && be.has("block_entity") && !be.get("block_entity").isJsonNull()
                    ? be.get("block_entity").getAsString() : "none";
            JsonObject placed = c.block(0, Y + 1, 0);
            System.out.println("[" + id + " side " + side + "] placed " + placed + " core "
                    + (core == null ? "?" : core[0] + "," + core[1] + "," + core[2]) + " be " + klass);
            if (placed.has("id") && placed.get("id").getAsString().contains("air")) {
                failures.add(id + " side " + side + ": block not placed (air)");
            }
            double dist = id.matches(".*(factory|crystallizer|fel|silex|purex|rotary|compressor|chemical_plant|assembly_machine).*") ? 10.5 : 6.5;
            c.teleport(d[0] * dist + 0.5, Y + 3, d[1] * dist + 0.5);
            c.waitTicks(2);
            c.lookAt(0.5, Y + 1.8, 0.5);
            String hide = c.setOption("hide_gui", "true");
            c.waitTicks(8);
            c.screenshot(id + "-f" + side);
            c.setOption("hide_gui", "false");
            if (side == 0 && core != null && !klass.equals("none")) {
                c.teleport(d[0] * 2.5 + 0.5, Y + 1, d[1] * 2.5 + 0.5);
                c.waitTicks(2);
                Fixtures.interact(c, core[0], core[1], core[2]);
                c.waitTicks(12);
                String screen = c.screenClass();
                String title = "";
                if (screen != null) {
                    JsonObject s = c.screen();
                    title = s.has("title") ? s.get("title").getAsString() : "";
                    c.screenshot(id + "-gui");
                    c.waitTicks(10);
                    if (c.screenClass() == null) failures.add(id + ": screen closed by itself");
                    c.key("escape");
                    c.waitTicks(4);
                }
                System.out.println("[" + id + "] screen " + screen + " title '" + title + "'");
                if (title.matches("[a-z0-9_.]+\\.[a-z0-9_.]+")) failures.add(id + ": raw lang key as title: " + title);
            }
        }
    }

    private static int[] findCore(Client c) {
        for (int dy = 1; dy <= 6; dy++)
            for (int r = 0; r <= 7; r++)
                for (int dz = -r; dz <= r; dz++)
                    for (int dx = -r; dx <= r; dx++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        JsonObject be = c.blockEntity(dx, Y + dy, dz);
                        if (be != null && be.has("block_entity") && !be.get("block_entity").isJsonNull()) {
                            return new int[]{dx, Y + dy, dz};
                        }
                    }
        return null;
    }
}
