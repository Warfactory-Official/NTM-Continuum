package com.hbm.blocks.generic;

import net.minecraft.world.level.block.Block;

/**
 * Depth rock — the 1.12.2 {@code BlockDepth}. Set <b>unbreakable</b> ({@code setBlockUnbreakable()})
 * with an explosion resistance of only 10, so it cannot be mined by hand at any tool tier but is
 * trivially removed by explosives. Upstream, the sole exception is a held {@code IDepthRockTool}
 * (the depth drill), which drops the relative hardness to {@code 1/100}.
 *
 * <p><b>Deferred:</b> the {@code IDepthRockTool} seam. No depth-drill item is ported yet, so depth
 * rock and every {@link BlockDepthOre} on top of it are currently explosion-only — which is exactly
 * how the original behaves without that tool. Wire the hardness override in when the drill lands.
 */
public class BlockDepth extends Block {

    public BlockDepth(Properties properties) {
        super(properties);
    }
}
