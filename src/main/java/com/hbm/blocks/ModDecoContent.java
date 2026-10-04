package com.hbm.blocks;

import dev.golem.core.dejson.When;
import dev.golem.core.dejson.Variant;
import dev.golem.core.dejson.States;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockDepth;
import com.hbm.blocks.generic.BlockFallingBase;
import com.hbm.blocks.generic.HbmStairBlock;
import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.BlockTag;
import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.sound.ModSounds;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Placement;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import com.hbm.blocks.generic.BlockAmmoCrate;
import com.hbm.blocks.generic.BlockChain;
import com.hbm.blocks.generic.BlockCrate;
import com.hbm.blocks.generic.BlockGrateWide;
import com.hbm.blocks.generic.BlockImpactDirt;
import com.hbm.blocks.generic.BlockJungleCrate;
import com.hbm.blocks.generic.BlockNTMDirt;
import com.hbm.blocks.generic.BlockNTMGlassPane;
import com.hbm.blocks.generic.BlockRedBrick;
import com.hbm.blocks.generic.BlockSpeedy;
import com.hbm.blocks.generic.BlockUberConcrete;
import com.hbm.blocks.generic.BlockWriting;
import com.hbm.blocks.generic.DecoModelBlock;
import com.hbm.blocks.generic.DecoPipe;
import com.hbm.blocks.generic.ReinforcedLamp;
import com.hbm.blocks.generic.SpotlightBeams;
import com.hbm.blocks.generic.TritiumLamp;
import com.hbm.blocks.machine.DemonLamp;
import com.hbm.blocks.machine.Floodlight;
import com.hbm.blocks.machine.FloodlightBeam;
import com.hbm.blocks.machine.Spotlight;
import com.hbm.blocks.machine.SpotlightBeam;
import com.hbm.blocks.machine.SpotlightModular;
import com.hbm.items.block.DecoBlockItem;
import com.hbm.sound.ModDecoSounds;
import com.hbm.tileentity.ModDecoBlockEntities;

/**
 * The structural / decorative block family of 1.7 HBM that the generated {@code ModBlocks} does not
 * carry: decorative pipes, rotatable pillars, lamps and spotlights, the odd bricks and concretes,
 * asphalt, vinyl/platemetal, depth bricks, poles, panes, the chain, terrain oddities and the loot
 * crates. Registry ids are the original {@code setBlockName} strings; metadata families are flattened
 * to {@code <base>_<variant>} ({@code vinyl_tile_large}, {@code platemetal_black}, ...).
 *
 * <p>Hardness/resistance numbers are copied literally from {@code ModBlocks} 1.7, as the rest of the
 * port does. {@code Material.iron} blocks require a pickaxe and sit in the mineable tag. Each block's
 * models, blockstate, item model, loot and tags are stated on its registration (Reg; emitted by Dejson at
 * build time). Authored files this class leans on: the four {@code template_pipe*} element models, the two
 * {@code template_chain*} models, the glass-pane multipart blockstates and models, and the
 * {@code hbm:machine_obj} item definitions of the OBJ-drawn blocks ({@code ObjItemDeclarations}).
 */
public final class ModDecoContent {

    private ModDecoContent() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /**
     * The family's custom sound types. A holder, not fields of this class: they read {@code SoundEvents} and
     * HBM's sound registers, which only exist in a running game, and this class also initialises in
     * DejsonMain's plain JVM to record its declarations. Only the properties lambdas touch them.
     */
    public static final class Sounds {
        private Sounds() {}

        /** 1.7 {@code ModSoundTypes.grate}: stone, but a metal-block footstep (same as {@code ModSteelBlocks}). */
        public static final SoundType GRATE_SOUND = new SoundType(0.5F, 1.0F,
                SoundEvents.STONE_BREAK, ModSounds.METAL_BLOCK_EVENT, SoundEvents.STONE_PLACE,
                SoundEvents.STONE_HIT, SoundEvents.STONE_FALL);
        /** 1.7 {@code ModSoundTypes.platemetal}: place/break/step all custom. */
        public static final SoundType PLATEMETAL_SOUND = new SoundType(1.0F, 1.0F,
                ModDecoSounds.PLATEMETAL_PLACE_EVENT, ModDecoSounds.PLATEMETAL_STEP_EVENT, ModDecoSounds.PLATEMETAL_PLACE_EVENT,
                SoundEvents.METAL_HIT, SoundEvents.METAL_FALL);
    }

    /** The 1.7 {@code PlatemetalType} enum, lowercased, in ordinal order. */
    public static final String[] PLATEMETAL_COLORS = {"base", "black", "white", "red", "green", "light_gray", "blue", "purple",
            "cyan", "pink", "lime", "yellow", "light_blue", "magenta", "orange"};

    private static final Supplier<Component> PURELY_DECORATIVE = () -> Component.literal("Purely decorative");
    private static final Supplier<Component> NO_SPAWN = () -> Component.translatable("tile.nospawn").withStyle(ChatFormatting.RED);
    private static final Supplier<Component> DEPTH = () -> Component.translatable("trait.tile.depth").withStyle(ChatFormatting.YELLOW);
    private static final Supplier<Component> SPEEDY = () -> Component.literal("Increases speed by 50%").withStyle(ChatFormatting.BLUE);
    private static final Supplier<Component> GRATE_WIDE = () -> Component.translatable("tile.steel_grate_wide.desc");

