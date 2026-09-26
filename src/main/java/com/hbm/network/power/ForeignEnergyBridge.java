package com.hbm.network.power;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * One power net's side of the FE capability our conductors expose, in HE throughout - every number answered out of here is a BUDGET the last distribute published, never a stored charge.
 */
public final class ForeignEnergyBridge {

    /**
     * How long a face stays latched after our capability accepted a push from it - two push-based networks aimed at each other would bill both distributors and inflate the flow meters; the latch breaks the loop, first-mover-wins.
     */
    public static final int RECEIVE_LATCH_TICKS = 4;
    /**
     * Neighbour position &rarr; the game tick our capability last accepted a push from it.
     */
    private final Long2LongOpenHashMap latchByNeighbour = new Long2LongOpenHashMap();
    /**
     * The injected pool, presented to the distributor as an ordinary provider; getMaxPower equals getPower - this is not a store with headroom.
     */
    private final IEnergyHandlerMK2 injectedSource = new IEnergyHandlerMK2() {

        @Override
        public boolean isLoaded() {
            return true;
        }

        @Override
        public long getPower() {
            return injectedHe;
        }

        @Override
        public void setPower(long power) {
            injectedHe = Math.max(0, power);
        }

        @Override
        public long getMaxPower() {
            return injectedHe;
        }

        @Override
        public long getProviderSpeed() {
            return injectedHe;
        }
    };
    /**
     * This net's sub-unit remainder for the crossings its own budgets make; per-net and bounded, a merge folds it into the survivor's carry.
     */
    private final EnergyConversion.Carry carry = new EnergyConversion.Carry();
    private long injectedHe;
    private long insertBudgetHe;
    private long extractBudgetHe;
    private long owedHe;
    /**
     * Guards the whole net while its own round is running; see {@code PowerNetwork.distribute}.
     */
    private boolean distributing;

    public ForeignEnergyBridge() {
        latchByNeighbour.defaultReturnValue(Long.MIN_VALUE);
    }

    public State snapshot() {
        return new State(injectedHe, insertBudgetHe, extractBudgetHe, owedHe, carry.heToFeRemainder(),
                carry.feToHeRemainder(), new Long2LongOpenHashMap(latchByNeighbour));
    }

    public void restore(State state) {
        injectedHe = state.injected();
        insertBudgetHe = state.insertBudget();
        extractBudgetHe = state.extractBudget();
        owedHe = state.owed();
        carry.restore(state.heToFeCarry(), state.feToHeCarry());
        latchByNeighbour.clear();
        latchByNeighbour.putAll(state.latch());
    }

    /**
     * FE this net would take right now. Zero while its own round is running.
     */
    public long insertableFe() {
        return distributing ? 0 : EnergyConversion.feFromHe(insertBudgetHe);
    }

    /**
     * FE this net would give up right now. Zero while its own round is running.
     */
    public long extractableFe() {
        return distributing ? 0 : EnergyConversion.feFromHe(extractBudgetHe);
    }

    /**
     * @param neighbourKey the block pushing at us - the latch key, one foreign block on several faces is one peer. @return FE actually accepted.
     */
    public long insertFe(long fe, long neighbourKey, long gameTime) {
        if (fe <= 0 || distributing) return 0;
        long accepted = Math.min(fe, EnergyConversion.feFromHe(insertBudgetHe));
        if (accepted <= 0) return 0;
        long he = carry.heFromFe(accepted);
        if (he > insertBudgetHe) he = insertBudgetHe;
        insertBudgetHe -= he;
        injectedHe += he;
        latchByNeighbour.put(neighbourKey, gameTime);
        return accepted;
    }

    /**
     * HE a native pusher (a diode's output face, a capacitor's bus) may hand this net right now: the unmet
     * native demand the last round published. Zero while the net's own round is running, which is the
     * diode's recursion brake when its output is wired back into the net feeding it.
     */
    public long insertableHe() {
        return distributing ? 0 : insertBudgetHe;
    }

    /**
     * The HE counterpart of {@link #insertFe}: no conversion, no latch. What is accepted joins the injected
     * pool and is offered as a provider on the next round; the budget shrinks by the same amount, so a
     * pusher can never build up more than this net asked for.
     *
     * @return HE actually accepted.
     */
    public long insertHe(long he) {
        if (he <= 0 || distributing) return 0;
        long accepted = Math.min(he, insertBudgetHe);
        if (accepted <= 0) return 0;
        insertBudgetHe -= accepted;
        injectedHe += accepted;
        return accepted;
    }

