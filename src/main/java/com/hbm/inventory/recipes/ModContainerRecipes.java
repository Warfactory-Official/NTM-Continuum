package com.hbm.inventory.recipes;

import com.hbm.lib.ComponentIds;
import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;

import static dev.golem.core.dejson.Crafting.anyOf;
import static dev.golem.core.dejson.Crafting.componentShaped;
import static dev.golem.core.dejson.Crafting.componentShapedCopy;
import static dev.golem.core.dejson.Crafting.componentShapeless;
import static dev.golem.core.dejson.Crafting.fluid;
import static dev.golem.core.dejson.Crafting.item;
import static dev.golem.core.dejson.Crafting.key;
import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.stack;
import static dev.golem.core.dejson.Crafting.tag;

/**
 * The 1.7 crafting-table recipes whose ingredients a plain vanilla ingredient cannot say: a filled fluid
 * container of any kind ({@code Fluids.X.getDict(amount)}, leaving the empty container), a metadata/NBT variant
 * (flattened into components), the water bottle ({@code Items.potionitem} meta 0, consumed whole since 1.7's
 * potion had no container item), a crafting tool that stays in the grid one use more worn
 * ({@code KEY_TOOL_SCREWDRIVER}, {@code KEY_TOOL_CHEMISTRYSET}), and the {@code ContainerUpgradeCraftingHandler}
 * crate upgrades that carry the old crate's contents into the new one.
 *
 * <p>Sources: {@code CraftingManager}, {@code ConsumableRecipes}, {@code PowderRecipes}, {@code WeaponRecipes}.
 * Not declared, with the reason: the patrol-drone express upgrades (no drone item in the port) and
 * {@code enable528}/config-gated recipes. The minecarts are in {@link RestoredCraftingRecipes} (History §19ej-1b).
 * Touches no Minecraft class.
 */
public final class ModContainerRecipes {

    private static final String NS = "hbm";
    private static final String VARIANT = ComponentIds.item_variant;
    private static boolean declared;

    // not yet in the generated id tables (registered this wave)
    private static final String HOLOTAPE_IMAGE = "hbm:holotape_image";
    private static final String WRENCH = "hbm:wrench";
    private static final String SCREWDRIVER_DESH = "hbm:screwdriver_desh";
    private static final String CHEMISTRY_SET_BORON = "hbm:chemistry_set_boron";
    private static final String UPGRADE_MUFFLER = "hbm:upgrade_muffler";
    private static final String STARBLASTER_CELL = "hbm:weaponized_starblaster_cell";
    private static final String BOXCAR_WARHEAD = "hbm:mp_warhead_15_boxcar";
    private static final String MASS_STORAGE_DESH = "hbm:mass_storage_desh";
    private static final String MASS_STORAGE = "hbm:mass_storage";

    /** 1.7 Fluids ids of the component-matched containers below (fluid_tank_full meta = fluid id). */
    private static final String FLUID_LUBRICANT = "hbm:lubricant";
    private static final String FLUID_PEROXIDE = "hbm:peroxide";

