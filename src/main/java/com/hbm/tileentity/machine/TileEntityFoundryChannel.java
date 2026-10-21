package com.hbm.tileentity.machine;

import com.hbm.util.SavedIds;
import dev.golem.core.data.Tags;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.blocks.machine.FoundryChannel;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Foundry channel (1.12.2 {@code TileEntityFoundryChannel}): carries molten metal horizontally.
 * Every 5 ticks it first tries to {@code flow} into an adjacent non-channel acceptor (caster, outlet),
 * else equalises/swaps with neighbouring channels. A contiguous run refuses pours of a
 * second material while any of its channels still holds the first. Capacity 144 quanta (2 ingots).
 */
public class TileEntityFoundryChannel extends TileEntityFoundryBase {

    public int nextUpdate;
    public int lastFlow = 0;

    public TileEntityFoundryChannel(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_CHANNEL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFoundryChannel te) {
        if (te.type == null && te.amount != 0) {
            te.amount = 0;
        }

        te.nextUpdate--;

        if (te.nextUpdate <= 0 && te.amount > 0 && te.type != null) {
            boolean hasOp = false;
            te.nextUpdate = 5;

            List<Integer> ints = new ArrayList<>(List.of(2, 3, 4, 5));
            Collections.shuffle(ints);
            if (te.lastFlow > 0) {
                ints.remove((Integer) te.lastFlow);
                ints.add(te.lastFlow);
            }

            for (Integer i : ints) {
                ForgeDirection dir = ForgeDirection.getOrientation(i);
                BlockPos target = pos.offset(dir.offsetX, 0, dir.offsetZ);
                Block b = level.getBlockState(target).getBlock();

                if (b instanceof ICrucibleAcceptor acc && !(b instanceof FoundryChannel)) {
                    if (acc.canAcceptPartialFlow(level, target, dir.getOpposite(), new MaterialStack(te.type, te.amount))) {
                        MaterialStack left = acc.flow(level, target, dir.getOpposite(), new MaterialStack(te.type, te.amount));
                        if (left == null) {
                            te.type = null;
                            te.amount = 0;
                        } else {
                            te.amount = left.amount;
                        }
                        te.setChanged();
                        hasOp = true;
                        break;
                    }
                }
            }

            if (!hasOp) {
                for (Integer i : ints) {
                    ForgeDirection dir = ForgeDirection.getOrientation(i);
                    BlockEntity b = level.getBlockEntity(pos.offset(dir.offsetX, 0, dir.offsetZ));

                    if (b instanceof TileEntityFoundryChannel acc) {
                        if (acc.type == null || acc.type == te.type || acc.amount == 0) {
                            acc.type = te.type;
                            acc.lastFlow = dir.getOpposite().ordinal();

                            if (level.getRandom().nextInt(5) == 0 || te.amount == 1) { // force swaps with single quanta to keep them moving
                                // 1:5 chance the fill states are swapped, which promotes faster spreading
                                int buf = te.amount;
                                te.amount = acc.amount;
                                acc.amount = buf;
                            } else {
                                // otherwise, equalise the neighbours
                                int diff = te.amount - acc.amount;
                                if (diff > 0) {
                                    diff /= 2;
                                    te.amount -= diff;
                                    acc.amount += diff;
                                }
                            }
                            te.setChanged();
                            acc.setChanged();
                            acc.networkPackNT();
                        }
                    }
                }
            }
        }

        if (te.amount == 0) {
            te.lastFlow = 0;
            te.nextUpdate = 5;
        }

        te.networkPackNT();
    }

    @Override
    public int getCapacity() {
        return MaterialShapes.INGOT.quantity * 2;
    }

    /** A pour is refused while any channel of the connected run holds a different material. */
    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) {
        List<TileEntityFoundryChannel> visited = new ArrayList<>();
        visited.add(this);
        for (int i = 0; i < visited.size(); i++) {
            TileEntityFoundryChannel channel = visited.get(i);
            if (channel.type != null && channel.amount > 0 && channel.type != stack.material) return false;
            for (ForgeDirection dir : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST}) {
                if (world.getBlockEntity(channel.worldPosition.offset(dir.offsetX, 0, dir.offsetZ)) instanceof TileEntityFoundryChannel next && !visited.contains(next)) {
                    visited.add(next);
                }
            }
        }
        return super.canAcceptPartialPour(world, pos, dX, dY, dZ, side, stack);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        SavedIds.putEnum(output, "flow", ForgeDirection.getOrientation(this.lastFlow)); // by name, never an ordinal
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.lastFlow = SavedIds.getEnum(input, "flow", ForgeDirection.DOWN).ordinal();
    }
}
