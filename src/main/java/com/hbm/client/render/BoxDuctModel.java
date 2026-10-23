package com.hbm.client.render;

import com.hbm.blocks.network.FluidDuctBox;
import dev.golem.core.render.Disguises;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 1.7 {@code FluidDuctBox}/{@code FluidDuctBoxExhaust} in the chunk mesh, as {@code RenderBoxDuct} drew it:
 * a straight run along one axis is a single long box; a two-way bend is a centre cube plus an arm
 * per connected side; anything else is a slightly larger junction cube plus arms. Every face of every
 * box wears the icon the 1.7 {@code getIcon(world, x, y, z, side)} chose for that side of the block
 * (straight / end / one of four curves / one of five junction plates), in the block's material
 * (silver, copper, white, exhaust) and size. The white duct takes tint index 0, the lightened fluid colour.
 *
 * <p>The 1.7 renderer's per-face UV rotations ({@code uvRotateTop} etc.) are reproduced: a straight run along X
 * or Z, a vertical bend with an X arm and a horizontal bend each turn some faces' icons so the straight icon runs
 * along the duct (without them every horizontal run showed its bands across the pipe). The
 * turned UVs are {@code RenderBlocks.renderFace*}'s for those flags, including its quirk of reflecting the
 * along-Y coordinate about the box's own centre ({@code minY + maxY - y}); see {@link #uv}.
 */
public final class BoxDuctModel implements Disguises.Look {

    private record Key(int size, int material, int shape, int mask) {}

    private static final int SHAPES = 7;
    /**
     * {@code CURVE} is a bend with no UV rotation (vertical, in the Z plane), {@code CURVE_TB} a vertical bend with an
     * X arm (1.7 {@code uvRotateTop = uvRotateBottom = 1}), {@code CURVE_SIDES} a horizontal bend (1.7
     * {@code uvRotateNorth = 1, South = 2, East = 2, West = 1}).
     */
    private static final int STRAIGHT_X = 0, STRAIGHT_Y = 1, STRAIGHT_Z = 2, CURVE = 3, JUNCTION = 4,
            CURVE_TB = 5, CURVE_SIDES = 6;
    // Declared before the static block that bakes the geometry (static initialisers run in source order).
    /** {@code RenderBoxDuct}'s flags, shared with {@link PowerCableBoxModel} (the same 1.7 renderer). */
    static final int[] ROT_NONE = new int[6], ROT_STRAIGHT_X = {1, 1, 2, 1, 0, 0}, ROT_STRAIGHT_Z = {0, 0, 0, 0, 1, 2},
            ROT_CURVE_TB = {1, 1, 0, 0, 0, 0}, ROT_CURVE_SIDES = {0, 0, 2, 1, 1, 2};
    private static final int STRAIGHT = 0, END = 1, CURVE_TL = 2, CURVE_TR = 3, CURVE_BL = 4, CURVE_BR = 5, JUNCTION_0 = 6;
    private static final String[] ICONS = {"straight", "end", "curve_tl", "curve_tr", "curve_bl", "curve_br",
            "junction_0", "junction_1", "junction_2", "junction_3", "junction_4"};
    private static final String[] MATERIALS = {"silver", "copper", "white", "exhaust"};

    /** [size][shape][box 0 = core, 1..6 = arm toward Direction ordinal][face 0..5]. */
    private static final Geometry[][][][] GEO = new Geometry[5][SHAPES][7][6];

