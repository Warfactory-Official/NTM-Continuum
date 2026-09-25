package com.hbm.scripting.fuels;

import com.hbm.Constants;
import com.hbm.hazard.HazardData;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.ModWatzItems;
import com.hbm.items.machine.EnumWatzType;
import com.hbm.lib.TagIds;
import com.hbm.scripting.ScriptedContent;
import com.hbm.util.Function;
import dev.golem.core.registry.DeferredRegister;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Items {@code watz_pellet_<id>} and {@code watz_pellet_depleted_<id>}.
 *
 * <pre>
 * e.watz.add('kubejs_hes', 'KJS Watz Pellet', w => w
 *     .colors(0x66DCD6, 0x023933)                   bar colour = light
 *     .passive(1750).heatEmission(20).mud(0.005)    mud halved, as built-ins
 *     .burn(f => f.linear(1.25))                    linear sqrt sqrtFalling quadratic log passive; .div(x) .off(x) after
 *     .heatDiv(f => f.sqrt(1 / 25).off(625))
 *     .absorb(f => f.quadratic(1, 1).div(100))      absorber pellets: absorb only
 *     .texture('kubejs:item/pellet')                default hbm:items/watz_pellet
 *     .depletedTexture('kubejs:item/pellet_spent')  default hbm:items/watz_pellet_depleted
 *     .depletedName('Spent KJS Pellet')             default '&lt;name&gt; (Depleted)'
 *     .radiation(5, 20)                             fresh, depleted RAD/s
 *     .recipe('hbm:ingot_hes'))                     " I "/"IGI"/" I ", G = #c:ingots/graphite; also .shapeless(...) .shaped(rows, key)
 * </pre>
 * Watz locks persist the type's index: reordering scripted types remaps locked slots.
 */
public final class WatzFuels {

    public void add(String id, String name, Consumer<PelletSpec> spec) {
        PelletSpec pellet = new PelletSpec();
        spec.accept(pellet);
        DeferredRegister.Items register = ScriptedContent.items(ModWatzItems.ITEMS);
        EnumWatzType type = EnumWatzType.create(id, pellet.colorLight, pellet.colorDark, pellet.passive,
                pellet.heatEmission, pellet.mud, pellet.burn, pellet.heatDiv, pellet.absorb);
        ModWatzItems.register(register, type);
        ScriptedContent.itemAsset("watz_pellet_" + type.itemSuffix(), pellet.texture, name);
        ScriptedContent.itemAsset("watz_pellet_depleted_" + type.itemSuffix(), pellet.depletedTexture,
                pellet.depletedName == null ? name + " (Depleted)" : pellet.depletedName);
        if (pellet.radiation != null) {
            double[] rad = pellet.radiation;
            ScriptedContent.whenRegistered(() -> {
                HazardSystem.register(ModWatzItems.fresh(type), new HazardData().addEntry(HazardRegistry.RADIATION, rad[0]));
                HazardSystem.register(ModWatzItems.depleted(type), new HazardData().addEntry(HazardRegistry.RADIATION, rad[1]));
            });
        }
        pellet.recipes.emit("watz_pellet_" + type.itemSuffix());
    }

    public static final class PelletSpec {

        private int colorLight = 0xD2D2D2;
        private int colorDark = 0x333333;
        private double passive;
        private double heatEmission = 10D;
        private double mud = 0.0005D;
        private Function burn;
        private Function heatDiv;
        private Function absorb;
        private String texture = Constants.MOD_ID + ":items/watz_pellet";
        private String depletedTexture = Constants.MOD_ID + ":items/watz_pellet_depleted";
        private String depletedName;
        private double[] radiation;
        private final FuelRecipes.Pending recipes = new FuelRecipes.Pending();

        public PelletSpec colors(int light, int dark) { this.colorLight = light; this.colorDark = dark; return this; }
        public PelletSpec passive(double passive) { this.passive = passive; return this; }
        public PelletSpec heatEmission(double heat) { this.heatEmission = heat; return this; }
        public PelletSpec mud(double mud) { this.mud = mud; return this; }
        public PelletSpec burn(Consumer<Curve> curve) { this.burn = Curve.build(curve); return this; }
        public PelletSpec heatDiv(Consumer<Curve> curve) { this.heatDiv = Curve.build(curve); return this; }
        public PelletSpec absorb(Consumer<Curve> curve) { this.absorb = Curve.build(curve); return this; }
        public PelletSpec texture(String sprite) { this.texture = sprite; return this; }
        public PelletSpec depletedTexture(String sprite) { this.depletedTexture = sprite; return this; }
        public PelletSpec depletedName(String name) { this.depletedName = name; return this; }
        public PelletSpec radiation(double fresh, double depleted) { this.radiation = new double[] {fresh, depleted}; return this; }
        public PelletSpec recipe(String ingot) {
            recipes.add(item -> FuelRecipes.shaped(item, 1, List.of(" I ", "IGI", " I "), Map.of('I', ingot, 'G', TagIds.ingots_graphite)));
            return this;
        }
        public PelletSpec shapeless(String... ingredients) { recipes.shapeless(ingredients); return this; }
        public PelletSpec shaped(String[] pattern, Map<?, ?> key) { recipes.shaped(pattern, key); return this; }
    }

    public static final class Curve {

        private Function function;

        private Curve() {}

        private static Function build(Consumer<Curve> spec) {
            Curve curve = new Curve();
            spec.accept(curve);
            return curve.shape();
        }

        public Curve linear(double level) { function = new Function.FunctionLinear(level); return this; }
        public Curve sqrt(double level) { function = new Function.FunctionSqrt(level); return this; }
        public Curve sqrtFalling(double fallFactor) { function = new Function.FunctionSqrtFalling(fallFactor); return this; }
        public Curve quadratic(double level, double vOff) { function = new Function.FunctionQuadratic(level, vOff); return this; }
        public Curve log(double level) { function = new Function.FunctionLogarithmic(level); return this; }
        public Curve passive(double level) { function = new Function.FunctionPassive(level); return this; }
        public Curve div(double div) { shape().withDiv(div); return this; }
        public Curve off(double off) { shape().withOff(off); return this; }

        private Function shape() {
            if (function == null) throw new IllegalArgumentException("Watz curve needs a shape");
            return function;
        }
    }
}
