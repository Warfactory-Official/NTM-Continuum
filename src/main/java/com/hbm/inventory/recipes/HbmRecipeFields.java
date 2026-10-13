package com.hbm.inventory.recipes;

import static dev.golem.core.world.RecipeFields.field;

import com.hbm.handler.NTMToolHandler;
import com.hbm.inventory.material.MaterialDistribution;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.RecipeShapes;
import dev.golem.core.world.RecipeFields;
import dev.golem.core.world.RecipeFields.Field;
import dev.golem.core.world.RecipeFields.Kind;
import dev.golem.core.world.RecipeFields.Shape;

/**
 * The script fields ({@link RecipeFields}) of every HBM record-shaped recipe table ({@link DataRecipeTable} and
 * {@code FluidRecipeCodecs.ByFluid}): one line per codec key, named exactly as the key, so Golem Core's scripting adapter
 * offers {@code event.recipes.hbm.<table>(...)} with typed arguments. The generic tables ({@code DataGenericRecipes})
 * state theirs themselves ({@code GenericRecipeCodec.FIELDS}).
 *
 * <p>Positional order: the required keys first (the codec fails without them), outputs before inputs, then the machine's
 * numbers; optional keys follow in the same order. Every key with an underscore also answers to its camelCase name as a
 * builder method ({@code .solidFuelEnergy(...)}). A key whose JSON is an HBM form with no neutral value (molten
 * material stacks, SILEX's weighted outputs, annihilator milestones, the sawmill's crafting rule) is {@link Kind#JSON}:
 * scripts write that object as the datapack file would. The GameTest {@code script_fields_match_codecs} checks that every
 * stated key exists in its codec and that the shapes write rows the codecs read.
 */
public final class HbmRecipeFields {

    private HbmRecipeFields() {}

