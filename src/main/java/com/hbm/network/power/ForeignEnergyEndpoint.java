package com.hbm.network.power;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import dev.golem.core.world.EnergyView;
import dev.golem.core.graph.Distribution;

import java.util.ArrayList;
import java.util.List;

/**
 * A foreign (FE/RF) block presented to the NTM power net as one endpoint, with the HE<->FE conversion applied here and nowhere else.
 */
final class ForeignEnergyEndpoint implements IEnergyHandlerMK2 {

    /**
     * A handler that both accepts and gives is served as a RECEIVER ONLY (Mekanism's push semantics); a support flag may only be believed when it says NO. // 26.2: EnergyHandler has no flags to be tempted by.
     */
    static final boolean BIDIRECTIONAL_IS_RECEIVER_ONLY = true;

    /**
     * Foreign receivers sit at LOWEST (deliberately diverging from the fluid net's NORMAL); foreign providers get no tier - debit is untiered, the 1.7 behaviour.
     */
    static final IEnergyHandlerMK2.ConnectionPriority FOREIGN_PRIORITY =
            IEnergyHandlerMK2.ConnectionPriority.LOWEST;
    private final EnergyView view;
    /**
     * This face's sub-unit remainder in both directions - the ratio carries it exactly instead of flooring it away.
     */
    private final EnergyConversion.Carry carry = new EnergyConversion.Carry();
    private long availableHe;
    private long roomHe;
    private boolean provider;
    private boolean receiver;

    /**
     * Latched: this handler has been observed accepting AND giving at the same moment. See the policy note.
     */
    private boolean bidirectionalSeen;
    /**
     * FE the net was credited but the store could not pay (usePower is void); subtracted from the next offer, so it is repaid rather than written off.
     */
    private long owedFe;

    ForeignEnergyEndpoint(EnergyView view) {
        this.view = view;
    }

    /** Whether this endpoint still speaks for the handler a fresh lookup just produced. */
    boolean looksAt(EnergyView other) {
        return view.handlerIdentity() == other.handlerIdentity();
    }

    /**
     * A foreign handler is not one of our block entities, so the loaded question is the chunk's and was
     * already answered by the lookup that produced the view.
     */
    @Override
    public boolean isLoaded() {
        return true;
    }

    /**
     * File foreign receiver demand into the priority tiers, applying reserve; lives here because it IS FOREIGN_PRIORITY's arithmetic.
     */
    static long fileForeignDemand(List<Distribution.Share<IEnergyHandlerMK2>> foreign,
                                  long powerAvailable, double reserve, long nativeDemand,
                                  List<Distribution.Share<IEnergyHandlerMK2>>[] receivers,
                                  long[] demand) {
        long reserveBudget = reserve > 0 ? (long) (powerAvailable * Math.min(reserve, 1.0)) : 0;
        long surplusBudget = Math.max(0L, powerAvailable - nativeDemand);
        int lowest = FOREIGN_PRIORITY.ordinal();
        int normal = ConnectionPriority.NORMAL.ordinal();
        long added = 0;
        for (Distribution.Share<IEnergyHandlerMK2> share : foreign) {
            long atNormal = Math.min(share.amount(), reserveBudget);
            reserveBudget -= atNormal;
            long atLowest = Math.min(share.amount() - atNormal, surplusBudget);
            surplusBudget -= atLowest;
            if (atNormal > 0) {
                if (receivers[normal] == null) receivers[normal] = new ArrayList<>();
                receivers[normal].add(new Distribution.Share<>(share.endpoint(), atNormal));
                demand[normal] += atNormal;
                added += atNormal;
            }
            if (atLowest > 0) {
                if (receivers[lowest] == null) receivers[lowest] = new ArrayList<>();
                receivers[lowest].add(new Distribution.Share<>(share.endpoint(), atLowest));
                demand[lowest] += atLowest;
                added += atLowest;
            }
        }
        return added;
    }

    /**
     * Take this tick's snapshot: two simulated transactions, once per gather, before any accessor.
     */
    void refresh() {
        long insertableFe = view.insertable();
        long extractableFe = view.extractable();
        boolean canInsert = insertableFe > 0;
        boolean canExtract = extractableFe > 0;
        if (canInsert && canExtract) bidirectionalSeen = true;

        receiver = canInsert;
        provider = canExtract && !(BIDIRECTIONAL_IS_RECEIVER_ONLY && bidirectionalSeen);

        availableHe = provider ? EnergyConversion.heFromFe(Math.max(0, extractableFe - owedFe)) : 0;
        roomHe = receiver ? EnergyConversion.heFromFe(insertableFe) : 0;
    }

    boolean servesAsProvider() {
        return provider && availableHe > 0;
    }

    boolean servesAsReceiver() {
        return receiver && roomHe > 0;
    }

    @Override
    public long getPower() {
        return availableHe;
    }

    /**
     * Never called, deliberately loud: a no-op here would silently drop energy at whatever new caller reached for it.
     */
    @Override
    public void setPower(long power) {
        throw new UnsupportedOperationException("a foreign energy endpoint has no absolute HE level to set");
    }

    @Override
    public long getMaxPower() {
        long total = availableHe + roomHe;
        return total < 0 ? Long.MAX_VALUE : total;
    }

    @Override
    public long getProviderSpeed() {
        return availableHe;
    }

    @Override
    public long getReceiverSpeed() {
        return roomHe;
    }

    @Override
    public ConnectionPriority getPriority() {
        return FOREIGN_PRIORITY;
    }

    /**
     * @return the HE that could not be placed - the MK2 remainder contract; only whole FE cross.
     */
    @Override
    public long transferPower(long power, boolean simulate) {
        if (power <= 0 || !receiver) return power;
        if (simulate) {
            long fits = Math.min(EnergyConversion.feFromHe(power), view.insertable());
            return power - EnergyConversion.heFromFe(fits);
        }
        long fe = carry.feFromHe(power);
        long acceptedFe = fe > 0 ? view.insert(fe) : 0;
        long unplacedHe = acceptedFe < fe ? carry.refundFe(fe - acceptedFe, power) : 0;
        roomHe = Math.max(0, roomHe - (power - unplacedHe));
        return unplacedHe;
    }

    @Override
    public void usePower(long power) {
        if (power <= 0) return;
        availableHe = Math.max(0, availableHe - power);
        long fe = carry.feFromHe(power) + owedFe;
        if (fe <= 0) return;
        owedFe = fe - view.extract(fe);
    }

    /**
     * The banked sub-unit remainders, for verification.
     */
    EnergyConversion.Carry carry() {
        return carry;
    }
}
