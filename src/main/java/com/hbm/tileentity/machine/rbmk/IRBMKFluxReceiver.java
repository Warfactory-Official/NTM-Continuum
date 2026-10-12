package com.hbm.tileentity.machine.rbmk;

import com.hbm.handler.neutron.NeutronStream;

/** A column which accepts a neutron stream and reacts during its own tick. */
public interface IRBMKFluxReceiver {
    // 1.7 declared the neutron types here as IRBMKFluxReceiver.NType; the port's one copy is ItemRBMKRod.NType (rods
    // and columns read the same enum), a second identical copy here had no users.
    void receiveFlux(NeutronStream stream);
}
