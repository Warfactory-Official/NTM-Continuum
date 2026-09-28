package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockBallsSpawner;
import com.hbm.blocks.generic.BlockForgottenBrick;
import com.hbm.blocks.generic.BlockForgottenLock;
import com.hbm.blocks.generic.BlockGlyph;
import com.hbm.blocks.generic.BlockJungleOre;
import com.hbm.blocks.generic.BlockKeyhole;
import com.hbm.blocks.generic.BlockLanternBehemoth;
import com.hbm.blocks.generic.BlockLoot;
import com.hbm.blocks.generic.BlockPedestal;
import com.hbm.blocks.generic.BlockRebar;
import com.hbm.blocks.generic.BlockRedBrickKeyhole;
import com.hbm.blocks.generic.BlockSkeletonHolder;
import com.hbm.blocks.generic.DungeonSpawner;
import com.hbm.blocks.generic.FragileBrick;
import com.hbm.blocks.generic.LogicBlock;
import com.hbm.blocks.generic.TrappedBrick;
import com.hbm.blocks.machine.RailBooster;
import com.hbm.blocks.machine.RailGeneric;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.block.ItemGlyphBlock;
import com.hbm.items.block.ItemTrapBlock;
import com.hbm.items.block.RailBlockItem;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The dungeon and structure blocks, the cart rails and rebar of 1.7 {@code ModBlocks}: the forgotten dungeon's
 * bricks and lock, the jungle temple's enargite bricks (brittle, magmatic, radioactive, arcane, the fifteen
 * trapped bricks, the sixteen glyph bricks, the Mechanist's Circle), the Red Room keyholes, the pedestal, the
 * skeleton holder, the loot pile, the old lantern, the dungeon spawner, the two dungeon action blocks, the four
 * vanilla-style cart rails and rebar.
 *
 * <p>Metadata families are flattened the port's way ({@code <base>_<variant>}): {@code brick_forgotten_*},
 * {@code brick_forgotten_lock_*}, {@code brick_jungle_trap_<trap>} and {@code brick_jungle_glyph_<n>}. Hardness and
 * resistance are 1.7's numbers as written; 1.7's {@code Material.rock}/{@code iron} blocks need a pickaxe.
 * Blocks 1.7 kept out of every creative tab stay out of them here.
 */
public final class ModDungeonBlocks {

    private ModDungeonBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** Every id registered here, in order, for the declarations and tests. */
    public static final List<String> IDS = new ArrayList<>();

    private static <B extends Block> RegistryObject<B> block(String id, Function<BlockBehaviour.Properties, B> factory,
                                                             UnaryOperator<BlockBehaviour.Properties> props, HbmTab tab) {
        RegistryObject<B> block = BLOCKS.registerBlock(id, factory, props);
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(id, block, Compat::blockDescription);
        if (tab != null) ModTabs.addTo(tab, item);
        IDS.add(id);
        return block;
    }

