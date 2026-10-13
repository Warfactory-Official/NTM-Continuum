package com.hbm.blocks.generic;

import dev.golem.core.world.BlockTickScheduler;
import dev.golem.core.world.CompatBlock;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import com.hbm.client.render.DecoModelRenderer;
import com.hbm.tileentity.deco.TileEntityDecoModel;

/**
 * The OBJ-drawn furniture of 1.7 whose only state is the placer's look direction: {@code DecoSteelPoles},
 * {@code DecoPoleTop}, {@code DecoPoleSatelliteReceiver}, {@code DecoTapeRecorder}, the three
 * {@code BlockDecoToaster} and four {@code BlockDecoCRT} subtypes (flattened) and the
 * {@code BlockDecoModel} computer. Every one of them stored the
 * {@code MathHelper.floor_double(yaw * 4 / 360 + 0.5) & 3} quadrant in some bit layout; {@link #FACING}
 * keeps the placer's horizontal look direction itself and {@code DecoModelRenderer} owns each kind's
 * old rotation table. Looks are drawn by that renderer through {@link TileEntityDecoModel}.
 */
public class DecoModelBlock extends CompatBlock implements EntityBlock {

    public enum Kind {
        POLE, TOP, SATELLITE, TAPE_RECORDER,
        TOASTER_IRON, TOASTER_STEEL, TOASTER_WOOD,
        CRT_CLEAN, CRT_BROKEN, CRT_BLINKING, CRT_BSOD,
        COMPUTER;

        public boolean toaster() { return this == TOASTER_IRON || this == TOASTER_STEEL || this == TOASTER_WOOD; }
        public boolean crt() { return this == CRT_CLEAN || this == CRT_BROKEN || this == CRT_BLINKING || this == CRT_BSOD; }
    }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape TOASTER_NS = Shapes.box(0.25, 0, 0.375, 0.75, 0.325, 0.625);
    private static final VoxelShape TOASTER_EW = Shapes.box(0.375, 0, 0.25, 0.625, 0.325, 0.75);

    public final Kind kind;

    public DecoModelBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDecoModel(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction look = state.getValue(FACING);
        if (kind.toaster()) {
            // 1.7: meta % 2 == 0 (quadrant 0/2, looking south/north) is the x-long box
            return look.getAxis() == Direction.Axis.Z ? TOASTER_NS : TOASTER_EW;
        }
        if (kind == Kind.COMPUTER) {
            // BlockDecoModel.setBlockBoundsTo(.125, 0, 0, .875, .875, .625) swizzled by its rotation code
            return switch (look) {
                case SOUTH -> Shapes.box(0.125, 0, 0.375, 0.875, 0.875, 1);   // rot 0 "North"
                case NORTH -> Shapes.box(0.125, 0, 0, 0.875, 0.875, 0.625);   // rot 1 "South"
                case EAST -> Shapes.box(0.375, 0, 0.125, 1, 0.875, 0.875);    // rot 2 "West"
                default -> Shapes.box(0, 0, 0.125, 0.625, 0.875, 0.875);      // rot 3 "East"
            };
        }
        return Shapes.block();
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // floor(yaw*4/360+0.5)&3 is exactly the horizontal look quadrant
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** {@code steel_poles}: the one furniture piece that is waterloggable (port addition, see {@link Waterlogging}). */
    public static class Pole extends DecoModelBlock implements SimpleWaterloggedBlock {

        public Pole(Properties properties) {
            super(properties, Kind.POLE);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Waterlogging.WATERLOGGED, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(Waterlogging.WATERLOGGED);
        }

        @Override
        @Nullable
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return Waterlogging.place(super.getStateForPlacement(context), context);
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

        /** {@code pole.obj}'s three 2x2 posts, centred on the block, before {@code DecoModelRenderer}'s yaw. */
        private static final double[][] POSTS = {{0, -5}, {5, 5}, {-5, 5}};
        private static final Map<Direction, VoxelShape> COLLISION = new EnumMap<>(Direction.class);

        static {
            for (Direction look : Direction.Plane.HORIZONTAL) {
                // DecoModelRenderer: POLE yaw = quadrant - 90 (south 0, west -90, north 180, east 90), JOML rotateY
                double yaw = Math.toRadians(switch (look) {
                    case WEST -> -90;
                    case NORTH -> 180;
                    case EAST -> 90;
                    default -> 0;
                });
                double cos = Math.round(Math.cos(yaw)), sin = Math.round(Math.sin(yaw));
                VoxelShape shape = Shapes.empty();
                for (double[] post : POSTS) {
                    double x = 8 + post[0] * cos + post[1] * sin;
                    double z = 8 - post[0] * sin + post[1] * cos;
                    shape = Shapes.or(shape, Block.box(x - 1, 0, z - 1, x + 1, 16, z + 1));
                }
                COLLISION.put(look, shape);
            }
        }

        /*
         * port: collision is the three posts. 1.7's full cube would keep a waterlogged pole's water in,
         * since vanilla fluid only leaves through faces the collision does not cover. The outline
         * and the support shape stay the 1.7 full cube; the gaps between posts are narrower than a player.
         */
        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return COLLISION.get(state.getValue(FACING));
        }

        @Override
        public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
            return Shapes.block();
        }
    }
}
