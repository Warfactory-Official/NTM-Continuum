package com.hbm.blocks.generic;

import net.minecraft.world.level.block.IronBarsBlock;

/**
 * 1.7 {@code BlockNTMGlassPane}: a glass pane that also joins onto the HBM glass blocks. Vanilla's pane
 * already attaches to any block with a sturdy face, which every {@code BlockNTMGlass} full cube has, so
 * the old {@code canPaneConnectTo} extension needs no override here ({@code attachsTo} is final).
 * Drops are the loot table's business: both panes are silk-touch-only, because 1.7 passes
 * {@code false} as vanilla {@code BlockPane}'s "drops itself" flag (so {@code getItemDropped} is null
 * whatever {@code doesDrop} says) and {@code BlockPane.canSilkHarvest()} is true.
 */
public class BlockNTMGlassPane extends IronBarsBlock {

    public BlockNTMGlassPane(Properties properties) {
        super(properties);
    }
}
