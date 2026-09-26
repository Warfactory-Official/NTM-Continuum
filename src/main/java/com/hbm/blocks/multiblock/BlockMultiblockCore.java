package com.hbm.blocks.multiblock;

import dev.golem.core.multiblock.MultiblockCore;

/** HBM folded core: Core {@link MultiblockCore} + duct answer. */
public abstract class BlockMultiblockCore extends MultiblockCore implements FoldedFluidPorts {

    protected BlockMultiblockCore(Properties properties) {
        super(properties);
    }
}
