import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * GitHub issue 10: HBM buffers in Jade's HUD through Golem Core's block-info adapter. Places a machine battery (HE),
 * a steel barrel with kerosene (fluid), a chemical plant with power and a filled tank (a folded multiblock: its
 * cells must show the core) and a firebox with heat (TU), looks at each and screenshots the HUD; the pictures are
 * the check. With Jade in the run ({@code ORG_GRADLE_PROJECT_jade.run=true} and {@code up --rebuild}) the log must
 * show Golem Core's plugin loading; without it the same steps must run clean.
 */
public final class Gh10Jade implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 14);
        c.cmd("fill -12 " + G + " -12 12 " + (Y + 10) + " 12 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.cmd("gamemode creative");

        List<String> jade = c.log(4000, "(?i)golem\\.core\\.jade|jade.*plugin");
        boolean withJade = !jade.isEmpty();
        System.out.println("[Gh10] Jade " + (withJade ? "present: " + jade.get(jade.size() - 1) : "absent"));
        c.markLog();

        // HE: a machine battery at 600k of its capacity
        c.cmd("setblock -6 " + G + " 0 hbm:machine_battery");
        c.waitTicks(5);
        c.cmd("data merge block -6 " + G + " 0 {power:600000L}");
        // fluid: a steel barrel with 4,000 mB kerosene
        c.cmd("setblock 6 " + G + " 0 hbm:barrel_steel");
        c.waitTicks(5);
        c.cmd("data merge block 6 " + G + " 0 {tank:4000,tank_type:\"hbm:kerosene\"}");

        c.cmd("clear");
        look(c, -6, 0, "01-battery-he");
        look(c, 6, 0, "02-barrel-fluid");

        // a folded multiblock and a firebox, placed by hand with HBM's gun keys on their default right and middle
        // buttons: below 26.1 Fabric keeps one mapping per key, and key.use must keep right-click with Jade loaded
        int[] plant = place(c, "hbm:machine_chemical_plant", -3.5, 0.5, "chemical_plant", -1, 3);
        int[] firebox = place(c, "hbm:heater_firebox", 4.5, 8.5, "firebox", 6, 10);
        Expect.that(plant != null, "the chemical plant formed a core");
        Expect.that(firebox != null, "the firebox formed a core");
        c.cmd("data merge block " + plant[0] + " " + plant[1] + " " + plant[2]
                + " {power:50000L,i0_type:\"hbm:kerosene\",i0:6000}");
        c.cmd("data merge block " + firebox[0] + " " + firebox[1] + " " + firebox[2] + " {heatEnergy:40000}");
        c.cmd("clear");

        // the plant's front cell (not the core, which sits inside)
        c.teleport(plant[0] + 0.5, G, plant[2] - 3.5);
        c.waitTicks(3);
        c.lookAt(plant[0] + 0.5, G + 1.5, plant[2] - 1.0);
        c.waitTicks(20);
        c.screenshot("03-chemplant-cell");
        look(c, firebox[0], firebox[2], "04-firebox-heat");

        if (withJade) Expect.logCleanSinceMark(c, "(?i)(ERROR|FATAL).*(jade|golem)");
        else Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    /** Places a machine by hand from the south, retrying a click the client dropped, and finds its core. */
    private static int[] place(Client c, String item, double fromZ, double atZ, String type, int z0, int z1) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(0.5, G, fromZ);
        c.waitTicks(5);
        c.lookAt(0.5, Y + 0.98, atZ);
        for (int attempt = 0; attempt < 3; attempt++) {
            c.waitTicks(5);
            c.use(2);
            c.waitTicks(10);
            int[] core = find(c, type, -2, 2, z0, z1);
            if (core != null) return core;
        }
        return null;
    }

    /** Looks at the south face of the block at (x, G, z) from two and a half blocks away and screenshots the HUD. */
    private static void look(Client c, int x, int z, String shot) {
        c.teleport(x + 0.5, G, z - 2.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, G + 0.5, z - 0.02);
        c.waitTicks(20);
        c.screenshot(shot);
    }

    private static int[] find(Client c, String type, int x0, int x1, int z0, int z1) {
        for (int dx = x0; dx <= x1; dx++) for (int dz = z0; dz <= z1; dz++) {
            JsonObject be = c.blockEntity(dx, G, dz);
            if (be.has("block_entity") && !be.get("block_entity").isJsonNull() && be.toString().contains(type)) {
                return new int[]{dx, G, dz};
            }
        }
        return null;
    }
}
