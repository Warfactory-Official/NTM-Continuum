package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityChimneyBase;
import com.hbm.tileentity.machine.TileEntityChimneyIndustrial;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The 23-high industrial smokestack. */
public final class MachineChimneyIndustrial extends MachineChimneyBase {

    public MachineChimneyIndustrial(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityChimneyIndustrial(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{22, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }

}
