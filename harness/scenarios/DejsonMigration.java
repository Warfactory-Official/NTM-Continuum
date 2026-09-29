import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Visual acceptance for the 2026-09-28 JSON-to-Dejson migration (§19ed-dejson): every asset shown here used
 * to be an authored JSON file and is now emitted by a declaration.
 *
 * <p>Three inventory sheets cover the migrated item-definition shapes — {@code hbm:machine_obj} OBJ icons
 * (direct and inside {@code minecraft:special}), RBMK column icons, Sedna guns ({@code hbm:gun}),
 * {@code select} families, the tinted flag item and plain sprites whose item models moved with them. A row of
 * hand-placed blocks covers the migrated blockstates and hand-built element models (multipart ducts, cables,
 * fence, pipe anchor, glass pane, conveyor chute, foundry channel/tank, snow-style layers, lit furnaces,
 * weapon table, RBMK gauge). The client log is checked for model and texture load errors.
 */
public final class DejsonMigration implements Scenario {

    private static final int X = 384;
    private static final int Y = 100;
    private static final int Z = 384;

    private static final String[][] SHEETS = {
            { // OBJ machine and RBMK icons
                    "hbm:machine_assembly_machine", "hbm:machine_chemical_plant", "hbm:machine_press",
                    "hbm:machine_cyclotron", "hbm:machine_fluidtank", "hbm:machine_stirling", "hbm:machine_sawmill",
                    "hbm:machine_turbofan", "hbm:machine_mining_laser", "hbm:machine_large_turbine",
                    "hbm:machine_industrial_generator", "hbm:machine_satlink", "hbm:fusion_plasma_forge",
                    "hbm:turret_chekhov", "hbm:turret_howard", "hbm:turret_himars", "hbm:turret_arty",
                    "hbm:missile_nuclear", "hbm:missile_anti_ballistic", "hbm:nuke_boy", "hbm:nuke_man",
                    "hbm:blast_door", "hbm:vault_door", "hbm:silo_hatch", "hbm:cargo_elevator", "hbm:crane_splitter",
                    "hbm:crane_partitioner", "hbm:anvil_steel", "hbm:battery_lead", "hbm:barrel_antimatter",
                    "hbm:rbmk_absorber", "hbm:rbmk_control", "hbm:rbmk_fuel_rod", "hbm:rbmk_boiler",
                    "hbm:rbmk_heater", "hbm:rbmk_gauge"},
            { // guns, armour OBJ icons, glider, gear, scaffold, HIMARS ammo
                    "hbm:gun_aberrator", "hbm:gun_am180", "hbm:gun_b92", "hbm:gun_bolter", "hbm:gun_carbine",
                    "hbm:gun_congolake", "hbm:gun_fatman", "hbm:gun_flamer", "hbm:gun_g3", "hbm:gun_greasegun",
                    "hbm:gun_heavy_revolver", "hbm:gun_henry", "hbm:gun_lag", "hbm:gun_light_revolver",
                    "hbm:gun_maresleg", "hbm:gun_minigun", "hbm:gun_panzerschreck", "hbm:gun_spas12",
                    "hbm:gun_stg77", "hbm:gun_tau", "hbm:gun_uzi", "hbm:gun_coilgun", "hbm:gun_pepperbox",
                    "hbm:gun_amat", "hbm:bj_helmet", "hbm:bj_plate", "hbm:bj_plate_jetpack", "hbm:bj_legs",
                    "hbm:bj_boots", "hbm:dns_helmet", "hbm:t51_plate", "hbm:t51_boots", "hbm:jetpack_glider",
                    "hbm:gear_large", "hbm:steel_scaffold", "hbm:ammo_himars"},
            { // select families, flag tint, sprites and block items of the migrated blockstates
                    "hbm:blueprints", "hbm:blueprint_folder", "hbm:ammo_container", "hbm:concrete_colored",
                    "hbm:concrete_colored_ext", "hbm:weapon_mod_generic", "hbm:weapon_mod_special",
                    "hbm:weapon_mod_caliber", "hbm:scraps", "hbm:lightstone", "hbm:ammo_bag", "hbm:casing_bag",
                    "hbm:ammo_standard_g12", "hbm:ammo_standard_p45_fmj", "hbm:ammo_standard_nuke_standard",
                    "hbm:key", "hbm:key_fake", "hbm:gun_pa_ranged", "hbm:fluid_duct_neo", "hbm:fluid_duct_gauge",
                    "hbm:fluid_valve", "hbm:foam_layer", "hbm:foundry_channel", "hbm:foundry_tank",
                    "hbm:foundry_mold", "hbm:pipe_anchor", "hbm:pneumatic_tube", "hbm:red_cable_classic",
                    "hbm:red_cable_paintable", "hbm:sand_boron_layer", "hbm:machine_weapon_table",
                    "hbm:machine_difurnace_off", "hbm:machine_electric_furnace_off", "hbm:machine_furnace_brick_off",
                    "hbm:fence_metal", "hbm:reinforced_glass_pane"},
    };

