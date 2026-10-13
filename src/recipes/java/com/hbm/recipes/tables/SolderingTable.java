package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.ANY_HARDPLASTIC;
import static com.hbm.inventory.OreDictManager.ANY_PLASTIC;
import static com.hbm.inventory.OreDictManager.EMERALD;
import static com.hbm.inventory.OreDictManager.GOLD;
import static com.hbm.inventory.OreDictManager.LI;
import static com.hbm.inventory.OreDictManager.MINGRADE;
import static com.hbm.inventory.OreDictManager.NB;
import static com.hbm.inventory.OreDictManager.PB;
import static com.hbm.inventory.OreDictManager.RUBBER;
import static com.hbm.inventory.OreDictManager.W;
import static com.hbm.items.ItemEnums.EnumCircuitType.ADVANCED;
import static com.hbm.items.ItemEnums.EnumCircuitType.ANALOG;
import static com.hbm.items.ItemEnums.EnumCircuitType.ATOMIC_CLOCK;
import static com.hbm.items.ItemEnums.EnumCircuitType.BASIC;
import static com.hbm.items.ItemEnums.EnumCircuitType.BISMOID;
import static com.hbm.items.ItemEnums.EnumCircuitType.CAPACITOR;
import static com.hbm.items.ItemEnums.EnumCircuitType.CAPACITOR_BOARD;
import static com.hbm.items.ItemEnums.EnumCircuitType.CAPACITOR_TANTALIUM;
import static com.hbm.items.ItemEnums.EnumCircuitType.CHIP;
import static com.hbm.items.ItemEnums.EnumCircuitType.CHIP_BISMOID;
import static com.hbm.items.ItemEnums.EnumCircuitType.CHIP_QUANTUM;
import static com.hbm.items.ItemEnums.EnumCircuitType.CONTROLLER;
import static com.hbm.items.ItemEnums.EnumCircuitType.CONTROLLER_ADVANCED;
import static com.hbm.items.ItemEnums.EnumCircuitType.CONTROLLER_CHASSIS;
import static com.hbm.items.ItemEnums.EnumCircuitType.CONTROLLER_QUANTUM;
import static com.hbm.items.ItemEnums.EnumCircuitType.PCB;
import static com.hbm.items.ItemEnums.EnumCircuitType.QUANTUM;
import static com.hbm.items.ItemEnums.EnumCircuitType.VACUUM_TUBE;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.SolderingRecipes.Row;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModRadarUtilityItems;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.ModUpgradeItems;
import dev.golem.core.world.RecipeSource;
import dev.golem.core.Ids;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code SolderingRecipes.registerDefaults}. Each recipe with an {@code lbsm ? a : b} ingredient is written twice
 * ({@link #lbsm(Runnable)}): the default row with {@code "!lbsm_simple_crafting"} and the LBSM row, id
 * {@code <id>.lbsm_simple_crafting}, with {@code "lbsm_simple_crafting"}.
 */
public final class SolderingTable extends RecipeSource<Row> {

    /** 1.7 {@code "dustGlowstone"}. */
    private static final TagKey<Item> DUST_GLOWSTONE = TagKey.create(Registries.ITEM, Ids.of("c", "dusts/glowstone"));

    /** The 1.7 config branch the rows sit in (a row's {@code requires}). */
    private List<String> requires = List.of();
    /** 1.7 {@code GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleCrafting}, per branch ({@link #lbsm(Runnable)}). */
    private boolean lbsm;
    /** The id suffix of the branch being written. */
    private String suffix = "";

    public SolderingTable() {
        super(SolderingRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        // 1.7 boolean lbsm = GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleCrafting; the recipes reading it
        // are written once per branch (lbsm(...))

        /*
         * CIRCUITS
         */

        add(new SolderingRecipe(new ItemStack(ModIntermediateItems.circuit(ANALOG)), 100, 100,
                new AStack[] {
                        circuit(3, VACUUM_TUBE),
                        circuit(2, CAPACITOR)},
                new AStack[] {
                        circuit(4, PCB)},
                new AStack[] {
                        PB.wire().count(4)}
        ));

        add(new SolderingRecipe(new ItemStack(ModIntermediateItems.circuit(BASIC)), 200, 250,
                new AStack[] {
                        circuit(4, CHIP)},
                new AStack[] {
                        circuit(4, PCB)},
                new AStack[] {
                        PB.wire().count(4)}
        ));

        lbsm(() ->
            add(new SolderingRecipe(new ItemStack(ModIntermediateItems.circuit(ADVANCED)), 300, 1_000,
                    new FluidStack(Fluids.SULFURIC_ACID, 1_000),
                    new AStack[] {
                            circuit(lbsm ? 4 : 16, CHIP),
                            circuit(4, CAPACITOR)},
                    new AStack[] {
                            circuit(8, PCB),
                            RUBBER.ingot().count(2)},
                    new AStack[] {
                            PB.wire().count(8)}
            )));

        add(new SolderingRecipe(new ItemStack(ModIntermediateItems.circuit(CAPACITOR_BOARD)), 200, 300,
                new FluidStack(Fluids.PEROXIDE, 250),
                new AStack[] {
                        circuit(3, CAPACITOR_TANTALIUM)},
                new AStack[] {
                        circuit(1, PCB)},
                new AStack[] {
                        PB.wire().count(3)}
        ));

        lbsm(() ->
            add(new SolderingRecipe(new ItemStack(ModIntermediateItems.circuit(BISMOID)), 400, 10_000,
                    new FluidStack(Fluids.SOLVENT, 1_000),
                    new AStack[] {
                            circuit(4, CHIP_BISMOID),
                            circuit(lbsm ? 4 : 16, CHIP),
                            circuit(lbsm ? 8 : 24, CAPACITOR)},
                    new AStack[] {
                            circuit(12, PCB),
                            ANY_HARDPLASTIC.ingot().count(2)},
                    new AStack[] {
                            PB.wire().count(12)}
            )));

        lbsm(() ->
            add(new SolderingRecipe(new ItemStack(ModIntermediateItems.circuit(QUANTUM)), 400, 100_000,
                    new FluidStack(Fluids.HELIUM4, 1_000),
                    new AStack[] {
                            circuit(4, CHIP_QUANTUM),
                            circuit(lbsm ? 4 : 16, CHIP_BISMOID),
                            circuit(lbsm ? 1 : 4, ATOMIC_CLOCK)},
                    new AStack[] {
                            circuit(16, PCB),
                            ANY_HARDPLASTIC.ingot().count(4)},
                    new AStack[] {
                            PB.wire().count(16)}
            )));

        /*
         * COMPUTERS
         */

        // if(no528) {
        requires = List.of("!enable_528");
        lbsm(() ->
            add(new SolderingRecipe(new ItemStack(ModIntermediateItems.circuit(CONTROLLER)), 400, 15_000,
                    new FluidStack(Fluids.PERFLUOROMETHYL, 1_000),
                    new AStack[] {
                            circuit(lbsm ? 8 : 32, CHIP),
                            circuit(lbsm ? 8 : 32, CAPACITOR),
                            circuit(lbsm ? 8 : 16, CAPACITOR_TANTALIUM)},
                    new AStack[] {
                            circuit(1, CONTROLLER_CHASSIS),
                            RUBBER.ingot().count(4)},
                    new AStack[] {
                            PB.wire().count(16)}
            )));
        lbsm(() ->
            add(new SolderingRecipe(new ItemStack(ModIntermediateItems.circuit(CONTROLLER_ADVANCED)), 600, 25_000,
                    new FluidStack(Fluids.PERFLUOROMETHYL, 4_000),
                    new AStack[] {
                            circuit(lbsm ? 8 : 16, CHIP_BISMOID),
                            circuit(lbsm ? 16 : 48, CAPACITOR_TANTALIUM),
                            circuit(1, ATOMIC_CLOCK)},
                    new AStack[] {
                            circuit(1, CONTROLLER_CHASSIS),
                            ANY_HARDPLASTIC.ingot().count(4)},
                    new AStack[] {
                            PB.wire().count(24)}
            )));
        lbsm(() ->
            add(new SolderingRecipe(new ItemStack(ModIntermediateItems.circuit(CONTROLLER_QUANTUM)), 600, 250_000,
                    new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 6_000),
                    new AStack[] {
                            circuit(lbsm ? 8 : 16, CHIP_QUANTUM),
                            circuit(lbsm ? 16 : 48, CHIP_BISMOID),
                            circuit(lbsm ? 1 : 8, ATOMIC_CLOCK)},
                    new AStack[] {
                            circuit(2, CONTROLLER_ADVANCED),
                            new ComparableStack(ModUpgradeItems.upgrade_speed_3.get())},
                    new AStack[] {
                            PB.wire().count(32)}
            )));
        requires = List.of();
        // }

        add(new SolderingRecipe(new ItemStack(ModSatelliteItems.sat_chip.get()), 100, 100,
                new AStack[] {
                        circuit(8, CHIP),
                        circuit(2, CAPACITOR)},
                new AStack[] {
                        circuit(2, PCB)},
                new AStack[] {
                        PB.wire().count(4)}
        ));

        /*
         * UPGRADES
         */

        add(new SolderingRecipe(new ItemStack(ModUpgradeItems.upgrade_speed_1.get()), 200, 1_000,
                new AStack[] {circuit(4, VACUUM_TUBE), circuit(1, CAPACITOR)},
                new AStack[] {new ComparableStack(ModItems.upgrade_template.get()), MINGRADE.dust().count(4)},
                new AStack[] {}
        ));
        add(new SolderingRecipe(new ItemStack(ModUpgradeItems.upgrade_effect_1.get()), 200, 1_000,
                new AStack[] {circuit(4, VACUUM_TUBE), circuit(1, CAPACITOR)},
                new AStack[] {new ComparableStack(ModItems.upgrade_template.get()), EMERALD.dust().count(4)},
                new AStack[] {}
        ));
        add(new SolderingRecipe(new ItemStack(ModUpgradeItems.upgrade_power_1.get()), 200, 1_000,
                new AStack[] {circuit(4, VACUUM_TUBE), circuit(1, CAPACITOR)},
                new AStack[] {new ComparableStack(ModItems.upgrade_template.get()), GOLD.dust().count(4)},
                new AStack[] {}
        ));
        add(new SolderingRecipe(new ItemStack(ModUpgradeItems.upgrade_fortune_1.get()), 200, 1_000,
                new AStack[] {circuit(4, VACUUM_TUBE), circuit(1, CAPACITOR)},
                new AStack[] {new ComparableStack(ModItems.upgrade_template.get()), NB.dust().count(4)},
                new AStack[] {}
        ));
        add(new SolderingRecipe(new ItemStack(ModUpgradeItems.upgrade_afterburn_1.get()), 200, 1_000,
                new AStack[] {circuit(4, VACUUM_TUBE), circuit(1, CAPACITOR)},
                new AStack[] {new ComparableStack(ModItems.upgrade_template.get()), W.dust().count(4)},
                new AStack[] {}
        ));
        add(new SolderingRecipe(new ItemStack(ModRadarUtilityItems.upgrade_radius.get()), 200, 1_000,
                new AStack[] {circuit(4, CHIP), circuit(4, CAPACITOR)},
                new AStack[] {new ComparableStack(ModItems.upgrade_template.get()), new TagStack(DUST_GLOWSTONE, 4)},
                new AStack[] {}
        ));
        add(new SolderingRecipe(new ItemStack(ModRadarUtilityItems.upgrade_health.get()), 200, 1_000,
                new AStack[] {circuit(4, CHIP), circuit(4, CAPACITOR)},
                new AStack[] {new ComparableStack(ModItems.upgrade_template.get()), LI.dust().count(4)},
                new AStack[] {}
        ));

        // if(no528) {
        requires = List.of("!enable_528");
        addFirstUpgrade(ModUpgradeItems.upgrade_speed_1.get(), ModUpgradeItems.upgrade_speed_2.get());
        addSecondUpgrade(ModUpgradeItems.upgrade_speed_2.get(), ModUpgradeItems.upgrade_speed_3.get());
        addFirstUpgrade(ModUpgradeItems.upgrade_effect_1.get(), ModUpgradeItems.upgrade_effect_2.get());
        addSecondUpgrade(ModUpgradeItems.upgrade_effect_2.get(), ModUpgradeItems.upgrade_effect_3.get());
        addFirstUpgrade(ModUpgradeItems.upgrade_power_1.get(), ModUpgradeItems.upgrade_power_2.get());
        addSecondUpgrade(ModUpgradeItems.upgrade_power_2.get(), ModUpgradeItems.upgrade_power_3.get());
        addFirstUpgrade(ModUpgradeItems.upgrade_fortune_1.get(), ModUpgradeItems.upgrade_fortune_2.get());
        addSecondUpgrade(ModUpgradeItems.upgrade_fortune_2.get(), ModUpgradeItems.upgrade_fortune_3.get());
        addFirstUpgrade(ModUpgradeItems.upgrade_afterburn_1.get(), ModUpgradeItems.upgrade_afterburn_2.get());
        addSecondUpgrade(ModUpgradeItems.upgrade_afterburn_2.get(), ModUpgradeItems.upgrade_afterburn_3.get());
        requires = List.of();
        // }
    }

    private void addFirstUpgrade(Item lower, Item higher) {
        // 1.7 boolean lbsm = GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleCrafting;
        lbsm(() ->
            add(new SolderingRecipe(new ItemStack(higher), 300, 10_000,
                    new AStack[] {circuit(lbsm ? 4 : 8, CHIP), circuit(lbsm ? 2 : 4, CAPACITOR)},
                    new AStack[] {new ComparableStack(lower), ANY_PLASTIC.ingot().count(4)},
                    new AStack[] {}
            )));
    }

    private void addSecondUpgrade(Item lower, Item higher) {
        // 1.7 boolean lbsm = GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleCrafting;
        lbsm(() ->
            add(new SolderingRecipe(new ItemStack(higher), 400, 25_000,
                    new FluidStack(Fluids.SOLVENT, 500),
                    new AStack[] {circuit(lbsm ? 6 : 16, CHIP), circuit(lbsm ? 4 : 16, CAPACITOR)},
                    new AStack[] {new ComparableStack(lower), RUBBER.ingot().count(4)},
                    new AStack[] {}
            )));
    }

    /**
     * 1.7's {@code lbsm ? a : b} recipes: {@code rows} runs once with {@code lbsm} false (the default rows,
     * {@code "!lbsm_simple_crafting"}) and once with it true ({@code "lbsm_simple_crafting"}, id suffix
     * {@code .lbsm_simple_crafting}), each inside the current branch ({@code !enable_528} for the computers and the
     * tier II/III upgrades).
     */
    private void lbsm(Runnable rows) {
        List<String> outer = requires;
        lbsm = false;
        requires = with(outer, "!lbsm_simple_crafting");
        rows.run();
        lbsm = true;
        requires = with(outer, "lbsm_simple_crafting");
        suffix = ".lbsm_simple_crafting";
        rows.run();
        lbsm = false;
        requires = outer;
        suffix = "";
    }

    private static List<String> with(List<String> requires, String flag) {
        List<String> all = new ArrayList<>(requires);
        all.add(flag);
        return List.copyOf(all);
    }

    /** 1.7 {@code SolderingRecipe}. */
    private record SolderingRecipe(ItemStack output, int duration, long consumption, FluidStack fluid,
                                   AStack[] toppings, AStack[] pcb, AStack[] solder) {

        SolderingRecipe(ItemStack output, int duration, long consumption, AStack[] toppings, AStack[] pcb, AStack[] solder) {
            this(output, duration, consumption, null, toppings, pcb, solder);
        }
    }

    /** 1.7 {@code recipes.add(recipe)}; the id is the output's item name. */
    private void add(SolderingRecipe recipe) {
        register(BuiltInRegistries.ITEM.getKey(recipe.output().getItem()).getPath() + suffix,
                new Row(ItemSpec.of(recipe.output()).getOrThrow(), recipe.duration(), recipe.consumption(),
                        Optional.ofNullable(recipe.fluid()), List.of(recipe.toppings()), List.of(recipe.pcb()),
                        List.of(recipe.solder()), requires, nextOrder()));
    }

    /** 1.7 {@code new ComparableStack(ModItems.circuit, count, type)}. */
    private static AStack circuit(int count, EnumCircuitType type) {
        return new ComparableStack(ModIntermediateItems.circuit(type), count);
    }
}
