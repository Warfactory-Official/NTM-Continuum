package com.hbm.inventory.recipes;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.ModSteelBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ItemEnums.EnumPartType;
import com.hbm.items.ItemEnums.EnumExpensiveType;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModUpgradeItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;

/**
 * Assembly machine recipes — a faithful but <b>feasibility-limited</b> slice of the 1.12.2 catalogue.
 * Recipes are picked by the player in the in-GUI recipe selector ({@link com.hbm.inventory.gui
 * .GUIScreenRecipeSelector}); the {@code blueprint_plate} template still gates the pooled plate recipes.
 *
 * <p><b>What's ported:</b> every recipe whose items already exist in the port (see IMPLEMENTATION.md §18 —
 * "GUI selector + feasible recipes"):
 * <ul>
 *   <li>the metal-<b>plate</b> family (ingot → plate), pooled under {@code alt.plates} + the {@code plates}
 *       auto-switch group, for all metals whose ingot+plate are ported;</li>
 *   <li>a handful of always-available (non-pooled) part recipes: asbestos cloth, the centrifuge element,
 *       and the beryllium/copper cyclotron powders ({@code cyclotron} auto-switch group).</li>
 * </ul>
 *
 * <p><b>Deferred</b> (the bulk of the ~343 originals): every recipe needing an un-ported intermediate —
 * cast/welded plates, shells, pipes, fine/dense wires, the metal <i>dusts</i> that aren't ported, circuits
 * ({@code EnumCircuitType}), expensive parts ({@code EnumExpensiveType}), motor variants and the machine
 * blocks they build — plus all fluid/chance recipes (the engine is still item-only). These land as the
 * material/tag system ({@code OreDictManager}) and more items are ported.
 */
public class AssemblyMachineRecipes extends GenericRecipes {

    public static final AssemblyMachineRecipes INSTANCE = new AssemblyMachineRecipes();

    public static final String POOL_PLATES = POOL_PREFIX_ALT + "plates";
    private static final String GROUP_PLATES = "plates";
    private static final String GROUP_CYCLOTRON = "cyclotron";
    private static final String GROUP_PILE_ROD = "pilerod";

    private static ItemStack satellite(EnumSatType type) {
        return com.hbm.items.ModSatelliteItems.satellite.get().of(type);
    }

    /** A full fluid barrel as a recipe input; the barrel itself comes back. */
    private static ComparableStack barrel(com.hbm.inventory.fluid.FluidType fluid) {
        return new ComparableStack(com.hbm.items.ModFluidItems.fluid_barrel.get().make(fluid, 16_000));
    }

    private static ComparableStack lithiumPack() {
        return new ComparableStack(com.hbm.items.ModBatteryItems.PACKS
                .get(com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack.BATTERY_LITHIUM).get());
    }

