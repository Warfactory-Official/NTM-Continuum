package com.hbm.client.render;

import com.hbm.blocks.network.PneumoTube;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;
import dev.golem.core.render.Disguises;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code RenderPneumoTube.renderWorldBlock}, part for part, in the chunk mesh: a core cube whose faces vanish behind
 * arms, one arm per connected face, the seamless straight form along one axis when nothing else is attached, and on top
 * the nozzles, a collar in the tube's own sprite and a head in the in/out/connector sprite.
 */
public final class PneumoTubeModel implements Disguises.Look {

    private static final String BASE = "hbm:blocks/pneumatic_tube";
    private static final String STRAIGHT = "hbm:blocks/pneumatic_tube_straight";
    private static final String IN = "hbm:blocks/pneumatic_tube_in";
    private static final String OUT = "hbm:blocks/pneumatic_tube_out";
    private static final String CONNECTOR = "hbm:blocks/pneumatic_tube_connector";

    private static final double LOWER = 0.3125D;
    private static final double UPPER = 0.6875D;
    // 1.7 renderCon: c* collar, n* head
    private static final double C_LOWER = 0.25D;
    private static final double C_UPPER = 0.75D;

    private static final int X_STRAIGHT = TubeBoxes.mask(Direction.EAST, Direction.WEST);
    private static final int Y_STRAIGHT = TubeBoxes.mask(Direction.UP, Direction.DOWN);
    private static final int Z_STRAIGHT = TubeBoxes.mask(Direction.NORTH, Direction.SOUTH);

    /**
     * What the tube shows: {@code 1 << Direction.ordinal()} arm and air-intake masks, the insertion and ejection faces
     * (-1 for none) and whether any nozzle is attached (1.7: such a tube never takes the seamless straight form).
     */
    private record Key(int arms, int insertion, int ejection, int air, boolean hasConnections) {}

    @Override
    public Object meshKey(BlockGetter level, BlockPos pos, BlockState state) {
        int arms = state.getBlock() instanceof PneumoTube ? PneumoTube.faceMask(state) : 0;
        if (!(level.getBlockEntity(pos) instanceof TileEntityPneumoTube tube)) return new Key(arms, -1, -1, 0, false);
        return new Key(arms, tube.insertionDir == null ? -1 : tube.insertionDir.ordinal(),
                tube.ejectionDir == null ? -1 : tube.ejectionDir.ordinal(), tube.airMask & 0x3F,
                tube.isCompressor() || tube.isEndpoint());
    }

    @Override
    public void mesh(Object key, Disguises.Meshes out) {
        Key k = (Key) key;
        boolean straight = !k.hasConnections()
                && (k.arms() == X_STRAIGHT || k.arms() == Y_STRAIGHT || k.arms() == Z_STRAIGHT);
        if (straight) {
            // the seamless straight forms: sides only, the ends open onto the neighbouring tube
            if (k.arms() == X_STRAIGHT) {
                out.add(TubeBoxes.box("pneumo_tube_straight_x", 0, LOWER, LOWER, 1, UPPER, UPPER,
                        TubeBoxes.lateral(Direction.Axis.X), 0), STRAIGHT, -1);
            } else if (k.arms() == Z_STRAIGHT) {
                // 1.7: uvRotateTop = 2, uvRotateBottom = 1 - the sprite's tube axis turned onto Z
                out.add(TubeBoxes.box("pneumo_tube_straight_z", LOWER, LOWER, 0, UPPER, UPPER, 1,
                        TubeBoxes.lateral(Direction.Axis.Z), TubeBoxes.mask(Direction.UP, Direction.DOWN)), STRAIGHT, -1);
            } else {
                // 1.7: uvRotate N/S/E/W = 2 - the sprite's tube axis turned upright
                out.add(TubeBoxes.box("pneumo_tube_straight_y", LOWER, 0, LOWER, UPPER, 1, UPPER,
                        TubeBoxes.lateral(Direction.Axis.Y), TubeBoxes.lateral(Direction.Axis.Y)), STRAIGHT, -1);
            }
        } else {
            // the core: one face per direction, hidden where an arm attaches; the arms show their lateral faces
            for (Direction dir : Direction.values()) {
                if (has(k.arms(), dir)) {
                    double[] b = armBounds(dir, LOWER, UPPER, UPPER, 1);
                    out.add(TubeBoxes.box("pneumo_tube_arm_" + dir.getName(), b[0], b[1], b[2], b[3], b[4], b[5],
                            TubeBoxes.lateral(dir.getAxis()), 0), BASE, -1);
                } else {
                    out.add(TubeBoxes.box("pneumo_tube_core_" + dir.getName(), LOWER, LOWER, LOWER, UPPER, UPPER, UPPER,
                            1 << dir.ordinal(), 0), BASE, -1);
                }
            }
        }

        // 1.7 renderCon: a collar (base sprite, lateral faces) then a wider head (the nozzle sprite, all faces)
        for (Direction dir : Direction.values()) {
            int ordinal = dir.ordinal();
            String head = k.insertion() == ordinal ? IN : k.ejection() == ordinal ? OUT
                    : has(k.air(), dir) ? CONNECTOR : null;
            if (head == null) continue;
            double[] collar = armBounds(dir, LOWER, UPPER, UPPER, C_UPPER);
            double[] cap = armBounds(dir, C_LOWER, C_UPPER, C_UPPER, 1);
            out.add(TubeBoxes.box("pneumo_tube_collar_" + dir.getName(), collar[0], collar[1], collar[2], collar[3],
                    collar[4], collar[5], TubeBoxes.lateral(dir.getAxis()), 0), BASE, -1);
            out.add(TubeBoxes.box("pneumo_tube_head_" + dir.getName(), cap[0], cap[1], cap[2], cap[3], cap[4], cap[5],
                    TubeBoxes.ALL, 0), head, -1);
        }
    }

    @Override
    public boolean ambientOcclusion() {
        return false;
    }

    private static boolean has(int mask, Direction dir) {
        return (mask & (1 << dir.ordinal())) != 0;
    }

    /**
     * A box of cross-section {@code [lo, hi]} on the two axes across {@code dir}, spanning {@code [from, to]} on the
     * positive side of the block along {@code dir}, mirrored to {@code [1 - to, 1 - from]} for a negative {@code dir}.
     */
    private static double[] armBounds(Direction dir, double lo, double hi, double from, double to) {
        double[] min = {lo, lo, lo};
        double[] max = {hi, hi, hi};
        int axis = dir.getAxis().ordinal(); // X=0, Y=1, Z=2
        if (dir.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            min[axis] = from;
            max[axis] = to;
        } else {
            min[axis] = 1 - to;
            max[axis] = 1 - from;
        }
        return new double[]{min[0], min[1], min[2], max[0], max[1], max[2]};
    }
}
