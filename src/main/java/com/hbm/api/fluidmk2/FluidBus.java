package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.uninos.graph.Distribution;

import java.util.List;

/**
 * Arbitration for the FLUID net, lifted unchanged off the network it used to live in.
 *
 * <p>Pressure bands outermost, priority tiers highest-first inside each, weighted share within a tier,
 * and a round-robin cursor so a line of equal claimants does not always fill the same one first. The
 * energy bus has no such cursor because 1.7's energy law has none; 1.7's fluid law has none either, but
 * this rotation is behaviour that already shipped, and moving discovery off UNINOS is not the change
 * that should decide which machine on a shared line fills first.
 *
 * <p>What is <b>not</b> reproduced from 1.7, and was already not being reproduced here, is its
 * cumulative subtraction of {@code received} across priority tiers — which charges every later tier for
 * what the earlier ones took, twice over — and its random scapegoat loop for the unsettled remainder.
 */
public final class FluidBus {

    private FluidBus() {}

    /**
     * Serve one pressure band's gathered receivers from {@code available}, then charge the band's
     * providers for exactly what was taken.
     *
     * @return what was actually moved, for the net's throughput tracker.
     */
    public static long transfer(FluidType type, int pressure, long available,
                                List<Distribution.Share<IFluidProviderMK2>> providers,
                                List<Distribution.Share<IFluidReceiverMK2>>[] receivers, long[] demand,
                                int[] receiverCursors, int cursorBase, int[] providerCursors, int providerCursor) {
        long moved = serve(type, pressure, available, receivers, demand, receiverCursors, cursorBase);
        if (moved > 0L) {
            Distribution.debit(providers, moved, available, providerCursors, providerCursor,
                    (provider, amount) -> {
                        provider.useUpFluid(type, pressure, amount);
                        return amount;
                    });
        }
        return moved;
    }

    private static long serve(FluidType type, int pressure, long available,
                              List<Distribution.Share<IFluidReceiverMK2>>[] receivers, long[] demand,
                              int[] cursors, int cursorBase) {
        long received = 0L;
        long totalAvailable = available;

        for (int tier = receivers.length - 1; tier >= 0; tier--) {
            long priorityDemand = demand[tier];
            List<Distribution.Share<IFluidReceiverMK2>> tierReceivers = receivers[tier];
            if (priorityDemand <= 0L || tierReceivers.isEmpty() || totalAvailable <= 0L) continue;

            long toTransfer = Math.min(priorityDemand, totalAvailable);
            long sentThisPriority = 0L;
            long remainingDemand = priorityDemand;
            int count = tierReceivers.size();
            int start = Distribution.normalizedCursor(cursors[cursorBase + tier], count);

            for (int step = 0; step < count && sentThisPriority < toTransfer; step++) {
                Distribution.Share<IFluidReceiverMK2> entry = tierReceivers.get((start + step) % count);
                long receiverDemand = entry.amount();
                long remainingBudget = toTransfer - sentThisPriority;
                long maxForReceiver = Math.min(receiverDemand, remainingBudget);
                if (maxForReceiver <= 0L) {
                    remainingDemand -= receiverDemand;
                    continue;
                }

                long toSend = step == count - 1 ? maxForReceiver
                        : Distribution.weightedShare(remainingBudget, receiverDemand, remainingDemand, maxForReceiver);
                remainingDemand -= receiverDemand;
                if (toSend <= 0L) continue;

                long accepted = toSend - entry.endpoint().transferFluid(type, pressure, toSend);
                if (accepted <= 0L) continue;
                long accounted = Math.min(accepted, toSend);
                sentThisPriority += accounted;
                received += accounted;
            }

            cursors[cursorBase + tier] = (start + 1) % count;
            totalAvailable -= sentThisPriority;
        }
        return received;
    }
}
