package com.hbm.uninos;

import com.hbm.uninos.graph.NodeNetwork;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One 1.7 {@code NodeNet} for one round: the graph nets of ducts or cables and the port nodes that touch them, joined
 * through ports with several connections and through ports facing each other.
 */
public final class PortNet<D> {

    public final INetworkProvider type;
    public final List<NodeNetwork<D>> nets = new ArrayList<>(2);
    public final List<PortNode> ports = new ArrayList<>(2);
    /** Every role holder the round saw, whether or not it had anything to move. */
    public final ReferenceOpenHashSet<Object> receiverEntries = new ReferenceOpenHashSet<>();
    public final ReferenceOpenHashSet<Object> providerEntries = new ReferenceOpenHashSet<>();
    /** 1.7 {@code energyTracker} / {@code fluidTracker}: what the providers lost this round. */
    public long tracker;
    /** The fluid round's gather scratch for a net of ports only; a net with ducts keeps it on its first duct net. */
    public @Nullable Object fluidGather;

    PortNet(INetworkProvider type) {
        this.type = type;
    }

    /** Round-robin cursors: the first graph net's, which outlive the round, or the first port's for a net of ports only. */
    public int[] receiverCursors(int size) {
        if (!nets.isEmpty()) {
            NodeNetwork<D> anchor = nets.get(0);
            if (anchor.receiverCursors == null || anchor.receiverCursors.length != size) anchor.receiverCursors = new int[size];
            return anchor.receiverCursors;
        }
        PortNode anchor = ports.get(0);
        if (anchor.receiverCursors == null || anchor.receiverCursors.length != size) anchor.receiverCursors = new int[size];
        return anchor.receiverCursors;
    }

    public int[] providerCursors(int size) {
        if (!nets.isEmpty()) {
            NodeNetwork<D> anchor = nets.get(0);
            if (anchor.providerCursors == null || anchor.providerCursors.length != size) anchor.providerCursors = new int[size];
            return anchor.providerCursors;
        }
        PortNode anchor = ports.get(0);
        if (anchor.providerCursors == null || anchor.providerCursors.length != size) anchor.providerCursors = new int[size];
        return anchor.providerCursors;
    }
}
