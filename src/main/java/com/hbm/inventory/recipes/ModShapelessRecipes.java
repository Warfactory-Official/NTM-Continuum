package com.hbm.inventory.recipes;

import com.hbm.api.ids.ComponentIds;
import com.hbm.api.ids.ItemIds;
import com.hbm.api.ids.TagIds;
import com.hbm.api.ids.VanillaIds;

import static dev.golem.core.dejson.Crafting.shapeless;
import static dev.golem.core.dejson.Crafting.stack;

/**
 * Every shapeless hbm recipe, declared through Golem Core's {@code dejson} vocabulary.
 *
 * <p>Ingredients in the order the file listed them, and a result: that is the whole of each of the
 * {@value #FILES_REPLACED} files these stand in for. See {@link ModShapedRecipes} for why the
 * recipe ids are carried through rather than derived.
 */
public final class ModShapelessRecipes {

    private static final String NS = "hbm";

    /** How many recipe files these declarations stand in for; quoted in the javadoc above. */
    static final int FILES_REPLACED = 473;

    private static boolean declared;

    private ModShapelessRecipes() {}

    public static void declare() {
        if (declared) {
            return;
        }
        declared = true;

        shapeless0();
        shapeless1();
        shapeless2();
        shapeless3();
        shapeless4();
    }

