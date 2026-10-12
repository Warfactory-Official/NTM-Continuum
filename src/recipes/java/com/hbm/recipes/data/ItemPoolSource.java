package com.hbm.recipes.data;

import static com.hbm.itempool.ItemPools.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPool.PoolFile;
import com.hbm.itempool.ItemPool.Row;
import dev.golem.core.world.DataSource;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.HolderLookup;

/**
 * 1.7 {@code ItemPool.initialize()}: the pools of {@code ItemPoolsLegacy}, {@code ItemPoolsComponent},
 * {@code ItemPoolsSingle}, {@code ItemPoolsRedRoom}, {@code ItemPoolsSatellite}, {@code ItemPoolsPile},
 * {@code ItemPoolsC130} and {@code ItemPoolsVendingMachine} in that order, each pool's entries in 1.7's order, written as
 * {@code data/hbm/item_pool/<pool>.json} for {@link ItemPool}. A 1.7 (item, metadata) pair is the port's split item id
 * (the 1.7 expression is noted where it was not a plain item); a metadata fluid container names its fluid, and an
 * {@code ItemStack} built in code is the port item with the components that build states.
 */
public final class ItemPoolSource extends DataSource<PoolFile> {

    public ItemPoolSource() {
        super(ItemPool.FOLDER);
    }

    @Override
    public JsonElement encode(PoolFile row, HolderLookup.Provider registries) {
        return row.write();
    }

    @Override
    public PoolFile decode(JsonElement json, HolderLookup.Provider registries) {
        return PoolFile.read(json.getAsJsonObject());
    }

