package com.hbm.tileentity.machine.pile;

import com.hbm.blocks.machine.pile.BlockPileDevice;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared body of the three devices that bolt onto a Chicago Pile from the outside. Each one faces a
 * single channel mouth, so it carries the index of the channel it found and the horizontal direction it
 * points, both read from the low two bits of its block meta.
 */
public abstract class TileEntityPileDeviceBase extends TileEntityLoadedBase {

    /** Index of the channel this device is docked to, for the look overlay and radio readouts. */
    public int chanNum;

    protected TileEntityPileDeviceBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ForgeDirection getOrientation() {
        BlockState state = getBlockState();
        int meta = state.hasProperty(BlockPileDevice.META) ? state.getValue(BlockPileDevice.META) : 0;
        return ForgeDirection.getOrientation(meta % 4 + 2);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(this.chanNum);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.chanNum = buf.readInt();
    }
}
