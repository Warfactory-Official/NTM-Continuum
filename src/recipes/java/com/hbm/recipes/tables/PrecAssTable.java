package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;
import static com.hbm.inventory.recipes.loader.GenericRecipes.POOL_PREFIX_528;
import static com.hbm.inventory.recipes.loader.GenericRecipes.POOL_PREFIX_DISCOVER;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe.OutputChoice;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.machine.ItemBlueprintFolder;
import com.hbm.items.machine.ItemBroken;
import com.hbm.items.machine.ItemOrbitalAssembly.EnumOrbitalAssembly;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1.7 {@code PrecAssRecipes.registerDefaults}. 1.7 registered the circuits and upgrades only under {@code enable528} and
 * chose the recycle chance by expensive mode; the rows carry those branches as {@code requires} flags.
 */
public final class PrecAssTable extends GenericTableSource<GenericRecipe> {

    private static final String ENABLE_528 = "enable_528";
    private static final String EXPENSIVE = "expensive_mode";
    private static final TagKey<Item> KEY_BLUE = TagKey.create(Registries.ITEM, Ids.of("c", "dyes/blue"));

    /** Inside 1.7's {@code if(GeneralConfig.enable528)} block. */
    private boolean enable528;
    /** The 1.7 position of the next row. */
    private int position;

    public PrecAssTable() {
        super(PrecAssRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {

        // 1.7 picks the inputs and the success chance by expensive mode; each mode is its own pair of rows
        crystalCircuit(false);
        crystalCircuit(true);

        // i cast: bleeding anus
        // i cast: XL horse dildo (unlubed)
        // i cast: salted lemon juice in stab wound
        // i cast: ovorial torsion
        // i cast: perpetually breaking and healing kneecaps
        // i cast: used needle sandwich
        // 1.7: if(GeneralConfig.enable528) { ... } -- every row of the branch carries "requires": ["enable_528"]
        enable528 = true;
        {
            registerPair(new GenericRecipe("precass.chip").setup(100, 200L)
                    .inputItems(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.SILICON), 1),
                            new ComparableStack(ModItems.plate_polymer.get(), 3),
                            GOLD.wire().count(4)).setPools(POOL_PREFIX_528 + "chip"),
                    new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP)), 90, expensiveMode(50, 90));

