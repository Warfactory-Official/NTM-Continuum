package com.hbm.blocks.generic;

import net.minecraft.world.level.block.IronBarsBlock;

/**
 * 1.7 {@code BlockNTMGlassPane}: a glass pane that also joins onto the HBM glass blocks. Vanilla's pane
 * already attaches to any block with a sturdy face, which every {@code BlockNTMGlass} full cube has, so
 * the old {@code canPaneConnectTo} extension needs no override here ({@code attachsTo} is final).
 * Whether it drops itself ({@code doesDrop}) is the loot table's business: the laminate pane does,
 * the glass pane does not.
 */
public class BlockNTMGlassPane extends IronBarsBlock {

    public BlockNTMGlassPane(Properties properties) {
        super(properties);
    }
}