    /** The {@code c:concretes} tag; concrete has no {@link com.hbm.inventory.material.NTMMaterial}. */
    private static com.hbm.inventory.RecipesCommon.AStack concrete(int count) {
        return new com.hbm.inventory.RecipesCommon.TagStack(
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        dev.golem.core.Ids.of("c", "concretes")), count);
    }

    /** The old {@code ANY_HIGHEXPLOSIVE} ore key: the {@code c:ingots/any_highexplosive} tag (TATB, TNT). */
    private static com.hbm.inventory.RecipesCommon.AStack highExplosive(int count) {
        return new com.hbm.inventory.RecipesCommon.TagStack(
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        dev.golem.core.Ids.of("c", "ingots/any_highexplosive")), count);
    }

    private static ItemStack pileRod(EnumPileRod type, int count) {
        return ModPileBlocks.pile_rod.get().of(type, count);
    }

    @Override
    public void registerRecipes() {
        if (!recipeOrderedList.isEmpty()) return; // idempotent

        // --- plates (pooled: need the plate blueprint; auto-switch by inserted ingot) ---
        // Ingredients are now MATERIAL TAGS (c:ingots/<metal>) via the material system, so any mod's ingot
        // of that metal works — the modern ore-dictionary equivalent.
        plate("ass.plateiron", IRON, ModItems.plate_iron.get());
        plate("ass.plategold", GOLD, ModItems.plate_gold.get());
        plate("ass.platetitanium", TI, ModItems.plate_titanium.get());
        plate("ass.platealu", AL, ModItems.plate_aluminium.get());
        plate("ass.platesteel", STEEL, ModItems.plate_steel.get());
        plate("ass.platelead", PB, ModItems.plate_lead.get());
        plate("ass.platecopper", CU, ModItems.plate_copper.get());
        plate("ass.plategunmetal", GUNMETAL, ModItems.plate_gunmetal.get());
        plate("ass.plateweaponsteel", WEAPONSTEEL, ModItems.plate_weaponsteel.get());
        plate("ass.platecmb", CMB, ModItems.plate_combine_steel.get());
        plate("ass.platedura", DURA, ModItems.plate_dura_steel.get());
        plate("ass.platesaturnite", BIGMT, ModItems.plate_saturnite.get());
        plate("ass.plateschrab", SA326, ModItems.plate_schrabidium.get());

        // --- always-available (non-pooled) parts ---
        register(new GenericRecipe("ass.firecloth").setup(50, 100)
                .inputItems(new ComparableStack(ModItems.ingot_asbestos.get(), 1), new ComparableStack(Items.STRING, 8))
                .outputItems(new ItemStack(ModItems.asbestos_cloth.get(), 4)));

        register(new GenericRecipe("ass.centrifugetower").setup(100, 100)
                .inputItems(new ComparableStack(ModItems.plate_dura_steel.get(), 4),
                        new ComparableStack(ModItems.plate_titanium.get(), 4),
                        new ComparableStack(ModItems.motor.get(), 1))
                .outputItems(new ItemStack(ModItems.centrifuge_element.get(), 1)));

        register(new GenericRecipe("ass.gascent").setup(400, 100)
                .inputItems(new ComparableStack(ModItems.centrifuge_element.get(), 4), ANY_PLASTIC.ingot().count(8),
                        DESH.ingot().count(2), STEEL.plate().count(8),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 1))
                .outputItems(new ItemStack(ModMachineBlocks.machine_gascent.get())));

        // --- standard cyclotron (1.12.2 "ass.cyclotron") ---
        register(new GenericRecipe("ass.cyclotron").setup(600, 100)
                .inputItems(new ComparableStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LITHIUM).get()),
                        ND.wireDense().count(32), STEEL.plateCast().count(16), STEEL.plate().count(32),
                        AL.plate().count(32), ANY_PLASTIC.ingot().count(24), RUBBER.ingot().count(24),
                        CU.plateCast().count(8),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 16))
                .outputItems(new ItemStack(ModMachineBlocks.machine_cyclotron.get())));

        register(new GenericRecipe("ass.electrolyzer").setup(200, 100)
                .inputItems(STEEL.plateCast().count(8), CU.plate().count(16), TI.shell().count(3), RUBBER.ingot().count(8),
                        new ComparableStack(ModItems.ingot_firebrick.get(), 16),
                        new ComparableStack(ModItems.coil_copper.get(), 16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 8))
                .outputItems(new ItemStack(ModMachineBlocks.machine_electrolyser.get())));

        // --- cyclotron powders (non-pooled; auto-switch by inserted powder) ---
        register(new GenericRecipe("ass.partberyl").setup(40, 100)
                .inputItems(new ComparableStack(ModItems.powder_beryllium.get(), 1))
                .outputItems(new ItemStack(ModItems.part_beryllium.get(), 8))
                .setGroup(GROUP_CYCLOTRON, this));
        register(new GenericRecipe("ass.partcop").setup(40, 100)
                .inputItems(new ComparableStack(ModItems.powder_copper.get(), 1))
                .outputItems(new ItemStack(ModItems.part_copper.get(), 8))
                .setGroup(GROUP_CYCLOTRON, this));

        // --- autogen metal-shape items: cast plate (3 plates -> 1 cast) + welded plate (2 cast -> 1 welded),
        // generated for EVERY material that auto-generates the shape (was hardcoded to steel/copper/titanium).
        // Gated like the 1.12.2 Mats.orderedList loops: skip a material whose precursor item isn't ported so
        // we never register an unmatchable recipe. Cast input is the c:plates/<mat> tag (needs a ported plate);
        // welded input is the autogen cast plate (MaterialStack — no tag dependency). ---
        for (NTMMaterial mat : Mats.orderedList) {
            Item cast = ModAutogenItems.get(MaterialShapes.CASTPLATE, mat);
            Item welded = ModAutogenItems.get(MaterialShapes.WELDEDPLATE, mat);
            if (cast != null && hasItem(MaterialShapes.PLATE, mat)) {
                register(new GenericRecipe("ass.cast." + mat.name).setup(80, 100)
                        .inputItems(mat.plate().count(3))            // 3 plates in ONE stack (c:plates/<mat>)
                        .outputItems(new ItemStack(cast, 1)));
            }
            if (welded != null && cast != null) {
                register(new GenericRecipe("ass.weld." + mat.name).setup(120, 100)
                        .inputItems(new MaterialStack(mat, MaterialShapes.CASTPLATE, 2))
                        .outputItems(new ItemStack(welded, 1)));
            }
        }

        // --- "expensive mode" structural parts: canonical (unconditional) assembly recipes that CONSUME
        // the ported ItemEnumMulti intermediates (§14) + autogen shapes. NB: the BASIC circuit ("Integrated
        // Circuit Board") is produced by the SOLDERING station (SolderingRecipes), NOT the assembly machine
        // — here it is only ever an input — so producing it is deferred with the soldering machine. (The old
        // assembly "factorioChip" that output BASIC is a non-default `!GeneralConfig.enable528` alternate,
        // intentionally not ported.) Non-pooled; values are the 1.12.2 originals. ---
        register(new GenericRecipe("ass.ex_steel_plating").setup(200, 400)
                .inputItems(STEEL.plateCast().count(4), TI.plate().count(4), STEEL.bolt().count(16))
                .outputItems(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING, 1)));
        register(new GenericRecipe("ass.ex_lead_plating").setup(400, 4_000)
                .inputItems(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 2),
                        PB.plateCast().count(8), B.ingot().count(2), W.bolt().count(32))
                .outputItems(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING, 1)));
        register(new GenericRecipe("ass.ex_circuit").setup(400, 4_000)
                .inputItems(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 12),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 8),
                        RUBBER.ingot().count(4))
                .outputItems(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT, 1)));
        register(new GenericRecipe("ass.ex_heavy_frame").setup(600, 800)   // uses the new ANY_PLASTIC aggregate tag
                .inputItems(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 3),
                        ANY_PLASTIC.ingot().count(8), CU.plateWelded().count(4), DESH.ingot().count(1), DURA.bolt().count(32))
                .outputItems(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME, 1)));

        // --- machine upgrades: the OVERDRIVE chain (1.12.2 "ass.overdrive1..3", the default non-528
        // branch). Mk.III's ANY_BISMOIDBRONZE dict-group isn't ported; bismuth bronze (its primary
        // member) stands in. ---
        register(new GenericRecipe("ass.overdrive1").setup(200, 100)
                .inputItems(new ComparableStack(ModUpgradeItems.upgrade_speed_3.get(), 1),
                        new ComparableStack(ModUpgradeItems.upgrade_effect_3.get(), 1),
                        BIGMT.ingot().count(16), ANY_HARDPLASTIC.ingot().count(16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 16))
                .outputItems(new ItemStack(ModUpgradeItems.upgrade_overdrive_1.get(), 1)));
        register(new GenericRecipe("ass.overdrive2").setup(600, 100)
                .inputItems(new ComparableStack(ModUpgradeItems.upgrade_overdrive_1.get(), 1),
                        new ComparableStack(ModUpgradeItems.upgrade_speed_3.get(), 1),
                        new ComparableStack(ModUpgradeItems.upgrade_effect_3.get(), 1),
                        BIGMT.ingot().count(16), new ComparableStack(ModItems.ingot_cft.get(), 8),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR_BOARD), 16))
                .outputItems(new ItemStack(ModUpgradeItems.upgrade_overdrive_2.get(), 1)));
        register(new GenericRecipe("ass.overdrive3").setup(1_200, 100)
                .inputItems(new ComparableStack(ModUpgradeItems.upgrade_overdrive_2.get(), 1),
                        new ComparableStack(ModUpgradeItems.upgrade_speed_3.get(), 1),
                        new ComparableStack(ModUpgradeItems.upgrade_effect_3.get(), 1),
                        BBRONZE.ingot().count(16), new ComparableStack(ModItems.ingot_cft.get(), 16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 16))
                .outputItems(new ItemStack(ModUpgradeItems.upgrade_overdrive_3.get(), 1)));

        // --- precision assembler construction (the old ass.precass machine recipe) ---
        register(new GenericRecipe("ass.precass").setup(1_200, 100)
                .inputItems(STEEL.plateCast().count(8), ZR.ingot().count(8),
                        new ComparableStack(ModItems.motor.get(), 4),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR_BOARD), 4))
                .outputItems(new ItemStack(ModMachineBlocks.machine_precass.get())));

        // --- the PUREX plant (1.12.2 "ass.purex"). Curated rather than generated: the generator resolves
        // it, but re-running it currently drops a set of already-emitted recipes (vanilla-ingredient
        // resolution regressed — see IMPLEMENTATION.md §20), so the file was left as last generated. The
        // old recipe's `inputItemsEx` expensive-mode variant is not ported (no machine reads it yet). ---
        register(new GenericRecipe("ass.purex").setup(300, 100)
                .inputItems(STEEL.shell().count(4), RUBBER.pipe().count(8), PB.plateCast().count(4),
                        new ComparableStack(ModItems.motor_desh.get()),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 4))
                .outputItems(new ItemStack(ModMachineBlocks.machine_purex.get())));

        // --- the SILEX (1.12.2 "ass.silex"). Curated for the same reason as ass.purex above. The old
        // recipe's `inputItemsEx` expensive-mode variant is not ported (no machine reads it yet). ---
        register(new GenericRecipe("ass.silex").setup(400, 100)
                .inputItems(new ComparableStack(com.hbm.blocks.ModGlassBlocks.glass_quartz.get().asItem(), 16),
                        STEEL.plateCast().count(8), DESH.ingot().count(4), RUBBER.ingot().count(8),
                        STEEL.pipe().count(8))
                .outputItems(new ItemStack(ModMachineBlocks.machine_silex.get())));

        // --- fusion plasma forge (1.12.2 "ass.fusionplasmaforge"). The old ANY_BISMOIDBRONZE
        // aggregate is represented by its ported primary member; the normal recipe remains feasible. ---
        register(new GenericRecipe("ass.fusionplasmaforge").setup(1_200, 100)
                .inputItems(ANY_RESISTANTALLOY.plateWelded().count(8), BSCCO.wireDense().count(32),
                        BBRONZE.plateCast().count(16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 4))
                .outputItems(new ItemStack(ModMachineBlocks.fusion_plasma_forge.get())));

        // --- fusion breeder (1.12.2 "ass.fusionbreeder") ---
        register(new GenericRecipe("ass.fusionbreeder").setup(300, 100)
                .inputItems(ANY_RESISTANTALLOY.plateCast().count(4), STEEL.pipe().count(4),
                        B.ingot().count(16), ANY_HARDPLASTIC.ingot().count(16))
                .outputItems(new ItemStack(ModMachineBlocks.fusion_breeder.get())));
        // The old inputItemsEx variant is deferred: it requires the unported expensive-mode selector.

        // --- industrial steam turbine (1.12.2 "ass.iturbine") ---
        register(new GenericRecipe("ass.iturbine").setup(200, 100)
                .inputItems(STEEL.plate().count(12), RUBBER.ingot().count(4),
                        new ComparableStack(ModItems.turbine_titanium.get(), 2), GOLD.wireDense().count(6),
                        DURA.pipe().count(3), STEEL.pipe().count(4),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 4))
                .outputItems(new ItemStack(ModMachineBlocks.machine_industrial_turbine.get())));

        // --- chicago pile (1.12.2 "ass.pile*") ---
        register(new GenericRecipe("ass.pileblock").setup(20, 250)
                .inputItems(new com.hbm.inventory.RecipesCommon.TagStack(net.minecraft.tags.ItemTags.PLANKS, 1),
                        Mats.MAT_GRAPHITE.ingot().count(4), STEEL.bolt().count(2))
                .outputItems(new ItemStack(ModPileBlocks.pile_brick.get())));
        register(new GenericRecipe("ass.pilepabe").setup(40, 200)
                .inputItems(new ComparableStack(ModItems.billet_ra226be.get(), 3))
                .outputItems(pileRod(EnumPileRod.RA226BE, 1))
                .setGroup(GROUP_PILE_ROD, this));
        register(new GenericRecipe("ass.pilepobe").setup(40, 200)
                .inputItems(new ComparableStack(ModItems.billet_po210be.get(), 3))
                .outputItems(pileRod(EnumPileRod.PO210BE, 1))
                .setGroup(GROUP_PILE_ROD, this));
        register(new GenericRecipe("ass.pilezr").setup(40, 200)
                .inputItems(ZR.billet().count(1))
                .outputItems(pileRod(EnumPileRod.ZR, 3))
                .setGroup(GROUP_PILE_ROD, this));
        register(new GenericRecipe("ass.pilenu").setup(40, 200)
                .inputItems(U.billet().count(3))
                .outputItems(pileRod(EnumPileRod.NU, 1))
                .setGroup(GROUP_PILE_ROD, this));
        register(new GenericRecipe("ass.pilethorium").setup(40, 200)
                .inputItems(TH232.billet().count(3))
                .outputItems(pileRod(EnumPileRod.THORIUM, 1))
                .setGroup(GROUP_PILE_ROD, this));

        // --- the satellite launch vehicle ---
        register(new GenericRecipe("ass.soyuz").setup(6_000, 100)
                .inputItems(TI.shell().count(32), RUBBER.ingot().count(64),
                        new ComparableStack(com.hbm.items.ModFuelItems.rocket_fuel.get(), 64),
                        new ComparableStack(ModItems.thruster_small.get(), 12),
                        new ComparableStack(ModItems.thruster_medium.get(), 12),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 4),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.LDE), 32))
                .outputItems(com.hbm.blocks.machine.soyuz.ModSoyuzBlocks.missile_soyuz.get()
                        .of(com.hbm.items.ItemEnums.SoyuzSkinType.NORMAL)));

        // --- satellites and the ground station ---
        register(new GenericRecipe("ass.satlink").setup(100, 1_000)
                .inputItems(new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 16),
                        AL.plate().count(16), new ComparableStack(ModItems.magnetron.get(), 3),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER), 1))
                .outputItems(new ItemStack(ModMachineBlocks.machine_satlink.get())));

        register(new GenericRecipe("ass.satelliterelay").setup(600, 100)
                .inputItems(TI.shell().count(3), new ComparableStack(ModItems.plate_desh.get(), 8),
                        barrel(Fluids.HYDROGEN), new ComparableStack(ModItems.photo_panel.get(), 16),
                        new ComparableStack(ModItems.thruster_nuclear.get(), 1),
                        new ComparableStack(ModItems.ingot_uranium_fuel.get(), 6),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 24),
                        new ComparableStack(ModItems.magnetron.get(), 3), lithiumPack())
                .outputItems(satellite(EnumSatType.RELAY)));
        register(new GenericRecipe("ass.satelliteasteroidminer").setup(600, 100)
                .inputItems(BIGMT.plate().count(24), new ComparableStack(ModItems.motor_desh.get(), 2),
                        new ComparableStack(ModItems.drill_titanium.get(), 2),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 12),
                        barrel(Fluids.KEROSENE), new ComparableStack(ModItems.thruster_small.get(), 1),
                        new ComparableStack(ModItems.photo_panel.get(), 12),
                        new ComparableStack(ModItems.centrifuge_element.get(), 4), lithiumPack())
                .outputItems(satellite(EnumSatType.MINER_ASTRO)));
        register(new GenericRecipe("ass.satellitelunarminer").setup(600, 100)
                .inputItems(new ComparableStack(ModItems.plate_desh.get(), 4),
                        new ComparableStack(ModItems.motor.get(), 2),
                        new ComparableStack(ModItems.drill_titanium.get(), 2),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 8),
                        barrel(Fluids.KEROSENE), new ComparableStack(ModItems.thruster_small.get(), 1),
                        new ComparableStack(ModItems.photo_panel.get(), 12), lithiumPack())
                .outputItems(satellite(EnumSatType.MINER_LUNAR)));
        register(new GenericRecipe("ass.detectorsat").setup(1_200, 25_000)
                .inputItems(GOLD.plateCast().count(16), new ComparableStack(ModItems.photo_panel.get(), 64),
                        BSCCO.wireDense().count(16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 24),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.LDE), 16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 1))
                .outputItems(satellite(EnumSatType.DETECTOR)));
        register(new GenericRecipe("ass.rayscansat").setup(1_200, 25_000)
                .inputItems(BIGMT.shell().count(16), new ComparableStack(ModItems.photo_panel.get(), 32),
                        SBD.wireDense().count(16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.QUANTUM), 16),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.LDE), 16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 3))
                .outputItems(satellite(EnumSatType.RAY_SCAN)));


        // --- gas turbine and turbofan (1.12.2 "ass.gasturbine" / "ass.turbofan") ---
        register(new GenericRecipe("ass.gasturbine").setup(400, 100)
                .inputItems(STEEL.shell().count(10), GOLD.wireDense().count(12), DURA.pipe().count(4),
                        STEEL.pipe().count(4), new ComparableStack(ModItems.turbine_tungsten.get()),
                        new ComparableStack(ModItems.ingot_rubber.get(), 12),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 3))
                .outputItems(new ItemStack(ModMachineBlocks.machine_turbine_gas.get())));
        register(new GenericRecipe("ass.turbofan").setup(300, 100)
                .inputItems(TI.shell().count(8), DURA.pipe().count(4), ANY_PLASTIC.ingot().count(12),
                        new ComparableStack(ModItems.turbine_tungsten.get()), GOLD.wireDense().count(12),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 3))
                 .outputItems(new ItemStack(ModMachineBlocks.machine_turbofan.get())));

        // --- geothermal heat exchanger and Leviathan steam turbine (1.7 "ass.hephaestus" / "ass.leviturbine";
        //     the expensive-mode inputItemsEx variants are deferred like the others) ---
        register(new GenericRecipe("ass.hephaestus").setup(200, 100)
                .inputItems(STEEL.pipe().count(12), STEEL.ingot().count(24), CU.plate().count(24), NB.ingot().count(4),
                        RUBBER.ingot().count(12),
                        new ComparableStack(com.hbm.blocks.ModGlassBlocks.glass_quartz.get().asItem(), 16))
                .outputItems(new ItemStack(com.hbm.blocks.ModPowerGenBlocks.MACHINE_HEPHAESTUS.get())));
        register(new GenericRecipe("ass.leviturbine").setup(600, 100)
                .inputItems(STEEL.shell().count(6), STEEL.plateWelded().count(16), TI.plate().count(12),
                        ANY_RESISTANTALLOY.ingot().count(16), new ComparableStack(ModItems.turbine_tungsten.get(), 5),
                        new ComparableStack(ModItems.turbine_titanium.get(), 3),
                        new ComparableStack(ModItems.flywheel_beryllium.get(), 1), GOLD.wireDense().count(48),
                        DURA.pipe().count(16), STEEL.pipe().count(16))
                .outputItems(new ItemStack(com.hbm.blocks.ModPowerGenBlocks.MACHINE_CHUNGUS.get())));

        // --- giant fluid storage (1.12.2 "ass.bat9k" / "ass.bigasstank") ---
        register(new GenericRecipe("ass.bat9k").setup(200, 100)
                .inputItems(STEEL.plate().count(16), ANY_RESISTANTALLOY.plateWelded().count(2),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 16),
                        new com.hbm.inventory.RecipesCommon.TagStack(
                                com.hbm.inventory.recipes.PyroOvenRecipes.TARS, 16))
                .outputItems(new ItemStack(ModMachineBlocks.machine_bat9000.get())));
        register(new GenericRecipe("ass.bigasstank").setup(200, 100)
                .inputItems(STEEL.plate().count(16), ANY_RESISTANTALLOY.plateWelded().count(4),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 16))
                .outputItems(new ItemStack(ModMachineBlocks.machine_bigasstank.get())));

        // Radioactive drums (1.7 AssemblyMachineRecipes "blocks": steel tank + 2 lead plates + 10 waste).
        register(new GenericRecipe("ass.yellowbarrel").setup(400, 400)
                .inputItems(new ComparableStack(ModItems.tank_steel.get(), 1), PB.plate().count(2),
                        new ComparableStack(ModItems.nuclear_waste.get(), 10))
                .outputItems(new ItemStack(com.hbm.blocks.ModFluidNetBlocks.yellow_barrel.get(), 1)));
        register(new GenericRecipe("ass.vitrifiedbarrel").setup(400, 400)
                .inputItems(new ComparableStack(ModItems.tank_steel.get(), 1), PB.plate().count(2),
                        new ComparableStack(ModItems.nuclear_waste_vitrified.get(), 10))
                .outputItems(new ItemStack(com.hbm.blocks.ModFluidNetBlocks.vitrified_barrel.get(), 1)));

        // 1.7 AssemblyMachineRecipes 1058-1066: the fluid pack is packed and unpacked here and nowhere
        // else (it was never a FluidContainerRegistry container), 32,000 mB per pack, every fluid without
        // FT_NoContainer. ass.emptypackage (4 Ti plates + 2 any-plastic) is in the generated file.
        fluidPacks();

        // 1.7 AssemblyMachineRecipes 823-824, values unchanged. Curated because the generated file has no entry
        // for it, which left the stealth missile creative-only. KEY_BLACK is black dye, as gen_recipes maps it
        // for the generated recipes.
        register(new GenericRecipe("ass.stealthmissile").setup(1_200, 100)
                .inputItems(TI.plate().count(20), AL.plate().count(20), new ComparableStack(Items.BLACK_DYE, 16),
                        ANY_HARDPLASTIC.ingot().count(16), highExplosive(4),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 4),
                        STEEL.bolt().count(32))
                .outputItems(new ItemStack(com.hbm.items.ModMissileItems.missile_stealth.get(), 1)));

        // --- Albion particle accelerator ---
        register(new GenericRecipe("ass.beamline").setup(200, 100)
                .inputItems(STEEL.plateCast().count(8), CU.plate().count(16), GOLD.wireDense().count(4))
                .outputItems(new ItemStack(ModMachineBlocks.pa_beamline.get(), 1)));
        register(new GenericRecipe("ass.rfc").setup(400, 100)
                .inputItems(new ComparableStack(ModMachineBlocks.pa_beamline.get().asItem(), 3),
                        STEEL.plateCast().count(16), CU.plate().count(64), ANY_HARDPLASTIC.ingot().count(16),
                        new ComparableStack(ModItems.magnetron.get(), 16))
                .outputItems(new ItemStack(ModMachineBlocks.pa_rfc.get(), 1)));
        register(new GenericRecipe("ass.quadrupole").setup(400, 100)
                .inputItems(new ComparableStack(ModMachineBlocks.pa_beamline.get().asItem(), 1),
                        STEEL.plateCast().count(16), ANY_HARDPLASTIC.ingot().count(16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 1))
                .outputItems(new ItemStack(ModMachineBlocks.pa_quadrupole.get(), 1)));
        register(new GenericRecipe("ass.dipole").setup(400, 100)
                .inputItems(new ComparableStack(ModMachineBlocks.pa_beamline.get().asItem(), 2),
                        STEEL.plateCast().count(16), ANY_HARDPLASTIC.ingot().count(32),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 4))
                .outputItems(new ItemStack(ModMachineBlocks.pa_dipole.get(), 1)));
        register(new GenericRecipe("ass.source").setup(400, 100)
                .inputItems(new ComparableStack(ModMachineBlocks.pa_beamline.get().asItem(), 3),
                        STEEL.plateCast().count(16), ANY_HARDPLASTIC.ingot().count(16),
                        new ComparableStack(ModItems.magnetron.get(), 16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.QUANTUM), 1))
                .outputItems(new ItemStack(ModMachineBlocks.pa_source.get(), 1)));
        register(new GenericRecipe("ass.detector").setup(400, 100)
                .inputItems(new ComparableStack(ModMachineBlocks.pa_beamline.get().asItem(), 3),
                        STEEL.plateCast().count(24), GOLD.wireDense().count(16), ANY_HARDPLASTIC.ingot().count(16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.QUANTUM), 4))
                .outputItems(new ItemStack(ModMachineBlocks.pa_detector.get(), 1)));

        register(new GenericRecipe("ass.pagold").setup(400, 100)
                .inputItems(GOLD.wireDense().count(64), GOLD.wireDense().count(64))
                .outputItems(new ItemStack(ModItems.pa_coil_gold.get(), 1)));
        register(new GenericRecipe("ass.panbti").setup(400, 100)
                .inputItems(NB.wireDense().count(64), TI.wireDense().count(64))
                .outputItems(new ItemStack(ModItems.pa_coil_niobium.get(), 1)));
        register(new GenericRecipe("ass.pabscco").setup(400, 100)
                .inputItems(BSCCO.wireDense().count(64), ANY_PLASTIC.ingot().count(64))
                .outputItems(new ItemStack(ModItems.pa_coil_bscco.get(), 1)));
        register(new GenericRecipe("ass.pachlorophyte").setup(400, 100)
                .inputItems(CU.wireDense().count(64), CU.wireDense().count(64),
                        new ComparableStack(ModItems.powder_chlorophyte.get(), 16))
                .outputItems(new ItemStack(ModItems.pa_coil_chlorophyte.get(), 1)));

        // Exact old-hbm MachineCompressorCompact recipe. The mechanical generator deliberately
        // excludes this id because its compact inputs differ from the standard compressor.
        register(new GenericRecipe("ass.compactcompressor").setup(200, 100)
                .inputItems(STEEL.plateCast().count(8), TI.shell().count(4), CU.pipe().count(4),
                        new ComparableStack(ModItems.motor.get(), 2),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 4))
                .outputItems(new ItemStack(ModMachineBlocks.machine_compressor_compact.get(), 1)));

        // The bulk of the 1.12.2 catalogue, generated by port-tools/gen_machine_recipes.py (skips
        // anything already registered above — curated recipes win).
        com.hbm.inventory.recipes.gen.AssemblyMachineRecipesGenerated.registerAll(this);
        com.hbm.blocks.bomb.BombAssemblyRecipes.registerAll(this);
        com.hbm.inventory.recipes.Bombs2AssemblyRecipes.registerAll(this);
        com.hbm.inventory.recipes.RTGRecipes.registerAll(this);
        com.hbm.inventory.recipes.DoorAssemblyRecipes.registerAll(this);
        com.hbm.inventory.recipes.NuclearMiscAssemblyRecipes.registerAll(this);
        com.hbm.inventory.recipes.Production2AssemblyRecipes.registerAll(this);
        com.hbm.inventory.recipes.PwrAssemblyRecipes.registerAll(this);
        com.hbm.inventory.recipes.ExplosivesAssemblyRecipes.registerAll(this);
    }

    /**
     * True if the shape's per-material item is registered ({@code hbm:<prefix>_<name>}) — the tag-system
     * equivalent of the 1.12.2 {@code OreDictionary.doesOreNameExist} guard (items flush before recipes
     * lazily register). Vanilla iron/gold/copper also count for INGOT (they back {@code c:ingots/<name>}).
     */
    static boolean hasItem(MaterialShapes shape, NTMMaterial mat) {
        if (shape == MaterialShapes.INGOT
                && (mat.name.equals("iron") || mat.name.equals("gold") || mat.name.equals("copper"))) return true;
        return BuiltInRegistries.ITEM.containsKey(
                dev.golem.core.Ids.of("hbm", shape.itemPrefix + "_" + mat.name));
    }

    private void fluidPacks() {
        com.hbm.items.machine.ItemFluidContainerNT pack = com.hbm.items.ModFluidNetItems.fluid_pack_full.get();
        Item empty = ModItems.fluid_pack_empty.get();
        int amount = com.hbm.items.ModFluidNetItems.FLUID_PACK_CAPACITY;
        for (FluidType type : Fluids.getAll()) {
            if (type == Fluids.NONE || !pack.allows(type)) continue;
            String suffix = type.getName().toLowerCase(java.util.Locale.ROOT);
            register(new GenericRecipe("ass.package" + suffix).setup(40, 100)
                    .inputItems(new ComparableStack(empty))
                    .inputFluids(new FluidStack(type, amount))
                    .outputItems(pack.make(type, amount)));
            register(new GenericRecipe("ass.unpackage" + suffix).setup(40, 100)
                    .setIcon(ItemFluidIcon.make(new FluidStack(type, amount)))
                    .inputItems(new ComparableStack(pack.make(type, amount)))
                    .outputItems(new ItemStack(empty))
                    .outputFluids(new FluidStack(type, amount)));
        }
    }

    private void plate(String name, NTMMaterial mat, Item plate) {
        register(new GenericRecipe(name)
                .setup(60, 100)
                .inputItems(mat.ingot()) // c:ingots/<metal> tag (the material system's AStack)
                .outputItems(new ItemStack(plate, 1))
                .setPools(POOL_PLATES)
                .setGroup(GROUP_PLATES, this));
    }
}
