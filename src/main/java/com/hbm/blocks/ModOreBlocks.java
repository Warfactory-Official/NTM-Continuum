package com.hbm.blocks;

import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
import com.hbm.registration.BlockTag;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.world.Compat;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockCluster;
import com.hbm.blocks.generic.BlockBedrockOre;
import com.hbm.blocks.generic.BlockDepth;
import com.hbm.blocks.generic.BlockDepthOre;
import com.hbm.blocks.generic.BlockGasExplosive;
import com.hbm.blocks.generic.BlockGasFlammable;
import com.hbm.blocks.generic.BlockNetherCoal;
import com.hbm.blocks.generic.BlockResourceStone;
import com.hbm.blocks.generic.BlockSmolder;
import com.hbm.blocks.generic.BlockStalagmite;
import com.hbm.blocks.generic.OreDrops;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.tileentity.BedrockOreBlockEntity;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The ore blocks the ore-worldgen wave needed and {@code gen_wave1.py} could not produce: the four
 * rich {@link BlockCluster}s, the depth-rock family ({@link BlockDepth}/{@link BlockDepthOre}), the six
 * flattened {@link BlockResourceStone} variants, the two burning nether blocks, the four cave spikes,
 * and the two drifting gas pockets.
 *
 * <p>These are hand-registered because each carries real behaviour (custom drop maths, unbreakability,
 * drift/ignition, anchoring) that a generated plain {@code Block} cannot express. Every block here
 * computes its drops in Java (see {@link OreDrops} for why) and therefore declares
 * {@code noLootTable()} — <b>do not add these ids to the generator's loot-table list</b>. The generator
 * still owns their models, blockstates, lang and mineable-tag entries.
 *
 * <p>The bedrock-ore block also lives here: its block entity and extraction contract are feature-local,
 * while the full excavator machine remains a separate integration.
 */
public final class ModOreBlocks {

    private ModOreBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.createBlockEntityTypes(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS, BLOCK_ENTITIES);

    /** Every block registered here, in registration order. Block items are created from this. */
    private static final Map<String, RegistryObject<? extends Block>> ALL = new LinkedHashMap<>();

    /** Ids that get no block item — a floating gas cell is worldgen-only and drops nothing upstream. */
    private static final java.util.Set<String> NO_ITEM = java.util.Set.of("gas_flammable", "gas_explosive");

    // ---------------------------------------------------------------- clusters (hardness 5, blast 35)

    public static final RegistryObject<BlockCluster> cluster_iron =
            cluster("cluster_iron", () -> ModItems.crystal_iron.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockCluster> cluster_titanium =
            cluster("cluster_titanium", () -> ModItems.crystal_titanium.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockCluster> cluster_aluminium =
            cluster("cluster_aluminium", () -> ModItems.crystal_aluminium.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockCluster> cluster_copper =
            cluster("cluster_copper", () -> ModItems.crystal_copper.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);

    // ---------------------------------------------------------------- depth rock + depth ores

    /** Overworld depth filler laid down around a depth deposit. */
    public static final RegistryObject<BlockDepth> stone_depth = depth("stone_depth")
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);
    /** Nether depth filler. */
    public static final RegistryObject<BlockDepth> stone_depth_nether = depth("stone_depth_nether")
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);

    /** Infinite floor deposit carrying resource, acid, tier, colour and shape state. */
    public static final RegistryObject<BlockBedrockOre> ore_bedrock_block = reg("ore_bedrock_block",
            BlockBedrockOre::new, p -> p.strength(-1.0F, 3_600_000.0F).sound(SoundType.STONE)
                    .noLootTable().isValidSpawn((state, level, pos, type) -> false))
            .transform(Presets.cubeAll())
            .tag(BlockTag.PICKAXE);

    /** Raw coordinate-density stack emitted by overworld deposits. */
    public static final RegistryObject<ItemBedrockOreBase> bedrock_ore_base = REG.item(
            "bedrock_ore_base", ItemBedrockOreBase::new, p -> p.stacksTo(64))
            .transform(Presets.sprite("hbm:items/bedrock_ore_new"));

    public static final RegistryObject<BlockEntityType<BedrockOreBlockEntity>> BEDROCK_ORE_BLOCK_ENTITY =
            REG.blockEntity("bedrock_ore", () -> BlockEntities.type(
                    BedrockOreBlockEntity::new, ore_bedrock_block.get()));

    /** {@code OreEnum.CINNABAR}: {@code 1 + rand(2) + fortune} cinnabar. */
    public static final RegistryObject<BlockDepthOre> ore_depth_cinnabar =
            depthOre("ore_depth_cinnabar", OreDrops::base1Rand2Fortune, () -> ModItems.cinnabar.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);
    /** {@code OreEnum.ZIRCON}: {@code 2 + rand(2) + fortune} zirconium nuggets. */
    public static final RegistryObject<BlockDepthOre> ore_depth_zirconium =
            depthOre("ore_depth_zirconium", OreDrops::base2Rand2Fortune, () -> ModItems.nugget_zirconium.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);
    /** Upstream passes a {@code null} OreEnum — falls through to dropping the block itself. */
    public static final RegistryObject<BlockDepthOre> ore_depth_borax =
            reg("ore_depth_borax", BlockDepthOre::new, ModOreBlocks::depthProps)
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);
    /** {@code OreEnum.ALEXANDRITE}: {@code min(1 + rand(2) + fortune, 2)} gems — hard-capped at 2. */
    public static final RegistryObject<BlockDepthOre> ore_alexandrite =
            depthOre("ore_alexandrite", OreDrops::alexandriteAmount, () -> ModItems.gem_alexandrite.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);

