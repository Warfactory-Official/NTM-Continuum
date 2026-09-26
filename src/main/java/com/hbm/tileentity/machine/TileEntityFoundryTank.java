package com.hbm.tileentity.machine;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.tileentity.ModFluidNetBlockEntities;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.api.material.MaterialStack;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The 1.7 {@code TileEntityFoundryTank}: a four-block-quanta molten store. Every 5–10 ticks it drains
 * down into a tank below, else flows sideways into a random accepting block that is not a channel,
 * else levels with (or, one time in five, swaps contents with) a random neighbouring tank.
 */
public class TileEntityFoundryTank extends TileEntityFoundryBase {

    public int nextUpdate;

    public TileEntityFoundryTank(BlockPos pos, BlockState state) {
        super(ModFluidNetBlockEntities.FOUNDRY_TANK.get(), pos, state);
    }

    @Override
    public int getCapacity() {
        return MaterialShapes.BLOCK.quantity * 4; // MaterialShapes.BLOCK.q(4)
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFoundryTank tank) {
        if (tank.type == null && tank.amount != 0) tank.amount = 0;

        tank.nextUpdate--;

        if (tank.nextUpdate <= 0 && tank.amount > 0 && tank.type != null) {
            boolean hasOp = false;
            tank.nextUpdate = level.getRandom().nextInt(6) + 5;

            BlockEntity te = level.getBlockEntity(pos.below());
            if (te instanceof TileEntityFoundryTank below) {
                if ((below.type == null || below.type == tank.type) && below.amount < below.getCapacity()) {
                    below.type = tank.type;
                    int toFill = Math.min(tank.amount, below.getCapacity() - below.amount);
                    tank.amount -= toFill;
                    below.amount += toFill;
                    below.setChanged();
                    hasOp = true;
                }
            }

            List<Integer> ints = new ArrayList<>(List.of(2, 3, 4, 5));
            Collections.shuffle(ints);

            if (!hasOp) {
                for (Integer i : ints) {
                    ForgeDirection dir = ForgeDirection.getOrientation(i);
                    BlockPos at = pos.offset(dir.offsetX, 0, dir.offsetZ);
                    Block b = level.getBlockState(at).getBlock();
                    if (b instanceof ICrucibleAcceptor acc && b != ModMachineBlocks.foundry_channel.get()) {
                        if (acc.canAcceptPartialFlow(level, at, dir.getOpposite().toDirection(), new MaterialStack(tank.type, tank.amount))) {
                            MaterialStack left = acc.flow(level, at, dir.getOpposite().toDirection(), new MaterialStack(tank.type, tank.amount));
                            if (left == null) {
                                tank.type = null;
                                tank.amount = 0;
                            } else {
                                tank.amount = left.amount;
                            }
                            hasOp = true;
                            break;
                        }
                    }
                }
            }

            if (!hasOp) {
                for (Integer i : ints) {
                    ForgeDirection dir = ForgeDirection.getOrientation(i);
                    BlockEntity b = level.getBlockEntity(pos.offset(dir.offsetX, 0, dir.offsetZ));
                    if (b instanceof TileEntityFoundryTank acc) {
                        if (acc.type == null || acc.type == tank.type || acc.amount == 0) {
                            acc.type = tank.type;
                            if (level.getRandom().nextInt(5) == 0) {
                                // 1:4 chance that the fill states are simply swapped
                                // this promotes faster spreading and prevents spread limits
                                int buf = tank.amount;
                                tank.amount = acc.amount;
                                acc.amount = buf;
                            } else {
                                int diff = tank.amount - acc.amount;
                                if (diff > 0) {
                                    diff /= 2;
                                    tank.amount -= diff;
                                    acc.amount += diff;
                                }
                            }
                            acc.setChanged();
                        }
                    }
                }
            }
            tank.setChanged();
        }
        tank.syncState();
    }
}
