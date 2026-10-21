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
public final class RcMetalGallery implements Scenario {

    private static final int X = 600;
    private static final int Y = 100;
    private static final int Z = 600;
    private static final int L = Y + 1;
    private static final int STEP = 14;
    private static final String[] FACINGS = {"north", "east", "south", "west"};

    private static final String[] MACHINES = {
            "heater_firebox", "heater_oven", "heater_oilburner", "heater_electric", "heater_heatex",
            "furnace_iron", "furnace_steel", "machine_furnace_combination", "machine_furnace_brick_off",
            "machine_crucible", "foundry_mold", "foundry_basin", "foundry_channel", "foundry_tank",
            "foundry_outlet", "foundry_slagtap", "machine_strand_caster", "machine_difurnace_off",
            "machine_difurnace_rtg_off", "machine_blast_furnace", "machine_rotary_furnace",
            "machine_rtg_furnace_off", "machine_electric_furnace_off", "machine_pyrooven", "machine_arc_welder",
            "machine_soldering_station", "machine_arc_furnace", "machine_ashpit",
            "anvil_iron", "anvil_lead", "anvil_steel", "anvil_desh", "anvil_ferrouranium", "anvil_saturnite",
            "anvil_bismuth_bronze", "anvil_arsenic_bronze", "anvil_schrabidate", "anvil_dnt", "anvil_osmiridium",
            "anvil_murky"};

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
        System.out.println("[RcMetalGallery] not placed: " + missing);
        System.out.println("[RcMetalGallery] no block entity: " + noBe);
        c.waitTicks(100);
        for (int i = 0; i < MACHINES.length; i++) {
            for (int pair = 0; pair < 2; pair++) {
                double cx = X + (pair * 2 + 0.5) * STEP + 0.5;
                c.teleport(cx, L + 6, Z + i * STEP - 10);
                c.waitTicks(2);
                c.lookAt(cx, L + 2.0, Z + i * STEP + 0.5);
                c.waitTicks(12);
                c.screenshot(String.format("row-%02d-%s-%s", i, MACHINES[i], pair == 0 ? "NE" : "SW"));
            }
        }
        Expect.that(missing.isEmpty(), "all machines placed: " + missing);
        Expect.that(noBe.isEmpty(), "all machines have a block entity: " + noBe);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointer|ClassCast|IllegalState");
    }
}
