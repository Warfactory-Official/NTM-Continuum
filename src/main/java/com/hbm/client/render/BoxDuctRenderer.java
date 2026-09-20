package com.hbm.client.render;

import com.hbm.blocks.network.FluidDuctBox;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.hbm.blocks.network.FluidDuctBoxExhaust;

/**
 * Draws the 1.7 {@code FluidDuctBox}/{@code FluidDuctBoxExhaust} the way {@code RenderBoxDuct} did:
 * a straight run along one axis is a single long box; a two-way bend is a centre cube plus an arm
 * per connected side; anything else is a slightly larger junction cube plus arms. Every face of every
 * box wears the icon the 1.7 {@code getIcon(world, x, y, z, side)} chose for that side of the block
 * (straight / end / one of four curves / one of five junction plates), in the block's material
 * (silver, copper, white, exhaust) and size. The white duct is tinted with the lightened fluid colour.
 *
 * <p>Not reproduced: the 1.7 renderer's per-face UV rotations ({@code uvRotateTop} etc.), which turned
 * the straight/curve icons to run along the duct; faces here are mapped upright.
 */
public class BoxDuctRenderer<T extends TileEntityFluidDuctNT> extends GemBER<T, BoxDuctRenderer.State> {

    public static class State extends CompatRenderState {
        public int size;
        public int material;
        public int shape;
        public int mask;
        public int count;
        public int color = 0xFFFFFFFF;
    }

    private static final int SHAPES = 5;
    private static final int STRAIGHT_X = 0, STRAIGHT_Y = 1, STRAIGHT_Z = 2, CURVE = 3, JUNCTION = 4;
    private static final int STRAIGHT = 0, END = 1, CURVE_TL = 2, CURVE_TR = 3, CURVE_BL = 4, CURVE_BR = 5, JUNCTION_0 = 6;
    private static final String[] ICONS = {"straight", "end", "curve_tl", "curve_tr", "curve_bl", "curve_br",
            "junction_0", "junction_1", "junction_2", "junction_3", "junction_4"};
    private static final String[] MATERIALS = {"silver", "copper", "white", "exhaust"};

    /** [size][shape][box 0 = core, 1..6 = arm toward Direction ordinal][face 0..5]. */
    private static final Geometry[][][][] GEO = new Geometry[5][SHAPES][7][6];
    private static final Surface[][] SURF = new Surface[MATERIALS.length][ICONS.length];

