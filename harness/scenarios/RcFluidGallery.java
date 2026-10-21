import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Places every fluid, oil-chain and pump machine at all four facings, checks each has its block entity, lets
 * them tick, screenshots each row from above and requires a clean log.
 */
public final class RcFluidGallery implements Scenario {

    private static final int X = 400;
    private static final int Y = 100;
    private static final int Z = 400;
    private static final int L = Y + 1;
    private static final int STEP = 12;
    private static final String[] FACINGS = {"north", "east", "south", "west"};

    private static final String[] MACHINES = {
            "machine_fluidtank", "machine_bigasstank", "machine_flare", "machine_well", "machine_pumpjack",
            "machine_fracking_tower", "machine_refinery", "machine_vacuum_distill", "machine_catalytic_reformer",
            "machine_catalytic_cracker", "machine_hydrotreater", "machine_coker", "machine_liquefactor",
            "machine_solidifier", "machine_fraction_tower", "fraction_spacer", "machine_condenser",
            "machine_condenser_powered", "machine_tower_small", "machine_tower_large", "machine_pump_electric",
            "machine_pump_steam", "heat_boiler", "machine_industrial_boiler", "heater_oilburner", "machine_gascent",
            "machine_uf6_tank", "machine_puf6_tank", "machine_deuterium_tower", "machine_solar_boiler",
            "machine_wood_burner", "fluid_valve", "fluid_pump", "flow_control_pump", "fluid_switch",
            "fluid_counter_valve", "creative_fluid_source", "watz_pump", "fusion_boiler"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        String forceload = (X - 20) + " " + (Z - 20) + " " + (X + 4 * STEP + 20) + " " + (Z + MACHINES.length * STEP + 20);
        c.cmd("forceload add " + forceload);
        c.waitTicks(40);
        c.cmd("gamemode creative");
        for (int i = 0; i < MACHINES.length; i++) {
            int z0 = Z + i * STEP - 6;
            c.cmd("fill " + (X - 8) + " " + Y + " " + z0 + " " + (X + 3 * STEP + 8) + " " + Y + " " + (z0 + STEP - 1) + " minecraft:stone");
            c.cmd("fill " + (X - 8) + " " + L + " " + z0 + " " + (X + 3 * STEP + 8) + " " + (L + 12) + " " + (z0 + STEP - 1) + " minecraft:air");
        }
        c.cmd("time set 6000");
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("gamemode spectator");
        List<String> noBe = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < MACHINES.length; i++) {
            for (int f = 0; f < 4; f++) {
                String id = MACHINES[i];
                String state = "hbm:" + id + "[facing=" + FACINGS[f] + "]";
                Fixtures.setBlock(c, X + f * STEP, L, Z + i * STEP, state);
            }
        }
        c.waitTicks(30);
        for (int i = 0; i < MACHINES.length; i++) {
            for (int f = 0; f < 4; f++) {
                if (c.block(X + f * STEP, L, Z + i * STEP).get("block").getAsString().equals("minecraft:air")) {
                    Fixtures.setBlock(c, X + f * STEP, L, Z + i * STEP, "hbm:" + MACHINES[i]);
                }
            }
        }
        c.waitTicks(30);
        for (int i = 0; i < MACHINES.length; i++) {
            for (int f = 0; f < 4; f++) {
                JsonObject b = c.block(X + f * STEP, L, Z + i * STEP);
                String id = MACHINES[i];
                String got = b.get("block").getAsString();
                if (!got.equals("hbm:" + id)) { missing.add(id + "/" + FACINGS[f] + " -> " + got); continue; }
                if (b.get("block_entity").isJsonNull() && !id.equals("fraction_spacer")) noBe.add(id + "/" + FACINGS[f]);
            }
        }
        System.out.println("[RcFluidGallery] not placed: " + missing);
        System.out.println("[RcFluidGallery] no block entity: " + noBe);
        c.waitTicks(100);
        for (int i = 0; i < MACHINES.length; i += 2) {
            c.teleport(X + 1.5 * STEP, L + 14, Z + i * STEP + STEP / 2.0);
            c.waitTicks(2);
            c.lookAt(X + 1.5 * STEP, L, Z + i * STEP + STEP / 2.0);
            c.waitTicks(20);
            c.screenshot(String.format("row-%02d-%s", i, MACHINES[i]));
        }
        Expect.that(missing.isEmpty(), "all machines placed: " + missing);
        Expect.that(noBe.isEmpty(), "all machines have a block entity: " + noBe);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }
}