    private ModContainerRecipes() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        consumables();
        tools();
        powders();
        weapons();
        crates();
    }

    /** KEY_TOOL_SCREWDRIVER: both screwdrivers, which stay in the grid one use more worn. */
    private static dev.golem.core.dejson.Crafting.Ingredient screwdriver() {
        return anyOf(ItemIds.screwdriver, SCREWDRIVER_DESH).degrade();
    }

    /** KEY_TOOL_CHEMISTRYSET: both laboratory sets, which stay in the grid one use more worn. */
    private static dev.golem.core.dejson.Crafting.Ingredient chemistrySet() {
        return anyOf("hbm:chemistry_set", CHEMISTRY_SET_BORON).degrade();
    }

    /** Items.potionitem meta 0 - the water bottle - consumed whole. */
    private static dev.golem.core.dejson.Crafting.Ingredient waterBottle() {
        return item("minecraft:potion").potion("minecraft:water").remainder("minecraft:air");
    }

    private static void consumables() {
        // ConsumableRecipes: KEY_ANYPANE, dye meta 3 (cocoa), milk bucket, 1,000 mB nitroglycerin
        componentShapeless(NS, "chocolate_milk", stack("hbm:chocolate_milk"),
                tag(TagIds.glass_panes), item("minecraft:cocoa_beans"), item("minecraft:milk_bucket"),
                fluid("NITROGLYCERIN", 1_000));
        componentShapeless(NS, "mucho_mango", stack("hbm:mucho_mango"),
                waterBottle(), item("minecraft:sugar"), item("minecraft:sugar"), tag(TagIds.dyes_orange));
        componentShapeless(NS, "can_creature", stack(ItemIds.can_creature),
                item(ItemIds.can_empty), waterBottle(), item("minecraft:sugar"), fluid("DIESEL", 1_000));
        // CraftingManager
        componentShapeless(NS, "bdcl", stack("hbm:bdcl"),
                tag(TagIds.tars), fluid("WATER", 1_000), tag(TagIds.dyes_white));
        componentShapeless(NS, "gun_kit_1", stack("hbm:gun_kit_1"),
                tag(TagIds.ingots_any_rubber), fluid("WOODOIL", 1_000), tag(TagIds.ingots_iron));
        componentShapeless(NS, "gun_kit_2", stack("hbm:gun_kit_2"),
                item("hbm:gun_kit_1"), item(WRENCH), item(ItemIds.ducttape), fluid("LUBRICANT", 1_000));
        componentShapeless(NS, "solid_fuel_from_heatingoil", stack(ItemIds.solid_fuel, 3),
                fluid("HEATINGOIL", 16_000), chemistrySet());
        componentShapeless(NS, "canister_full_lubricant", stack(ItemIds.canister_full, 2)
                        .with(ComponentIds.fluid, FLUID_LUBRICANT).with(ComponentIds.fluid_amount, 1_000),
                fluid("HEATINGOIL", 1_000), fluid("UNSATURATEDS", 1_000), item(ItemIds.canister_empty),
                item(ItemIds.canister_empty), chemistrySet());
        // 1.7 infinite water barrels (gated on !enable528, which defaults to off)
        componentShaped(NS, "inf_water", stack(ItemIds.inf_water), rows("222", "131", "222"),
                key("1", fluid("WATER", 1_000)), key("2", tag(TagIds.plates_aluminium)),
                key("3", tag(TagIds.gems_diamond)));
        shaped(NS, "inf_water_mk2", ItemIds.inf_water_mk2, rows("BPB", "PTP", "BPB"),
                "B", ItemIds.inf_water, "P", TagIds.pipes_steel, "T", TagIds.shells_steel);
    }

    private static void tools() {
        // HOLO_DIGAMMA is variant 0, which a stack may carry explicitly or leave absent (the enum's default)
        componentShapeless(NS, "holotape_image_restored", stack(HOLOTAPE_IMAGE).with(VARIANT, 1),
                item(HOLOTAPE_IMAGE).with(VARIANT, 0), screwdriver(), item(ItemIds.ducttape),
                item(ItemIds.armor_polish));
        componentShapeless(NS, "holotape_image_restored_1", stack(HOLOTAPE_IMAGE).with(VARIANT, 1),
                item(HOLOTAPE_IMAGE).without(VARIANT), screwdriver(), item(ItemIds.ducttape),
                item(ItemIds.armor_polish));
        // the gem_alexandrite is 1.7's own "placeholder for amplifier"
        componentShapeless(NS, "holotape_damaged", stack(ItemIds.holotape_damaged),
                item(HOLOTAPE_IMAGE).with(VARIANT, 1), item(UPGRADE_MUFFLER), item(ItemIds.crt_display),
                item(ItemIds.gem_alexandrite));
    }

    private static void powders() {
        // PowderRecipes
        componentShapeless(NS, "ball_dynamite", stack(ItemIds.ball_dynamite, 2),
                tag(TagIds.dusts_niter), item("minecraft:sugar"), item("minecraft:sand"), chemistrySet());
        componentShapeless(NS, "ball_tnt", stack(ItemIds.ball_tnt, 4),
                fluid("AROMATICS", 1_000), tag(TagIds.dusts_niter), chemistrySet());
        componentShapeless(NS, "ingot_c4", stack(ItemIds.ingot_c4, 4),
                fluid("UNSATURATEDS", 1_000), tag(TagIds.dusts_niter), chemistrySet());
        componentShapeless(NS, "clay_ball_from_dust", stack("minecraft:clay_ball", 4),
                tag(TagIds.minecraft_sand), item(ItemIds.dust), item(ItemIds.dust), fluid("WATER", 1_000));
        componentShapeless(NS, "powder_bakelite", stack(ItemIds.powder_bakelite, 2),
                fluid("AROMATICS", 1_000), fluid("PETROLEUM", 1_000), chemistrySet());
        // CraftingManager: four bone meal (dye meta 15) and a sulfuric acid container
        componentShapeless(NS, "slime_ball_from_acid", stack("minecraft:slime_ball", 16),
                item("minecraft:bone_meal"), item("minecraft:bone_meal"), item("minecraft:bone_meal"),
                item("minecraft:bone_meal"), fluid("SULFURIC_ACID", 1_000));
    }

    private static void weapons() {
        // WeaponRecipes: fluid_tank_full meta PEROXIDE (the tank itself, not any container), the full B92
        // cell (meta 1; 1.7 matched item and meta only, so the charge NBT is not checked) and fine copper wire
        componentShapeless(NS, "weaponized_starblaster_cell", stack(STARBLASTER_CELL),
                item(ItemIds.fluid_tank).with(ComponentIds.fluid, FLUID_PEROXIDE)
                        .with(ComponentIds.fluid_amount, 1_000).remainder(ItemIds.fluid_tank),
                item(ItemIds.gun_b92_ammo).with(VARIANT, 1), tag(TagIds.wires_copper));
        componentShapeless(NS, "missile_taint", stack(ItemIds.missile_taint),
                item(ItemIds.missile_assembly), fluid("WATZ", 1_000), item(ItemIds.powder_spark_mix),
                item(ItemIds.powder_magic));
        componentShaped(NS, "mp_warhead_15_boxcar", stack(BOXCAR_WARHEAD), rows("SNS", "CBC", "SFS"),
                key("S", tag(TagIds.ingots_starmetal)), key("N", item(ItemIds.det_nuke)),
                key("C", item(ItemIds.circuit_advanced)), key("B", item(ItemIds.boxcar)),
                key("F", fluid("TRITIUM", 16_000)));
    }

    /** CraftingManager's ContainerUpgradeCraftingHandler recipes: the upgraded crate keeps the old one's NBT. */
    private static void crates() {
        componentShapedCopy(NS, "crate_desh", stack(ItemIds.crate_desh), "S", rows(" D ", "DSD", " D "),
                key("D", item(ItemIds.plate_desh)), key("S", item(ItemIds.crate_steel)));
        // 1.7 copies all 54 steel-crate slots into the 27-slot tungsten crate / 15-slot safe; slots past the
        // new size are simply never read back when the block entity loads.
        componentShapedCopy(NS, "crate_tungsten", stack("hbm:crate_tungsten"), "C", rows("BPB", "PCP", "BPB"),
                key("B", tag(TagIds.storage_blocks_tungsten)), key("P", tag(TagIds.plates_cast_copper)),
                key("C", item(ItemIds.crate_steel)));
        componentShapedCopy(NS, "safe", stack("hbm:safe"), "C", rows("LAL", "ACA", "LAL"),
                key("L", tag(TagIds.plates_lead)), key("A", tag(TagIds.plates_titanium)),
                key("C", item(ItemIds.crate_steel)));
        componentShapedCopy(NS, "mass_storage_desh", stack(MASS_STORAGE_DESH), "M", rows(" C ", "PMP", " P "),
                key("P", tag(TagIds.ingots_desh)), key("C", item(ItemIds.circuit_chip)),
                key("M", item(ItemIds.mass_storage_iron)));
        componentShapedCopy(NS, "mass_storage", stack(MASS_STORAGE), "M", rows(" C ", "PMP", " P "),
                key("P", tag(TagIds.ingots_any_resistant_alloy)), key("C", item(ItemIds.circuit_advanced)),
                key("M", item(MASS_STORAGE_DESH)));
    }
}
