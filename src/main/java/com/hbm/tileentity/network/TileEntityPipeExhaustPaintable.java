package com.hbm.tileentity.network;

import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code FluidDuctPaintableBlockExhaust.TileEntityPipeExhaustPaintable}: the coated exhaust duct and its paint. */
public class TileEntityPipeExhaustPaintable extends TileEntityPipeExhaust implements DuctPaint.Holder {

    private final DuctPaint paint = new DuctPaint();

    public TileEntityPipeExhaustPaintable(BlockPos pos, BlockState state) {
        super(pos, state);
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
