package com.hbm.client.render;

import com.hbm.blocks.network.PowerCableBox;
import dev.golem.core.render.Disguises;
import dev.golem.core.render.gem.Geometry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 1.7 {@code PowerCableBox} in the chunk mesh, as {@code RenderBoxDuct.renderWorldBlock} drew it: a straight run
 * along one axis is one long box, anything else a centre box plus an arm per connected side. Unlike the fluid box
 * the junction does not grow a pixel ({@code jLower} follows {@code lower}). Every face wears the icon the 1.7
 * {@code PowerCableBox.getIcon(world, x, y, z, side)} chose: straight, the size's end plate, one of four curves,
 * or the single junction plate.
 *
 * <p>The per-face UV rotations of the 1.7 renderer are {@link BoxDuctModel}'s ({@code JOINT_TB}: a vertical
 * bend with an X arm; {@code JOINT_SIDES}: a horizontal bend; {@code JOINT}: every other joint, unturned).
 */
public final class PowerCableBoxModel implements Disguises.Look {

    private record Key(int size, int shape, int mask) {}

    private static final int SHAPES = 6;
    private static final int STRAIGHT_X = 0, STRAIGHT_Y = 1, STRAIGHT_Z = 2, JOINT = 3, JOINT_TB = 4, JOINT_SIDES = 5;
    private static final int STRAIGHT = 0, CURVE_TL = 1, CURVE_TR = 2, CURVE_BL = 3, CURVE_BR = 4, JUNCTION = 5, END_0 = 6;
    private static final String[] ICONS = {"straight", "curve_tl", "curve_tr", "curve_bl", "curve_br", "junction",
            "end_0", "end_1", "end_2", "end_3", "end_4"};

    /** [size][shape][box 0 = core, 1..6 = arm toward Direction ordinal][face 0..5]. */
    private static final Geometry[][][][] GEO = new Geometry[5][SHAPES][7][6];

    static {
        for (int size = 0; size < 5; size++) {
            float l = 0.125F + size * 0.0625F, u = 1F - l;
            for (int shape = 0; shape < SHAPES; shape++) {
                float[] core = switch (shape) {
                    case STRAIGHT_X -> new float[]{0F, l, l, 1F, u, u};
                    case STRAIGHT_Y -> new float[]{l, 0F, l, u, 1F, u};
                    case STRAIGHT_Z -> new float[]{l, l, 0F, u, u, 1F};
                    default -> new float[]{l, l, l, u, u, u};
                };
                int[] rot = switch (shape) {
                    case STRAIGHT_X -> BoxDuctModel.ROT_STRAIGHT_X;
                    case STRAIGHT_Z -> BoxDuctModel.ROT_STRAIGHT_Z;
                    case JOINT_TB -> BoxDuctModel.ROT_CURVE_TB;
                    case JOINT_SIDES -> BoxDuctModel.ROT_CURVE_SIDES;
                    default -> BoxDuctModel.ROT_NONE;
                };
                GEO[size][shape][0] = BoxDuctModel.box("pcore_" + size + "_" + shape, core, rot);
                float[][] arms = {
                        {l, 0F, l, u, l, u},   // DOWN
                        {l, u, l, u, 1F, u},   // UP
                        {l, l, 0F, u, u, l},   // NORTH
                        {l, l, u, u, u, 1F},   // SOUTH
                        {0F, l, l, l, u, u},   // WEST
                        {u, l, l, 1F, u, u},   // EAST
                };
                for (int arm = 0; arm < 6; arm++) {
                    GEO[size][shape][arm + 1] = BoxDuctModel.box("parm_" + size + "_" + shape + "_" + arm, arms[arm], rot);
                }
            }
        }
    }

    @Override
    public Object meshKey(BlockGetter level, BlockPos pos, BlockState state) {
        int size = state.getBlock() instanceof PowerCableBox box ? box.size : 0;
        int mask = state.getBlock() instanceof PowerCableBox ? PowerCableBox.connectionMask(state) : 0;
        return new Key(size, shape(mask), mask);
    }