    private static void shapeless0() {
        shapeless(NS, "ams_catalyst_aluminium", ItemIds.ams_catalyst_aluminium,
                ItemIds.ams_catalyst_blank, ItemIds.rune_isa, ItemIds.rune_isa, ItemIds.rune_jera, ItemIds.rune_thurisaz,
                TagIds.dusts_aluminium, TagIds.dusts_aluminium, TagIds.dusts_aluminium, TagIds.dusts_aluminium);
        shapeless(NS, "ams_catalyst_beryllium", ItemIds.ams_catalyst_beryllium,
                ItemIds.ams_catalyst_blank, ItemIds.rune_isa, ItemIds.rune_dagaz, ItemIds.rune_jera, ItemIds.rune_jera,
                TagIds.dusts_beryllium, TagIds.dusts_beryllium, TagIds.dusts_beryllium, TagIds.dusts_beryllium);
        shapeless(NS, "ams_catalyst_cobalt", ItemIds.ams_catalyst_cobalt,
                ItemIds.ams_catalyst_blank, ItemIds.rune_dagaz, ItemIds.rune_hagalaz, ItemIds.rune_jera,
                ItemIds.rune_jera, TagIds.dusts_cobalt, TagIds.dusts_cobalt, TagIds.dusts_cobalt, TagIds.dusts_cobalt);
        shapeless(NS, "ams_catalyst_copper", ItemIds.ams_catalyst_copper,
                ItemIds.ams_catalyst_blank, ItemIds.rune_dagaz, ItemIds.rune_dagaz, ItemIds.rune_jera, ItemIds.rune_jera,
                TagIds.dusts_copper, TagIds.dusts_copper, TagIds.dusts_copper, TagIds.dusts_copper);
        shapeless(NS, "ams_catalyst_dineutronium", ItemIds.ams_catalyst_dineutronium,
                ItemIds.ams_catalyst_blank, ItemIds.rune_hagalaz, ItemIds.rune_hagalaz, ItemIds.rune_thurisaz,
                ItemIds.rune_thurisaz, TagIds.dusts_dineutronium, TagIds.dusts_dineutronium, TagIds.dusts_dineutronium,
                TagIds.dusts_dineutronium);
        shapeless(NS, "ams_catalyst_euphemium", ItemIds.ams_catalyst_euphemium,
                ItemIds.ams_catalyst_blank, ItemIds.rune_dagaz, ItemIds.rune_dagaz, ItemIds.rune_thurisaz,
                ItemIds.rune_thurisaz, TagIds.dusts_euphemium, TagIds.dusts_euphemium, TagIds.dusts_euphemium,
                TagIds.dusts_euphemium);
        shapeless(NS, "ams_catalyst_iron", ItemIds.ams_catalyst_iron,
                ItemIds.ams_catalyst_blank, ItemIds.rune_isa, ItemIds.rune_dagaz, ItemIds.rune_jera,
                ItemIds.rune_thurisaz, TagIds.dusts_iron, TagIds.dusts_iron, TagIds.dusts_iron, TagIds.dusts_iron);
        shapeless(NS, "ams_catalyst_lithium", ItemIds.ams_catalyst_lithium,
                ItemIds.ams_catalyst_blank, ItemIds.rune_isa, ItemIds.rune_isa, ItemIds.rune_jera, ItemIds.rune_jera,
                TagIds.dusts_lithium, TagIds.dusts_lithium, TagIds.dusts_lithium, TagIds.dusts_lithium);
        shapeless(NS, "ams_catalyst_niobium", ItemIds.ams_catalyst_niobium,
                ItemIds.ams_catalyst_blank, ItemIds.rune_dagaz, ItemIds.rune_hagalaz, ItemIds.rune_jera,
                ItemIds.rune_thurisaz, TagIds.dusts_niobium, TagIds.dusts_niobium, TagIds.dusts_niobium,
                TagIds.dusts_niobium);
        shapeless(NS, "ams_catalyst_schrabidium", ItemIds.ams_catalyst_schrabidium,
                ItemIds.ams_catalyst_blank, ItemIds.rune_dagaz, ItemIds.rune_hagalaz, ItemIds.rune_thurisaz,
                ItemIds.rune_thurisaz, TagIds.dusts_schrabidium, TagIds.dusts_schrabidium, TagIds.dusts_schrabidium,
                TagIds.dusts_schrabidium);
        shapeless(NS, "ams_catalyst_strontium", ItemIds.ams_catalyst_strontium,
                ItemIds.ams_catalyst_blank, ItemIds.rune_dagaz, ItemIds.rune_dagaz, ItemIds.rune_jera,
                ItemIds.rune_thurisaz, TagIds.dusts_strontium, TagIds.dusts_strontium, TagIds.dusts_strontium,
                TagIds.dusts_strontium);
        shapeless(NS, "ams_catalyst_thorium", ItemIds.ams_catalyst_thorium,
                ItemIds.ams_catalyst_blank, ItemIds.rune_isa, ItemIds.rune_dagaz, ItemIds.rune_thurisaz,
                ItemIds.rune_thurisaz, TagIds.dusts_thorium, TagIds.dusts_thorium, TagIds.dusts_thorium,
                TagIds.dusts_thorium);
        shapeless(NS, "ams_catalyst_tungsten", ItemIds.ams_catalyst_tungsten,
                ItemIds.ams_catalyst_blank, ItemIds.rune_hagalaz, ItemIds.rune_hagalaz, ItemIds.rune_jera,
                ItemIds.rune_jera, TagIds.dusts_tungsten, TagIds.dusts_tungsten, TagIds.dusts_tungsten,
                TagIds.dusts_tungsten);
        shapeless(NS, "asphalt", ItemIds.asphalt, ItemIds.asphalt_light);
        shapeless(NS, "asphalt_light", ItemIds.asphalt_light, ItemIds.asphalt, VanillaIds.glowstone_dust);
        shapeless(NS, "ball_fireclay", ItemIds.ball_fireclay, 4,
                VanillaIds.clay_ball, VanillaIds.clay_ball, VanillaIds.clay_ball, TagIds.dusts_aluminium);
        shapeless(NS, "ball_fireclay_1", ItemIds.ball_fireclay, 4,
                VanillaIds.clay_ball, VanillaIds.clay_ball, VanillaIds.clay_ball, TagIds.ores_aluminium);
        shapeless(NS, "ballistite", ItemIds.ballistite, 3, VanillaIds.gunpowder, TagIds.dusts_niter, VanillaIds.sugar);
        shapeless(NS, "billet_am_mix_2", ItemIds.billet_am_mix, 3,
                ItemIds.billet_am241, ItemIds.billet_am242, ItemIds.billet_am242);
        shapeless(NS, "billet_am_mix_3", ItemIds.billet_am_mix,
                TagIds.nuggets_am241, TagIds.nuggets_am241, TagIds.nuggets_am242, TagIds.nuggets_am242,
                TagIds.nuggets_am242, TagIds.nuggets_am242);
        shapeless(NS, "billet_americium_fuel", ItemIds.billet_americium_fuel, 3,
                ItemIds.billet_u238, ItemIds.billet_u238, ItemIds.billet_am_mix);
        shapeless(NS, "billet_americium_fuel_1", ItemIds.billet_americium_fuel,
                ItemIds.nugget_am_mix, ItemIds.nugget_am_mix, TagIds.nuggets_u238, TagIds.nuggets_u238,
                TagIds.nuggets_u238, TagIds.nuggets_u238);
        shapeless(NS, "billet_mox_fuel_2", ItemIds.billet_mox_fuel, 3,
                ItemIds.billet_uranium_fuel, ItemIds.billet_uranium_fuel, TagIds.billets_pu239);
        shapeless(NS, "billet_mox_fuel_3", ItemIds.billet_mox_fuel,
                TagIds.nuggets_pu239, TagIds.nuggets_pu239, ItemIds.nugget_uranium_fuel, ItemIds.nugget_uranium_fuel,
                ItemIds.nugget_uranium_fuel, ItemIds.nugget_uranium_fuel);
        shapeless(NS, "billet_neptunium_fuel_2", ItemIds.billet_neptunium_fuel, 3,
                ItemIds.billet_u238, ItemIds.billet_u238, ItemIds.billet_neptunium);
        shapeless(NS, "billet_neptunium_fuel_3", ItemIds.billet_neptunium_fuel,
                TagIds.nuggets_neptunium, TagIds.nuggets_neptunium, TagIds.nuggets_u238, TagIds.nuggets_u238,
                TagIds.nuggets_u238, TagIds.nuggets_u238);
        shapeless(NS, "billet_plutonium_fuel_2", ItemIds.billet_plutonium_fuel, 3,
                ItemIds.billet_u238, ItemIds.billet_u238, ItemIds.billet_pu_mix);
        shapeless(NS, "billet_plutonium_fuel_3", ItemIds.billet_plutonium_fuel,
                ItemIds.nugget_pu_mix, ItemIds.nugget_pu_mix, TagIds.nuggets_u238, TagIds.nuggets_u238,
                TagIds.nuggets_u238, TagIds.nuggets_u238);
        shapeless(NS, "billet_po210be", ItemIds.billet_po210be,
                TagIds.nuggets_polonium, TagIds.nuggets_polonium, TagIds.nuggets_polonium, ItemIds.nugget_beryllium,
                ItemIds.nugget_beryllium, ItemIds.nugget_beryllium);
        shapeless(NS, "billet_po210be_1", ItemIds.billet_po210be, 2, ItemIds.billet_polonium, ItemIds.billet_beryllium);
        shapeless(NS, "billet_po210be_2", ItemIds.billet_po210be, 6,
                ItemIds.billet_polonium, ItemIds.billet_polonium, ItemIds.billet_polonium, ItemIds.billet_beryllium,
                ItemIds.billet_beryllium, ItemIds.billet_beryllium);
        shapeless(NS, "billet_pu238be", ItemIds.billet_pu238be,
                TagIds.nuggets_pu238, TagIds.nuggets_pu238, TagIds.nuggets_pu238, ItemIds.nugget_beryllium,
                ItemIds.nugget_beryllium, ItemIds.nugget_beryllium);
        shapeless(NS, "billet_pu238be_1", ItemIds.billet_pu238be, 2, ItemIds.billet_pu238, ItemIds.billet_beryllium);
        shapeless(NS, "billet_pu238be_2", ItemIds.billet_pu238be, 6,
                ItemIds.billet_pu238, ItemIds.billet_pu238, ItemIds.billet_pu238, ItemIds.billet_beryllium,
                ItemIds.billet_beryllium, ItemIds.billet_beryllium);
        shapeless(NS, "billet_pu_mix_2", ItemIds.billet_pu_mix, 3,
                ItemIds.billet_pu239, ItemIds.billet_pu239, ItemIds.billet_pu240);
        shapeless(NS, "billet_pu_mix_3", ItemIds.billet_pu_mix,
                TagIds.nuggets_pu239, TagIds.nuggets_pu239, TagIds.nuggets_pu239, TagIds.nuggets_pu239,
                TagIds.nuggets_pu240, TagIds.nuggets_pu240);
        shapeless(NS, "billet_ra226be", ItemIds.billet_ra226be,
                TagIds.nuggets_ra226, TagIds.nuggets_ra226, TagIds.nuggets_ra226, ItemIds.nugget_beryllium,
                ItemIds.nugget_beryllium, ItemIds.nugget_beryllium);
        shapeless(NS, "billet_ra226be_1", ItemIds.billet_ra226be, 2, ItemIds.billet_ra226, ItemIds.billet_beryllium);
        shapeless(NS, "billet_ra226be_2", ItemIds.billet_ra226be, 6,
                ItemIds.billet_ra226, ItemIds.billet_ra226, ItemIds.billet_ra226, ItemIds.billet_beryllium,
                ItemIds.billet_beryllium, ItemIds.billet_beryllium);
        shapeless(NS, "billet_schrabidium_fuel_2", ItemIds.billet_schrabidium_fuel, 3,
                ItemIds.billet_schrabidium, ItemIds.billet_neptunium, ItemIds.billet_beryllium);
        shapeless(NS, "billet_schrabidium_fuel_3", ItemIds.billet_schrabidium_fuel,
                ItemIds.nugget_schrabidium, ItemIds.nugget_schrabidium, TagIds.nuggets_neptunium,
                TagIds.nuggets_neptunium, ItemIds.nugget_beryllium, ItemIds.nugget_beryllium);
        shapeless(NS, "billet_thorium_fuel_2", ItemIds.billet_thorium_fuel, 6,
                ItemIds.billet_th232, ItemIds.billet_th232, ItemIds.billet_th232, ItemIds.billet_th232,
                ItemIds.billet_th232, ItemIds.billet_u233);
        shapeless(NS, "billet_thorium_fuel_3", ItemIds.billet_thorium_fuel,
                TagIds.nuggets_thorium, TagIds.nuggets_thorium, TagIds.nuggets_thorium, TagIds.nuggets_thorium,
                TagIds.nuggets_thorium, TagIds.nuggets_u233);
        shapeless(NS, "billet_uranium_2", ItemIds.billet_uranium, 2, ItemIds.billet_uranium_fuel, ItemIds.billet_u238);
        shapeless(NS, "billet_uranium_3", ItemIds.billet_uranium, 2,
                ItemIds.billet_u238, TagIds.nuggets_u238, TagIds.nuggets_u238, TagIds.nuggets_u238, TagIds.nuggets_u238,
                TagIds.nuggets_u238, TagIds.nuggets_u235);
        shapeless(NS, "billet_uranium_fuel_2", ItemIds.billet_uranium_fuel, 6,
                ItemIds.billet_u238, ItemIds.billet_u238, ItemIds.billet_u238, ItemIds.billet_u238, ItemIds.billet_u238,
                ItemIds.billet_u235);
        shapeless(NS, "billet_uranium_fuel_3", ItemIds.billet_uranium_fuel,
                TagIds.nuggets_u238, TagIds.nuggets_u238, TagIds.nuggets_u238, TagIds.nuggets_u238, TagIds.nuggets_u238,
                TagIds.nuggets_u235);
        shapeless(NS, "billet_zfb_am_mix", ItemIds.billet_zfb_am_mix,
                TagIds.nuggets_zirconium, TagIds.nuggets_zirconium, TagIds.nuggets_zirconium, TagIds.nuggets_pu241,
                TagIds.nuggets_pu241, TagIds.nuggets_am_mix);
        shapeless(NS, "billet_zfb_am_mix_1", ItemIds.billet_zfb_am_mix, 6,
                TagIds.billets_zirconium, TagIds.billets_zirconium, TagIds.billets_zirconium, TagIds.billets_pu241,
                TagIds.billets_pu241, TagIds.billets_am_mix);
        shapeless(NS, "billet_zfb_bismuth", ItemIds.billet_zfb_bismuth,
                TagIds.nuggets_zirconium, TagIds.nuggets_zirconium, TagIds.nuggets_zirconium, TagIds.nuggets_uranium,
                TagIds.nuggets_pu241, TagIds.nuggets_bismuth);
        shapeless(NS, "billet_zfb_bismuth_1", ItemIds.billet_zfb_bismuth, 6,
                TagIds.billets_zirconium, TagIds.billets_zirconium, TagIds.billets_zirconium, TagIds.billets_uranium,
                TagIds.billets_pu241, TagIds.billets_bismuth);
        shapeless(NS, "billet_zfb_pu241", ItemIds.billet_zfb_pu241,
                TagIds.nuggets_zirconium, TagIds.nuggets_zirconium, TagIds.nuggets_zirconium, TagIds.nuggets_u235,
                TagIds.nuggets_pu240, TagIds.nuggets_pu241);
        shapeless(NS, "billet_zfb_pu241_1", ItemIds.billet_zfb_pu241, 6,
                TagIds.billets_zirconium, TagIds.billets_zirconium, TagIds.billets_zirconium, TagIds.billets_u235,
                TagIds.billets_pu240, TagIds.billets_pu241);
        shapeless(NS, "biomass", ItemIds.biomass, 4,
                VanillaIds.sugar, ItemIds.powder_sawdust, ItemIds.powder_sawdust, ItemIds.powder_sawdust,
                ItemIds.powder_sawdust, ItemIds.powder_sawdust);
        shapeless(NS, "biomass_1", ItemIds.biomass, 4,
                VanillaIds.sugar, ItemIds.powder_sawdust, ItemIds.powder_sawdust, VanillaIds.apple, VanillaIds.apple,
                VanillaIds.apple);
        shapeless(NS, "biomass_10", ItemIds.biomass, 4,
                VanillaIds.sugar, ItemIds.powder_sawdust, ItemIds.powder_sawdust, VanillaIds.cactus, VanillaIds.cactus,
                VanillaIds.cactus);
        shapeless(NS, "biomass_11", ItemIds.biomass, 4,
                VanillaIds.sugar, ItemIds.powder_sawdust, ItemIds.powder_sawdust, VanillaIds.wheat, VanillaIds.wheat,
                VanillaIds.wheat, VanillaIds.wheat, VanillaIds.wheat, VanillaIds.wheat);
        shapeless(NS, "biomass_2", ItemIds.biomass, 4,
                VanillaIds.sugar, ItemIds.powder_sawdust, ItemIds.powder_sawdust, VanillaIds.sugar_cane,
                VanillaIds.sugar_cane, VanillaIds.sugar_cane);
        shapeless(NS, "biomass_3", ItemIds.biomass, 4,
                VanillaIds.sugar, ItemIds.powder_sawdust, ItemIds.powder_sawdust, VanillaIds.rotten_flesh,
                VanillaIds.rotten_flesh, VanillaIds.rotten_flesh);
        shapeless(NS, "biomass_4", ItemIds.biomass, 4,
                VanillaIds.sugar, ItemIds.powder_sawdust, ItemIds.powder_sawdust, VanillaIds.carrot, VanillaIds.carrot,
                VanillaIds.carrot, VanillaIds.carrot, VanillaIds.carrot, VanillaIds.carrot);
        shapeless(NS, "biomass_5", ItemIds.biomass, 4,
                VanillaIds.sugar, ItemIds.powder_sawdust, ItemIds.powder_sawdust, VanillaIds.potato, VanillaIds.potato,
                VanillaIds.potato, VanillaIds.potato, VanillaIds.potato, VanillaIds.potato);
        shapeless(NS, "biomass_6", ItemIds.biomass, 4,
                VanillaIds.sugar, ItemIds.powder_sawdust, ItemIds.powder_sawdust, TagIds.minecraft_saplings,
                TagIds.minecraft_saplings, TagIds.minecraft_saplings, TagIds.minecraft_saplings,
                TagIds.minecraft_saplings, TagIds.minecraft_saplings);
        shapeless(NS, "biomass_7", ItemIds.biomass, 4,
                VanillaIds.sugar, ItemIds.powder_sawdust, ItemIds.powder_sawdust, TagIds.minecraft_leaves,
                TagIds.minecraft_leaves, TagIds.minecraft_leaves, TagIds.minecraft_leaves, TagIds.minecraft_leaves,
                TagIds.minecraft_leaves);
        shapeless(NS, "biomass_8", ItemIds.biomass, 4,
                VanillaIds.sugar, ItemIds.powder_sawdust, ItemIds.powder_sawdust, VanillaIds.pumpkin);
        shapeless(NS, "biomass_9", ItemIds.biomass, 4,
                VanillaIds.sugar, ItemIds.powder_sawdust, ItemIds.powder_sawdust, VanillaIds.melon);
        shapeless(NS, "block_c4_1", ItemIds.ingot_c4, 9, ItemIds.block_c4);
        shapeless(NS, "block_semtex_1", ItemIds.ingot_semtex, 9, ItemIds.block_semtex);
        shapeless(NS, "brick_asbestos", ItemIds.brick_asbestos, 3,
                ItemIds.brick_asbestos_stairs, ItemIds.brick_asbestos_stairs, ItemIds.brick_asbestos_stairs,
                ItemIds.brick_asbestos_stairs);
        shapeless(NS, "brick_asbestos_1", ItemIds.brick_asbestos,
                ItemIds.brick_asbestos_slab, ItemIds.brick_asbestos_slab);
        shapeless(NS, "brick_compound", ItemIds.brick_compound, 3,
                ItemIds.brick_compound_stairs, ItemIds.brick_compound_stairs, ItemIds.brick_compound_stairs,
                ItemIds.brick_compound_stairs);
        shapeless(NS, "brick_compound_1", ItemIds.brick_compound,
                ItemIds.brick_compound_slab, ItemIds.brick_compound_slab);
        shapeless(NS, "brick_concrete", ItemIds.brick_concrete, 3,
                ItemIds.brick_concrete_stairs, ItemIds.brick_concrete_stairs, ItemIds.brick_concrete_stairs,
                ItemIds.brick_concrete_stairs);
        shapeless(NS, "brick_concrete_1", ItemIds.brick_concrete,
                ItemIds.brick_concrete_slab, ItemIds.brick_concrete_slab);
        shapeless(NS, "brick_concrete_broken", ItemIds.brick_concrete_broken, 3,
                ItemIds.brick_concrete_broken_stairs, ItemIds.brick_concrete_broken_stairs,
                ItemIds.brick_concrete_broken_stairs, ItemIds.brick_concrete_broken_stairs);
        shapeless(NS, "brick_concrete_broken_1", ItemIds.brick_concrete_broken,
                ItemIds.brick_concrete_broken_slab, ItemIds.brick_concrete_broken_slab);
        shapeless(NS, "brick_concrete_cracked", ItemIds.brick_concrete_cracked, 3,
                ItemIds.brick_concrete_cracked_stairs, ItemIds.brick_concrete_cracked_stairs,
                ItemIds.brick_concrete_cracked_stairs, ItemIds.brick_concrete_cracked_stairs);
        shapeless(NS, "brick_concrete_cracked_1", ItemIds.brick_concrete_cracked,
                ItemIds.brick_concrete_cracked_slab, ItemIds.brick_concrete_cracked_slab);
        shapeless(NS, "brick_concrete_mossy", ItemIds.brick_concrete_mossy, 3,
                ItemIds.brick_concrete_mossy_stairs, ItemIds.brick_concrete_mossy_stairs,
                ItemIds.brick_concrete_mossy_stairs, ItemIds.brick_concrete_mossy_stairs);
        shapeless(NS, "brick_concrete_mossy_1", ItemIds.brick_concrete_mossy,
                ItemIds.brick_concrete_mossy_slab, ItemIds.brick_concrete_mossy_slab);
        shapeless(NS, "brick_fire", ItemIds.brick_fire, 3,
                ItemIds.brick_fire_stairs, ItemIds.brick_fire_stairs, ItemIds.brick_fire_stairs,
                ItemIds.brick_fire_stairs);
        shapeless(NS, "brick_fire_1", ItemIds.brick_fire, ItemIds.brick_fire_slab, ItemIds.brick_fire_slab);
        shapeless(NS, "brick_light", ItemIds.brick_light, 3,
                ItemIds.brick_light_stairs, ItemIds.brick_light_stairs, ItemIds.brick_light_stairs,
                ItemIds.brick_light_stairs);
        shapeless(NS, "brick_light_1", ItemIds.brick_light, ItemIds.brick_light_slab, ItemIds.brick_light_slab);
        shapeless(NS, "brick_obsidian", ItemIds.brick_obsidian, 3,
                ItemIds.brick_obsidian_stairs, ItemIds.brick_obsidian_stairs, ItemIds.brick_obsidian_stairs,
                ItemIds.brick_obsidian_stairs);
        shapeless(NS, "brick_obsidian_1", ItemIds.brick_obsidian,
                ItemIds.brick_obsidian_slab, ItemIds.brick_obsidian_slab);
        shapeless(NS, "catalyst_clay", ItemIds.catalyst_clay, TagIds.dusts_iron, VanillaIds.clay_ball);
        shapeless(NS, "charge_c4", ItemIds.charge_c4,
                ItemIds.stick_c4, ItemIds.stick_c4, ItemIds.stick_c4, ItemIds.ducttape);
        shapeless(NS, "charge_dynamite", ItemIds.charge_dynamite,
                ItemIds.stick_dynamite, ItemIds.stick_dynamite, ItemIds.stick_dynamite, ItemIds.ducttape);
        shapeless(NS, "charge_semtex", ItemIds.charge_semtex,
                ItemIds.stick_semtex, ItemIds.stick_semtex, ItemIds.stick_semtex, ItemIds.ducttape);
        shapeless(NS, "cheese_quesadilla", ItemIds.cheese_quesadilla, 3,
                ItemIds.cheese, ItemIds.cheese, VanillaIds.bread);
        shapeless(NS, "chemical_dye_brown", ItemIds.chemical_dye_brown, 2,
                ItemIds.chemical_dye_orange, ItemIds.chemical_dye_black);
        shapeless(NS, "chemical_dye_cyan", ItemIds.chemical_dye_cyan, 2,
                ItemIds.chemical_dye_blue, ItemIds.chemical_dye_green);
        shapeless(NS, "chemical_dye_gray", ItemIds.chemical_dye_gray, 2,
                ItemIds.chemical_dye_black, ItemIds.chemical_dye_white);
        shapeless(NS, "chemical_dye_green", ItemIds.chemical_dye_green, 2,
                ItemIds.chemical_dye_blue, ItemIds.chemical_dye_yellow);
        shapeless(NS, "chemical_dye_lightblue", ItemIds.chemical_dye_lightblue, 2,
                ItemIds.chemical_dye_blue, ItemIds.chemical_dye_white);
        shapeless(NS, "chemical_dye_lime", ItemIds.chemical_dye_lime, 2,
                ItemIds.chemical_dye_green, ItemIds.chemical_dye_white);
        shapeless(NS, "chemical_dye_magenta", ItemIds.chemical_dye_magenta, 2,
                ItemIds.chemical_dye_pink, ItemIds.chemical_dye_purple);
        shapeless(NS, "chemical_dye_orange", ItemIds.chemical_dye_orange, 2,
                ItemIds.chemical_dye_red, ItemIds.chemical_dye_yellow);
        shapeless(NS, "chemical_dye_pink", ItemIds.chemical_dye_pink, 2,
                ItemIds.chemical_dye_red, ItemIds.chemical_dye_white);
        shapeless(NS, "chemical_dye_purple", ItemIds.chemical_dye_purple, 2,
                ItemIds.chemical_dye_red, ItemIds.chemical_dye_blue);
        shapeless(NS, "chemical_dye_silver", ItemIds.chemical_dye_silver, 2,
                ItemIds.chemical_dye_gray, ItemIds.chemical_dye_white);
        shapeless(NS, "cladding_paint", ItemIds.cladding_paint,
                TagIds.nuggets_lead, TagIds.nuggets_lead, TagIds.nuggets_lead, TagIds.nuggets_lead, VanillaIds.clay_ball,
                VanillaIds.glass_bottle);
        shapeless(NS, "clay_ball", VanillaIds.clay_ball, 4, VanillaIds.clay);
        shapeless(NS, "cmb_brick_reinforced", ItemIds.cmb_brick_reinforced, 3,
                ItemIds.cmb_brick_reinforced_stairs, ItemIds.cmb_brick_reinforced_stairs,
                ItemIds.cmb_brick_reinforced_stairs, ItemIds.cmb_brick_reinforced_stairs);
        shapeless(NS, "cmb_brick_reinforced_1", ItemIds.cmb_brick_reinforced,
                ItemIds.cmb_brick_reinforced_slab, ItemIds.cmb_brick_reinforced_slab);
        shapeless(NS, "concrete", ItemIds.concrete, 3,
                ItemIds.concrete_stairs, ItemIds.concrete_stairs, ItemIds.concrete_stairs, ItemIds.concrete_stairs);
        shapeless(NS, "concrete_1", ItemIds.concrete, ItemIds.concrete_slab, ItemIds.concrete_slab);
        shapeless(NS, "concrete_asbestos", ItemIds.concrete_asbestos, 3,
                ItemIds.concrete_asbestos_stairs, ItemIds.concrete_asbestos_stairs, ItemIds.concrete_asbestos_stairs,
                ItemIds.concrete_asbestos_stairs);
        shapeless(NS, "concrete_asbestos_1", ItemIds.concrete_asbestos,
                ItemIds.concrete_asbestos_slab, ItemIds.concrete_asbestos_slab);
        shapeless(NS, "concrete_colored_ext_stairs_bronze_from_stairs",
                stack(ItemIds.concrete_colored_ext, 3).with(ComponentIds.item_variant, 7),
                ItemIds.concrete_colored_ext_stairs_bronze, ItemIds.concrete_colored_ext_stairs_bronze,
                ItemIds.concrete_colored_ext_stairs_bronze, ItemIds.concrete_colored_ext_stairs_bronze);
    }