    private static UnaryOperator<BlockBehaviour.Properties> rock(float hardness, float resistance) {
        return p -> p.strength(hardness, resistance).sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    /** 1.7 {@code setBlockUnbreakable().setResistance(r)}. */
    private static UnaryOperator<BlockBehaviour.Properties> unbreakable(float resistance) {
        return p -> p.strength(-1.0F, resistance).sound(SoundType.STONE).noLootTable();
    }

    // ---- forgotten dungeon (BlockForgottenBrick / BlockForgottenLock, unbreakable, 666 666) ----
    public static final RegistryObject<BlockForgottenBrick> brick_forgotten = forgotten("brick_forgotten", BlockForgottenBrick.Variant.DEFAULT);
    public static final RegistryObject<BlockForgottenBrick> brick_forgotten_bw = forgotten("brick_forgotten_bw", BlockForgottenBrick.Variant.BW);
    public static final RegistryObject<BlockForgottenBrick> brick_forgotten_nullstone = forgotten("brick_forgotten_nullstone", BlockForgottenBrick.Variant.NULLSTONE);
    public static final RegistryObject<BlockForgottenBrick> brick_forgotten_hole = forgotten("brick_forgotten_hole", BlockForgottenBrick.Variant.HOLE);
    public static final RegistryObject<BlockForgottenBrick> brick_forgotten_hole_empty = forgotten("brick_forgotten_hole_empty", BlockForgottenBrick.Variant.HOLE_EMPTY);
    public static final RegistryObject<BlockForgottenBrick> brick_forgotten_nullroom_wood = forgotten("brick_forgotten_nullroom_wood", BlockForgottenBrick.Variant.NULLROOM_WOOD);
    public static final RegistryObject<BlockForgottenBrick> brick_forgotten_nullroom_stone = forgotten("brick_forgotten_nullroom_stone", BlockForgottenBrick.Variant.NULLROOM_STONE);
    public static final RegistryObject<BlockForgottenLock> brick_forgotten_lock = block("brick_forgotten_lock", BlockForgottenLock::new, unbreakable(666_666F), null);
    public static final RegistryObject<BlockForgottenLock> brick_forgotten_lock_bw = block("brick_forgotten_lock_bw", BlockForgottenLock::new, unbreakable(666_666F), null);
    public static final RegistryObject<BlockForgottenLock> brick_forgotten_lock_nullstone = block("brick_forgotten_lock_nullstone", BlockForgottenLock::new, unbreakable(666_666F), null);

    private static RegistryObject<BlockForgottenBrick> forgotten(String id, BlockForgottenBrick.Variant variant) {
        return block(id, p -> new BlockForgottenBrick(p, variant), unbreakable(666_666F), null);
    }

    // ---- jungle temple (Material.rock, 15 / 360, block tab) ----
    public static final RegistryObject<FragileBrick> brick_jungle_fragile = block("brick_jungle_fragile", FragileBrick::new, rock(15.0F, 360.0F), HbmTab.BLOCKS);
    public static final RegistryObject<Block> brick_jungle_lava = block("brick_jungle_lava", Block::new,
            p -> rock(15.0F, 360.0F).apply(p).lightLevel(s -> 5), HbmTab.BLOCKS);
    public static final RegistryObject<BlockJungleOre> brick_jungle_ooze = block("brick_jungle_ooze",
            p -> new BlockJungleOre(p, BlockJungleOre.Kind.OOZE), p -> rock(15.0F, 360.0F).apply(p).lightLevel(s -> 5), HbmTab.BLOCKS);
    public static final RegistryObject<BlockJungleOre> brick_jungle_mystic = block("brick_jungle_mystic",
            p -> new BlockJungleOre(p, BlockJungleOre.Kind.MYSTIC), p -> rock(15.0F, 360.0F).apply(p).lightLevel(s -> 5), HbmTab.BLOCKS);

    public static final List<RegistryObject<? extends TrappedBrick>> TRAPS = new ArrayList<>();
    public static final List<RegistryObject<BlockGlyph>> GLYPHS = new ArrayList<>();

    static {
        for (TrappedBrick.Trap trap : TrappedBrick.Trap.values()) {
            RegistryObject<? extends TrappedBrick> block = BLOCKS.registerBlock(trap.id(),
                    p -> trap.type == TrappedBrick.TrapType.DETECTOR ? new TrappedBrick.Detector(p, trap) : new TrappedBrick(p, trap),
                    rock(15.0F, 360.0F));
            RegistryObject<ItemTrapBlock> item = ITEMS.registerItem(trap.id(), p -> new ItemTrapBlock(block.get(), p), Compat::blockDescription);
            ModTabs.addTo(HbmTab.BLOCKS, item);
            TRAPS.add(block);
            IDS.add(trap.id());
        }
        for (int i = 0; i < 16; i++) {
            final int glyph = i;
            String id = "brick_jungle_glyph_" + i;
            RegistryObject<BlockGlyph> block = BLOCKS.registerBlock(id, p -> new BlockGlyph(p, glyph), rock(15.0F, 360.0F));
            RegistryObject<ItemGlyphBlock> item = ITEMS.registerItem(id, p -> new ItemGlyphBlock(block.get(), p, glyph), Compat::blockDescription);
            ModTabs.addTo(HbmTab.BLOCKS, item);
            GLYPHS.add(block);
            IDS.add(id);
        }
    }

    public static final RegistryObject<BlockBallsSpawner> brick_jungle_circle = block("brick_jungle_circle", BlockBallsSpawner::new, rock(15.0F, 360.0F), HbmTab.BLOCKS);

    // ---- hidden structure blocks (no creative tab) ----
    /** 1.7 {@code BlockKeyhole extends BlockStone}, which set no hardness of its own. */
    public static final RegistryObject<BlockKeyhole> stone_keyhole = block("stone_keyhole", BlockKeyhole::new,
            p -> p.strength(0.0F, 0.0F).sound(SoundType.STONE), null);
    public static final RegistryObject<BlockRedBrickKeyhole> stone_keyhole_meta = block("stone_keyhole_meta", BlockRedBrickKeyhole::new,
            p -> p.strength(0.0F, 10_000.0F).sound(SoundType.STONE), null);
    public static final RegistryObject<BlockPedestal> pedestal = block("pedestal", BlockPedestal::new,
            p -> rock(2.0F, 10.0F).apply(p).noOcclusion(), null);
    public static final RegistryObject<BlockSkeletonHolder> skeleton_holder = block("skeleton_holder", BlockSkeletonHolder::new,
            p -> rock(2.0F, 10.0F).apply(p).noOcclusion(), null);
    public static final RegistryObject<BlockLoot> deco_loot = block("deco_loot", BlockLoot::new,
            p -> p.strength(0.0F, 0.0F).sound(SoundType.METAL).noOcclusion(), null);
    public static final RegistryObject<BlockLanternBehemoth> lantern_behemoth = block("lantern_behemoth", BlockLanternBehemoth::new,
            p -> p.strength(3.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()
                    .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false), null);
    public static final RegistryObject<DungeonSpawner> dungeon_spawner = block("dungeon_spawner", DungeonSpawner::new, unbreakable(500_000.0F), null);
    public static final RegistryObject<LogicBlock> logic_block = block("logic_block", p -> new LogicBlock(p, false),
            p -> p.strength(0.0F, 0.0F).sound(SoundType.STONE).noOcclusion(), null);
    public static final RegistryObject<LogicBlock> logic_block_invis = block("logic_block_invis", p -> new LogicBlock(p, true),
            p -> p.strength(0.0F, 0.0F).sound(SoundType.STONE).noOcclusion(), null);

    // ---- rails (1.7 transport tab; the port has no vanilla-tab hook, so the machine tab like the large rails) ----
    public static final RegistryObject<RailGeneric> rail_wood = rail("rail_wood", p -> new RailGeneric(p).setMaxSpeed(0.2F));
    public static final RegistryObject<RailGeneric> rail_narrow = rail("rail_narrow", RailGeneric::new);
    public static final RegistryObject<RailGeneric> rail_highspeed = rail("rail_highspeed", p -> new RailGeneric(p).setMaxSpeed(1.0F).setFlexible(false));
    public static final RegistryObject<RailBooster> rail_booster = rail("rail_booster", RailBooster::new);

    private static <R extends RailGeneric> RegistryObject<R> rail(String id, Function<BlockBehaviour.Properties, R> factory) {
        RegistryObject<R> block = BLOCKS.registerBlock(id, factory, p -> p.strength(5.0F, 10.0F).noCollision());
        RegistryObject<RailBlockItem> item = ITEMS.registerItem(id, p -> new RailBlockItem(block.get(), p), Compat::blockDescription);
        ModTabs.addTo(HbmTab.MACHINES, item);
        IDS.add(id);
        return block;
    }

    // ---- rebar (Material.iron, 15 / 20, block tab) ----
    public static final RegistryObject<BlockRebar> rebar = block("rebar", BlockRebar::new,
            p -> p.strength(15.0F, 20.0F).sound(SoundType.STONE).noOcclusion().requiresCorrectToolForDrops()
                    .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false), HbmTab.BLOCKS);

    public static void init() {}
}
