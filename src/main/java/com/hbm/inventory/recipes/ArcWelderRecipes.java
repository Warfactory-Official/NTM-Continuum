package com.hbm.inventory.recipes;

import com.hbm.blocks.ModSteelBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ItemEnums.EnumPartType;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModMissileItems;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.hbm.inventory.OreDictManager.*;

/** The feasible 1.12.2 Arc Welder catalogue and its unordered three-slot matcher. */
public final class ArcWelderRecipes {

    public static final List<ArcWelderRecipe> recipes = new ArrayList<>();
    private static final TagKey<Item> ANY_HIGH_EXPLOSIVE = TagKey.create(
            Registries.ITEM, Ids.of("c", "ingots/any_highexplosive"));
    private static boolean initialized;

    private ArcWelderRecipes() {}

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;

        // Parts. Fiberglass is a single current item, so it retains the old FIBER ore-dictionary meaning.
        add(new ItemStack(ModItems.motor.get(), 2), 100, 400,
                STEEL.plate().count(2), MINGRADE.wireDense().count(2));
        add(ModIngredientItems.part(EnumPartType.LDE, 1), 200, 5_000,
                AL.plate().count(4), new ComparableStack(ModItems.ingot_fiberglass.get(), 4),
                ANY_HARDPLASTIC.ingot());
        add(ModIngredientItems.part(EnumPartType.LDE, 1), 200, 10_000,
                TI.plate().count(2), new ComparableStack(ModItems.ingot_fiberglass.get(), 4),
                ANY_HARDPLASTIC.ingot());
        // ANY_BISMOIDBRONZE is not a current aggregate; the established port uses its bismuth-bronze member.
        add(ModIngredientItems.part(EnumPartType.HDE, 1), 600, 25_000_000,
                new FluidStack(Fluids.STELLAR_FLUX, 4_000), BBRONZE.plateCast().count(2),
                CMB.plateWelded(), new ComparableStack(ModItems.ingot_cft.get()));

        // Dense wires.
        addDenseWire(Mats.MAT_COPPER, 100, 10_000, CU.wire().count(8));
        addDenseWire(Mats.MAT_RED_COPPER, 100, 10_000, MINGRADE.wire().count(8));
        addDenseWire(Mats.MAT_GOLD, 100, 10_000, GOLD.wire().count(8));

        // Welded plates.
        addWelded(Mats.MAT_IRON, 100, 100, null);
        addWelded(Mats.MAT_STEEL, 100, 500, null);
        addWelded(Mats.MAT_COPPER, 200, 1_000, null);
        addWelded(Mats.MAT_TITANIUM, 600, 50_000, null);
        addWelded(Mats.MAT_ZIRCONIUM, 600, 10_000, null);
        addWelded(Mats.MAT_ALUMINIUM, 300, 10_000, null);
        addWelded(Mats.MAT_TCALLOY, 1_200, 1_000_000, new FluidStack(Fluids.OXYGEN, 1_000));
        addWelded(Mats.MAT_CDALLOY, 1_200, 1_000_000, new FluidStack(Fluids.OXYGEN, 1_000));
        addWelded(Mats.MAT_TUNGSTEN, 1_200, 250_000, new FluidStack(Fluids.OXYGEN, 1_000));
        addWelded(Mats.MAT_CMB, 1_200, 10_000_000, new FluidStack(Fluids.REFORMGAS, 1_000));
        addWelded(Mats.MAT_OSMIRIDIUM, 6_000, 20_000_000, new FluidStack(Fluids.REFORMGAS, 16_000));