            registerPair(new GenericRecipe("precass.chip_bismoid").setup(200, 1_000L)
                    .inputItems(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.SILICON), 4),
                            new ComparableStack(ModItems.plate_polymer.get(), 8),
                            ANY_BISMOID.nugget().count(2),
                            GOLD.wire().count(4))
                    .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 500)).setPools(POOL_PREFIX_528 + "chip_bismoid"),
                    new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP_BISMOID)), 75, expensiveMode(50, 75));

            registerPair(new GenericRecipe("precass.chip_quantum").setup(300, 20_000L)
                    .inputItems(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.SILICON), 8),
                            BSCCO.wireDense().count(2),
                            ANY_HARDPLASTIC.ingot().count(4),
                            new ComparableStack(ModItems.pellet_charged.get(), 4),
                            GOLD.wire().count(8))
                    .inputFluids(new FluidStack(Fluids.HELIUM4, 250)).setPools(POOL_PREFIX_528 + "chip_quantum"),
                    new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP_QUANTUM)), 90, expensiveMode(50, 75));

            registerPair(new GenericRecipe("precass.atomic_clock").setup(200, 2_000L)
                    .inputItems(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP), 8),
                            ANY_PLASTIC.ingot().count(4),
                            ZR.wire().count(8),
                            SR.dust().count(1)).setPools(POOL_PREFIX_528 + "strontium"),
                    new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.ATOMIC_CLOCK)), 50, expensiveMode(50, 75));

            registerPair(new GenericRecipe("precass.controller").setup(400, 15_000L)
                    .inputItems(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP), 32),
                            new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 32),
                            new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR_TANTALIUM), 16),
                            new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_CHASSIS), 1),
                            new ComparableStack(ModUpgradeItems.upgrade_speed_1.get()),
                            PB.wire().count(16))
                    .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 1_000)),
                    new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER)), 75, expensiveMode(50, 90));

            registerPair(new GenericRecipe("precass.controller_advanced").setup(600, 25_000)
                    .inputItems(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP_BISMOID), 16),
                            new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR_TANTALIUM), 48),
                            new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ATOMIC_CLOCK), 1),
                            new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_CHASSIS), 1),
                            new ComparableStack(ModUpgradeItems.upgrade_speed_3.get()),
                            PB.wire().count(24))
                    .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 4_000)),
                    new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED)), 50, expensiveMode(50, 75));

            registerPair(new GenericRecipe("precass.controller_quantum").setup(600, 250_000)
                    .inputItems(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP_QUANTUM), 16),
                            new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP_BISMOID), 48),
                            new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ATOMIC_CLOCK), 8),
                            new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 2),
                            new ComparableStack(ModUpgradeItems.upgrade_overdrive_1.get()),
                            PB.wire().count(32))
                    .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 6_000)),
                    new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_QUANTUM)), 75, expensiveMode(50, 75));

            addFirstUpgrade(ModUpgradeItems.upgrade_speed_1.get(), ModUpgradeItems.upgrade_speed_2.get(), "precass.upgrade_speed_ii");
            addSecondUpgrade(ModUpgradeItems.upgrade_speed_2.get(), ModUpgradeItems.upgrade_speed_3.get(), "precass.upgrade_speed_iii");
            addFirstUpgrade(ModUpgradeItems.upgrade_effect_1.get(), ModUpgradeItems.upgrade_effect_2.get(), "precass.upgrade_effect_ii");
            addSecondUpgrade(ModUpgradeItems.upgrade_effect_2.get(), ModUpgradeItems.upgrade_effect_3.get(), "precass.upgrade_effect_iii");
            addFirstUpgrade(ModUpgradeItems.upgrade_power_1.get(), ModUpgradeItems.upgrade_power_2.get(), "precass.upgrade_power_ii");
            addSecondUpgrade(ModUpgradeItems.upgrade_power_2.get(), ModUpgradeItems.upgrade_power_3.get(), "precass.upgrade_power_iii");
            addFirstUpgrade(ModUpgradeItems.upgrade_fortune_1.get(), ModUpgradeItems.upgrade_fortune_2.get(), "precass.upgrade_fortune_ii");
            addSecondUpgrade(ModUpgradeItems.upgrade_fortune_2.get(), ModUpgradeItems.upgrade_fortune_3.get(), "precass.upgrade_fortune_iii");
            addFirstUpgrade(ModUpgradeItems.upgrade_afterburn_1.get(), ModUpgradeItems.upgrade_afterburn_2.get(), "precass.upgrade_ab_ii");
            addSecondUpgrade(ModUpgradeItems.upgrade_afterburn_2.get(), ModUpgradeItems.upgrade_afterburn_3.get(), "precass.upgrade_ab_iii");

            registerPair(new GenericRecipe("precass.upgrade_overdive_i").setup(200, 1_000)
                    .inputItems(new ComparableStack(ModUpgradeItems.upgrade_speed_3.get(), 1),
                            new ComparableStack(ModUpgradeItems.upgrade_effect_3.get(), 1),
                            BIGMT.ingot().count(16),
                            ANY_HARDPLASTIC.ingot().count(16),
                            new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 16)),
                    new ItemStack(ModUpgradeItems.upgrade_overdrive_1.get()), 50, expensiveMode(50, 75));
            registerPair(new GenericRecipe("precass.upgrade_overdive_ii").setup(600, 5_000)
                    .inputItems(new ComparableStack(ModUpgradeItems.upgrade_overdrive_1.get(), 1),
                            new ComparableStack(ModUpgradeItems.upgrade_speed_3.get(), 1),
                            new ComparableStack(ModUpgradeItems.upgrade_effect_3.get(), 1),
                            BIGMT.ingot().count(16),
                            new ComparableStack(ModItems.ingot_cft.get(), 8),
                            new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR_BOARD), 16)),
                    new ItemStack(ModUpgradeItems.upgrade_overdrive_2.get()), 50, expensiveMode(50, 75));
            registerPair(new GenericRecipe("precass.upgrade_overdive_iii").setup(1_200, 100_000)
                    .inputItems(new ComparableStack(ModUpgradeItems.upgrade_overdrive_2.get(), 1),
                            new ComparableStack(ModUpgradeItems.upgrade_speed_3.get(), 1),
                            new ComparableStack(ModUpgradeItems.upgrade_effect_3.get(), 1),
                            ANY_BISMOIDBRONZE.ingot().count(16),
                            new ComparableStack(ModItems.ingot_cft.get(), 16),
                            new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 16)),
                    new ItemStack(ModUpgradeItems.upgrade_overdrive_3.get()), 25, expensiveMode(50, 75));
        }
        enable528 = false;

        int min = 1_200;

        // all hail the pufferfish, driver of all innovation
        this.register(new GenericRecipe("precass.blueprints").setup(5 * min, 20_000L)
                .inputItems(new ComparableStack(Items.PAPER, 16),
                        new TagStack(KEY_BLUE).count(16),
                        new ComparableStack(Items.PUFFERFISH, 4))
                .outputChoices(
                    new OutputChoice(ItemBlueprintFolder.make(0), 10),
                    new OutputChoice(new ItemStack(Items.PAPER, 16), 90)
                ));
        this.register(new GenericRecipe("precass.beigeprints").setup(5 * min, 50_000L)
                .inputItems(new ComparableStack(Items.PAPER, 24),
                        CINNABAR.gem().count(24),
                        new ComparableStack(Items.PUFFERFISH, 8))
                .outputChoices(
                    new OutputChoice(ItemBlueprintFolder.make(1), 5),
                    new OutputChoice(new ItemStack(Items.PAPER, 24), 95)
                ));

        this.register(new GenericRecipe("precass.rpahelmet").setup(3 * min, 25_000L)
                .inputItems(new ComparableStack(ModItems.plate_armor_ajr.get(), 12),
                        BIGMT.plateCast().count(4),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 4),
                        new ComparableStack(ModItems.plate_kevlar.get(), 8))
                .outputItems(new ItemStack(ModArmorItems.rpa_helmet.get())).setPools(POOL_PREFIX_DISCOVER + ".rpa"));
        this.register(new GenericRecipe("precass.rpaplate").setup(3 * min, 25_000L)
                .inputItems(new ComparableStack(ModItems.plate_armor_ajr.get(), 24),
                        BIGMT.plateCast().count(8),
                        new TagStack(BIGMT.tag(MaterialShapes.MECHANISM)).count(8),
                        new ComparableStack(ModItems.motor_desh.get(), 8),
                        new ComparableStack(ModItems.plate_kevlar.get(), 16),
                        new ComparableStack(ModFinalConsumableItems.parts_legendary_tier2.get(), 1))
                .outputItems(new ItemStack(ModArmorItems.rpa_plate.get())).setPools(POOL_PREFIX_DISCOVER + ".rpa"));
        this.register(new GenericRecipe("precass.rpalegs").setup(3 * min, 25_000L)
                .inputItems(new ComparableStack(ModItems.plate_armor_ajr.get(), 24),
                        BIGMT.plateCast().count(8),
                        new ComparableStack(ModItems.motor_desh.get(), 8),
                        new ComparableStack(ModItems.plate_kevlar.get(), 16))
                .outputItems(new ItemStack(ModArmorItems.rpa_legs.get())).setPools(POOL_PREFIX_DISCOVER + ".rpa"));
        this.register(new GenericRecipe("precass.rpaboots").setup(3 * min, 25_000L)
                .inputItems(new ComparableStack(ModItems.plate_armor_ajr.get(), 12),
                        BIGMT.plateCast().count(4),
                        new ComparableStack(ModItems.plate_kevlar.get(), 8))
                .outputItems(new ItemStack(ModArmorItems.rpa_boots.get())).setPools(POOL_PREFIX_DISCOVER + ".rpa"));
    }

    private void crystalCircuit(boolean expensive) {
        GenericRecipe recipe = new GenericRecipe("precass.crystalcircuit").setup(600, 20_000L);
        if (expensive) {
            recipe.inputItems(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP_QUANTUM), 8),
                    new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR_TANTALIUM), 16),
                    new ComparableStack(ModItems.ingot_cft.get(), 4),
                    new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.PCB), 16),
                    BSCCO.wireDense().count(4));
        } else {
            recipe.inputItems(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP_QUANTUM), 4),
                    new ComparableStack(ModItems.ingot_cft.get(), 4),
                    new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.PCB), 16),
                    GOLD.wire().count(32));
        }
        ItemStack output = new ItemStack(ModSatelliteItems.orbital_assembly.get(EnumOrbitalAssembly.CRYSTAL_CIRCUIT));
        recipe.outputChoices(new OutputChoice(output, expensive ? 25 : 50), new OutputChoice(ItemBroken.make(output), expensive ? 75 : 50));
        GenericRecipe recycle = recycle(recipe, output, 100);
        String flag = expensive ? EXPENSIVE : "!" + EXPENSIVE;
        recipe.requires(flag);
        recycle.requires(flag);

        if (!expensive) {
            this.register(recipe);
            this.register(recycle);
        } else {
            recipe.order = position - 2;
            recycle.order = position - 1;
            add(id(recipe) + "." + EXPENSIVE, recipe);
            add(id(recycle) + "." + EXPENSIVE, recycle);
        }
    }

    private void addFirstUpgrade(Item lower, Item higher, String name) {

        registerPair(new GenericRecipe(name).setup(300, 10_000)
                .inputItems(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP), 8),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR_TANTALIUM), 4),
                        new ComparableStack(lower), ANY_PLASTIC.ingot().count(4)),
                new ItemStack(higher), 50, 75); // upgrades are now actually valuable
    }

    private void addSecondUpgrade(Item lower, Item higher, String name) {

        registerPair(new GenericRecipe(name).setup(400, 25_000)
                .inputItems(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP), 16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR_TANTALIUM), 16),
                        new ComparableStack(lower), RUBBER.ingot().count(4))
                .inputFluids(new FluidStack(Fluids.SOLVENT, 500)),
                new ItemStack(higher), 25, 75); // admittedly this one's just me being a dick
    }

    /** 1.7 {@code GeneralConfig.enableExpensiveMode ? expensive : normal}: both values, {normal, expensive}. */
    private static int[] expensiveMode(int expensive, int normal) {
        return new int[] {normal, expensive};
    }

    private void registerPair(GenericRecipe recipe, ItemStack output, int chance, int reclaim) {
        registerPair(recipe, output, chance, new int[] {reclaim, reclaim});
    }

    /**
     * Registers a generic pair of faulty product and recycling of broken items. Where the reclaim chance differs by
     * expensive mode, the recycle row comes twice, {@code "!expensive_mode"} and {@code "expensive_mode"}, at one position.
     */
    private void registerPair(GenericRecipe recipe, ItemStack output, int chance, int[] reclaim) {
        recipe.outputChoices(
                new OutputChoice(output, chance),
                new OutputChoice(ItemBroken.make(output), 100 - chance)
                );

        this.register(branch(recipe));

        if (reclaim[0] == reclaim[1]) {
            this.register(branch(recycle(recipe, output, reclaim[0])));
        } else {
            GenericRecipe normal = recycle(recipe, output, reclaim[0]).requires(ENABLE_528, "!" + EXPENSIVE);
            this.register(normal);
            GenericRecipe expensive = recycle(recipe, output, reclaim[1]).requires(ENABLE_528, EXPENSIVE);
            expensive.order = normal.order;
            add(id(expensive) + "." + ENABLE_528 + "." + EXPENSIVE, expensive);
        }
    }

    /** A row takes the next 1.7 position; the expensive twin of a recycle row shares its position. */
    @Override
    public void register(String id, GenericRecipe recipe) {
        recipe.order = position++;
        add(id, recipe);
    }

    /** The recycle half of 1.7 {@code registerPair}: each input back at the reclaim chance, and the fluid scaled by it. */
    private static GenericRecipe recycle(GenericRecipe recipe, ItemStack output, int reclaim) {
        float fReclaim = reclaim / 100F;

        ItemStack[] recycle = new ItemStack[recipe.inputItem.length];
        float[] chances = new float[recycle.length];
        for(int i = 0; i < recycle.length; i++) {
            recycle[i] = recipe.inputItem[i].getDisplayStack().copy();
            chances[i] = fReclaim;
        }

        GenericRecipe row = new GenericRecipe(recipe.getInternalName() + ".recycle").setup(recipe.duration, recipe.power).setNameWrapper("precass.recycle")
                .setIcon(ItemBroken.make(output))
                .inputItems(new ComparableStack(ItemBroken.make(output)))
                .outputItems(recycle).outputChances(chances);
        if(recipe.inputFluid != null) {
            row.outputFluids(new FluidStack(recipe.inputFluid[0].type, (int) Math.round(recipe.inputFluid[0].fill * fReclaim)));
        }
        return row;
    }

    /** A row registered inside 1.7's enable528 branch carries the branch. */
    private GenericRecipe branch(GenericRecipe recipe) {
        return enable528 ? recipe.requires(ENABLE_528) : recipe;
    }
}