    /** Hand-placed in pairs so connection-driven multipart states actually vary. */
    private static final String[] PLACED = {
            "hbm:fluid_duct_neo", "hbm:red_cable", "hbm:fence_metal", "hbm:reinforced_glass_pane",
            "hbm:pneumatic_tube", "hbm:conveyor_chute", "hbm:foundry_channel", "hbm:foundry_tank",
            "hbm:pipe_anchor", "hbm:foam_layer", "hbm:sand_boron_layer", "hbm:machine_difurnace_off",
            "hbm:machine_electric_furnace_off", "hbm:machine_weapon_table", "hbm:rbmk_gauge", "hbm:wood_barrier",
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 24);
        c.cmd("effect give @s minecraft:resistance 900 4 true");

        for (int i = 0; i < SHEETS.length; i++) inventorySheet(c, i, SHEETS[i]);

        c.cmd("gamemode creative");
        c.waitTicks(2);
        for (int i = 0; i < PLACED.length; i++) {
            int x = X - 15 + (i % 8) * 4;
            int z = Z - 4 + (i / 8) * 5;
            String id = PLACED[i];
            Fixtures.placeOnTop(c, id, x, Y, z);
            Fixtures.placeOnTop(c, id, x + 1, Y, z);
            c.waitTicks(2);
            Expect.block(c, x, Y + 1, z, id);
        }
        shot(c, "20-placed-blocks", X + 0.5, Y + 9, Z - 14.5, X + 0.5, Y + 1.0, Z + 1.0);
        shot(c, "21-placed-blocks-close-left", X - 8.5, Y + 4, Z - 8.5, X - 8.5, Y + 1.0, Z);
        shot(c, "22-placed-blocks-close-right", X + 8.5, Y + 4, Z - 8.5, X + 8.5, Y + 1.0, Z);

        for (String error : c.logSinceMark(
                "(Unable to load model|Missing model|missing texture|Failed to load texture|Unable to resolve texture"
                        + "|Couldn't parse|Exception loading blockstate|Unable to load definition|ERROR)")) {
            boolean offlineRealms = error.contains("[RealmsAvailability]");
            Expect.that(offlineRealms, "unexpected client error: " + error);
        }
    }

    private static void inventorySheet(Client c, int index, String[] items) {
        c.cmd("gamemode creative");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(8);
        c.cmd("gamemode survival");
        c.waitTicks(2);
        c.cmd("clear");
        for (String id : items) c.cmd("give @s " + id + " 1");
        c.waitTicks(4);
        for (String id : items) Expect.inventoryHolds(c, id, 1);
        c.key("e");
        c.waitScreen("Inventory", 5_000L);
        Expect.screenOpen(c, "Inventory");
        c.waitTicks(10);
        c.screenshot("1" + index + "-inventory-sheet-" + index);
        c.key("escape");
        c.waitTicks(3);
    }

    private static void shot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.hotbar(9);
        c.cmd("gamemode spectator");
        c.waitTicks(2);
        c.teleport(x, y, z);
        c.waitTicks(15);
        c.lookAt(tx, ty, tz);
        c.waitTicks(12);
        c.screenshot(name);
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }
}
