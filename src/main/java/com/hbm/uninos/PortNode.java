package com.hbm.uninos;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code GenNode} of a machine port: the positions the port occupies and the connections it makes. Ducts and
 * cables stay in the level graph; a port node joins whatever net its connections touch (see {@link PortNodes}).
 */
public final class PortNode {

    public final INetworkProvider type;
    public final long[] positions;
    public final DirPos[] connections;
    final long[] conCells;

    /** 1.7 {@code NodeNet.receiverEntries/providerEntries}, held per node: only its owners ever sit on it. */
    public final ReferenceOpenHashSet<Object> receivers = new ReferenceOpenHashSet<>(2);
    public final ReferenceOpenHashSet<Object> providers = new ReferenceOpenHashSet<>(2);

    public boolean expired;
    /** The net of the last round, for the gauges, the network tools and {@code TilePort.getNetwork}. */
    public @Nullable PortNet<?> net;
    int[] receiverCursors;
    int[] providerCursors;

    public PortNode(INetworkProvider type, BlockPos[] positions, DirPos[] connections) {
        this.type = type;
        this.positions = new long[positions.length];
        for (int i = 0; i < positions.length; i++) this.positions[i] = positions[i].asLong();
        this.connections = connections;
        this.conCells = new long[connections.length];
        for (int i = 0; i < connections.length; i++) conCells[i] = connections[i].getPos().asLong();
    }

    /** Whether one of the connections aims at {@code cell} along {@code dir}. */
    public boolean connectsTo(long cell, ForgeDirection dir) {
        for (int i = 0; i < conCells.length; i++) {
            if (conCells[i] == cell && connections[i].getDir() == dir) return true;
        }
        return false;
    }
}
