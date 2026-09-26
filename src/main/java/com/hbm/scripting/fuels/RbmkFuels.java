package com.hbm.scripting.fuels;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.ModRBMKItems;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.api.ids.ItemIds;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.ScriptedContent;
import dev.golem.core.registry.RegistryObject;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Items {@code rbmk_fuel_<id>} and, with {@code pellet(...)}, {@code rbmk_pellet_<id>}.
 *
 * <pre>
 * e.rbmk.add('kubejs_meu', 'KJS RBMK Fuel Rod', r => r
 *     .fullName('Custom Medium-Enriched Uranium')
 *     .yield(1e8).reactivity(20).selfRate(0).heat(0.65).meltingPoint(2865).diffusion(0.02)
 *     .burn('LOG_TEN').deplete('RAISING_SLOPE')    PASSIVE LOG_TEN PLATEU ARCH SIGMOID SQUARE_ROOT LINEAR QUADRATIC
 *     .xenon(0.5, 50).heatCoeff(0, 0)              EXPERIMENTAL; LINEAR RAISING_SLOPE BOOSTED_SLOPE GENTLE_SLOPE STATIC
 *     .neutrons('SLOW', 'FAST').tint(0x868D82)     column tint; SLOW FAST ANY
 *     .texture('kubejs:item/my_rod')               default hbm:items/rbmk_fuel_empty
 *     .radiation(0.35 * 8, 15 * 8 * 20)            fresh, spent RAD/s; hot while burning
 *     .pellet('KJS Pellet', 'Custom MEU', p => p.texture('hbm:items/rbmk_pellet_meu').noXenon().radiation(0.2, 30))
 *     .recipe('#c:billets/uranium'))               rbmk_fuel_empty + 8 billets; also .shapeless(...) .shaped(rows, key)
 * </pre>
 * Recipes: {@link FuelRecipes}.
 */
public final class RbmkFuels {

    public void add(String id, String name, Consumer<RodSpec> spec) {
        RodSpec rod = new RodSpec();
        spec.accept(rod);
        String rodId = "rbmk_fuel_" + id;
        RegistryObject<ItemRBMKRod> item = ScriptedContent.items(ModRBMKItems.ITEMS).registerItem(rodId,
                p -> rod.apply(new ItemRBMKRod(p.stacksTo(1), rod.fullName == null ? name : rod.fullName)));
        ModTabs.addTo(HbmTab.CONTROL, item);
        ScriptedContent.itemAsset(rodId, rod.texture, name);
        if (rod.radiation != null) {
            double[] rad = rod.radiation;
            ScriptedContent.whenRegistered(() -> HazardSystem.register(item.get(),
                    HazardRegistry.rbmkData(rad[0], rad[1], true, false, 0D, 0D)));
        }
        if (rod.pellet != null) pellet(item, id, rod.pellet);
        rod.recipes.emit(rodId);
    }

    private static void pellet(RegistryObject<ItemRBMKRod> rod, String id, PelletSpec spec) {
        String pelletId = "rbmk_pellet_" + id;
        RegistryObject<ItemRBMKPellet> pellet = ScriptedContent.items(ModRBMKItems.ITEMS).registerItem(pelletId, p -> {
            ItemRBMKPellet value = new ItemRBMKPellet(p, spec.fullName);
            return spec.xenon ? value : value.disableXenon();
        });
        ModRBMKItems.PELLETS.put(rod, pellet);
        ModTabs.addTo(HbmTab.CONTROL, pellet);
        ScriptedContent.itemAsset(pelletId, spec.texture, spec.name);
        ScriptedContent.whenRegistered(() -> {
            rod.get().pellet = pellet.get();
            if (spec.radiation != null) HazardSystem.register(pellet.get(),
                    HazardRegistry.rbmkData(spec.radiation[0], spec.radiation[1], false, false, 0D, 0D));
        });
    }

    public static final class RodSpec {