    private static void shapeless1() {
        shapeless(NS, "concrete_colored_ext_stairs_hazard_from_stairs",
                stack(ItemIds.concrete_colored_ext, 3).with(ComponentIds.item_variant, 5),
                ItemIds.concrete_colored_ext_stairs_hazard, ItemIds.concrete_colored_ext_stairs_hazard,
                ItemIds.concrete_colored_ext_stairs_hazard, ItemIds.concrete_colored_ext_stairs_hazard);
        shapeless(NS, "concrete_colored_ext_stairs_indigo_from_stairs",
                stack(ItemIds.concrete_colored_ext, 3).with(ComponentIds.item_variant, 2),
                ItemIds.concrete_colored_ext_stairs_indigo, ItemIds.concrete_colored_ext_stairs_indigo,
                ItemIds.concrete_colored_ext_stairs_indigo, ItemIds.concrete_colored_ext_stairs_indigo);
        shapeless(NS, "concrete_colored_ext_stairs_machine_from_stairs",
                stack(ItemIds.concrete_colored_ext, 3).with(ComponentIds.item_variant, 0),
                ItemIds.concrete_colored_ext_stairs_machine, ItemIds.concrete_colored_ext_stairs_machine,
                ItemIds.concrete_colored_ext_stairs_machine, ItemIds.concrete_colored_ext_stairs_machine);
        shapeless(NS, "concrete_colored_ext_stairs_machine_stripe_from_stairs",
                stack(ItemIds.concrete_colored_ext, 3).with(ComponentIds.item_variant, 1),
                ItemIds.concrete_colored_ext_stairs_machine_stripe, ItemIds.concrete_colored_ext_stairs_machine_stripe,
                ItemIds.concrete_colored_ext_stairs_machine_stripe, ItemIds.concrete_colored_ext_stairs_machine_stripe);
        shapeless(NS, "concrete_colored_ext_stairs_pink_from_stairs",
                stack(ItemIds.concrete_colored_ext, 3).with(ComponentIds.item_variant, 4),
                ItemIds.concrete_colored_ext_stairs_pink, ItemIds.concrete_colored_ext_stairs_pink,
                ItemIds.concrete_colored_ext_stairs_pink, ItemIds.concrete_colored_ext_stairs_pink);
        shapeless(NS, "concrete_colored_ext_stairs_purple_from_stairs",
                stack(ItemIds.concrete_colored_ext, 3).with(ComponentIds.item_variant, 3),
                ItemIds.concrete_colored_ext_stairs_purple, ItemIds.concrete_colored_ext_stairs_purple,
                ItemIds.concrete_colored_ext_stairs_purple, ItemIds.concrete_colored_ext_stairs_purple);
        shapeless(NS, "concrete_colored_ext_stairs_sand_from_stairs",
                stack(ItemIds.concrete_colored_ext, 3).with(ComponentIds.item_variant, 6),
                ItemIds.concrete_colored_ext_stairs_sand, ItemIds.concrete_colored_ext_stairs_sand,
                ItemIds.concrete_colored_ext_stairs_sand, ItemIds.concrete_colored_ext_stairs_sand);
        shapeless(NS, "concrete_colored_stairs_black_from_slab",
                stack(ItemIds.concrete_colored).with(ComponentIds.item_variant, 15),
                ItemIds.concrete_black_slab, ItemIds.concrete_black_slab);
        shapeless(NS, "concrete_colored_stairs_black_from_stairs",
                stack(ItemIds.concrete_colored, 3).with(ComponentIds.item_variant, 15),
                ItemIds.concrete_colored_stairs_black, ItemIds.concrete_colored_stairs_black,
                ItemIds.concrete_colored_stairs_black, ItemIds.concrete_colored_stairs_black);
        shapeless(NS, "concrete_colored_stairs_blue_from_slab",
                stack(ItemIds.concrete_colored).with(ComponentIds.item_variant, 11),
                ItemIds.concrete_blue_slab, ItemIds.concrete_blue_slab);
        shapeless(NS, "concrete_colored_stairs_blue_from_stairs",
                stack(ItemIds.concrete_colored, 3).with(ComponentIds.item_variant, 11),
                ItemIds.concrete_colored_stairs_blue, ItemIds.concrete_colored_stairs_blue,
                ItemIds.concrete_colored_stairs_blue, ItemIds.concrete_colored_stairs_blue);
        shapeless(NS, "concrete_colored_stairs_brown_from_slab",
                stack(ItemIds.concrete_colored).with(ComponentIds.item_variant, 12),
                ItemIds.concrete_brown_slab, ItemIds.concrete_brown_slab);
        shapeless(NS, "concrete_colored_stairs_brown_from_stairs",
                stack(ItemIds.concrete_colored, 3).with(ComponentIds.item_variant, 12),
                ItemIds.concrete_colored_stairs_brown, ItemIds.concrete_colored_stairs_brown,
                ItemIds.concrete_colored_stairs_brown, ItemIds.concrete_colored_stairs_brown);
        shapeless(NS, "concrete_colored_stairs_cyan_from_slab",
                stack(ItemIds.concrete_colored).with(ComponentIds.item_variant, 9),
                ItemIds.concrete_cyan_slab, ItemIds.concrete_cyan_slab);
        shapeless(NS, "concrete_colored_stairs_cyan_from_stairs",
                stack(ItemIds.concrete_colored, 3).with(ComponentIds.item_variant, 9),
                ItemIds.concrete_colored_stairs_cyan, ItemIds.concrete_colored_stairs_cyan,
                ItemIds.concrete_colored_stairs_cyan, ItemIds.concrete_colored_stairs_cyan);
        shapeless(NS, "concrete_colored_stairs_gray_from_slab",
                stack(ItemIds.concrete_colored).with(ComponentIds.item_variant, 7),
                ItemIds.concrete_gray_slab, ItemIds.concrete_gray_slab);
        shapeless(NS, "concrete_colored_stairs_gray_from_stairs",
                stack(ItemIds.concrete_colored, 3).with(ComponentIds.item_variant, 7),
                ItemIds.concrete_colored_stairs_gray, ItemIds.concrete_colored_stairs_gray,
                ItemIds.concrete_colored_stairs_gray, ItemIds.concrete_colored_stairs_gray);
        shapeless(NS, "concrete_colored_stairs_green_from_slab",
                stack(ItemIds.concrete_colored).with(ComponentIds.item_variant, 13),
                ItemIds.concrete_green_slab, ItemIds.concrete_green_slab);
        shapeless(NS, "concrete_colored_stairs_green_from_stairs",
                stack(ItemIds.concrete_colored, 3).with(ComponentIds.item_variant, 13),
                ItemIds.concrete_colored_stairs_green, ItemIds.concrete_colored_stairs_green,
                ItemIds.concrete_colored_stairs_green, ItemIds.concrete_colored_stairs_green);
        shapeless(NS, "concrete_colored_stairs_light_blue_from_slab",
                stack(ItemIds.concrete_colored).with(ComponentIds.item_variant, 3),
                ItemIds.concrete_light_blue_slab, ItemIds.concrete_light_blue_slab);
        shapeless(NS, "concrete_colored_stairs_light_blue_from_stairs",
                stack(ItemIds.concrete_colored, 3).with(ComponentIds.item_variant, 3),
                ItemIds.concrete_colored_stairs_light_blue, ItemIds.concrete_colored_stairs_light_blue,
                ItemIds.concrete_colored_stairs_light_blue, ItemIds.concrete_colored_stairs_light_blue);
        shapeless(NS, "concrete_colored_stairs_lime_from_slab",
                stack(ItemIds.concrete_colored).with(ComponentIds.item_variant, 5),
                ItemIds.concrete_lime_slab, ItemIds.concrete_lime_slab);
        shapeless(NS, "concrete_colored_stairs_lime_from_stairs",
                stack(ItemIds.concrete_colored, 3).with(ComponentIds.item_variant, 5),
                ItemIds.concrete_colored_stairs_lime, ItemIds.concrete_colored_stairs_lime,
                ItemIds.concrete_colored_stairs_lime, ItemIds.concrete_colored_stairs_lime);
        shapeless(NS, "concrete_colored_stairs_magenta_from_slab",
                stack(ItemIds.concrete_colored).with(ComponentIds.item_variant, 2),
                ItemIds.concrete_magenta_slab, ItemIds.concrete_magenta_slab);
        shapeless(NS, "concrete_colored_stairs_magenta_from_stairs",
                stack(ItemIds.concrete_colored, 3).with(ComponentIds.item_variant, 2),
                ItemIds.concrete_colored_stairs_magenta, ItemIds.concrete_colored_stairs_magenta,
                ItemIds.concrete_colored_stairs_magenta, ItemIds.concrete_colored_stairs_magenta);
        shapeless(NS, "concrete_colored_stairs_orange_from_slab",
                stack(ItemIds.concrete_colored).with(ComponentIds.item_variant, 1),
                ItemIds.concrete_orange_slab, ItemIds.concrete_orange_slab);
        shapeless(NS, "concrete_colored_stairs_orange_from_stairs",
                stack(ItemIds.concrete_colored, 3).with(ComponentIds.item_variant, 1),
                ItemIds.concrete_colored_stairs_orange, ItemIds.concrete_colored_stairs_orange,
                ItemIds.concrete_colored_stairs_orange, ItemIds.concrete_colored_stairs_orange);
        shapeless(NS, "concrete_colored_stairs_pink_from_slab",
                stack(ItemIds.concrete_colored).with(ComponentIds.item_variant, 6),
                ItemIds.concrete_pink_slab, ItemIds.concrete_pink_slab);
        shapeless(NS, "concrete_colored_stairs_pink_from_stairs",
                stack(ItemIds.concrete_colored, 3).with(ComponentIds.item_variant, 6),
                ItemIds.concrete_colored_stairs_pink, ItemIds.concrete_colored_stairs_pink,
                ItemIds.concrete_colored_stairs_pink, ItemIds.concrete_colored_stairs_pink);
        shapeless(NS, "concrete_colored_stairs_purple_from_slab",
                stack(ItemIds.concrete_colored).with(ComponentIds.item_variant, 10),
                ItemIds.concrete_purple_slab, ItemIds.concrete_purple_slab);
        shapeless(NS, "concrete_colored_stairs_purple_from_stairs",
                stack(ItemIds.concrete_colored, 3).with(ComponentIds.item_variant, 10),
                ItemIds.concrete_colored_stairs_purple, ItemIds.concrete_colored_stairs_purple,
                ItemIds.concrete_colored_stairs_purple, ItemIds.concrete_colored_stairs_purple);
        shapeless(NS, "concrete_colored_stairs_red_from_slab",
                stack(ItemIds.concrete_colored).with(ComponentIds.item_variant, 14),
                ItemIds.concrete_red_slab, ItemIds.concrete_red_slab);
        shapeless(NS, "concrete_colored_stairs_red_from_stairs",
                stack(ItemIds.concrete_colored, 3).with(ComponentIds.item_variant, 14),
                ItemIds.concrete_colored_stairs_red, ItemIds.concrete_colored_stairs_red,
                ItemIds.concrete_colored_stairs_red, ItemIds.concrete_colored_stairs_red);
        shapeless(NS, "concrete_colored_stairs_silver_from_slab",
                stack(ItemIds.concrete_colored).with(ComponentIds.item_variant, 8),
                ItemIds.concrete_silver_slab, ItemIds.concrete_silver_slab);
        shapeless(NS, "concrete_colored_stairs_silver_from_stairs",
                stack(ItemIds.concrete_colored, 3).with(ComponentIds.item_variant, 8),
                ItemIds.concrete_colored_stairs_silver, ItemIds.concrete_colored_stairs_silver,
                ItemIds.concrete_colored_stairs_silver, ItemIds.concrete_colored_stairs_silver);
        shapeless(NS, "concrete_colored_stairs_white_from_slab",
                stack(ItemIds.concrete_colored).with(ComponentIds.item_variant, 0),
                ItemIds.concrete_white_slab, ItemIds.concrete_white_slab);
        shapeless(NS, "concrete_colored_stairs_white_from_stairs",
                stack(ItemIds.concrete_colored, 3).with(ComponentIds.item_variant, 0),
                ItemIds.concrete_colored_stairs_white, ItemIds.concrete_colored_stairs_white,
                ItemIds.concrete_colored_stairs_white, ItemIds.concrete_colored_stairs_white);
        shapeless(NS, "concrete_colored_stairs_yellow_from_slab",
                stack(ItemIds.concrete_colored).with(ComponentIds.item_variant, 4),
                ItemIds.concrete_yellow_slab, ItemIds.concrete_yellow_slab);
        shapeless(NS, "concrete_colored_stairs_yellow_from_stairs",
                stack(ItemIds.concrete_colored, 3).with(ComponentIds.item_variant, 4),
                ItemIds.concrete_colored_stairs_yellow, ItemIds.concrete_colored_stairs_yellow,
                ItemIds.concrete_colored_stairs_yellow, ItemIds.concrete_colored_stairs_yellow);
        shapeless(NS, "concrete_smooth", ItemIds.concrete_smooth, 3,
                ItemIds.concrete_smooth_stairs, ItemIds.concrete_smooth_stairs, ItemIds.concrete_smooth_stairs,
                ItemIds.concrete_smooth_stairs);
        shapeless(NS, "concrete_smooth_1", ItemIds.concrete_smooth,
                ItemIds.concrete_smooth_slab, ItemIds.concrete_smooth_slab);
        shapeless(NS, "crayon_black", ItemIds.crayon_black, 4,
                ItemIds.chemical_dye_black, TagIds.tars, VanillaIds.paper);
        shapeless(NS, "crayon_blue", ItemIds.crayon_blue, 4, ItemIds.chemical_dye_blue, TagIds.tars, VanillaIds.paper);
        shapeless(NS, "crayon_brown", ItemIds.crayon_brown, 4,
                ItemIds.chemical_dye_brown, TagIds.tars, VanillaIds.paper);
        shapeless(NS, "crayon_cyan", ItemIds.crayon_cyan, 4, ItemIds.chemical_dye_cyan, TagIds.tars, VanillaIds.paper);
        shapeless(NS, "crayon_gray", ItemIds.crayon_gray, 4, ItemIds.chemical_dye_gray, TagIds.tars, VanillaIds.paper);
        shapeless(NS, "crayon_green", ItemIds.crayon_green, 4,
                ItemIds.chemical_dye_green, TagIds.tars, VanillaIds.paper);
        shapeless(NS, "crayon_lightblue", ItemIds.crayon_lightblue, 4,
                ItemIds.chemical_dye_lightblue, TagIds.tars, VanillaIds.paper);
        shapeless(NS, "crayon_lime", ItemIds.crayon_lime, 4, ItemIds.chemical_dye_lime, TagIds.tars, VanillaIds.paper);
        shapeless(NS, "crayon_magenta", ItemIds.crayon_magenta, 4,
                ItemIds.chemical_dye_magenta, TagIds.tars, VanillaIds.paper);
        shapeless(NS, "crayon_orange", ItemIds.crayon_orange, 4,
                ItemIds.chemical_dye_orange, TagIds.tars, VanillaIds.paper);
        shapeless(NS, "crayon_pink", ItemIds.crayon_pink, 4, ItemIds.chemical_dye_pink, TagIds.tars, VanillaIds.paper);
        shapeless(NS, "crayon_purple", ItemIds.crayon_purple, 4,
                ItemIds.chemical_dye_purple, TagIds.tars, VanillaIds.paper);
        shapeless(NS, "crayon_red", ItemIds.crayon_red, 4, ItemIds.chemical_dye_red, TagIds.tars, VanillaIds.paper);
        shapeless(NS, "crayon_silver", ItemIds.crayon_silver, 4,
                ItemIds.chemical_dye_silver, TagIds.tars, VanillaIds.paper);
        shapeless(NS, "crayon_white", ItemIds.crayon_white, 4,
                ItemIds.chemical_dye_white, TagIds.tars, VanillaIds.paper);
        shapeless(NS, "crayon_yellow", ItemIds.crayon_yellow, 4,
                ItemIds.chemical_dye_yellow, TagIds.tars, VanillaIds.paper);
        shapeless(NS, "deco_pipe_1", ItemIds.deco_pipe, ItemIds.deco_pipe_rim);
        shapeless(NS, "deco_pipe_2", ItemIds.deco_pipe, ItemIds.deco_pipe_framed);
        shapeless(NS, "deco_pipe_3", ItemIds.deco_pipe, ItemIds.deco_pipe_quad);
        shapeless(NS, "definitelyfood", ItemIds.definitelyfood, 4,
                TagIds.ingots_any_rubber, VanillaIds.wheat, VanillaIds.rotten_flesh, TagIds.minecraft_saplings);
        shapeless(NS, "definitelyfood_1", ItemIds.definitelyfood, 4,
                TagIds.ingots_any_rubber, VanillaIds.wheat, VanillaIds.rotten_flesh, VanillaIds.wheat_seeds,
                VanillaIds.wheat_seeds, VanillaIds.wheat_seeds);
        shapeless(NS, "detonator_deadman", ItemIds.detonator_deadman,
                ItemIds.detonator, ItemIds.defuser, ItemIds.ducttape);
        shapeless(NS, "detonator_multi", ItemIds.detonator_multi, ItemIds.detonator, ItemIds.circuit_advanced);
        shapeless(NS, "ducrete", ItemIds.ducrete, 3,
                ItemIds.ducrete_stairs, ItemIds.ducrete_stairs, ItemIds.ducrete_stairs, ItemIds.ducrete_stairs);
        shapeless(NS, "ducrete_1", ItemIds.ducrete, ItemIds.ducrete_slab, ItemIds.ducrete_slab);
        shapeless(NS, "ducrete_smooth", ItemIds.ducrete_smooth, 3,
                ItemIds.ducrete_smooth_stairs, ItemIds.ducrete_smooth_stairs, ItemIds.ducrete_smooth_stairs,
                ItemIds.ducrete_smooth_stairs);
        shapeless(NS, "ducrete_smooth_1", ItemIds.ducrete_smooth,
                ItemIds.ducrete_smooth_slab, ItemIds.ducrete_smooth_slab);
        shapeless(NS, "fence_metal_1", ItemIds.fence_metal, ItemIds.fence_metal_post);
        shapeless(NS, "fence_metal_post", ItemIds.fence_metal_post, ItemIds.fence_metal);
        shapeless(NS, "fluid_duct_gauge", ItemIds.fluid_duct_gauge,
                ItemIds.fluid_duct_paintable, TagIds.ingots_steel, ItemIds.circuit_basic);
        shapeless(NS, "fmn", ItemIds.fmn, TagIds.dusts_coal, TagIds.dusts_polonium, TagIds.dusts_strontium);
        shapeless(NS, "foundry_outlet", ItemIds.foundry_outlet, ItemIds.foundry_channel, TagIds.plates_steel);
        shapeless(NS, "foundry_slagtap", ItemIds.foundry_slagtap, ItemIds.foundry_channel, VanillaIds.stone_bricks);
        shapeless(NS, "fuse", ItemIds.fuse, TagIds.plates_steel, ItemIds.plate_polymer, TagIds.wires_tungsten);
        shapeless(NS, "fusion_heater", ItemIds.fusion_heater, ItemIds.fusion_hatch);
        shapeless(NS, "gneiss_chiseled", ItemIds.gneiss_chiseled, ItemIds.gneiss_tile);
        shapeless(NS, "gunpowder", VanillaIds.gunpowder, 3, TagIds.dusts_sulfur, TagIds.dusts_niter, TagIds.gems_coal);
        shapeless(NS, "gunpowder_1", VanillaIds.gunpowder, 3,
                TagIds.dusts_sulfur, TagIds.dusts_niter, VanillaIds.charcoal);
        shapeless(NS, "gunpowder_2", VanillaIds.gunpowder, 3,
                TagIds.dusts_sulfur, TagIds.dusts_niter, TagIds.gems_coal);
        shapeless(NS, "gunpowder_3", VanillaIds.gunpowder, 3,
                TagIds.dusts_sulfur, TagIds.dusts_niter, VanillaIds.charcoal);
        shapeless(NS, "ingot_actinium_3", ItemIds.ingot_actinium, 6, ItemIds.billet_actinium);
        shapeless(NS, "ingot_am241_1", ItemIds.ingot_am241, 6, ItemIds.billet_am241);
        shapeless(NS, "ingot_am242_1", ItemIds.ingot_am242, 6, ItemIds.billet_am242);
        shapeless(NS, "ingot_am_mix_1", ItemIds.ingot_am_mix, 6, ItemIds.billet_am_mix);
        shapeless(NS, "ingot_am_mix_2", ItemIds.ingot_am_mix,
                TagIds.nuggets_am241, TagIds.nuggets_am241, TagIds.nuggets_am241, TagIds.nuggets_am242,
                TagIds.nuggets_am242, TagIds.nuggets_am242, TagIds.nuggets_am242, TagIds.nuggets_am242,
                TagIds.nuggets_am242);
        shapeless(NS, "ingot_au198_2", ItemIds.ingot_au198, 6, ItemIds.billet_au198);
        shapeless(NS, "ingot_australium_1", ItemIds.ingot_australium, 6, ItemIds.billet_australium);
        shapeless(NS, "ingot_beryllium_3", ItemIds.ingot_beryllium, 6, ItemIds.billet_beryllium);
        shapeless(NS, "ingot_bismuth_3", ItemIds.ingot_bismuth, 6, ItemIds.billet_bismuth);
        shapeless(NS, "ingot_co60_1", ItemIds.ingot_co60, 6, ItemIds.billet_co60);
        shapeless(NS, "ingot_cobalt_4", ItemIds.ingot_cobalt, 6, ItemIds.billet_cobalt);
        shapeless(NS, "ingot_firebrick", ItemIds.ingot_firebrick, 4, ItemIds.brick_fire);
        shapeless(NS, "ingot_gh336_1", ItemIds.ingot_gh336, 6, ItemIds.billet_gh336);
        shapeless(NS, "ingot_hes", ItemIds.ingot_hes, 6, ItemIds.billet_hes);
        shapeless(NS, "ingot_hes_2", ItemIds.ingot_hes,
                TagIds.nuggets_schrabidium, TagIds.nuggets_schrabidium, TagIds.nuggets_schrabidium,
                TagIds.nuggets_schrabidium, TagIds.nuggets_schrabidium, TagIds.nuggets_neptunium,
                TagIds.nuggets_neptunium, ItemIds.nugget_beryllium, ItemIds.nugget_beryllium);
        shapeless(NS, "ingot_les", ItemIds.ingot_les, 6, ItemIds.billet_les);
        shapeless(NS, "ingot_les_2", ItemIds.ingot_les,
                TagIds.nuggets_schrabidium, TagIds.nuggets_neptunium, TagIds.nuggets_neptunium, TagIds.nuggets_neptunium,
                TagIds.nuggets_neptunium, ItemIds.nugget_beryllium, ItemIds.nugget_beryllium, ItemIds.nugget_beryllium,
                ItemIds.nugget_beryllium);
        shapeless(NS, "ingot_mox_fuel", ItemIds.ingot_mox_fuel, 6, ItemIds.billet_mox_fuel);
        shapeless(NS, "ingot_neptunium_1", ItemIds.ingot_neptunium, 6, ItemIds.billet_neptunium);
        shapeless(NS, "ingot_neptunium_fuel_1", ItemIds.ingot_neptunium_fuel, 6, ItemIds.billet_neptunium_fuel);
        shapeless(NS, "ingot_pb209_1", ItemIds.ingot_pb209, 6, ItemIds.billet_pb209);
        shapeless(NS, "ingot_plutonium_4", ItemIds.ingot_plutonium, 6, ItemIds.billet_plutonium);
        shapeless(NS, "ingot_plutonium_fuel", ItemIds.ingot_plutonium_fuel, 6, ItemIds.billet_plutonium_fuel);
        shapeless(NS, "ingot_polonium_1", ItemIds.ingot_polonium, 6, ItemIds.billet_polonium);
        shapeless(NS, "ingot_pu238", ItemIds.ingot_pu238, 6, ItemIds.billet_pu238);
        shapeless(NS, "ingot_pu239", ItemIds.ingot_pu239, 6, ItemIds.billet_pu239);
        shapeless(NS, "ingot_pu240", ItemIds.ingot_pu240, 6, ItemIds.billet_pu240);
        shapeless(NS, "ingot_pu241_1", ItemIds.ingot_pu241, 6, ItemIds.billet_pu241);
        shapeless(NS, "ingot_pu_mix_2", ItemIds.ingot_pu_mix, 6, ItemIds.billet_pu_mix);
    }

