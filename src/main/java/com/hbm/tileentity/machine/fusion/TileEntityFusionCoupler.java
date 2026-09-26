package com.hbm.tileentity.machine.fusion;

import dev.golem.core.graph.Domains;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.machine.SyncedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Plasma-to-klystron coupler — the relay that lets one torus drive another. It claims the <b>receiving</b>
 * end of a plasma beam (so the upstream torus counts it as one of the consumers sharing the output) and
 * the <b>emitting</b> end of a klystron beam, handing everything it receives straight through as
 * ignition energy.
 *
 * <p>Its two claims straddle the machine: klystron one block to the left at +2, plasma one to the right,
 * so a coupler bridges the two beams without them ever touching.
 */
public class TileEntityFusionCoupler extends SyncedBlockEntity implements IFusionPowerReceiver {

    public TileEntityFusionCoupler(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_COUPLER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFusionCoupler te) {
        ForgeDirection dir = FusionNodes.facing(state).getOpposite();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

        FusionLink.claim(level, te, klystronCell(pos, rot), FusionLink.KLYSTRON_OUT);
        FusionLink.claim(level, te, plasmaCell(pos, rot), FusionLink.PLASMA_IN);
    }

    /** True: the coupler takes a full share of the plasma output, like a boiler or turbine would. */
    @Override public boolean receivesFusionPower() { return true; }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        ForgeDirection rot = FusionNodes.facing(getBlockState()).getOpposite().getRotation(ForgeDirection.UP);
        if (FusionLink.at(level, beamPos(worldPosition, rot), FusionLink.KLYSTRON_IN)
                instanceof TileEntityFusionTorus torus && torus.isLoaded() && !torus.isRemoved()) {
            torus.receiveKlystronEnergy(fusionPower);
        }
    }

    @Override
    public void setRemoved() {
        ForgeDirection rot = FusionNodes.facing(getBlockState()).getOpposite().getRotation(ForgeDirection.UP);
        FusionLink.release(level, klystronCell(worldPosition, rot), plasmaCell(worldPosition, rot));
        super.setRemoved();
    }

    /** The klystron side, one block across at +2, and the torus it feeds one block further. */
    private static BlockPos klystronCell(BlockPos pos, ForgeDirection rot) {
        return new BlockPos(pos.getX() + rot.offsetX, pos.getY() + 2, pos.getZ() + rot.offsetZ);
    }

    private static BlockPos beamPos(BlockPos pos, ForgeDirection rot) {
        return new BlockPos(pos.getX() + rot.offsetX * 2, pos.getY() + 2, pos.getZ() + rot.offsetZ * 2);
    }

    /** The plasma side, the opposite way across. */
    private static BlockPos plasmaCell(BlockPos pos, ForgeDirection rot) {
        return new BlockPos(pos.getX() - rot.offsetX, pos.getY() + 2, pos.getZ() - rot.offsetZ);
    }
}
