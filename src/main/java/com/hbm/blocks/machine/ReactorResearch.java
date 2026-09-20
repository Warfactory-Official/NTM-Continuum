package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.machine.TileEntityReactorResearch;

/** The legacy research reactor: a 1x3 column (1.7 dims {@code {2,0,0,0,0,0}}) that bubbles where it touches water. */
public final class ReactorResearch extends BlockMultiblockMachine implements WorldlyContainerHolder {

    public ReactorResearch(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityReactorResearch(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 0, 0, 0, 0}; }
    @Override public int getOffset() { return 0; }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityReactorResearch te ? te : null;
    }

    /** 1.7 {@code TileEntityProxyCombo(false, true, true)} on the upper cells: items pass through to the core. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.above(), MASK_ALL, PASSIVE_ITEMS);
        visitor.passiveCell(core.above(2), MASK_ALL, PASSIVE_ITEMS);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        super.animateTick(state, level, pos, rand);

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(dir);
            if (!level.getFluidState(side).is(net.minecraft.tags.FluidTags.WATER)) continue;

            double ix = pos.getX() + 0.5F + dir.getStepX() + rand.nextDouble() - 0.5D;
            double iy = pos.getY() + 0.5F + dir.getStepY() + rand.nextDouble() - 0.5D;
            double iz = pos.getZ() + 0.5F + dir.getStepZ() + rand.nextDouble() - 0.5D;

            if (dir.getStepX() != 0) ix = pos.getX() + 0.5F + dir.getStepX() * 0.5 + rand.nextDouble() * 0.125 * dir.getStepX();
            if (dir.getStepZ() != 0) iz = pos.getZ() + 0.5F + dir.getStepZ() * 0.5 + rand.nextDouble() * 0.125 * dir.getStepZ();

            level.addParticle(ParticleTypes.BUBBLE, ix, iy, iz, 0.0, 0.2, 0.0);
        }
    }
}