    static {
        for (int size = 0; size < 5; size++) {
            float l = 0.125F + size * 0.0625F, u = 1F - l;
            float j = 0.0625F + size * 0.0625F, jU = 1F - j;
            for (int shape = 0; shape < SHAPES; shape++) {
                float[] core = switch (shape) {
                    case STRAIGHT_X -> new float[]{0F, l, l, 1F, u, u};
                    case STRAIGHT_Y -> new float[]{l, 0F, l, u, 1F, u};
                    case STRAIGHT_Z -> new float[]{l, l, 0F, u, u, 1F};
                    case CURVE, CURVE_TB, CURVE_SIDES -> new float[]{l, l, l, u, u, u};
                    default -> new float[]{j, j, j, jU, jU, jU};
                };
                int[] rot = rotations(shape);
                GEO[size][shape][0] = box("core_" + size + "_" + shape, core, rot);
                float c = shape == JUNCTION ? j : l, cU = 1F - c;
                float[][] arms = {
                        {l, 0F, l, u, c, u},   // DOWN
                        {l, cU, l, u, 1F, u},  // UP
                        {l, l, 0F, u, u, c},   // NORTH
                        {l, l, cU, u, u, 1F},  // SOUTH
                        {0F, l, l, c, u, u},   // WEST
                        {cU, l, l, 1F, u, u},  // EAST
                };
                for (int arm = 0; arm < 6; arm++) {
                    GEO[size][shape][arm + 1] = box("arm_" + size + "_" + shape + "_" + arm, arms[arm], rot);
                }
            }
        }
    }

    /**
     * The 1.7 {@code uvRotate*} flag per face (Direction ordinal: down, up, north, south, west, east) that
     * {@code RenderBoxDuct.renderWorldBlock} set for a shape. {@code renderFaceZNeg} reads {@code uvRotateEast},
     * {@code ZPos} {@code uvRotateWest}, {@code XNeg} {@code uvRotateNorth}, {@code XPos} {@code uvRotateSouth}.
     */
    private static int[] rotations(int shape) {
        return switch (shape) {
            case STRAIGHT_X -> ROT_STRAIGHT_X;
            case STRAIGHT_Z -> ROT_STRAIGHT_Z;
            case CURVE_TB -> ROT_CURVE_TB;
            case CURVE_SIDES -> ROT_CURVE_SIDES;
            default -> ROT_NONE;
        };
    }


    /**
     * Six face quads of an axis-aligned box with the 1.7 render bounds' UVs on a 16x16 tile, each face turned by
     * its {@code uvRotate} flag. The vertex order is the one {@code RenderBlocks} emitted for each face.
     */
    static Geometry[] box(String name, float[] b, int[] rot) {
        float x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
        float[][][] corners = {
                {{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}}, // down
                {{x1, y1, z1}, {x1, y1, z0}, {x0, y1, z0}, {x0, y1, z1}}, // up
                {{x0, y1, z0}, {x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}}, // north
                {{x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}}, // south
                {{x0, y1, z1}, {x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}}, // west
                {{x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}}, // east
        };
        float[][] normals = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
        String[] suffix = {"_d", "_u", "_n", "_s", "_w", "_e"};
        Geometry[] faces = new Geometry[6];
        for (int f = 0; f < 6; f++) {
            Quads.Builder quad = Quads.named(name + suffix[f]).normal(normals[f][0], normals[f][1], normals[f][2]);
            for (float[] c : corners[f]) {
                float[] uv = uv(f, rot[f], c[0], c[1], c[2], y0, y1);
                quad.vertex(c[0], c[1], c[2], uv[0], uv[1]);
            }
            faces[f] = quad.build();
        }
        return faces;
    }