        private String fullName;
        private String texture = Constants.MOD_ID + ":items/rbmk_fuel_empty";
        private double yield = 100_000_000D;
        private double reactivity;
        private double selfRate;
        private double heat = 1D;
        private double meltingPoint = 1000D;
        private double diffusion = 0.02D;
        private double xenonGen = 0.5D;
        private double xenonBurn = 50D;
        private double heatCoeffStart;
        private double heatCoeffLength;
        private ItemRBMKRod.EnumBurnFunc burn = ItemRBMKRod.EnumBurnFunc.LOG_TEN;
        private ItemRBMKRod.EnumDepleteFunc deplete = ItemRBMKRod.EnumDepleteFunc.GENTLE_SLOPE;
        private ItemRBMKRod.NType neutronIn = ItemRBMKRod.NType.SLOW;
        private ItemRBMKRod.NType neutronOut = ItemRBMKRod.NType.FAST;
        private int tint = 0x304825;
        private double[] radiation;
        private PelletSpec pellet;
        private final FuelRecipes.Pending recipes = new FuelRecipes.Pending();

        public RodSpec fullName(String fullName) { this.fullName = fullName; return this; }
        public RodSpec texture(String sprite) { this.texture = sprite; return this; }
        public RodSpec yield(double yield) { this.yield = yield; return this; }
        public RodSpec reactivity(double reactivity) { this.reactivity = reactivity; return this; }
        public RodSpec selfRate(double selfRate) { this.selfRate = selfRate; return this; }
        public RodSpec heat(double heat) { this.heat = heat; return this; }
        public RodSpec meltingPoint(double celsius) { this.meltingPoint = celsius; return this; }
        public RodSpec diffusion(double diffusion) { this.diffusion = diffusion; return this; }
        public RodSpec xenon(double gen, double burn) { this.xenonGen = gen; this.xenonBurn = burn; return this; }
        public RodSpec heatCoeff(double start, double length) { this.heatCoeffStart = start; this.heatCoeffLength = length; return this; }
        public RodSpec burn(String function) { this.burn = ScriptSpecs.constant(ItemRBMKRod.EnumBurnFunc.class, function); return this; }
        public RodSpec deplete(String function) { this.deplete = ScriptSpecs.constant(ItemRBMKRod.EnumDepleteFunc.class, function); return this; }
        public RodSpec neutrons(String in, String out) {
            this.neutronIn = ScriptSpecs.constant(ItemRBMKRod.NType.class, in);
            this.neutronOut = ScriptSpecs.constant(ItemRBMKRod.NType.class, out);
            return this;
        }
        public RodSpec tint(int rgb) { this.tint = rgb; return this; }
        public RodSpec radiation(double fresh, double spent) { this.radiation = new double[] {fresh, spent}; return this; }
        public RodSpec recipe(String billet) {
            recipes.add(item -> FuelRecipes.shapeless(item, 1, FuelRecipes.with(ItemIds.rbmk_fuel_empty, billet, 8)));
            return this;
        }
        public RodSpec shapeless(String... ingredients) { recipes.shapeless(ingredients); return this; }
        public RodSpec shaped(String[] pattern, Map<?, ?> key) { recipes.shaped(pattern, key); return this; }

        public RodSpec pellet(String name, String fullName, Consumer<PelletSpec> spec) {
            this.pellet = new PelletSpec(name, fullName);
            spec.accept(this.pellet);
            return this;
        }

        private ItemRBMKRod apply(ItemRBMKRod rod) {
            rod.setYield(yield).setStats(reactivity, selfRate).setHeat(heat).setMeltingPoint(meltingPoint)
                    .setDiffusion(diffusion).setXenon(xenonGen, xenonBurn).setHeatCoeff(heatCoeffStart, heatCoeffLength)
                    .setFunction(burn).setDepletionFunction(deplete).setNeutronTypes(neutronIn, neutronOut).setTint(tint);
            return rod;
        }
    }

    public static final class PelletSpec {

        private final String name;
        private final String fullName;
        private String texture = Constants.MOD_ID + ":items/rbmk_pellet_ueu";
        private boolean xenon = true;
        private double[] radiation;

        private PelletSpec(String name, String fullName) {
            this.name = name;
            this.fullName = fullName;
        }

        public PelletSpec texture(String sprite) { this.texture = sprite; return this; }
        public PelletSpec noXenon() { this.xenon = false; return this; }
        public PelletSpec radiation(double fresh, double spent) { this.radiation = new double[] {fresh, spent}; return this; }
    }
}
