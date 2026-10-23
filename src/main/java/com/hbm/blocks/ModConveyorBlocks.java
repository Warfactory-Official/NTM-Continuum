package com.hbm.blocks;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Shape;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.States;
import dev.golem.core.dejson.Variant;
import dev.golem.core.dejson.Variants;
import dev.golem.core.dejson.When;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.Item;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.UnaryOperator;
import com.hbm.blocks.machine.PistonInserter;
import com.hbm.blocks.network.BlockConveyor;
import com.hbm.blocks.network.BlockConveyorChute;
import com.hbm.blocks.network.BlockConveyorDouble;
import com.hbm.blocks.network.BlockConveyorExpress;
import com.hbm.blocks.network.BlockConveyorLift;
import com.hbm.blocks.network.BlockConveyorTriple;
import com.hbm.blocks.network.CraneBoxer;
import com.hbm.blocks.network.CraneExtractor;
import com.hbm.blocks.network.CraneGrabber;
import com.hbm.blocks.network.CraneInserter;
import com.hbm.blocks.network.CranePartitioner;
import com.hbm.blocks.network.CraneRouter;
import com.hbm.blocks.network.CraneSplitter;
import com.hbm.blocks.network.CraneUnboxer;

/**
 * Feature-local block and block-item registry for the conveyor belt and crane family (1.7 {@code ModBlocks} L1894-1909),
 * with each block's models, blockstate, loot and mining tag stated on its registration (Reg; emitted by Dejson at
 * build time). The crane faces follow the 1.7 {@code BlockCraneBase.getIcon} on direction ordinals ({@link CraneModels}).
 *
 * <p>Hand-built geometry stays in authored files: the belt/bend/lift/chute element models, the four
 * {@code crane_cube_<rot>} parents (a cube whose top face carries the 1.7 {@code getRotationFromSide} turn)
 * and the router's tinted overlay cube.
 */
public final class ModConveyorBlocks {