    @Override
    protected void registerDefaults() {
        // 1.7 ItemPoolsLegacy
        pool(POOL_GENERIC,
                weighted("minecraft:bread", 1, 5, 8),
                weighted("hbm:twinkie", 1, 3, 6),
                weighted("minecraft:iron_ingot", 2, 6, 10),
                weighted("hbm:ingot_steel", 2, 5, 7),
                weighted("hbm:ingot_beryllium", 1, 2, 4),
                weighted("hbm:ingot_titanium", 1, 1, 3),
                weighted("hbm:circuit_vacuum_tube", 1, 1, 5), // circuit, EnumCircuitType.VACUUM_TUBE
                weighted("hbm:gun_light_revolver", 1, 1, 3),
                weighted("hbm:ammo_standard_m357_sp", 2, 6, 4), // ammo_standard, EnumAmmo.M357_SP
                weighted("hbm:ammo_standard_g12_bp", 3, 6, 3), // ammo_standard, EnumAmmo.G12_BP
                weighted("hbm:ammo_standard_g26_flare_supply", 1, 1, 1), // ammo_standard, EnumAmmo.G26_FLARE_SUPPLY
                weighted("hbm:gun_kit_1", 1, 3, 4),
                weighted("hbm:gun_maresleg", 1, 1, 1),
                weighted("hbm:casing_small", 4, 10, 3), // casing, EnumCasingType.SMALL
                weighted("hbm:casing_shotshell", 4, 10, 3), // casing, EnumCasingType.SHOTSHELL
                weighted("hbm:cordite", 4, 6, 5),
                weighted("hbm:battery_redstone", 1, 1, 1), // battery_pack, EnumBatteryPack.BATTERY_REDSTONE
                weighted("hbm:scrap", 1, 3, 10),
                weighted("hbm:dust", 2, 4, 9),
                weighted("hbm:bottle_opener", 1, 1, 2),
                weighted("hbm:bottle_nuka", 1, 3, 4),
                weighted("hbm:bottle_cherry", 1, 1, 2),
                weighted("hbm:stealth_boy", 1, 1, 1),
                weighted("hbm:cap_nuka", 1, 15, 7),
                filled("hbm:canister_full", "DIESEL", 1, 2, 2), // canister_full, Fluids.DIESEL.getID()
                filled("hbm:canister_full", "BIOFUEL", 1, 2, 3), // canister_full, Fluids.BIOFUEL.getID()
                weighted("hbm:gas_mask_m65", damage(60), 1, 1, 2), // gas_mask_m65, 60
                weighted("hbm:gas_mask_filter", 1, 1, 3),
                weighted("hbm:blueprint_folder", 1, 1, 1),
                weighted("hbm:coin_token", 1, 1, 2));
        pool(POOL_ANTENNA,
                weighted("hbm:twinkie", 1, 3, 4),
                weighted("hbm:ingot_steel", 1, 2, 7),
                weighted("hbm:ingot_red_copper", 1, 1, 4),
                weighted("hbm:ingot_titanium", 1, 3, 5),
                weighted("hbm:wire_fine_red_copper", 2, 3, 7), // wire_fine, Mats.MAT_MINGRADE.id
                weighted("hbm:circuit_vacuum_tube", 1, 1, 4), // circuit, EnumCircuitType.VACUUM_TUBE
                weighted("hbm:circuit_capacitor", 1, 1, 2), // circuit, EnumCircuitType.CAPACITOR
                weighted("hbm:battery_redstone", 1, 1, 1), // battery_pack, EnumBatteryPack.BATTERY_REDSTONE
                weighted("hbm:powder_iodine", 1, 1, 1),
                weighted("hbm:powder_bromine", 1, 1, 1),
                weighted("hbm:steel_poles", 1, 4, 8),
                weighted("hbm:steel_scaffold", 1, 3, 8),
                weighted("hbm:pole_top", 1, 1, 4),
                weighted("hbm:pole_satellite_receiver", 1, 1, 7),
                weighted("hbm:scrap", 1, 3, 10),
                weighted("hbm:dust", 2, 4, 9),
                weighted("hbm:bottle_opener", 1, 1, 2),
                weighted("hbm:bottle_nuka", 1, 3, 4),
                weighted("hbm:bottle_cherry", 1, 1, 2),
                weighted("hbm:stealth_boy", 1, 1, 1),
                weighted("hbm:cap_nuka", 1, 15, 7),
                weighted("hbm:bomb_caller_carpet", 1, 1, 1), // bomb_caller, 0
                weighted("hbm:gas_mask_filter", 1, 1, 2));
        pool(POOL_EXPENSIVE,
                weighted("hbm:chlorine_pinwheel", 1, 1, 1),
                weighted("hbm:circuit_vacuum_tube", 1, 1, 4), // circuit, EnumCircuitType.VACUUM_TUBE
                weighted("hbm:circuit_analog", 1, 1, 3), // circuit, EnumCircuitType.ANALOG
                weighted("hbm:circuit_chip", 1, 1, 2), // circuit, EnumCircuitType.CHIP
                weighted("hbm:gun_kit_1", 1, 3, 6),
                weighted("hbm:gun_kit_2", 1, 2, 3),
                weighted("hbm:gun_panzerschreck", 1, 1, 4),
                weighted("hbm:ammo_standard_rocket_he", 1, 4, 5), // ammo_standard, EnumAmmo.ROCKET_HE
                weighted("hbm:ammo_standard_g26_flare_supply", 1, 1, 5), // ammo_standard, EnumAmmo.G26_FLARE_SUPPLY
                weighted("hbm:ammo_standard_g26_flare_weapon", 1, 1, 3), // ammo_standard, EnumAmmo.G26_FLARE_WEAPON
                weighted("hbm:grenade_universal", grenade(1, "nuke", "nuclear", "s7"), 1, 1, 2), // ItemGrenadeUniversal.make(EnumGrenadeShell.NUKE, EnumGrenadeFilling.NUCLEAR, EnumGrenadeFuze.S7)
                weighted("hbm:grenade_universal", grenade(4, "frag", "cluster", "s7"), 1, 3, 3), // ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.CLUSTER, EnumGrenadeFuze.S7)
                weighted("hbm:grenade_extra_triplex", 1, 1, 1), // grenade_extra, EnumGrenadeExtra.TRIPLEX
                weighted("hbm:stealth_boy", 1, 1, 2),
                weighted("hbm:battery_lithium", 1, 1, 1), // battery_pack, EnumBatteryPack.BATTERY_LITHIUM
                weighted("hbm:syringe_awesome", 1, 1, 1),
                weighted("hbm:fusion_core", 1, 1, 4),
                weighted("hbm:bottle_nuka", 1, 3, 6),
                weighted("hbm:bottle_quantum", 1, 1, 3),
                weighted("hbm:red_barrel", 1, 1, 6),
                filled("hbm:canister_full", "DIESEL", 1, 2, 2), // canister_full, Fluids.DIESEL.getID()
                filled("hbm:canister_full", "BIOFUEL", 1, 2, 3), // canister_full, Fluids.BIOFUEL.getID()
                weighted("hbm:gas_mask_m65", damage(60), 1, 1, 5), // gas_mask_m65, 60
                weighted("hbm:bomb_caller_carpet", 1, 1, 2), // bomb_caller, 0
                weighted("hbm:bomb_caller_napalm", 1, 1, 1), // bomb_caller, 1
                weighted("hbm:bomb_caller_chlorine", 1, 1, 1), // bomb_caller, 2
                weighted("hbm:gas_mask_filter", 1, 1, 4),
                weighted("hbm:launch_code_piece", 1, 1, 1),
                weighted("hbm:gun_double_barrel", 1, 1, 1),
                weighted("hbm:blueprint_folder_discover", 1, 1, 1)); // blueprint_folder, 1
        pool(POOL_NUKE_TRASH,
                weighted("hbm:nugget_u238", 3, 12, 5),
                weighted("hbm:nugget_pu240", 3, 8, 5),
                weighted("hbm:nugget_neptunium", 1, 4, 3),
                weighted("hbm:rod_u238", 1, 1, 3), // rod, BreedingRodType.U238
                weighted("hbm:rod_dual_u238", 1, 1, 3), // rod_dual, BreedingRodType.U238
                weighted("hbm:rod_quad_u238", 1, 1, 3), // rod_quad, BreedingRodType.U238
                weighted("hbm:bottle_quantum", 1, 1, 1),
                weighted("hbm:gas_mask_m65", damage(60), 1, 1, 5), // gas_mask_m65, 60
                weighted("hbm:hazmat_kit", 1, 1, 1),
                weighted("hbm:gas_mask_filter", 1, 1, 5),
                weighted("hbm:yellow_barrel", 1, 1, 2));
        pool(POOL_NUKE_MISC,
                weighted("hbm:nugget_u235", 3, 12, 5),
                weighted("hbm:nugget_pu238", 3, 12, 5),
                weighted("hbm:nugget_ra226", 3, 6, 5),
                weighted("hbm:rod_u235", 1, 1, 3), // rod, BreedingRodType.U235
                weighted("hbm:rod_dual_u235", 1, 1, 3), // rod_dual, BreedingRodType.U235
                weighted("hbm:rod_quad_u235", 1, 1, 3), // rod_quad, BreedingRodType.U235
                weighted("hbm:rod_zirnox_uranium_fuel", 1, 1, 4), // rod_zirnox, EnumZirnoxType.URANIUM_FUEL
                weighted("hbm:rod_zirnox_mox_fuel", 1, 1, 4), // rod_zirnox, EnumZirnoxType.MOX_FUEL
                weighted("hbm:rod_zirnox_lithium_fuel", 1, 1, 3), // rod_zirnox, EnumZirnoxType.LITHIUM
                weighted("hbm:rod_zirnox_thorium_fuel", 1, 1, 3), // rod_zirnox, EnumZirnoxType.THORIUM_FUEL
                weighted("hbm:rod_dual_thf", 1, 1, 3), // rod_dual, BreedingRodType.THF
                weighted("hbm:rod_zirnox_tritium", 1, 1, 1),
                weighted("hbm:rod_zirnox_u233_fuel", 1, 1, 1), // rod_zirnox, EnumZirnoxType.U233_FUEL
                weighted("hbm:rod_zirnox_u235_fuel", 1, 1, 1), // rod_zirnox, EnumZirnoxType.U235_FUEL
                weighted("hbm:pellet_rtg", 1, 1, 3),
                weighted("hbm:powder_thorium", 1, 1, 1),
                weighted("hbm:powder_neptunium", 1, 1, 1),
                weighted("hbm:powder_strontium", 1, 1, 1),
                weighted("hbm:powder_cobalt", 1, 1, 1),
                weighted("hbm:bottle_quantum", 1, 1, 1),
                weighted("hbm:gas_mask_m65", damage(60), 1, 1, 5), // gas_mask_m65, 60
                weighted("hbm:hazmat_kit", 1, 1, 2),
                weighted("hbm:gas_mask_filter", 1, 1, 5),
                weighted("hbm:yellow_barrel", 1, 3, 3));
        pool(POOL_VERTIBIRD,
                weighted("hbm:t51_helmet", 1, 1, 15),
                weighted("hbm:t51_plate", 1, 1, 15),
                weighted("hbm:t51_legs", 1, 1, 15),
                weighted("hbm:t51_boots", 1, 1, 15),
                weighted("hbm:fusion_core", 1, 1, 10),
                weighted("hbm:gun_light_revolver", 1, 1, 4),
                weighted("hbm:gun_kit_1", 2, 3, 4),
                weighted("hbm:ammo_standard_m357_fmj", 1, 24, 4), // ammo_standard, EnumAmmo.M357_FMJ
                weighted("hbm:ammo_standard_g40_he", 1, 6, 3), // ammo_standard, EnumAmmo.G40_HE
                weighted("hbm:ammo_standard_g26_flare_weapon", 1, 1, 5), // ammo_standard, EnumAmmo.G26_FLARE_WEAPON
                weighted("hbm:rod_u235", 1, 1, 2), // rod, BreedingRodType.U235
                weighted("hbm:billet_uranium_fuel", 1, 1, 2),
                weighted("hbm:ingot_uranium_fuel", 1, 1, 2),
                weighted("hbm:bottle_nuka", 1, 3, 6),
                weighted("hbm:bottle_quantum", 1, 1, 3),
                weighted("hbm:stealth_boy", 1, 1, 7),
                weighted("hbm:gas_mask_m65", 1, 1, 5),
                weighted("hbm:gas_mask_filter", 1, 1, 5),
                weighted("hbm:grenade_universal", grenade(1, "nuke", "nuclear", "s7"), 1, 2, 2), // ItemGrenadeUniversal.make(EnumGrenadeShell.NUKE, EnumGrenadeFilling.NUCLEAR, EnumGrenadeFuze.S7)
                weighted("hbm:bomb_caller_carpet", 1, 1, 1), // bomb_caller, 0
                weighted("hbm:bomb_caller_napalm", 1, 1, 1), // bomb_caller, 1
                weighted("hbm:bomb_caller_chlorine", 1, 1, 2)); // bomb_caller, 2
        pool(POOL_SPACESHIP,
                weighted("hbm:battery_lead", 1, 1, 2), // battery_pack, EnumBatteryPack.BATTERY_LEAD
                weighted("hbm:coil_copper", 2, 16, 5),
                weighted("hbm:wire_fine_red_copper", 8, 32, 5), // wire_fine, Mats.MAT_MINGRADE.id
                filled("hbm:cell", "DEUTERIUM", 1, 8, 5), // cell_deuterium, 0
                filled("hbm:cell", "TRITIUM", 1, 8, 5), // cell_tritium, 0
                filled("hbm:cell", "AMAT", 1, 1, 1), // cell_antimatter, 0
                weighted("hbm:powder_neodymium", 1, 1, 1),
                weighted("hbm:powder_niobium", 1, 1, 1),
                weighted("hbm:wire_dense_red_copper", 2, 4, 5), // wire_dense, Mats.MAT_MINGRADE.id
                weighted("hbm:wire_dense_gold", 1, 3, 5), // wire_dense, Mats.MAT_GOLD.id
                weighted("hbm:pwr_fuel", 1, 2, 5),
                weighted("hbm:block_tungsten", 3, 8, 5),
                weighted("hbm:red_wire_coated", 4, 8, 5),
                weighted("hbm:red_cable", 8, 16, 5));

        // 1.7 ItemPoolsComponent
        pool(POOL_MACHINE_PARTS,
                weighted("hbm:plate_steel", 1, 5, 5),
                weighted("hbm:shell_steel", 1, 3, 3), // shell, Mats.MAT_STEEL.id
                weighted("hbm:plate_polymer", 1, 6, 5),
                weighted("hbm:bolt_steel", 4, 16, 3), // bolt, Mats.MAT_STEEL.id
                weighted("hbm:bolt_tungsten", 4, 16, 3), // bolt, Mats.MAT_TUNGSTEN.id
                weighted("hbm:coil_tungsten", 1, 2, 5),
                weighted("hbm:motor", 1, 2, 4),
                weighted("hbm:coil_copper", 1, 3, 4),
                weighted("hbm:coil_copper_torus", 1, 2, 3),
                weighted("hbm:wire_fine_red_copper", 1, 8, 5), // wire_fine, Mats.MAT_MINGRADE.id
                weighted("hbm:piston_selenium", 1, 1, 3),
                weighted("hbm:battery_lead", 1, 1, 3), // battery_pack, EnumBatteryPack.BATTERY_LEAD
                weighted("hbm:circuit_vacuum_tube", 1, 2, 4), // circuit, EnumCircuitType.VACUUM_TUBE
                weighted("hbm:circuit_pcb", 1, 3, 5), // circuit, EnumCircuitType.PCB
                weighted("hbm:circuit_capacitor", 1, 1, 3), // circuit, EnumCircuitType.CAPACITOR
                weighted("hbm:blade_titanium", 1, 8, 1),
                weighted("hbm:blueprint_folder", 1, 1, 1));
        pool(POOL_NUKE_FUEL,
                weighted("hbm:billet_uranium", 1, 4, 4),
                weighted("hbm:billet_th232", 1, 3, 3),
                weighted("hbm:billet_uranium_fuel", 1, 3, 5),
                weighted("hbm:billet_mox_fuel", 1, 3, 5),
                weighted("hbm:billet_thorium_fuel", 1, 3, 3),
                weighted("hbm:billet_ra226be", 1, 2, 2),
                weighted("hbm:billet_beryllium", 1, 1, 1),
                weighted("hbm:nugget_u233", 1, 1, 1),
                weighted("hbm:nugget_uranium_fuel", 1, 1, 1),
                weighted("hbm:rod_zirnox_empty", 1, 3, 3),
                weighted("hbm:ingot_graphite", 1, 4, 3),
                weighted("hbm:pile_rod_uranium", 2, 5, 3),
                weighted("hbm:pile_rod_source", 1, 2, 2),
                weighted("hbm:reacher", 1, 1, 3),
                weighted("hbm:screwdriver", 1, 1, 2));
        pool(POOL_SILO,
                weighted("hbm:missile_generic", 1, 1, 4),
                weighted("hbm:missile_incendiary", 1, 1, 4),
                weighted("hbm:gas_mask_m65", 1, 1, 5),
                weighted("hbm:battery_lead", 1, 1, 3), // battery_pack, EnumBatteryPack.BATTERY_LEAD
                weighted("hbm:designator", 1, 1, 5),
                weighted("hbm:thruster_small", 1, 1, 5),
                weighted("hbm:thruster_medium", 1, 1, 4),
                weighted("hbm:fuel_tank_small", 1, 1, 5),
                weighted("hbm:fuel_tank_medium", 1, 1, 4),
                weighted("hbm:bomb_caller_carpet", 1, 1, 1), // bomb_caller, 0
                weighted("hbm:bomb_caller_orange", 1, 1, 1), // bomb_caller, 3
                weighted("hbm:bottle_nuka", 1, 3, 10));
        pool(POOL_OFFICE_TRASH,
                weighted("minecraft:paper", 1, 12, 10),
                weighted("minecraft:book", 1, 3, 4),
                weighted("hbm:twinkie", 1, 2, 6),
                weighted("hbm:coffee", 1, 1, 4),
                weighted("hbm:flame_politics", 1, 1, 2),
                weighted("hbm:ring_pull", 1, 1, 4),
                weighted("hbm:can_empty", 1, 1, 2),
                weighted("hbm:can_creature", 1, 2, 2),
                weighted("hbm:can_smart", 1, 3, 2),
                weighted("hbm:can_mrsugar", 1, 2, 2),
                weighted("hbm:cap_nuka", 1, 16, 2),
                weighted("hbm:book_guide_starter", 1, 1, 1), // book_guide, 3
                weighted("hbm:deco_computer_ibm_300pl", 1, 1, 1), // deco_computer, 0
                weighted("hbm:blueprint_folder", 1, 1, 1),
                weighted("hbm:coin_token", 1, 1, 2));
        pool(POOL_FILING_CABINET,
                weighted("minecraft:paper", 1, 12, 240),
                weighted("minecraft:book", 1, 3, 90),
                weighted("minecraft:map", 1, 1, 50),
                weighted("minecraft:writable_book", 1, 1, 30),
                weighted("hbm:cigarette", 1, 16, 20),
                weighted("hbm:dust", 1, 1, 40),
                weighted("hbm:dust_tiny", 1, 3, 75),
                weighted("hbm:ink", 1, 1, 1),
                weighted("hbm:screwdriver", 1, 1, 10),
                weighted("hbm:blueprint_folder", 1, 1, 5),
                weighted("hbm:coin_token", 1, 1, 30));
        pool(POOL_SOLID_FUEL,
                weighted("hbm:solid_fuel", 1, 5, 1),
                weighted("hbm:solid_fuel_presto", 1, 2, 2),
                weighted("hbm:ball_dynamite", 1, 4, 2),
                weighted("hbm:coke_petroleum", 1, 3, 1), // coke, EnumCokeType.PETROLEUM
                weighted("minecraft:redstone", 1, 3, 1),
                weighted("hbm:niter", 1, 3, 1));
        pool(POOL_VAULT_LAB,
                weighted("hbm:blowtorch", fluidAmount(0), 1, 1, 4), // ItemBlowtorch.getEmptyTool(blowtorch)
                weighted("hbm:chemistry_set", 1, 1, 15),
                weighted("hbm:screwdriver", 1, 1, 10),
                weighted("hbm:nugget_mercury_tiny", 1, 1, 3), // nugget_mercury, 0
                weighted("hbm:morning_glory", 1, 1, 1),
                weighted("hbm:filter_coal", 1, 1, 5),
                weighted("hbm:dust", 1, 3, 25),
                weighted("minecraft:paper", 1, 2, 15),
                weighted("hbm:cell", 1, 1, 5), // cell_empty, 0
                weighted("minecraft:glass_bottle", 1, 1, 5),
                weighted("hbm:powder_iodine", 1, 1, 1),
                weighted("hbm:powder_bromine", 1, 1, 1),
                weighted("hbm:powder_cobalt", 1, 1, 1),
                weighted("hbm:powder_neodymium", 1, 1, 1),
                weighted("hbm:powder_boron", 1, 1, 1),
                weighted("hbm:blueprint_folder_discover", 1, 1, 1)); // blueprint_folder, 1
        pool(POOL_VAULT_LOCKERS,
                weighted("hbm:robes_helmet", 1, 1, 1),
                weighted("hbm:robes_plate", 1, 1, 1),
                weighted("hbm:robes_legs", 1, 1, 1),
                weighted("hbm:robes_boots", 1, 1, 1),
                weighted("hbm:jackt", 1, 1, 1),
                weighted("hbm:jackt2", 1, 1, 1),
                weighted("hbm:gas_mask_m65", 1, 1, 2),
                weighted("hbm:gas_mask_mono", 1, 1, 2),
                weighted("hbm:goggles", 1, 1, 2),
                weighted("hbm:gas_mask_filter", 1, 1, 4),
                weighted("hbm:flame_opinion", 1, 3, 5),
                weighted("hbm:flame_conspiracy", 1, 3, 5),
                weighted("hbm:flame_politics", 1, 3, 5),
                weighted("hbm:definitelyfood", 2, 7, 5),
                weighted("hbm:cigarette", 1, 8, 5),
                weighted("hbm:armor_polish", 1, 1, 3),
                weighted("hbm:gun_kit_1", 1, 1, 3),
                weighted("hbm:rag", 1, 3, 5),
                weighted("minecraft:paper", 1, 6, 7),
                weighted("minecraft:clock", 1, 1, 3),
                weighted("minecraft:book", 1, 5, 10),
                weighted("minecraft:experience_bottle", 1, 3, 1),
                weighted("hbm:blueprint_folder", 1, 1, 1),
                weighted("hbm:blueprint_folder_discover", 1, 1, 1), // blueprint_folder, 1
                weighted("hbm:ammo_container", 1, 1, 1),
                weighted("hbm:coin_token", 1, 1, 5));
        pool(POOL_METEOR_SAFE,
                weighted("hbm:book_of_", 1, 1, 1),
                weighted("hbm:stamp_book_printing1", 1, 1, 1), // stamp_book, 0
                weighted("hbm:stamp_book_printing2", 1, 1, 1), // stamp_book, 1
                weighted("hbm:stamp_book_printing3", 1, 1, 1), // stamp_book, 2
                weighted("hbm:stamp_book_printing4", 1, 1, 1), // stamp_book, 3
                weighted("hbm:stamp_book_printing5", 1, 1, 1), // stamp_book, 4
                weighted("hbm:stamp_book_printing6", 1, 1, 1), // stamp_book, 5
                weighted("hbm:stamp_book_printing7", 1, 1, 1), // stamp_book, 6
                weighted("hbm:stamp_book_printing8", 1, 1, 1)); // stamp_book, 7
        pool(POOL_OIL_RIG,
                weighted("hbm:oil_detector", 1, 1, 1),
                filled("hbm:canister_full", "OIL", 1, 4, 5), // canister_full, Fluids.OIL.getID()
                weighted("hbm:canister_empty", 4, 16, 10),
                weighted("hbm:circuit_analog", 1, 4, 1), // circuit,EnumCircuitType.ANALOG
                weighted("hbm:circuit_capacitor", 1, 1, 3)); // circuit, EnumCircuitType.CAPACITOR
        pool(POOL_RTG,
                weighted("hbm:pellet_rtg_depleted_lead", 1, 1, 40), // pellet_rtg_depleted, ItemRTGPelletDepleted.DepletedRTGMaterial.LEAD
                weighted("hbm:pellet_rtg_weak", 0, 1, 1));
        pool(POOL_REPAIR_MATERIALS,
                weighted("hbm:ingot_aluminium", 2, 8, 3),
                weighted("hbm:ingot_steel", 0, 12, 4),
                weighted("hbm:plate_aluminium", 5, 12, 3),
                weighted("hbm:plate_iron", 6, 16, 3),
                weighted("hbm:plate_steel", 2, 12, 2),
                weighted("hbm:ingot_tungsten", 0, 2, 1),
                weighted("hbm:deco_aluminium", 12, 24, 4),
                weighted("hbm:deco_steel", 5, 12, 2),
                weighted("hbm:block_aluminium", 0, 2, 1),
                weighted("hbm:block_steel", 0, 1, 1),
                weighted("hbm:bolt_steel", 4, 16, 3), // bolt, Mats.MAT_STEEL.id
                weighted("hbm:circuit_vacuum_tube", 1, 2, 4), // circuit, EnumCircuitType.VACUUM_TUBE
                weighted("hbm:circuit_analog", 1, 3, 5), // circuit, EnumCircuitType.ANALOG
                weighted("hbm:circuit_capacitor", 1, 1, 3)); // circuit, EnumCircuitType.CAPACITOR

        // 1.7 ItemPoolsSingle
        pool(POOL_VAULT_RUSTY,
                weighted("minecraft:gold_ingot", 3, 14, 1),
                weighted("hbm:gun_heavy_revolver", 1, 1, 2),
                weighted("hbm:pin", 8, 8, 1),
                weighted("hbm:gun_am180", 1, 1, 1),
                weighted("hbm:bottle_quantum", 1, 3, 1),
                weighted("hbm:ingot_cobalt", 4, 12, 1),
                weighted("hbm:ammo_standard_bmg50_fmj", 24, 48, 1), // ammo_standard, EnumAmmo.BMG50_FMJ
                weighted("hbm:ammo_standard_p9_jhp", 48, 64, 2), // ammo_standard, EnumAmmo.P9_JHP
                weighted("hbm:circuit_chip", 3, 6, 1), // circuit, EnumCircuitType.CHIP
                weighted("hbm:gas_mask_m65", 1, 1, 1),
                weighted("hbm:grenade_universal", grenade(4, "frag", "he", "s3"), 1, 1, 1), // ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.HE, EnumGrenadeFuze.S3)
                weighted("hbm:grenade_universal", grenade(4, "frag", "inc", "s3"), 1, 1, 1), // ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.INC, EnumGrenadeFuze.S3)
                weighted("minecraft:diamond", 1, 2, 1));
        pool(POOL_VAULT_STANDARD,
                weighted("hbm:ingot_desh", 2, 6, 1),
                weighted("hbm:powder_desh_mix", 1, 5, 1),
                weighted("minecraft:diamond", 3, 6, 1),
                weighted("hbm:ammo_standard_nuke_standard", 1, 1, 1), // ammo_standard, EnumAmmo.NUKE_STANDARD
                weighted("hbm:ammo_container", 1, 1, 1),
                weighted("hbm:grenade_universal", grenade(1, "nuke", "nuclear", "s7"), 1, 1, 1), // ItemGrenadeUniversal.make(EnumGrenadeShell.NUKE, EnumGrenadeFilling.NUCLEAR, EnumGrenadeFuze.S7)
                weighted("hbm:grenade_universal", grenade(2, "tech", "emp", "s3"), 1, 6, 1), // ItemGrenadeUniversal.make(EnumGrenadeShell.TECH, EnumGrenadeFilling.EMP, EnumGrenadeFuze.S3)
                weighted("hbm:powder_yellowcake", 16, 24, 1),
                weighted("hbm:gun_uzi", 1, 1, 1),
                weighted("hbm:circuit_vacuum_tube", 12, 16, 1), // circuit, EnumCircuitType.VACUUM_TUBE
                weighted("hbm:circuit_chip", 2, 6, 1)); // circuit, EnumCircuitType.CHIP
        pool(POOL_VAULT_REINFORCED,
                weighted("hbm:ingot_desh", 6, 16, 1),
                weighted("hbm:powder_power", 1, 5, 1),
                weighted("hbm:sat_chip", 1, 1, 1),
                weighted("minecraft:diamond", 5, 9, 1),
                weighted("hbm:ammo_standard_nuke_standard", 1, 3, 1), // ammo_standard, EnumAmmo.NUKE_STANDARD
                weighted("hbm:ammo_container", 1, 4, 1),
                weighted("hbm:grenade_universal", grenade(1, "nuke", "nuclear", "s7"), 1, 2, 1), // ItemGrenadeUniversal.make(EnumGrenadeShell.NUKE, EnumGrenadeFilling.NUCLEAR, EnumGrenadeFuze.S7)
                weighted("hbm:grenade_universal", grenade(4, "stick", "he", "impact", "triplex"), 1, 1, 1), // ItemGrenadeUniversal.make(EnumGrenadeShell.STICK, EnumGrenadeFilling.HE, EnumGrenadeFuze.IMPACT, EnumGrenadeExtra.TRIPLEX)
                weighted("hbm:powder_yellowcake", 26, 42, 1),
                weighted("hbm:gun_heavy_revolver", 1, 1, 1),
                weighted("hbm:circuit_chip", 18, 32, 1), // circuit, EnumCircuitType.CHIP
                weighted("hbm:circuit_basic", 6, 12, 1)); // circuit, EnumCircuitType.BASIC
        pool(POOL_VAULT_UNBREAKABLE,
                weighted("hbm:ammo_container", 3, 6, 1),
                weighted("hbm:ammo_standard_nuke_demo", 2, 3, 1), // ammo_standard, EnumAmmo.NUKE_DEMO
                weighted("hbm:gun_carbine", 1, 1, 1),
                weighted("hbm:ammo_standard_r762_du", 16, 32, 1), // ammo_standard, EnumAmmo.R762_DU
                weighted("hbm:gun_congolake", 1, 1, 1),
                weighted("hbm:circuit_advanced", 6, 12, 1)); // circuit, EnumCircuitType.ADVANCED
        pool(POOL_METEORITE_TREASURE,
                weighted("hbm:cobalt_pickaxe", 1, 1, 10),
                weighted("hbm:ingot_zirconium", 1, 16, 10),
                weighted("hbm:ingot_niobium", 1, 16, 10),
                weighted("hbm:ingot_cobalt", 1, 16, 10),
                weighted("hbm:ingot_boron", 1, 16, 10),
                weighted("hbm:ingot_starmetal", 1, 1, 5),
                weighted("hbm:crystal_gold", 1, 4, 10),
                weighted("hbm:circuit_vacuum_tube", 4, 8, 10), // circuit, EnumCircuitType.VACUUM_TUBE
                weighted("hbm:circuit_chip", 2, 4, 10), // circuit, EnumCircuitType.CHIP
                weighted("hbm:definitelyfood", 16, 32, 25),
                weighted("hbm:crate_can", 1, 3, 10),
                weighted("hbm:pill_herbal", 1, 2, 10),
                weighted("hbm:serum", 1, 1, 5),
                weighted("hbm:heart_piece", 1, 1, 5),
                weighted("hbm:scrumpy", 1, 1, 5),
                weighted("hbm:launch_code_piece", 1, 1, 5),
                weighted("hbm:egg_glyphid", 1, 1, 5),
                weighted("hbm:gem_alexandrite", 1, 1, 1),
                weighted("hbm:blueprint_folder_discover", 1, 1, 1)); // blueprint_folder, 1
        // 1.7 declares POOL_BLUEPRINTS twice with identical rows; the second replaces the first in the pool map
        pool(POOL_BLUEPRINTS,
                weighted("hbm:blueprint_folder", 1, 1, 10),
                weighted("hbm:blueprint_folder_discover", 1, 1, 5), // blueprint_folder, 1
                weighted("hbm:blueprint_folder", 1, 1, 1));

        // 1.7 ItemPoolsRedRoom
        pool(POOL_RED_PEDESTAL,
                weighted("hbm:ballistic_gauntlet", 1, 1, 10),
                weighted("hbm:armor_polish", 1, 1, 10),
                weighted("hbm:bandaid", 1, 1, 10),
                weighted("hbm:serum", 1, 1, 10),
                weighted("hbm:quartz_plutonium", 1, 1, 10),
                weighted("hbm:morning_glory", 1, 1, 10),
                weighted("hbm:spider_milk", 1, 1, 10),
                weighted("hbm:ink", 1, 1, 10),
                weighted("hbm:heart_container", 1, 1, 10),
                weighted("hbm:black_diamond", 1, 1, 10),
                weighted("hbm:scrumpy", 1, 1, 10),
                weighted("hbm:wild_p", 1, 1, 5),
                weighted("hbm:card_aos", 1, 1, 5),
                weighted("hbm:card_qos", 1, 1, 5),
                weighted("hbm:starmetal_sword", 1, 1, 5),
                weighted("hbm:gem_alexandrite", 1, 1, 5),
                weighted("hbm:crackpipe", 1, 1, 5),
                weighted("hbm:flask_infusion_shield", 1, 1, 5), // flask_infusion, 0
                weighted("hbm:boxcar", 1, 1, 5),
                weighted("hbm:book_of_", 1, 1, 5),
                weighted("hbm:gun_hangman", 1, 1, 1),
                weighted("hbm:gun_mas36", 1, 1, 1),
                weighted("hbm:item_secret_folly", 1, 1, 1), // item_secret, EnumSecretType.FOLLY
                weighted("hbm:weapon_mod_special_nickel", 1, 1, 1), // weapon_mod_special, EnumModSpecial.NICKEL
                weighted("hbm:weapon_mod_special_doubloons", 1, 1, 1)); // weapon_mod_special, EnumModSpecial.DOUBLOONS
        pool(POOL_BLACK_SLAB,
                weighted("hbm:clay_tablet", 1, 1, 10));
        pool(POOL_BLACK_PART,
                weighted("hbm:item_secret_selenium_steel", 4, 4, 10), // item_secret, EnumSecretType.SELENIUM_STEEL
                weighted("hbm:item_secret_controller", 1, 1, 10), // item_secret, EnumSecretType.CONTROLLER
                weighted("hbm:item_secret_canister", 1, 1, 10), // item_secret, EnumSecretType.CANISTER
                weighted("hbm:blueprint_folder_secret", 1, 1, 1)); // blueprint_folder, 2

        // 1.7 ItemPoolsSatellite
        pool(POOL_SAT_MINER,
                weighted("hbm:powder_aluminium", 3, 3, 10),
                weighted("hbm:powder_iron", 3, 3, 10),
                weighted("hbm:powder_titanium", 2, 2, 8),
                weighted("hbm:crystal_tungsten", 2, 2, 7),
                weighted("hbm:powder_coal", 4, 4, 15),
                weighted("hbm:powder_uranium", 2, 2, 5),
                weighted("hbm:powder_plutonium", 1, 1, 5),
                weighted("hbm:powder_thorium", 2, 2, 7),
                weighted("hbm:powder_desh_mix", 3, 3, 5),
                weighted("hbm:powder_diamond", 2, 2, 7),
                weighted("minecraft:redstone", 5, 5, 15),
                weighted("hbm:powder_nitan_mix", 2, 2, 5),
                weighted("hbm:powder_power", 2, 2, 5),
                weighted("hbm:powder_copper", 5, 5, 15),
                weighted("hbm:powder_lead", 3, 3, 10),
                weighted("hbm:fluorite", 4, 4, 15),
                weighted("hbm:powder_lapis", 4, 4, 10),
                weighted("hbm:crystal_aluminium", 1, 1, 5),
                weighted("hbm:crystal_gold", 1, 1, 5),
                weighted("hbm:crystal_phosphorus", 1, 1, 10),
                weighted("hbm:gravel_diamond", 1, 1, 3),
                weighted("hbm:crystal_uranium", 1, 1, 3),
                weighted("hbm:crystal_plutonium", 1, 1, 3),
                weighted("hbm:crystal_trixite", 1, 1, 1),
                weighted("hbm:crystal_starmetal", 1, 1, 1),
                weighted("hbm:crystal_lithium", 2, 2, 4));
        pool(POOL_SAT_LUNAR,
                weighted("hbm:moon_turf", 48, 48, 5),
                weighted("hbm:moon_turf", 32, 32, 7),
                weighted("hbm:moon_turf", 16, 16, 5),
                weighted("hbm:powder_lithium", 3, 3, 5),
                weighted("hbm:powder_iron", 3, 3, 5),
                weighted("hbm:crystal_iron", 1, 1, 1),
                weighted("hbm:crystal_lithium", 1, 1, 1));

        // 1.7 ItemPoolsPile
        pool(POOL_PILE_HIVE,
                weighted("minecraft:iron_ingot", 1, 3, 10),
                weighted("hbm:ingot_steel", 1, 2, 10),
                weighted("hbm:ingot_aluminium", 1, 2, 10),
                weighted("hbm:scrap", 3, 6, 10),
                weighted("hbm:gas_mask_m65", 1, 1, 10),
                weighted("hbm:steel_plate", 1, 1, 5),
                weighted("hbm:steel_legs", 1, 1, 5),
                weighted("hbm:steel_pickaxe", 1, 1, 5),
                weighted("hbm:steel_shovel", 1, 1, 5),
                weighted("hbm:gun_maresleg", 1, 1, 5),
                weighted("hbm:gun_light_revolver", 1, 1, 1),
                weighted("hbm:grenade_universal", grenade(4, "frag", "he", "s3", "frag_sleeve"), 1, 2, 5), // ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.HE, EnumGrenadeFuze.S3, EnumGrenadeExtra.FRAG_SLEEVE)
                weighted("hbm:grenade_universal", grenade(4, "stick", "demo", "impact"), 1, 2, 3), // ItemGrenadeUniversal.make(EnumGrenadeShell.STICK, EnumGrenadeFilling.DEMO, EnumGrenadeFuze.IMPACT)
                weighted("hbm:ammo_standard_g12", 4, 4, 10), // ammo_standard, EnumAmmo.G12
                weighted("hbm:ammo_standard_m357_sp", 6, 12, 10), // ammo_standard, EnumAmmo.M357_SP
                weighted("hbm:ammo_standard_g40_he", 1, 1, 2), // ammo_standard, EnumAmmo.G40_HE
                weighted("hbm:bottle_nuka", 1, 2, 20),
                weighted("hbm:bottle_quantum", 1, 2, 1),
                weighted("hbm:definitelyfood", 5, 12, 20),
                weighted("hbm:egg_glyphid", 1, 3, 30),
                weighted("hbm:syringe_metal_stimpak", 1, 1, 5),
                weighted("hbm:iv_blood", 1, 1, 10),
                weighted("minecraft:experience_bottle", 1, 3, 5));
        pool(POOL_PILE_BONES,
                weighted("minecraft:bone", 1, 1, 10),
                weighted("minecraft:rotten_flesh", 1, 1, 5),
                weighted("hbm:biomass", 1, 1, 2));
        pool(POOL_PILE_CAPS,
                weighted("hbm:cap_nuka", 4, 4, 20),
                weighted("hbm:cap_quantum", 4, 4, 3),
                weighted("hbm:cap_sparkle", 4, 4, 1));
        pool(POOL_PILE_MED_SYRINGE,
                weighted("hbm:syringe_metal_stimpak", 1, 1, 10),
                weighted("hbm:syringe_metal_medx", 1, 1, 5),
                weighted("hbm:syringe_metal_psycho", 1, 1, 5));
        pool(POOL_PILE_MED_PILLS,
                weighted("hbm:radaway", 1, 1, 10),
                weighted("hbm:radx", 1, 1, 10),
                weighted("hbm:iv_blood", 1, 1, 15),
                weighted("hbm:siox", 1, 1, 5));
        pool(POOL_PILE_MAKESHIFT_GUN,
                weighted("hbm:gun_maresleg", 1, 1, 10));
        pool(POOL_PILE_MAKESHIFT_WRENCH,
                weighted("hbm:wrench", 1, 1, 10));
        pool(POOL_PILE_MAKESHIFT_PLATES,
                weighted("hbm:plate_steel", 1, 1, 10));
        pool(POOL_PILE_MAKESHIFT_WIRE,
                weighted("hbm:wire_fine_aluminium", 1, 1, 10)); // wire_fine, Mats.MAT_ALUMINIUM.id
        pool(POOL_PILE_NUKE_STORAGE,
                weighted("hbm:ammo_standard_nuke_standard", 1, 1, 50), // ammo_standard, EnumAmmo.NUKE_STANDARD
                weighted("hbm:ammo_standard_nuke_high", 1, 1, 10), // ammo_standard, EnumAmmo.NUKE_HIGH
                weighted("hbm:ammo_standard_nuke_tots", 1, 1, 10)); // ammo_standard, EnumAmmo.NUKE_TOTS
        pool(POOL_PILE_OF_GARBAGE,
                weighted("hbm:pipe_iron", 0, 2, 20), // pipe, 2600
                weighted("hbm:scrap", 1, 5, 20),
                weighted("hbm:dust", 1, 3, 40),
                weighted("hbm:dust_tiny", 1, 7, 40),
                weighted("hbm:powder_cement", 1, 6, 40),
                weighted("hbm:nugget_lead", 0, 3, 20),
                weighted("hbm:wire_fine_lead", 1, 2, 20), // wire_fine, Mats.MAT_LEAD.id
                weighted("hbm:powder_ash_wood", 0, 1, 15), // powder_ash, 0
                weighted("hbm:plate_lead", 0, 1, 15),
                weighted("minecraft:string", 0, 1, 15),
                weighted("hbm:bolt_lead", 0, 2, 15), // bolt, 8200
                weighted("hbm:pin", 0, 2, 15),
                weighted("hbm:cap_nuka", 0, 8, 15),
                weighted("hbm:plate_iron", 0, 2, 15),
                weighted("hbm:falloutitem", 0, 2, 15), // fallout, 0
                weighted("hbm:coil_tungsten", 0, 2, 15),
                weighted("hbm:can_empty", 0, 1, 15),
                weighted("hbm:ingot_asbestos", 0, 1, 15),
                weighted("hbm:syringe_metal_empty", 0, 1, 15),
                weighted("hbm:syringe_empty", 0, 1, 15),
                weighted("hbm:weapon_pipe_lead", 0, 1, 5), // pipe_lead, 0
                weighted("hbm:motor", 0, 1, 5),
                weighted("hbm:canned_conserve_mystery", 0, 1, 5)); // canned_conserve, 2
        pool(POOL_PILE_MECHANICAL,
                weighted("hbm:defuser", 1, 1, 30),
                weighted("hbm:screwdriver", 1, 1, 30),
                weighted("hbm:wire_fine_copper", 8, 12, 120), // wire_fine, Mats.MAT_COPPER.id
                weighted("hbm:plate_steel", 3, 8, 40),
                weighted("hbm:plate_copper", 2, 5, 40),
                weighted("hbm:coil_copper", 2, 5, 40),
                weighted("hbm:coil_tungsten", 2, 5, 40));
        pool(POOL_PILE_GEAR,
                weighted("hbm:defuser", 1, 1, 40),
                weighted("hbm:screwdriver", 1, 1, 30),
                weighted("hbm:canteen_vodka", 1, 1, 40),
                weighted("hbm:casing_small_steel", 1, 4, 30), // casing, ItemEnums.EnumCasingType.SMALL_STEEL
                weighted("hbm:casing_small", 3, 8, 40), // casing, ItemEnums.EnumCasingType.SMALL
                weighted("hbm:casing_buckshot", 3, 8, 40), // casing, ItemEnums.EnumCasingType.BUCKSHOT
                weighted("hbm:canned_conserve_beef", 2, 5, 40), // canned_conserve, 0
                weighted("hbm:taurun_helmet", 1, 1, 20),
                weighted("hbm:taurun_plate", 1, 1, 20),
                weighted("hbm:taurun_legs", 1, 1, 20),
                weighted("hbm:taurun_boots", 1, 1, 20));
        pool(POOL_PILE_SUPPLIES,
                weighted("hbm:grenade_universal", grenade(4, "frag", "he", "s3", "frag_sleeve"), 3, 5, 10), // ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.HE, EnumGrenadeFuze.S3, EnumGrenadeExtra.FRAG_SLEEVE)
                weighted("hbm:grenade_universal", grenade(4, "frag", "he", "s3", "frag_sleeve"), 3, 5, 10), // ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.HE, EnumGrenadeFuze.S3, EnumGrenadeExtra.FRAG_SLEEVE)
                weighted("hbm:grenade_universal", grenade(4, "frag", "he", "s3", "frag_sleeve"), 3, 5, 10), // ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.HE, EnumGrenadeFuze.S3, EnumGrenadeExtra.FRAG_SLEEVE)
                weighted("hbm:syringe_metal_stimpak", 3, 5, 30),
                weighted("hbm:syringe_metal_psycho", 3, 5, 30),
                weighted("hbm:syringe_antidote", 1, 2, 30),
                weighted("hbm:ammo_container", 2, 3, 40));

        // 1.7 ItemPoolsC130
        pool(POOL_SUPPLIES,
                weighted("hbm:definitelyfood", 3, 10, 25),
                weighted("hbm:syringe_metal_stimpak", 1, 3, 10),
                weighted("hbm:pill_iodine", 1, 2, 2),
                filled("hbm:canister_full", "DIESEL", 1, 4, 5), // canister_full, Fluids.DIESEL.getID()
                weighted("hbm:machine_diesel", 1, 1, 1),
                weighted("hbm:geiger_counter", 1, 1, 2),
                weighted("hbm:med_bag", 1, 1, 3),
                weighted("hbm:radaway", 1, 5, 10));
        pool(POOL_WEAPONS,
                weighted("hbm:gun_light_revolver", 1, 1, 100),
                weighted("hbm:gun_henry", 1, 1, 100),
                weighted("hbm:gun_maresleg", 1, 1, 100),
                weighted("hbm:gun_greasegun", 1, 1, 100),
                weighted("hbm:gun_carbine", 1, 1, 50),
                weighted("hbm:gun_heavy_revolver", 1, 1, 50),
                weighted("hbm:gun_panzerschreck", 1, 1, 20),
                weighted("hbm:gun_double_barrel", 1, 1, 10),
                weighted("hbm:gun_n_i_4_n_i", 1, 1, 1));
        pool(POOL_AMMO,
                weighted("hbm:ammo_standard_m357_sp", 12, 12, 10), // ammo_standard, EnumAmmo.M357_SP
                weighted("hbm:ammo_standard_m357_fmj", 6, 6, 10), // ammo_standard, EnumAmmo.M357_FMJ
                weighted("hbm:ammo_standard_m44_sp", 12, 12, 5), // ammo_standard, EnumAmmo.M44_SP
                weighted("hbm:ammo_standard_m44_fmj", 6, 6, 5), // ammo_standard, EnumAmmo.M44_FMJ
                weighted("hbm:ammo_standard_p9_sp", 12, 12, 10), // ammo_standard, EnumAmmo.P9_SP
                weighted("hbm:ammo_standard_p9_fmj", 6, 6, 10), // ammo_standard, EnumAmmo.P9_FMJ
                weighted("hbm:ammo_standard_r762_sp", 6, 6, 5), // ammo_standard, EnumAmmo.R762_SP
                weighted("hbm:ammo_standard_g12_bp", 6, 6, 10), // ammo_standard, EnumAmmo.G12_BP
                weighted("hbm:ammo_standard_rocket_he", 1, 1, 3), // ammo_standard, EnumAmmo.ROCKET_HE
                weighted("hbm:ammo_container", 1, 1, 1));

        // 1.7 ItemPoolsVendingMachine
        pool(POOL_SODA,
                weighted("hbm:bottle_nuka", 1, 1, 10),
                weighted("hbm:bottle_cherry", 1, 1, 5),
                weighted("hbm:bottle_quantum", 1, 1, 1),
                weighted("hbm:can_bepis", 1, 1, 10),
                weighted("hbm:can_luna", 1, 1, 10),
                weighted("hbm:can_mug", 1, 1, 10),
                weighted("hbm:can_breen", 1, 1, 1));
        pool(POOL_SNACKS,
                weighted("hbm:definitelyfood", 1, 1, 10),
                weighted("hbm:canned_conserve_beef", 1, 1, 5), // canned_conserve, EnumFoodType.BEEF
                weighted("hbm:canned_conserve_tube", 1, 1, 5), // canned_conserve, EnumFoodType.TUBE
                weighted("hbm:twinkie", 1, 1, 10),
                weighted("hbm:chocolate", 1, 1, 10));

        // 1.7 SiloComponent.launchKey, a bare WeightedRandomChestContent[] rather than a pool
        pool(POOL_LAUNCH_KEY,
                weighted("hbm:launch_key", 1, 1, 1));
    }

