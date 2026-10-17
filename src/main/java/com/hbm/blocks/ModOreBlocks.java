package com.hbm.blocks;

import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
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
import java.util.Set;
import java.util.function.UnaryOperator;
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
 * The ore blocks that {@code gen_wave1.py} cannot produce: the four rich {@link BlockCluster}s, the
 * depth-rock family ({@link BlockDepth}/{@link BlockDepthOre}), the six flattened
 * {@link BlockResourceStone} variants, the two burning nether blocks, the four cave spikes, and the two
 * drifting gas pockets.
 *
 * <p>These are hand-registered because each carries behaviour (custom drop maths, unbreakability,
 * drift/ignition, anchoring) that a generated plain {@code Block} cannot express. Every block here
 * computes its drops in Java (see {@link OreDrops}) and therefore declares {@code noLootTable()}:
 * <b>do not add these ids to the generator's loot-table list</b>. The generator still owns their
 * models, blockstates, lang and mineable-tag entries.
 *
 * <p>The bedrock-ore block also lives here: its block entity and extraction contract are feature-local.
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

    /** Ids that get no block item: a floating gas cell is worldgen-only and drops nothing upstream. */
    private static final Set<String> NO_ITEM = Set.of("gas_flammable", "gas_explosive");

    // Clusters (hardness 5, blast 35)

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

    // Depth rock + depth ores

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

    /** 1.7 {@code BlockDepthOre}: {@code 2 + rand(3)} cinnabar, fortune-multiplied (applies to every depth ore). */
    public static final RegistryObject<BlockDepthOre> ore_depth_cinnabar =
            depthOre("ore_depth_cinnabar", 2, 3, () -> ModItems.cinnabar.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);
    /** 1.7: {@code 2 + rand(2)} zirconium nuggets. */
    public static final RegistryObject<BlockDepthOre> ore_depth_zirconium =
            depthOre("ore_depth_zirconium", 2, 2, () -> ModItems.nugget_zirconium.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);
    /** 1.7 {@code BlockDepthOre.getItemDropped}: one {@code powder_borax} (1.12.2 dropped the block itself). */
    public static final RegistryObject<BlockDepthOre> ore_depth_borax =
            depthOre("ore_depth_borax", 1, 0, () -> ModItems.powder_borax.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);
    /** 1.7: one gem, fortune-multiplied, then clamped to 2. */
    public static final RegistryObject<BlockDepthOre> ore_alexandrite =
            reg("ore_alexandrite", p -> new BlockDepthOre(p, 1, 0, 2, true, () -> new ItemStack(ModItems.gem_alexandrite.get())),
                    ModOreBlocks::depthProps)
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);

    /** Depth-rock clusters: one crystal, fortune-multiplied like every 1.7 depth ore (surface clusters ignore fortune). */
    public static final RegistryObject<BlockDepthOre> cluster_depth_iron =
            depthOre("cluster_depth_iron", 1, 0, () -> ModItems.crystal_iron.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockDepthOre> cluster_depth_titanium =
            depthOre("cluster_depth_titanium", 1, 0, () -> ModItems.crystal_titanium.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockDepthOre> cluster_depth_tungsten =
            depthOre("cluster_depth_tungsten", 1, 0, () -> ModItems.crystal_tungsten.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);

    /** 1.7: {@code 2 + rand(2)} neodymium fragments. */
    public static final RegistryObject<BlockDepthOre> ore_depth_nether_neodymium =
            depthOre("ore_depth_nether_neodymium", 2, 2, () -> ModItems.fragment_neodymium.get())
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);
    /**
     * Not in 1.7; a 1.12.2 addition kept as it was: exactly one nitan mix, fortune ignored.
     */
    public static final RegistryObject<BlockDepthOre> ore_depth_nether_nitan =
            reg("ore_depth_nether_nitan", p -> new BlockDepthOre(p, 1, 0, Integer.MAX_VALUE, false,
                    () -> new ItemStack(ModItems.powder_nitan_mix.get())), ModOreBlocks::depthProps)
            .transform(Presets.cubeAll().viaItemModel())
            .tag(BlockTag.PICKAXE);

    // Resource stone (flattened ×6)

    public static final RegistryObject<BlockResourceStone> stone_resource_sulfur = resourceStone("sulfur");
    public static final RegistryObject<BlockResourceStone> stone_resource_asbestos = resourceStone("asbestos");
    public static final RegistryObject<BlockResourceStone> stone_resource_hematite = resourceStone("hematite");
    public static final RegistryObject<BlockResourceStone> stone_resource_malachite = resourceStone("malachite");
    public static final RegistryObject<BlockResourceStone> stone_resource_limestone = resourceStone("limestone");
    public static final RegistryObject<BlockResourceStone> stone_resource_bauxite = resourceStone("bauxite");

    // Burning nether blocks

    /** Light 10/15, hardness 0.4. An outgas block upstream, so mining it in the nether leaves CO. */
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

    // Cave spikes

    public static final RegistryObject<BlockStalagmite> stalagmite_sulfur =
            spike("stalagmite_sulfur", Direction.DOWN, () -> ModItems.sulfur.get())
            .model(d -> d.models(Models.renderType("minecraft:cutout", Models.cross("hbm:blocks/stalagmite.sulfur"))).states(Variants.simple()))
            .transform(Presets.sprite("hbm:blocks/stalagmite.sulfur")).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockStalagmite> stalagmite_asbestos =
            spike("stalagmite_asbestos", Direction.DOWN, () -> ModItems.powder_asbestos.get())
            .model(d -> d.models(Models.renderType("minecraft:cutout", Models.cross("hbm:blocks/stalagmite.asbestos"))).states(Variants.simple()))
            .transform(Presets.sprite("hbm:blocks/stalagmite.asbestos")).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockStalagmite> stalactite_sulfur =
            spike("stalactite_sulfur", Direction.UP, () -> ModItems.sulfur.get())
            .model(d -> d.models(Models.renderType("minecraft:cutout", Models.cross("hbm:blocks/stalactite.sulfur"))).states(Variants.simple()))
            .transform(Presets.sprite("hbm:blocks/stalactite.sulfur")).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockStalagmite> stalactite_asbestos =
            spike("stalactite_asbestos", Direction.UP, () -> ModItems.powder_asbestos.get())
            .model(d -> d.models(Models.renderType("minecraft:cutout", Models.cross("hbm:blocks/stalactite.asbestos"))).states(Variants.simple()))
            .transform(Presets.sprite("hbm:blocks/stalactite.asbestos")).tag(BlockTag.PICKAXE);

    // Gas pockets

    public static final RegistryObject<BlockGasFlammable> gas_flammable =
            reg("gas_flammable", BlockGasFlammable::new, ModOreBlocks::gasProps)
            .transform(Presets.rendered(Variants.simple()));
    public static final RegistryObject<BlockGasExplosive> gas_explosive =
            reg("gas_explosive", BlockGasExplosive::new, ModOreBlocks::gasProps)
            .transform(Presets.rendered(Variants.simple()));

    // Helpers

    private static <B extends Block> Reg.Handle<B> reg(String id,
                                                           Function<BlockBehaviour.Properties, ? extends B> factory,
                                                           UnaryOperator<BlockBehaviour.Properties> props) {
        Reg.Handle<B> block = REG.block(id, factory, props);
        ALL.put(id, block);
        return block;
    }

    private static Reg.Handle<BlockCluster> cluster(String id, Supplier<Item> drop) {
        return reg(id, p -> new BlockCluster(p, () -> new ItemStack(drop.get())),
                p -> p.strength(5.0F, 35.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noLootTable());
    }

    /** 1.7 {@code BlockDepth} has no drop override: broken by a depth-rock tool (or blown up) it drops itself. */
    private static Reg.Handle<BlockDepth> depth(String id) {
        return reg(id, BlockDepth::new, p -> p.strength(-1.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops())
                .loot(Loot.self());
    }

    private static Reg.Handle<BlockDepthOre> depthOre(String id, int base, int spread, Supplier<Item> drop) {
        return reg(id, p -> new BlockDepthOre(p, base, spread, Integer.MAX_VALUE, true, () -> new ItemStack(drop.get())),
                ModOreBlocks::depthProps);
    }

    /**
     * 1.7's {@code setBlockUnbreakable()} + {@code setResistance(10)}: a destroy time of -1 means no
     * tool can break it, while the low blast resistance lets explosives.
     */
    private static BlockBehaviour.Properties depthProps(BlockBehaviour.Properties p) {
        return p.strength(-1.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noLootTable();
    }

    private static Reg.Handle<BlockResourceStone> resourceStone(String variant) {
        return reg("stone_resource_" + variant, p -> new BlockResourceStone(p, variant),
                        p -> p.strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noLootTable())
                .transform(Presets.cubeAll("hbm:blocks/stone_resource." + variant).viaItemModel())
                .tag(BlockTag.PICKAXE);
    }

    private static Reg.Handle<BlockStalagmite> spike(String id, Direction anchor, Supplier<Item> drop) {
        return reg(id, p -> new BlockStalagmite(p, anchor, () -> new ItemStack(drop.get())),
                p -> p.requiresCorrectToolForDrops().strength(0.5F, 2.0F).sound(SoundType.STONE).noOcclusion().noCollision().noLootTable());
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
