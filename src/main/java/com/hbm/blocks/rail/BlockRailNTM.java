package com.hbm.blocks.rail;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.BlockEntityRail;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Common base of every {@link IRailNTM} block. 1.7 rails were {@code BlockDummyable}s whose dummies were the
 * same block class, so any anchor position answered {@code instanceof IRailNTM} directly; here only the core is
 * the rail block (cells are the shared folded cells), so resolution goes through the core index via
 * {@link #railAt}. The BE exists only where this block stands - the core - and carries nothing but the switch state.
 */
public abstract class BlockRailNTM extends BlockMultiblockMachine
        implements IRailNTM, SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    protected BlockRailNTM(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(WATERLOGGED,
                ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, dev.golem.core.world.BlockTickScheduler ticks,
                                       BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.onUpdateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }


    /**
     * Where a resolved rail stands. Mutable and caller-owned: {@link #railAt} runs once per bogie per step of
     * every rail walk, and a record per call was the second-largest allocator in the system after the travel
     * vectors. Each walker keeps its own instance; a rail block never holds one across a call.
     */
    public static final class Owner {
        public int coreX, coreY, coreZ;
        public Direction facing;
        public BlockRailNTM rail;
        private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        /** Distinct from {@link #probe}, which may BE the position under test. */
        private final BlockPos.MutableBlockPos core = new BlockPos.MutableBlockPos();

        boolean matches(BlockPos pos) {
            return pos.getX() == coreX && pos.getY() == coreY && pos.getZ() == coreZ;
        }
    }

    /** cell -> rail owner on both physical sides; false when the position holds no folded rail */
    public static boolean railAt(Level level, int x, int y, int z, Owner out) {
        return railAt(level, out.probe.set(x, y, z), out);
    }

    /**
     * As {@link #railAt(Level, int, int, int, Owner)}. {@code pos} may be a mutable the caller keeps reusing; it
     * is fully read before {@code out}'s own scratch is touched, so passing {@code out.probe} is safe.
     */
    public static boolean railAt(Level level, BlockPos pos, Owner out) {
        // A rail walk asks about one position after another along the same rail, and a rail spans up to fifteen
        // blocks; re-validating the last answer is a state read, where resolving again is an index lookup on the
        // server and a walk over every multiblock's cell geometry on the client.
        if(out.rail != null) {
            BlockPos core = out.core.set(out.coreX, out.coreY, out.coreZ);
            if(out.matches(pos) || MultiblockSurface.coreClaims(level, core, pos)) {
                BlockState cached = level.getBlockState(core);
                if(cached.getBlock() == out.rail && cached.getValue(FACING) == out.facing) return true;
            }
        }

        BlockPos found = coreOfAny(level, pos);
        if(found == null) return false;
        int cx = found.getX(), cy = found.getY(), cz = found.getZ();
        BlockState state = level.getBlockState(out.core.set(cx, cy, cz));
        if(!(state.getBlock() instanceof BlockRailNTM rail)) return false;
        out.coreX = cx;
        out.coreY = cy;
        out.coreZ = cz;
        out.facing = state.getValue(FACING);
        out.rail = rail;
        return true;
    }

    /** The core a rail cell belongs to: the block itself when it is a core, else this port's core index. */
    private static @Nullable BlockPos coreOfAny(Level level, BlockPos pos) {
        BlockState here = level.getBlockState(pos);
        if(here.getBlock() instanceof BlockRailNTM rail && !rail.isCellState(here)) return pos.immutable();
        return MultiblockSurface.coreOfAny(level, pos);
    }

    /**
     * 1.7 gave every rail block a flat 1/8 slab collision; the ramp's stepped boxes are its own override.
     */
    @Override
    public @Nullable VoxelShape cellShape(int lx, int ly, int lz, Direction facing, boolean open,
                                          boolean forCollision) {
        return Shapes.create(0, 0, 0, 1, 0.125, 1);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityRail(pos, state);
    }
}
