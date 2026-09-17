package com.hbm.api.energymk2;

import com.hbm.uninos.graph.Distribution;

import java.util.List;

/**
 * Arbitration for the ENERGY net - 1.7's law in one pass: SURPLUS gives everyone full demand, SHORTAGE walks tiers HIGHEST first; 1.7's cumulative-subtraction slip is deliberately not reproduced.
 */
public final class EnergyBus {

    private EnergyBus() {
    }

    /**
     * Serve budget across the gathered receivers, highest tier first. @return total actually delivered.
     */
    public static long serveShortage(List<Distribution.Share<IEnergyHandlerMK2>>[] byPriority, long[] demand,
                                     long budget, Distribution.Apply<IEnergyHandlerMK2> apply) {
        long delivered = 0;
        long remaining = budget;
        // 1.7: strict tiers, descending
        for (int t = byPriority.length - 1; t >= 0 && remaining > 0; t--) {
            List<Distribution.Share<IEnergyHandlerMK2>> tier = byPriority[t];
            if (tier == null || tier.isEmpty()) continue;
            long remainingDemand = demand[t];
            if (remainingDemand <= 0) continue;
            long tierBudget = remaining;
            // 1.7: no sort, no cursor
            for (int i = 0, size = tier.size(); i < size && tierBudget > 0; i++) {
                Distribution.Share<IEnergyHandlerMK2> row = tier.get(i);
                long want = row.amount();
                // 1.7: share = toTransfer * rec_demand / tierDemand
                long offer = Distribution.weightedShare(tierBudget, want, remainingDemand,
                        Math.min(want, tierBudget));
                remainingDemand -= want;
                if (offer <= 0) continue;
                long taken = Math.min(apply.give(row.endpoint(), offer), offer);
                if (taken <= 0) continue;
                tierBudget -= taken;
                delivered += taken;
            }
            // 1.7: no cumulative re-subtraction
            remaining = tierBudget;
        }
        return delivered;
    }

    /**
     * Hand every receiver its full demand. The surplus case, where nothing but demand can matter.
     */
    public static long serveInFull(List<Distribution.Share<IEnergyHandlerMK2>>[] byPriority, long budget,
                                   Distribution.Apply<IEnergyHandlerMK2> apply) {
        long delivered = 0;
        long remaining = budget;
        for (int t = byPriority.length - 1; t >= 0; t--) {
            List<Distribution.Share<IEnergyHandlerMK2>> tier = byPriority[t];
            if (tier == null || tier.isEmpty()) continue;
            for (int i = 0, size = tier.size(); i < size; i++) {
                Distribution.Share<IEnergyHandlerMK2> row = tier.get(i);
                if (remaining <= 0) return delivered;
                long give = Math.min(row.amount(), remaining);
                if (give <= 0) continue;
                long taken = Math.min(apply.give(row.endpoint(), give), give);
                if (taken <= 0) continue;
                delivered += taken;
                remaining -= taken;
            }
        }
        return delivered;
    }

}
