package com.hbm.blocks.generic;

import com.hbm.config.GeneralConfig;
import dev.golem.core.world.CompatBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Flammable gas pocket, the 1.12.2 {@code BlockGasFlammable} over {@code BlockGasBase}. An invisible,
 * non-collidable, drop-nothing cell that drifts one block per tick attempt and ignites on contact with
 * fire, lava, a torch or a burning entity.
 *
 * <p>Behaviour ported from the pair of old classes:
 * <ul>
 *   <li><b>Drift</b> ({@code BlockGasBase.updateTick}): a 1-in-2 chance to move, trying
 *       {@link #firstDirection} and falling back to {@link #secondDirection}; a move only succeeds
 *       into air, and vacates the source cell.</li>
 *   <li><b>Buoyancy</b>: {@code getFirstDirection} is 1-in-3 a vertical direction (DOWN or UP,
 *       {@code nextInt(2)} over the ForgeDirection order) and otherwise horizontal; the second try is
 *       always horizontal.</li>
 *   <li><b>Dissipation</b>: 1-in-20 per tick when the cell below is air.</li>
 *   <li><b>Ignition</b>: any of the six neighbours being a fire source combusts the cell into fire and
 *       schedules the adjacent gas cells 2 ticks out, which is what makes a pocket chain-burn.</li>
 *   <li><b>Config</b>: with {@link GeneralConfig#enableFlammableGas} off the cell deletes itself on
 *       its next tick and never ignites.</li>
 * </ul>
 *
 * <p>1.12 drove this off {@code setTickRandomly} + {@code updateTick} + explicit
 * {@code scheduleUpdate(…, getDelay())}. Here {@code randomTick} is the steady cadence and
 * {@code scheduleTick} the explicit pulse (see the block-tick gotcha in IMPLEMENTATION.md §22), so
 * drift/dissipation runs on {@code randomTick} and only the ignition chain uses {@code scheduleTick}.
 */
public class BlockGasFlammable extends CompatBlock {

    public BlockGasFlammable(Properties properties) {
        super(properties);
    }

    /** 1.7 {@code canCollideCheck} false: the crosshair passes through a gas, so it has no outline. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** 1.7 {@code BlockGasBase.randomDisplayTick} (1.7's flammable gas is a BlockGasBase): the ash-glasses cloud. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockGasBase.ashGlassesCloud(level, pos, random, BlockGasBase.FLAMMABLE_COLOR[0], BlockGasBase.FLAMMABLE_COLOR[1],
                BlockGasBase.FLAMMABLE_COLOR[2]);
    }

    // --- drops: a gas cell yields nothing, ever ---------------------------------------------------

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    // --- drift ------------------------------------------------------------------------------------

    /** 1-in-3 vertical (DOWN/UP, the old {@code nextInt(2)} over ForgeDirection), else horizontal. */
    protected Direction firstDirection(RandomSource random) {
        if (random.nextInt(3) == 0) return random.nextInt(2) == 0 ? Direction.DOWN : Direction.UP;
        return randomHorizontal(random);
    }

    /** The fallback try is always horizontal. */
    protected Direction secondDirection(RandomSource random) {
        return randomHorizontal(random);
    }

    protected static Direction randomHorizontal(RandomSource random) {
        return Direction.Plane.HORIZONTAL.getRandomDirection(random);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!enabled()) {
            level.removeBlock(pos, false);
            return;
        }
        for (Direction dir : Direction.values()) {
            if (isFireSource(level.getBlockState(pos.relative(dir)))) {
                combust(level, pos);
                return;
            }
        }
        if (random.nextInt(20) == 0 && level.getBlockState(pos.below()).isAir()) {
            level.removeBlock(pos, false);
            return;
        }
        if (random.nextInt(2) == 0) {
            if (!tryMove(level, pos, firstDirection(random))) tryMove(level, pos, secondDirection(random));
        }
    }

    private boolean tryMove(ServerLevel level, BlockPos pos, Direction dir) {
        BlockPos target = pos.relative(dir);
        if (!level.isLoaded(target) || !level.getBlockState(target).isAir()) return false;
        BlockState state = level.getBlockState(pos);
        level.removeBlock(pos, false);
        level.setBlock(target, state, 2);
        return true;
    }

    // --- ignition ---------------------------------------------------------------------------------

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                     boolean movedByPiston) {
        if (level.isClientSide() || !enabled()) return;
        for (Direction dir : Direction.values()) {
            if (isFireSource(level.getBlockState(pos.relative(dir)))) {
                combust(level, pos);
                return;
            }
        }
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && enabled() && entity.isOnFire()) combust(level, pos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // the chain-burn pulse scheduled by a neighbouring cell's combust()
        if (enabled()) combust(level, pos);
    }

    /** Turn this cell into fire and pulse the adjacent gas cells 2 ticks out (the chain reaction). */
    protected void combust(Level level, BlockPos pos) {
        level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 2);
        for (Direction dir : Direction.values()) {
            BlockPos neighbour = pos.relative(dir);
            BlockState state = level.getBlockState(neighbour);
            if (state.getBlock() instanceof BlockGasFlammable) {
                level.scheduleTick(neighbour, state.getBlock(), 2);
            }
        }
    }

    /** Fire, lava (block or fluid) or a torch: the {@code isFireSource} material test. */
    public boolean isFireSource(BlockState state) {
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH) || state.is(Blocks.LAVA)) {
            return true;
        }
        FluidState fluid = state.getFluidState();
        return !fluid.isEmpty() && fluid.is(FluidTags.LAVA);
    }

    protected boolean enabled() {
        return GeneralConfig.enableFlammableGas;
    }
}
