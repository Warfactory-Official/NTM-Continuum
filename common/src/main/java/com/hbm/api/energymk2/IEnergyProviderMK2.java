package com.hbm.api.energymk2;

import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IEnergyProviderMK2 extends IEnergyHandlerMK2 {

    default void usePower(long power) {
        this.setPower(this.getPower() - power);
    }

    default long getProviderSpeed() {
        return this.getMaxPower();
    }

    default void tryProvide(Level world, int x, int y, int z, ForgeDirection dir) {
        BlockPos targetPos = new BlockPos(x, y, z);
        BlockEntity targetTE = world.getBlockEntity(targetPos);
        if (targetTE == null) return;

        if (targetTE instanceof IEnergyConductorMK2 con) {
            if (con.canConnect(dir.getOpposite())) {
                Nodespace.PowerNode node = Nodespace.getNode(world, targetPos);
                if (node != null && node.net != null) {
                    node.net.addProvider(this);
                }
            }
        }

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

    }

    default void tryProvide(Level world, BlockPos pos, ForgeDirection dir) {
        tryProvide(world, pos.getX(), pos.getY(), pos.getZ(), dir);
    }
}
