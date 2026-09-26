package com.hbm.scripting.fuels;

import com.hbm.Constants;
import com.hbm.hazard.HazardRegistry;
import com.hbm.items.ModZirnoxItems;
import com.hbm.items.machine.ZirnoxFuel;
import com.hbm.api.ids.ItemIds;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.ScriptedContent;
import dev.golem.core.registry.DeferredRegister;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Items {@code rod_zirnox_<id>}; with {@code waste(...)} also {@code rod_zirnox_depleted_<id>} and {@code waste_<wasteId>}.
 *
 * <pre>
 * e.zirnox.add('kubejs_heu', 'KJS ZIRNOX Rod', z => z
 *     .life(150000).heat(100)                         operations to deplete, heat per operation
 *     .radiation(5, 150)                              fresh, depleted RAD/s; rod interpolates by life
 *     .waste('kubejs_heu', 'Depleted KJS Fuel', w => w.texture('kubejs:item/waste').contamination(15))
 *     .depletedName('Depleted KJS Rod').depletedTexture('kubejs:item/rod_spent').blinding(20)
 *     .texture('kubejs:item/rod')                     default hbm:items/rod_zirnox_uranium_fuel
 *     .recipe('hbm:billet_u235'))                     2 rod_zirnox_empty + 2 billets; also .shapeless(...) .shaped(rows, key)
 *
 * e.zirnox.add('kubejs_breeder', 'KJS Breeder Rod', z => z
 *     .life(20000).heat(0).breeding().radiation(0.1, 1.75)
 *     .product('hbm:rod_zirnox_thorium_fuel'))       spent rod becomes this item
 * </pre>
 * Exactly one of {@code waste}/{@code product}. Breeding: no self-operation, not counted by neighbours.
 * Depleted rod: craft remainder {@code rod_zirnox_empty}, texture default {@code hbm:items/rod_zirnox_depleted_uranium_fuel},
 * name default {@code 'Depleted <name>'}. Waste texture default {@code hbm:items/rod_zirnox_empty}.
 */
public final class ZirnoxFuels {

    public void add(String id, String name, Consumer<RodSpec> spec) {
        RodSpec rod = new RodSpec();
        spec.accept(rod);
        if ((rod.waste == null) == (rod.product == null)) {
            throw new IllegalArgumentException("ZIRNOX fuel " + id + " needs exactly one of waste(...) or product(...)");
        }
        for (ZirnoxFuel other : ZirnoxFuel.values()) {
            if (other.id.equals(id) || rod.waste != null && rod.waste.id.equals(other.wasteId)) {
                throw new IllegalArgumentException("ZIRNOX fuel " + id + " collides with " + other.name());
            }
        }
        DeferredRegister.Items register = ScriptedContent.items(ModZirnoxItems.ITEMS);
        ZirnoxFuel fuel = ZirnoxFuel.create(id, id, rod.life, rod.heat, rod.breeding, rod.baseRadiation,
                rod.depletedRadiation, rod.waste == null ? null : rod.waste.id);
        ModZirnoxItems.register(register, fuel);
        ScriptedContent.itemAsset("rod_zirnox_" + fuel.id, rod.texture, name);
        rod.recipes.emit("rod_zirnox_" + fuel.id);
        if (rod.product != null) {
            String product = rod.product;
            ModZirnoxItems.product(fuel, () -> ScriptSpecs.item(product));
            ScriptedContent.whenRegistered(() -> ScriptSpecs.item(product));
            return;
        }
        WasteSpec waste = rod.waste;
        ScriptedContent.itemAsset("rod_zirnox_depleted_" + fuel.id, rod.depletedTexture,
                rod.depletedName == null ? "Depleted " + name : rod.depletedName);
        ScriptedContent.itemAsset("waste_" + waste.id, waste.texture, waste.name);
        float blinding = rod.blinding;
        ScriptedContent.whenRegistered(() -> HazardRegistry.registerZirnoxFuel(fuel, blinding, waste.contamination));
    }

    public static final class RodSpec {

        private int life = 200_000;
        private int heat = 50;
        private boolean breeding;
        private float baseRadiation;
        private float depletedRadiation;
        private float blinding;
        private String texture = Constants.MOD_ID + ":items/rod_zirnox_uranium_fuel";
        private String depletedTexture = Constants.MOD_ID + ":items/rod_zirnox_depleted_uranium_fuel";
        private String depletedName;
        private WasteSpec waste;
        private String product;
        private final FuelRecipes.Pending recipes = new FuelRecipes.Pending();

        public RodSpec life(int maxLife) { this.life = maxLife; return this; }
        public RodSpec heat(int heat) { this.heat = heat; return this; }
        public RodSpec breeding() { this.breeding = true; return this; }
        public RodSpec radiation(float fresh, float depleted) { this.baseRadiation = fresh; this.depletedRadiation = depleted; return this; }
        public RodSpec blinding(float blinding) { this.blinding = blinding; return this; }
        public RodSpec texture(String sprite) { this.texture = sprite; return this; }
        public RodSpec depletedTexture(String sprite) { this.depletedTexture = sprite; return this; }
        public RodSpec depletedName(String name) { this.depletedName = name; return this; }
        public RodSpec product(String item) { this.product = item; return this; }
        public RodSpec recipe(String billet) {
            recipes.add(item -> FuelRecipes.shapeless(item, 1, ItemIds.rod_zirnox_empty, ItemIds.rod_zirnox_empty, billet, billet));
            return this;
        }
        public RodSpec shapeless(String... ingredients) { recipes.shapeless(ingredients); return this; }
        public RodSpec shaped(String[] pattern, Map<?, ?> key) { recipes.shaped(pattern, key); return this; }

        public RodSpec waste(String wasteId, String name) {
            return waste(wasteId, name, w -> {});
        }

        public RodSpec waste(String wasteId, String name, Consumer<WasteSpec> spec) {
            this.waste = new WasteSpec(wasteId, name);
            spec.accept(this.waste);
            return this;
        }
    }

    public static final class WasteSpec {

        private final String id;
        private final String name;
        private String texture = Constants.MOD_ID + ":items/rod_zirnox_empty";
        private float contamination = 15F;

        private WasteSpec(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public WasteSpec texture(String sprite) { this.texture = sprite; return this; }
        public WasteSpec contamination(float contamination) { this.contamination = contamination; return this; }
    }
}
