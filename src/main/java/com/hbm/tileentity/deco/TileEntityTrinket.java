package com.hbm.tileentity.deco;

import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The shared half of 1.7's {@code TileEntityBobble}, {@code TileEntitySnowglobe} and {@code TileEntityPlushie}:
 * the figure's type, persisted as the byte {@code type} and sent whole on chunk load (1.7's description
 * packet). The type is read back as {@code abs(type) % count}, exactly as 1.7 did.
 */
public abstract class TileEntityTrinket extends TileEntityLoadedBase {

    protected int type;

    protected TileEntityTrinket(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
        super(blockEntityType, pos, state);
    }

    /** How many types the figure's enum has. */
    protected abstract int typeCount();

    public int typeOrdinal() {
        return Math.floorMod(type, typeCount());
    }

    /** Sets the type and pushes it to watching clients (1.7 {@code markDirty} plus the description packet). */
    public void setTypeOrdinal(int ordinal) {
        this.type = Math.abs(ordinal) % typeCount();
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.type = Math.abs(input.getByteOr("type", (byte) 0)) % typeCount();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putByte("type", (byte) typeOrdinal());
    }
}
