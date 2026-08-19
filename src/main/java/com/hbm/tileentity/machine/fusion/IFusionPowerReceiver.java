package com.hbm.tileentity.machine.fusion;

/** A machine that draws energy from a fusion torus over the plasma network. */
public interface IFusionPowerReceiver {

    /**
     * True if this receives the torus's <b>plasma</b> output, which is shared between every such
     * receiver on the net; false if it only takes neutron flux, which is not shared.
     */
    boolean receivesFusionPower();

    /**
     * @param fusionPower  the per-port plasma output, already scaled down by how many receivers share it
     * @param neutronPower the recipe's flat neutron flux (never shared)
     * @param r,g,b        the burning plasma's colour, for the receiver's own rendering
     */
    void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b);

    /**
     * True for the fusion collector, which grants the torus a bonus output rate on top of the plasma
     * it consumes. Upstream tested {@code instanceof TileEntityFusionCollector} directly; expressing it
     * as a capability keeps the torus from having to know about that one machine.
     */
    default boolean providesCollectorBonus() {
        return false;
    }
}
