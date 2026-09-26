package com.hbm.api.content.recipes;

import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.GasCentrifugeRecipes.GasCentrifugeRecipe;
import com.hbm.inventory.recipes.GasCentrifugeRecipes.PseudoFluidType;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * <pre>
 * e.gasCentrifuge.stage('NUF6', 400, 300, 'LEUF6', false, 'hbm:nugget_u238')   consumed mB -> produced mB of next stage
 * e.gasCentrifuge.stage('THF4', 500, 0, 'NONE', false, 'hbm:nugget_th232')     new stage; next must already exist
 * e.gasCentrifuge.convert('uf6', 'NUF6')                                       a tank of this fluid feeds that stage
 * e.gasCentrifuge.removeConversion('watz')
 * e.gasCentrifuge.display('uf6', 1200, false, 2, '6x hbm:nugget_u238', '4x hbm:fluorite')
 * e.gasCentrifuge.removeDisplay('uf6')
 * e.gasCentrifuge.removeById('hbm:gas_centrifuge/display/uf6_2')              id = stage|conversion|display / name or fluid
 * </pre>
 * Machine behaviour = conversions + stages; display rows are JEI only. Stage names are saved in tanks and
 * shown via lang key hbmpseudofluid.&lt;name&gt;.
 */
public final class GasCentrifugeHandler extends TableHandler {

    @Override
    protected Object registry() {
        return GasCentrifugeRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        List<RecipeView> views = new ArrayList<>(RecipeIds.views(machine(), GasCentrifugeRecipes.stages(),
                s -> "stage/" + stageName(s), s -> List.of(s.getFluidConsumed() + "x " + stageName(s)),
                GasCentrifugeHandler::stageOutputs));
        views.addAll(RecipeIds.views(machine(), List.copyOf(GasCentrifugeRecipes.FLUID_CONVERSIONS.entrySet()),
                e -> "conversion/" + RecipeIds.spec(e.getKey()), e -> List.of(RecipeIds.spec(e.getKey())),
                e -> List.of(stageName(e.getValue()))));
        views.addAll(RecipeIds.views(machine(), List.copyOf(GasCentrifugeRecipes.RECIPES),
                r -> "display/" + RecipeIds.spec(r.input.type), r -> List.of(RecipeIds.spec(r.input)),
                r -> outputs(r.output)));
        return views;
    }

    private static String stageName(PseudoFluidType stage) {
        return stage.getName().toLowerCase(Locale.ROOT);
    }

    private static List<String> stageOutputs(PseudoFluidType stage) {
        List<String> out = new ArrayList<>();
        if (stage.getOutputType() != PseudoFluidType.NONE && stage.getFluidProduced() > 0) {
            out.add(stage.getFluidProduced() + "x " + stageName(stage.getOutputType()));
        }
        out.addAll(outputs(stage.getOutput()));
        return out;
    }

    private static List<String> outputs(ItemStack[] stacks) {
        List<String> out = new ArrayList<>();
        for (ItemStack stack : stacks) out.add(RecipeIds.spec(stack));
        return out;
    }

    @Override
    protected void delete(RecipeView view) {
        if (view.recipe() instanceof GasCentrifugeRecipe recipe) GasCentrifugeRecipes.remove(recipe);
        else if (view.recipe() instanceof Map.Entry<?, ?> conversion) GasCentrifugeRecipes.removeConversion((FluidType) conversion.getKey());
        else throw new UnsupportedOperationException("stage " + view.id() + " cannot be removed, only redefined");
    }

    @Override
    public void reset() {
        GasCentrifugeRecipes.reset();
    }

    public void stage(String name, int consumed, int produced, String next, boolean highSpeed, Spec.Item... outputs) {
        edit("stage " + name, () -> GasCentrifugeRecipes.defineStage(name, consumed, produced,
                PseudoFluidType.byName(next), highSpeed, ScriptSpecs.stacks(outputs)));
    }

    public void convert(Spec.Fluid fluid, String stage) {
        edit("convert " + fluid, () ->
                GasCentrifugeRecipes.putConversion(ScriptSpecs.fluidType(fluid), PseudoFluidType.byName(stage)));
    }

    public void removeConversion(Spec.Fluid fluid) {
        edit("removeConversion " + fluid, () -> GasCentrifugeRecipes.removeConversion(ScriptSpecs.fluidType(fluid)));
    }

    public void display(Spec.Fluid fluid, int amount, boolean highSpeed, int centrifuges, Spec.Item... outputs) {
        edit("display " + fluid, () -> GasCentrifugeRecipes.put(ScriptSpecs.fluidType(fluid), amount,
                ScriptSpecs.stacks(outputs), highSpeed, centrifuges));
    }

    public void removeDisplay(Spec.Fluid fluid) {
        edit("removeDisplay " + fluid, () -> GasCentrifugeRecipes.remove(ScriptSpecs.fluidType(fluid)));
    }
}
