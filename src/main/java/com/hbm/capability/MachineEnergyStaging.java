package com.hbm.capability;

import com.hbm.api.energymk2.EnergyConversion;
import com.hbm.api.energymk2.IEnergyHandlerMK2;

/**
 * One owner's uncommitted whole-HE crossings, shared by its real FE faces. A relay (no buffer of its own, the charger)
 * only takes, and takes what its own simulated transfer would accept.
 */
final class MachineEnergyStaging {
    private static final int RELAY_PASSES = 256;
    private final IEnergyHandlerMK2 machine;
    private final boolean relay;
    private long insertedHe, extractedHe;

    MachineEnergyStaging(IEnergyHandlerMK2 machine) {
        this.machine = machine;
        this.relay = !machine.ownsEnergyBuffer();
    }

    private void checkBuffer() {
        if (relay) return;
        var held = machine.getPower();
        var ceiling = machine.getMaxPower();
        if (held < 0 || ceiling < 0 || held > ceiling)
            throw new IllegalStateException("Owned HE buffer is outside its declared capacity");
    }

    private static long wholeFe(long maximumFe, long availableHe) {
        var amount = Math.min(maximumFe, EnergyConversion.feFromHe(Math.max(0L, availableHe)));
        var quantum = EnergyConversion.feQuantum();
        return amount <= 0 ? 0 : amount - amount % quantum;
    }

    long insertableFe(long maximumFe, IEnergyHandlerMK2 receiver) {
        checkBuffer();
        var offered = Math.min(receiver.getMaxPower() - receiver.getPower(), receiver.getReceiverSpeed());
        if (relay && offered > 0) offered -= receiver.transferPower(offered, true);
        return wholeFe(maximumFe, offered - insertedHe);
    }

    long extractableFe(long maximumFe, IEnergyHandlerMK2 provider) {
        if (relay) return 0;
        checkBuffer();
        var available = Math.min(provider.getPower(), provider.getProviderSpeed());
        return wholeFe(maximumFe, available - extractedHe);
    }

    long insert(long maximumFe, IEnergyHandlerMK2 receiver) {
        var accepted = insertableFe(maximumFe, receiver);
        insertedHe += EnergyConversion.heFromFe(accepted);
        return accepted;
    }

    long extract(long maximumFe, IEnergyHandlerMK2 provider) {
        var given = extractableFe(maximumFe, provider);
        extractedHe += EnergyConversion.heFromFe(given);
        return given;
    }

    long amountFe() {
        checkBuffer();
        return EnergyConversion.feFromHe(Math.subtractExact(Math.addExact(machine.getPower(), insertedHe), extractedHe));
    }

    long capacityFe() { checkBuffer(); return EnergyConversion.feFromHe(machine.getMaxPower()); }
    long[] save() { return new long[]{insertedHe, extractedHe}; }
    void restore(long[] state) { insertedHe = state[0]; extractedHe = state[1]; }

    /** Native entry/exit methods run only after the outermost transaction commits. */
    boolean hasPending() { return insertedHe != 0 || extractedHe != 0; }

    boolean commit() {
        if (!hasPending()) return false;
        if (relay) {
            // a relay splits each offer non-linearly (the charger gives each item a fifth): offer the rest again until all
            // that was accepted is placed, which the simulation's larger offer guarantees within this tick
            var remaining = insertedHe;
            insertedHe = extractedHe = 0;
            for (int i = 0; remaining > 0 && i < RELAY_PASSES; i++) {
                var left = machine.transferPower(remaining, false);
                if (left >= remaining) break;
                remaining = left;
            }
            return true;
        }
        checkBuffer();
        var held = machine.getPower();
        if (insertedHe > machine.getMaxPower() - held || extractedHe > held)
            throw new IllegalStateException("Owned HE buffer changed outside its reserved transaction");
        var incoming = insertedHe;
        var outgoing = extractedHe;
        insertedHe = extractedHe = 0;
        if (incoming > 0 && machine.transferPower(incoming, false) != 0)
            throw new IllegalStateException("Owned HE buffer refused reserved input");
        // no read-back: the REDD reports at most half its BigInteger store, so its getPower need not move
        if (outgoing > 0) machine.usePower(outgoing);
        return true;
    }
}