    private static void shapeless2() {
        shapeless(NS, "ingot_ra226_3", ItemIds.ingot_ra226, 6, ItemIds.billet_ra226);
        shapeless(NS, "ingot_schrabidium_6", ItemIds.ingot_schrabidium, 6, ItemIds.billet_schrabidium);
        shapeless(NS, "ingot_schrabidium_fuel", ItemIds.ingot_schrabidium_fuel, 6, ItemIds.billet_schrabidium_fuel);
        shapeless(NS, "ingot_schrabidium_fuel_3", ItemIds.ingot_schrabidium_fuel,
                TagIds.nuggets_schrabidium, TagIds.nuggets_schrabidium, TagIds.nuggets_schrabidium,
                TagIds.nuggets_neptunium, TagIds.nuggets_neptunium, TagIds.nuggets_neptunium, ItemIds.nugget_beryllium,
                ItemIds.nugget_beryllium, ItemIds.nugget_beryllium);
        shapeless(NS, "ingot_silicon_1", ItemIds.ingot_silicon, 6, ItemIds.billet_silicon);
        shapeless(NS, "ingot_solinium", ItemIds.ingot_solinium, 6, ItemIds.billet_solinium);
        shapeless(NS, "ingot_sr90_2", ItemIds.ingot_sr90, 6, ItemIds.billet_sr90);
        shapeless(NS, "ingot_steel_dusted_10", ItemIds.ingot_steel_dusted, TagIds.ingots_steel, TagIds.dusts_coal);
        shapeless(NS, "ingot_technetium_1", ItemIds.ingot_technetium, 6, ItemIds.billet_technetium);
        shapeless(NS, "ingot_th232_3", ItemIds.ingot_th232, 6, ItemIds.billet_th232);
        shapeless(NS, "ingot_thorium_fuel", ItemIds.ingot_thorium_fuel, 6, ItemIds.billet_thorium_fuel);
        shapeless(NS, "ingot_u233", ItemIds.ingot_u233, 6, ItemIds.billet_u233);
        shapeless(NS, "ingot_u235", ItemIds.ingot_u235, 6, ItemIds.billet_u235);
        shapeless(NS, "ingot_u238", ItemIds.ingot_u238, 6, ItemIds.billet_u238);
        shapeless(NS, "ingot_uranium_8", ItemIds.ingot_uranium, 6, ItemIds.billet_uranium);
        shapeless(NS, "ingot_uranium_fuel", ItemIds.ingot_uranium_fuel, 6, ItemIds.billet_uranium_fuel);
        shapeless(NS, "ingot_zirconium_3", ItemIds.ingot_zirconium, 6, ItemIds.billet_zirconium);
        shapeless(NS, "launch_code", ItemIds.launch_code,
                ItemIds.launch_code_piece, ItemIds.launch_code_piece, ItemIds.launch_code_piece,
                ItemIds.launch_code_piece, ItemIds.launch_code_piece, ItemIds.launch_code_piece,
                ItemIds.launch_code_piece, ItemIds.launch_code_piece, ItemIds.circuit_advanced);
        shapeless(NS, "lightstone", stack(ItemIds.lightstone, 4).with(ComponentIds.item_variant, 0),
                VanillaIds.stone, VanillaIds.stone, VanillaIds.stone, ItemIds.powder_limestone);
        shapeless(NS, "lightstone_bricks_stairs_from_stairs",
                stack(ItemIds.lightstone, 3).with(ComponentIds.item_variant, 2),
                ItemIds.lightstone_bricks_stairs, ItemIds.lightstone_bricks_stairs, ItemIds.lightstone_bricks_stairs,
                ItemIds.lightstone_bricks_stairs);
        shapeless(NS, "lightstone_tile_stairs_from_stairs",
                stack(ItemIds.lightstone, 3).with(ComponentIds.item_variant, 1),
                ItemIds.lightstone_tile_stairs, ItemIds.lightstone_tile_stairs, ItemIds.lightstone_tile_stairs,
                ItemIds.lightstone_tile_stairs);
        shapeless(NS, "loops", ItemIds.loops, ItemIds.flame_pony, VanillaIds.wheat, VanillaIds.sugar);
        shapeless(NS, "machine_industrial_turbine", ItemIds.machine_industrial_turbine, ItemIds.machine_large_turbine);
        shapeless(NS, "marshmallow", ItemIds.marshmallow, VanillaIds.stick, VanillaIds.sugar, VanillaIds.wheat_seeds);
        shapeless(NS, "med_ipecac", ItemIds.med_ipecac, VanillaIds.glass_bottle, VanillaIds.nether_wart);
        shapeless(NS, "med_ptsd", ItemIds.med_ptsd, ItemIds.med_ipecac);
        shapeless(NS, "missile_emp", ItemIds.missile_emp, ItemIds.missile_assembly, ItemIds.ducttape, ItemIds.emp_bomb);
        shapeless(NS, "nuclear_waste", ItemIds.nuclear_waste, 6, ItemIds.billet_nuclear_waste);
        shapeless(NS, "nugget_australium_greater", ItemIds.nugget_australium_greater, 6,
                ItemIds.billet_australium_greater);
        shapeless(NS, "nugget_australium_lesser", ItemIds.nugget_australium_lesser, 6,
                ItemIds.billet_australium_lesser);
        shapeless(NS, "particle_copper", ItemIds.particle_copper,
                ItemIds.particle_empty, TagIds.dusts_copper, ItemIds.pellet_charged);
        shapeless(NS, "particle_lead", ItemIds.particle_lead,
                ItemIds.particle_empty, TagIds.dusts_lead, ItemIds.pellet_charged);
        shapeless(NS, "pellet_gas", ItemIds.pellet_gas, 2,
                VanillaIds.water_bucket, VanillaIds.glowstone_dust, TagIds.plates_steel);
        shapeless(NS, "pink_planks", ItemIds.pink_planks, 3,
                ItemIds.pink_stairs, ItemIds.pink_stairs, ItemIds.pink_stairs, ItemIds.pink_stairs);
        shapeless(NS, "pink_planks_1", ItemIds.pink_planks, ItemIds.pink_slab, ItemIds.pink_slab);
        shapeless(NS, "platemetal_base_from_black", ItemIds.platemetal_base, ItemIds.platemetal_black);
        shapeless(NS, "platemetal_base_from_blue", ItemIds.platemetal_base, ItemIds.platemetal_blue);
        shapeless(NS, "platemetal_base_from_cyan", ItemIds.platemetal_base, ItemIds.platemetal_cyan);
        shapeless(NS, "platemetal_base_from_green", ItemIds.platemetal_base, ItemIds.platemetal_green);
        shapeless(NS, "platemetal_base_from_light_blue", ItemIds.platemetal_base, ItemIds.platemetal_light_blue);
        shapeless(NS, "platemetal_base_from_light_gray", ItemIds.platemetal_base, ItemIds.platemetal_light_gray);
        shapeless(NS, "platemetal_base_from_lime", ItemIds.platemetal_base, ItemIds.platemetal_lime);
        shapeless(NS, "platemetal_base_from_magenta", ItemIds.platemetal_base, ItemIds.platemetal_magenta);
        shapeless(NS, "platemetal_base_from_orange", ItemIds.platemetal_base, ItemIds.platemetal_orange);
        shapeless(NS, "platemetal_base_from_pink", ItemIds.platemetal_base, ItemIds.platemetal_pink);
        shapeless(NS, "platemetal_base_from_purple", ItemIds.platemetal_base, ItemIds.platemetal_purple);
        shapeless(NS, "platemetal_base_from_red", ItemIds.platemetal_base, ItemIds.platemetal_red);
        shapeless(NS, "platemetal_base_from_white", ItemIds.platemetal_base, ItemIds.platemetal_white);
        shapeless(NS, "platemetal_base_from_yellow", ItemIds.platemetal_base, ItemIds.platemetal_yellow);
        shapeless(NS, "powder_cement", ItemIds.powder_cement, 4,
                TagIds.dusts_limestone, VanillaIds.clay_ball, VanillaIds.clay_ball, VanillaIds.clay_ball);
        shapeless(NS, "powder_desh_ready", ItemIds.powder_desh_ready,
                ItemIds.powder_desh_mix, ItemIds.nugget_mercury, ItemIds.nugget_mercury, TagIds.dusts_coal);
        shapeless(NS, "powder_flux", ItemIds.powder_flux, VanillaIds.charcoal, TagIds.minecraft_sand);
        shapeless(NS, "powder_flux_1", ItemIds.powder_flux, 2, TagIds.dusts_coal, TagIds.minecraft_sand);
        shapeless(NS, "powder_flux_2", ItemIds.powder_flux, 4, TagIds.dusts_fluorite, TagIds.minecraft_sand);
        shapeless(NS, "powder_flux_3", ItemIds.powder_flux, 8,
                TagIds.dusts_lead, TagIds.dusts_sulfur, TagIds.minecraft_sand);
        shapeless(NS, "powder_flux_4", ItemIds.powder_flux, 12, TagIds.dusts_limestone, TagIds.minecraft_sand);
        shapeless(NS, "powder_flux_5", ItemIds.powder_flux, 12, TagIds.dusts_calcium, TagIds.minecraft_sand);
        shapeless(NS, "powder_flux_6", ItemIds.powder_flux, 16, TagIds.dusts_borax, TagIds.minecraft_sand);
        shapeless(NS, "powder_ice", ItemIds.powder_ice, 4,
                VanillaIds.snowball, TagIds.dusts_niter, TagIds.dusts_redstone);
        shapeless(NS, "powder_meteorite_1", ItemIds.powder_meteorite, 4,
                TagIds.dusts_iron, TagIds.dusts_copper, TagIds.dusts_lithium, TagIds.dusts_quartz);
        shapeless(NS, "powder_poison", ItemIds.powder_poison, 4,
                VanillaIds.spider_eye, TagIds.dusts_redstone, TagIds.gems_quartz);
        shapeless(NS, "powder_power", ItemIds.powder_power, 3,
                VanillaIds.glowstone_dust, TagIds.dusts_diamond, TagIds.dusts_magnetized_tungsten);
        shapeless(NS, "powder_semtex_mix", ItemIds.powder_semtex_mix, 3,
                ItemIds.solid_fuel, ItemIds.cordite, TagIds.dusts_niter);
        shapeless(NS, "powder_semtex_mix_1", ItemIds.powder_semtex_mix,
                ItemIds.solid_fuel, ItemIds.ballistite, TagIds.dusts_niter);
        shapeless(NS, "powder_spark_mix", ItemIds.powder_spark_mix, 3,
                TagIds.dusts_desh, TagIds.dusts_euphemium, ItemIds.powder_power);
        shapeless(NS, "powder_thermite", ItemIds.powder_thermite, 4,
                TagIds.dusts_iron, TagIds.dusts_iron, TagIds.dusts_iron, TagIds.dusts_aluminium);
        shapeless(NS, "radx", ItemIds.radx, TagIds.dusts_coal, TagIds.dusts_coal, TagIds.dusts_fluorite);
        shapeless(NS, "rbmk_fuel_balefire", ItemIds.rbmk_fuel_balefire,
                ItemIds.rbmk_fuel_empty, ItemIds.egg_balefire_shard, ItemIds.egg_balefire_shard,
                ItemIds.egg_balefire_shard, ItemIds.egg_balefire_shard, ItemIds.egg_balefire_shard,
                ItemIds.egg_balefire_shard, ItemIds.egg_balefire_shard, ItemIds.egg_balefire_shard);
        shapeless(NS, "rbmk_fuel_balefire_gold", ItemIds.rbmk_fuel_balefire_gold,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_balefire_gold, ItemIds.billet_balefire_gold,
                ItemIds.billet_balefire_gold, ItemIds.billet_balefire_gold, ItemIds.billet_balefire_gold,
                ItemIds.billet_balefire_gold, ItemIds.billet_balefire_gold, ItemIds.billet_balefire_gold);
        shapeless(NS, "rbmk_fuel_drx", ItemIds.rbmk_fuel_drx, ItemIds.rbmk_fuel_balefire, ItemIds.particle_digamma);
        shapeless(NS, "rbmk_fuel_flashlead", ItemIds.rbmk_fuel_flashlead,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_flashlead, ItemIds.billet_flashlead, ItemIds.billet_flashlead,
                ItemIds.billet_flashlead, ItemIds.billet_flashlead, ItemIds.billet_flashlead, ItemIds.billet_flashlead,
                ItemIds.billet_flashlead);
        shapeless(NS, "rbmk_fuel_hea241", ItemIds.rbmk_fuel_hea241,
                ItemIds.rbmk_fuel_empty, TagIds.billets_am241, TagIds.billets_am241, TagIds.billets_am241,
                TagIds.billets_am241, TagIds.billets_am241, TagIds.billets_am241, TagIds.billets_am241,
                TagIds.billets_am241);
        shapeless(NS, "rbmk_fuel_hea242", ItemIds.rbmk_fuel_hea242,
                ItemIds.rbmk_fuel_empty, TagIds.billets_am242, TagIds.billets_am242, TagIds.billets_am242,
                TagIds.billets_am242, TagIds.billets_am242, TagIds.billets_am242, TagIds.billets_am242,
                TagIds.billets_am242);
        shapeless(NS, "rbmk_fuel_heaus", ItemIds.rbmk_fuel_heaus,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_australium_greater, ItemIds.billet_australium_greater,
                ItemIds.billet_australium_greater, ItemIds.billet_australium_greater, ItemIds.billet_australium_greater,
                ItemIds.billet_australium_greater, ItemIds.billet_australium_greater, ItemIds.billet_australium_greater);
        shapeless(NS, "rbmk_fuel_hen", ItemIds.rbmk_fuel_hen,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_neptunium, ItemIds.billet_neptunium, ItemIds.billet_neptunium,
                ItemIds.billet_neptunium, ItemIds.billet_neptunium, ItemIds.billet_neptunium, ItemIds.billet_neptunium,
                ItemIds.billet_neptunium);
        shapeless(NS, "rbmk_fuel_hep", ItemIds.rbmk_fuel_hep,
                ItemIds.rbmk_fuel_empty, TagIds.billets_pu239, TagIds.billets_pu239, TagIds.billets_pu239,
                TagIds.billets_pu239, TagIds.billets_pu239, TagIds.billets_pu239, TagIds.billets_pu239,
                TagIds.billets_pu239);
        shapeless(NS, "rbmk_fuel_hep241", ItemIds.rbmk_fuel_hep241,
                ItemIds.rbmk_fuel_empty, TagIds.billets_pu241, TagIds.billets_pu241, TagIds.billets_pu241,
                TagIds.billets_pu241, TagIds.billets_pu241, TagIds.billets_pu241, TagIds.billets_pu241,
                TagIds.billets_pu241);
        shapeless(NS, "rbmk_fuel_hes", ItemIds.rbmk_fuel_hes,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_hes, ItemIds.billet_hes, ItemIds.billet_hes, ItemIds.billet_hes,
                ItemIds.billet_hes, ItemIds.billet_hes, ItemIds.billet_hes, ItemIds.billet_hes);
        shapeless(NS, "rbmk_fuel_heu233", ItemIds.rbmk_fuel_heu233,
                ItemIds.rbmk_fuel_empty, TagIds.billets_u233, TagIds.billets_u233, TagIds.billets_u233,
                TagIds.billets_u233, TagIds.billets_u233, TagIds.billets_u233, TagIds.billets_u233, TagIds.billets_u233);
        shapeless(NS, "rbmk_fuel_heu235", ItemIds.rbmk_fuel_heu235,
                ItemIds.rbmk_fuel_empty, TagIds.billets_u235, TagIds.billets_u235, TagIds.billets_u235,
                TagIds.billets_u235, TagIds.billets_u235, TagIds.billets_u235, TagIds.billets_u235, TagIds.billets_u235);
        shapeless(NS, "rbmk_fuel_lea", ItemIds.rbmk_fuel_lea,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_americium_fuel, ItemIds.billet_americium_fuel,
                ItemIds.billet_americium_fuel, ItemIds.billet_americium_fuel, ItemIds.billet_americium_fuel,
                ItemIds.billet_americium_fuel, ItemIds.billet_americium_fuel, ItemIds.billet_americium_fuel);
        shapeless(NS, "rbmk_fuel_leaus", ItemIds.rbmk_fuel_leaus,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_australium_lesser, ItemIds.billet_australium_lesser,
                ItemIds.billet_australium_lesser, ItemIds.billet_australium_lesser, ItemIds.billet_australium_lesser,
                ItemIds.billet_australium_lesser, ItemIds.billet_australium_lesser, ItemIds.billet_australium_lesser);
        shapeless(NS, "rbmk_fuel_lep", ItemIds.rbmk_fuel_lep,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_plutonium_fuel, ItemIds.billet_plutonium_fuel,
                ItemIds.billet_plutonium_fuel, ItemIds.billet_plutonium_fuel, ItemIds.billet_plutonium_fuel,
                ItemIds.billet_plutonium_fuel, ItemIds.billet_plutonium_fuel, ItemIds.billet_plutonium_fuel);
        shapeless(NS, "rbmk_fuel_les", ItemIds.rbmk_fuel_les,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_les, ItemIds.billet_les, ItemIds.billet_les, ItemIds.billet_les,
                ItemIds.billet_les, ItemIds.billet_les, ItemIds.billet_les, ItemIds.billet_les);
        shapeless(NS, "rbmk_fuel_mea", ItemIds.rbmk_fuel_mea,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_am_mix, ItemIds.billet_am_mix, ItemIds.billet_am_mix,
                ItemIds.billet_am_mix, ItemIds.billet_am_mix, ItemIds.billet_am_mix, ItemIds.billet_am_mix,
                ItemIds.billet_am_mix);
        shapeless(NS, "rbmk_fuel_men", ItemIds.rbmk_fuel_men,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_neptunium_fuel, ItemIds.billet_neptunium_fuel,
                ItemIds.billet_neptunium_fuel, ItemIds.billet_neptunium_fuel, ItemIds.billet_neptunium_fuel,
                ItemIds.billet_neptunium_fuel, ItemIds.billet_neptunium_fuel, ItemIds.billet_neptunium_fuel);
        shapeless(NS, "rbmk_fuel_mep", ItemIds.rbmk_fuel_mep,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_pu_mix, ItemIds.billet_pu_mix, ItemIds.billet_pu_mix,
                ItemIds.billet_pu_mix, ItemIds.billet_pu_mix, ItemIds.billet_pu_mix, ItemIds.billet_pu_mix,
                ItemIds.billet_pu_mix);
        shapeless(NS, "rbmk_fuel_mes", ItemIds.rbmk_fuel_mes,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_schrabidium_fuel, ItemIds.billet_schrabidium_fuel,
                ItemIds.billet_schrabidium_fuel, ItemIds.billet_schrabidium_fuel, ItemIds.billet_schrabidium_fuel,
                ItemIds.billet_schrabidium_fuel, ItemIds.billet_schrabidium_fuel, ItemIds.billet_schrabidium_fuel);
        shapeless(NS, "rbmk_fuel_meu", ItemIds.rbmk_fuel_meu,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_uranium_fuel, ItemIds.billet_uranium_fuel,
                ItemIds.billet_uranium_fuel, ItemIds.billet_uranium_fuel, ItemIds.billet_uranium_fuel,
                ItemIds.billet_uranium_fuel, ItemIds.billet_uranium_fuel, ItemIds.billet_uranium_fuel);
        shapeless(NS, "rbmk_fuel_mox", ItemIds.rbmk_fuel_mox,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_mox_fuel, ItemIds.billet_mox_fuel, ItemIds.billet_mox_fuel,
                ItemIds.billet_mox_fuel, ItemIds.billet_mox_fuel, ItemIds.billet_mox_fuel, ItemIds.billet_mox_fuel,
                ItemIds.billet_mox_fuel);
        shapeless(NS, "rbmk_fuel_po210be", ItemIds.rbmk_fuel_po210be,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_po210be, ItemIds.billet_po210be, ItemIds.billet_po210be,
                ItemIds.billet_po210be, ItemIds.billet_po210be, ItemIds.billet_po210be, ItemIds.billet_po210be,
                ItemIds.billet_po210be);
        shapeless(NS, "rbmk_fuel_pu238be", ItemIds.rbmk_fuel_pu238be,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_pu238be, ItemIds.billet_pu238be, ItemIds.billet_pu238be,
                ItemIds.billet_pu238be, ItemIds.billet_pu238be, ItemIds.billet_pu238be, ItemIds.billet_pu238be,
                ItemIds.billet_pu238be);
        shapeless(NS, "rbmk_fuel_ra226be", ItemIds.rbmk_fuel_ra226be,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_ra226be, ItemIds.billet_ra226be, ItemIds.billet_ra226be,
                ItemIds.billet_ra226be, ItemIds.billet_ra226be, ItemIds.billet_ra226be, ItemIds.billet_ra226be,
                ItemIds.billet_ra226be);
        shapeless(NS, "rbmk_fuel_thmeu", ItemIds.rbmk_fuel_thmeu,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_thorium_fuel, ItemIds.billet_thorium_fuel,
                ItemIds.billet_thorium_fuel, ItemIds.billet_thorium_fuel, ItemIds.billet_thorium_fuel,
                ItemIds.billet_thorium_fuel, ItemIds.billet_thorium_fuel, ItemIds.billet_thorium_fuel);
        shapeless(NS, "rbmk_fuel_ueu", ItemIds.rbmk_fuel_ueu,
                ItemIds.rbmk_fuel_empty, TagIds.billets_uranium, TagIds.billets_uranium, TagIds.billets_uranium,
                TagIds.billets_uranium, TagIds.billets_uranium, TagIds.billets_uranium, TagIds.billets_uranium,
                TagIds.billets_uranium);
        shapeless(NS, "rbmk_fuel_uzh", ItemIds.rbmk_fuel_uzh,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_uzh, ItemIds.billet_uzh, ItemIds.billet_uzh, ItemIds.billet_uzh,
                ItemIds.billet_uzh, ItemIds.billet_uzh, ItemIds.billet_uzh, ItemIds.billet_uzh);
        shapeless(NS, "rbmk_fuel_zfb_am_mix", ItemIds.rbmk_fuel_zfb_am_mix,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_zfb_am_mix, ItemIds.billet_zfb_am_mix, ItemIds.billet_zfb_am_mix,
                ItemIds.billet_zfb_am_mix, ItemIds.billet_zfb_am_mix, ItemIds.billet_zfb_am_mix,
                ItemIds.billet_zfb_am_mix, ItemIds.billet_zfb_am_mix);
        shapeless(NS, "rbmk_fuel_zfb_bismuth", ItemIds.rbmk_fuel_zfb_bismuth,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_zfb_bismuth, ItemIds.billet_zfb_bismuth,
                ItemIds.billet_zfb_bismuth, ItemIds.billet_zfb_bismuth, ItemIds.billet_zfb_bismuth,
                ItemIds.billet_zfb_bismuth, ItemIds.billet_zfb_bismuth, ItemIds.billet_zfb_bismuth);
        shapeless(NS, "rbmk_fuel_zfb_pu241", ItemIds.rbmk_fuel_zfb_pu241,
                ItemIds.rbmk_fuel_empty, ItemIds.billet_zfb_pu241, ItemIds.billet_zfb_pu241, ItemIds.billet_zfb_pu241,
                ItemIds.billet_zfb_pu241, ItemIds.billet_zfb_pu241, ItemIds.billet_zfb_pu241, ItemIds.billet_zfb_pu241,
                ItemIds.billet_zfb_pu241);
        shapeless(NS, "red_cable_classic", ItemIds.red_cable_classic, ItemIds.red_cable);
        shapeless(NS, "red_cable_from_classic", ItemIds.red_cable, ItemIds.red_cable_classic);
        shapeless(NS, "red_cable_gauge", ItemIds.red_cable_gauge,
                ItemIds.red_cable, TagIds.ingots_steel, ItemIds.circuit_basic);
        shapeless(NS, "red_pylon_medium_steel_transformer", ItemIds.red_pylon_medium_steel_transformer,
                ItemIds.red_pylon_medium_steel, ItemIds.plate_polymer, ItemIds.coil_copper);
        shapeless(NS, "red_pylon_medium_wood_transformer", ItemIds.red_pylon_medium_wood_transformer,
                ItemIds.red_pylon_medium_wood, ItemIds.plate_polymer, ItemIds.coil_copper);
        shapeless(NS, "reinforced_brick", ItemIds.reinforced_brick, 3,
                ItemIds.reinforced_brick_stairs, ItemIds.reinforced_brick_stairs, ItemIds.reinforced_brick_stairs,
                ItemIds.reinforced_brick_stairs);
        shapeless(NS, "reinforced_brick_1", ItemIds.reinforced_brick,
                ItemIds.reinforced_brick_slab, ItemIds.reinforced_brick_slab);
        shapeless(NS, "reinforced_sand", ItemIds.reinforced_sand, 3,
                ItemIds.reinforced_sand_stairs, ItemIds.reinforced_sand_stairs, ItemIds.reinforced_sand_stairs,
                ItemIds.reinforced_sand_stairs);
        shapeless(NS, "reinforced_sand_1", ItemIds.reinforced_sand,
                ItemIds.reinforced_sand_slab, ItemIds.reinforced_sand_slab);
        shapeless(NS, "reinforced_stone", ItemIds.reinforced_stone, 3,
                ItemIds.reinforced_stone_stairs, ItemIds.reinforced_stone_stairs, ItemIds.reinforced_stone_stairs,
                ItemIds.reinforced_stone_stairs);
        shapeless(NS, "reinforced_stone_1", ItemIds.reinforced_stone,
                ItemIds.reinforced_stone_slab, ItemIds.reinforced_stone_slab);
    }

