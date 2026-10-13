package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockBeaconable;
import com.hbm.blocks.generic.BlockLayering;
import com.hbm.blocks.generic.BlockStalagmite;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Shape;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Tints;
import dev.golem.core.dejson.Variants;
import com.hbm.items.ModItems;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import com.hbm.blocks.bomb.BlockVolcano;
import com.hbm.blocks.bomb.CrystalPulsar;
import com.hbm.blocks.bomb.CrystalVirus;
import com.hbm.blocks.gas.BlockGasCoal;
import com.hbm.blocks.generic.BlockBiomeStone;
import com.hbm.blocks.generic.BlockChlorineSeal;
import com.hbm.blocks.generic.BlockDeadPlant;
import com.hbm.blocks.generic.BlockFissure;
import com.hbm.blocks.generic.BlockGasChlorine;
import com.hbm.blocks.generic.BlockGeysir;
import com.hbm.blocks.generic.BlockHangingVine;
import com.hbm.blocks.generic.BlockHazardFalling;
import com.hbm.blocks.generic.BlockLithium;
import com.hbm.blocks.generic.BlockMeteorMolten;
import com.hbm.blocks.generic.BlockMush;
import com.hbm.blocks.generic.BlockMushHuge;
import com.hbm.blocks.generic.BlockNTMFlower;
import com.hbm.blocks.generic.BlockNuclearWaste;
import com.hbm.blocks.generic.BlockOreBasalt;
import com.hbm.blocks.generic.BlockPorous;
import com.hbm.blocks.generic.BlockReeds;
import com.hbm.blocks.generic.BlockSellafield;
import com.hbm.blocks.generic.BlockTallPlant;
import com.hbm.blocks.generic.BlockTerrainEarth;
import com.hbm.blocks.generic.BlockVent;

/**
 * The natural, terrain and hazard blocks of 1.7's {@code ModBlocks} not registered elsewhere:
 * the plant family, burning ground, stone and ores, geothermal and volcano cores, geysers, gases, hot
 * sellafite, cave spikes, dark crystals, material storage blocks and the gas vents.
 *
 * <p>Hardness, blast resistance, sound, light and creative tab follow each old registration line; the
 * old {@code blockTab} is {@link HbmTab#BLOCKS}, {@code machineTab} {@link HbmTab#MACHINES}, {@code nukeTab}
 * {@link HbmTab#BOMBS}. Metadata sub-blocks are flattened to {@code <base>_<variant>}.
 *
 * <p>Models, blockstate, loot and tags are stated on each registration. The hand-built {@code elements}
 * models {@code plant_reeds}, {@code vine_phosphor_*} and {@code ore_volcano} are authored files under
 * {@code assets/hbm/models/block/}. The packing recipes and the grass tint are in {@link TerrainDeclarations}.
 */
public final class ModTerrainBlocks {

