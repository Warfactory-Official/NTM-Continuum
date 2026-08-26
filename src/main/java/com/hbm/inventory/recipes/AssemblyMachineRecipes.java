package com.hbm.inventory.recipes;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ItemEnums.EnumExpensiveType;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModUpgradeItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import static com.hbm.inventory.OreDictManager.*;

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

        // --- industrial steam turbine (1.12.2 "ass.iturbine") ---
        register(new GenericRecipe("ass.iturbine").setup(200, 100)
                .inputItems(STEEL.plate().count(12), RUBBER.ingot().count(4),
                        new ComparableStack(ModItems.turbine_titanium.get(), 2), GOLD.wireDense().count(6),
                        DURA.pipe().count(3), STEEL.pipe().count(4),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 4))
                .outputItems(new ItemStack(ModMachineBlocks.machine_industrial_turbine.get())));

        // The bulk of the 1.12.2 catalogue, generated by port-tools/gen_machine_recipes.py (skips
        // anything already registered above — curated recipes win).
        com.hbm.inventory.recipes.gen.AssemblyMachineRecipesGenerated.registerAll(this);
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

    private void plate(String name, NTMMaterial mat, Item plate) {
        register(new GenericRecipe(name)
                .setup(60, 100)
                .inputItems(mat.ingot()) // c:ingots/<metal> tag (the material system's AStack)
                .outputItems(new ItemStack(plate, 1))
                .setPools(POOL_PLATES)
                .setGroup(GROUP_PLATES, this));
    }
}