    /**
     * The 1.7 icon coordinate (0..1 of the tile) at a box corner. Unturned faces map the face's extent upright, as
     * {@code RenderBlocks} did (north and east through HBM's {@code RenderBlocksNT}, which unmirrored them). The turned
     * cases are the {@code renderFace*} branches for the flags {@link #rotations} sets: a quarter turn, with the
     * along-Y coordinate taken as {@code minY + maxY - y} (the branches swap the bounds, not the coordinate).
     */
    private static float[] uv(int face, int rot, float x, float y, float z, float minY, float maxY) {
        float flipY = minY + maxY - y;
        return switch (face) {
            case 0 -> rot == 1 ? new float[]{1 - z, x} : new float[]{x, z};          // renderFaceYNeg
            case 1 -> rot == 1 ? new float[]{z, 1 - x} : new float[]{x, z};          // renderFaceYPos
            case 2 -> rot == 2 ? new float[]{flipY, 1 - x} : new float[]{1 - x, 1 - y}; // renderFaceZNeg (NT)
            case 3 -> rot == 1 ? new float[]{flipY, 1 - x} : new float[]{x, 1 - y};      // renderFaceZPos
            case 4 -> rot == 1 ? new float[]{flipY, 1 - z} : new float[]{z, 1 - y};      // renderFaceXNeg
            default -> rot == 2 ? new float[]{flipY, 1 - z} : new float[]{1 - z, 1 - y}; // renderFaceXPos (NT)
        };
    }

    @Override
    public Object meshKey(BlockGetter level, BlockPos pos, BlockState state) {
        FluidDuctBox block = state.getBlock() instanceof FluidDuctBox b ? b : null;
        int size = block == null ? 0 : block.size;
        int material = block == null || block.material < 0 ? 3 : block.material;
        int mask = FluidDuctBox.connectionMask(state);
        return new Key(size, material, shape(mask), mask);
    }

    /** {@code RenderBoxDuct.renderWorldBlock}'s shape for a connection mask. */
    private static int shape(int mask) {
        if ((mask & 0b001111) == 0 && mask > 0) return STRAIGHT_X;
        if ((mask & 0b111100) == 0 && mask > 0) return STRAIGHT_Z;
        if ((mask & 0b110011) == 0 && mask > 0) return STRAIGHT_Y;
        if (Integer.bitCount(mask) == 2) {
            boolean anyY = (mask & 0b001100) != 0, anyX = (mask & 0b110000) != 0;
            // 1.7: (nY || pY) && (pX || nX) turns top and bottom; !nY && !pY turns the four sides.
            return anyY && anyX ? CURVE_TB : !anyY ? CURVE_SIDES : CURVE;
        }
        return JUNCTION;
    }

    @Override
    public void mesh(Object key, Disguises.Meshes out) {
        Key k = (Key) key;
        // 1.7: the white duct takes ColorUtil.lightenColor(type colour, 0.25), the block tint; the others are untinted
        int tint = k.material() == 2 ? 0 : -1;
        for (int box = 0; box < 7; box++) {
            if (box > 0 && (k.shape() < CURVE || (k.mask() & bit(box - 1)) == 0)) continue;
            for (int face = 0; face < 6; face++) {
                out.add(GEO[k.size()][k.shape()][box][face],
                        "hbm:blocks/boxduct_" + MATERIALS[k.material()] + "_" + ICONS[icon(k, face)], tint);
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

    /** The 1.7 {@code FluidDuctBox.getIcon(world, x, y, z, side)} as an icon index. */
    private static int icon(Key s, int side) {
        int mask = s.mask(), count = Integer.bitCount(mask);
        boolean pX = (mask & 32) != 0, nX = (mask & 16) != 0, pY = (mask & 8) != 0, nY = (mask & 4) != 0, pZ = (mask & 2) != 0, nZ = (mask & 1) != 0;
        if ((mask & 0b001111) == 0 && mask > 0) return (side == 4 || side == 5) ? END : STRAIGHT;
        if ((mask & 0b111100) == 0 && mask > 0) return (side == 2 || side == 3) ? END : STRAIGHT;
        if ((mask & 0b110011) == 0 && mask > 0) return (side == 0 || side == 1) ? END : STRAIGHT;
        if (count == 2) {
            if (side == 0 && nY || side == 1 && pY || side == 2 && nZ || side == 3 && pZ || side == 4 && nX || side == 5 && pX) return END;
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
            return JUNCTION_0 + s.size();
        }
        return JUNCTION_0 + s.size();
    }
}
