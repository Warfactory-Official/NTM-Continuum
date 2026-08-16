package com.hbm.blocks.generic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * HBM's glass family — the 1.12.2 {@code BlockNTMGlass}. A {@code BlockBreakable} with
 * {@code translucent = true} and {@code lightOpacity = 0}, i.e. a vanilla-glass-like block that does
 * not cull its neighbours' faces and lets light through.
 *
 * <p>The {@code doesDrop} flag decides whether breaking it yields the block or nothing (vanilla glass
 * behaviour). {@code isRadResistant} — the {@code IRadResistantBlock} marker used by the chunk
 * radiation engine — is carried through so the rad-resistant panes behave correctly once more of the
 * family lands.
 *
 * <p>Only {@code glass_quartz} is registered so far (it gates the SILEX assembly recipe and all five
 * laser-crystal recipes); the other eleven variants come with a later glass wave.
 */
public class BlockNTMGlass extends Block implements com.hbm.interfaces.IRadResistantBlock {

    private final boolean doesDrop;
    private final boolean radResistant;

    public BlockNTMGlass(Properties properties, boolean doesDrop, boolean radResistant) {
        super(properties);
        this.doesDrop = doesDrop;
        this.radResistant = radResistant;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return doesDrop ? List.of(new ItemStack(this)) : List.of();
    }

    @Override
    public boolean isRadResistant(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        return radResistant;
    }
}
