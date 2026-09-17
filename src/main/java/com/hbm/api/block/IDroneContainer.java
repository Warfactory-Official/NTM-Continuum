package com.hbm.api.block;

import net.minecraft.world.Container;

/**
 * The block-entity side of the delivery-drone network, as the drones see it.
 *
 * <p>1.7 had {@code EntityRequestDrone} reach directly into the concrete {@code TileEntityDroneProvider},
 * {@code TileEntityDroneRequester} and {@code TileEntityDroneDock} and index their raw {@code slots} arrays.
 * Those three block entities are not ported yet, so the coupling is stated as three role interfaces instead -
 * the drone is complete, and each block entity becomes a drone participant by implementing one of them. The
 * same shape the port already uses for {@code IRadarDetectable} and {@code IConveyorBelt}.
 *
 * <p>The 1.7 slot windows are folded into the returned {@link Container}: the requester's delivery window was
 * slots 9..17 of a larger inventory, so an implementor returns a view of just those.
 */
public interface IDroneContainer {

    /** Supplies stacks to a drone that has been sent to fetch them. 1.7 {@code TileEntityDroneProvider}. */
    interface Provider extends IDroneContainer {
        Container droneOfferInventory();
    }

    /** Receives what a drone carried in. 1.7 {@code TileEntityDroneRequester}, slots 9..17. */
    interface Requester extends IDroneContainer {
        Container droneDeliveryInventory();
    }

    /** Parks idle drones and their cargo. 1.7 {@code TileEntityDroneDock}. */
    interface Dock extends IDroneContainer {
        Container droneDockInventory();
    }
}
