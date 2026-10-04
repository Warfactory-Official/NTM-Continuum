package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.blocks.machine.FoundryChannel;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Foundry channel (1.12.2 {@code TileEntityFoundryChannel}) — carries molten metal horizontally.
 * Every 5 ticks it first tries to {@code flow} into an adjacent non-channel acceptor (caster, outlet),
 * else equalises/swaps with neighbouring channels. A contiguous run of channels keeps a single
 * {@code neighborType} (clog protection: a run holding one material rejects pours of another), which
 * un-propagates 100 ticks after the run empties. Capacity 144 quanta (2 ingots).
 */
public class TileEntityFoundryChannel extends TileEntityFoundryBase {

    public int nextUpdate;
    public int lastFlow = 0;

    protected @Nullable NTMMaterial neighborType;
    protected boolean hasCheckedNeighbors;
    protected int unpropagateTime;

    public TileEntityFoundryChannel(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_CHANNEL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFoundryChannel te) {
        // Initialise before allowing pours, so newly added channels avoid causing clog feeds.
        if (!te.hasCheckedNeighbors) {
            List<TileEntityFoundryChannel> visited = new ArrayList<>();
            visited.add(te);
            te.neighborType = te.checkNeighbors(visited);
            te.hasCheckedNeighbors = true;
        }

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
                            te.propagateMaterial(null);
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
                                // 1:5 chance the fill states are simply swapped — promotes faster spreading
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

        if (te.neighborType != null && te.amount == 0) te.unpropagateTime++;

        // every 5 seconds do an unprop test; only occurs once per contiguous channel per 5s (the prop resets the timers)
        if (te.unpropagateTime > 100) {
            te.propagateMaterial(null);
        }

        if (te.amount == 0) {
            te.lastFlow = 0;
            te.nextUpdate = 5;
        } else {
            te.unpropagateTime = 0;
        }

        te.networkPackNT();
    }

    @Override
    public int getCapacity() {
        return MaterialShapes.INGOT.quantity * 2;
    }

    /**
     * Channels accept pouring as normal, except when the contiguous run already holds a different
     * material (clog protection).
     */
    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) {
        if (!hasCheckedNeighbors || (neighborType != null && neighborType != stack.material)) return false;
        return super.canAcceptPartialPour(world, pos, dX, dY, dZ, side, stack);
    }

    /** Upon pouring, propagate the material type along contiguous channels. */
    @Override
    public @Nullable MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) {
        propagateMaterial(stack.material);
        return super.pour(world, pos, dX, dY, dZ, side, stack);
    }

    public void propagateMaterial(@Nullable NTMMaterial propType) {
        if (propType != null && neighborType != null) return; // optimise away pours that change nothing

        List<TileEntityFoundryChannel> visited = new ArrayList<>();
        visited.add(this);

        boolean hasMaterial = propagateMaterial(propType, visited, false);

        // fully clear → safe to unassign the contiguous channel type
        if (propType == null && !hasMaterial) {
            for (TileEntityFoundryChannel acc : visited) {
                acc.neighborType = null;
            }
        }
    }

    protected boolean propagateMaterial(@Nullable NTMMaterial propType, List<TileEntityFoundryChannel> visited, boolean hasMaterial) {
        if (propType != null) {
            neighborType = propType;
        } else {
            unpropagateTime = 0;
        }

        if (level == null) return hasMaterial;

        for (ForgeDirection dir : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST}) {
            BlockEntity b = level.getBlockEntity(worldPosition.offset(dir.offsetX, 0, dir.offsetZ));

            if (b instanceof TileEntityFoundryChannel acc && !visited.contains(acc)) {
                visited.add(acc);
                if (acc.amount > 0) hasMaterial = true;
                hasMaterial = acc.propagateMaterial(propType, visited, hasMaterial);
            }
        }

        return hasMaterial;
    }

    protected @Nullable NTMMaterial checkNeighbors(List<TileEntityFoundryChannel> visited) {
        if (neighborType != null) return neighborType;
        if (level == null) return null;

        for (ForgeDirection dir : new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST}) {
            BlockEntity b = level.getBlockEntity(worldPosition.offset(dir.offsetX, 0, dir.offsetZ));

            if (b instanceof TileEntityFoundryChannel acc && !visited.contains(acc)) {
                visited.add(acc);
                NTMMaterial neighborMaterial = acc.checkNeighbors(visited);
                if (neighborMaterial != null) return neighborMaterial;
            }
        }

        return null;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        com.hbm.util.SavedIds.putEnum(output, "flow", ForgeDirection.getOrientation(this.lastFlow)); // by name, never an ordinal
        Mats.write(output, "nType", this.neighborType);
        output.putBoolean("init", hasCheckedNeighbors);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.lastFlow = com.hbm.util.SavedIds.getEnum(input, "flow", ForgeDirection.DOWN).ordinal();
        this.neighborType = Mats.read(input, "nType");
        this.hasCheckedNeighbors = input.getBooleanOr("init", false);
    }
}
