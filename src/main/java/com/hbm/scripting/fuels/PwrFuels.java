package com.hbm.scripting.fuels;

import com.hbm.Constants;
import com.hbm.blocks.ModPwrContent;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ModPwrItems;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.lib.ItemIds;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.ScriptedContent;
import com.hbm.util.Function;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <pre>
 * e.pwr.add('kjs_heu', 'KJS PWR Fuel Rod', f => f
 *     .heat(7.5)                                   TU per flux; default 5
 *     .function('SQRT', 25).div(2500).off(1)       LOG SQRT SQRT_FALLING LINEAR PASSIVE; QUADRATIC(level, vOff); default MEU's
 *     .hotName('Hot KJS PWR Fuel Rod')             default 'Hot ' + name
 *     .depletedName('Depleted KJS PWR Fuel Rod')   default 'Depleted ' + name
 *     .texture('kubejs:item/my_rod')               default hbm:items/pwr_fuel.meu
 *     .hotTexture('hbm:items/pwr_fuel_hot').depletedTexture('hbm:items/pwr_fuel_depleted')
 *     .radiation(2.5)                              fresh RAD/s; hot, depleted x10; hot HOT 5
 *     .recipe('#c:billets/u235'))                  F/I/F, I = plate_polymer; also .shapeless(...) .shaped(rows, key)
 * </pre>
 * Items {@code pwr_fuel_<id>}, {@code pwr_fuel_hot_<id>}, {@code pwr_fuel_depleted_<id>}; fuel constant {@code ID}.
 * Yield 1e9 flux/rod. Controller saves the fuel name; a loaded fuel dropped from the script falls back to its index.
 */
public final class PwrFuels {

    public void add(String id, String name, Consumer<FuelSpec> spec) {
        FuelSpec fuel = new FuelSpec();
        spec.accept(fuel);
        EnumPWRFuel type = EnumPWRFuel.create(id, fuel.heat, fuel.function());
        ModPwrItems.Rods rods = ModPwrItems.register(ScriptedContent.items(ModPwrItems.ITEMS), type);
        ModTabs.addTo(HbmTab.CONTROL, rods.fresh());
        ModTabs.addTo(HbmTab.CONTROL, rods.hot());
        ModTabs.addTo(HbmTab.CONTROL, rods.depleted());
        ScriptedContent.itemAsset("pwr_fuel_" + type.id, fuel.texture, name);
        ScriptedContent.itemAsset("pwr_fuel_hot_" + type.id, fuel.hotTexture,
                fuel.hotName == null ? "Hot " + name : fuel.hotName);
        ScriptedContent.itemAsset("pwr_fuel_depleted_" + type.id, fuel.depletedTexture,
                fuel.depletedName == null ? "Depleted " + name : fuel.depletedName);
        if (fuel.radiation != null) {
            float rad = fuel.radiation.floatValue();
            ScriptedContent.whenRegistered(() -> ModPwrContent.registerPWRFuel(type, rad));
        }
        fuel.recipes.emit("pwr_fuel_" + type.id);
    }

    private enum Curve { LOG, PASSIVE, SQRT, SQRT_FALLING, LINEAR, QUADRATIC }

    public static final class FuelSpec {

        private double heat = 5D;
        private Curve curve;
        private double level;
        private double vOff;
        private Double div;
        private Double off;
        private String hotName;
        private String depletedName;
        private String texture = Constants.MOD_ID + ":items/pwr_fuel.meu";
        private String hotTexture = Constants.MOD_ID + ":items/pwr_fuel_hot";
        private String depletedTexture = Constants.MOD_ID + ":items/pwr_fuel_depleted";
        private Double radiation;
        private final FuelRecipes.Pending recipes = new FuelRecipes.Pending();

        public FuelSpec heat(double heatPerFlux) { this.heat = heatPerFlux; return this; }
        public FuelSpec function(String curve, double level) { return function(curve, level, 0D); }
        public FuelSpec function(String curve, double level, double vOff) {
            this.curve = ScriptSpecs.constant(Curve.class, curve);
            this.level = level;
            this.vOff = vOff;
            return this;
        }
        public FuelSpec div(double div) { this.div = div; return this; }
        public FuelSpec off(double off) { this.off = off; return this; }
        public FuelSpec hotName(String name) { this.hotName = name; return this; }
        public FuelSpec depletedName(String name) { this.depletedName = name; return this; }
        public FuelSpec texture(String sprite) { this.texture = sprite; return this; }
        public FuelSpec hotTexture(String sprite) { this.hotTexture = sprite; return this; }
        public FuelSpec depletedTexture(String sprite) { this.depletedTexture = sprite; return this; }
        public FuelSpec radiation(double fresh) { this.radiation = fresh; return this; }
        public FuelSpec recipe(String billet) {
            recipes.add(item -> FuelRecipes.shaped(item, 1, List.of("F", "I", "F"), Map.of('F', billet, 'I', ItemIds.plate_polymer)));
            return this;
        }
        public FuelSpec shapeless(String... ingredients) { recipes.shapeless(ingredients); return this; }
        public FuelSpec shaped(String[] pattern, Map<?, ?> key) { recipes.shaped(pattern, key); return this; }

        private Function function() {
            if (curve == null) {
                curve = Curve.LOG;
                level = 600D;
                if (div == null) div = 2_500D;
            }
            Function function = switch (curve) {
                case LOG -> new Function.FunctionLogarithmic(level);
                case PASSIVE -> new Function.FunctionPassive(level);
                case SQRT -> new Function.FunctionSqrt(level);
                case SQRT_FALLING -> new Function.FunctionSqrtFalling(level);
                case LINEAR -> new Function.FunctionLinear(level);
                case QUADRATIC -> new Function.FunctionQuadratic(level, vOff);
            };
            if (div != null) function.withDiv(div);
            if (off != null) function.withOff(off);
            return function;
        }
    }
}
