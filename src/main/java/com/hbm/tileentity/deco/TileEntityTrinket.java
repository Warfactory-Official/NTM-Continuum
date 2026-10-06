package com.hbm.tileentity.deco;

import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The shared half of 1.7's {@code TileEntityBobble}, {@code TileEntitySnowglobe} and {@code TileEntityPlushie}:
 * the figure's type, sent whole on chunk load (1.7's description packet). 1.7 saved the type as a byte ordinal; the
 * port saves the type's name ({@code "strength"}), as every enum (AGENTS.md: numeric ids are never persisted,
 * History §19es-itemvariant). An old byte reads as the NONE type with one WARN.
 */
public abstract class TileEntityTrinket extends TileEntityLoadedBase {

    protected int type;

    protected TileEntityTrinket(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
        super(blockEntityType, pos, state);
    }

    /** The figure's enum constants, 1.7 order. */
    protected abstract Enum<?>[] typeValues();

    /** How many types the figure's enum has. */
    protected int typeCount() {
        return typeValues().length;
    }

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
        this.type = readType(input, typeValues());
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        com.hbm.util.SavedIds.putEnum(output, "type", typeValues()[typeOrdinal()]);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int readType(DataIn input, Enum<?>[] values) {
        return com.hbm.util.SavedIds.getEnum(input, "type", (Enum) values[0]).ordinal();
    }
}