    private ModConveyorBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    private static final String B = "hbm:blocks/";
    /** Everything but the belts drops itself; all are pickaxe-mined. */
    private static final Preset SELF_PICKAXE = handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE);

    private static final UnaryOperator<BlockBehaviour.Properties> BELT =
            p -> p.strength(2.0F, 1.2F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion();
    private static final UnaryOperator<BlockBehaviour.Properties> CRANE =
            p -> p.strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    private static final UnaryOperator<BlockBehaviour.Properties> CRANE_OPEN =
            p -> p.strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion();

    // belts: no creative tab in 1.7, the wand is their item
    public static final RegistryObject<BlockConveyor> CONVEYOR = REG.block("conveyor", BlockConveyor::new, BELT)
            .transform(belt("conveyor", "hbm:conveyor_wand_regular"));
    public static final RegistryObject<BlockConveyorExpress> CONVEYOR_EXPRESS = REG.block("conveyor_express", BlockConveyorExpress::new, BELT)
            .transform(belt("conveyor_express", "hbm:conveyor_wand_express"));
    public static final RegistryObject<BlockConveyorDouble> CONVEYOR_DOUBLE = REG.block("conveyor_double", BlockConveyorDouble::new, BELT)
            .transform(belt("conveyor_double", "hbm:conveyor_wand_double"));
    public static final RegistryObject<BlockConveyorTriple> CONVEYOR_TRIPLE = REG.block("conveyor_triple", BlockConveyorTriple::new, BELT)
            .transform(belt("conveyor_triple", "hbm:conveyor_wand_triple"));
    public static final RegistryObject<BlockConveyorChute> CONVEYOR_CHUTE = REG.block("conveyor_chute", BlockConveyorChute::new, BELT)
            .transform(multipart(file -> file
                        .part(When.of("feeds", "false", "facing", "north"), new Variant("hbm:block/conveyor_chute_belt"))
                        .part(When.of("feeds", "false", "facing", "east"), new Variant("hbm:block/conveyor_chute_belt", 0, 90, false))
                        .part(When.of("feeds", "false", "facing", "south"), new Variant("hbm:block/conveyor_chute_belt", 0, 180, false))
                        .part(When.of("feeds", "false", "facing", "west"), new Variant("hbm:block/conveyor_chute_belt", 0, 270, false))
                        .part(When.of("feeds", "true", "north", "true"), new Variant("hbm:block/conveyor_chute_stub_north"))
                        .part(When.of("feeds", "true", "south", "true"), new Variant("hbm:block/conveyor_chute_stub_south"))
                        .part(When.of("feeds", "true", "west", "true"), new Variant("hbm:block/conveyor_chute_stub_west"))
                        .part(When.of("feeds", "true", "east", "true"), new Variant("hbm:block/conveyor_chute_stub_east"))
                        .part(new Variant("hbm:block/conveyor_chute_frame"))
                        .part(When.of("north", "false", "feeds", "true"), new Variant("hbm:block/conveyor_chute_grate"))
                        .part(When.of("north", "false", "feeds", "false", "facing", "east|south|west"), new Variant("hbm:block/conveyor_chute_grate_short"))
                        .part(When.of("east", "false", "feeds", "true"), new Variant("hbm:block/conveyor_chute_grate", 0, 90, false))
                        .part(When.of("east", "false", "feeds", "false", "facing", "north|south|west"), new Variant("hbm:block/conveyor_chute_grate_short", 0, 90, false))
                        .part(When.of("south", "false", "feeds", "true"), new Variant("hbm:block/conveyor_chute_grate", 0, 180, false))
                        .part(When.of("south", "false", "feeds", "false", "facing", "north|east|west"), new Variant("hbm:block/conveyor_chute_grate_short", 0, 180, false))
                        .part(When.of("west", "false", "feeds", "true"), new Variant("hbm:block/conveyor_chute_grate", 0, 270, false))
                        .part(When.of("west", "false", "feeds", "false", "facing", "north|east|south"), new Variant("hbm:block/conveyor_chute_grate_short", 0, 270, false))))
            .loot(Loot.only("hbm:conveyor_wand_regular"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockConveyorLift> CONVEYOR_LIFT = REG.block("conveyor_lift", BlockConveyorLift::new, BELT)
            .transform(Presets.statesOnly(Variants.product(Variants.facing(),
                    Variants.select("part", "bottom", "_bottom", "middle", "_middle", "top", "_top"))))
            .loot(Loot.only("hbm:conveyor_wand_regular"))
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<CraneExtractor> CRANE_EXTRACTOR = REG.block("crane_extractor", CraneExtractor::new, CRANE)
            .transform(CraneModels.crane(CraneModels.reversed("crane_out", "crane_in", "crane_side_in", "crane_out", "crane_side_out")).and(SELF_PICKAXE));
    public static final RegistryObject<CraneInserter> CRANE_INSERTER = REG.block("crane_inserter", CraneInserter::new, CRANE)
            .transform(CraneModels.crane(CraneModels.standard("crane_in", "crane_in", "crane_side_in", "crane_out", "crane_side_out")).and(SELF_PICKAXE));
    public static final RegistryObject<CraneGrabber> CRANE_GRABBER = REG.block("crane_grabber", CraneGrabber::new, CRANE)
            .transform(CraneModels.crane(CraneModels.standard("crane_grabber", "crane_pull", "crane_side_pull", "crane_out", "crane_side_out")).and(SELF_PICKAXE));
    public static final RegistryObject<CraneRouter> CRANE_ROUTER = REG.block("crane_router", CraneRouter::new, CRANE)
            .transform(Presets.statesOnly(Variants.simple()).and(SELF_PICKAXE));
    public static final RegistryObject<CraneBoxer> CRANE_BOXER = REG.block("crane_boxer", CraneBoxer::new, CRANE)
            .transform(CraneModels.crane(CraneModels.standard("crane_boxer", "crane_in", "crane_side_in", "crane_box", "crane_side_box")).and(SELF_PICKAXE));
    public static final RegistryObject<CraneUnboxer> CRANE_UNBOXER = REG.block("crane_unboxer", CraneUnboxer::new, CRANE)
            .transform(CraneModels.crane(CraneModels.reversed("crane_unboxer", "crane_in", "crane_side_in", "crane_box", "crane_side_box")).and(SELF_PICKAXE));
    public static final RegistryObject<CraneSplitter> CRANE_SPLITTER = REG.block("crane_splitter", CraneSplitter::new, CRANE_OPEN)
            .transform(Presets.rendered(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                    Variants.uniform("cell", "false", "true"))).and(SELF_PICKAXE));
    public static final RegistryObject<CranePartitioner> CRANE_PARTITIONER = REG.block("crane_partitioner", CranePartitioner::new, CRANE_OPEN)
            .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")).and(SELF_PICKAXE));
    public static final RegistryObject<PistonInserter> PISTON_INSERTER = REG.block("piston_inserter", PistonInserter::new, CRANE_OPEN)
            .transform(Presets.rendered(Variants.uniform("facing", "down", "up", "north", "south", "west", "east")).and(SELF_PICKAXE));

    public static final RegistryObject<BlockItem> CONVEYOR_ITEM = item("conveyor", CONVEYOR);
    public static final RegistryObject<BlockItem> CONVEYOR_EXPRESS_ITEM = item("conveyor_express", CONVEYOR_EXPRESS);
    public static final RegistryObject<BlockItem> CONVEYOR_DOUBLE_ITEM = item("conveyor_double", CONVEYOR_DOUBLE);
    public static final RegistryObject<BlockItem> CONVEYOR_TRIPLE_ITEM = item("conveyor_triple", CONVEYOR_TRIPLE);
    public static final RegistryObject<BlockItem> CONVEYOR_CHUTE_ITEM = item("conveyor_chute", CONVEYOR_CHUTE)
            .transform(Presets.shares("hbm:block/conveyor_chute_inventory"));
    public static final RegistryObject<BlockItem> CONVEYOR_LIFT_ITEM = item("conveyor_lift", CONVEYOR_LIFT)
            .transform(Presets.shares("hbm:block/conveyor_lift_top"));
    public static final RegistryObject<BlockItem> CRANE_EXTRACTOR_ITEM = item("crane_extractor", CRANE_EXTRACTOR)
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> CRANE_INSERTER_ITEM = item("crane_inserter", CRANE_INSERTER)
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> CRANE_GRABBER_ITEM = item("crane_grabber", CRANE_GRABBER)
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> CRANE_ROUTER_ITEM = item("crane_router", CRANE_ROUTER)
            .transform(Presets.shares("hbm:block/crane_router"))
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> CRANE_BOXER_ITEM = item("crane_boxer", CRANE_BOXER)
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> CRANE_UNBOXER_ITEM = item("crane_unboxer", CRANE_UNBOXER)
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> CRANE_SPLITTER_ITEM = item("crane_splitter", CRANE_SPLITTER)
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> CRANE_PARTITIONER_ITEM = item("crane_partitioner", CRANE_PARTITIONER)
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> PISTON_INSERTER_ITEM = item("piston_inserter", PISTON_INSERTER);

    // 1.7 machineTab for the cranes; belts and the piston inserter had no tab
    private static Reg.Handle<BlockItem> item(String name, RegistryObject<? extends Block> block) {
        return REG.<BlockItem>item(name, (Item.Properties properties) -> new CompatBlockItem(block.get(),
                Compat.blockDescription(properties)));
    }

    /**
     * A belt: authored element parents, one straight and two bends per type, turned by the travel facing; the
     * block item names the straight belt; the belt drops its wand.
     */
    private static Preset belt(String texture, String wand) {
        return handle -> handle
                .model(d -> d.models(Models.group()
                                .and("", Models.parented("hbm:block/conveyor_belt",
                                        Slots.of("top", B + texture, "side", B + "conveyor_side")))
                                .and("_left", Models.parented("hbm:block/conveyor_belt_bend",
                                        Slots.of("top", B + texture + "_curve_left", "side", B + "conveyor_side")))
                                .and("_right", Models.parented("hbm:block/conveyor_belt_bend",
                                        Slots.of("top", B + texture + "_curve_right", "side", B + "conveyor_side"))))
                        .states(Variants.product(Variants.facing(),
                                Variants.select("bend", "straight", "", "left", "_left", "right", "_right")))
                        .blockItemDirect())
                .loot(Loot.only(wand))
                .tag(BlockTag.PICKAXE);
    }

    /** A multipart blockstate built part by part (the chute's authored arms). */
    private static Preset multipart(UnaryOperator<States.MultipartFile> parts) {
        return handle -> parts.apply(States.multipart(handle.namespace(), handle.path())).register();
    }

    /** The cranes' faces: one cube model per (input, output) pair, chosen exactly as 1.7's getIcon did. */
    static final class CraneModels {

        private CraneModels() {}

        /** The 1.7 icon fields, in {@code BlockCraneBase} order. */
        private record CraneSkin(String top, String side, String in, String sideIn, String out, String sideOut,
                                 String directional, String dirUp, String dirDown, String turnLeft, String turnRight,
                                 String sideLeftTurnUp, String sideRightTurnUp, String sideLeftTurnDown, String sideRightTurnDown,
                                 String sideUpTurnLeft, String sideUpTurnRight, String sideDownTurnLeft, String sideDownTurnRight,
                                 boolean turnedTop) {}

        private static final String[] DIR = {"down", "up", "north", "south", "west", "east"};

        /** 1.7 ForgeDirection.ROTATION_MATRIX: {@code getRotation(axis)} is {@code MATRIX[axis][dir]}. */
        private static final int[][] ROTATION_MATRIX = {
                {0, 1, 4, 5, 3, 2, 6},
                {0, 1, 5, 4, 2, 3, 6},
                {5, 4, 2, 3, 0, 1, 6},
                {4, 5, 2, 3, 1, 0, 6},
                {2, 3, 1, 0, 4, 5, 6},
                {3, 2, 0, 1, 4, 5, 6},
                {0, 1, 2, 3, 4, 5, 6},
        };

        /** 1.7 {@code RenderBlocks.uvRotateTop} values to a JSON face rotation, as the belt renderer fixes them. */
        private static final int[] TOP_ROTATION = {0, 90, 270, 180};

        private static int opposite(int dir) {
            return dir ^ 1;
        }

        /** Standard-pattern cranes (inserter, grabber, boxer): the directional sheets read straight. */
        static CraneSkin standard(String prefix, String in, String sideIn, String out, String sideOut) {
            return new CraneSkin(B + "crane_top", B + "crane_side", B + in, B + sideIn, B + out, B + sideOut,
                    B + prefix + "_top", B + prefix + "_side_up", B + prefix + "_side_down",
                    B + prefix + "_top_left", B + prefix + "_top_right",
                    B + prefix + "_side_left_turn_up", B + prefix + "_side_right_turn_up",
                    B + prefix + "_side_left_turn_down", B + prefix + "_side_right_turn_down",
                    B + prefix + "_side_up_turn_left", B + prefix + "_side_up_turn_right",
                    B + prefix + "_side_down_turn_left", B + prefix + "_side_down_turn_right", false);
        }

        /** Reversed-pattern cranes (extractor, unboxer): 1.7 registers the up/down and left/right sheets swapped. */
        static CraneSkin reversed(String prefix, String in, String sideIn, String out, String sideOut) {
            return new CraneSkin(B + "crane_top", B + "crane_side", B + in, B + sideIn, B + out, B + sideOut,
                    B + prefix + "_top", B + prefix + "_side_down", B + prefix + "_side_up",
                    B + prefix + "_top_right", B + prefix + "_top_left",
                    B + prefix + "_side_up_turn_left", B + prefix + "_side_up_turn_right",
                    B + prefix + "_side_down_turn_left", B + prefix + "_side_down_turn_right",
                    B + prefix + "_side_left_turn_up", B + prefix + "_side_right_turn_up",
                    B + prefix + "_side_left_turn_down", B + prefix + "_side_right_turn_down", true);
        }

        /** One cube model per (input, output) pair plus the inventory cube; the block item names the block model. */
        static Preset crane(CraneSkin skin) {
            return handle -> handle.model(d -> crane(d, skin));
        }

        private static Dejson.Declaration crane(Dejson.Declaration declaration, CraneSkin skin) {
            Models.Group group = Models.group().and("", inventoryCube(skin));
            String[] inputValues = new String[12];
            String[] outputValues = new String[12];
            for (int i = 0; i < 6; i++) {
                inputValues[i * 2] = DIR[i];
                inputValues[i * 2 + 1] = "_" + DIR[i];
                outputValues[i * 2] = DIR[i];
                outputValues[i * 2 + 1] = "_" + DIR[i];
            }
            for (int in = 0; in < 6; in++) {
                for (int out = 0; out < 6; out++) {
                    group = group.and("_" + DIR[in] + "_" + DIR[out], worldCube(skin, in, out));
                }
            }
            return declaration
                    .models(group)
                    .states(Variants.product(Variants.select("input", inputValues), Variants.select("output", outputValues)))
                    .blockItemDirect();
        }

        /** 1.7 {@code getIcon(side, meta)} "kept for inventory rendering", at damage 0: out below, in above, the up-sheet around. */
        private static Shape inventoryCube(CraneSkin skin) {
            return Models.parented("hbm:block/crane_cube_0", Slots.of("down", skin.out(), "up", skin.in())
                    .and("north", skin.dirUp()).and("south", skin.dirUp())
                    .and("west", skin.dirUp()).and("east", skin.dirUp())
                    .and("side", skin.side()));
        }

        /** 1.7 {@code getIcon(world, x, y, z, side)} for every face, plus the top sheet's {@code getRotationFromSide}. */
        private static Shape worldCube(CraneSkin skin, int in, int out) {
            boolean overridden = out != opposite(in);
            // take your left hand, make your thumb the input side and the index finger the output side
            // angle your middle finger to make your hand look like coordinate axes
            // the direction your middle finger is pointing towards will be the direction returned from this function
            int leftHand = ROTATION_MATRIX[in][out];

            Slots slots = Slots.of("side", skin.side());
            for (int side = 0; side < 6; side++) {
                slots = slots.and(DIR[side], face(skin, in, out, overridden, leftHand, side));
            }

            int rotation = skin.turnedTop() ? turnedTopRotation(in, out, leftHand) : topRotation(in);
            return Models.parented("hbm:block/crane_cube_" + TOP_ROTATION[rotation], slots);
        }

        private static String face(CraneSkin skin, int in, int out, boolean overridden, int leftHand, int side) {
            if (side == 0 || side == 1) {
                if (side == out) return skin.out();
                if (side == in) return skin.in();

                if (side == 1) {
                    if (overridden) {
                        if (leftHand == 1) return skin.turnLeft();
                        if (leftHand == 0) return skin.turnRight();
                    } else {
                        return skin.directional();
                    }
                }

                return skin.top();
            }

            if (side == out) return skin.sideOut();
            if (side == in) return skin.sideIn();

            if (overridden) {
                if (leftHand == side) {
                    if (out == 1) return skin.sideLeftTurnUp();
                    if (out == 0) return skin.sideRightTurnDown();
                    if (in == 1) return skin.sideUpTurnRight();
                    if (in == 0) return skin.sideDownTurnLeft();
                }
                if (opposite(leftHand) == side) {
                    if (out == 1) return skin.sideRightTurnUp();
                    if (out == 0) return skin.sideLeftTurnDown();
                    if (in == 1) return skin.sideUpTurnLeft();
                    if (in == 0) return skin.sideDownTurnRight();
                }
            } else {
                if (out == 1) return skin.dirUp();
                if (out == 0) return skin.dirDown();
            }

            return skin.side();
        }

        /** 1.7 {@code BlockCraneBase.getRotationFromSide} at side 1. */
        private static int topRotation(int in) {
            return switch (in) {
                case 2 -> 3;
                case 3 -> 0;
                case 4 -> 1;
                case 5 -> 2;
                default -> 0;
            };
        }

        /** 1.7 {@code CraneExtractor.getRotationFromSide} (verbatim in {@code CraneUnboxer}): "it's 2:30 am, please just accept this". */
        private static int turnedTopRotation(int in, int out, int leftHand) {
            if (in <= 1) return 0;
            if (leftHand == 1) {
                return switch (in) {
                    case 2 -> 2;
                    case 3 -> 1;
                    case 4 -> 3;
                    default -> 0;
                };
            }
            if (leftHand == 0) {
                return switch (in) {
                    case 2 -> 1;
                    case 3 -> 2;
                    case 4 -> 0;
                    default -> 3;
                };
            }
            return switch (in) {
                case 2 -> 0;
                case 3 -> 3;
                case 4 -> 2;
                default -> 1;
            };
        }
    }

    public static void init() {}
}