    // ---- declaration bundles ----
    /** One texture all round ({@code hbm:blocks/<id>}), single state, dropping itself. */
    private static final Preset CUBE = Presets.cubeAll().and(handle -> handle.loot(Loot.self()));
    /** A spotlight fixture: renderer-drawn (facing x broken). */
    private static final Presets.BlockModel FIXTURE = Presets.rendered(Variants.product(
            Variants.uniform("facing", "down", "up", "north", "south", "west", "east"),
            Variants.uniform("broken", "false", "true")));
    /** OBJ-drawn steel furniture: renderer-drawn, authored machine_obj item icons, dropping itself, pickaxe. */
    private static final Preset FURNITURE = Presets.rendered(Variants.uniform("facing", "north", "south", "west", "east"))
            .and(handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE));

    /** Rotatable pillar: an end and a side texture, three axes, dropping itself. */
    private static Preset pillarModel(String top, String side) {
        return Presets.column("hbm:blocks/" + top, "hbm:blocks/" + side).states(Variants.axis())
                .and(handle -> handle.loot(Loot.self()));
    }

    // ---- decorative pipes (BlockPipe: Material.iron, grate sound, 2/5): an authored octagonal template per
    // rType, textured per colour ----
    public static final RegistryObject<DecoPipe> deco_pipe = pipe("deco_pipe", DecoPipe.Style.PLAIN, "pipe_top", "pipe_side");
    public static final RegistryObject<DecoPipe> deco_pipe_rusted = pipe("deco_pipe_rusted", DecoPipe.Style.PLAIN, "pipe_top_rusty", "pipe_side_rusty");
    public static final RegistryObject<DecoPipe> deco_pipe_green = pipe("deco_pipe_green", DecoPipe.Style.PLAIN, "pipe_top_green", "pipe_side_green");
    public static final RegistryObject<DecoPipe> deco_pipe_green_rusted = pipe("deco_pipe_green_rusted", DecoPipe.Style.PLAIN, "pipe_top_green_rusty", "pipe_side_green_rusty");
    public static final RegistryObject<DecoPipe> deco_pipe_red = pipe("deco_pipe_red", DecoPipe.Style.PLAIN, "pipe_top_red", "pipe_side_red");
    public static final RegistryObject<DecoPipe> deco_pipe_marked = pipe("deco_pipe_marked", DecoPipe.Style.PLAIN, "pipe_top_marked", "pipe_side_marked");
    public static final RegistryObject<DecoPipe> deco_pipe_rim = pipe("deco_pipe_rim", DecoPipe.Style.RIM, "pipe_top", "pipe_side");
    public static final RegistryObject<DecoPipe> deco_pipe_rim_rusted = pipe("deco_pipe_rim_rusted", DecoPipe.Style.RIM, "pipe_top_rusty", "pipe_side_rusty");
    public static final RegistryObject<DecoPipe> deco_pipe_rim_green = pipe("deco_pipe_rim_green", DecoPipe.Style.RIM, "pipe_top_green", "pipe_side_green");
    public static final RegistryObject<DecoPipe> deco_pipe_rim_green_rusted = pipe("deco_pipe_rim_green_rusted", DecoPipe.Style.RIM, "pipe_top_green_rusty", "pipe_side_green_rusty");
    public static final RegistryObject<DecoPipe> deco_pipe_rim_red = pipe("deco_pipe_rim_red", DecoPipe.Style.RIM, "pipe_top_red", "pipe_side_red");
    public static final RegistryObject<DecoPipe> deco_pipe_rim_marked = pipe("deco_pipe_rim_marked", DecoPipe.Style.RIM, "pipe_top_marked", "pipe_side_marked");
    public static final RegistryObject<DecoPipe> deco_pipe_framed = pipe("deco_pipe_framed", DecoPipe.Style.FRAMED, "pipe_top", "pipe_side");
    public static final RegistryObject<DecoPipe> deco_pipe_framed_rusted = pipe("deco_pipe_framed_rusted", DecoPipe.Style.FRAMED, "pipe_top_rusty", "pipe_side_rusty");
    public static final RegistryObject<DecoPipe> deco_pipe_framed_green = pipe("deco_pipe_framed_green", DecoPipe.Style.FRAMED, "pipe_top_green", "pipe_side_green");
    public static final RegistryObject<DecoPipe> deco_pipe_framed_green_rusted = pipe("deco_pipe_framed_green_rusted", DecoPipe.Style.FRAMED, "pipe_top_green_rusty", "pipe_side_green_rusty");
    public static final RegistryObject<DecoPipe> deco_pipe_framed_red = pipe("deco_pipe_framed_red", DecoPipe.Style.FRAMED, "pipe_top_red", "pipe_side_red");
    public static final RegistryObject<DecoPipe> deco_pipe_framed_marked = pipe("deco_pipe_framed_marked", DecoPipe.Style.FRAMED, "pipe_top_marked", "pipe_side_marked");
    public static final RegistryObject<DecoPipe> deco_pipe_quad = pipe("deco_pipe_quad", DecoPipe.Style.QUAD, "pipe_top", "pipe_side");
    public static final RegistryObject<DecoPipe> deco_pipe_quad_rusted = pipe("deco_pipe_quad_rusted", DecoPipe.Style.QUAD, "pipe_top_rusty", "pipe_side_rusty");
    public static final RegistryObject<DecoPipe> deco_pipe_quad_green = pipe("deco_pipe_quad_green", DecoPipe.Style.QUAD, "pipe_top_green", "pipe_side_green");
    public static final RegistryObject<DecoPipe> deco_pipe_quad_green_rusted = pipe("deco_pipe_quad_green_rusted", DecoPipe.Style.QUAD, "pipe_top_green_rusty", "pipe_side_green_rusty");
    public static final RegistryObject<DecoPipe> deco_pipe_quad_red = pipe("deco_pipe_quad_red", DecoPipe.Style.QUAD, "pipe_top_red", "pipe_side_red");
    public static final RegistryObject<DecoPipe> deco_pipe_quad_marked = pipe("deco_pipe_quad_marked", DecoPipe.Style.QUAD, "pipe_top_marked", "pipe_side_marked");

    // ---- rotatable pillars (BlockRotatablePillar) ----
    public static final RegistryObject<RotatedPillarBlock> block_schrabidium_cluster = pillar("block_schrabidium_cluster",
            p -> p.strength(5.0F, 60000.0F).requiresCorrectToolForDrops(), i -> i.rarity(Rarity.RARE))
            .transform(pillarModel("block_schrabidium_cluster_top", "block_schrabidium_cluster_side")).tag(BlockTag.PICKAXE);
    public static final RegistryObject<RotatedPillarBlock> block_euphemium_cluster = pillar("block_euphemium_cluster",
            p -> p.strength(5.0F, 60000.0F).requiresCorrectToolForDrops(), UnaryOperator.identity())
            .transform(pillarModel("block_euphemium_cluster_top", "block_euphemium_cluster_side")).tag(BlockTag.PICKAXE);
    public static final RegistryObject<RotatedPillarBlock> block_insulator = pillar("block_insulator",
            p -> p.strength(5.0F, 10.0F).sound(SoundType.WOOL), UnaryOperator.identity())
            .transform(pillarModel("block_insulator_top", "block_insulator_side"));
    public static final RegistryObject<RotatedPillarBlock> block_fiberglass = pillar("block_fiberglass",
            p -> p.strength(5.0F, 15.0F).sound(SoundType.WOOL), UnaryOperator.identity())
            .transform(pillarModel("block_fiberglass_top", "block_fiberglass_side"));
    public static final RegistryObject<RotatedPillarBlock> block_tritium = pillar("block_tritium",
            p -> p.strength(3.0F, 2.0F).sound(SoundType.GLASS), UnaryOperator.identity())
            .transform(pillarModel("block_tritium_top", "block_tritium_side"));
    public static final RegistryObject<RotatedPillarBlock> concrete_pillar = pillar("concrete_pillar",
            p -> p.strength(15.0F, 180.0F).requiresCorrectToolForDrops(), UnaryOperator.identity())
            .transform(pillarModel("concrete_pillar_top", "concrete_pillar_side")).tag(BlockTag.PICKAXE);
    public static final RegistryObject<RotatedPillarBlock> meteor_pillar = pillar("meteor_pillar",
            p -> p.strength(15.0F, 360.0F).requiresCorrectToolForDrops(), UnaryOperator.identity())
            .transform(pillarModel("meteor_pillar_top", "meteor_pillar")).tag(BlockTag.PICKAXE);

    // ---- lamps (a lit block drops, and is picked as, its unlit twin) ----
    public static final RegistryObject<Block> reinforced_light = plain("reinforced_light", Block::new,
            p -> p.strength(15.0F, 80.0F).lightLevel(s -> 15).requiresCorrectToolForDrops(), HbmTab.BLOCKS)
            .transform(CUBE).tag(BlockTag.PICKAXE);
    public static final RegistryObject<ReinforcedLamp> reinforced_lamp_off = REG.block("reinforced_lamp_off",
                    p -> new ReinforcedLamp(p, false, () -> ModDecoContent.reinforced_lamp_off.get(), () -> ModDecoContent.reinforced_lamp_on.get()),
                    p -> p.strength(15.0F, 80.0F).requiresCorrectToolForDrops())
            .transform(CUBE).tag(BlockTag.PICKAXE);
    public static final RegistryObject<ReinforcedLamp> reinforced_lamp_on = REG.block("reinforced_lamp_on",
                    p -> new ReinforcedLamp(p, true, () -> ModDecoContent.reinforced_lamp_off.get(), () -> ModDecoContent.reinforced_lamp_on.get()),
                    p -> p.strength(15.0F, 80.0F).lightLevel(s -> 15).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll()).loot(Loot.only("hbm:reinforced_lamp_off")).tag(BlockTag.PICKAXE);
    public static final RegistryObject<TritiumLamp> lamp_tritium_green_off = tritiumLamp("lamp_tritium_green_off", false, SpotlightBeams.META_GREEN,
            () -> ModDecoContent.lamp_tritium_green_off.get(), () -> ModDecoContent.lamp_tritium_green_on.get())
            .transform(CUBE);
    public static final RegistryObject<TritiumLamp> lamp_tritium_green_on = tritiumLamp("lamp_tritium_green_on", true, SpotlightBeams.META_GREEN,
            () -> ModDecoContent.lamp_tritium_green_off.get(), () -> ModDecoContent.lamp_tritium_green_on.get())
            .transform(Presets.cubeAll()).loot(Loot.only("hbm:lamp_tritium_green_off"));
    public static final RegistryObject<TritiumLamp> lamp_tritium_blue_off = tritiumLamp("lamp_tritium_blue_off", false, SpotlightBeams.META_BLUE,
            () -> ModDecoContent.lamp_tritium_blue_off.get(), () -> ModDecoContent.lamp_tritium_blue_on.get())
            .transform(CUBE);
    public static final RegistryObject<TritiumLamp> lamp_tritium_blue_on = tritiumLamp("lamp_tritium_blue_on", true, SpotlightBeams.META_BLUE,
            () -> ModDecoContent.lamp_tritium_blue_off.get(), () -> ModDecoContent.lamp_tritium_blue_on.get())
            .transform(Presets.cubeAll()).loot(Loot.only("hbm:lamp_tritium_blue_off"));

    /**
     * 1.7 {@code BlockLantern}: a five-tall folded post whose lamp blinds glyphids in range.
     * {@code Material.iron}, hardness 3, metal sound; the light comes from the lamp cell, not the base.
     * Drawn entirely by LanternRenderer, so the blockstate exists only because vanilla demands one
     * (FACING x CELL is BlockMultiblockMachine's state space). The item is RenderLantern's OBJ item renderer, the
     * typed definition in ObjItemDeclarations (it was the flat block_steel sprite until the 2026-10-01 icon audit).
     */
    public static final RegistryObject<com.hbm.blocks.generic.BlockLantern> lantern =
            plain("lantern", com.hbm.blocks.generic.BlockLantern::new,
                    p -> p.strength(3.0F).sound(SoundType.METAL).lightLevel(s -> 15).noOcclusion()
                            .requiresCorrectToolForDrops()
                            .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false),
                    HbmTab.BLOCKS)
            .transform(Presets.rendered(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                    Variants.uniform("cell", "false", "true"))))
            .loot(Loot.self());

    /** DemonLamp: BlockContainer(Material.iron), metal sound, light 1.0, hardness 3. */
    public static final RegistryObject<DemonLamp> lamp_demon = plain("lamp_demon", DemonLamp::new,
            p -> p.strength(3.0F).sound(SoundType.METAL).lightLevel(s -> 15).noOcclusion().requiresCorrectToolForDrops()
                    .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false), HbmTab.BLOCKS)
            .transform(Presets.rendered(Variants.uniform("facing", "down", "up", "north", "south", "west", "east")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    // ---- spotlights (Material.iron, hardness 0.5; lit by default, redstone switches them off): renderer-drawn;
    // the off block drops the on block (1.7 getItemDropped -> getOn) ----
    public static final RegistryObject<Spotlight> spotlight_incandescent = spotlight("spotlight_incandescent", 2, Spotlight.LightType.INCANDESCENT, true,
            () -> ModDecoContent.spotlight_incandescent_off.get(), () -> ModDecoContent.spotlight_incandescent.get())
            .transform(FIXTURE).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<Spotlight> spotlight_incandescent_off = spotlight("spotlight_incandescent_off", 2, Spotlight.LightType.INCANDESCENT, false,
            () -> ModDecoContent.spotlight_incandescent_off.get(), () -> ModDecoContent.spotlight_incandescent.get())
            .transform(FIXTURE).loot(Loot.only("hbm:spotlight_incandescent")).tag(BlockTag.PICKAXE);
    public static final RegistryObject<Spotlight> spotlight_fluoro = spotlight("spotlight_fluoro", 8, Spotlight.LightType.FLUORESCENT, true,
            () -> ModDecoContent.spotlight_fluoro_off.get(), () -> ModDecoContent.spotlight_fluoro.get())
            .transform(FIXTURE).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<Spotlight> spotlight_fluoro_off = spotlight("spotlight_fluoro_off", 8, Spotlight.LightType.FLUORESCENT, false,
            () -> ModDecoContent.spotlight_fluoro_off.get(), () -> ModDecoContent.spotlight_fluoro.get())
            .transform(FIXTURE).loot(Loot.only("hbm:spotlight_fluoro")).tag(BlockTag.PICKAXE);
    public static final RegistryObject<Spotlight> spotlight_halogen = spotlight("spotlight_halogen", 32, Spotlight.LightType.HALOGEN, true,
            () -> ModDecoContent.spotlight_halogen_off.get(), () -> ModDecoContent.spotlight_halogen.get())
            .transform(FIXTURE).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<Spotlight> spotlight_halogen_off = spotlight("spotlight_halogen_off", 32, Spotlight.LightType.HALOGEN, false,
            () -> ModDecoContent.spotlight_halogen_off.get(), () -> ModDecoContent.spotlight_halogen.get())
            .transform(FIXTURE).loot(Loot.only("hbm:spotlight_halogen")).tag(BlockTag.PICKAXE);
    /** BlockBeamBase: unbreakable, unlit-looking, full light, replaceable, no item. */
    public static final RegistryObject<SpotlightBeam> spotlight_beam = REG.block("spotlight_beam", SpotlightBeam::new,
                    ModDecoContent::beamProperties)
            .transform(Presets.rendered(Variants.product(Variants.uniform("color", "0", "1", "2"),
                    Variants.uniform("down", "false", "true"), Variants.uniform("up", "false", "true"),
                    Variants.uniform("north", "false", "true"), Variants.uniform("south", "false", "true"),
                    Variants.uniform("west", "false", "true"), Variants.uniform("east", "false", "true"))))
            .loot(Loot.nothing());
    /** The powered floodlight (Material.iron, 5/10; renderer-drawn facing x flip, item icon authored) and the bare light block its rays end in. */
    public static final RegistryObject<Floodlight> floodlight = plain("floodlight", Floodlight::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()
                    .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false), HbmTab.BLOCKS)
            .transform(Presets.rendered(Variants.product(Variants.uniform("facing", "down", "up", "north", "south", "west", "east"),
                    Variants.uniform("flip", "false", "true"))))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<FloodlightBeam> floodlight_beam = REG.block("floodlight_beam", FloodlightBeam::new,
                    ModDecoContent::beamProperties)
            .transform(Presets.rendered(Variants.simple()))
            .loot(Loot.nothing());

    private static BlockBehaviour.Properties beamProperties(BlockBehaviour.Properties p) {
        return p.strength(-1.0F, 1_000_000.0F).lightLevel(s -> 15).noCollision().noOcclusion().replaceable()
                .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false).noLootTable();
    }

    // ---- bricks and concrete ----
    /**
     * Red Room brick: no creative tab, no drop, blast resistance 10,000. The marked face follows the six-way
     * facing; up/down use the "_top" icon set. plain=true is 1.7 meta 6 (no marked face, brick_base all round),
     * set only by the keyhole rooms.
     */
    public static final RegistryObject<BlockRedBrick> brick_red = REG.block("brick_red", BlockRedBrick::new,
                    p -> p.strength(0.0F, 10_000.0F))
            .model(d -> d.models(Models.group()
                            .and("", Models.parented("minecraft:block/cube", Slots.of("particle", "hbm:blocks/brick_base")
                                    .and("down", "hbm:blocks/brick_base").and("up", "hbm:blocks/brick_base")
                                    .and("north", "hbm:blocks/brick_red").and("south", "hbm:blocks/brick_base")
                                    .and("west", "hbm:blocks/brick_base").and("east", "hbm:blocks/brick_base")))
                            .and("_top", Models.cubeBottomTop("hbm:blocks/brick_base", "hbm:blocks/brick_base", "hbm:blocks/brick_red_top"))
                            .and("_plain", Models.cubeAll("hbm:blocks/brick_base")))
                    .states(Variants.of(Set.of("facing", "plain"), redBrickStates()))
                    .blockItemDirect())
            // 1.7: drops nothing, but a plain cube without a tile entity, so silk touch keeps it
            .loot(Loot.silkTouchOnly());
    public static final RegistryObject<BlockWriting> brick_concrete_marked = plain("brick_concrete_marked", BlockWriting::new,
            p -> p.strength(15.0F, 160.0F).requiresCorrectToolForDrops(), HbmTab.BLOCKS)
            .transform(Presets.cubeBottomTop("hbm:blocks/brick_concrete", "hbm:blocks/brick_concrete_marked", "hbm:blocks/brick_concrete"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<Block> brick_ducrete = noSpawn("brick_ducrete", 15.0F, 750.0F)
            .transform(CUBE).tag(BlockTag.PICKAXE);
    public static final RegistryObject<Block> reinforced_ducrete = noSpawn("reinforced_ducrete", 20.0F, 1000.0F)
            .transform(CUBE).tag(BlockTag.PICKAXE);
    public static final RegistryObject<Block> concrete_rebar = noSpawn("concrete_rebar", 50.0F, 240.0F)
            .transform(CUBE).tag(BlockTag.PICKAXE);
    public static final RegistryObject<HbmStairBlock> brick_ducrete_stairs = stairs("brick_ducrete_stairs", brick_ducrete,
                    p -> p.strength(15.0F, 750.0F).requiresCorrectToolForDrops())
            .transform(Presets.shape(Models.stairsAll("hbm:blocks/brick_ducrete")).states(Variants.stairs()))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    /** Über concrete: damage 0-9 pristine, 10-11 m0, 12-13 m1, 14 m2, 15 m3 (1.7 getIcon). */
    public static final RegistryObject<BlockUberConcrete> concrete_super = plain("concrete_super", BlockUberConcrete::new,
            p -> p.strength(150.0F, 1000.0F).randomTicks().requiresCorrectToolForDrops(), HbmTab.BLOCKS)
            .model(d -> d.models(Models.group()
                            .and("", Models.cubeAll("hbm:blocks/concrete_super"))
                            .and("_m0", Models.cubeAll("hbm:blocks/concrete_super_m0"))
                            .and("_m1", Models.cubeAll("hbm:blocks/concrete_super_m1"))
                            .and("_m2", Models.cubeAll("hbm:blocks/concrete_super_m2"))
                            .and("_m3", Models.cubeAll("hbm:blocks/concrete_super_m3")))
                    .states(Variants.of(Set.of("damage"), uberStates()))
                    .blockItemDirect())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockFallingBase> concrete_super_broken = plain("concrete_super_broken", BlockFallingBase::new,
            p -> p.strength(10.0F, 20.0F).requiresCorrectToolForDrops(), HbmTab.BLOCKS)
            .transform(CUBE).tag(BlockTag.PICKAXE);

    // ---- asphalt (BlockSpeedy x1.5) ----
    public static final RegistryObject<BlockSpeedy> asphalt = plain("asphalt", p -> new BlockSpeedy(p, 1.5),
            p -> p.strength(15.0F, 120.0F).requiresCorrectToolForDrops(), HbmTab.BLOCKS, SPEEDY)
            .transform(CUBE).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockSpeedy> asphalt_light = plain("asphalt_light", p -> new BlockSpeedy(p, 1.5),
            p -> p.strength(15.0F, 120.0F).lightLevel(s -> 15).requiresCorrectToolForDrops(), HbmTab.BLOCKS, SPEEDY)
            .transform(CUBE).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockSpeedy.Stairs> asphalt_stairs = REG.block("asphalt_stairs",
                    p -> new BlockSpeedy.Stairs(asphalt.get().defaultBlockState(), p, 1.5),
                    p -> p.strength(15.0F, 120.0F).requiresCorrectToolForDrops())
            .transform(Presets.shape(Models.stairsAll("hbm:blocks/asphalt")).states(Variants.stairs()))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    /** The asphalt entry of the old {@code concrete_slab} multi-slab. */
    public static final RegistryObject<BlockSpeedy.Slab> asphalt_slab = REG.block("asphalt_slab", BlockSpeedy.Slab::new,
                    p -> p.strength(15.0F, 120.0F).requiresCorrectToolForDrops())
            .transform(Presets.shape(Models.slabAll("hbm:blocks/asphalt")).states(Variants.slab()))
            .loot(Loot.slab())
            .tag(BlockTag.PICKAXE);

    // ---- vinyl tile / platemetal (flattened enum blocks; the dotted 1.7 texture names are kept) ----
    public static final RegistryObject<Block> vinyl_tile_large = plain("vinyl_tile_large", Block::new,
            p -> p.strength(10.0F, 60.0F).sound(SoundType.GLASS).requiresCorrectToolForDrops(), HbmTab.BLOCKS)
            .transform(Presets.cubeAll("hbm:blocks/vinyl_tile.large")).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<Block> vinyl_tile_small = plain("vinyl_tile_small", Block::new,
            p -> p.strength(10.0F, 60.0F).sound(SoundType.GLASS).requiresCorrectToolForDrops(), HbmTab.BLOCKS)
            .transform(Presets.cubeAll("hbm:blocks/vinyl_tile.small")).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final java.util.Map<String, RegistryObject<Block>> platemetal = new java.util.LinkedHashMap<>();

    static {
        for (String color : PLATEMETAL_COLORS) {
            platemetal.put(color, plain("platemetal_" + color, Block::new,
                    p -> p.strength(5.0F, 10.0F).sound(Sounds.PLATEMETAL_SOUND).requiresCorrectToolForDrops(), HbmTab.BLOCKS)
                    .transform(Presets.cubeAll("hbm:blocks/platemetal." + color)).loot(Loot.self()).tag(BlockTag.PICKAXE));
        }
    }

    // ---- depth bricks (unbreakable, blast resistance 10 / 60,000 for DNT) ----
    public static final RegistryObject<BlockDepth> depth_brick = depth("depth_brick", 10.0F);
    public static final RegistryObject<BlockDepth> depth_tiles = depth("depth_tiles", 10.0F);
    public static final RegistryObject<BlockDepth> depth_nether_brick = depth("depth_nether_brick", 10.0F);
    public static final RegistryObject<BlockDepth> depth_nether_tiles = depth("depth_nether_tiles", 10.0F);
    public static final RegistryObject<BlockDepth> depth_dnt = depth("depth_dnt", 60000.0F);

    // ---- steel odds and ends ----
    /** Item grate: the ten authored steel_grate heights, re-skinned with the wide top. */
    public static final RegistryObject<BlockGrateWide> steel_grate_wide = plain("steel_grate_wide", BlockGrateWide::new,
            p -> p.strength(2.0F, 5.0F).sound(Sounds.GRATE_SOUND).noOcclusion().requiresCorrectToolForDrops()
                    .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false), HbmTab.BLOCKS, GRATE_WIDE)
            .model(d -> d.models(grateWideModels())
                    .states(Variants.select("height", "0", "0", "1", "1", "2", "2", "3", "3", "4", "4",
                            "5", "5", "6", "6", "7", "7", "8", "8", "9", "9"))
                    .item(Models.blockItem("hbm:block/steel_grate_wide0")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    // panes: multipart blockstates and models are authored; only the flat item and the loot are declared
    public static final RegistryObject<BlockNTMGlassPane> reinforced_glass_pane = plain("reinforced_glass_pane", BlockNTMGlassPane::new,
            p -> p.strength(2.0F, 25.0F).sound(SoundType.GLASS).noOcclusion(), HbmTab.BLOCKS)
            .transform(Presets.sprite("hbm:blocks/reinforced_glass_pane"))
            // 1.7 BlockNTMGlassPane passes "drops itself = false" to BlockPane, whose getItemDropped is then null
            // (doesDrop only fed quantityDropped); BlockPane.canSilkHarvest() is true. Same for the laminate pane.
            .loot(Loot.silkTouchOnly())
            .tag(BlockTag.PICKAXE)
            .andThen(handle -> glassPaneStates());
    public static final RegistryObject<BlockNTMGlassPane> reinforced_laminate_pane = plain("reinforced_laminate_pane", BlockNTMGlassPane::new,
            p -> p.strength(15.0F, 300.0F).sound(SoundType.GLASS).noOcclusion().requiresCorrectToolForDrops(), HbmTab.BLOCKS) // 1.7 Material.rock
            .transform(Presets.sprite("hbm:blocks/reinforced_laminate_pane"))
            .loot(Loot.silkTouchOnly())
            .tag(BlockTag.PICKAXE)
            .andThen(handle -> laminatePaneStates());
    /** Chain: hanging cross or flat wall plane, "end" icon when nothing holds it from below. */
    public static final RegistryObject<BlockChain> dungeon_chain = plain("dungeon_chain", BlockChain::new,
            p -> p.strength(0.25F, 2.0F).noOcclusion().noCollision().requiresCorrectToolForDrops(), HbmTab.BLOCKS) // 1.7: no setStepSound -> default stone
            .model(d -> d.models(Models.group()
                            .and("", Models.parented("hbm:block/template_chain", Slots.of("chain", "hbm:blocks/chain")))
                            .and("_end", Models.parented("hbm:block/template_chain", Slots.of("chain", "hbm:blocks/chain_end")))
                            .and("_wall", Models.parented("hbm:block/template_chain_wall", Slots.of("chain", "hbm:blocks/chain")))
                            .and("_wall_end", Models.parented("hbm:block/template_chain_wall", Slots.of("chain", "hbm:blocks/chain_end"))))
                    .states(Variants.of(Set.of("facing", "wall", "end"), chainStates()))
                    .item(Models.generated("hbm:blocks/chain")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.CLIMBABLE);

    // ---- terrain ----
    /** HBM's dirt, named "Dirt", drops vanilla dirt, no creative tab (the red-crate easter egg). */
    public static final RegistryObject<BlockNTMDirt> ntm_dirt = REG.block("ntm_dirt", BlockNTMDirt::new,
                    p -> p.strength(0.5F).sound(SoundType.GRAVEL))
            .transform(Presets.cubeAll("minecraft:block/dirt"))
            // 1.7 BlockDirt: one dirt, no fortune; silk touch keeps the block
            .loot(Loot.oreNoFortune("minecraft:dirt", 1, 1))
            .tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockImpactDirt> impact_dirt = plain("impact_dirt", BlockImpactDirt::new,
            p -> p.strength(0.5F).sound(SoundType.GRAVEL).randomTicks(), HbmTab.BLOCKS)
            .transform(Presets.cubeAll("hbm:blocks/waste_earth_bottom"))
            // 1.7 BlockDirt: one dirt, no fortune; silk touch keeps the block
            .loot(Loot.oreNoFortune("minecraft:dirt", 1, 1))
            .tag(BlockTag.SHOVEL);
    /** The two {@code sand_mix} subtypes the earlier waves did not carry (boron/lead/quartz already exist). */
    public static final RegistryObject<BlockFallingBase> sand_uranium = plain("sand_uranium", BlockFallingBase::new,
            p -> p.strength(0.5F).sound(SoundType.SAND), HbmTab.MACHINES)
            .transform(CUBE).tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockFallingBase> sand_polonium = plain("sand_polonium", BlockFallingBase::new,
            p -> p.strength(0.5F).sound(SoundType.SAND), HbmTab.MACHINES)
            .transform(CUBE).tag(BlockTag.SHOVEL);

    // ---- loot crates ----
    public static final RegistryObject<BlockCrate> crate_weapon = plain("crate_weapon", p -> new BlockCrate(p, BlockCrate.Kind.WEAPON),
            p -> p.strength(5.0F, 10.0F).sound(SoundType.WOOD), HbmTab.CONSUMABLES)
            .transform(CUBE);
    public static final RegistryObject<BlockCrate> crate_lead = plain("crate_lead", p -> new BlockCrate(p, BlockCrate.Kind.LEAD),
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), HbmTab.CONSUMABLES)
            .transform(CUBE).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockCrate> crate_metal = plain("crate_metal", p -> new BlockCrate(p, BlockCrate.Kind.METAL),
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), HbmTab.CONSUMABLES)
            .transform(CUBE).tag(BlockTag.PICKAXE);
    /** The red crate has no creative tab in 1.7. */
    public static final RegistryObject<BlockCrate> crate_red = plain("crate_red", p -> new BlockCrate(p, BlockCrate.Kind.RED),
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), null)
            .transform(CUBE).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockAmmoCrate> crate_ammo = plain("crate_ammo", BlockAmmoCrate::new,
            p -> p.strength(1.0F, 2.5F).sound(SoundType.METAL).requiresCorrectToolForDrops(), HbmTab.CONSUMABLES)
            .transform(Presets.cubeBottomTop("hbm:blocks/crate_ammo_bottom", "hbm:blocks/crate_ammo_side", "hbm:blocks/crate_ammo_top"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockJungleCrate> crate_jungle = plain("crate_jungle", BlockJungleCrate::new,
            p -> p.strength(1.0F, 2.5F).sound(SoundType.STONE).requiresCorrectToolForDrops(), HbmTab.CONSUMABLES) // 1.7 Material.rock
            .transform(CUBE).tag(BlockTag.PICKAXE);

    // ---- OBJ-drawn decorations (invisible block + TileEntityDecoModel + DecoModelRenderer) ----
    public static final RegistryObject<DecoModelBlock> steel_poles = model("steel_poles", DecoModelBlock.Kind.POLE, 5.0F, 15.0F, true);
    public static final RegistryObject<DecoModelBlock> pole_top = model("pole_top", DecoModelBlock.Kind.TOP, 5.0F, 15.0F, true);
    /** 1.7 render type -1 with no item renderer: the item is the block's flat {@code deco_satellite_receiver} icon. */
    public static final RegistryObject<DecoModelBlock> pole_satellite_receiver = model("pole_satellite_receiver", DecoModelBlock.Kind.SATELLITE, 5.0F, 15.0F, true)
            .transform(Presets.sprite("hbm:blocks/deco_satellite_receiver"));
    public static final RegistryObject<DecoModelBlock> tape_recorder = model("tape_recorder", DecoModelBlock.Kind.TAPE_RECORDER, 5.0F, 15.0F, true);
    /** BlockDecoToaster subtypes (Material.iron, 5/10), flattened. */
    public static final RegistryObject<DecoModelBlock> deco_toaster_iron = model("deco_toaster_iron", DecoModelBlock.Kind.TOASTER_IRON, 5.0F, 10.0F, true);
    public static final RegistryObject<DecoModelBlock> deco_toaster_steel = model("deco_toaster_steel", DecoModelBlock.Kind.TOASTER_STEEL, 5.0F, 10.0F, true);
    public static final RegistryObject<DecoModelBlock> deco_toaster_wood = model("deco_toaster_wood", DecoModelBlock.Kind.TOASTER_WOOD, 5.0F, 10.0F, true);
    /** BlockDecoCRT subtypes (Material.iron, 5/10), flattened. */
    public static final RegistryObject<DecoModelBlock> deco_crt_clean = model("deco_crt_clean", DecoModelBlock.Kind.CRT_CLEAN, 5.0F, 10.0F, true);
    public static final RegistryObject<DecoModelBlock> deco_crt_broken = model("deco_crt_broken", DecoModelBlock.Kind.CRT_BROKEN, 5.0F, 10.0F, true);
    public static final RegistryObject<DecoModelBlock> deco_crt_blinking = model("deco_crt_blinking", DecoModelBlock.Kind.CRT_BLINKING, 5.0F, 10.0F, true);
    public static final RegistryObject<DecoModelBlock> deco_crt_bsod = model("deco_crt_bsod", DecoModelBlock.Kind.CRT_BSOD, 5.0F, 10.0F, true);
    /** BlockDecoModel(DecoComputerEnum.IBM_300PL) (Material.iron, 5/10), flattened. */
    public static final RegistryObject<DecoModelBlock> deco_computer_ibm_300pl = model("deco_computer_ibm_300pl", DecoModelBlock.Kind.COMPUTER, 5.0F, 10.0F, true);

    private static Reg.Handle<DecoModelBlock> model(String id, DecoModelBlock.Kind kind, float hardness, float resistance, boolean iron) {
        return plain(id, p -> new DecoModelBlock(p, kind), p -> {
            p = p.strength(hardness, resistance).noOcclusion().isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false);
            return iron ? p.requiresCorrectToolForDrops() : p;
        }, HbmTab.BLOCKS).transform(FURNITURE);
    }

    static {
        // Block items for the blocks that have no creative-tab entry but still had an ItemBlock in 1.7.
        REG.blockItem(brick_red, Compat::blockDescription);
        REG.blockItem(ntm_dirt, Compat::blockDescription);
        // 1.7: reinforced_lamp_off is the crafted, tabbed item; the lit twin has a tab-less ItemBlock.
        REG.blockItem(reinforced_lamp_off, Compat::blockDescription).tab(HbmTab.BLOCKS);
        REG.blockItem(reinforced_lamp_on, Compat::blockDescription);
        REG.blockItem(lamp_tritium_green_on, Compat::blockDescription);
        REG.blockItem(lamp_tritium_blue_on, Compat::blockDescription);
        REG.blockItem(spotlight_incandescent_off, Compat::blockDescription);
        REG.blockItem(spotlight_fluoro_off, Compat::blockDescription);
        REG.blockItem(spotlight_halogen_off, Compat::blockDescription);
        REG.blockItem(asphalt_stairs, Compat::blockDescription).tab(HbmTab.BLOCKS);
        REG.blockItem(asphalt_slab, Compat::blockDescription).tab(HbmTab.BLOCKS);
    }

    // ---- blockstate tables ----

    private static Map<String, List<Placement>> redBrickStates() {
        Map<String, List<Placement>> redBrick = new LinkedHashMap<>();
        redBrick.put("facing=up,plain=false", List.of(Placement.model("_top")));
        redBrick.put("facing=down,plain=false", List.of(Placement.model("_top").plus(Placement.rotated(180, 0, false))));
        redBrick.put("facing=north,plain=false", List.of(Placement.NONE));
        redBrick.put("facing=east,plain=false", List.of(Placement.rotated(0, 90, false)));
        redBrick.put("facing=south,plain=false", List.of(Placement.rotated(0, 180, false)));
        redBrick.put("facing=west,plain=false", List.of(Placement.rotated(0, 270, false)));
        for (String facing : new String[]{"up", "down", "north", "east", "south", "west"}) {
            redBrick.put("facing=" + facing + ",plain=true", List.of(Placement.model("_plain")));
        }
        return redBrick;
    }

    private static Map<String, List<Placement>> uberStates() {
        Map<String, List<Placement>> uber = new LinkedHashMap<>();
        for (int damage = 0; damage <= 15; damage++) {
            String suffix = damage == 15 ? "_m3" : damage == 14 ? "_m2" : damage > 11 ? "_m1" : damage > 9 ? "_m0" : "";
            uber.put("damage=" + damage, List.of(Placement.model(suffix)));
        }
        return uber;
    }

    private static Models.Group grateWideModels() {
        Models.Group grate = Models.group();
        for (int height = 0; height <= 9; height++) {
            grate = grate.and(String.valueOf(height), Models.parented("hbm:block/steel_grate" + height,
                    Slots.of("end", "hbm:blocks/grate_wide_top", "particle", "hbm:blocks/grate_wide_top")));
        }
        return grate;
    }

    private static Map<String, List<Placement>> chainStates() {
        Map<String, List<Placement>> chain = new LinkedHashMap<>();
        String[] facings = {"north", "east", "south", "west"};
        for (int quarter = 0; quarter < facings.length; quarter++) {
            for (String end : new String[]{"false", "true"}) {
                String endSuffix = "true".equals(end) ? "_end" : "";
                chain.put("facing=" + facings[quarter] + ",wall=false,end=" + end, List.of(Placement.model(endSuffix)));
                chain.put("facing=" + facings[quarter] + ",wall=true,end=" + end,
                        List.of(Placement.model("_wall" + endSuffix).plus(Placement.rotated(0, quarter * 90, false))));
            }
        }
        return chain;
    }

    // ---- helpers ----

    private static Reg.Handle<DecoPipe> pipe(String id, DecoPipe.Style style, String top, String side) {
        Reg.Handle<DecoPipe> block = REG.block(id, p -> new DecoPipe(p, style),
                p -> p.strength(2.0F, 5.0F).sound(Sounds.GRATE_SOUND).noOcclusion().requiresCorrectToolForDrops());
        REG.item(id, (net.minecraft.world.item.Item.Properties p) -> new DecoBlockItem(block.get(), Compat.blockDescription(p), List.of(PURELY_DECORATIVE)))
                .tab(HbmTab.BLOCKS);
        return block
                .transform(Presets.shape(Models.parented("hbm:block/" + style.template,
                                Slots.of("top", "hbm:blocks/" + top, "side", "hbm:blocks/" + side, "particle", "hbm:blocks/" + top)))
                        .states(Variants.axis()))
                .loot(Loot.self())
                .tag(BlockTag.PICKAXE);
    }

    private static Reg.Handle<RotatedPillarBlock> pillar(String id, UnaryOperator<BlockBehaviour.Properties> props,
                                                         UnaryOperator<net.minecraft.world.item.Item.Properties> itemProps) {
        Reg.Handle<RotatedPillarBlock> block = REG.block(id, RotatedPillarBlock::new, props);
        REG.blockItem(block, p -> itemProps.apply(Compat.blockDescription(p))).tab(HbmTab.BLOCKS);
        return block;
    }

    private static Reg.Handle<TritiumLamp> tritiumLamp(String id, boolean on, int meta, Supplier<Block> off, Supplier<Block> onBlock) {
        Reg.Handle<TritiumLamp> block = REG.block(id, p -> new TritiumLamp(p, on, meta, off, onBlock),
                p -> p.strength(3.0F).sound(SoundType.GLASS).lightLevel(s -> on ? 15 : 0));
        if (!on) {
            REG.blockItem(block, Compat::blockDescription).tab(HbmTab.BLOCKS);
        }
        return block;
    }

    private static Reg.Handle<Spotlight> spotlight(String id, int beamLength, Spotlight.LightType type, boolean on,
                                                   Supplier<Block> off, Supplier<Block> onBlock) {
        Function<BlockBehaviour.Properties, Spotlight> factory = type == Spotlight.LightType.FLUORESCENT
                ? p -> new SpotlightModular(p, beamLength, type, on, off, onBlock)
                : p -> new Spotlight(p, beamLength, type, on, off, onBlock);
        Reg.Handle<Spotlight> block = REG.block(id, factory,
                p -> p.strength(0.5F, 0.0F).sound(SoundType.METAL).noOcclusion().noCollision().requiresCorrectToolForDrops()
                        .lightLevel(s -> on ? 15 : 0).isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false)
                        .dynamicShape());
        if (on) {
            REG.blockItem(block, Compat::blockDescription).tab(HbmTab.BLOCKS);
        }
        return block;
    }

    private static Reg.Handle<Block> noSpawn(String id, float hardness, float resistance) {
        return plain(id, Block::new, p -> p.strength(hardness, resistance).requiresCorrectToolForDrops()
                .isValidSpawn((state, level, pos, type) -> false), HbmTab.BLOCKS, NO_SPAWN);
    }

    /** Unbreakable, so no loot (like stone_depth). */
    private static Reg.Handle<BlockDepth> depth(String id, float resistance) {
        // 1.7 BlockDepth has no drop override: a depth-rock tool (or an explosion) gets the block back.
        return plain(id, BlockDepth::new, p -> p.strength(-1.0F, resistance).sound(SoundType.STONE)
                .requiresCorrectToolForDrops(), HbmTab.BLOCKS, DEPTH)
                .transform(Presets.cubeAll())
                .loot(Loot.self())
                .tag(BlockTag.PICKAXE);
    }

    private static Reg.Handle<HbmStairBlock> stairs(String id, RegistryObject<? extends Block> base, UnaryOperator<BlockBehaviour.Properties> props) {
        Reg.Handle<HbmStairBlock> block = REG.block(id, p -> new HbmStairBlock(base.get().defaultBlockState(), p), props);
        REG.blockItem(block, Compat::blockDescription).tab(HbmTab.BLOCKS);
        return block;
    }

    private static <B extends Block> Reg.Handle<B> plain(String id, Function<BlockBehaviour.Properties, B> factory,
                                                         UnaryOperator<BlockBehaviour.Properties> props, HbmTab tab) {
        Reg.Handle<B> block = REG.block(id, factory, props);
        Reg.Handle<BlockItem> item = REG.blockItem(block, Compat::blockDescription);
        if (tab != null) item.tab(tab);
        return block;
    }

    @SafeVarargs
    private static <B extends Block> Reg.Handle<B> plain(String id, Function<BlockBehaviour.Properties, B> factory,
                                                         UnaryOperator<BlockBehaviour.Properties> props, HbmTab tab,
                                                         Supplier<Component>... lines) {
        Reg.Handle<B> block = REG.block(id, factory, props);
        Reg.Handle<DecoBlockItem> item = REG.item(id, (net.minecraft.world.item.Item.Properties p) -> new DecoBlockItem(block.get(), Compat.blockDescription(p), List.of(lines)));
        if (tab != null) item.tab(tab);
        return block;
    }

    /** The reinforced glass pane's multipart blockstate (post, sides, no-sides). */
    private static void glassPaneStates() {
        States.multipart(Constants.MOD_ID, "reinforced_glass_pane")
                .part(new Variant("hbm:block/reinforced_glass_pane_post"))
                .part(When.of("north", "true"), new Variant("hbm:block/reinforced_glass_pane_side"))
                .part(When.of("east", "true"), new Variant("hbm:block/reinforced_glass_pane_side", 0, 90, false))
                .part(When.of("south", "true"), new Variant("hbm:block/reinforced_glass_pane_side_alt"))
                .part(When.of("west", "true"), new Variant("hbm:block/reinforced_glass_pane_side_alt", 0, 90, false))
                .part(When.of("north", "false"), new Variant("hbm:block/reinforced_glass_pane_noside"))
                .part(When.of("east", "false"), new Variant("hbm:block/reinforced_glass_pane_noside_alt"))
                .part(When.of("south", "false"), new Variant("hbm:block/reinforced_glass_pane_noside_alt", 0, 90, false))
                .part(When.of("west", "false"), new Variant("hbm:block/reinforced_glass_pane_noside", 0, 270, false))
                .register();
    }

    /** The laminate pane's multipart blockstate. */
    private static void laminatePaneStates() {
        States.multipart(Constants.MOD_ID, "reinforced_laminate_pane")
                .part(new Variant("hbm:block/reinforced_laminate_pane_post"))
                .part(When.of("north", "true"), new Variant("hbm:block/reinforced_laminate_pane_side"))
                .part(When.of("east", "true"), new Variant("hbm:block/reinforced_laminate_pane_side", 0, 90, false))
                .part(When.of("south", "true"), new Variant("hbm:block/reinforced_laminate_pane_side_alt"))
                .part(When.of("west", "true"), new Variant("hbm:block/reinforced_laminate_pane_side_alt", 0, 90, false))
                .part(When.of("north", "false"), new Variant("hbm:block/reinforced_laminate_pane_noside"))
                .part(When.of("east", "false"), new Variant("hbm:block/reinforced_laminate_pane_noside_alt"))
                .part(When.of("south", "false"), new Variant("hbm:block/reinforced_laminate_pane_noside_alt", 0, 90, false))
                .part(When.of("west", "false"), new Variant("hbm:block/reinforced_laminate_pane_noside", 0, 270, false))
                .register();
    }

    public static void init() {
        // Registers flush in creation order: the grate/platemetal sound types used to be fields here, so
        // initialising this class created ModSounds' and then ModDecoSounds' registers. Keep that order.
        ModSounds.init();
        ModDecoSounds.init();
        ModDecoBlockEntities.init();
    }
}
