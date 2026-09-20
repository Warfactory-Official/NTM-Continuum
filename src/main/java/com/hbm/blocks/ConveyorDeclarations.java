package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Shape;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
import com.hbm.blocks.network.BlockCraneBase;
import com.hbm.blocks.network.CraneExtractor;
import com.hbm.blocks.network.CraneUnboxer;

/**
 * The conveyor family's models, blockstates, item definitions and loot, declared in Golem's Dejson vocabulary.
 * Runs in a plain JVM (the {@code dejsonVerify}/{@code dejsonGenerate} tasks), so nothing here touches a
 * Minecraft class; the crane face logic below is the 1.7 {@code BlockCraneBase.getIcon} on direction ordinals.
 *
 * <p>Hand-built geometry stays in authored files: the belt/bend/lift/chute element models, the four
 * {@code crane_cube_<rot>} parents (a cube whose top face carries the 1.7 {@code getRotationFromSide} turn)
 * and the router's tinted overlay cube.
 */
public final class ConveyorDeclarations {

    private static final String NS = "hbm";
    private static final String B = "hbm:blocks/";

    private static boolean declared;

    private ConveyorDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        belts();
        chuteAndLift();
        cranes();
        renderedMachines();
        items();
        drops();
    }

    // --- belts: authored element parents, one straight + two bends per type, turned by the travel facing ---

    private static void belts() {
        belt("conveyor", "conveyor");
        belt("conveyor_express", "conveyor_express");
        belt("conveyor_double", "conveyor_double");
        belt("conveyor_triple", "conveyor_triple");
    }

    private static void belt(String id, String texture) {
        Dejson.block(NS, id)
                .models(Models.group()
                        .and("", Models.parented("hbm:block/conveyor_belt",
                                Slots.of("top", B + texture, "side", B + "conveyor_side")))
                        .and("_left", Models.parented("hbm:block/conveyor_belt_bend",
                                Slots.of("top", B + texture + "_curve_left", "side", B + "conveyor_side")))
                        .and("_right", Models.parented("hbm:block/conveyor_belt_bend",
                                Slots.of("top", B + texture + "_curve_right", "side", B + "conveyor_side"))))
                .states(Variants.product(Variants.facing(),
                        Variants.select("bend", "straight", "", "left", "_left", "right", "_right")))
                .blockItemDirect()
                .register();
    }

    // --- the chute is an authored multipart file; the lift picks one of three authored part models ---

    private static void chuteAndLift() {
        Dejson.block(NS, "conveyor_lift").statesOnly()
                .states(Variants.product(Variants.facing(),
                        Variants.select("part", "bottom", "_bottom", "middle", "_middle", "top", "_top")))
                .register();
    }

    // --- cranes: one cube model per (input, output) pair, faces chosen exactly as 1.7's getIcon did ---

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
    private static CraneSkin standard(String prefix, String in, String sideIn, String out, String sideOut) {
        return new CraneSkin(B + "crane_top", B + "crane_side", B + in, B + sideIn, B + out, B + sideOut,
                B + prefix + "_top", B + prefix + "_side_up", B + prefix + "_side_down",
                B + prefix + "_top_left", B + prefix + "_top_right",
                B + prefix + "_side_left_turn_up", B + prefix + "_side_right_turn_up",
                B + prefix + "_side_left_turn_down", B + prefix + "_side_right_turn_down",
                B + prefix + "_side_up_turn_left", B + prefix + "_side_up_turn_right",
                B + prefix + "_side_down_turn_left", B + prefix + "_side_down_turn_right", false);
    }

    /** Reversed-pattern cranes (extractor, unboxer): 1.7 registers the up/down and left/right sheets swapped. */
    private static CraneSkin reversed(String prefix, String in, String sideIn, String out, String sideOut) {
        return new CraneSkin(B + "crane_top", B + "crane_side", B + in, B + sideIn, B + out, B + sideOut,
                B + prefix + "_top", B + prefix + "_side_down", B + prefix + "_side_up",
                B + prefix + "_top_right", B + prefix + "_top_left",
                B + prefix + "_side_up_turn_left", B + prefix + "_side_up_turn_right",
                B + prefix + "_side_down_turn_left", B + prefix + "_side_down_turn_right",
                B + prefix + "_side_left_turn_up", B + prefix + "_side_right_turn_up",
                B + prefix + "_side_left_turn_down", B + prefix + "_side_right_turn_down", true);
    }

    private static void cranes() {
        crane("crane_inserter", standard("crane_in", "crane_in", "crane_side_in", "crane_out", "crane_side_out"));
        crane("crane_extractor", reversed("crane_out", "crane_in", "crane_side_in", "crane_out", "crane_side_out"));
        crane("crane_grabber", standard("crane_grabber", "crane_pull", "crane_side_pull", "crane_out", "crane_side_out"));
        crane("crane_boxer", standard("crane_boxer", "crane_in", "crane_side_in", "crane_box", "crane_side_box"));
        crane("crane_unboxer", reversed("crane_unboxer", "crane_in", "crane_side_in", "crane_box", "crane_side_box"));
    }

    private static void crane(String id, CraneSkin skin) {
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
        Dejson.block(NS, id)
                .models(group)
                .states(Variants.product(Variants.select("input", inputValues), Variants.select("output", outputValues)))
                .blockItemDirect()
                .register();
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

    // --- the router (authored tinted cube), and the block-entity-rendered machines ---

    private static void renderedMachines() {
        Dejson.block(NS, "crane_router").statesOnly().states(Variants.simple()).register();
        Dejson.block(NS, "crane_splitter").rendered("hbm:block/machine_dummy_empty")
                .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                        Variants.uniform("cell", "false", "true"))).register();
        Dejson.block(NS, "crane_partitioner").rendered("hbm:block/machine_dummy_empty")
                .states(Variants.uniform("facing", "north", "east", "south", "west")).register();
        Dejson.block(NS, "piston_inserter").rendered("hbm:block/machine_dummy_empty")
                .states(Variants.uniform("facing", "down", "up", "north", "south", "west", "east")).register();
    }

    // --- items: the wands show their belt (1.7 ItemRenderBlock), the chute/router their authored block models ---

    private static void items() {
        Dejson.item(NS, "conveyor_wand_regular").shares("hbm:block/conveyor").register();
        Dejson.item(NS, "conveyor_wand_express").shares("hbm:block/conveyor_express").register();
        Dejson.item(NS, "conveyor_wand_double").shares("hbm:block/conveyor_double").register();
        Dejson.item(NS, "conveyor_wand_triple").shares("hbm:block/conveyor_triple").register();
        Dejson.item(NS, "conveyor_chute").shares("hbm:block/conveyor_chute_inventory").register();
        Dejson.item(NS, "conveyor_lift").shares("hbm:block/conveyor_lift_top").register();
        Dejson.item(NS, "crane_router").shares("hbm:block/crane_router").register();
        for (String id : new String[]{"upgrade_ejector_1", "upgrade_ejector_2", "upgrade_ejector_3",
                "upgrade_stack_1", "upgrade_stack_2", "upgrade_stack_3"}) {
            Dejson.item(NS, id).models(Models.generated("hbm:items/" + id)).register();
        }
    }

    // --- loot: belts drop their wand, everything else itself ---

    private static void drops() {
        Drops.only(NS, "conveyor", "hbm:conveyor_wand_regular");
        Drops.only(NS, "conveyor_express", "hbm:conveyor_wand_express");
        Drops.only(NS, "conveyor_double", "hbm:conveyor_wand_double");
        Drops.only(NS, "conveyor_triple", "hbm:conveyor_wand_triple");
        Drops.only(NS, "conveyor_chute", "hbm:conveyor_wand_regular");
        Drops.only(NS, "conveyor_lift", "hbm:conveyor_wand_regular");
        for (String id : new String[]{"crane_extractor", "crane_inserter", "crane_grabber", "crane_router",
                "crane_boxer", "crane_unboxer", "crane_splitter", "crane_partitioner", "piston_inserter"}) {
            Drops.self(NS, id);
        }
    }
}