        // Missile parts.
        add(new ItemStack(ModItems.thruster_small.get()), 60, 1_000,
                STEEL.plate().count(4), AL.wire().count(4), CU.plate().count(4));
        add(new ItemStack(ModItems.thruster_medium.get()), 100, 2_000,
                STEEL.plate().count(8), new ComparableStack(ModItems.motor.get()), Mats.MAT_GRAPHITE.ingot().count(8));
        add(new ItemStack(ModItems.thruster_large.get()), 200, 5_000,
                DURA.ingot().count(10), new ComparableStack(ModItems.motor.get()),
                new ComparableStack(ModItems.neutron_reflector.get(), 12));
        add(new ItemStack(ModItems.fuel_tank_small.get()), 60, 1_000,
                AL.plate().count(6), CU.plate().count(4),
                new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 4));
        add(new ItemStack(ModItems.fuel_tank_medium.get()), 100, 2_000,
                AL.plateCast().count(4), TI.plate().count(8),
                new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 12));
        add(new ItemStack(ModItems.fuel_tank_large.get()), 200, 5_000,
                AL.plateWelded().count(8), BIGMT.plate().count(12),
                new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 16));

        // Missiles.
        add(new ItemStack(ModMissileItems.missile_anti_ballistic.get()), 100, 5_000,
                new TagStack(ANY_HIGH_EXPLOSIVE, 3), new ComparableStack(ModItems.missile_assembly.get()),
                new ComparableStack(ModItems.thruster_small.get(), 4));
        add(new ItemStack(ModMissileItems.missile_generic.get()), 100, 5_000,
                new ComparableStack(ModItems.warhead_generic_small.get()),
                new ComparableStack(ModItems.fuel_tank_small.get()), new ComparableStack(ModItems.thruster_small.get()));
        add(new ItemStack(ModMissileItems.missile_incendiary.get()), 100, 5_000,
                new ComparableStack(ModItems.warhead_incendiary_small.get()),
                new ComparableStack(ModItems.fuel_tank_small.get()), new ComparableStack(ModItems.thruster_small.get()));
        add(new ItemStack(ModMissileItems.missile_cluster.get()), 100, 5_000,
                new ComparableStack(ModItems.warhead_cluster_small.get()),
                new ComparableStack(ModItems.fuel_tank_small.get()), new ComparableStack(ModItems.thruster_small.get()));
        add(new ItemStack(ModMissileItems.missile_buster.get()), 100, 5_000,
                new ComparableStack(ModItems.warhead_buster_small.get()),
                new ComparableStack(ModItems.fuel_tank_small.get()), new ComparableStack(ModItems.thruster_small.get()));
        add(new ItemStack(ModMissileItems.missile_decoy.get()), 60, 2_500,
                STEEL.ingot(), new ComparableStack(ModItems.fuel_tank_small.get()),
                new ComparableStack(ModItems.thruster_small.get()));

        add(new ItemStack(ModMissileItems.missile_strong.get()), 200, 10_000,
                new ComparableStack(ModItems.warhead_generic_medium.get()),
                new ComparableStack(ModItems.fuel_tank_medium.get()), new ComparableStack(ModItems.thruster_medium.get()));
        add(new ItemStack(ModMissileItems.missile_incendiary_strong.get()), 200, 10_000,
                new ComparableStack(ModItems.warhead_incendiary_medium.get()),
                new ComparableStack(ModItems.fuel_tank_medium.get()), new ComparableStack(ModItems.thruster_medium.get()));
        add(new ItemStack(ModMissileItems.missile_cluster_strong.get()), 200, 10_000,
                new ComparableStack(ModItems.warhead_cluster_medium.get()),
                new ComparableStack(ModItems.fuel_tank_medium.get()), new ComparableStack(ModItems.thruster_medium.get()));
        add(new ItemStack(ModMissileItems.missile_buster_strong.get()), 200, 10_000,
                new ComparableStack(ModItems.warhead_buster_medium.get()),
                new ComparableStack(ModItems.fuel_tank_medium.get()), new ComparableStack(ModItems.thruster_medium.get()));
        // The old missile_emp_strong entry is omitted: its emp_bomb block has no current ID.

        add(new ItemStack(ModMissileItems.missile_burst.get()), 300, 25_000,
                new ComparableStack(ModItems.warhead_generic_large.get()),
                new ComparableStack(ModItems.fuel_tank_medium.get(), 2),
                new ComparableStack(ModItems.thruster_medium.get(), 4));
        add(new ItemStack(ModMissileItems.missile_inferno.get()), 300, 25_000,
                new ComparableStack(ModItems.warhead_incendiary_large.get()),
                new ComparableStack(ModItems.fuel_tank_medium.get(), 2),
                new ComparableStack(ModItems.thruster_medium.get(), 4));
        add(new ItemStack(ModMissileItems.missile_rain.get()), 300, 25_000,
                new ComparableStack(ModItems.warhead_cluster_large.get()),
                new ComparableStack(ModItems.fuel_tank_medium.get(), 2),
                new ComparableStack(ModItems.thruster_medium.get(), 4));
        add(new ItemStack(ModMissileItems.missile_drill.get()), 300, 25_000,
                new ComparableStack(ModItems.warhead_buster_large.get()),
                new ComparableStack(ModItems.fuel_tank_medium.get(), 2),
                new ComparableStack(ModItems.thruster_medium.get(), 4));

        add(new ItemStack(ModMissileItems.missile_nuclear.get()), 600, 50_000,
                new ComparableStack(ModItems.warhead_nuclear.get()),
                new ComparableStack(ModItems.fuel_tank_large.get()), new ComparableStack(ModItems.thruster_large.get(), 3));
        add(new ItemStack(ModMissileItems.missile_nuclear_cluster.get()), 600, 50_000,
                new ComparableStack(ModItems.warhead_mirv.get()),
                new ComparableStack(ModItems.fuel_tank_large.get()), new ComparableStack(ModItems.thruster_large.get(), 3));
        add(new ItemStack(ModMissileItems.missile_volcano.get()), 600, 50_000,
                new ComparableStack(ModItems.warhead_volcano.get()),
                new ComparableStack(ModItems.fuel_tank_large.get()), new ComparableStack(ModItems.thruster_large.get(), 3));

        // The five satellite outputs remain deferred: the current tree has the deco blocks but no
        // corresponding sat_mapper/sat_scanner/sat_radar/sat_laser/sat_resonator item IDs.
    }

    private static void addDenseWire(NTMMaterial material, int duration, long consumption, AStack input) {
        Item output = ModAutogenItems.get(MaterialShapes.DENSEWIRE, material);
        if (output != null) add(new ItemStack(output), duration, consumption, input);
    }

    private static void addWelded(NTMMaterial material, int duration, long consumption, FluidStack fluid) {
        Item output = ModAutogenItems.get(MaterialShapes.WELDEDPLATE, material);
        if (output != null) {
            if (fluid == null) add(new ItemStack(output), duration, consumption, material.plateCast().count(2));
            else add(new ItemStack(output), duration, consumption, fluid, material.plateCast().count(2));
        }
    }

    private static void add(ItemStack output, int duration, long consumption, AStack... ingredients) {
        recipes.add(new ArcWelderRecipe(output, duration, consumption, null, ingredients));
    }

    private static void add(ItemStack output, int duration, long consumption, FluidStack fluid, AStack... ingredients) {
        recipes.add(new ArcWelderRecipe(output, duration, consumption, fluid, ingredients));
    }

    public static ArcWelderRecipe getRecipe(ItemStack... inputs) {
        register();
        outer:
        for (ArcWelderRecipe recipe : recipes) {
            List<AStack> remaining = new ArrayList<>(Arrays.asList(recipe.ingredients));
            for (ItemStack input : inputs) {
                if (input.isEmpty()) continue;
                AStack matched = null;
                for (AStack ingredient : remaining) {
                    if (ingredient.matchesRecipe(input, true) && input.getCount() >= ingredient.stacksize) {
                        matched = ingredient;
                        break;
                    }
                }
                if (matched == null) continue outer;
                remaining.remove(matched);
            }
            if (remaining.isEmpty()) return recipe;
        }
        return null;
    }

    public static final class ArcWelderRecipe {
        public final AStack[] ingredients;
        public final FluidStack fluid;
        public final ItemStack output;
        public final int duration;
        public final long consumption;

        public ArcWelderRecipe(ItemStack output, int duration, long consumption, FluidStack fluid,
                               AStack... ingredients) {
            this.ingredients = ingredients;
            this.fluid = fluid;
            this.output = output;
            this.duration = duration;
            this.consumption = consumption;
        }

        public ArcWelderRecipe(ItemStack output, int duration, long consumption, AStack... ingredients) {
            this(output, duration, consumption, null, ingredients);
        }
    }
}
