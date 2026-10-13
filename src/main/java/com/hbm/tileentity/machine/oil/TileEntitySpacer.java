package com.hbm.tileentity.machine.oil;

import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fractioning Tower Separator's core BlockEntity: a render/structure anchor with no logic (1.12.2
 * {@code TileEntitySpacer}). It only exists so the spacer multiblock has a core to hang its renderer on.
 */
public class TileEntitySpacer extends TileEntityLoadedBase {

    public TileEntitySpacer(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRACTION_SPACER.get(), pos, state);
    }
}
