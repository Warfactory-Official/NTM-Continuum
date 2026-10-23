package com.hbm.tileentity.network;

import com.hbm.tileentity.ModFluidNetBlockEntities;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code FluidDuctPaintable.TileEntityPipePaintable}: the coated duct and the block it is painted as. */
public class TileEntityPipePaintable extends TileEntityFluidDuctVariant implements DuctPaint.Holder {

    private final DuctPaint paint = new DuctPaint();

    public TileEntityPipePaintable(BlockPos pos, BlockState state) {
        super(ModFluidNetBlockEntities.PAINTABLE.get(), pos, state);
    }

    @Override
    public DuctPaint paint() {
        return paint;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        paint.save(output);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        if (paint.load(input)) remesh();
    }

    @Override
    public @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
        return paint.settings();
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        paint.paste(this, nbt);
    }
}