    /**
     * @return FE actually given up; the matching HE is charged to this net's providers on its next round.
     */
    public long extractFe(long fe) {
        if (fe <= 0 || distributing) return 0;
        long given = Math.min(fe, EnergyConversion.feFromHe(extractBudgetHe));
        if (given <= 0) return 0;
        long he = carry.heFromFe(given);
        if (he > extractBudgetHe) he = extractBudgetHe;
        extractBudgetHe -= he;
        owedHe += he;
        return given;
    }

    /**
     * What a peer reads as our charge: the surplus, because that is all of it we will ever hand over.
     */
    public long amountFe() {
        return EnergyConversion.feFromHe(extractBudgetHe);
    }

    /**
     * Surplus plus unmet demand, so {@code capacity - amount} is exactly the free space an insert will fill.
     */
    public long capacityFe() {
        long total = amountFe() + EnergyConversion.feFromHe(insertBudgetHe);
        return total < 0 ? Long.MAX_VALUE : total;
    }

    /**
     * Fold a dying net's bridge into this one at a merge; everything that must survive is position-blind, so addition IS conservation - the published budgets are deliberately not summed, the next round republishes.
     */
    public void absorb(ForeignEnergyBridge other) {
        injectedHe += other.injectedHe;
        owedHe += other.owedHe;
        carry.absorb(other.carry);
        for (ObjectIterator<Long2LongMap.Entry> it = other.latchByNeighbour.long2LongEntrySet().fastIterator();
             it.hasNext(); ) {
            Long2LongMap.Entry e = it.next();
            long mine = latchByNeighbour.get(e.getLongKey());
            if (mine == Long.MIN_VALUE || e.getLongValue() > mine) {
                latchByNeighbour.put(e.getLongKey(), e.getLongValue());
            }
        }
    }

    public boolean beginDistribute() {
        if (distributing) return false;
        distributing = true;
        return true;
    }

    public void endDistribute() {
        distributing = false;
    }

    public long injected() {
        return injectedHe;
    }

    public IEnergyHandlerMK2 injectedSource() {
        return injectedSource;
    }

    /**
     * HE peers have pulled since the last round, handed over for charging against this net's providers.
     */
    public long drainOwed() {
        long owed = owedHe;
        owedHe = 0;
        return owed;
    }

    /**
     * Re-file what no provider could cover, so a debt survives a tick rather than becoming free energy.
     */
    public void carryOwed(long he) {
        if (he > 0) owedHe += he;
    }

    /**
     * Did our capability accept a push from neighbourKey recently enough to skip pushing back? The sentinel test is not redundant - a bare subtraction overflows and reports every never-latched face as fresh.
     */
    public boolean latched(long neighbourKey, long gameTime) {
        long stamp = latchByNeighbour.get(neighbourKey);
        return stamp != Long.MIN_VALUE && gameTime - stamp <= RECEIVE_LATCH_TICKS;
    }

    /**
     * Republish both budgets from the round that just finished, and drop stamps that can no longer suppress anything. NATIVE demand only - counting foreign receivers would advertise a re-export appetite and circulate.
     */
    public void publish(long surplusHe, long unmetNativeHe, long gameTime) {
        extractBudgetHe = Math.max(0, surplusHe);
        insertBudgetHe = Math.max(0, unmetNativeHe);
        if (latchByNeighbour.isEmpty()) return;
        for (ObjectIterator<Long2LongMap.Entry> it = latchByNeighbour.long2LongEntrySet().fastIterator();
             it.hasNext(); ) {
            if (gameTime - it.next().getLongValue() > RECEIVE_LATCH_TICKS) it.remove();
        }
    }

    /**
     * Everything a capability call can mutate, copied for a transaction that may be rolled back - the two CARRY banks included, or a simulated conversion's remainder would outlive the rollback.
     */
    public record State(long injected, long insertBudget, long extractBudget, long owed, long heToFeCarry,
                        long feToHeCarry, Long2LongMap latch) {
    }
}
