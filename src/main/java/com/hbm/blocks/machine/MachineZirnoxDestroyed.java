package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import dev.golem.core.multiblock.MultiblockCore;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockFootprint;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityZirnoxDestroyed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The inert, radioactive ZIRNOX wreck left behind by a reactor accident. */
public class MachineZirnoxDestroyed extends BlockMultiblockMachine {

    public MachineZirnoxDestroyed(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityZirnoxDestroyed(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 2, 2, 2, 2};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    /** Builds the wreck after the blast, so the blast cannot remove the replacement multiblock. */
    public void formWreck(Level level, BlockPos pos, Direction facing) {
        MultiblockCore.withoutTeardown(() ->
                MultiblockFootprint.placeAssembled(level, pos, this, facing));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, 100 + level.getRandom().nextInt(20));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState above = level.getBlockState(pos.above());
        if (above.isAir() && random.nextInt(10) == 0) {
            level.setBlock(pos.above(), ModNuclearBlocks.gas_meltdown.get().defaultBlockState(), 2);
        } else if (above.is(ModBlocks.block_foam.get()) && random.nextInt(25) == 0
                && MultiblockSurface.coreEntity(level, pos) instanceof TileEntityZirnoxDestroyed wreck) {
            wreck.extinguish();
        }
    }

}
