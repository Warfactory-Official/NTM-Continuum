package com.hbm.scripting;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import dev.golem.core.world.BiomeFeatures;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * {@code NTMEvents.materials}. A material's id must be free and stays fixed: crucibles save it.
 *
 * <pre>
 * e.create(12000, 'mithril')
 *   .colors(0xE0E0FF, 0x303060, 0xA0A0FF)     light, dark (autogen sprite band), molten
 *   .displayName('Mithril')
 *   .smelting('SMELTABLE')                     NOT_SMELTABLE VAPORIZES BREAKS SMELTABLE ADDITIVE
 *   .convertsTo('iron', 2, 1)                  melts into 1 iron per 2 mithril
 *   .autogen('CASTPLATE', 'SHELL', 'GEAR')     shape names; items are minted for autogen shapes
 *   .items('INGOT', 'NUGGET', 'DUST', 'PLATE', 'BILLET', 'CRYSTAL')   &lt;prefix&gt;_mithril in c:&lt;plural&gt;/mithril
 *   .block()                                   block_mithril; 9 ingots/nuggets craft up and back
 *   .texture('INGOT', 'kubejs:item/mithril_ingot')   authored sprite; required for shapes outside the list above
 *   .radioactive(5)                            RAD/s per ingot, scaled per shape like built-in isotopes
 *   .radioactive(5, 'hot', 3)                  plus one more hazard on every shape
 *   .ore(o => o
 *       .dimension('overworld')                overworld nether end
 *       .stone('stone', 'deepslate')           ore_mithril, then ore_deepslate_mithril; netherrack end_stone or a block id
 *       .veinSize(8).count(10).height(-32, 64) uniform; triangle(min, max) for a trapezoid
 *       .drops('INGOT', 1, 3)                  'self' (default) | an item id | a shape: this material's item of it
 *       .xp(0, 2).hardness(3).tool('iron'))    stone iron diamond; null: any pickaxe
 * e.get('iron').autogen('SHELL')
 * e.smelting('minecraft:ancient_debris', '72x mithril', '8x iron')   crucible yield, replaces tag-derived
 * e.smelting('#c:ores/mithril', '144x mithril')
 * </pre>
 * items, block and ore only on materials this script creates. Minted items melt into their shape's quantity
 * when the material smelts; ores into 2 ingots plus stone, like built-in ores. Ore textures: stone base plus
 * the overlay art recoloured, or {@code texture('ORE', overlaySprite)}.
 */
public final class MaterialsEvent {

    MaterialsEvent() {}

    public MaterialBuilder create(int id, String name) {
        return new MaterialBuilder(Mats.register(id, name), true);
    }

    public MaterialBuilder get(String name) {
        return new MaterialBuilder(ScriptSpecs.material(name), false);
    }

    public void smelting(String input, Spec.Molten... molten) {
        ScriptedContent.whenRegistered(() -> {
            List<Mats.MaterialStack> stacks = List.of(ScriptSpecs.molten(molten));
            if (input.startsWith("#")) Mats.registerTagEntry(input.substring(1), stacks);
            else Mats.registerEntry(ScriptSpecs.item(input), stacks);
        });
    }

    public static final class MaterialBuilder {

        private final NTMMaterial material;
        private final boolean created;

        private MaterialBuilder(NTMMaterial material, boolean created) {
            this.material = material;
            this.created = created;
        }

        public MaterialBuilder colors(int light, int dark, int molten) {
            material.colors(light, dark, molten);
            return this;
        }

        public MaterialBuilder displayName(String name) {
            material.displayName(name);
            return this;
        }

        public MaterialBuilder smelting(String behavior) {
            material.smelting(ScriptSpecs.constant(NTMMaterial.SmeltingBehavior.class, behavior));
            return this;
        }

        public MaterialBuilder convertsTo(String into, int in, int out) {
            material.conv(ScriptSpecs.material(into), in, out);
            return this;
        }

        public MaterialBuilder autogen(String... shapes) {
            for (String shape : shapes) material.setAutogen(ScriptSpecs.shape(shape));
            return this;
        }

        public MaterialBuilder items(String... shapes) {
            ScriptedMaterials.Declared spec = minting();
            for (String shape : shapes) {
                MaterialShapes resolved = ScriptSpecs.shape(shape);
                if (resolved == MaterialShapes.BLOCK || resolved == MaterialShapes.ORE) {
                    throw new IllegalArgumentException(shape + " is minted by block() / ore()");
                }
                spec.items.add(resolved);
            }
            return this;
        }

        public MaterialBuilder block() {
            minting().block = true;
            return this;
        }

        public MaterialBuilder texture(String shape, String sprite) {
            ScriptedMaterials.spec(material).textures.put(ScriptSpecs.shape(shape), sprite);
            return this;
        }

        public MaterialBuilder radioactive(double radsPerIngot) {
            ScriptedMaterials.spec(material).rads = radsPerIngot;
            return this;
        }

        public MaterialBuilder radioactive(double radsPerIngot, String hazard, double level) {
            ScriptedMaterials.Declared spec = ScriptedMaterials.spec(material);
            spec.rads = radsPerIngot;
            spec.hazard = hazard;
            spec.hazardLevel = level;
            return this;
        }

        public MaterialBuilder ore(Consumer<OreSpec> configure) {
            OreSpec ore = new OreSpec();
            configure.accept(ore);
            minting().ore = ore;
            return this;
        }

        private ScriptedMaterials.Declared minting() {
            if (!created) throw new IllegalStateException(material.name + " is built in; items, block and ore mint only for created materials");
            return ScriptedMaterials.spec(material);
        }
    }

    public static final class OreSpec {

        BiomeFeatures.Dimension dimension = BiomeFeatures.Dimension.OVERWORLD;
        private List<String> stones;
        int veinSize = 8;
        int count = 8;
        int minY = 0;
        int maxY = 64;
        boolean triangle;
        String drop = "self";
        int dropMin = 1;
        int dropMax = 1;
        int xpMin;
        int xpMax;
        float hardness = 3F;
        String tool = "stone";

        OreSpec() {}

        public OreSpec dimension(String dimension) {
            this.dimension = ScriptSpecs.constant(BiomeFeatures.Dimension.class, dimension);
            return this;
        }

        public OreSpec stone(String... stones) {
            this.stones = List.of(stones);
            return this;
        }

        public OreSpec veinSize(int blocks) { this.veinSize = blocks; return this; }
        public OreSpec count(int perChunk) { this.count = perChunk; return this; }
        public OreSpec height(int min, int max) { this.minY = min; this.maxY = max; this.triangle = false; return this; }
        public OreSpec triangle(int min, int max) { this.minY = min; this.maxY = max; this.triangle = true; return this; }
        public OreSpec drops(String item) { return drops(item, 1, 1); }
        public OreSpec drops(String item, int min, int max) { this.drop = item; this.dropMin = min; this.dropMax = max; return this; }
        public OreSpec xp(int min, int max) { this.xpMin = min; this.xpMax = max; return this; }
        public OreSpec hardness(float hardness) { this.hardness = hardness; return this; }

        public OreSpec tool(String tier) {
            this.tool = tier == null ? null : tier.toLowerCase(Locale.US);
            return this;
        }

        List<String> stones() {
            if (stones != null && !stones.isEmpty()) return stones;
            return switch (dimension) {
                case OVERWORLD -> List.of("stone");
                case NETHER -> List.of("netherrack");
                case END -> List.of("end_stone");
            };
        }
    }
}