    private static void shapeless3() {
        shapeless(NS, "rod_ac227", ItemIds.rod_ac227, ItemIds.rod_empty, TagIds.billets_actinium);
        shapeless(NS, "rod_ac227_unload", ItemIds.billet_actinium, ItemIds.rod_ac227);
        shapeless(NS, "rod_co", ItemIds.rod_co, ItemIds.rod_empty, TagIds.billets_cobalt);
        shapeless(NS, "rod_co60", ItemIds.rod_co60, ItemIds.rod_empty, TagIds.billets_co60);
        shapeless(NS, "rod_co60_unload", ItemIds.billet_co60, ItemIds.rod_co60);
        shapeless(NS, "rod_co_unload", ItemIds.billet_cobalt, ItemIds.rod_co);
        shapeless(NS, "rod_dual_ac227", ItemIds.rod_dual_ac227,
                ItemIds.rod_dual_empty, TagIds.billets_actinium, TagIds.billets_actinium);
        shapeless(NS, "rod_dual_ac227_unload", ItemIds.billet_actinium, 2, ItemIds.rod_dual_ac227);
        shapeless(NS, "rod_dual_co", ItemIds.rod_dual_co,
                ItemIds.rod_dual_empty, TagIds.billets_cobalt, TagIds.billets_cobalt);
        shapeless(NS, "rod_dual_co60", ItemIds.rod_dual_co60,
                ItemIds.rod_dual_empty, TagIds.billets_co60, TagIds.billets_co60);
        shapeless(NS, "rod_dual_co60_unload", ItemIds.billet_co60, 2, ItemIds.rod_dual_co60);
        shapeless(NS, "rod_dual_co_unload", ItemIds.billet_cobalt, 2, ItemIds.rod_dual_co);
        shapeless(NS, "rod_dual_empty", ItemIds.rod_dual_empty, ItemIds.rod_empty, ItemIds.rod_empty);
        shapeless(NS, "rod_dual_lead", ItemIds.rod_dual_lead,
                ItemIds.rod_dual_empty, TagIds.ingots_lead, TagIds.nuggets_lead, TagIds.nuggets_lead,
                TagIds.nuggets_lead);
        shapeless(NS, "rod_dual_lead_unload", ItemIds.nugget_lead, 12, ItemIds.rod_dual_lead);
        shapeless(NS, "rod_dual_lithium", ItemIds.rod_dual_lithium,
                ItemIds.rod_dual_empty, TagIds.ingots_lithium, TagIds.ingots_lithium);
        shapeless(NS, "rod_dual_lithium_unload", ItemIds.lithium, 2, ItemIds.rod_dual_lithium);
        shapeless(NS, "rod_dual_np237", ItemIds.rod_dual_np237,
                ItemIds.rod_dual_empty, TagIds.billets_neptunium, TagIds.billets_neptunium);
        shapeless(NS, "rod_dual_np237_unload", ItemIds.billet_neptunium, 2, ItemIds.rod_dual_np237);
        shapeless(NS, "rod_dual_pu238", ItemIds.rod_dual_pu238,
                ItemIds.rod_dual_empty, TagIds.billets_pu238, TagIds.billets_pu238);
        shapeless(NS, "rod_dual_pu238_unload", ItemIds.billet_pu238, 2, ItemIds.rod_dual_pu238);
        shapeless(NS, "rod_dual_pu239", ItemIds.rod_dual_pu239,
                ItemIds.rod_dual_empty, TagIds.billets_pu239, TagIds.billets_pu239);
        shapeless(NS, "rod_dual_pu239_unload", ItemIds.billet_pu239, 2, ItemIds.rod_dual_pu239);
        shapeless(NS, "rod_dual_ra226", ItemIds.rod_dual_ra226,
                ItemIds.rod_dual_empty, TagIds.billets_ra226, TagIds.billets_ra226);
        shapeless(NS, "rod_dual_ra226_unload", ItemIds.billet_ra226, 2, ItemIds.rod_dual_ra226);
        shapeless(NS, "rod_dual_rgp", ItemIds.rod_dual_rgp,
                ItemIds.rod_dual_empty, ItemIds.billet_pu_mix, ItemIds.billet_pu_mix);
        shapeless(NS, "rod_dual_rgp_unload", ItemIds.billet_pu_mix, 2, ItemIds.rod_dual_rgp);
        shapeless(NS, "rod_dual_th232", ItemIds.rod_dual_th232,
                ItemIds.rod_dual_empty, TagIds.billets_thorium, TagIds.billets_thorium);
        shapeless(NS, "rod_dual_th232_unload", ItemIds.billet_th232, 2, ItemIds.rod_dual_th232);
        shapeless(NS, "rod_dual_thf", ItemIds.rod_dual_thf,
                ItemIds.rod_dual_empty, ItemIds.billet_thorium_fuel, ItemIds.billet_thorium_fuel);
        shapeless(NS, "rod_dual_thf_unload", ItemIds.billet_thorium_fuel, 2, ItemIds.rod_dual_thf);
        shapeless(NS, "rod_dual_u235", ItemIds.rod_dual_u235,
                ItemIds.rod_dual_empty, TagIds.billets_u235, TagIds.billets_u235);
        shapeless(NS, "rod_dual_u235_unload", ItemIds.billet_u235, 2, ItemIds.rod_dual_u235);
        shapeless(NS, "rod_dual_u238", ItemIds.rod_dual_u238,
                ItemIds.rod_dual_empty, TagIds.billets_u238, TagIds.billets_u238);
        shapeless(NS, "rod_dual_u238_unload", ItemIds.billet_u238, 2, ItemIds.rod_dual_u238);
        shapeless(NS, "rod_dual_uranium", ItemIds.rod_dual_uranium,
                ItemIds.rod_dual_empty, TagIds.billets_uranium, TagIds.billets_uranium);
        shapeless(NS, "rod_dual_uranium_unload", ItemIds.billet_uranium, 2, ItemIds.rod_dual_uranium);
        shapeless(NS, "rod_dual_waste", ItemIds.rod_dual_waste,
                ItemIds.rod_dual_empty, ItemIds.billet_nuclear_waste, ItemIds.billet_nuclear_waste);
        shapeless(NS, "rod_dual_waste_unload", ItemIds.billet_nuclear_waste, 2, ItemIds.rod_dual_waste);
        shapeless(NS, "rod_empty_1", ItemIds.rod_empty, 2, ItemIds.rod_dual_empty);
        shapeless(NS, "rod_empty_2", ItemIds.rod_empty, 4, ItemIds.rod_quad_empty);
        shapeless(NS, "rod_lead", ItemIds.rod_lead,
                ItemIds.rod_empty, TagIds.nuggets_lead, TagIds.nuggets_lead, TagIds.nuggets_lead, TagIds.nuggets_lead,
                TagIds.nuggets_lead, TagIds.nuggets_lead);
        shapeless(NS, "rod_lead_unload", ItemIds.nugget_lead, 6, ItemIds.rod_lead);
        shapeless(NS, "rod_lithium", ItemIds.rod_lithium, ItemIds.rod_empty, TagIds.ingots_lithium);
        shapeless(NS, "rod_lithium_unload", ItemIds.lithium, ItemIds.rod_lithium);
        shapeless(NS, "rod_np237", ItemIds.rod_np237, ItemIds.rod_empty, TagIds.billets_neptunium);
        shapeless(NS, "rod_np237_unload", ItemIds.billet_neptunium, ItemIds.rod_np237);
        shapeless(NS, "rod_pu238", ItemIds.rod_pu238, ItemIds.rod_empty, TagIds.billets_pu238);
        shapeless(NS, "rod_pu238_unload", ItemIds.billet_pu238, ItemIds.rod_pu238);
        shapeless(NS, "rod_pu239", ItemIds.rod_pu239, ItemIds.rod_empty, TagIds.billets_pu239);
        shapeless(NS, "rod_pu239_unload", ItemIds.billet_pu239, ItemIds.rod_pu239);
        shapeless(NS, "rod_quad_ac227", ItemIds.rod_quad_ac227,
                ItemIds.rod_quad_empty, TagIds.billets_actinium, TagIds.billets_actinium, TagIds.billets_actinium,
                TagIds.billets_actinium);
        shapeless(NS, "rod_quad_ac227_unload", ItemIds.billet_actinium, 4, ItemIds.rod_quad_ac227);
        shapeless(NS, "rod_quad_co", ItemIds.rod_quad_co,
                ItemIds.rod_quad_empty, TagIds.billets_cobalt, TagIds.billets_cobalt, TagIds.billets_cobalt,
                TagIds.billets_cobalt);
        shapeless(NS, "rod_quad_co60", ItemIds.rod_quad_co60,
                ItemIds.rod_quad_empty, TagIds.billets_co60, TagIds.billets_co60, TagIds.billets_co60,
                TagIds.billets_co60);
        shapeless(NS, "rod_quad_co60_unload", ItemIds.billet_co60, 4, ItemIds.rod_quad_co60);
        shapeless(NS, "rod_quad_co_unload", ItemIds.billet_cobalt, 4, ItemIds.rod_quad_co);
        shapeless(NS, "rod_quad_empty", ItemIds.rod_quad_empty,
                ItemIds.rod_empty, ItemIds.rod_empty, ItemIds.rod_empty, ItemIds.rod_empty);
        shapeless(NS, "rod_quad_empty_1", ItemIds.rod_quad_empty, ItemIds.rod_dual_empty, ItemIds.rod_dual_empty);
        shapeless(NS, "rod_quad_lead", ItemIds.rod_quad_lead,
                ItemIds.rod_quad_empty, TagIds.ingots_lead, TagIds.ingots_lead, TagIds.nuggets_lead, TagIds.nuggets_lead,
                TagIds.nuggets_lead, TagIds.nuggets_lead, TagIds.nuggets_lead, TagIds.nuggets_lead);
        shapeless(NS, "rod_quad_lead_unload", ItemIds.nugget_lead, 24, ItemIds.rod_quad_lead);
        shapeless(NS, "rod_quad_lithium", ItemIds.rod_quad_lithium,
                ItemIds.rod_quad_empty, TagIds.ingots_lithium, TagIds.ingots_lithium, TagIds.ingots_lithium,
                TagIds.ingots_lithium);
        shapeless(NS, "rod_quad_lithium_unload", ItemIds.lithium, 4, ItemIds.rod_quad_lithium);
        shapeless(NS, "rod_quad_np237", ItemIds.rod_quad_np237,
                ItemIds.rod_quad_empty, TagIds.billets_neptunium, TagIds.billets_neptunium, TagIds.billets_neptunium,
                TagIds.billets_neptunium);
        shapeless(NS, "rod_quad_np237_unload", ItemIds.billet_neptunium, 4, ItemIds.rod_quad_np237);
        shapeless(NS, "rod_quad_pu238", ItemIds.rod_quad_pu238,
                ItemIds.rod_quad_empty, TagIds.billets_pu238, TagIds.billets_pu238, TagIds.billets_pu238,
                TagIds.billets_pu238);
        shapeless(NS, "rod_quad_pu238_unload", ItemIds.billet_pu238, 4, ItemIds.rod_quad_pu238);
        shapeless(NS, "rod_quad_pu239", ItemIds.rod_quad_pu239,
                ItemIds.rod_quad_empty, TagIds.billets_pu239, TagIds.billets_pu239, TagIds.billets_pu239,
                TagIds.billets_pu239);
        shapeless(NS, "rod_quad_pu239_unload", ItemIds.billet_pu239, 4, ItemIds.rod_quad_pu239);
        shapeless(NS, "rod_quad_ra226", ItemIds.rod_quad_ra226,
                ItemIds.rod_quad_empty, TagIds.billets_ra226, TagIds.billets_ra226, TagIds.billets_ra226,
                TagIds.billets_ra226);
        shapeless(NS, "rod_quad_ra226_unload", ItemIds.billet_ra226, 4, ItemIds.rod_quad_ra226);
        shapeless(NS, "rod_quad_rgp", ItemIds.rod_quad_rgp,
                ItemIds.rod_quad_empty, ItemIds.billet_pu_mix, ItemIds.billet_pu_mix, ItemIds.billet_pu_mix,
                ItemIds.billet_pu_mix);
        shapeless(NS, "rod_quad_rgp_unload", ItemIds.billet_pu_mix, 4, ItemIds.rod_quad_rgp);
        shapeless(NS, "rod_quad_th232", ItemIds.rod_quad_th232,
                ItemIds.rod_quad_empty, TagIds.billets_thorium, TagIds.billets_thorium, TagIds.billets_thorium,
                TagIds.billets_thorium);
        shapeless(NS, "rod_quad_th232_unload", ItemIds.billet_th232, 4, ItemIds.rod_quad_th232);
        shapeless(NS, "rod_quad_thf", ItemIds.rod_quad_thf,
                ItemIds.rod_quad_empty, ItemIds.billet_thorium_fuel, ItemIds.billet_thorium_fuel,
                ItemIds.billet_thorium_fuel, ItemIds.billet_thorium_fuel);
        shapeless(NS, "rod_quad_thf_unload", ItemIds.billet_thorium_fuel, 4, ItemIds.rod_quad_thf);
        shapeless(NS, "rod_quad_u235", ItemIds.rod_quad_u235,
                ItemIds.rod_quad_empty, TagIds.billets_u235, TagIds.billets_u235, TagIds.billets_u235,
                TagIds.billets_u235);
        shapeless(NS, "rod_quad_u235_unload", ItemIds.billet_u235, 4, ItemIds.rod_quad_u235);
        shapeless(NS, "rod_quad_u238", ItemIds.rod_quad_u238,
                ItemIds.rod_quad_empty, TagIds.billets_u238, TagIds.billets_u238, TagIds.billets_u238,
                TagIds.billets_u238);
        shapeless(NS, "rod_quad_u238_unload", ItemIds.billet_u238, 4, ItemIds.rod_quad_u238);
        shapeless(NS, "rod_quad_uranium", ItemIds.rod_quad_uranium,
                ItemIds.rod_quad_empty, TagIds.billets_uranium, TagIds.billets_uranium, TagIds.billets_uranium,
                TagIds.billets_uranium);
        shapeless(NS, "rod_quad_uranium_unload", ItemIds.billet_uranium, 4, ItemIds.rod_quad_uranium);
        shapeless(NS, "rod_quad_waste", ItemIds.rod_quad_waste,
                ItemIds.rod_quad_empty, ItemIds.billet_nuclear_waste, ItemIds.billet_nuclear_waste,
                ItemIds.billet_nuclear_waste, ItemIds.billet_nuclear_waste);
        shapeless(NS, "rod_quad_waste_unload", ItemIds.billet_nuclear_waste, 4, ItemIds.rod_quad_waste);
        shapeless(NS, "rod_ra226", ItemIds.rod_ra226, ItemIds.rod_empty, TagIds.billets_ra226);
        shapeless(NS, "rod_ra226_unload", ItemIds.billet_ra226, ItemIds.rod_ra226);
        shapeless(NS, "rod_rgp", ItemIds.rod_rgp, ItemIds.rod_empty, ItemIds.billet_pu_mix);
        shapeless(NS, "rod_rgp_unload", ItemIds.billet_pu_mix, ItemIds.rod_rgp);
        shapeless(NS, "rod_th232", ItemIds.rod_th232, ItemIds.rod_empty, TagIds.billets_thorium);
        shapeless(NS, "rod_th232_unload", ItemIds.billet_th232, ItemIds.rod_th232);
        shapeless(NS, "rod_thf", ItemIds.rod_thf, ItemIds.rod_empty, ItemIds.billet_thorium_fuel);
        shapeless(NS, "rod_thf_unload", ItemIds.billet_thorium_fuel, ItemIds.rod_thf);
        shapeless(NS, "rod_u235", ItemIds.rod_u235, ItemIds.rod_empty, TagIds.billets_u235);
        shapeless(NS, "rod_u235_unload", ItemIds.billet_u235, ItemIds.rod_u235);
        shapeless(NS, "rod_u238", ItemIds.rod_u238, ItemIds.rod_empty, TagIds.billets_u238);
        shapeless(NS, "rod_u238_unload", ItemIds.billet_u238, ItemIds.rod_u238);
        shapeless(NS, "rod_uranium", ItemIds.rod_uranium, ItemIds.rod_empty, TagIds.billets_uranium);
        shapeless(NS, "rod_uranium_unload", ItemIds.billet_uranium, ItemIds.rod_uranium);
        shapeless(NS, "rod_waste", ItemIds.rod_waste, ItemIds.rod_empty, ItemIds.billet_nuclear_waste);
        shapeless(NS, "rod_waste_unload", ItemIds.billet_nuclear_waste, ItemIds.rod_waste);
        shapeless(NS, "rtg_fuel_actinium", ItemIds.pellet_rtg_actinium,
                ItemIds.billet_actinium, ItemIds.billet_actinium, ItemIds.billet_actinium, TagIds.plates_iron);
        shapeless(NS, "rtg_fuel_americium", ItemIds.pellet_rtg_americium,
                ItemIds.billet_am241, ItemIds.billet_am241, ItemIds.billet_am241, TagIds.plates_iron);
        shapeless(NS, "rtg_fuel_cobalt", ItemIds.pellet_rtg_cobalt,
                ItemIds.billet_co60, ItemIds.billet_co60, ItemIds.billet_co60, TagIds.plates_iron);
        shapeless(NS, "rtg_fuel_gold", ItemIds.pellet_rtg_gold,
                ItemIds.billet_au198, ItemIds.billet_au198, ItemIds.billet_au198, TagIds.plates_iron);
        shapeless(NS, "rtg_fuel_lead", ItemIds.pellet_rtg_lead,
                ItemIds.billet_pb209, ItemIds.billet_pb209, ItemIds.billet_pb209, TagIds.plates_iron);
        shapeless(NS, "rtg_fuel_polonium", ItemIds.pellet_rtg_polonium,
                ItemIds.billet_polonium, ItemIds.billet_polonium, ItemIds.billet_polonium, TagIds.plates_iron);
        shapeless(NS, "rtg_fuel_pu238", ItemIds.pellet_rtg,
                ItemIds.billet_pu238, ItemIds.billet_pu238, ItemIds.billet_pu238, TagIds.plates_iron);
        shapeless(NS, "rtg_fuel_radium", ItemIds.pellet_rtg_radium,
                ItemIds.billet_ra226, ItemIds.billet_ra226, ItemIds.billet_ra226, TagIds.plates_iron);
        shapeless(NS, "rtg_fuel_strontium", ItemIds.pellet_rtg_strontium,
                ItemIds.billet_sr90, ItemIds.billet_sr90, ItemIds.billet_sr90, TagIds.plates_iron);
    }

