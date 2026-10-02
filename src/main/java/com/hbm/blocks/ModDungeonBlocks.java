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
import com.hbm.registration.BlockTag;
import com.hbm.registration.ItemTag;
import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Placement;
import dev.golem.core.dejson.Shape;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
 *
 * <p>Each block's models, blockstate, item model, loot and tags are stated on its registration (Reg; emitted by
 * Dejson at build time). The pedestal and rebar models are plain boxes built here; the recipes stay in
 * {@link DungeonDeclarations}.
 */
public final class ModDungeonBlocks {

    private ModDungeonBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    private static final String DUMMY = "hbm:block/machine_dummy_empty";

    /** One texture all round ({@code hbm:blocks/<texture>}), single state. */
    private static Preset cube(String texture) {
        return Presets.cubeAll("hbm:blocks/" + texture);
    }

    /** A pillar that does not turn. */
    private static Preset column(String end, String side) {
        return Presets.column(end, side);
    }

    /** Every id registered here, in order, for the declarations and tests. */
    public static final List<String> IDS = new ArrayList<>();

    private static <B extends Block> Reg.Handle<B> block(String id, Function<BlockBehaviour.Properties, B> factory,
                                                             UnaryOperator<BlockBehaviour.Properties> props, HbmTab tab) {
        Reg.Handle<B> block = REG.block(id, factory, props);
        Reg.Handle<BlockItem> item = REG.blockItem(block, Compat::blockDescription);
        if (tab != null) item.tab(tab);
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

    // ---- forgotten dungeon (BlockForgottenBrick / BlockForgottenLock, unbreakable, 666 666): drops nothing ----
    public static final RegistryObject<BlockForgottenBrick> brick_forgotten = forgotten("brick_forgotten", BlockForgottenBrick.Variant.DEFAULT,
            column("hbm:blocks/brick_forgotten_top", "hbm:blocks/brick_forgotten"));
    public static final RegistryObject<BlockForgottenBrick> brick_forgotten_bw = forgotten("brick_forgotten_bw", BlockForgottenBrick.Variant.BW,
            column("hbm:blocks/brick_forgotten_bw_top", "hbm:blocks/brick_forgotten_bw"));
    public static final RegistryObject<BlockForgottenBrick> brick_forgotten_nullstone = forgotten("brick_forgotten_nullstone", BlockForgottenBrick.Variant.NULLSTONE,
            cube("playground/nullstone_demo_1_wip"));
    public static final RegistryObject<BlockForgottenBrick> brick_forgotten_hole = forgotten("brick_forgotten_hole", BlockForgottenBrick.Variant.HOLE,
            column("hbm:blocks/brick_forgotten_top", "hbm:blocks/brick_forgotten_hole"));
    public static final RegistryObject<BlockForgottenBrick> brick_forgotten_hole_empty = forgotten("brick_forgotten_hole_empty", BlockForgottenBrick.Variant.HOLE_EMPTY,
            column("hbm:blocks/brick_forgotten_top", "hbm:blocks/brick_forgotten_hole_empty"));
    public static final RegistryObject<BlockForgottenBrick> brick_forgotten_nullroom_wood = forgotten("brick_forgotten_nullroom_wood", BlockForgottenBrick.Variant.NULLROOM_WOOD,
            cube("nr_planks"));
    public static final RegistryObject<BlockForgottenBrick> brick_forgotten_nullroom_stone = forgotten("brick_forgotten_nullroom_stone", BlockForgottenBrick.Variant.NULLROOM_STONE,
            cube("nr_stone"));
    public static final RegistryObject<BlockForgottenLock> brick_forgotten_lock = block("brick_forgotten_lock", BlockForgottenLock::new, unbreakable(666_666F), null)
            .transform(column("hbm:blocks/brick_forgotten_top", "hbm:blocks/brick_forgotten_lock")).loot(Loot.nothing());
    public static final RegistryObject<BlockForgottenLock> brick_forgotten_lock_bw = block("brick_forgotten_lock_bw", BlockForgottenLock::new, unbreakable(666_666F), null)
            .transform(column("hbm:blocks/brick_forgotten_bw_top", "hbm:blocks/brick_forgotten_bw_lock")).loot(Loot.nothing());
    public static final RegistryObject<BlockForgottenLock> brick_forgotten_lock_nullstone = block("brick_forgotten_lock_nullstone", BlockForgottenLock::new, unbreakable(666_666F), null)
            .transform(column("hbm:blocks/playground/nullstone_demo_1_wip", "hbm:blocks/playground/nullstone_demo_2_wip")).loot(Loot.nothing());

    private static RegistryObject<BlockForgottenBrick> forgotten(String id, BlockForgottenBrick.Variant variant, Preset model) {
        return block(id, p -> new BlockForgottenBrick(p, variant), unbreakable(666_666F), null)
                .transform(model)
                .loot(Loot.nothing());
    }

    // ---- jungle temple (Material.rock, 15 / 360, block tab) ----
    public static final RegistryObject<FragileBrick> brick_jungle_fragile = block("brick_jungle_fragile", FragileBrick::new, rock(15.0F, 360.0F), HbmTab.BLOCKS)
            .transform(cube("brick_jungle_fragile")).loot(Loot.nothing()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<Block> brick_jungle_lava = block("brick_jungle_lava", Block::new,
            p -> rock(15.0F, 360.0F).apply(p).lightLevel(s -> 5), HbmTab.BLOCKS)
            .transform(cube("brick_jungle_lava")).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockJungleOre> brick_jungle_ooze = block("brick_jungle_ooze",
            p -> new BlockJungleOre(p, BlockJungleOre.Kind.OOZE), p -> rock(15.0F, 360.0F).apply(p).lightLevel(s -> 5), HbmTab.BLOCKS)
            .transform(cube("brick_jungle_ooze")).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockJungleOre> brick_jungle_mystic = block("brick_jungle_mystic",
            p -> new BlockJungleOre(p, BlockJungleOre.Kind.MYSTIC), p -> rock(15.0F, 360.0F).apply(p).lightLevel(s -> 5), HbmTab.BLOCKS)
            .transform(cube("brick_jungle_mystic")).loot(Loot.self()).tag(BlockTag.PICKAXE);

    public static final List<RegistryObject<? extends TrappedBrick>> TRAPS = new ArrayList<>();
    public static final List<RegistryObject<BlockGlyph>> GLYPHS = new ArrayList<>();

    static {
        for (TrappedBrick.Trap trap : TrappedBrick.Trap.values()) {
            // 1.7 TrappedBrick had no damageDropped: every trap drops metadata 0, the falling-rocks trap
            Reg.Handle<TrappedBrick> block = REG.<TrappedBrick>block(trap.id(),
                            p -> trap.type == TrappedBrick.TrapType.DETECTOR ? new TrappedBrick.Detector(p, trap) : new TrappedBrick(p, trap),
                            rock(15.0F, 360.0F))
                    .transform(cube("brick_jungle_trap"))
                    .loot(Loot.only("hbm:brick_jungle_trap_falling_rocks"))
                    .tag(BlockTag.PICKAXE);
            REG.item(trap.id(), p -> new ItemTrapBlock(block.get(), p), Compat::blockDescription).tab(HbmTab.BLOCKS);
            TRAPS.add(block);
            IDS.add(trap.id());
        }
        for (int i = 0; i < 16; i++) {
            final int glyph = i;
            String id = "brick_jungle_glyph_" + i;
            RegistryObject<BlockGlyph> block = REG.block(id, p -> new BlockGlyph(p, glyph), rock(15.0F, 360.0F))
                    .transform(column("hbm:blocks/brick_jungle", "hbm:blocks/brick_jungle_glyph_" + i))
                    .loot(Loot.self())
                    .tag(BlockTag.PICKAXE);
            REG.item(id, p -> new ItemGlyphBlock(block.get(), p, glyph), Compat::blockDescription).tab(HbmTab.BLOCKS);
            GLYPHS.add(block);
            IDS.add(id);
        }
    }

    public static final RegistryObject<BlockBallsSpawner> brick_jungle_circle = block("brick_jungle_circle", BlockBallsSpawner::new, rock(15.0F, 360.0F), HbmTab.BLOCKS)
            .transform(cube("brick_jungle_circle")).loot(Loot.self()).tag(BlockTag.PICKAXE);

    // ---- hidden structure blocks (no creative tab) ----
    /** 1.7 {@code BlockKeyhole extends BlockStone}, which set no hardness of its own. */
    public static final RegistryObject<BlockKeyhole> stone_keyhole = block("stone_keyhole", BlockKeyhole::new,
            p -> p.strength(0.0F, 0.0F).sound(SoundType.STONE), null)
            .transform(column("minecraft:block/stone", "hbm:blocks/stone_keyhole"))
            // 1.7 BlockKeyhole extends BlockStone, which drops cobblestone
            .loot(Loot.only("minecraft:cobblestone"));
    public static final RegistryObject<BlockRedBrickKeyhole> stone_keyhole_meta = block("stone_keyhole_meta", BlockRedBrickKeyhole::new,
            p -> p.strength(0.0F, 10_000.0F).sound(SoundType.STONE), null)
            .model(d -> d.models(Models.group()
                            .and("", Models.parented("minecraft:block/cube", Slots.of("particle", "hbm:blocks/brick_base")
                                    .and("down", "hbm:blocks/brick_base").and("up", "hbm:blocks/brick_base")
                                    .and("north", "hbm:blocks/stone_keyhole_meta").and("south", "hbm:blocks/brick_base")
                                    .and("west", "hbm:blocks/brick_base").and("east", "hbm:blocks/brick_base")))
                            .and("_top", Models.cubeBottomTop("hbm:blocks/brick_base", "hbm:blocks/brick_base", "hbm:blocks/brick_red_top")))
                    .states(Variants.of(Set.of("facing"), keyholeStates()))
                    .blockItemDirect())
            .loot(Loot.nothing());
    /** 1.7 RenderPedestal: foot 0..0.25, head 0.75..1, column 0.125..0.875 between. */
    public static final RegistryObject<BlockPedestal> pedestal = block("pedestal", BlockPedestal::new,
            p -> rock(2.0F, 10.0F).apply(p).noOcclusion(), null)
            .transform(Presets.shape(elements("hbm:blocks/pedestal_top", "hbm:blocks/pedestal_top", "hbm:blocks/pedestal_side", false,
                    box(0, 0, 0, 16, 4, 16), box(0, 12, 0, 16, 16, 16), box(2, 4, 2, 14, 12, 14))))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockSkeletonHolder> skeleton_holder = block("skeleton_holder", BlockSkeletonHolder::new,
            p -> rock(2.0F, 10.0F).apply(p).noOcclusion(), null)
            .transform(Presets.rendered(DUMMY, Variants.uniform("facing", "north", "east", "south", "west")))
            .transform(Presets.sprite("minecraft:block/soul_sand"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockLoot> deco_loot = block("deco_loot", BlockLoot::new,
            p -> p.strength(0.0F, 0.0F).sound(SoundType.METAL).noOcclusion(), null)
            .transform(Presets.rendered(DUMMY, Variants.simple()))
            .transform(Presets.sprite("hbm:blocks/block_steel"))
            .loot(Loot.nothing());
    public static final RegistryObject<BlockLanternBehemoth> lantern_behemoth = block("lantern_behemoth", BlockLanternBehemoth::new,
            p -> p.strength(3.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()
                    .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false), null)
            .transform(Presets.rendered(DUMMY, Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                    Variants.uniform("cell", "false", "true"))))
            .transform(Presets.sprite("hbm:blocks/block_rust"))
            .loot(Loot.nothing())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<DungeonSpawner> dungeon_spawner = block("dungeon_spawner", DungeonSpawner::new, unbreakable(500_000.0F), null)
            .transform(cube("dungeon_spawner"))
            .loot(Loot.nothing());
    // both drawn by RenderLogicBlock (the invisible one by nothing); the model is their particle and item
    public static final RegistryObject<LogicBlock> logic_block = block("logic_block", p -> new LogicBlock(p, false),
            p -> p.strength(0.0F, 0.0F).sound(SoundType.STONE).noOcclusion(), null)
            .transform(Presets.rendered(DUMMY, Variants.simple()))
            .icon(d -> d.models(Models.cubeAll("hbm:blocks/logic_block")))
            .loot(Loot.self());
    public static final RegistryObject<LogicBlock> logic_block_invis = block("logic_block_invis", p -> new LogicBlock(p, true),
            p -> p.strength(0.0F, 0.0F).sound(SoundType.STONE).noOcclusion(), null)
            .transform(Presets.rendered(DUMMY, Variants.simple()))
            .transform(Presets.sprite("hbm:blocks/logic_block"))
            .loot(Loot.self());

    // ---- rails (1.7 transport tab; the port has no vanilla-tab hook, so the machine tab like the large rails) ----
    public static final RegistryObject<RailGeneric> rail_wood = rail("rail_wood", p -> new RailGeneric(p).setMaxSpeed(0.2F),
            "rail_wood", "rail_wood_turned").tag(BlockTag.AXE);
    public static final RegistryObject<RailGeneric> rail_narrow = rail("rail_narrow", RailGeneric::new,
            "rail_narrow", "rail_narrow_turned").tag(BlockTag.PICKAXE);
    public static final RegistryObject<RailGeneric> rail_highspeed = rail("rail_highspeed", p -> new RailGeneric(p).setMaxSpeed(1.0F).setFlexible(false),
            "rail_highspeed", "rail_highspeed").tag(BlockTag.PICKAXE);
    public static final RegistryObject<RailBooster> rail_booster = rail("rail_booster", RailBooster::new,
            "rail_booster", "rail_booster").tag(BlockTag.PICKAXE);

    /** The vanilla {@code rail} blockstate and models over this rail's textures, on the cutout layer; a vanilla rail. */
    private static <R extends RailGeneric> Reg.Handle<R> rail(String id, Function<BlockBehaviour.Properties, R> factory,
                                                              String straight, String turned) {
        Reg.Handle<R> block = REG.block(id, factory, p -> p.strength(5.0F, 10.0F).noCollision());
        REG.item(id, p -> new RailBlockItem(block.get(), p), Compat::blockDescription).tab(HbmTab.MACHINES)
                .tag(ItemTag.of("minecraft:rails"));
        IDS.add(id);
        String s = "hbm:blocks/" + straight;
        return block
                .model(d -> d.models(Models.group()
                                .and("", Models.renderType("minecraft:cutout", Models.parented("minecraft:block/rail_flat", Slots.of("rail", s))))
                                .and("_corner", Models.renderType("minecraft:cutout",
                                        Models.parented("minecraft:block/rail_curved", Slots.of("rail", "hbm:blocks/" + turned))))
                                .and("_raised_ne", Models.renderType("minecraft:cutout",
                                        Models.parented("minecraft:block/template_rail_raised_ne", Slots.of("rail", s))))
                                .and("_raised_sw", Models.renderType("minecraft:cutout",
                                        Models.parented("minecraft:block/template_rail_raised_sw", Slots.of("rail", s)))))
                        .states(Variants.of(Set.of("shape", "waterlogged"), railStates()))
                        .item(Models.generated(s)))
                .loot(Loot.self())
                .tag(BlockTag.of("minecraft:rails"));
    }

    // ---- rebar (Material.iron, 15 / 20, block tab) ----
    public static final RegistryObject<BlockRebar> rebar = block("rebar", BlockRebar::new,
            p -> p.strength(15.0F, 20.0F).sound(SoundType.STONE).noOcclusion().requiresCorrectToolForDrops()
                    .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false), HbmTab.BLOCKS)
            .transform(Presets.shape(Models.renderType("minecraft:cutout", rebarModel())))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    // ---- blockstate tables and element models ----

    private static Map<String, List<Placement>> keyholeStates() {
        Map<String, List<Placement>> keyhole = new LinkedHashMap<>();
        keyhole.put("facing=up", List.of(Placement.model("_top")));
        keyhole.put("facing=down", List.of(Placement.model("_top").plus(Placement.rotated(180, 0, false))));
        keyhole.put("facing=north", List.of(Placement.NONE));
        keyhole.put("facing=east", List.of(Placement.rotated(0, 90, false)));
        keyhole.put("facing=south", List.of(Placement.rotated(0, 180, false)));
        keyhole.put("facing=west", List.of(Placement.rotated(0, 270, false)));
        return keyhole;
    }

    /** Every rail state exists waterlogged too; the model does not care. */
    private static Map<String, List<Placement>> railStates() {
        Map<String, List<Placement>> shapes = new LinkedHashMap<>();
        shapes.put("shape=north_south", List.of(Placement.NONE));
        shapes.put("shape=east_west", List.of(Placement.rotated(0, 90, false)));
        shapes.put("shape=ascending_east", List.of(Placement.model("_raised_ne").plus(Placement.rotated(0, 90, false))));
        shapes.put("shape=ascending_west", List.of(Placement.model("_raised_sw").plus(Placement.rotated(0, 90, false))));
        shapes.put("shape=ascending_north", List.of(Placement.model("_raised_ne")));
        shapes.put("shape=ascending_south", List.of(Placement.model("_raised_sw")));
        shapes.put("shape=south_east", List.of(Placement.model("_corner")));
        shapes.put("shape=south_west", List.of(Placement.model("_corner").plus(Placement.rotated(0, 90, false))));
        shapes.put("shape=north_west", List.of(Placement.model("_corner").plus(Placement.rotated(0, 180, false))));
        shapes.put("shape=north_east", List.of(Placement.model("_corner").plus(Placement.rotated(0, 270, false))));
        Map<String, List<Placement>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<Placement>> e : shapes.entrySet()) {
            out.put(e.getKey() + ",waterlogged=false", e.getValue());
            out.put(e.getKey() + ",waterlogged=true", e.getValue());
        }
        return out;
    }

    /** 1.7 BlockRebar full render: four bars along each axis, a quarter block off centre, 2 px thick. */
    private static Shape rebarModel() {
        List<float[]> bars = new ArrayList<>();
        float lo = 7 - 4, hi = 9 - 4, lo2 = 7 + 4, hi2 = 9 + 4;
        float[][] offsets = {{lo, hi}, {lo2, hi2}};
        for (float[] a : offsets) for (float[] b : offsets) {
            bars.add(box(a[0], 0, b[0], a[1], 16, b[1]));
            bars.add(box(0, a[0], b[0], 16, a[1], b[1]));
            bars.add(box(a[0], b[0], 0, a[1], b[1], 16));
        }
        return elements("hbm:blocks/rebar", "hbm:blocks/rebar", "hbm:blocks/rebar", true, bars.toArray(new float[0][]));
    }

    private static float[] box(float x0, float y0, float z0, float x1, float y1, float z1) {
        return new float[]{x0, y0, z0, x1, y1, z1};
    }

    /**
     * A model of plain boxes, each face mapping the part of the texture its bounds cover (1.7 RenderBlocks' UVs).
     * {@code noCull}: 1.7's {@code shouldSideBeRendered} answered true, so no face is culled against neighbours.
     */
    private static Shape elements(String bottom, String top, String side, boolean noCull, float[]... boxes) {
        return base -> {
            JsonObject model = new JsonObject();
            model.addProperty("parent", "minecraft:block/block");
            JsonObject textures = new JsonObject();
            textures.addProperty("particle", side);
            textures.addProperty("bottom", bottom);
            textures.addProperty("top", top);
            textures.addProperty("side", side);
            model.add("textures", textures);
            JsonArray elements = new JsonArray();
            for (float[] b : boxes) {
                JsonObject el = new JsonObject();
                el.add("from", vec(b[0], b[1], b[2]));
                el.add("to", vec(b[3], b[4], b[5]));
                JsonObject faces = new JsonObject();
                faces.add("down", face("#bottom", b[0], 16 - b[5], b[3], 16 - b[2], noCull, b[1] == 0 ? "down" : null));
                faces.add("up", face("#top", b[0], b[2], b[3], b[5], noCull, b[4] == 16 ? "up" : null));
                faces.add("north", face("#side", 16 - b[3], 16 - b[4], 16 - b[0], 16 - b[1], noCull, b[2] == 0 ? "north" : null));
                faces.add("south", face("#side", b[0], 16 - b[4], b[3], 16 - b[1], noCull, b[5] == 16 ? "south" : null));
                faces.add("west", face("#side", b[2], 16 - b[4], b[5], 16 - b[1], noCull, b[0] == 0 ? "west" : null));
                faces.add("east", face("#side", 16 - b[5], 16 - b[4], 16 - b[2], 16 - b[1], noCull, b[3] == 16 ? "east" : null));
                el.add("faces", faces);
                elements.add(el);
            }
            model.add("elements", elements);
            return Map.of(base, model);
        };
    }

    private static JsonArray vec(float x, float y, float z) {
        JsonArray a = new JsonArray();
        a.add(x);
        a.add(y);
        a.add(z);
        return a;
    }

    private static JsonObject face(String texture, float u0, float v0, float u1, float v1, boolean noCull, String cull) {
        JsonObject f = new JsonObject();
        JsonArray uv = new JsonArray();
        uv.add(u0);
        uv.add(v0);
        uv.add(u1);
        uv.add(v1);
        f.add("uv", uv);
        f.addProperty("texture", texture);
        if (!noCull && cull != null) f.addProperty("cullface", cull);
        return f;
    }

    public static void init() {}
}
