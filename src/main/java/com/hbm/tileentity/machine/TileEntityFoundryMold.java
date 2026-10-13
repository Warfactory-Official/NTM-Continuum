package com.hbm.tileentity.machine;

import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The small caster (1.12.2 {@code TileEntityFoundryMold}), takes size-0 molds. */
public class TileEntityFoundryMold extends TileEntityFoundryCastingBase {

    public TileEntityFoundryMold(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_MOLD.get(), pos, state);
    }

    @Override
    public int getMoldSize() {
        return 0;
    }

    // render parameters (old IRenderFoundry)
    public double getMeltLevel() { return 0.125 + this.amount * 0.25D / Math.max(this.getCapacity(), 1); }
    public double moldHeight() { return 0.13D; }
    public double outHeight() { return 0.25D; }
}
