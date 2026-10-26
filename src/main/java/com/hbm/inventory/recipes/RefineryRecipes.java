package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.loader.FluidRecipeCodecs;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/**
 * Oil refinery and vacuum distiller recipes (1.7 {@code RefineryRecipes}, which holds both), as two datapack tables:
 * <ul>
 *   <li>{@code hbm:refinery} in {@code data/hbm/recipe/refinery/}: {@code {"input": fluid, "outputs": [four {type, amount}],
 *       "byproduct": item}} - per 100 mB of hot input the four fractions, plus the item emitted every 100 operations;</li>
 *   <li>{@code hbm:vacuum_distill} in {@code data/hbm/recipe/vacuum_distill/}: {@code {"input": fluid, "outputs": [four]}}
 *       - per 100 mB of (pressure 2) input, no item.</li>
 * </ul>
 * Converted once from the Java table (4 refinery and 2 vacuum rows).
 */
public final class RefineryRecipes {

    private RefineryRecipes() {}

    // 1.7's public fraction constants, kept for code that names them
    public static final int oil_frac_heavy = 50;
    public static final int oil_frac_naph = 25;
    public static final int oil_frac_light = 15;
    public static final int oil_frac_petro = 10;
    public static final int crack_frac_naph = 40;
    public static final int crack_frac_light = 30;
    public static final int crack_frac_aroma = 15;
    public static final int crack_frac_unsat = 15;
    public static final int oilds_frac_heavy = 30;
    public static final int oilds_frac_naph = 35;
    public static final int oilds_frac_light = 20;
    public static final int oilds_frac_unsat = 15;
    public static final int crackds_frac_naph = 35;
    public static final int crackds_frac_light = 35;
    public static final int crackds_frac_aroma = 15;
    public static final int crackds_frac_unsat = 15;
    public static final int vac_frac_heavy = 40;
    public static final int vac_frac_reform = 25;
    public static final int vac_frac_light = 20;
    public static final int vac_frac_sour = 15;

    /** Four fraction outputs (per 100 mB input) + the per-100-ops byproduct item. */
    public record RefineryRecipe(FluidStack frac1, FluidStack frac2, FluidStack frac3, FluidStack frac4, ItemStack byproduct) {
        public FluidStack[] fractions() {
            return new FluidStack[]{frac1, frac2, frac3, frac4};
        }
    }

    /** Vacuum distiller: four fraction outputs per 100 mB input (no byproduct item). */
    public record VacuumRecipe(FluidStack frac1, FluidStack frac2, FluidStack frac3, FluidStack frac4) {
        public FluidStack[] fractions() {
            return new FluidStack[]{frac1, frac2, frac3, frac4};
        }
    }

    public record Row(FluidType input, List<FluidStack> outputs, GenericRecipeCodec.ItemOut byproduct) {}

    public record VacuumRow(FluidType input, List<FluidStack> outputs) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GenericRecipeCodec.FLUID.fieldOf("input").forGetter(Row::input),
            FluidRecipeCodecs.stacks(4).fieldOf("outputs").forGetter(Row::outputs),
            GenericRecipeCodec.ITEM_OUT.fieldOf("byproduct").forGetter(Row::byproduct)
    ).apply(i, Row::new));

    public static final MapCodec<VacuumRow> VACUUM_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GenericRecipeCodec.FLUID.fieldOf("input").forGetter(VacuumRow::input),
            FluidRecipeCodecs.stacks(4).fieldOf("outputs").forGetter(VacuumRow::outputs)
    ).apply(i, VacuumRow::new));

    public static final FluidRecipeCodecs.ByFluid<Row, RefineryRecipe> TABLE = new FluidRecipeCodecs.ByFluid<>("refinery",
            () -> CODEC, Row::input, row -> new RefineryRecipe(row.outputs().get(0), row.outputs().get(1), row.outputs().get(2),
            row.outputs().get(3), row.byproduct().stack()));

    public static final FluidRecipeCodecs.ByFluid<VacuumRow, VacuumRecipe> VACUUM = new FluidRecipeCodecs.ByFluid<>(
            "vacuum_distill", () -> VACUUM_CODEC, VacuumRow::input, row -> new VacuumRecipe(row.outputs().get(0),
            row.outputs().get(1), row.outputs().get(2), row.outputs().get(3)));

    /** Declares the tables at mod construction. */
    public static void declare() {}

    public static RefineryRecipe getRefinery(FluidType oil) {
        return TABLE.get(oil);
    }

    public static VacuumRecipe getVacuum(FluidType oil) {
        return VACUUM.get(oil);
    }

    /** Input consumed by one vacuum-distillation operation. */
    public static int getVacuumInputAmount() {
        return 100;
    }

    /** Read-only view for JEI display. */
    public static Map<FluidType, RefineryRecipe> getAll() {
        return TABLE.all();
    }

    /** Read-only view for JEI display (vacuum distiller). */
    public static Map<FluidType, VacuumRecipe> getAllVacuum() {
        return VACUUM.all();
    }
}