    /** States the fields; called by {@link HbmRecipeTables#declare()} once every table exists. */
    public static void declare() {
        // Fluid tables
        CokerRecipes.TABLE.table.fields(of(
                out("output", RecipeShapes.STACK_OUT), fin("input", RecipeShapes.FLUID_ID),
                field("amount", Kind.INT).optional(), field("solid_fuel_energy", Kind.LONG).optional(),
                fout("byproduct", RecipeShapes.FLUID_STACK).optional(), fout("byproduct_type", RecipeShapes.FLUID_ID).optional()));
        CompressorRecipes.TABLE.fields(of(
                fout("output", RecipeShapes.FLUID_STACK), fin("input", RecipeShapes.FLUID_ID),
                field("amount", Kind.INT), field("duration", Kind.INT), field("pressure", Kind.INT).optional()));
        RecipeFields leftRight = of(
                fout("left", RecipeShapes.FLUID_STACK), fin("input", RecipeShapes.FLUID_ID),
                fout("right", RecipeShapes.FLUID_STACK).optional());
        CrackingRecipes.TABLE.table.fields(leftRight);
        RadiolysisRecipes.TABLE.table.fields(leftRight);
        ElectrolyserFluidRecipes.TABLE.table.fields(of(
                fin("input", RecipeShapes.FLUID_ID), field("amount", Kind.INT),
                fout("output1", RecipeShapes.FLUID_STACK).optional(), fout("output2", RecipeShapes.FLUID_STACK).optional(),
                field("duration", Kind.INT).optional(), out("byproducts", RecipeShapes.STACK_OUT).list().optional()));
        GasCentrifugeRecipes.PSEUDO_FLUIDS.fields(of(
                field("name", Kind.STRING), out("outputs", RecipeShapes.STACK_OUT).list(),
                field("consumed", Kind.INT), field("produced", Kind.INT),
                fin("from", RecipeShapes.FLUID_ID).optional(), field("next", Kind.STRING).optional(),
                field("high_speed", Kind.BOOLEAN).optional()));
        GasCentrifugeRecipes.DISPLAY.fields(of(
                out("outputs", RecipeShapes.STACK_OUT).list(), fin("input", RecipeShapes.FLUID_STACK),
                field("centrifuges", Kind.INT), field("high_speed", Kind.BOOLEAN).optional()));
        HydrotreatingRecipes.TABLE.table.fields(of(
                fout("outputs", RecipeShapes.FLUID_STACK).list(), fin("input", RecipeShapes.FLUID_ID),
                fin("hydrogen", RecipeShapes.FLUID_STACK)));
        LiquefactionRecipes.TABLE.fields(of(
                fout("output", RecipeShapes.FLUID_STACK), in("input", RecipeShapes.INGREDIENT_ID)));
        PyroOvenRecipes.TABLE.fields(of(
                field("duration", Kind.INT),
                out("output_item", RecipeShapes.STACK_OUT).optional(), fout("output_fluid", RecipeShapes.FLUID_STACK).optional(),
                in("input_item", RecipeShapes.INGREDIENT).optional(), fin("input_fluid", RecipeShapes.FLUID_STACK).optional(),
                fin("solid_fuel", RecipeShapes.FLUID_ID).optional(), field("solid_fuel_energy", Kind.LONG).optional()));
        RefineryRecipes.TABLE.table.fields(of(
                fout("outputs", RecipeShapes.FLUID_STACK).list(), out("byproduct", RecipeShapes.STACK_OUT),
                fin("input", RecipeShapes.FLUID_ID)));
        RefineryRecipes.VACUUM.table.fields(of(
                fout("outputs", RecipeShapes.FLUID_STACK).list(), fin("input", RecipeShapes.FLUID_ID)));
        ReformingRecipes.TABLE.table.fields(of(
                fout("outputs", RecipeShapes.FLUID_STACK).list(), fin("input", RecipeShapes.FLUID_ID)));
        SolidificationRecipes.TABLE.table.fields(of(
                out("output", RecipeShapes.STACK_OUT), fin("input", RecipeShapes.FLUID_ID),
                field("amount", Kind.INT).optional(), field("solid_fuel_energy", Kind.LONG).optional()));
        StorageDrumRecipes.TABLE.fields(of(
                out("output", RecipeShapes.STACK_OUT), in("input", RecipeShapes.ITEM_STACK_IN), field("decay", Kind.STRING),
                field("divisor", Kind.INT).optional(), field("liquid", Kind.INT).optional(), field("gas", Kind.INT).optional()));
        FractionRecipes.TABLE.fields(of(
                fout("left", RecipeShapes.FLUID_STACK), fout("right", RecipeShapes.FLUID_STACK),
                fin("input", RecipeShapes.FLUID_ID)));

        // Item-processing tables
        NTMToolHandler.TABLE.fields(of(
                field("tool", Kind.STRING), field("from", Kind.ID), field("to", Kind.ID),
                in("materials", RecipeShapes.INGREDIENT).list().optional()));
        MaterialDistribution.TABLE.fields(of(
                field("materials", Kind.JSON).list(), in("input", RecipeShapes.INGREDIENT_ID)));
        ArcFurnaceRecipes.TABLE.fields(of(
                in("input", RecipeShapes.INGREDIENT),
                out("solid_output", RecipeShapes.STACK_OUT).optional(), field("molten_outputs", Kind.JSON).list().optional(),
                order()));
        CrucibleRecipes.TABLE.fields(of(
                field("name", Kind.STRING), field("outputs", Kind.JSON).list(), field("inputs", Kind.JSON).list(),
                field("frequency", Kind.INT).optional(),
                out("icon", RecipeShapes.STACK_OUT).role(RecipeFields.Role.OTHER).optional(), order()));
        CentrifugeRecipes.TABLE.fields(of(
                out("outputs", RecipeShapes.STACK_OUT).list(), in("input", RecipeShapes.INGREDIENT), requires(), order()));
        CrystallizerRecipes.TABLE.fields(of(
                out("output", RecipeShapes.STACK_OUT), in("input", RecipeShapes.INGREDIENT),
                fin("acid", RecipeShapes.FLUID_STACK), field("duration", Kind.INT),
                field("productivity", Kind.DOUBLE).optional(), field("requires", Kind.ID).optional(), order()));
        CyclotronRecipes.TABLE.fields(of(
                out("output", RecipeShapes.STACK_OUT), in("input", RecipeShapes.INGREDIENT),
                in("particle", RecipeShapes.ITEM_ID_IN), field("antimatter", Kind.INT), order()));
        ElectrolyserMetalRecipes.TABLE.fields(of(
                field("molten", Kind.JSON).list(), in("input", RecipeShapes.ITEM_ID_IN),
                out("byproducts", RecipeShapes.STACK_OUT).list().optional(), field("duration", Kind.INT).optional(), order()));
        RotaryFurnaceRecipes.TABLE.fields(of(
                field("output", Kind.JSON), field("duration", Kind.INT), field("steam", Kind.INT),
                in("inputs", RecipeShapes.INGREDIENT).list().optional(), fin("fluid", RecipeShapes.FLUID_STACK).optional(),
                order()));
        SILEXRecipes.TABLE.fields(of(
                field("outputs", Kind.JSON).list(), in("input", RecipeShapes.INGREDIENT),
                field("laser_strength", Kind.STRING), field("fluid_produced", Kind.INT), field("fluid_consumed", Kind.INT),
                order()));
        ShredderRecipes.TABLE.fields(of(
                out("output", RecipeShapes.STACK_OUT), in("input", RecipeShapes.INGREDIENT_ID)));

        // Item tables
        AmmoPressRecipes.TABLE.fields(of(
                out("output", RecipeShapes.STACK_OUT), in("grid", RecipeShapes.SLOT).list(), order()));
        AnnihilatorRecipes.TABLE.fields(of(
                field("milestones", Kind.JSON).list(),
                in("item", RecipeShapes.ITEM_ID_IN).optional(), in("tag", RecipeShapes.TAG_ID_IN).optional(),
                fin("fluid", RecipeShapes.FLUID_ID).optional(), requires()));
        ArcWelderRecipes.TABLE.fields(of(
                out("output", RecipeShapes.STACK_OUT), in("ingredients", RecipeShapes.INGREDIENT).list(),
                field("duration", Kind.INT), field("consumption", Kind.LONG),
                fin("fluid", RecipeShapes.FLUID_STACK).optional(), order()));
        BreederRecipes.TABLE.fields(of(
                out("output", RecipeShapes.STACK_OUT), in("input", RecipeShapes.ITEM_ID_IN), field("flux", Kind.INT), order()));
        CombinationRecipes.TABLE.fields(of(
                in("input", RecipeShapes.INGREDIENT),
                out("item_output", RecipeShapes.STACK_OUT).optional(), fout("fluid_output", RecipeShapes.FLUID_STACK).optional(),
                order()));
        LemegetonRecipes.TABLE.fields(of(
                out("output", RecipeShapes.ITEM_ID_OUT), in("input", RecipeShapes.INGREDIENT_ID), order()));
        MagicRecipes.TABLE.fields(of(
                out("result", RecipeShapes.ITEM_ID_OUT), in("ingredients", RecipeShapes.INGREDIENT_ID).list(), order()));
        ParticleAcceleratorRecipes.TABLE.fields(of(
                out("output1", RecipeShapes.STACK_OUT), in("input1", RecipeShapes.INGREDIENT),
                in("input2", RecipeShapes.INGREDIENT), field("momentum", Kind.INT),
                out("output2", RecipeShapes.STACK_OUT).optional(), order()));
        PedestalRecipes.TABLE.fields(of(
                out("output", RecipeShapes.ITEM_ID_OUT), in("grid", RecipeShapes.PEDESTAL_SLOT).list(),
                field("count", Kind.INT).optional(), field("extra", Kind.STRING).optional(), field("set", Kind.INT).optional(),
                order()));
        PressRecipes.TABLE.fields(of(
                out("output", RecipeShapes.STACK_OUT), in("input", RecipeShapes.INGREDIENT), field("stamp", Kind.STRING),
                order()));
        SawmillRecipes.TABLE.fields(of(
                out("output", RecipeShapes.STACK_OUT), in("input", RecipeShapes.INGREDIENT),
                field("crafting_result", Kind.JSON).optional(), field("sawdust_chance", Kind.DOUBLE).optional(),
                field("hidden", Kind.BOOLEAN).optional(), order()));
        SolderingRecipes.TABLE.fields(of(
                out("output", RecipeShapes.STACK_OUT), field("duration", Kind.INT), field("consumption", Kind.LONG),
                in("toppings", RecipeShapes.INGREDIENT).list().optional(), in("pcb", RecipeShapes.INGREDIENT).list().optional(),
                in("solder", RecipeShapes.INGREDIENT).list().optional(), fin("fluid", RecipeShapes.FLUID_STACK).optional(),
                requires(), order()));
        AnvilRecipes.SMITHING.fields(of(
                out("output", RecipeShapes.STACK_OUT), in("left", RecipeShapes.INGREDIENT), in("right", RecipeShapes.INGREDIENT),
                field("tier", Kind.INT), field("hot", Kind.BOOLEAN).optional(), field("shapeless", Kind.BOOLEAN).optional(),
                order()));
        AnvilRecipes.CONSTRUCTION.fields(of(
                out("outputs", RecipeShapes.CHANCE_OUTPUT_OBJECT).list(), in("inputs", RecipeShapes.INGREDIENT).list(),
                field("tier", Kind.INT), field("tier_upper", Kind.INT).optional(), field("overlay", Kind.STRING).optional(),
                requires(), order()));
    }

