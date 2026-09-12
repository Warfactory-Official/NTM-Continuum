package com.hbm.blocks.machine;

import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityHeaterOven;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Heating oven (1.12.2 {@code heater_oven}), the high-temperature firebox tier. */
public class HeaterOven extends HeaterFirebox {

    public HeaterOven(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityHeaterOven(pos, state);
    }

}
