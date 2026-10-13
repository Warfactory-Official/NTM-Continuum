package com.hbm.blocks.machine;

import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.registry.TickingBlock;
import com.hbm.tileentity.machine.TileEntityMachineMiniRTG;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Small and polonium RTG blocks. The shared BlockEntity selects the output from the block identity. */
public class MachineMiniRTG extends CompatBlock implements TickingBlock {

    public MachineMiniRTG(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineMiniRTG(pos, state);
    }

}
