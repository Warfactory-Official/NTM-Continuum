package com.hbm.scripting.fuels;

import com.google.gson.JsonElement;
import com.hbm.Constants;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import com.hbm.scripting.ScriptedContent;
import dev.golem.core.world.ItemModelJson;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Chicago Pile rods: variants of the one item {@code pile_rod}, shown on the control tab.
 *
 * <pre>
 * e.pile.add('kjs_heu', 'KJS Pile Rod', p => p
 *     .reaction(1.2).life(20000).heat(0.4)         flux mult on sqrt(flux), flux to spend, heat per neutron
 *     .turnsInto('waste')                          spent rod: pile rod id, built-in or scripted; required with life
 *     .desc('Bred from KJS.')                      tooltip line
 *     .texture('kubejs:item/pile_rod'))            default hbm:items/pile_rod_nu
 *
 * e.pile.add('kjs_source', 'KJS Source', p => p.neutronSource(2))
 * </pre>
 * Variant index = built-ins, then scripted in declaration order; persisted on stacks => reorder shifts saved rods.
 * Name key {@code item.hbm.pile_rod.<id>}. No crafting helper: outputs carry the variant component.
 */
public final class PileFuels {

    private static boolean modelsHooked;

    public void add(String id, String name, Consumer<RodSpec> spec) {
        RodSpec rod = new RodSpec();
        spec.accept(rod);
        if (rod.life > 0D && rod.turnsInto == null) {
            throw new IllegalArgumentException("Pile rod " + id + " has a life and needs turnsInto(...)");
        }
        String model = Constants.MOD_ID + ":item/pile_rod_" + id.toLowerCase(Locale.ROOT);
        EnumPileRod type = EnumPileRod.create(id, rod.neutronSource, rod.reaction, rod.life, rod.heat, rod.turnsInto,
                model, rod.desc != null);
        String key = "item." + Constants.MOD_ID + ".pile_rod." + type.id();
        ScriptedContent.lang(key, name);
        if (rod.desc != null) ScriptedContent.lang(key + ".desc", rod.desc);
        String texture = rod.texture;
        ScriptedContent.assets(out -> out.json(Constants.MOD_ID, "models/item/pile_rod_" + type.id(), ItemModelJson.generated(texture)));
        hookModels();
    }

    private static synchronized void hookModels() {
        if (modelsHooked) return;
        modelsHooked = true;
        ScriptedContent.assets(out -> itemModels((path, json) -> out.json(Constants.MOD_ID, path, json)));
    }

    /** The {@code pile_rod} variant selection over every type, built-in and scripted. */
    public static void itemModels(BiConsumer<String, JsonElement> out) {
        List<String> models = EnumPileRod.values().stream().map(type -> type.model).toList();
        ItemModelJson.variantSelect(out, "pile_rod", Constants.MOD_ID + ":item_variant",
                ItemModelJson.generated(Constants.MOD_ID + ":items/pile_rod_ra226be"), models);
    }

    public static final class RodSpec {

        private double neutronSource;
        private double reaction;
        private double life;
        private double heat;
        private String turnsInto;
        private String desc;
        private String texture = Constants.MOD_ID + ":items/pile_rod_nu";

        public RodSpec neutronSource(double neutrons) { this.neutronSource = neutrons; return this; }
        public RodSpec reaction(double mult) { this.reaction = mult; return this; }
        public RodSpec life(double flux) { this.life = flux; return this; }
        public RodSpec heat(double perNeutron) { this.heat = perNeutron; return this; }
        public RodSpec turnsInto(String id) { this.turnsInto = id; return this; }
        public RodSpec desc(String line) { this.desc = line; return this; }
        public RodSpec texture(String sprite) { this.texture = sprite; return this; }
    }
}
