package com.hbm.api.energymk2;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.graph.PortClaims;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** If it sends energy, use this. The contract is on {@link IEnergyHandlerMK2}; this adds provision. */
public interface IEnergyProviderMK2 extends IEnergyHandlerMK2 {

    /** 1.7 {@code IEnergyProviderMK2.getProviderSpeed}: a provider sends up to its capacity. */
    @Override
    default long getProviderSpeed() {
        return this.getMaxPower();
    }

    /**
     * 1.7's {@code tryProvide}: makes this machine a provider of the net of the cable at the given
     * coordinates for the next three seconds (a {@link PortClaims} claim; the graph finds the cable's
     * faces, the claim says which of them carry this machine's output), and pushes energy straight into a
     * receiver standing there, bypassing any network.
     *
     * @param dir the {@link ForgeDirection} from this provider to the target.
     */
    default void tryProvide(Level world, int x, int y, int z, ForgeDirection dir) {
        PortClaims.claim(world, x, y, z, dir, this, PortClaims.POWER, PortClaims.PROVIDE);
        BlockPos targetPos = new BlockPos(x, y, z);
        BlockEntity targetTE = world.getBlockEntity(targetPos);
        if (targetTE == null) return;

        if (targetTE instanceof IEnergyReceiverMK2 rec && targetTE != this) {
            if (rec.canConnect(dir.getOpposite()) && rec.allowDirectProvision()) {
                long canProvide = Math.min(this.getPower(), this.getProviderSpeed());
                long canReceive = Math.min(rec.getMaxPower() - rec.getPower(), rec.getReceiverSpeed());
                long toTransfer = Math.min(canProvide, canReceive);

                if (toTransfer > 0) {
                    long rejected = rec.transferPower(toTransfer, false);
                    long accepted = toTransfer - rejected;
                    if (accepted > 0) {
                        this.usePower(accepted);
                    }
                }
            }
        }
        // NOTE: the 1.12.2 Forge-Energy (HE<->RF) interop branch is intentionally dropped here.
        // Cross-loader energy interop will be re-added later as a per-loader capability bridge.
    }

    default void tryProvide(Level world, BlockPos pos, ForgeDirection dir) {
        tryProvide(world, pos.getX(), pos.getY(), pos.getZ(), dir);
    }

    default void tryProvide(Level world, DirPos pos) {
        tryProvide(world, pos.getPos().getX(), pos.getPos().getY(), pos.getPos().getZ(), pos.getDir());
    }

    /** 1.7 {@code net.removeProvider(this)} at the cable at {@code x, y, z}: stop providing there now. */
    default void tryUnprovide(Level world, int x, int y, int z) {
        PortClaims.withdraw(world, x, y, z, this, PortClaims.POWER, PortClaims.PROVIDE);
    }
}
