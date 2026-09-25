package com.hbm.scripting.fuels;

import com.hbm.Constants;
import com.hbm.items.ModRtgContent;
import com.hbm.items.machine.DepletedRTGMaterial;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.lib.TagIds;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.ScriptedContent;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * <pre>
 * e.rtg.add('kjs_pu238', 'KJS RTG Pellet', r => r
 *     .heat(10).halfLife(768252000).halfLives(2)   heat <= 32767; lifespan = halfLife * halfLives ticks
 *     .decay('LEAD')                               BISMUTH MERCURY NEPTUNIUM LEAD ZIRCONIUM NICKEL | 'ns:item'
 *     .texture('kubejs:item/my_pellet')            default hbm:items/pellet_rtg
 *     .radiation(5).hot(3).blinding(0)             RAD/s, scaled by remaining power; hot/blinding flat, 0 = none
 *     .recipe('hbm:billet_pu238'))                 3 billets + #c:plates/iron; also .shapeless(...) .shaped(rows, key)
 * </pre>
 * Item {@code pellet_rtg_<id>}; name is literal, not translated.
 */
public final class RtgFuels {

    public void add(String id, String name, Consumer<PelletSpec> spec) {
        PelletSpec pellet = new PelletSpec();
        spec.accept(pellet);
        String pelletId = "pellet_rtg_" + id;
        Supplier<? extends Item> decay = pellet.decay();
        RegistryObject<ItemRTGPellet> item = ModRtgContent.registerPellet(ScriptedContent.items(ModRtgContent.ITEMS),
                pelletId, pellet.heat, pellet.halfLife, pellet.halfLives, decay, name);
        ScriptedContent.itemAsset(pelletId, pellet.texture, name);
        pellet.recipes.emit(pelletId);
        ScriptedContent.whenRegistered(() -> {
            decay.get();
            if (pellet.radiation != null) ModRtgContent.rtgHazard(item.get(), pellet.radiation.floatValue(),
                    (float) pellet.hot, (float) pellet.blinding);
        });
    }

    public static final class PelletSpec {

        private int heat = 10;
        private long halfLife = 768_252_000L;
        private int halfLives = 2;
        private String decay = DepletedRTGMaterial.LEAD.name();
        private String texture = Constants.MOD_ID + ":items/pellet_rtg";
        private Double radiation;
        private double hot;
        private double blinding;
        private final FuelRecipes.Pending recipes = new FuelRecipes.Pending();

        public PelletSpec heat(int heat) {
            if (heat < 0 || heat > Short.MAX_VALUE) throw new IllegalArgumentException("RTG heat out of range: " + heat);
            this.heat = heat;
            return this;
        }
        public PelletSpec halfLife(long ticks) { this.halfLife = ticks; return this; }
        public PelletSpec halfLives(int halfLives) { this.halfLives = halfLives; return this; }
        public PelletSpec decay(String item) {
            if (item.indexOf(':') < 0) ScriptSpecs.constant(DepletedRTGMaterial.class, item);
            this.decay = item;
            return this;
        }
        public PelletSpec texture(String sprite) { this.texture = sprite; return this; }
        public PelletSpec radiation(double radiation) { this.radiation = radiation; return this; }
        public PelletSpec hot(double hot) { this.hot = hot; return this; }
        public PelletSpec blinding(double blinding) { this.blinding = blinding; return this; }
        public PelletSpec recipe(String billet) {
            recipes.add(item -> FuelRecipes.shapeless(item, 1, billet, billet, billet, TagIds.plates_iron));
            return this;
        }
        public PelletSpec shapeless(String... ingredients) { recipes.shapeless(ingredients); return this; }
        public PelletSpec shaped(String[] pattern, Map<?, ?> key) { recipes.shaped(pattern, key); return this; }

        private Supplier<? extends Item> decay() {
            if (decay.indexOf(':') < 0) {
                DepletedRTGMaterial material = ScriptSpecs.constant(DepletedRTGMaterial.class, decay);
                return () -> ModRtgContent.DEPLETED.get(material).get();
            }
            String id = decay;
            Item[] resolved = new Item[1];
            return () -> resolved[0] != null ? resolved[0] : (resolved[0] = ScriptSpecs.item(id));
        }
    }
}
