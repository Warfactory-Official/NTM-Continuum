package com.hbm.api.energymk2;

import dev.golem.core.world.EnergyView;
import com.hbm.uninos.graph.Distribution;

import java.util.ArrayList;
import java.util.List;

/**
 * A foreign (FE/RF) block presented to the NTM power net as one endpoint, with the HE<->FE conversion applied here and nowhere else.
 */
final class ForeignEnergyEndpoint implements IEnergyHandlerMK2 {

    /**
     * A handler that both accepts and gives is served as a RECEIVER ONLY (Mekanism's push semantics); a support flag may only be believed when it says NO. // 26.1: EnergyHandler has no flags to be tempted by.
     */
    static final boolean BIDIRECTIONAL_IS_RECEIVER_ONLY = true;

    /**
     * Foreign receivers sit at LOWEST (diverging from the fluid net's NORMAL), except the part a buffer would pay itself,
     * which goes at HIGHEST. Foreign providers get no tier. Debit is untiered, as in 1.7.
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
     * A buffer (a handler giving and taking in one round) would spend {@code bufferSelf} of its own supply on itself:
     * foreign receivers may take that part first, as far as the supply still covers every other receiver.
     */
    static long fileForeignDemand(List<Distribution.Share<IEnergyHandlerMK2>> foreign,
                                  long powerAvailable, double reserve, long nativeDemand,
                                  long bufferDemand, long bufferSelf,
                                  List<Distribution.Share<IEnergyHandlerMK2>>[] receivers,
                                  long[] demand) {
        long reserveBudget = reserve > 0 ? (long) (powerAvailable * Math.min(reserve, 1.0)) : 0;
        long surplusBudget = Math.max(0L, powerAvailable - nativeDemand);
        int lowest = FOREIGN_PRIORITY.ordinal();
        int normal = ConnectionPriority.NORMAL.ordinal();
        long[] unfiled = new long[foreign.size()];
        long added = 0;
        for (int i = 0; i < unfiled.length; i++) {
            Distribution.Share<IEnergyHandlerMK2> share = foreign.get(i);
            long atNormal = Math.min(share.amount(), reserveBudget);
            reserveBudget -= atNormal;
            long atLowest = Math.min(share.amount() - atNormal, surplusBudget);
            surplusBudget -= atLowest;
            added += file(receivers, demand, normal, share.endpoint(), atNormal);
            added += file(receivers, demand, lowest, share.endpoint(), atLowest);
            unfiled[i] = share.amount() - atNormal - atLowest;
        }
        // served before any buffer's tier; the supply still covers every receiver that is not a buffer
        long bufferBudget = Math.min(bufferSelf, Math.max(0L, powerAvailable - (nativeDemand - bufferDemand) - added));
        int highest = ConnectionPriority.HIGHEST.ordinal();
        for (int i = 0; i < unfiled.length && bufferBudget > 0; i++) {
            long atHighest = Math.min(unfiled[i], bufferBudget);
            bufferBudget -= atHighest;
            added += file(receivers, demand, highest, foreign.get(i).endpoint(), atHighest);
        }
        return added;
    }

    private static long file(List<Distribution.Share<IEnergyHandlerMK2>>[] receivers, long[] demand, int tier,
                             IEnergyHandlerMK2 endpoint, long amount) {
        if (amount <= 0) return 0;
        if (receivers[tier] == null) receivers[tier] = new ArrayList<>();
        receivers[tier].add(new Distribution.Share<>(endpoint, amount));
        demand[tier] += amount;
        return amount;
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
     * Never called; a no-op here would silently drop energy for any new caller.
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