    /** Depth-rock clusters use {@code vanillaFortune} — unlike the surface {@link BlockCluster}s. */
    public static final RegistryObject<BlockDepthOre> cluster_depth_iron =
            depthOre("cluster_depth_iron", OreDrops::vanillaFortune, () -> ModItems.crystal_iron.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockDepthOre> cluster_depth_titanium =
            depthOre("cluster_depth_titanium", OreDrops::vanillaFortune, () -> ModItems.crystal_titanium.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockDepthOre> cluster_depth_tungsten =
            depthOre("cluster_depth_tungsten", OreDrops::vanillaFortune, () -> ModItems.crystal_tungsten.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);

    /** {@code OreEnum.NEODYMIUM}: {@code 2 + rand(2) + fortune} neodymium fragments. */
    public static final RegistryObject<BlockDepthOre> ore_depth_nether_neodymium =
            depthOre("ore_depth_nether_neodymium", OreDrops::base2Rand2Fortune, () -> ModItems.fragment_neodymium.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);
    /** {@code OreEnum.NITAN}: exactly one nitan mix, fortune ignored. */
    public static final RegistryObject<BlockDepthOre> ore_depth_nether_nitan =
            depthOre("ore_depth_nether_nitan", OreDrops::const1, () -> ModItems.powder_nitan_mix.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);

    // ---------------------------------------------------------------- resource stone (flattened ×6)

    public static final RegistryObject<BlockResourceStone> stone_resource_sulfur = resourceStone("sulfur");
    public static final RegistryObject<BlockResourceStone> stone_resource_asbestos = resourceStone("asbestos");
    public static final RegistryObject<BlockResourceStone> stone_resource_hematite = resourceStone("hematite");
    public static final RegistryObject<BlockResourceStone> stone_resource_malachite = resourceStone("malachite");
    public static final RegistryObject<BlockResourceStone> stone_resource_limestone = resourceStone("limestone");
    public static final RegistryObject<BlockResourceStone> stone_resource_bauxite = resourceStone("bauxite");

    // ---------------------------------------------------------------- burning nether blocks

    /** Light 10/15, hardness 0.4 — an outgas block upstream, so mining it in the nether leaves CO. */
    public static final RegistryObject<BlockNetherCoal> ore_nether_coal = reg("ore_nether_coal",
            BlockNetherCoal::new, p -> p.strength(0.4F, 10.0F).sound(SoundType.STONE).lightLevel(s -> 10)
                    .requiresCorrectToolForDrops().randomTicks().noLootTable())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);

    /** Light 15, hardness 0.4. */
    public static final RegistryObject<BlockSmolder> ore_nether_smoldering = reg("ore_nether_smoldering",
            BlockSmolder::new, p -> p.strength(0.4F, 10.0F).sound(SoundType.STONE).lightLevel(s -> 15)
                    .requiresCorrectToolForDrops().noLootTable())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);

    // ---------------------------------------------------------------- cave spikes

