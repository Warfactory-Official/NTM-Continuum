package com.hbm.scripting.fuels;

import com.hbm.Constants;
import com.hbm.hazard.HazardRegistry;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.machine.ItemDepletedFuel;
import com.hbm.items.machine.ItemPlateFuel;
import com.hbm.items.machine.ItemPlateFuel.FunctionEnum;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.ScriptedContent;
import com.hbm.tileentity.machine.TileEntityReactorResearch;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

/**
 * <pre>
 * e.researchReactor.add('kjs_u235', 'KJS Plate Fuel', p => p
 *     .life(2200000)                          flux events until spent; default 2e6
 *     .function('SQUARE_ROOT').reactivity(40) LOGARITHM SQUARE_ROOT NEGATIVE_QUADRATIC LINEAR PASSIVE
 *     .waste('hbm:waste_plate_u235')          burnt-out result, item spec; ItemDepletedFuel => hot; none => plate consumed
 *     .texture('kubejs:item/my_plate')        default hbm:items/plate_fuel_u235
 *     .radiation(2.5, 125).blinding()         fresh -> spent RAD/s by burnup
 *     .shaped([' P ', 'PBP', ' P '], {P: '#c:plates/zirconium', B: '#c:billets/u235'}))   also .shapeless(...)
 * </pre>
 * Item {@code plate_fuel_<id>}. No preset recipe: built-in plates have no crafting recipe.
 */
public final class PlateFuels {

    public void add(String id, String name, Consumer<PlateSpec> spec) {
        PlateSpec plate = new PlateSpec();
        spec.accept(plate);
        String plateId = "plate_fuel_" + id;
        RegistryObject<ItemPlateFuel> item = ModIngredientItems.registerPlateFuel(
                ScriptedContent.items(ModIngredientItems.ITEMS), plateId, plate.life, plate.function, plate.reactivity);
        ScriptedContent.itemAsset(plateId, plate.texture, name);
        plate.recipes.emit(plateId);
        ScriptedContent.whenRegistered(() -> {
            if (plate.waste != null) {
                ItemStack waste = ScriptSpecs.stack(plate.waste);
                if (waste.getItem() instanceof ItemDepletedFuel) ItemDepletedFuel.setHot(waste, true);
                TileEntityReactorResearch.registerFuel(item.get(), waste);
            }
            if (plate.radiation != null) HazardRegistry.registerOtherFuel(item.get(),
                    (float) plate.radiation[0], (float) plate.radiation[1], plate.blinding);
        });
    }

    public static final class PlateSpec {

        private int life = 2_000_000;
        private FunctionEnum function = FunctionEnum.SQUARE_ROOT;
        private int reactivity = 40;
        private String waste;
        private String texture = Constants.MOD_ID + ":items/plate_fuel_u235";
        private double[] radiation;
        private boolean blinding;
        private final FuelRecipes.Pending recipes = new FuelRecipes.Pending();

        public PlateSpec life(int events) { this.life = events; return this; }
        public PlateSpec function(String function) { this.function = ScriptSpecs.constant(FunctionEnum.class, function); return this; }
        public PlateSpec reactivity(int reactivity) { this.reactivity = reactivity; return this; }
        public PlateSpec waste(String item) { this.waste = item; return this; }
        public PlateSpec texture(String sprite) { this.texture = sprite; return this; }
        public PlateSpec radiation(double fresh, double spent) { this.radiation = new double[] {fresh, spent}; return this; }
        public PlateSpec blinding() { this.blinding = true; return this; }
        public PlateSpec shapeless(String... ingredients) { recipes.shapeless(ingredients); return this; }
        public PlateSpec shaped(String[] pattern, Map<?, ?> key) { recipes.shaped(pattern, key); return this; }
    }
}