    /** 1.7 {@code new ItemPool(name) {{ this.pool = ... }}}: the file is the pool name lower-cased. */
    private void pool(String name, Row... rows) {
        register(name.toLowerCase(Locale.ROOT), new PoolFile(false, List.of(rows)));
    }

    /** 1.7 {@code weighted(item, 0, min, max, weight)}. */
    private static Row weighted(String item, int min, int max, int weight) {
        return new Row(item, min, max, weight, null, null);
    }

    /** 1.7 {@code weighted(stack, min, max, weight)} for a stack built with state. */
    private static Row weighted(String item, JsonObject components, int min, int max, int weight) {
        return new Row(item, min, max, weight, null, components);
    }

    /** 1.7 {@code weighted(container, fluid.getID(), min, max, weight)} and the per-fluid cells. */
    private static Row filled(String item, String fluid, int min, int max, int weight) {
        return new Row(item, min, max, weight, fluid, null);
    }

    /** 1.7 metadata as item damage ({@code gas_mask_m65} meta 60). */
    private static JsonObject damage(int damage) {
        JsonObject components = new JsonObject();
        components.addProperty("minecraft:damage", damage);
        return components;
    }

    /** 1.7 {@code ItemBlowtorch.getEmptyTool}: a tool holding no fuel. */
    private static JsonObject fluidAmount(int amount) {
        JsonObject components = new JsonObject();
        components.addProperty("hbm:fluid_amount", amount);
        return components;
    }

    /**
     * 1.7 {@code ItemGrenadeUniversal.make(shell, filling, fuze[, extra])}; the port also states the stack size the
     * shell gives the stack.
     */
    private static JsonObject grenade(int maxStackSize, String shell, String filling, String fuze, String... extra) {
        JsonObject grenade = new JsonObject();
        grenade.addProperty("shell", shell);
        grenade.addProperty("filling", filling);
        grenade.addProperty("fuze", fuze);
        if (extra.length > 0) grenade.addProperty("extra", extra[0]);
        JsonObject components = new JsonObject();
        components.add("hbm:grenade", grenade);
        components.addProperty("minecraft:max_stack_size", maxStackSize);
        return components;
    }
}