    // Helpers

    /** The fields in order, each key with an underscore also answering to its camelCase name. */
    private static RecipeFields of(Field... fields) {
        RecipeFields result = RecipeFields.of();
        for (Field field : fields) {
            String camel = camel(field.name());
            result = result.add(camel.equals(field.name()) ? field : field.alias(camel));
        }
        return result;
    }

    private static String camel(String name) {
        StringBuilder out = new StringBuilder();
        boolean upper = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                out.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return out.toString();
    }

    private static Field in(String name, Shape shape) {
        return field(name, Kind.ITEM_IN).shape(shape);
    }

    private static Field out(String name, Shape shape) {
        return field(name, Kind.ITEM_OUT).shape(shape);
    }

    private static Field fin(String name, Shape shape) {
        return field(name, Kind.FLUID_IN).shape(shape);
    }

    private static Field fout(String name, Shape shape) {
        return field(name, Kind.FLUID_OUT).shape(shape);
    }

    /** {@code "order"}: the row's position in 1.7's list (rows without one come last, by recipe id). */
    private static Field order() {
        return field("order", Kind.INT).optional();
    }

    /** {@code "requires"}: the 1.7 config switches the row depends on ({@code TableCodecs.REQUIRES}). */
    private static Field requires() {
        return field("requires", Kind.STRING).list().optional();
    }
}
