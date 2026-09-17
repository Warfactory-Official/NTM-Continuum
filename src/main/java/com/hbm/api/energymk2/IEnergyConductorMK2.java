package com.hbm.api.energymk2;

/**
 * A block entity that conducts power. The conduction itself is a cell of the level's power graph,
 * minted and dropped by the block; what this marks is that a neighbour may connect to it.
 */
public interface IEnergyConductorMK2 extends IEnergyConnectorMK2 {

}
