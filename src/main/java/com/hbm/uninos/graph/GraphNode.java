package com.hbm.uninos.graph;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public final class GraphNode<D> {

    public final long posKey;
    /**
     * The up-to-six segments leaving this node - the REDUCED graph's adjacency; first-class cells exist only where the topology is interesting, every other cell is interior to a segment.
     */
    public final GraphSegment<D> @Nullable [] segments = newSegments();
    public D data;
    // Bit (1 << Direction.ordinal()) set = face open
    public int openConnections;
    // 1.7: TileEntityPipelineBase.connected - pipe anchors
    public @Nullable LongOpenHashSet remoteLinks;
    // 1.7: buffer barrel endpoint-at-self flag
    public boolean selfEndpoint;
    /**
     * Presence degree - the count of non-null {@link #segments} slots. Drives the fold rule.
     */
    public int degree;

    /**
     * A mask toggle has targeted this cell at least once (1.7's valve/switch), so it can never fold into a run; sticky - demotion would have to re-prove the cell carries nothing.
     */
    public boolean device;

    /**
     * The net this node belongs to. Membership is a pointer compare.
     */
    public @Nullable NodeNetwork<D> net;

    /**
     * This node's edge-face register, if it has one. Only first-class cells carry one.
     */
    public @Nullable EdgeFaces ef;

    int floodStamp;
    int floodOwner;

    /**
     * A fold compile is in flight for this middle node - the dedupe bit, transient like the job itself.
     */
    boolean foldQueued;

    public GraphNode(long posKey, D data, int openConnections) {
        this.posKey = posKey;
        this.data = data;
        this.openConnections = openConnections;
    }

    @SuppressWarnings("unchecked")
    private static <D> GraphSegment<D>[] newSegments() {
        return new GraphSegment[6];
    }

    public boolean isOpen(Direction dir) {
        return isOpen(dir.ordinal());
    }

    public boolean isOpen(int ordinal) {
        return (this.openConnections & (1 << ordinal)) != 0;
    }

    public boolean hasRemoteLinks() {
        return remoteLinks != null && !remoteLinks.isEmpty();
    }
}
