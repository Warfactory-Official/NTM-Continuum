package com.hbm.tileentity.machine;

import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.ModPowerGenBlockEntities;

/**
 * 1.7 {@code TileEntityMachineLPW2}: a bare tile that holds no state and never ticks; it exists so the
 * renderer has something to hang off and a render box (x−10..x+11, y..y+7, z−10..z+11) to be culled by.
 * The port states that box as the model's {@code offScreen()} declaration instead.
 *
 * <p>Addon API (ADDON-API-INVENTORY P-14): an addon may subclass it ({@link #TileEntityMachineLPW2(BlockEntityType,
 * BlockPos, BlockState)}, the {@link TileEntityLoadedBase} sync and save hooks) and drive the renderer's moving parts
 * through {@link #animationTime}. HBM's own tile uses none of that and behaves as 1.7's.
 */
public class TileEntityMachineLPW2 extends TileEntityLoadedBase {

    public TileEntityMachineLPW2(BlockPos pos, BlockState state) {
        this(ModPowerGenBlockEntities.LPW2.get(), pos, state);
    }

    protected TileEntityMachineLPW2(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * The clock {@code RenderLPW2}'s moving parts follow, in ticks: 1.7's world time ({@code getTotalWorldTime() %
     * 1000000 + interp}). The monitor's error scroll always follows world time.
     */
    public double animationTime(float partialTicks) {
        return (level == null ? 0L : level.getGameTime() % 1000000) + partialTicks;
    }
}
