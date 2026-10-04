package com.hbm.blocks.generic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * HBM's glass family — original-hbm (1.7.10) {@code BlockNTMGlass}: a {@code BlockBreakable} with
 * {@code lightOpacity = 0}, i.e. a vanilla-glass-like block that does not cull its neighbours' faces and
 * lets light through.
 *
 * <p>What it drops is the loot table's business, declared per block: 1.7 {@code quantityDropped} is 1 only
 * when {@code doesDrop} is set (reinforced laminate, quartz glass) and {@code canSilkHarvest()} is
 * {@code true} for all of them, so the rest are silk-touch-only. {@code isRadResistant} — the
 * {@code IRadResistantBlock} marker used by the chunk radiation engine — is carried through.
 */
public class BlockNTMGlass extends Block implements com.hbm.interfaces.IRadResistantBlock {

    private final boolean radResistant;

    public BlockNTMGlass(Properties properties, boolean radResistant) {
        super(properties);
        this.radResistant = radResistant;
    }

    @Override
    public boolean isRadResistant(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        return radResistant;
    }
}