    static {
        for (int size = 0; size < 5; size++) {
            float l = 0.125F + size * 0.0625F, u = 1F - l;
            float j = 0.0625F + size * 0.0625F, jU = 1F - j;
            for (int shape = 0; shape < SHAPES; shape++) {
                float[] core = switch (shape) {
                    case STRAIGHT_X -> new float[]{0F, l, l, 1F, u, u};
                    case STRAIGHT_Y -> new float[]{l, 0F, l, u, 1F, u};
                    case STRAIGHT_Z -> new float[]{l, l, 0F, u, u, 1F};
                    case CURVE -> new float[]{l, l, l, u, u, u};
                    default -> new float[]{j, j, j, jU, jU, jU};
                };
                GEO[size][shape][0] = box("core_" + size + "_" + shape, core);
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
                    GEO[size][shape][arm + 1] = box("arm_" + size + "_" + shape + "_" + arm, arms[arm]);
                }
            }
        }
        for (int m = 0; m < MATERIALS.length; m++) {
            for (int i = 0; i < ICONS.length; i++) {
                SURF[m][i] = Surface.cutoutNoCull("hbm:textures/blocks/boxduct_" + MATERIALS[m] + "_" + ICONS[i] + ".png");
            }
        }
    }

    /** Six face quads of an axis-aligned box, UV-mapped by the face's extent on a 16×16 tile (1.7 render bounds). */
    private static Geometry[] box(String name, float[] b) {
        float x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
        Geometry[] faces = new Geometry[6];
        faces[0] = Quads.named(name + "_d").normal(0, -1, 0)
                .vertex(x0, y0, z0, x0, z0).vertex(x1, y0, z0, x1, z0).vertex(x1, y0, z1, x1, z1).vertex(x0, y0, z1, x0, z1).build();
        faces[1] = Quads.named(name + "_u").normal(0, 1, 0)
                .vertex(x0, y1, z0, x0, z0).vertex(x0, y1, z1, x0, z1).vertex(x1, y1, z1, x1, z1).vertex(x1, y1, z0, x1, z0).build();
        faces[2] = Quads.named(name + "_n").normal(0, 0, -1)
                .vertex(x1, y1, z0, 1 - x1, 1 - y1).vertex(x1, y0, z0, 1 - x1, 1 - y0).vertex(x0, y0, z0, 1 - x0, 1 - y0).vertex(x0, y1, z0, 1 - x0, 1 - y1).build();
        faces[3] = Quads.named(name + "_s").normal(0, 0, 1)
                .vertex(x0, y1, z1, x0, 1 - y1).vertex(x0, y0, z1, x0, 1 - y0).vertex(x1, y0, z1, x1, 1 - y0).vertex(x1, y1, z1, x1, 1 - y1).build();
        faces[4] = Quads.named(name + "_w").normal(-1, 0, 0)
                .vertex(x0, y1, z0, z0, 1 - y1).vertex(x0, y0, z0, z0, 1 - y0).vertex(x0, y0, z1, z1, 1 - y0).vertex(x0, y1, z1, z1, 1 - y1).build();
        faces[5] = Quads.named(name + "_e").normal(1, 0, 0)
                .vertex(x1, y1, z1, 1 - z1, 1 - y1).vertex(x1, y0, z1, 1 - z1, 1 - y0).vertex(x1, y0, z0, 1 - z0, 1 - y0).vertex(x1, y1, z0, 1 - z0, 1 - y1).build();
        return faces;
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        for (int box = 0; box < 7; box++) {
            final int b = box;
            for (int face = 0; face < 6; face++) {
                final int f = face;
                declaration.part(GEO[0][JUNCTION][b][f], SURF[0][0])
                        .mesh(state -> GEO[state.size][state.shape][b][f])
                        .skin(state -> SURF[state.material][icon(state, f)])
                        .visibleIf(state -> b == 0 || (state.shape >= CURVE && (state.mask & bit(b - 1)) != 0))
                        .tint(state -> state.color);
            }
        }
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

    /** The 1.7 {@code FluidDuctBox.getIcon(world, x, y, z, side)}, verbatim, as an icon index. */
    private static int icon(State s, int side) {
        int mask = s.mask, count = s.count;
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
            return JUNCTION_0 + s.size;
        }
        return JUNCTION_0 + s.size;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(T be, State state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        FluidDuctBox block = bs.getBlock() instanceof FluidDuctBox b ? b : null;
        state.size = block == null ? 0 : block.size;
        state.material = block == null || block.material < 0 ? 3 : block.material;
        state.mask = FluidDuctBox.connectionMask(bs);
        state.count = Integer.bitCount(state.mask);
        int mask = state.mask;
        if ((mask & 0b001111) == 0 && mask > 0) state.shape = STRAIGHT_X;
        else if ((mask & 0b111100) == 0 && mask > 0) state.shape = STRAIGHT_Z;
        else if ((mask & 0b110011) == 0 && mask > 0) state.shape = STRAIGHT_Y;
        else if (state.count == 2) state.shape = CURVE;
        else state.shape = JUNCTION;
        // 1.7: the white duct takes ColorUtil.lightenColor(type colour, 0.25) — a quarter of the way to
        // white — for every type, NONE (0x888888) included.
        int color = 0xFFFFFF;
        if (state.material == 2) {
            int c = be.getFluidType().getColor();
            int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, bl = c & 0xFF;
            r = (int) (r + (255 - r) * 0.25D);
            g = (int) (g + (255 - g) * 0.25D);
            bl = (int) (bl + (255 - bl) * 0.25D);
            color = (r << 16) | (g << 8) | bl;
        }
        state.color = 0xFF000000 | color;
    }
}
