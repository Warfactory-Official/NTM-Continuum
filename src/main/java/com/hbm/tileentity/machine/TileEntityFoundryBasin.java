package com.hbm.tileentity.machine;

import com.hbm.api.material.MaterialStack;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The large caster (1.12.2 {@code TileEntityFoundryBasin}) — takes size-1 molds, pour-only (no flow). */
public class TileEntityFoundryBasin extends TileEntityFoundryCastingBase {

    public TileEntityFoundryBasin(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_BASIN.get(), pos, state);
    }

    @Override
    public int getMoldSize() {
        return 1;
    }

    /* Basin can't accept sideways flowing */
    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos p, ForgeDirection side, MaterialStack stack) { return false; }

    @Override
    public @Nullable MaterialStack flow(Level world, BlockPos p, ForgeDirection side, MaterialStack stack) { return stack; }

    // render parameters (old IRenderFoundry)
    public double getMeltLevel() { return 0.125 + this.amount * 0.75D / Math.max(this.getCapacity(), 1); }
    public double moldHeight() { return 0.13D; }
    public double outHeight() { return 0.875D; }
}