    private ModTerrainBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    private static final String CUTOUT = "minecraft:cutout";
    /** One texture all round, single state, dropping itself, pickaxe. */
    private static Preset cube(String texture) {
        return Presets.cubeAll(texture).and(handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE));
    }
    /** A cut-out cross plant whose item is its flat sprite. */
    private static Preset plant(String texture) {
        return handle -> handle.model(d -> d.models(Models.renderType(CUTOUT, Models.cross(texture)))
                .states(Variants.simple()).item(Models.generated(texture)));
    }
    /** The tobacco/hemp tint as an item tint source (see {@link TerrainDeclarations#GRASS_TINT}). */
    private static Presets.ItemModel grassTinted(String texture) {
        return Presets.sprite(texture).tints(Tints.constant(0xFF000000 | TerrainDeclarations.GRASS_TINT));
    }

    private static Shape cross(boolean tinted, String texture) {
        return tinted ? Models.parented("minecraft:block/tinted_cross", Slots.of("cross", texture)) : Models.cross(texture);
    }

    /** Every block item registered here, by block id (for hazard lookup and the declarations). */
    public static final Map<String, RegistryObject<? extends Item>> BLOCK_ITEMS = new LinkedHashMap<>();

    private static final Map<BlockDeadPlant.Type, RegistryObject<BlockDeadPlant>> DEAD_PLANTS = new EnumMap<>(BlockDeadPlant.Type.class);
    private static final Map<BlockNTMFlower.Type, RegistryObject<BlockNTMFlower>> FLOWERS = new EnumMap<>(BlockNTMFlower.Type.class);
    private static final Map<BlockTallPlant.Type, RegistryObject<BlockTallPlant>> TALL_PLANTS = new EnumMap<>(BlockTallPlant.Type.class);
    private static final Map<BlockBiomeStone.Type, RegistryObject<BlockBiomeStone>> BIOME_STONE = new EnumMap<>(BlockBiomeStone.Type.class);
    private static final Map<BlockOreBasalt.Type, RegistryObject<BlockOreBasalt>> BASALT_ORES = new EnumMap<>(BlockOreBasalt.Type.class);
    private static final Map<String, RegistryObject<Block>> METEOR_ORES = new LinkedHashMap<>();
    private static final Map<String, RegistryObject<BlockBeaconable>> COKE_BLOCKS = new LinkedHashMap<>();
    @SuppressWarnings("unchecked")
    private static final RegistryObject<BlockSellafield>[] SELLAFIELD = new RegistryObject[BlockSellafield.LEVELS];
    private static final Map<String, RegistryObject<BlockStalagmite>> SPIKES = new LinkedHashMap<>();
    private static final Map<String, RegistryObject<BlockVolcano>> VOLCANO_CORES = new LinkedHashMap<>();

    // Plants (blockTab, grass sound)

    static {
        for (BlockDeadPlant.Type t : BlockDeadPlant.Type.values()) {
            // 1.7 getItemDropped -> null; the Java getDrops says so, the table exists for completeness
            DEAD_PLANTS.put(t, block(t.id(), p -> new BlockDeadPlant(p, t), ModTerrainBlocks::plantProps, HbmTab.BLOCKS)
                    .transform(plant(t.texture()))
                    .loot(Loot.nothing()));
        }
        for (BlockNTMFlower.Type t : BlockNTMFlower.Type.values()) {
            Reg.Handle<BlockNTMFlower> flower = block(t.id(), p -> new BlockNTMFlower(p, t), p -> plantProps(p).randomTicks(), HbmTab.BLOCKS);
            if (t.tinted()) {
                flower.model(d -> d.models(Models.renderType(CUTOUT, cross(true, t.texture()))).states(Variants.simple()))
                        .transform(grassTinted(t.texture()));
            } else {
                flower.transform(plant(t.texture()));
            }
            FLOWERS.put(t, flower.loot(Loot.self()));
        }
        for (BlockTallPlant.Type t : BlockTallPlant.Type.values()) {
            Reg.Handle<BlockTallPlant> tall = block(t.id(), p -> new BlockTallPlant(p, t), p -> plantProps(p).randomTicks(), HbmTab.BLOCKS);
            Models.Group group = Models.group()
                    .and("_lower", Models.renderType(CUTOUT, cross(t.tinted(), t.texture("lower"))))
                    .and("_upper", Models.renderType(CUTOUT, cross(t.tinted(), t.texture("upper"))));
            if (t.tinted()) {
                tall.model(d -> d.models(group).states(Variants.select("half", "lower", "_lower", "upper", "_upper")))
                        .transform(grassTinted(t.texture("lower")));
            } else {
                tall.model(d -> d.models(group).states(Variants.select("half", "lower", "_lower", "upper", "_upper"))
                        .item(Models.generated(t.texture("lower"))));
            }
            TALL_PLANTS.put(t, tall.loot(Loot.nothing())); // drops are computed in Java (flower item + willow leaves)
        }
    }

    /** The block model is the top segment only; RenderReeds (block entity) draws the column below. */
    public static final RegistryObject<BlockReeds> plant_reeds = block("plant_reeds", BlockReeds::new,
            ModTerrainBlocks::plantProps, HbmTab.BLOCKS)
            .transform(Presets.statesOnly(Variants.simple()))
            .transform(Presets.sprite("hbm:blocks/reeds_top"))
            .loot(Loot.only("minecraft:stick"));
    /** Authored vine_phosphor_ground/_middle/_hang (1.12.2's three models). */
    public static final RegistryObject<BlockHangingVine> vine_phosphor = block("vine_phosphor", BlockHangingVine::new,
            p -> p.strength(0.5F).sound(SoundType.GRASS).noOcclusion().noCollision(), HbmTab.BLOCKS)
            .transform(Presets.statesOnly(Variants.select("part", "ground", "_ground", "middle", "_middle", "hang", "_hang")))
            .transform(Presets.sprite("hbm:blocks/vine_phosphor_item"))
            .loot(Loot.self());
    /** {@code setLightLevel(0.5F)} = 7. */
    public static final RegistryObject<BlockMush> mush = block("mush", BlockMush::new,
            p -> plantProps(p).lightLevel(s -> 7).randomTicks(), HbmTab.BLOCKS)
            .transform(plant("hbm:blocks/mush"))
            .loot(Loot.self());
    /** No creative tab upstream (the huge mushroom is grown, not placed). */
    public static final RegistryObject<BlockMushHuge> mush_block = block("mush_block", BlockMushHuge::new,
            p -> p.strength(0.2F).sound(SoundType.GRASS).lightLevel(s -> 15), null)
            .transform(Presets.cubeAll("hbm:blocks/mush_block_skin"));
    public static final RegistryObject<BlockMushHuge> mush_block_stem = block("mush_block_stem", BlockMushHuge::new,
            p -> p.strength(0.2F).sound(SoundType.GRASS).lightLevel(s -> 15), null)
            .transform(Presets.column("hbm:blocks/mush_block_inside", "hbm:blocks/mush_block_stem"));
    /**
     * {@code BlockLayering(Material.leaves)}, texture {@code waste_leaves}, hardness 0.1: the snow-layer shape over
     * waste_leaves (as oil_spill / foam_layer).
     */
    public static final RegistryObject<BlockLayering> leaves_layer = block("leaves_layer", BlockLayering::new,
            p -> p.strength(0.1F).sound(SoundType.GRASS).noOcclusion().randomTicks(), HbmTab.BLOCKS)
            .model(d -> d.models(leafLayers())
                    .states(Variants.select("layers", "1", "_height2", "2", "_height4", "3", "_height6", "4", "_height8",
                            "5", "_height10", "6", "_height12", "7", "_height14", "8", "_full")))
            .icon(d -> d.models(Models.blockItem("hbm:block/leaves_layer_height2")))
            .loot(Loot.nothing()); // 1.7 BlockLayering drops nothing (BlockLayering.getDrops)

    private static Models.Group leafLayers() {
        String leaves = "hbm:blocks/waste_leaves";
        Models.Group layers = Models.group().and("_full", Models.renderType(CUTOUT, Models.cubeAll(leaves)));
        for (int h = 2; h <= 14; h += 2) {
            layers = layers.and("_height" + h, Models.renderType(CUTOUT, Models.parented("minecraft:block/snow_height" + h,
                    Slots.of("particle", leaves, "texture", leaves))));
        }
        return layers;
    }

    // Burning ground (frozen_* live in com.hbm.bombs2)

    public static final RegistryObject<BlockTerrainEarth> burning_earth = block("burning_earth",
            BlockTerrainEarth::new,
            p -> p.strength(0.6F).sound(SoundType.GRASS).randomTicks(), HbmTab.BLOCKS)
            .transform(Presets.cubeBottomTop("hbm:blocks/waste_earth_bottom", "hbm:blocks/burning_grass_side", "hbm:blocks/burning_grass_top"))
            .loot(Loot.nothing()) // drops dirt, computed in Java
            .tag(BlockTag.SHOVEL);

    // Stone and ores

    static {
        for (BlockBiomeStone.Type t : BlockBiomeStone.Type.values()) {
            String top = "hbm:blocks/stone_biome_top" + t.suffix();
            BIOME_STONE.put(t, block(t.id(), p -> new BlockBiomeStone(p, t), ModTerrainBlocks::stoneOre, HbmTab.BLOCKS)
                    .model(d -> d.models(Models.group()
                                    .and("", Models.cubeBottomTop(top, "hbm:blocks/stone_biome_layer" + t.suffix(), top))
                                    .and("_covered", Models.cubeBottomTop(top, "hbm:blocks/stone_biome" + t.suffix(), top)))
                            .states(Variants.select("covered", "false", "", "true", "_covered"))
                            .blockItemDirect())
                    .loot(Loot.self())
                    .tag(BlockTag.PICKAXE));
        }
        for (BlockOreBasalt.Type t : BlockOreBasalt.Type.values()) {
            BASALT_ORES.put(t, block(t.id(), p -> new BlockOreBasalt(p, t), ModTerrainBlocks::stoneOre, HbmTab.BLOCKS)
                    .transform(Presets.cubeBottomTop(t.texture() + "_top", t.texture(), t.texture() + "_top"))
                    .loot(Loot.nothing()) // one drop item, silk touch the block, in Java
                    .tag(BlockTag.PICKAXE));
        }
        for (String v : new String[]{"iron", "copper", "aluminium", "rareearth", "cobalt"}) {
            METEOR_ORES.put(v, block("ore_meteor_" + v, Block::new, ModTerrainBlocks::stoneOre, HbmTab.BLOCKS)
                    .transform(cube("hbm:blocks/ore_meteor." + v)));
        }
    }

    /** 1.7 {@code BlockPorous}: {@code BlockStone} with hardness 1.5 / resistance 30. */
    public static final RegistryObject<BlockPorous> stone_porous = block("stone_porous", BlockPorous::new,
            p -> p.strength(1.5F, 30.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), HbmTab.BLOCKS)
            .transform(Presets.cubeAll("hbm:blocks/stone_porous"))
            .loot(Loot.nothing()) // cobblestone / silk-touch stone in Java
            .tag(BlockTag.PICKAXE);
    /** {@code setLightLevel(0.75F)} = 11; hardness 15, resistance 360, random-ticked. */
    public static final RegistryObject<BlockMeteorMolten> block_meteor_molten = block("block_meteor_molten", BlockMeteorMolten::new,
            p -> p.strength(15.0F, 360.0F).sound(SoundType.STONE).lightLevel(s -> 11).requiresCorrectToolForDrops().randomTicks(), HbmTab.BLOCKS)
            .transform(Presets.cubeAll("hbm:blocks/meteor_cobble_molten"))
            .loot(Loot.nothing())
            .tag(BlockTag.PICKAXE);
    /** Unbreakable, resistance 1,000,000, full light, random-ticked. */
    public static final RegistryObject<BlockFissure> ore_volcano = block("ore_volcano", BlockFissure::new,
            p -> p.strength(-1.0F, 1_000_000F).sound(SoundType.STONE).lightLevel(s -> 15).randomTicks(), HbmTab.BLOCKS)
            // fissure: authored bedrock + molten overlay model, both crater states
            .transform(Presets.statesOnly(Variants.uniform("crater", "false", "true")))
            .icon(d -> d.models(Models.blockItem("hbm:block/ore_volcano")))
            .loot(Loot.nothing());

    static {
        for (boolean rad : new boolean[]{false, true}) {
            for (BlockVolcano.Mode mode : BlockVolcano.Mode.values()) {
                String id = mode.id(rad);
                Reg.Handle<BlockVolcano> core = REG.block(id, p -> new BlockVolcano(p, mode, rad),
                                p -> p.strength(-1.0F, 10_000F)) // 1.7: no setStepSound -> default stone
                        .transform(Presets.cubeAll(rad ? "hbm:blocks/volcano_rad_core" : "hbm:blocks/volcano_core"))
                        .loot(Loot.self());
                RegistryObject<BlockVolcano.VolcanoItem> item = REG.item(id,
                        p -> new BlockVolcano.VolcanoItem(core.get(), p, mode), Compat::blockDescription)
                        .tab(HbmTab.BOMBS);
                BLOCK_ITEMS.put(id, item);
                VOLCANO_CORES.put(id, core);
            }
        }
    }

    /** Geysers had no creative tab upstream and drop nothing. */
    public static final RegistryObject<BlockGeysir> geysir_chlorine = block("geysir_chlorine",
            p -> new BlockGeysir(p, BlockGeysir.Kind.CHLORINE),
            p -> p.strength(5.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), null)
            .transform(Presets.cubeBottomTop("minecraft:block/stone", "minecraft:block/stone", "hbm:blocks/geysir_stone")
                    .states(Variants.uniform("active", "false", "true")))
            .loot(Loot.nothing())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockGeysir> geysir_nether = block("geysir_nether",
            p -> new BlockGeysir(p, BlockGeysir.Kind.NETHER),
            p -> p.strength(2.0F).sound(SoundType.STONE).lightLevel(s -> 15).requiresCorrectToolForDrops(), null)
            .transform(Presets.cubeBottomTop("minecraft:block/netherrack", "minecraft:block/netherrack", "hbm:blocks/geysir_nether")
                    .states(Variants.uniform("active", "false", "true")))
            .loot(Loot.nothing())
            .tag(BlockTag.PICKAXE);

    // Gases (worldgen and vent cells: no item, like ModOreBlocks)

    /** Invisible coal dust (smoke particles only). */
    public static final RegistryObject<BlockGasCoal> gas_coal = REG.block("gas_coal", BlockGasCoal::new, ModTerrainBlocks::gasProps)
            .transform(Presets.statesOnly("hbm:block/gas_radon", Variants.simple()));
    /** Translucent chlorine. */
    public static final RegistryObject<BlockGasChlorine> chlorine_gas = REG.block("chlorine_gas", BlockGasChlorine::new, ModTerrainBlocks::gasProps)
            .transform(Presets.shape(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/chlorine_gas"))).noItem());

    // Hot sellafite (sellafield metas 0..5)

    static {
        for (int i = 0; i < BlockSellafield.LEVELS; i++) {
            int level = i;
            SELLAFIELD[i] = block("sellafield_" + i, p -> new BlockSellafield(p, level),
                    p -> p.strength(5.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().randomTicks(), HbmTab.BLOCKS)
                    .transform(cube("hbm:blocks/sellafield_" + i));
        }
    }

    // Cave spikes (EnumStalagmiteType ICE.. GLYPHID3)

    static {
        spike("ice", () -> new ItemStack(ModItems.powder_ice.get()));
        spike("snow", () -> new ItemStack(Items.SNOWBALL));
        spike("glyphid1", () -> ItemStack.EMPTY);
        spike("glyphid2", () -> ItemStack.EMPTY);
        spike("glyphid3", () -> ItemStack.EMPTY);
    }

    // Dark crystals (no tab)

    public static final RegistryObject<CrystalVirus> crystal_virus = block("crystal_virus", CrystalVirus::new,
            p -> p.strength(15.0F, Float.POSITIVE_INFINITY).requiresCorrectToolForDrops().randomTicks(), null) // 1.7: default stone sound
            .transform(cube("hbm:blocks/crystal_virus"));
    public static final RegistryObject<CrystalPulsar> crystal_pulsar = block("crystal_pulsar", CrystalPulsar::new,
            p -> p.strength(15.0F, Float.POSITIVE_INFINITY).requiresCorrectToolForDrops(), null) // 1.7: default stone sound
            .transform(cube("hbm:blocks/crystal_pulsar"));

    // Material and storage blocks

    public static final RegistryObject<BlockBeaconable> block_tcalloy = block("block_tcalloy", BlockBeaconable::new,
            p -> p.strength(5.0F, 70.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), HbmTab.BLOCKS)
            .transform(cube("hbm:blocks/block_tcalloy"));
    public static final RegistryObject<BlockBeaconable> block_cdalloy = block("block_cdalloy", BlockBeaconable::new,
            p -> p.strength(5.0F, 70.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), HbmTab.BLOCKS)
            .transform(cube("hbm:blocks/block_cdalloy"));
    public static final RegistryObject<BlockBeaconable> block_australium = block("block_australium", BlockBeaconable::new,
            p -> p.strength(5.0F, 10.0F).requiresCorrectToolForDrops(), HbmTab.BLOCKS) // 1.7: no setStepSound -> default stone
            .transform(cube("hbm:blocks/block_australium"));

    static {
        for (String v : new String[]{"coal", "lignite", "petroleum"}) {
            // 1.7 BlockCoke: flammability 5, spread 10. No cross-loader flammability seam exists yet.
            COKE_BLOCKS.put(v, block("block_coke_" + v, BlockBeaconable::new,
                    p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), HbmTab.BLOCKS)
                    .transform(cube("hbm:blocks/block_coke." + v)));
        }
    }

    public static final RegistryObject<BlockLithium> block_lithium = block("block_lithium", BlockLithium::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), HbmTab.BLOCKS)
            .transform(cube("hbm:blocks/block_lithium"));

    public static final RegistryObject<BlockNuclearWaste> block_waste = waste("block_waste");
    public static final RegistryObject<BlockNuclearWaste> block_waste_painted = waste("block_waste_painted");
    public static final RegistryObject<BlockNuclearWaste> block_waste_vitrified = waste("block_waste_vitrified");

    public static final RegistryObject<BlockHazardFalling> block_yellowcake = block("block_yellowcake", BlockHazardFalling::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.SAND), HbmTab.BLOCKS)
            .transform(Presets.cubeAll("hbm:blocks/block_yellowcake"))
            .loot(Loot.self())
            .tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockHazardFalling> block_fallout = block("block_fallout", BlockHazardFalling::new,
            p -> p.strength(0.2F).sound(SoundType.GRAVEL), HbmTab.BLOCKS)
            .transform(Presets.cubeAll("hbm:blocks/ash"))
            .loot(Loot.self())
            .tag(BlockTag.SHOVEL);

    // Vents (machineTab, Material.iron)

    public static final RegistryObject<BlockVent> vent_chlorine = vent("vent_chlorine", BlockVent.Kind.CHLORINE);
    public static final RegistryObject<BlockVent> vent_cloud = vent("vent_cloud", BlockVent.Kind.CLOUD);
    public static final RegistryObject<BlockVent> vent_pink_cloud = vent("vent_pink_cloud", BlockVent.Kind.PINK_CLOUD);
    public static final RegistryObject<BlockChlorineSeal> vent_chlorine_seal = block("vent_chlorine_seal", BlockChlorineSeal::new,
            p -> p.strength(5.0F, 10.0F).requiresCorrectToolForDrops(), HbmTab.MACHINES) // 1.7: default stone sound
            .transform(Presets.cubeBottomTop("hbm:blocks/vent_chlorine_seal_side", "hbm:blocks/vent_chlorine_seal_side", "hbm:blocks/vent_chlorine_seal_top"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    // Lookups

    public static BlockDeadPlant plant_dead(BlockDeadPlant.Type type) { return DEAD_PLANTS.get(type).get(); }
    public static BlockNTMFlower plant_flower(BlockNTMFlower.Type type) { return FLOWERS.get(type).get(); }
    public static BlockTallPlant plant_tall(BlockTallPlant.Type type) { return TALL_PLANTS.get(type).get(); }
    public static BlockBiomeStone stone_biome(BlockBiomeStone.Type type) { return BIOME_STONE.get(type).get(); }
    public static BlockOreBasalt ore_basalt(BlockOreBasalt.Type type) { return BASALT_ORES.get(type).get(); }
    public static Block ore_meteor(String variant) { return METEOR_ORES.get(variant).get(); }
    public static BlockBeaconable block_coke(String variant) { return COKE_BLOCKS.get(variant).get(); }
    public static BlockSellafield sellafield(int level) { return SELLAFIELD[level].get(); }
    public static BlockStalagmite spike(String id) { return SPIKES.get(id).get(); }
    public static BlockVolcano volcanoCore(String id) { return VOLCANO_CORES.get(id).get(); }
    public static Iterable<RegistryObject<BlockVolcano>> volcanoCores() { return VOLCANO_CORES.values(); }
    public static Iterable<RegistryObject<BlockSellafield>> sellafieldLevels() { return Arrays.asList(SELLAFIELD); }

    // Helpers

    private static <B extends Block> Reg.Handle<B> block(String id, Function<BlockBehaviour.Properties, ? extends B> factory,
                                                         UnaryOperator<BlockBehaviour.Properties> props, HbmTab tab) {
        Reg.Handle<B> block = REG.block(id, factory, props);
        Reg.Handle<BlockItem> item = REG.blockItem(block, Compat::blockDescription);
        BLOCK_ITEMS.put(id, item);
        if (tab != null) item.tab(tab);
        return block;
    }

    /** 1.7 plants: {@code Material.plants}, hardness 0, grass sound, no collision. */
    private static BlockBehaviour.Properties plantProps(BlockBehaviour.Properties p) {
        return p.strength(0.0F).sound(SoundType.GRASS).noOcclusion().noCollision();
    }

    private static BlockBehaviour.Properties stoneOre(BlockBehaviour.Properties p) {
        return p.strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    /** Old {@code BlockGasBase}: hardness 0, no collision, no occlusion, replaceable, random-ticked (as ModOreBlocks). */
    private static BlockBehaviour.Properties gasProps(BlockBehaviour.Properties p) {
        return p.strength(0.0F, 0.0F).noOcclusion().noCollision().replaceable().randomTicks().noLootTable();
    }

    private static void spike(String variant, Supplier<ItemStack> drop) {
        for (String base : new String[]{"stalagmite", "stalactite"}) {
            String id = base + "_" + variant.toLowerCase(Locale.ROOT);
            Direction anchor = base.equals("stalagmite") ? Direction.DOWN : Direction.UP;
            SPIKES.put(id, block(id, p -> new BlockStalagmite(p, anchor, drop),
                    p -> p.strength(0.5F, 2.0F).sound(SoundType.STONE).noOcclusion().noCollision(), HbmTab.BLOCKS)
                    .transform(plant("hbm:blocks/" + base + "." + variant.toLowerCase(Locale.ROOT)))
                    .loot(Loot.nothing())); // BlockStalagmite computes its own drops
        }
    }

    private static RegistryObject<BlockNuclearWaste> waste(String id) {
        return block(id, BlockNuclearWaste::new,
                p -> p.strength(5.0F, 10.0F).requiresCorrectToolForDrops().randomTicks(), HbmTab.BLOCKS) // 1.7: default stone sound
                .transform(cube("hbm:blocks/" + id));
    }

    private static RegistryObject<BlockVent> vent(String id, BlockVent.Kind kind) {
        return block(id, p -> new BlockVent(p, kind),
                p -> p.strength(5.0F, 10.0F).requiresCorrectToolForDrops(), HbmTab.MACHINES) // 1.7: default stone sound
                .transform(Presets.column("hbm:blocks/vent_blank", "hbm:blocks/" + id))
                .loot(Loot.nothing())
                .tag(BlockTag.PICKAXE);
    }

    public static void init() {}
}
