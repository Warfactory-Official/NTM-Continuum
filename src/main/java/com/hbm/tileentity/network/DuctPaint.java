package com.hbm.tileentity.network;

import com.hbm.util.SavedIds;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The block a coated duct is painted as: 1.7 {@code TileEntityPipePaintable}'s block and meta, one state here, saved by
 * name. Shared by the coated duct and the coated exhaust duct.
 */
public final class DuctPaint {

    /** A block entity that carries a paint. */
    public interface Holder {
        DuctPaint paint();
    }

    private @Nullable BlockState paint;

    public @Nullable BlockState get() {
        return paint;
    }

    /** Sets the paint and sends the duct to the clients, which re-mesh it. */
    public void set(BlockEntity owner, @Nullable BlockState paint) {
        this.paint = paint;
        owner.setChanged();
        Level level = owner.getLevel();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), 3);
        }
    }

    public void save(DataOut output) {
        if (paint != null) output.store("paint", BlockState.CODEC, paint);
    }

    /** Reads the paint; true when it changed. */
    public boolean load(DataIn input) {
        BlockState old = paint;
        paint = input.read("paint", BlockState.CODEC).orElse(null);
        return old != paint;
    }

    /** 1.7: the copy tool takes the paint alone, not the fluid. */
    public CompoundTag settings() {
        CompoundTag nbt = new CompoundTag();
        if (paint != null) SavedIds.putState(nbt, "paintblock", paint);
        return nbt;
    }

    public void paste(BlockEntity owner, CompoundTag nbt) {
        if (Tags.contains(nbt, "paintblock")) set(owner, SavedIds.getState(nbt, "paintblock", paint));
    }
}
