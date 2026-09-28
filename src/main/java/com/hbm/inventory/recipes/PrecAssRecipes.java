package com.hbm.inventory.recipes;

import com.hbm.config.GeneralConfig;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.machine.ItemBlueprintFolder;
import com.hbm.items.machine.ItemBroken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;

import static com.hbm.inventory.OreDictManager.*;
import static com.hbm.items.ItemEnums.EnumCircuitType.*;

/** Precision assembler recipes, including the legacy 528-mode products and recycling path. */
public final class PrecAssRecipes extends GenericRecipes {

    public static final PrecAssRecipes INSTANCE = new PrecAssRecipes();

    private PrecAssRecipes() {}

    /** The 528 switch the list was last built with; a change rebuilds it (a 1.20.1 server builds the creative tabs, and so this list, before any world exists). */
    private Boolean builtWith528;

    @Override
    public synchronized void registerRecipes() {
        if (!recipeOrderedList.isEmpty() && builtWith528 != null && builtWith528 == GeneralConfig.enable528) return;
        recipeOrderedList.clear();
        recipeNameMap.clear();
        blueprintPools.values().forEach(pool -> pool.removeIf(name -> name.startsWith("precass.")));
        builtWith528 = GeneralConfig.enable528;

        // The central 528 switch is false by default in old-hbm. Keep these recipes behind the same
        // content switch instead of silently exposing a second circuit progression.
        if (GeneralConfig.enable528) {
            registerPair(new GenericRecipe("precass.chip").setup(100, 200L)
                            .inputItems(cir(SILICON, 1), new ComparableStack(ModItems.plate_polymer.get(), 3), GOLD.wire().count(4))
                            .setPools(GenericRecipes.POOL_PREFIX_528 + "chip"),
                    ModIntermediateItems.circuit(CHIP, 1), 90, GeneralConfig.enableExpensiveMode ? 50 : 90);
            registerPair(new GenericRecipe("precass.chip_bismoid").setup(200, 1_000L)
                            .inputItems(cir(SILICON, 4), new ComparableStack(ModItems.plate_polymer.get(), 8), ANY_BISMOID.nugget().count(2), GOLD.wire().count(4))
                            .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 500)).setPools(GenericRecipes.POOL_PREFIX_528 + "chip_bismoid"),
                    ModIntermediateItems.circuit(CHIP_BISMOID, 1), 75, GeneralConfig.enableExpensiveMode ? 50 : 75);
            registerPair(new GenericRecipe("precass.chip_quantum").setup(300, 20_000L)
                            .inputItems(cir(SILICON, 8), BSCCO.wireDense().count(2), ANY_HARDPLASTIC.ingot().count(4),
                                    new ComparableStack(ModItems.pellet_charged.get(), 4), GOLD.wire().count(8))
                            .inputFluids(new FluidStack(Fluids.HELIUM4, 250)).setPools(GenericRecipes.POOL_PREFIX_528 + "chip_quantum"),
                    ModIntermediateItems.circuit(CHIP_QUANTUM, 1), 90, GeneralConfig.enableExpensiveMode ? 50 : 75);
            registerPair(new GenericRecipe("precass.atomic_clock").setup(200, 2_000L)
                            .inputItems(cir(CHIP, 8), ANY_PLASTIC.ingot().count(4), ZR.wire().count(8), SR.dust())
                            .setPools(GenericRecipes.POOL_PREFIX_528 + "strontium"),
                    ModIntermediateItems.circuit(ATOMIC_CLOCK, 1), 50, GeneralConfig.enableExpensiveMode ? 50 : 75);
            registerPair(new GenericRecipe("precass.controller").setup(400, 15_000L)
                            .inputItems(cir(CHIP, 32), cir(CAPACITOR, 32), cir(CAPACITOR_TANTALIUM, 16), cir(CONTROLLER_CHASSIS, 1),
                                    new ComparableStack(ModUpgradeItems.upgrade_speed_1.get()), PB.wire().count(16))
                            .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 1_000)),
                    ModIntermediateItems.circuit(CONTROLLER, 1), 75, GeneralConfig.enableExpensiveMode ? 50 : 90);
            registerPair(new GenericRecipe("precass.controller_advanced").setup(600, 25_000L)
                            .inputItems(cir(CHIP_BISMOID, 16), cir(CAPACITOR_TANTALIUM, 48), cir(ATOMIC_CLOCK, 1), cir(CONTROLLER_CHASSIS, 1),
                                    new ComparableStack(ModUpgradeItems.upgrade_speed_3.get()), PB.wire().count(24))
                            .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 4_000)),
                    ModIntermediateItems.circuit(CONTROLLER_ADVANCED, 1), 50, GeneralConfig.enableExpensiveMode ? 50 : 75);
            registerPair(new GenericRecipe("precass.controller_quantum").setup(600, 250_000L)
                            .inputItems(cir(CHIP_QUANTUM, 16), cir(CHIP_BISMOID, 48), cir(ATOMIC_CLOCK, 8), cir(CONTROLLER_ADVANCED, 2),
                                    new ComparableStack(ModUpgradeItems.upgrade_overdrive_1.get()), PB.wire().count(32))
                            .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 6_000)),
                    ModIntermediateItems.circuit(CONTROLLER_QUANTUM, 1), 75, GeneralConfig.enableExpensiveMode ? 50 : 75);

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
                            .inputItems(new ComparableStack(ModUpgradeItems.upgrade_speed_3.get()), new ComparableStack(ModUpgradeItems.upgrade_effect_3.get()),
                                    BIGMT.ingot().count(16), ANY_HARDPLASTIC.ingot().count(16), cir(ADVANCED, 16)),
                    new ItemStack(ModUpgradeItems.upgrade_overdrive_1.get()), 50, GeneralConfig.enableExpensiveMode ? 50 : 75);
            registerPair(new GenericRecipe("precass.upgrade_overdive_ii").setup(600, 5_000)
                            .inputItems(new ComparableStack(ModUpgradeItems.upgrade_overdrive_1.get()), new ComparableStack(ModUpgradeItems.upgrade_speed_3.get()),
                                    new ComparableStack(ModUpgradeItems.upgrade_effect_3.get()), BIGMT.ingot().count(16), new ComparableStack(ModItems.ingot_cft.get(), 8),
                                    cir(CAPACITOR_BOARD, 16)),
                    new ItemStack(ModUpgradeItems.upgrade_overdrive_2.get()), 50, GeneralConfig.enableExpensiveMode ? 50 : 75);
            registerPair(new GenericRecipe("precass.upgrade_overdive_iii").setup(1_200, 100_000)
                            .inputItems(new ComparableStack(ModUpgradeItems.upgrade_overdrive_2.get()), new ComparableStack(ModUpgradeItems.upgrade_speed_3.get()),
                                    new ComparableStack(ModUpgradeItems.upgrade_effect_3.get()), BBRONZE.ingot().count(16), new ComparableStack(ModItems.ingot_cft.get(), 16),
                                    cir(BISMOID, 16)),
                    new ItemStack(ModUpgradeItems.upgrade_overdrive_3.get()), 25, GeneralConfig.enableExpensiveMode ? 50 : 75);
        }

        // These two are outside the old enable528 block. Their folder output is now represented by the
        // component-backed ItemBlueprintFolder/ItemBlueprints pair.
        registerChoices(new GenericRecipe("precass.blueprints").setup(5 * 1_200, 20_000L)
                        .inputItems(new ComparableStack(Items.PAPER, 16), blueDye(16),
                                new ComparableStack(Items.PUFFERFISH, 4)),
                ItemBlueprintFolder.make(0), 10, new ItemStack(Items.PAPER, 16), 90);
        registerChoices(new GenericRecipe("precass.beigeprints").setup(5 * 1_200, 50_000L)
                        .inputItems(new ComparableStack(Items.PAPER, 24), CINNABAR.gem().count(24),
                                new ComparableStack(Items.PUFFERFISH, 8)),
                ItemBlueprintFolder.make(1), 5, new ItemStack(Items.PAPER, 24), 95);
    }

    private static ComparableStack cir(EnumCircuitType type, int count) {
        return new ComparableStack(ModIntermediateItems.circuit(type), count);
    }

    private static TagStack blueDye(int count) {
        return new TagStack(TagKey.create(Registries.ITEM, dev.golem.core.Ids.of("c", "dyes/blue")), count);
    }

    private void addFirstUpgrade(Item lower, Item higher, String name) {
        registerPair(new GenericRecipe(name).setup(300, 10_000)
                        .inputItems(cir(CHIP, 8), cir(CAPACITOR_TANTALIUM, 4), new ComparableStack(lower), ANY_PLASTIC.ingot().count(4)),
                new ItemStack(higher), 50, 75); // 1.7: "upgrades are now actually valuable"
    }

    private void addSecondUpgrade(Item lower, Item higher, String name) {
        registerPair(new GenericRecipe(name).setup(400, 25_000)
                        .inputItems(cir(CHIP, 16), cir(CAPACITOR_TANTALIUM, 16), new ComparableStack(lower), RUBBER.ingot().count(4))
                        .inputFluids(new FluidStack(Fluids.SOLVENT, 500)),
                new ItemStack(higher), 25, 75); // 1.7: "admittedly this one's just me being a dick"
    }

    private void registerChoices(GenericRecipe recipe, ItemStack first, float firstWeight, ItemStack second, float secondWeight) {
        recipe.outputChoices(new GenericRecipe.OutputChoice(first, firstWeight),
                new GenericRecipe.OutputChoice(second, secondWeight));
        register(recipe);
    }

    /** Registers a product-or-broken weighted output and its per-input recycling recipe. */
    private void registerPair(GenericRecipe recipe, ItemStack output, int chance, int reclaim) {
        ItemStack broken = ItemBroken.make(output);
        recipe.outputChoices(new GenericRecipe.OutputChoice(output.copy(), chance),
                new GenericRecipe.OutputChoice(broken, 100 - chance));
        register(recipe);

        ItemStack[] recycle = new ItemStack[recipe.inputItem.length];
        float[] recycleChance = new float[recycle.length];
        for (int i = 0; i < recycle.length; i++) {
            recycle[i] = recipe.inputItem[i].getDisplayStack().copy();
            recycleChance[i] = reclaim / 100F;
        }
        FluidStack[] fluid = recipe.inputFluid == null ? null : new FluidStack[]{
                new FluidStack(recipe.inputFluid[0].type, Math.round(recipe.inputFluid[0].fill * reclaim / 100F))};
        GenericRecipe recycleRecipe = new GenericRecipe(recipe.getInternalName() + ".recycle")
                .setup(recipe.duration, recipe.power)
                .setNameWrapper("precass.recycle")
                .setIcon(broken)
                .inputItems(new ComparableStack(broken))
                .outputItems(recycle)
                .outputChances(recycleChance);
        if (fluid != null) recycleRecipe.outputFluids(fluid);
        register(recycleRecipe);
    }
}
