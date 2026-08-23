package com.hbm.tileentity.machine.rbmk;

import com.hbm.handler.neutron.NeutronStream;

/** A column which accepts a neutron stream and reacts during its own tick. */
public interface IRBMKFluxReceiver {
    void receiveFlux(NeutronStream stream);

    enum NType {
        SLOW("trait.rbmk.neutron.slow"), FAST("trait.rbmk.neutron.fast"), ANY("trait.rbmk.neutron.any");
        public final String unlocalized;
        NType(String unlocalized) { this.unlocalized = unlocalized; }
    }
}