    public static final RegistryObject<BlockStalagmite> stalagmite_sulfur =
            spike("stalagmite_sulfur", Direction.DOWN, () -> ModItems.sulfur.get())
            .model(d -> d.models(Models.renderType("minecraft:cutout", Models.cross("hbm:blocks/stalagmite.sulfur"))).states(Variants.simple()))
            .transform(Presets.sprite("hbm:blocks/stalagmite.sulfur"));
    public static final RegistryObject<BlockStalagmite> stalagmite_asbestos =
            spike("stalagmite_asbestos", Direction.DOWN, () -> ModItems.powder_asbestos.get())
            .model(d -> d.models(Models.renderType("minecraft:cutout", Models.cross("hbm:blocks/stalagmite.asbestos"))).states(Variants.simple()))
            .transform(Presets.sprite("hbm:blocks/stalagmite.asbestos"));
    public static final RegistryObject<BlockStalagmite> stalactite_sulfur =
            spike("stalactite_sulfur", Direction.UP, () -> ModItems.sulfur.get())
            .model(d -> d.models(Models.renderType("minecraft:cutout", Models.cross("hbm:blocks/stalactite.sulfur"))).states(Variants.simple()))
            .transform(Presets.sprite("hbm:blocks/stalactite.sulfur"));
    public static final RegistryObject<BlockStalagmite> stalactite_asbestos =
            spike("stalactite_asbestos", Direction.UP, () -> ModItems.powder_asbestos.get())
            .model(d -> d.models(Models.renderType("minecraft:cutout", Models.cross("hbm:blocks/stalactite.asbestos"))).states(Variants.simple()))
            .transform(Presets.sprite("hbm:blocks/stalactite.asbestos"));

    // ---------------------------------------------------------------- gas pockets

    public static final RegistryObject<BlockGasFlammable> gas_flammable =
            reg("gas_flammable", BlockGasFlammable::new, ModOreBlocks::gasProps)
            .transform(Presets.rendered(Variants.simple()));
    public static final RegistryObject<BlockGasExplosive> gas_explosive =
            reg("gas_explosive", BlockGasExplosive::new, ModOreBlocks::gasProps)
            .transform(Presets.rendered(Variants.simple()));

    // ---------------------------------------------------------------- helpers

    private static <B extends Block> Reg.Handle<B> reg(String id,
                                                           Function<BlockBehaviour.Properties, ? extends B> factory,
                                                           java.util.function.UnaryOperator<BlockBehaviour.Properties> props) {
        Reg.Handle<B> block = REG.block(id, factory, props);
        ALL.put(id, block);
        return block;
    }

    private static Reg.Handle<BlockCluster> cluster(String id, Supplier<Item> drop) {
        return reg(id, p -> new BlockCluster(p, () -> new ItemStack(drop.get())),
                p -> p.strength(5.0F, 35.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noLootTable());
    }

    private static Reg.Handle<BlockDepth> depth(String id) {
        return reg(id, BlockDepth::new, p -> depthProps(p).noLootTable());
    }

    private static Reg.Handle<BlockDepthOre> depthOre(String id, OreDrops.Quantity quantity, Supplier<Item> drop) {
        return reg(id, p -> new BlockDepthOre(p, quantity, () -> new ItemStack(drop.get())), ModOreBlocks::depthProps);
    }

    /**
     * Old {@code setBlockUnbreakable()} + {@code setResistance(10)} — a destroy time of −1 means no
     * tool can ever break it, while the low blast resistance means explosives still can.
     */
    private static BlockBehaviour.Properties depthProps(BlockBehaviour.Properties p) {
        return p.strength(-1.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noLootTable();
    }

    private static Reg.Handle<BlockResourceStone> resourceStone(String variant) {
        boolean malachite = "malachite".equals(variant);
        return reg("stone_resource_" + variant, p -> new BlockResourceStone(p, malachite),
                        p -> p.strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noLootTable())
                .transform(Presets.cubeAll("hbm:blocks/stone_resource." + variant).viaItemModel())
                .tag(BlockTag.PICKAXE);
    }

    private static Reg.Handle<BlockStalagmite> spike(String id, Direction anchor, Supplier<Item> drop) {
        return reg(id, p -> new BlockStalagmite(p, anchor, () -> new ItemStack(drop.get())),
                p -> p.strength(0.5F, 2.0F).sound(SoundType.STONE).noOcclusion().noCollision().noLootTable());
    }

    /** Old {@code BlockGasBase}: hardness 0, no collision, no occlusion, replaceable, random-ticked. */
    private static BlockBehaviour.Properties gasProps(BlockBehaviour.Properties p) {
        return p.strength(0.0F, 0.0F).noOcclusion().noCollision().replaceable().randomTicks().noLootTable();
    }

    static {
        ALL.forEach((id, block) -> {
            if (NO_ITEM.contains(id)) return;
            Reg.Handle<BlockItem> item = REG.blockItem(block, p -> Compat.blockDescription(p));
            ModTabs.addTo(HbmTab.RESOURCES, item);
        });
        ModTabs.addTo(HbmTab.MATERIALS, bedrock_ore_base);
    }

    public static void init() {
        ItemBedrockOreBase.init();
    }
}
