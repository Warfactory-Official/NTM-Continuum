package com.hbm.blocks.generic;

import dev.golem.core.world.BlockTickScheduler;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The decorative steel pipe (1.7 {@code BlockPipe}): an axis pillar with an octagonal OBJ body drawn by
 * {@code RenderPipe}. 1.7 kept full-cube bounds and only changed the render type, so the port is an axis pillar
 * whose geometry lives in the authored {@code template_pipe*} element models; {@link Style} picks which of the
 * four {@code rType} bodies the blockstate uses. Placement on a face's axis is vanilla pillar behaviour, which
 * is what 1.7's {@code onBlockPlaced} computed (0/4/8 by clicked side). It spells out {@link RotatedPillarBlock}'s
 * axis handling on a {@link CompatBlock} so it can also be waterlogged (port addition, see {@link Waterlogging}).
 */
public class DecoPipe extends CompatBlock implements SimpleWaterloggedBlock {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    /** The old {@code rType}: 0 plain, 1 rimmed, 2 quad, 3 rimmed inside a mesh frame. */
    public enum Style {
        PLAIN("template_pipe"), RIM("template_pipe_rim"), QUAD("template_pipe_quad"), FRAMED("template_pipe_framed");

        public final String template;

        Style(String template) {
            this.template = template;
        }
    }

    public final Style style;
    /** Collision per {@link Direction.Axis#ordinal()}; see {@link #getCollisionShape}. */
    private final VoxelShape[] collision = new VoxelShape[3];

    public DecoPipe(Properties properties, Style style) {
        super(properties);
        this.style = style;
        VoxelShape y = collisionY(style);
        for (Direction.Axis axis : Direction.Axis.values()) collision[axis.ordinal()] = along(y, axis);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(Waterlogging.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, Waterlogging.WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Waterlogging.place(defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis()), context);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return Waterlogging.fluid(state);
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        Waterlogging.tick(state, level, ticks, pos);
        return state;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return RotatedPillarBlock.rotatePillar(state, rotation);
    }

    /*
     * Collision follows the pipe body (a port deviation): 1.7 kept full-cube bounds, but
     * vanilla fluid only leaves a waterlogged block through faces its collision does not cover, so a full cube kept
     * the water in. The outline and the support shape stay the 1.7 full cube (selection, torches, chains).
     */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return collision[state.getValue(AXIS).ordinal()];
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    /** The {@code template_pipe*} geometry standing along Y, in pixels. */
    private static VoxelShape collisionY(Style style) {
        return switch (style) {
            case PLAIN -> octagon(8, 8, 5.543, 0, 16);
            case RIM -> rimmed();
            case QUAD -> Shapes.or(octagon(4, 4, 1.848, 0, 16), octagon(4, 12, 1.848, 0, 16),
                    octagon(12, 4, 1.848, 0, 16), octagon(12, 12, 1.848, 0, 16));
            case FRAMED -> Shapes.or(rimmed(),
                    Block.box(0, 0, 0, 2, 16, 2), Block.box(0, 0, 14, 2, 16, 16),
                    Block.box(14, 0, 0, 16, 16, 2), Block.box(14, 0, 14, 16, 16, 16),
                    // the mesh sheets sit 1 px inside the faces; a thin slab keeps them off the face layer
                    Block.box(2, 0, 0.5, 14, 16, 1.5), Block.box(2, 0, 14.5, 14, 16, 15.5),
                    Block.box(0.5, 0, 2, 1.5, 16, 14), Block.box(14.5, 0, 2, 15.5, 16, 14));
        };
    }

    private static VoxelShape rimmed() {
        return Shapes.or(octagon(8, 8, 5.543, 1, 15), octagon(8, 8, 6.467, 0, 1), octagon(8, 8, 6.467, 15, 16));
    }

    /** An axis-aligned stand-in for the model's octagonal prism of apothem {@code a} around {@code (cx, cz)}. */
    private static VoxelShape octagon(double cx, double cz, double a, double y0, double y1) {
        double half = a * 0.41421356; // half a face: a * tan(22.5 degrees)
        double corner = a * 0.70710678; // a square that touches the diagonal faces
        return Shapes.or(Block.box(cx - a, y0, cz - half, cx + a, y1, cz + half),
                Block.box(cx - half, y0, cz - a, cx + half, y1, cz + a),
                Block.box(cx - corner, y0, cz - corner, cx + corner, y1, cz + corner));
    }

    /** Lays a Y-standing shape along {@code axis}, as the blockstate's x/z rotations do (every style is symmetric). */
    private static VoxelShape along(VoxelShape y, Direction.Axis axis) {
        if (axis == Direction.Axis.Y) return y;
        VoxelShape out = Shapes.empty();
        for (AABB box : y.toAabbs()) {
            out = Shapes.or(out, axis == Direction.Axis.X
                    ? Shapes.box(box.minY, box.minX, box.minZ, box.maxY, box.maxX, box.maxZ)
                    : Shapes.box(box.minX, box.minZ, box.minY, box.maxX, box.maxZ, box.maxY));
        }
        return out.optimize();
    }
}
