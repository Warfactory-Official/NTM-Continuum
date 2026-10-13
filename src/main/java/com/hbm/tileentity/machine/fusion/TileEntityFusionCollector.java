package com.hbm.tileentity.machine.fusion;

import com.hbm.capability.MachineCaps;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fusion collector: a passive dish that boosts the torus's output rate rather than consuming any of it.
 *
 * <p>It registers on the plasma network as a receiver purely so the torus can see it; it takes no share
 * ({@link #receivesFusionPower()} is false, so it is not counted among the consumers that divide the
 * output) but each one adds half a product per tick to the reactor's bonus bar. Upstream expressed this
 * with an {@code instanceof TileEntityFusionCollector} test on the torus's side; here the collector
 * declares it, so the torus needs no knowledge of this class.
 */
public class TileEntityFusionCollector extends TileEntityLoadedBase implements IFusionPowerReceiver {


    public TileEntityFusionCollector(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_COLLECTOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFusionCollector te) {
        ForgeDirection dir = FusionNodes.facing(state).getOpposite();
        FusionLink.claim(level, te, plasmaCell(pos, dir), MachineCaps.PLASMA_IN);
    }

    /** False: the collector adds to the reactor's rate without taking a share of its output. */
    @Override public boolean receivesFusionPower() { return false; }

    @Override public boolean providesCollectorBonus() { return true; }

    @Override public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) { }

    @Override
    public void setRemoved() {
        FusionLink.release(level, plasmaCell(worldPosition,
                FusionNodes.facing(getBlockState()).getOpposite()));
        super.setRemoved();
    }

    /** Where this machine claims its end of the torus beam, 2 blocks out the back at +2. */
    private static BlockPos plasmaCell(BlockPos pos, ForgeDirection dir) {
        return new BlockPos(pos.getX() + dir.offsetX * 2, pos.getY() + 2,
                pos.getZ() + dir.offsetZ * 2);
    }
}
