package com.hbm.tileentity.machine.fusion;

import com.hbm.capability.MachineCaps;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The geometry every machine on the fusion beams shares.
 *
 * <p>Upstream wrote the same twenty lines of node bookkeeping into each of these machines and left a
 * comment about it ("christ on the cross why didn't i just make a baseclass to shove this crap into");
 * this was that base. There is far less of it now that {@link FusionLink} answers the question directly
 * rather than through a membership graph, and what is left is the positions themselves.
 */
public final class FusionNodes {

    private FusionNodes() {}

    /**
     * Where a klystron-style emitter claims its end of the beam: four blocks out the back at +2. Shared
     * by the klystron and its creative twin, which upstream expressed as a static on the former.
     */
    public static BlockPos emitter(BlockPos pos, ForgeDirection facing) {
        ForgeDirection dir = facing.getOpposite();
        return new BlockPos(pos.getX() + dir.offsetX * 4, pos.getY() + 2, pos.getZ() + dir.offsetZ * 4);
    }

    /** Where that emitter's beam lands: one block further out than it claims. */
    public static BlockPos emitterBeam(BlockPos pos, ForgeDirection facing) {
        ForgeDirection dir = facing.getOpposite();
        return new BlockPos(pos.getX() + dir.offsetX * 5, pos.getY() + 2, pos.getZ() + dir.offsetZ * 5);
    }

    /**
     * Hands klystron energy to the torus this emitter is aimed at.
     *
     * @return whether a torus was actually reached - the creative klystron shows this as its "connected"
     *         light, and it is the only feedback a player gets that the beam is landing.
     */
    public static boolean provideKyU(Level level, BlockPos pos, ForgeDirection facing, long output) {
        BlockEntity landed = FusionLink.at(level, emitterBeam(pos, facing), MachineCaps.KLYSTRON_IN);
        if (!(landed instanceof TileEntityFusionTorus torus) || torus.isRemoved() || !torus.isLoaded()) {
            return false;
        }
        torus.receiveKlystronEnergy(output);
        return true;
    }

    /**
     * The direction a dummyable machine faces, from its core meta. Every fusion machine positions its
     * beam relative to this, and upstream spelled out {@code getBlockMetadata() - 10} inline each time.
     */
    public static ForgeDirection facing(BlockState state) {
        return MachineCore.facing(state);
    }
}
