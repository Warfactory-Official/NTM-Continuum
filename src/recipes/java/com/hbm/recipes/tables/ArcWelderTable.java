package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.blocks.ModBombs2Blocks;
import com.hbm.blocks.ModSteelBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.ArcWelderRecipes;
import com.hbm.inventory.recipes.ArcWelderRecipes.Row;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.items.ItemEnums.EnumPartType;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModMissileItems;
import dev.golem.core.world.RecipeSource;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code ArcWelderRecipes.registerDefaults}. */
public final class ArcWelderTable extends RecipeSource<Row> {

    private static final NTMMaterial GRAPHITE = Mats.MAT_GRAPHITE;
    /** 1.7 {@code DictGroup ANY_HIGHEXPLOSIVE}, not among the port's {@code OreDictManager} groups: its aggregate tag. */
    private static final NTMMaterial ANY_HIGHEXPLOSIVE = new NTMMaterial(-1, "any_highexplosive");
    /** 1.7 {@code FIBER} ("Fiberglass"): {@code c:ingots/fiberglass}, 1.7's members. */
    private static final NTMMaterial FIBER = new NTMMaterial(-1, "fiberglass");
    /** 1.7 {@code WC} ("TungstenCarbide"): {@code c:ingots/tungsten_carbide}, 1.7's members. */
    private static final NTMMaterial WC = new NTMMaterial(-1, "tungsten_carbide");

