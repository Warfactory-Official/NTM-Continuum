package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityFurnaceSteel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The 1x2x2 steel furnace multiblock shell and its core. */
public class MachineFurnaceSteel extends BlockDummyable implements WorldlyContainerHolder {
    public MachineFurnaceSteel(Properties properties) { super(properties); }
    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) { return new TileEntityFurnaceSteel(pos, state); }
    @Override public int[] getDimensions() { return new int[]{1, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }
    @Override public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityFurnaceSteel furnace ? furnace : null;
    }
    }
