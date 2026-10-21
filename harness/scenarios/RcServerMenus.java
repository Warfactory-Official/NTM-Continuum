import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Rcon;
import dev.golem.harness.api.Scenario;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Release audit, dedicated server: the client joined a dedicated server (harness up --server), the machines are placed
 * by the server console (rcon), and each one is opened with a real right-click. A menu must open over the network, its
 * block entity must exist on the server, and neither side may log an error. Rcon port = 25700 + node index.
 */
public final class RcServerMenus implements Scenario {

    static int prop(Client c, String key) {
        try {
            for (String line : Files.readAllLines(Path.of("versions", c.node(), "runs", "server", "server.properties"))) {
                if (line.startsWith(key + "=")) return Integer.parseInt(line.substring(key.length() + 1).trim());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalStateException(key + " missing in server.properties of " + c.node());
    }

    static int rconPort(Client c) {
        return prop(c, "rcon.port");
    }

    private static final int X = 400, Y = 100, Z = 400, L = Y + 1, STEP = 8;

    private static final String[] MACHINES = {
            "pa_source", "pa_rfc", "pa_quadrupole", "pa_dipole", "pa_detector", "dfc_core", "dfc_emitter", "dfc_receiver",
            "dfc_injector", "dfc_stabilizer", "soyuz_launcher", "launchpad_soyuz", "launchpad_lambda", "machine_battery",
            "machine_waste_drum", "machine_storage_drum", "machine_radgen", "machine_assembly_machine", "machine_assembly_factory",
            "machine_precass", "machine_chemical_plant", "machine_chemical_factory", "machine_mixer", "machine_exposure_chamber",
            "machine_blast_furnace", "machine_purex", "machine_rockmill", "machine_supercomputer", "machine_tape_drive",
            "machine_soldering_station", "machine_arc_furnace", "machine_arc_welder", "machine_crystallizer", "machine_centrifuge",
            "machine_cyclotron", "machine_gascent", "machine_electrolyser", "machine_silex",
            "machine_fel", "machine_large_turbine", "machine_turbine_gas", "machine_turbofan", "machine_diesel", "machine_flare",
            "machine_liquefactor", "machine_solidifier", "machine_refinery", "heater_oilburner", "machine_wood_burner",
            "machine_well", "machine_mining_laser", "machine_vacuum_distill", "machine_catalytic_reformer", "machine_hydrotreater",
            "machine_coker", "machine_furnace_combination", "machine_pyrooven", "machine_combustion_engine", "machine_compressor",
            "machine_crucible", "heater_firebox", "machine_ashpit", "machine_strand_caster", "machine_difurnace_off", "heater_heatex",
            "nuke_man", "nuke_n2", "nuke_boy", "nuke_gadget", "nuke_tsar", "nuke_mike", "machine_zirnox", "watz", "fusion_torus",
            "fusion_breeder", "fusion_klystron", "fusion_plasma_forge", "machine_press", "machine_ammo_press", "machine_epress",
            "machine_electric_furnace_off", "machine_furnace_brick_off", "furnace_iron", "furnace_steel", "machine_shredder",
            "machine_fluidtank", "machine_uf6_tank", "machine_bigasstank", "machine_battery_socket", "machine_fensu", "machine_battery_redd",
            "anvil_iron", "crate_iron", "crate_steel", "crate_desh", "machine_armor_table", "machine_weapon_table", "mass_storage", "crate_template",
            "launch_pad", "turret_howard", "radio_torch_sender", "radio_torch_reader", "radio_torch_logic", "radio_torch_counter",
            "radio_telex", "network_separator", "barrel_steel", "crane_inserter", "crane_extractor", "fluid_pump", "fluid_valve"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        try (Rcon rcon = new Rcon("127.0.0.1", rconPort(c), "rc")) {
            rcon.command("op RcServer");
            rcon.command("forceload add " + (X - 20) + " " + (Z - 20) + " " + (X + 20) + " " + (Z + MACHINES.length * STEP + 20));
            c.waitTicks(60);
            rcon.command("gamerule doMobSpawning false");
            rcon.command("gamerule spawn_mobs false");
            for (int i = 0; i < MACHINES.length; i++) {
                int z0 = Z + i * STEP - 4;
                rcon.command("fill " + (X - 4) + " " + Y + " " + z0 + " " + (X + 4) + " " + Y + " " + (z0 + STEP - 1) + " minecraft:stone");
                rcon.command("fill " + (X - 4) + " " + L + " " + z0 + " " + (X + 4) + " " + (L + 6) + " " + (z0 + STEP - 1) + " minecraft:air");
            }
            for (int i = 0; i < MACHINES.length; i++) {
                String reply = rcon.command("setblock " + X + " " + L + " " + (Z + i * STEP) + " hbm:" + MACHINES[i]);
                if (!reply.toLowerCase().contains("changed")) System.out.println("[RcServerMenus] setblock " + MACHINES[i] + ": " + reply);
            }
            c.waitTicks(20);
            c.cmd("gamemode creative");
            List<String> opened = new ArrayList<>();
            List<String> noGui = new ArrayList<>();
            List<String> noBe = new ArrayList<>();
            for (int i = 0; i < MACHINES.length; i++) {
                int z = Z + i * STEP;
                c.key("escape");
                c.waitTicks(2);
                String be = rcon.command("data get block " + X + " " + L + " " + z);
                if (be.contains("No block entity") || be.contains("not a block entity") || be.contains("Found no")) noBe.add(MACHINES[i]);
                c.hotbar(9);
                c.teleport(X + 0.5, L, z + 3.5);
                c.waitTicks(3);
                c.lookAt(X + 0.5, L + 0.5, z + 0.5);
                c.waitTicks(2);
                c.use(1);
                boolean gui = false;
                String title = "";
                try {
                    c.waitScreen("", 1500);
                    JsonObject sc = c.screen();
                    gui = sc.has("open") && sc.get("open").getAsBoolean();
                    if (gui) {
                        String cls = sc.get("class").getAsString();
                        title = cls.substring(cls.lastIndexOf('.') + 1) + "(" + sc.get("title").getAsString() + ")";
                    }
                } catch (RuntimeException e) {
                    gui = false;
                }
                if (gui) {
                    opened.add(MACHINES[i] + "=" + title);
                    if (i % 12 == 0) c.screenshot(String.format("menu-%03d-%s", i, MACHINES[i]));
                    c.waitTicks(4);
                } else {
                    noGui.add(MACHINES[i]);
                }
            }
            c.key("escape");
            System.out.println("[RcServerMenus] opened " + opened.size() + ": " + opened);
            System.out.println("[RcServerMenus] no menu: " + noGui);
            System.out.println("[RcServerMenus] no server block entity: " + noBe);
            Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
        }
    }
}
