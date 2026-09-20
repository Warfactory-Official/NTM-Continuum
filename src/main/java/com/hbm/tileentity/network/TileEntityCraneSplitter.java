package com.hbm.tileentity.network;

import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import com.hbm.tileentity.ModConveyorBlockEntities;

/** 1.7 {@code tileentity/network/TileEntityCraneSplitter}: the deal state and the two lane ratios. */
public class TileEntityCraneSplitter extends TileEntityLoadedBase {

    /* false: left belt is preferred, true: right belt is preferred */
    private boolean position;
    private byte remaining; // count until position swaps

    public byte leftRatio = 1;
    public byte rightRatio = 1;

    public TileEntityCraneSplitter(BlockPos pos, BlockState state) {
        super(ModConveyorBlockEntities.CRANE_SPLITTER.get(), pos, state);
    }

    /** Splits the input stack into two, based on the current ratio and internal state: {left, right}. */
    public List<ItemStack> splitStack(ItemStack stack) {
        int left = 0;
        int right = 0;
        int count = stack.getCount();

        if (remaining <= 0) remaining = position ? rightRatio : leftRatio;

        while (count > 0) {
            int toExtract = Math.min(remaining, count);

            remaining -= toExtract;
            count -= toExtract;
            if (position) right += toExtract; else left += toExtract;

            if (remaining <= 0) {
                position = !position;
                remaining = position ? rightRatio : leftRatio;
            }
        }

        setChanged();
        return List.of(stack.copyWithCount(left), stack.copyWithCount(right));
    }

    public void serverTick() {
        networkPackNT();
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        position = input.getBooleanOr("pos", false);
        remaining = input.getByteOr("count", (byte) 0);
        // Make sure existing conveyors are initialised with ratios
        leftRatio = (byte) Math.max(input.getByteOr("left", (byte) 1), 1);
        rightRatio = (byte) Math.max(input.getByteOr("right", (byte) 1), 1);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("pos", position);
        output.putByte("count", remaining);
        output.putByte("left", leftRatio);
        output.putByte("right", rightRatio);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeByte(leftRatio);
        buf.writeByte(rightRatio);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        leftRatio = buf.readByte();
        rightRatio = buf.readByte();
    }
}
