package com.hbm.uninos.graph;

import com.hbm.uninos.PortNet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jspecify.annotations.Nullable;

public final class NodeNetwork<D> {

    /**
     * First-class nodes held as OBJECTS - every consumer wants the node, and membership is the pointer compare node.net == this.
     */
    public final ReferenceOpenHashSet<GraphNode<D>> nodes = new ReferenceOpenHashSet<>();

    /**
     * The segments; a segment carries its OWN net pointer - a severed segment's endpoints can sit in two nets.
     */
    public final ReferenceOpenHashSet<GraphSegment<D>> segments = new ReferenceOpenHashSet<>();
    public @Nullable ReferenceOpenHashSet<GraphNode<D>> edgeFaces;
    /**
     * The edgeFaces subset with an endpoint found - what the gather iterates; null means underived, maintained incrementally, and activeFaces != null implies edgeFaces != null.
     */
    public @Nullable ReferenceOpenHashSet<GraphNode<D>> activeFaces;
    /**
     * One half of the drain's work list; DROPPED, never cleared - fastutil pins a set's capacity, so reusing it cost O(net size) per tick. Null is settled, and null does NOT mean underived.
     */
    public @Nullable ReferenceOpenHashSet<GraphNode<D>> pendingProbe;
    /**
     * The other half of the drain's work list; same null-when-settled rule as pendingProbe.
     */
    public @Nullable ReferenceOpenHashSet<GraphSegment<D>> pendingSegments;
    // 1.7: PowerNetMK2.energyTracker / FluidNetMK2.fluidTracker, what the providers lost last round
    public long tracker;
    /** The port net this net was part of last round (a net of several fluids keeps the last one's). */
    public @Nullable PortNet<D> portNet;
    public int[] providerCursors;
    /**
     * Reusable gather scratch for the ENERGY net, so a steady-state tick allocates nothing; does NOT cache MEMBERSHIP - a missed invalidation bump would be silent staleness.
     */
    public @Nullable Object energyGather;
    /**
     * The reusable gather scratch for the FLUID net. Same contract as {@link #energyGather}.
     */
    public @Nullable Object fluidGather;
    /**
     * Round-robin cursors for the FLUID net's tiered distribution; energy has no receiver cursor - the 1.7 law it serves has neither sort nor rotation.
     */
    public int[] receiverCursors;
    public long throughputCap = Long.MAX_VALUE;
    public boolean throughputCapDirty = true;
    /**
     * One slot for the owning subsystem's per-net derived state; the kernel never reads it and nulls it at every membership edit, so a surviving value is current by construction. Untyped - the kernel has no channel to name a distributor's cache type.
     */
    public @Nullable Object attachment;
    /**
     * Component size in CELLS - first-class nodes plus every segment's interior.
     */
    int cells;

    /**
     * Component size in CELLS - the unit every consumer asserts and serialises in.
     */
    public int size() {
        return this.cells;
    }

    /**
     * First-class node count - the object count the split/merge cost actually tracks.
     */
    public int nodeCount() {
        return nodes.size();
    }

    public boolean contains(GraphNode<D> node) {
        return node.net == this;
    }
}
