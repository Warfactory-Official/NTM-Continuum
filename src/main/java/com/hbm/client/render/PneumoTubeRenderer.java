package com.hbm.client.render;

import com.hbm.blocks.network.PneumoTube;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderPneumoTube.renderWorldBlock}, part for part: a core cube whose faces vanish behind arms, one
 * arm per connected face, the seamless straight form along one axis when nothing else is attached, and on top
 * the nozzles - a collar in the tube's own sprite and a head in the in/out/connector sprite. The block itself is
 * invisible; this draws it.
 */
public class PneumoTubeRenderer extends GemBER<TileEntityPneumoTube, PneumoTubeRenderState> {

    private static final String BASE = "hbm:textures/blocks/pneumatic_tube.png";
    private static final String STRAIGHT = "hbm:textures/blocks/pneumatic_tube_straight.png";
    private static final String IN = "hbm:textures/blocks/pneumatic_tube_in.png";
    private static final String OUT = "hbm:textures/blocks/pneumatic_tube_out.png";
    private static final String CONNECTOR = "hbm:textures/blocks/pneumatic_tube_connector.png";

    private static final double LOWER = 0.3125D;
    private static final double UPPER = 0.6875D;
    // 1.7 renderCon: c* collar, n* head
    private static final double C_LOWER = 0.25D;
    private static final double C_UPPER = 0.75D;

    private static final int X_STRAIGHT = TubeBoxes.mask(Direction.EAST, Direction.WEST);
    private static final int Y_STRAIGHT = TubeBoxes.mask(Direction.UP, Direction.DOWN);
    private static final int Z_STRAIGHT = TubeBoxes.mask(Direction.NORTH, Direction.SOUTH);

    private static boolean straight(PneumoTubeRenderState state) {
        if (state.hasConnections) return false;
        return state.arms == X_STRAIGHT || state.arms == Y_STRAIGHT || state.arms == Z_STRAIGHT;
    }

    private static boolean has(int mask, Direction dir) {
        return (mask & (1 << dir.ordinal())) != 0;
    }

    @Override
    protected void setup(Gem.Model<PneumoTubeRenderState> model) {
        Surface base = Surface.cutout(BASE);
        Surface straight = Surface.cutout(STRAIGHT);

        // the seamless straight forms: sides only, the ends open onto the neighbouring tube
        model.part(Quads.box("pneumo_tube_straight_x", 0, LOWER, LOWER, 1, UPPER, UPPER,
                        TubeBoxes.lateral(Direction.Axis.X), 0), straight)
                .visibleIf(s -> !s.hasConnections && s.arms == X_STRAIGHT);
        // 1.7: uvRotateTop = 2, uvRotateBottom = 1 - the sprite's tube axis turned onto Z
        model.part(Quads.box("pneumo_tube_straight_z", LOWER, LOWER, 0, UPPER, UPPER, 1,
                        TubeBoxes.lateral(Direction.Axis.Z), TubeBoxes.mask(Direction.UP, Direction.DOWN)), straight)
                .visibleIf(s -> !s.hasConnections && s.arms == Z_STRAIGHT);
        // 1.7: uvRotate N/S/E/W = 2 - the sprite's tube axis turned upright
        model.part(Quads.box("pneumo_tube_straight_y", LOWER, 0, LOWER, UPPER, 1, UPPER,
                        TubeBoxes.lateral(Direction.Axis.Y), TubeBoxes.lateral(Direction.Axis.Y)), straight)
                .visibleIf(s -> !s.hasConnections && s.arms == Y_STRAIGHT);

        // the core: one face per direction, hidden where an arm attaches
        for (Direction dir : Direction.values()) {
            model.part(Quads.box("pneumo_tube_core_" + dir.getName(), LOWER, LOWER, LOWER, UPPER, UPPER, UPPER,
                            1 << dir.ordinal(), 0), base)
                    .visibleIf(s -> !straight(s) && !has(s.arms, dir));
        }

        // the arms: lateral faces only, the end opens onto the neighbour
        for (Direction dir : Direction.values()) {
            double[] b = armBounds(dir, LOWER, UPPER, 0, 1);
            model.part(Quads.box("pneumo_tube_arm_" + dir.getName(), b[0], b[1], b[2], b[3], b[4], b[5],
                            TubeBoxes.lateral(dir.getAxis()), 0), base)
                    .visibleIf(s -> !straight(s) && has(s.arms, dir));
        }

        // 1.7 renderCon: a collar (base sprite, lateral faces) then a wider head (the nozzle sprite, all faces)
        for (Direction dir : Direction.values()) {
            double[] collar = armBounds(dir, LOWER, UPPER, UPPER, C_UPPER);
            double[] head = armBounds(dir, C_LOWER, C_UPPER, C_UPPER, 1);
            Geometry collarBox = Quads.box("pneumo_tube_collar_" + dir.getName(),
                    collar[0], collar[1], collar[2], collar[3], collar[4], collar[5],
                    TubeBoxes.lateral(dir.getAxis()), 0);
            model.part(collarBox, base).visibleIf(s -> nozzle(s, dir));

            Geometry headBox = Quads.box("pneumo_tube_head_" + dir.getName(),
                    head[0], head[1], head[2], head[3], head[4], head[5], TubeBoxes.ALL, 0);
            int ordinal = dir.ordinal();
            model.part(headBox, Surface.cutout(IN)).visibleIf(s -> s.insertion == ordinal);
            model.part(headBox, Surface.cutout(OUT)).visibleIf(s -> s.ejection == ordinal);
            model.part(headBox, Surface.cutout(CONNECTOR))
                    .visibleIf(s -> has(s.air, dir) && s.insertion != ordinal && s.ejection != ordinal);
        }
    }

    private static boolean nozzle(PneumoTubeRenderState s, Direction dir) {
        return s.insertion == dir.ordinal() || s.ejection == dir.ordinal() || has(s.air, dir);
    }

    /**
     * A box of cross-section {@code [lo, hi]} on the two axes across {@code dir}, spanning {@code [from, to]} along
     * {@code dir} measured outwards from the block centre (0.5 = the centre, 1 = the block face).
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

    @Override
    public PneumoTubeRenderState createRenderState() {
        return new PneumoTubeRenderState();
    }

    @Override
    public void extract(TileEntityPneumoTube tube, PneumoTubeRenderState state, float partialTicks, Vec3 camera) {
        BlockState bs = tube.getBlockState();
        state.arms = bs.getBlock() instanceof PneumoTube ? PneumoTube.faceMask(bs) : 0;
        state.insertion = tube.insertionDir == null ? -1 : tube.insertionDir.ordinal();
        state.ejection = tube.ejectionDir == null ? -1 : tube.ejectionDir.ordinal();
        state.air = tube.airMask & 0x3F;
        state.hasConnections = tube.isCompressor() || tube.isEndpoint();
    }
}
