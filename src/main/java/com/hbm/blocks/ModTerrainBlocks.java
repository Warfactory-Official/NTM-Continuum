package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockBeaconable;
import com.hbm.blocks.generic.BlockLayering;
import com.hbm.blocks.generic.BlockStalagmite;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ModItems;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
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
 * The natural / terrain / hazard blocks of 1.7's {@code ModBlocks} that no earlier wave registered:
 * the plant family, the burning ground, biome and porous stone, basalt and meteor ores, the
 * geothermal fissure and volcano cores, geysers, coal dust and chlorine gas, hot sellafite, the remaining
 * cave spikes, the dark crystals, the material storage blocks (technetium/cadmium steel, australium, coke,
 * lithium, nuclear waste, yellowcake, fallout) and the gas vents.
 *
 * <p>Hardness, blast resistance, sound, light and creative tab follow each old registration line; the
 * old {@code blockTab} is {@link HbmTab#BLOCKS}, {@code machineTab} {@link HbmTab#MACHINES}, {@code nukeTab}
 * {@link HbmTab#BOMBS}. Metadata sub-blocks are flattened to {@code <base>_<variant>}.
 */
public final class ModTerrainBlocks {

    private ModTerrainBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

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

    // ---------------------------------------------------------------- plants (blockTab, grass sound)

    static {
        for (BlockDeadPlant.Type t : BlockDeadPlant.Type.values()) {
            DEAD_PLANTS.put(t, block(t.id(), p -> new BlockDeadPlant(p, t), ModTerrainBlocks::plantProps, HbmTab.BLOCKS));
        }
        for (BlockNTMFlower.Type t : BlockNTMFlower.Type.values()) {
            FLOWERS.put(t, block(t.id(), p -> new BlockNTMFlower(p, t), p -> plantProps(p).randomTicks(), HbmTab.BLOCKS));
        }
        for (BlockTallPlant.Type t : BlockTallPlant.Type.values()) {
            TALL_PLANTS.put(t, block(t.id(), p -> new BlockTallPlant(p, t), p -> plantProps(p).randomTicks(), HbmTab.BLOCKS));
        }
    }

    public static final RegistryObject<BlockReeds> plant_reeds = block("plant_reeds", BlockReeds::new,
            ModTerrainBlocks::plantProps, HbmTab.BLOCKS);
    public static final RegistryObject<BlockHangingVine> vine_phosphor = block("vine_phosphor", BlockHangingVine::new,
            p -> p.strength(0.5F).sound(SoundType.GRASS).noOcclusion().noCollision(), HbmTab.BLOCKS);
    /** {@code setLightLevel(0.5F)} = 7. */
    public static final RegistryObject<BlockMush> mush = block("mush", BlockMush::new,
            p -> plantProps(p).lightLevel(s -> 7).randomTicks(), HbmTab.BLOCKS);
    /** No creative tab upstream (the huge mushroom is grown, not placed). */
    public static final RegistryObject<BlockMushHuge> mush_block = block("mush_block", BlockMushHuge::new,
            p -> p.strength(0.2F).sound(SoundType.GRASS).lightLevel(s -> 15), null);
    public static final RegistryObject<BlockMushHuge> mush_block_stem = block("mush_block_stem", BlockMushHuge::new,
            p -> p.strength(0.2F).sound(SoundType.GRASS).lightLevel(s -> 15), null);
    /** {@code BlockLayering(Material.leaves)}, texture {@code waste_leaves}, hardness 0.1. */
    public static final RegistryObject<BlockLayering> leaves_layer = block("leaves_layer", BlockLayering::new,
            p -> p.strength(0.1F).sound(SoundType.GRASS).noOcclusion().randomTicks(), HbmTab.BLOCKS);

    // ---------------------------------------------------------------- burning ground (frozen_* live in com.hbm.bombs2)

    public static final RegistryObject<BlockTerrainEarth> burning_earth = block("burning_earth",
            BlockTerrainEarth::new,
            p -> p.strength(0.6F).sound(SoundType.GRASS).randomTicks(), HbmTab.BLOCKS);

    // ---------------------------------------------------------------- stone / ores

    static {
        for (BlockBiomeStone.Type t : BlockBiomeStone.Type.values()) {
            BIOME_STONE.put(t, block(t.id(), p -> new BlockBiomeStone(p, t), ModTerrainBlocks::stoneOre, HbmTab.BLOCKS));
        }
        for (BlockOreBasalt.Type t : BlockOreBasalt.Type.values()) {
            BASALT_ORES.put(t, block(t.id(), p -> new BlockOreBasalt(p, t), ModTerrainBlocks::stoneOre, HbmTab.BLOCKS));
        }
        for (String v : new String[]{"iron", "copper", "aluminium", "rareearth", "cobalt"}) {
            METEOR_ORES.put(v, block("ore_meteor_" + v, Block::new, ModTerrainBlocks::stoneOre, HbmTab.BLOCKS));
        }
    }

    /** 1.7 {@code BlockPorous}: {@code BlockStone} with hardness 1.5 / resistance 30. */
    public static final RegistryObject<BlockPorous> stone_porous = block("stone_porous", BlockPorous::new,
            p -> p.strength(1.5F, 30.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), HbmTab.BLOCKS);
    /** {@code setLightLevel(0.75F)} = 11; hardness 15, resistance 360, random-ticked. */
    public static final RegistryObject<BlockMeteorMolten> block_meteor_molten = block("block_meteor_molten", BlockMeteorMolten::new,
            p -> p.strength(15.0F, 360.0F).sound(SoundType.STONE).lightLevel(s -> 11).requiresCorrectToolForDrops().randomTicks(), HbmTab.BLOCKS);
    /** Unbreakable, resistance 1,000,000, full light, random-ticked. */
    public static final RegistryObject<BlockFissure> ore_volcano = block("ore_volcano", BlockFissure::new,
            p -> p.strength(-1.0F, 1_000_000F).sound(SoundType.STONE).lightLevel(s -> 15).randomTicks(), HbmTab.BLOCKS);

    static {
        for (boolean rad : new boolean[]{false, true}) {
            for (BlockVolcano.Mode mode : BlockVolcano.Mode.values()) {
                String id = mode.id(rad);
                RegistryObject<BlockVolcano> core = BLOCKS.registerBlock(id, p -> new BlockVolcano(p, mode, rad),
                        p -> p.strength(-1.0F, 10_000F)); // 1.7: no setStepSound -> default stone
                RegistryObject<BlockVolcano.VolcanoItem> item = ITEMS.registerItem(id,
                        p -> new BlockVolcano.VolcanoItem(core.get(), p, mode), Compat::blockDescription);
                BLOCK_ITEMS.put(id, item);
                ModTabs.addTo(HbmTab.BOMBS, item);
                VOLCANO_CORES.put(id, core);
            }
        }
    }

    /** Geysers had no creative tab upstream and drop nothing. */
    public static final RegistryObject<BlockGeysir> geysir_chlorine = block("geysir_chlorine",
            p -> new BlockGeysir(p, BlockGeysir.Kind.CHLORINE),
            p -> p.strength(5.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), null);
    public static final RegistryObject<BlockGeysir> geysir_nether = block("geysir_nether",
            p -> new BlockGeysir(p, BlockGeysir.Kind.NETHER),
            p -> p.strength(2.0F).sound(SoundType.STONE).lightLevel(s -> 15).requiresCorrectToolForDrops(), null);

    // ---------------------------------------------------------------- gases (worldgen / vent cells: no item, like ModOreBlocks)

    public static final RegistryObject<BlockGasCoal> gas_coal = BLOCKS.registerBlock("gas_coal", BlockGasCoal::new, ModTerrainBlocks::gasProps);
    public static final RegistryObject<BlockGasChlorine> chlorine_gas = BLOCKS.registerBlock("chlorine_gas", BlockGasChlorine::new, ModTerrainBlocks::gasProps);

    // ---------------------------------------------------------------- hot sellafite (sellafield metas 0..5)

    static {
        for (int i = 0; i < BlockSellafield.LEVELS; i++) {
            int level = i;
            SELLAFIELD[i] = block("sellafield_" + i, p -> new BlockSellafield(p, level),
                    p -> p.strength(5.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().randomTicks(), HbmTab.BLOCKS);
        }
    }

    // ---------------------------------------------------------------- remaining cave spikes (EnumStalagmiteType ICE.. GLYPHID3)

    static {
        spike("ice", () -> new ItemStack(ModItems.powder_ice.get()));
        spike("snow", () -> new ItemStack(Items.SNOWBALL));
        spike("glyphid1", () -> ItemStack.EMPTY);
        spike("glyphid2", () -> ItemStack.EMPTY);
        spike("glyphid3", () -> ItemStack.EMPTY);
    }

    // ---------------------------------------------------------------- dark crystals (no tab)

    public static final RegistryObject<CrystalVirus> crystal_virus = block("crystal_virus", CrystalVirus::new,
            p -> p.strength(15.0F, Float.POSITIVE_INFINITY).requiresCorrectToolForDrops().randomTicks(), null); // 1.7: default stone sound
    public static final RegistryObject<CrystalPulsar> crystal_pulsar = block("crystal_pulsar", CrystalPulsar::new,
            p -> p.strength(15.0F, Float.POSITIVE_INFINITY).requiresCorrectToolForDrops(), null); // 1.7: default stone sound

    // ---------------------------------------------------------------- material / storage blocks

    public static final RegistryObject<BlockBeaconable> block_tcalloy = block("block_tcalloy", BlockBeaconable::new,
            p -> p.strength(5.0F, 70.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), HbmTab.BLOCKS);
    public static final RegistryObject<BlockBeaconable> block_cdalloy = block("block_cdalloy", BlockBeaconable::new,
            p -> p.strength(5.0F, 70.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), HbmTab.BLOCKS);
    public static final RegistryObject<BlockBeaconable> block_australium = block("block_australium", BlockBeaconable::new,
            p -> p.strength(5.0F, 10.0F).requiresCorrectToolForDrops(), HbmTab.BLOCKS); // 1.7: no setStepSound -> default stone

    static {
        for (String v : new String[]{"coal", "lignite", "petroleum"}) {
            // 1.7 BlockCoke: flammability 5 / spread 10 — no cross-loader flammability seam exists yet (see report)
            COKE_BLOCKS.put(v, block("block_coke_" + v, BlockBeaconable::new,
                    p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), HbmTab.BLOCKS));
        }
    }

    public static final RegistryObject<BlockLithium> block_lithium = block("block_lithium", BlockLithium::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), HbmTab.BLOCKS);

    public static final RegistryObject<BlockNuclearWaste> block_waste = waste("block_waste");
    public static final RegistryObject<BlockNuclearWaste> block_waste_painted = waste("block_waste_painted");
    public static final RegistryObject<BlockNuclearWaste> block_waste_vitrified = waste("block_waste_vitrified");

    public static final RegistryObject<BlockHazardFalling> block_yellowcake = block("block_yellowcake", BlockHazardFalling::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.SAND), HbmTab.BLOCKS);
    public static final RegistryObject<BlockHazardFalling> block_fallout = block("block_fallout", BlockHazardFalling::new,
            p -> p.strength(0.2F).sound(SoundType.GRAVEL), HbmTab.BLOCKS);

    // ---------------------------------------------------------------- vents (machineTab, Material.iron)

    public static final RegistryObject<BlockVent> vent_chlorine = vent("vent_chlorine", BlockVent.Kind.CHLORINE);
    public static final RegistryObject<BlockVent> vent_cloud = vent("vent_cloud", BlockVent.Kind.CLOUD);
    public static final RegistryObject<BlockVent> vent_pink_cloud = vent("vent_pink_cloud", BlockVent.Kind.PINK_CLOUD);
    public static final RegistryObject<BlockChlorineSeal> vent_chlorine_seal = block("vent_chlorine_seal", BlockChlorineSeal::new,
            p -> p.strength(5.0F, 10.0F).requiresCorrectToolForDrops(), HbmTab.MACHINES); // 1.7: default stone sound

    // ---------------------------------------------------------------- lookups

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
    public static Iterable<RegistryObject<BlockSellafield>> sellafieldLevels() { return java.util.Arrays.asList(SELLAFIELD); }

    // ---------------------------------------------------------------- helpers

    private static <B extends Block> RegistryObject<B> block(String id, Function<BlockBehaviour.Properties, ? extends B> factory,
                                                             UnaryOperator<BlockBehaviour.Properties> props, HbmTab tab) {
        RegistryObject<B> block = BLOCKS.registerBlock(id, factory, props);
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(id, block, Compat::blockDescription);
        BLOCK_ITEMS.put(id, item);
        if (tab != null) ModTabs.addTo(tab, item);
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
                    p -> p.strength(0.5F, 2.0F).sound(SoundType.STONE).noOcclusion().noCollision(), HbmTab.BLOCKS));
        }
    }

    private static RegistryObject<BlockNuclearWaste> waste(String id) {
        return block(id, BlockNuclearWaste::new,
                p -> p.strength(5.0F, 10.0F).requiresCorrectToolForDrops().randomTicks(), HbmTab.BLOCKS); // 1.7: default stone sound
    }

    private static RegistryObject<BlockVent> vent(String id, BlockVent.Kind kind) {
        return block(id, p -> new BlockVent(p, kind),
                p -> p.strength(5.0F, 10.0F).requiresCorrectToolForDrops(), HbmTab.MACHINES); // 1.7: default stone sound
    }

    public static void init() {}
}
