package com.hbm.api.tile;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * A BlockEntity holding a heat reservoir ("TU") that heat consumers can drain, the 1.12.2
 * {@code IHeatSource}. Heat is not networked: a consumer (crucible, another heater) polls the
 * BlockEntity <b>directly below itself</b>, reads {@link #getHeatStored()}, transfers some fraction and
 * calls {@link #useUpHeat(int)}.
 */
public interface IHeatSource {

    int getHeatStored();

    void useUpHeat(int heat);

    /** What 1.7's consumers found at {@code pos}: a source block entity, or a cell its machine flags with {@code heatSourceAt}. */
    static @Nullable IHeatSource at(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof IHeatSource source) return source;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null) return null;
        BlockEntity owner = level.getBlockEntity(core);
        return owner instanceof IHeatSource source && BlockMultiblockCore.vendsHeatAt(owner, pos) ? source : null;
    }
}