    private static int shape(int mask) {
        if ((mask & 0b001111) == 0 && mask > 0) return STRAIGHT_X;
        if ((mask & 0b111100) == 0 && mask > 0) return STRAIGHT_Z;
        if ((mask & 0b110011) == 0 && mask > 0) return STRAIGHT_Y;
        if (Integer.bitCount(mask) == 2) {
            boolean anyY = (mask & 0b001100) != 0, anyX = (mask & 0b110000) != 0;
            // 1.7 RenderBoxDuct: (nY || pY) && (pX || nX) turns top and bottom; !nY && !pY turns the four sides.
            return anyY && anyX ? JOINT_TB : !anyY ? JOINT_SIDES : JOINT;
        }
        return JOINT;
    }

    @Override
    public void mesh(Object key, Disguises.Meshes out) {
        Key k = (Key) key;
        for (int box = 0; box < 7; box++) {
            if (box > 0 && (k.shape() < JOINT || (k.mask() & bit(box - 1)) == 0)) continue;
            for (int face = 0; face < 6; face++) {
                out.add(GEO[k.size()][k.shape()][box][face], "hbm:blocks/boxduct_cable_" + ICONS[icon(k, face)], -1);
            }
        }
    }

    @Override
    public boolean ambientOcclusion() {
        return false;
    }

    /** The 1.7 mask bit of a Direction ordinal: pX 32, nX 16, pY 8, nY 4, pZ 2, nZ 1. */
    private static int bit(int direction) {
        return switch (direction) {
            case 0 -> 4;   // DOWN  = nY
            case 1 -> 8;   // UP    = pY
            case 2 -> 1;   // NORTH = nZ
            case 3 -> 2;   // SOUTH = pZ
            case 4 -> 16;  // WEST  = nX
            default -> 32; // EAST  = pX
        };
    }

    /** The 1.7 {@code PowerCableBox.getIcon(world, x, y, z, side)}, verbatim, as an icon index. */
    private static int icon(Key s, int side) {
        int mask = s.mask(), count = Integer.bitCount(mask);
        int end = END_0 + s.size();
        boolean pX = (mask & 32) != 0, nX = (mask & 16) != 0, pY = (mask & 8) != 0, nY = (mask & 4) != 0, pZ = (mask & 2) != 0, nZ = (mask & 1) != 0;
        if ((mask & 0b001111) == 0 && mask > 0) return (side == 4 || side == 5) ? end : STRAIGHT;
        if ((mask & 0b111100) == 0 && mask > 0) return (side == 2 || side == 3) ? end : STRAIGHT;
        if ((mask & 0b110011) == 0 && mask > 0) return (side == 0 || side == 1) ? end : STRAIGHT;
        if (side == 0 && nY || side == 1 && pY || side == 2 && nZ || side == 3 && pZ || side == 4 && nX || side == 5 && pX) return end;
        if (count == 2) {
            if (side == 1 && nY || side == 0 && pY || side == 3 && nZ || side == 2 && pZ || side == 5 && nX || side == 4 && pX) return STRAIGHT;
            if (nY && pZ) return side == 4 ? CURVE_BR : CURVE_BL;
            if (nY && nZ) return side == 5 ? CURVE_BR : CURVE_BL;
            if (nY && pX) return side == 3 ? CURVE_BR : CURVE_BL;
            if (nY && nX) return side == 2 ? CURVE_BR : CURVE_BL;
            if (pY && pZ) return side == 4 ? CURVE_TR : CURVE_TL;
            if (pY && nZ) return side == 5 ? CURVE_TR : CURVE_TL;
            if (pY && pX) return side == 3 ? CURVE_TR : CURVE_TL;
            if (pY && nX) return side == 2 ? CURVE_TR : CURVE_TL;
            if (pX && nZ) return CURVE_TR;
            if (pX && pZ) return CURVE_BR;
            if (nX && nZ) return CURVE_TL;
            if (nX && pZ) return CURVE_BL;
        }
        return JUNCTION;
    }
}
