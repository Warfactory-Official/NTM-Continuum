package com.hbm.blocks.multiblock;

import dev.golem.core.multiblock.MultiblockMachine;

/** HBM folded machine (cells = own block): Core {@link MultiblockMachine} + duct answer. */
public abstract class BlockMultiblockMachine extends MultiblockMachine implements FoldedFluidPorts {

    protected BlockMultiblockMachine(Properties properties) {
        super(properties);
    }
}
