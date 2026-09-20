package com.hbm.tileentity.machine;

import dev.golem.core.data.CompatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.ModPowerGenBlockEntities;

/**
 * 1.7 {@code TileEntityMachineLPW2}: a bare tile that holds no state and never ticks; it exists so the
 * renderer has something to hang off and a render box (x−10..x+11, y..y+7, z−10..z+11) to be culled by.
 * The port states that box as the model's {@code offScreen()} declaration instead.
 */
public class TileEntityMachineLPW2 extends CompatBlockEntity {

    public TileEntityMachineLPW2(BlockPos pos, BlockState state) {
        super(ModPowerGenBlockEntities.LPW2.get(), pos, state);
    }
}
