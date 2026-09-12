package com.hbm.blocks.turret;

import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.turret.TileEntityTurretArty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class TurretArty extends TurretBaseArtillery {

    public TurretArty(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityTurretArty(pos, state);
    }

}
