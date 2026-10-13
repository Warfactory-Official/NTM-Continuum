package com.hbm.blocks.generic;

import com.hbm.interfaces.IRadResistantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * HBM's glass family, the 1.7.10 {@code BlockNTMGlass}: a {@code BlockBreakable} with
 * {@code lightOpacity = 0}, i.e. a vanilla-glass-like block that does not cull its neighbours' faces and
 * lets light through.
 *
 * <p>What it drops is the loot table's business, declared per block: 1.7 {@code quantityDropped} is 1 only
 * when {@code doesDrop} is set (reinforced laminate, quartz glass) and {@code canSilkHarvest()} is
 * {@code true} for all of them, so the rest are silk-touch-only. {@code isRadResistant}, the
 * {@code IRadResistantBlock} marker used by the chunk radiation engine, is carried through.
 */
public class BlockNTMGlass extends Block implements IRadResistantBlock {

    private final boolean radResistant;

    public BlockNTMGlass(Properties properties, boolean radResistant) {
        super(properties);
        this.radResistant = radResistant;
    }

    @Override
    public boolean isRadResistant(BlockGetter level, BlockPos pos) {
        return radResistant;
    }

    /**
     * 1.7 {@code BlockBreakable(name, material, false)}: a face against the same glass is not drawn, so a wall reads
     * as one pane (and the connected-texture border only shows on its outside).
     */
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, side);
    }
}
