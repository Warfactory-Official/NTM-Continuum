import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/** Right-clicks each fluid and oil machine and reports whether a menu opened; a log error fails the run. */
public final class RcFluidMenus implements Scenario {

    private static final int X = 600;
    private static final int Y = 100;
    private static final int Z = 600;
    private static final int L = Y + 1;
    private static final int STEP = 10;

    private static final String[] MACHINES = {
            "machine_fluidtank", "machine_bigasstank", "machine_flare", "machine_well", "machine_pumpjack",
            "machine_fracking_tower", "machine_refinery", "machine_vacuum_distill", "machine_catalytic_reformer",
            "machine_catalytic_cracker", "machine_hydrotreater", "machine_coker", "machine_liquefactor",
            "machine_solidifier", "machine_fraction_tower", "machine_condenser_powered", "machine_tower_small",
            "machine_tower_large", "machine_pump_electric", "machine_pump_steam", "heat_boiler",
            "machine_industrial_boiler", "heater_oilburner", "machine_gascent", "machine_uf6_tank",
            "machine_puf6_tank", "machine_deuterium_tower", "machine_solar_boiler", "machine_wood_burner",
            "fluid_valve", "fluid_pump", "flow_control_pump", "fluid_switch", "fluid_counter_valve",
            "creative_fluid_source", "barrel_steel", "barrel_plastic", "barrel_corroded", "barrel_tcalloy",
            "barrel_antimatter", "red_barrel", "pink_barrel", "yellow_barrel", "lox_barrel", "vitrified_barrel",
            "foundry_tank", "fluid_duct_gauge"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("forceload add " + (X - 20) + " " + (Z - 20) + " " + (X + 20) + " " + (Z + MACHINES.length * STEP + 20));
        c.waitTicks(40);
        c.cmd("gamemode creative");
        for (int i = 0; i < MACHINES.length; i++) {
            int z0 = Z + i * STEP - 5;
            c.cmd("fill " + (X - 8) + " " + Y + " " + z0 + " " + (X + 8) + " " + Y + " " + (z0 + STEP - 1) + " minecraft:stone");
            c.cmd("fill " + (X - 8) + " " + L + " " + z0 + " " + (X + 8) + " " + (L + 12) + " " + (z0 + STEP - 1) + " minecraft:air");
        }
        for (int i = 0; i < MACHINES.length; i++) {
            Fixtures.setBlock(c, X, L, Z + i * STEP, "hbm:" + MACHINES[i] + "[facing=north]");
        }
        c.waitTicks(30);
        for (int i = 0; i < MACHINES.length; i++) {
            if (c.block(X, L, Z + i * STEP).get("block").getAsString().equals("minecraft:air")) {
                Fixtures.setBlock(c, X, L, Z + i * STEP, "hbm:" + MACHINES[i]);
            }
        }
        c.waitTicks(30);
        List<String> noGui = new ArrayList<>();
        List<String> opened = new ArrayList<>();
        for (int i = 0; i < MACHINES.length; i++) {
            int z = Z + i * STEP;
            c.key("escape");
            c.waitTicks(3);
            c.hotbar(9);
            c.teleport(X + 0.5, L, z + 4.5);
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
                if (gui) title = sc.get("class").getAsString().substring(sc.get("class").getAsString().lastIndexOf('.') + 1) + "(" + sc.get("title").getAsString() + ")";
            } catch (RuntimeException e) {
                gui = false;
            }
            if (gui) {
                opened.add(MACHINES[i] + "=" + title);
                if (i % 4 == 0) c.screenshot(String.format("menu-%02d-%s", i, MACHINES[i]));
                c.key("escape");
                c.waitTicks(4);
            } else {
                noGui.add(MACHINES[i]);
            }
        }
        System.out.println("[RcFluidMenus] opened: " + opened);
        System.out.println("[RcFluidMenus] no menu: " + noGui);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }
}
