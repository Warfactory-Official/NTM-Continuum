package com.hbm.scripting.fuels;

import com.hbm.Constants;
import com.hbm.blocks.ModPwrBlocks;
import com.hbm.blocks.PwrParts;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.block.DescBlockItem;
import com.hbm.scripting.ScriptedContent;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import dev.golem.core.world.ItemModelJson;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

import java.util.Map;
import java.util.function.Consumer;

/**
 * PWR core parts counting as several of their kind in the controller's tallies.
 *
 * <pre>
 * e.pwrParts.heatsink('kjs_heatsink', 'KJS Heatsink', p => p
 *     .counts(2)                                         as n heatsinks; default 1
 *     .texture('kubejs:block/heatsink')                  cube; default the built-in's
 *     .desc('Counts as two heatsinks.'))                 tooltip lines
 * e.pwrParts.heatex(id, name, p => p.counts(3))
 * e.pwrParts.neutronSource(id, name, p => p.counts(2))   20 flux per count
 * e.pwrParts.channel(id, name, p => p.textures('kubejs:block/ch_side', 'kubejs:block/ch_top'))   pillar
 * </pre>
 * Heatsink total still capped at 80. Fuel, control, reflector, casing and port take no count: topology only.
 */
public final class PwrPartBlocks {

    private static final String PWR = Constants.MOD_ID + ":blocks/";

    public void heatsink(String id, String name, Consumer<PartSpec> spec) {
        part(id, name, PwrParts.Kind.HEATSINK, "pwr_heatsink", false, spec);
    }

    public void heatex(String id, String name, Consumer<PartSpec> spec) {
        part(id, name, PwrParts.Kind.HEATEX, "pwr_heatex", false, spec);
    }

    public void neutronSource(String id, String name, Consumer<PartSpec> spec) {
        part(id, name, PwrParts.Kind.SOURCE, "pwr_neutron_source", false, spec);
    }

    public void channel(String id, String name, Consumer<PartSpec> spec) {
        part(id, name, PwrParts.Kind.CHANNEL, "pwr_channel", true, spec);
    }

    private static void part(String id, String name, PwrParts.Kind kind, String base, boolean pillar, Consumer<PartSpec> spec) {
        PartSpec part = new PartSpec();
        spec.accept(part);
        RegistryObject<Block> block = ScriptedContent.blocks(ModPwrBlocks.BLOCKS).registerSimpleBlock(id,
                p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
        PwrParts.register(block, kind, part.counts);
        String[] desc = part.desc;
        RegistryObject<DescBlockItem> item = ScriptedContent.items(ModPwrBlocks.ITEMS).registerItem(id,
                p -> new DescBlockItem(block.get(), p, id, desc.length), Compat::blockDescription);
        ModTabs.addTo(HbmTab.MACHINES, item);
        for (int i = 0; i < desc.length; i++) ScriptedContent.lang("block." + Constants.MOD_ID + "." + id + ".desc." + i, desc[i]);
        MintedBlocks.declare(id, name, false);

        String model = Constants.MOD_ID + ":block/" + id;
        String side = part.side != null ? part.side : pillar ? PWR + base + "_side" : PWR + base;
        String top = part.top != null ? part.top : pillar ? PWR + base + "_top" : null;
        ScriptedContent.assets(out -> {
            out.json(Constants.MOD_ID, "models/block/" + id, top == null
                    ? MintedBlocks.retextured("minecraft:block/cube_all", Map.of("all", side))
                    : MintedBlocks.retextured("minecraft:block/cube_column", Map.of("end", top, "side", side)));
            out.json(Constants.MOD_ID, "blockstates/" + id, MintedBlocks.blockState(Map.of("", model)));
            ItemModelJson.definition((path, json) -> out.json(Constants.MOD_ID, path, json), id,
                    ItemModelJson.reference(model), model);
        });
    }

    public static final class PartSpec {

        private int counts = 1;
        private String side;
        private String top;
        private String[] desc = new String[0];

        public PartSpec counts(int n) { this.counts = n; return this; }
        public PartSpec texture(String sprite) { this.side = sprite; this.top = null; return this; }
        public PartSpec textures(String side, String top) { this.side = side; this.top = top; return this; }
        public PartSpec desc(String... lines) { this.desc = lines.clone(); return this; }
    }
}
