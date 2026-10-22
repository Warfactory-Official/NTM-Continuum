import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Release audit, power machines: places each generator or store by hand, opens its GUI with a real right-click and
 * takes a screenshot of the machine and of the screen. Soft checks, one entry per machine.
 */
public final class RcPowerGuis implements Scenario {

    private static final int Y = 100;
    private static final String[] BLOCKS = {
            "machine_diesel", "machine_combustion_engine", "machine_wood_burner", "machine_rtg_grey", "machine_minirtg",
            "machine_steam_engine", "machine_large_turbine", "machine_industrial_turbine", "machine_chungus",
            "machine_turbofan", "machine_turbine_gas", "machine_battery_socket", "machine_battery_redd",
            "machine_solar_boiler", "machine_industrial_boiler", "heat_boiler", "machine_hephaestus",
            "machine_industrial_generator", "machine_lpw2", "tesla", "machine_battery_potato", "machine_lithium_battery",
            "machine_schrabidium_battery", "machine_dineutronium_battery", "capacitor_bus", "machine_capacitor_copper",
            "machine_capacitor_gold", "machine_capacitor_niobium", "capacitor_tantalium", "capacitor_schrabidate",
            "hev_battery", "red_cable_gauge", "network_separator", "machine_detector", "substation", "red_connector",
            "red_connector_super", "red_pylon_large", "red_pylon_medium_steel_transformer"
    };
    private final List<String> failures = new ArrayList<>();

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);
        c.waitTicks(5);
        for (String id : BLOCKS) {
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
        c.cmd("fill -16 " + (Y + 1) + " -16 16 " + (Y + 12) + " 16 minecraft:air");
        c.waitTicks(3);
        JsonObject placed = Fixtures.placeOnTop(c, "hbm:" + id, 0, Y, 0);
        System.out.println("[" + id + "] placed: " + placed);
        c.teleport(0.5, Y + 1, -3.5);
        c.look(0, 15);
        c.waitTicks(15);
        c.screenshot(id + "-world");
        int[] k = findCore(c);
        JsonObject be = c.blockEntity(k[0], k[1], k[2]);
        String klass = be != null && be.has("block_entity") && !be.get("block_entity").isJsonNull()
                ? be.get("block_entity").getAsString() : "none";
        System.out.println("[" + id + "] block entity: " + klass);
        Fixtures.interact(c, k[0], k[1], k[2]);
        c.waitTicks(15);
        String screen = c.screenClass();
        System.out.println("[" + id + "] screen: " + screen);
        if (screen != null) {
            c.screenshot(id + "-gui");
            c.waitTicks(10);
            if (c.screenClass() == null) failures.add(id + ": screen closed by itself");
            c.key("escape");
            c.waitTicks(5);
        }
    }

    private static int[] findCore(Client c) {
        for (int dy = 0; dy <= 4; dy++)
            for (int dz = -8; dz <= 8; dz++)
                for (int dx = -6; dx <= 6; dx++) {
                    JsonObject be = c.blockEntity(dx, Y + 1 + dy, dz);
                    if (be != null && be.has("block_entity") && !be.get("block_entity").isJsonNull()) {
                        return new int[]{dx, Y + 1 + dy, dz};
                    }
                }
        return new int[]{0, Y + 1, 0};
    }
}