    private static void shapeless4() {
        shapeless(NS, "rtg_fuel_weak", ItemIds.pellet_rtg_weak,
                ItemIds.billet_u238, ItemIds.billet_u238, ItemIds.billet_pu238, TagIds.plates_iron);
        shapeless(NS, "rtg_recover_bismuth", ItemIds.billet_bismuth, 3, ItemIds.pellet_rtg_depleted_bismuth);
        shapeless(NS, "rtg_recover_lead", ItemIds.ingot_lead, 2, ItemIds.pellet_rtg_depleted_lead);
        shapeless(NS, "rtg_recover_neptunium", ItemIds.billet_neptunium, 3, ItemIds.pellet_rtg_depleted_neptunium);
        shapeless(NS, "rtg_recover_zirconium", ItemIds.billet_zirconium, 3, ItemIds.pellet_rtg_depleted_zirconium);
        shapeless(NS, "rune_dagaz", ItemIds.rune_dagaz,
                ItemIds.rune_blank, ItemIds.powder_spark_mix, ItemIds.singularity);
        shapeless(NS, "rune_hagalaz", ItemIds.rune_hagalaz,
                ItemIds.rune_blank, ItemIds.powder_spark_mix, ItemIds.singularity_super_heated);
        shapeless(NS, "rune_isa", ItemIds.rune_isa,
                ItemIds.rune_blank, ItemIds.powder_spark_mix, ItemIds.singularity_counter_resonant);
        shapeless(NS, "rune_jera", ItemIds.rune_jera,
                ItemIds.rune_blank, ItemIds.powder_spark_mix, ItemIds.singularity_spark);
        shapeless(NS, "rune_thurisaz", ItemIds.rune_thurisaz,
                ItemIds.rune_blank, ItemIds.powder_spark_mix, ItemIds.black_hole);
        shapeless(NS, "sand_boron", ItemIds.sand_boron, 8,
                TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand,
                TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand,
                TagIds.dusts_boron);
        shapeless(NS, "sand_lead", ItemIds.sand_lead, 8,
                TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand,
                TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand,
                TagIds.dusts_lead);
        shapeless(NS, "sand_polonium", ItemIds.sand_polonium, 8,
                TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand,
                TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand,
                TagIds.dusts_polonium);
        shapeless(NS, "sand_quartz", ItemIds.sand_quartz,
                TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.dusts_quartz, TagIds.dusts_quartz);
        shapeless(NS, "sand_uranium", ItemIds.sand_uranium, 8,
                TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand,
                TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand,
                TagIds.dusts_uranium);
        shapeless(NS, "sandbags", ItemIds.sandbags, 4,
                ItemIds.plate_polymer, TagIds.minecraft_sand, TagIds.minecraft_sand, TagIds.minecraft_sand);
        shapeless(NS, "solid_fuel_presto_triplet", ItemIds.solid_fuel_presto_triplet,
                ItemIds.solid_fuel_presto, ItemIds.solid_fuel_presto, ItemIds.solid_fuel_presto, ItemIds.ball_dynamite);
        shapeless(NS, "solid_fuel_presto_triplet_bf", ItemIds.solid_fuel_presto_triplet_bf,
                ItemIds.solid_fuel_presto_bf, ItemIds.solid_fuel_presto_bf, ItemIds.solid_fuel_presto_bf,
                ItemIds.ingot_c4);
        shapeless(NS, "steel_corner", ItemIds.steel_corner, ItemIds.steel_wall, ItemIds.steel_wall);
        shapeless(NS, "syringe_metal_stimpak_1", ItemIds.syringe_metal_stimpak,
                ItemIds.nitra_small, ItemIds.nitra_small, ItemIds.nitra_small, ItemIds.syringe_metal_empty);
        shapeless(NS, "terrain_block_waste_painted", ItemIds.block_waste_painted,
                TagIds.dyes_yellow, ItemIds.block_waste);
        shapeless(NS, "terrain_plant_item_rope", ItemIds.plant_item_rope,
                VanillaIds.string, VanillaIds.string, VanillaIds.string);
        shapeless(NS, "terrain_radaway_strong_mush", ItemIds.radaway_strong, ItemIds.radaway, ItemIds.mush);
        shapeless(NS, "tile_lab", ItemIds.tile_lab, 3,
                ItemIds.tile_lab_stairs, ItemIds.tile_lab_stairs, ItemIds.tile_lab_stairs, ItemIds.tile_lab_stairs);
        shapeless(NS, "tile_lab_1", ItemIds.tile_lab, ItemIds.tile_lab_slab, ItemIds.tile_lab_slab);
        shapeless(NS, "tile_lab_broken", ItemIds.tile_lab_broken, 3,
                ItemIds.tile_lab_broken_stairs, ItemIds.tile_lab_broken_stairs, ItemIds.tile_lab_broken_stairs,
                ItemIds.tile_lab_broken_stairs);
        shapeless(NS, "tile_lab_broken_1", ItemIds.tile_lab_broken,
                ItemIds.tile_lab_broken_slab, ItemIds.tile_lab_broken_slab);
        shapeless(NS, "tile_lab_cracked", ItemIds.tile_lab_cracked, 3,
                ItemIds.tile_lab_cracked_stairs, ItemIds.tile_lab_cracked_stairs, ItemIds.tile_lab_cracked_stairs,
                ItemIds.tile_lab_cracked_stairs);
        shapeless(NS, "tile_lab_cracked_1", ItemIds.tile_lab_cracked,
                ItemIds.tile_lab_cracked_slab, ItemIds.tile_lab_cracked_slab);
        shapeless(NS, "trapdoor_steel", ItemIds.trapdoor_steel, TagIds.minecraft_wooden_trapdoors, TagIds.ingots_steel);
        shapeless(NS, "vinyl_tile_large_1", ItemIds.vinyl_tile_large, ItemIds.vinyl_tile_small);
        shapeless(NS, "waste_trinitite", ItemIds.waste_trinitite, TagIds.minecraft_sand, ItemIds.trinitite);
        shapeless(NS, "waste_trinitite_red", ItemIds.waste_trinitite_red, TagIds.minecraft_sand, ItemIds.trinitite);
    }
}