    public ArcWelderTable() {
        super(ArcWelderRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        //Parts
        add(new ArcWelderRecipe(new ItemStack(ModItems.motor.get(), 2), 100, 400L,
                STEEL.plate().count(2), MINGRADE.wireDense().count(2)));
        add(new ArcWelderRecipe(ModIngredientItems.part(EnumPartType.LDE, 1), 200, 5_000L,
                AL.plate().count(4), FIBER.ingot().count(4), ANY_HARDPLASTIC.ingot()));
        add("part_generic_lde_plates_titanium", new ArcWelderRecipe(ModIngredientItems.part(EnumPartType.LDE, 1), 200, 10_000L, // a second row of one output: its id also names its first ingredient
                TI.plate().count(2), FIBER.ingot().count(4), ANY_HARDPLASTIC.ingot()));
        add(new ArcWelderRecipe(new ItemStack(ModItems.neutron_reflector.get(), 2), 400, 50_000L,
                WC.ingot().count(2), DURA.plate()));

        //Dense Wires
        add(new ArcWelderRecipe(autogen(MaterialShapes.DENSEWIRE, Mats.MAT_COPPER), 100, 10_000L,
                CU.wire().count(8)));
        add(new ArcWelderRecipe(autogen(MaterialShapes.DENSEWIRE, Mats.MAT_RED_COPPER), 100, 10_000L,
                MINGRADE.wire().count(8)));
        add(new ArcWelderRecipe(autogen(MaterialShapes.DENSEWIRE, Mats.MAT_GOLD), 100, 10_000L,
                GOLD.wire().count(8)));

        //earlygame welded parts
        add(new ArcWelderRecipe(autogen(MaterialShapes.WELDEDPLATE, Mats.MAT_IRON), 100, 100L,
                IRON.plateCast().count(2)));
        //high-demand mid-game parts
        add(new ArcWelderRecipe(autogen(MaterialShapes.WELDEDPLATE, Mats.MAT_STEEL), 100, 500L,
                STEEL.plateCast().count(2)));
        //literally just the combination oven
        add(new ArcWelderRecipe(autogen(MaterialShapes.WELDEDPLATE, Mats.MAT_COPPER), 200, 1_000L,
                CU.plateCast().count(2)));
        //mid-game, single combustion engine running on LPG
        add(new ArcWelderRecipe(autogen(MaterialShapes.WELDEDPLATE, Mats.MAT_TITANIUM), 600, 50_000L,
                TI.plateCast().count(2)));
        //mid-game PWR
        add(new ArcWelderRecipe(autogen(MaterialShapes.WELDEDPLATE, Mats.MAT_ZIRCONIUM), 600, 10_000L,
                ZR.plateCast().count(2)));
        add(new ArcWelderRecipe(autogen(MaterialShapes.WELDEDPLATE, Mats.MAT_ALUMINIUM), 300, 10_000L,
                AL.plateCast().count(2)));
        //late-game fusion
        add(new ArcWelderRecipe(autogen(MaterialShapes.WELDEDPLATE, Mats.MAT_TCALLOY), 1_200, 1_000_000L, new FluidStack(Fluids.OXYGEN, 1_000),
                TCALLOY.plateCast().count(2)));
        add(new ArcWelderRecipe(autogen(MaterialShapes.WELDEDPLATE, Mats.MAT_CDALLOY), 1_200, 1_000_000L, new FluidStack(Fluids.OXYGEN, 1_000),
                CDALLOY.plateCast().count(2)));
        add(new ArcWelderRecipe(autogen(MaterialShapes.WELDEDPLATE, Mats.MAT_TUNGSTEN), 1_200, 250_000L, new FluidStack(Fluids.OXYGEN, 1_000),
                W.plateCast().count(2)));
        add(new ArcWelderRecipe(autogen(MaterialShapes.WELDEDPLATE, Mats.MAT_CMB), 1_200, 10_000_000L, new FluidStack(Fluids.REFORMGAS, 1_000),
                CMB.plateCast().count(2)));
        //pre-DFC
        add(new ArcWelderRecipe(autogen(MaterialShapes.WELDEDPLATE, Mats.MAT_OSMIRIDIUM), 6_000, 50_000_000L, new FluidStack(Fluids.REFORMGAS, 16_000),
                OSMIRIDIUM.plateCast().count(2)));

        //Missile Parts
        add(new ArcWelderRecipe(new ItemStack(ModItems.thruster_small.get()), 60, 1_000L, STEEL.plate().count(4), AL.wire().count(4), CU.plate().count(4)));
        add(new ArcWelderRecipe(new ItemStack(ModItems.thruster_medium.get()), 100, 2_000L, STEEL.plate().count(8), new ComparableStack(ModItems.motor.get(), 1), GRAPHITE.ingot().count(8)));
        add(new ArcWelderRecipe(new ItemStack(ModItems.thruster_large.get()), 200, 5_000L, DURA.ingot().count(10), new ComparableStack(ModItems.motor.get(), 1), WC.ingot().count(12)));

        add(new ArcWelderRecipe(new ItemStack(ModItems.fuel_tank_small.get()), 60, 1_000L, AL.plate().count(6), CU.plate().count(4), new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 4)));
        add(new ArcWelderRecipe(new ItemStack(ModItems.fuel_tank_medium.get()), 100, 2_000L, AL.plateCast().count(4), TI.plate().count(8), new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 12)));
        add(new ArcWelderRecipe(new ItemStack(ModItems.fuel_tank_large.get()), 200, 5_000L, AL.plateWelded().count(8), BIGMT.plate().count(12), new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 16)));

        //Missiles
        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_anti_ballistic.get()), 100, 5_000L, ANY_HIGHEXPLOSIVE.ingot().count(3), new ComparableStack(ModItems.missile_assembly.get()), new ComparableStack(ModItems.thruster_small.get(), 4)));
        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_generic.get()), 100, 5_000L, new ComparableStack(ModItems.warhead_generic_small.get()), new ComparableStack(ModItems.fuel_tank_small.get()), new ComparableStack(ModItems.thruster_small.get())));
        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_incendiary.get()), 100, 5_000L, new ComparableStack(ModItems.warhead_incendiary_small.get()), new ComparableStack(ModItems.fuel_tank_small.get()), new ComparableStack(ModItems.thruster_small.get())));
        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_cluster.get()), 100, 5_000L, new ComparableStack(ModItems.warhead_cluster_small.get()), new ComparableStack(ModItems.fuel_tank_small.get()), new ComparableStack(ModItems.thruster_small.get())));
        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_buster.get()), 100, 5_000L, new ComparableStack(ModItems.warhead_buster_small.get()), new ComparableStack(ModItems.fuel_tank_small.get()), new ComparableStack(ModItems.thruster_small.get())));
        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_decoy.get()), 60, 2_500L, STEEL.ingot(), new ComparableStack(ModItems.fuel_tank_small.get()), new ComparableStack(ModItems.thruster_small.get())));

        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_strong.get()), 200, 10_000L, new ComparableStack(ModItems.warhead_generic_medium.get()), new ComparableStack(ModItems.fuel_tank_medium.get()), new ComparableStack(ModItems.thruster_medium.get())));
        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_incendiary_strong.get()), 200, 10_000L, new ComparableStack(ModItems.warhead_incendiary_medium.get()), new ComparableStack(ModItems.fuel_tank_medium.get()), new ComparableStack(ModItems.thruster_medium.get())));
        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_cluster_strong.get()), 200, 10_000L, new ComparableStack(ModItems.warhead_cluster_medium.get()), new ComparableStack(ModItems.fuel_tank_medium.get()), new ComparableStack(ModItems.thruster_medium.get())));
        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_buster_strong.get()), 200, 10_000L, new ComparableStack(ModItems.warhead_buster_medium.get()), new ComparableStack(ModItems.fuel_tank_medium.get()), new ComparableStack(ModItems.thruster_medium.get())));
        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_emp_strong.get()), 200, 10_000L, new ComparableStack(ModBombs2Blocks.emp_bomb.get().asItem(), 3), new ComparableStack(ModItems.fuel_tank_medium.get()), new ComparableStack(ModItems.thruster_medium.get())));

        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_burst.get()), 300, 25_000L, new ComparableStack(ModItems.warhead_generic_large.get()), new ComparableStack(ModItems.fuel_tank_medium.get(), 2), new ComparableStack(ModItems.thruster_medium.get(), 4)));
        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_inferno.get()), 300, 25_000L, new ComparableStack(ModItems.warhead_incendiary_large.get()), new ComparableStack(ModItems.fuel_tank_medium.get(), 2), new ComparableStack(ModItems.thruster_medium.get(), 4)));
        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_rain.get()), 300, 25_000L, new ComparableStack(ModItems.warhead_cluster_large.get()), new ComparableStack(ModItems.fuel_tank_medium.get(), 2), new ComparableStack(ModItems.thruster_medium.get(), 4)));
        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_drill.get()), 300, 25_000L, new ComparableStack(ModItems.warhead_buster_large.get()), new ComparableStack(ModItems.fuel_tank_medium.get(), 2), new ComparableStack(ModItems.thruster_medium.get(), 4)));

        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_nuclear.get()), 600, 50_000L, new ComparableStack(ModItems.warhead_nuclear.get()), new ComparableStack(ModItems.fuel_tank_large.get()), new ComparableStack(ModItems.thruster_large.get(), 3)));
        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_nuclear_cluster.get()), 600, 50_000L, new ComparableStack(ModItems.warhead_mirv.get()), new ComparableStack(ModItems.fuel_tank_large.get()), new ComparableStack(ModItems.thruster_large.get(), 3)));
        add(new ArcWelderRecipe(new ItemStack(ModMissileItems.missile_volcano.get()), 600, 50_000L, new ComparableStack(ModItems.warhead_volcano.get()), new ComparableStack(ModItems.fuel_tank_large.get()), new ComparableStack(ModItems.thruster_large.get(), 3)));
    }

    /** 1.7 {@code ArcWelderRecipe}. */
    private record ArcWelderRecipe(ItemStack output, int duration, long consumption, FluidStack fluid, AStack... ingredients) {

        ArcWelderRecipe(ItemStack output, int duration, long consumption, AStack... ingredients) {
            this(output, duration, consumption, null, ingredients);
        }
    }

    /** 1.7 {@code recipes.add(recipe)}; the id is the output's item name. */
    private void add(ArcWelderRecipe recipe) {
        add(BuiltInRegistries.ITEM.getKey(recipe.output().getItem()).getPath(), recipe);
    }

    private void add(String id, ArcWelderRecipe recipe) {
        register(id, new Row(List.of(recipe.ingredients()), Optional.ofNullable(recipe.fluid()),
                ItemSpec.of(recipe.output()).getOrThrow(), recipe.duration(), recipe.consumption(), nextOrder()));
    }

    /** 1.7 {@code new ItemStack(ModItems.wire_dense / plate_welded, 1, mat.id)}: the autogen item of that shape. */
    private static ItemStack autogen(MaterialShapes shape, NTMMaterial material) {
        return new ItemStack(ModAutogenItems.get(shape, material));
    }
}
