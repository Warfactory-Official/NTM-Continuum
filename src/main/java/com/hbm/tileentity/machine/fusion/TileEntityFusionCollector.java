package com.hbm.tileentity.machine.fusion;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.uninos.networkproviders.PlasmaNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fusion collector — a passive dish that boosts the torus's output rate rather than consuming any of it.
 *
 * <p>It registers on the plasma network as a receiver purely so the torus can see it; it takes no share
 * ({@link #receivesFusionPower()} is false, so it is not counted among the consumers that divide the
 * output) but each one adds half a product per tick to the reactor's bonus bar. Upstream expressed this
 * with an {@code instanceof TileEntityFusionCollector} test on the torus's side; here the collector
 * declares it, so the torus needs no knowledge of this class.
 */
public class TileEntityFusionCollector extends TileEntityLoadedBase implements IFusionPowerReceiver {

    private PlasmaNetwork.PlasmaNode plasmaNode;

    public TileEntityFusionCollector(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_COLLECTOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFusionCollector te) {
        ForgeDirection dir = FusionNodes.facing(state).getOpposite();
        te.plasmaNode = FusionNodes.plasma(level, te.plasmaNode,
                new BlockPos(pos.getX() + dir.offsetX * 2, pos.getY() + 2, pos.getZ() + dir.offsetZ * 2),
                new DirPos(pos.getX() + dir.offsetX * 3, pos.getY() + 2, pos.getZ() + dir.offsetZ * 3, dir));
        if (te.plasmaNode.hasValidNet()) te.plasmaNode.net.addReceiver(te);
    }

    /** False — the collector adds to the reactor's rate without taking a share of its output. */
    @Override public boolean receivesFusionPower() { return false; }

    @Override public boolean providesCollectorBonus() { return true; }

    @Override public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) { }

    @Override
    public void setRemoved() {
        FusionNodes.destroy(level, null, plasmaNode);
        super.setRemoved();
    }
}
