package com.hbm.api.fluidmk2;

/**
 * A block entity that conducts fluid. The conduction itself is a cell of the level's fluid graph, minted
 * and dropped by the block entity; what this marks is that it is plumbing rather than a machine, so a
 * net never gathers it as an endpoint of its own.
 */
public interface IFluidPipeMK2 extends IFluidConnectorMK2 {

}
