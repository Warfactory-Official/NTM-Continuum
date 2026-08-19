package com.hbm.tileentity.machine.fusion;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.uninos.networkproviders.KlystronNetwork;
import com.hbm.uninos.networkproviders.PlasmaNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Plasma-to-klystron coupler — the relay that lets one torus drive another. It sits as a <b>receiver</b>
 * on the plasma network (so the upstream torus counts it as one of the consumers sharing the output) and
 * as a <b>provider</b> on the klystron network, handing everything it receives straight through as
 * ignition energy.
 *
 * <p>Its two nodes straddle the machine: klystron one block to the left at +2, plasma one to the right,
 * so a coupler bridges the two networks without them ever touching.
 */
public class TileEntityFusionCoupler extends TileEntityLoadedBase implements IFusionPowerReceiver {

    private KlystronNetwork.KlystronNode klystronNode;
    private PlasmaNetwork.PlasmaNode plasmaNode;

    public TileEntityFusionCoupler(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_COUPLER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFusionCoupler te) {
        ForgeDirection dir = FusionNodes.facing(state).getOpposite();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

        te.klystronNode = FusionNodes.klystron(level, te.klystronNode,
                new BlockPos(pos.getX() + rot.offsetX, pos.getY() + 2, pos.getZ() + rot.offsetZ),
                new DirPos(pos.getX() + rot.offsetX * 2, pos.getY() + 2, pos.getZ() + rot.offsetZ * 2, rot));
        te.plasmaNode = FusionNodes.plasma(level, te.plasmaNode,
                new BlockPos(pos.getX() - rot.offsetX, pos.getY() + 2, pos.getZ() - rot.offsetZ),
                new DirPos(pos.getX() - rot.offsetX * 2, pos.getY() + 2, pos.getZ() - rot.offsetZ * 2, rot.getOpposite()));

        if (te.klystronNode.net != null) te.klystronNode.net.addProvider(te);
        if (te.plasmaNode.net != null) te.plasmaNode.net.addReceiver(te);
    }

    /** True: the coupler takes a full share of the plasma output, like a boiler or turbine would. */
    @Override public boolean receivesFusionPower() { return true; }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        if (klystronNode == null || klystronNode.net == null) return;
        // Hand it to the first live torus on the klystron side; upstream stops at the first one too.
        for (BlockEntity thing : klystronNode.net.receiverEntries.keySet()) {
            if (thing instanceof TileEntityFusionTorus torus && torus.isLoaded() && !torus.isRemoved()) {
                torus.receiveKlystronEnergy(fusionPower);
                break;
            }
        }
    }

    @Override
    public void setRemoved() {
        FusionNodes.destroy(level, klystronNode, plasmaNode);
        super.setRemoved();
    }
}
