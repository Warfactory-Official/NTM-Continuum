package com.hbm.uninos;

import com.hbm.api.tile.ILoadedTile;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Random;

public abstract class NodeNet<R, P, L extends GenNode<N>, N extends NodeNet<R, P, L, N>> {

    public static Random rand = new Random();

    public boolean valid = true;
    public ReferenceLinkedOpenHashSet<L> links = new ReferenceLinkedOpenHashSet<>();

    public Object2LongOpenHashMap<R> receiverEntries = new Object2LongOpenHashMap<>();
    public Object2LongOpenHashMap<P> providerEntries = new Object2LongOpenHashMap<>();

    public NodeNet() {
    }

    public boolean isSubscribed(R receiver) { return this.receiverEntries.containsKey(receiver); }
    public void addReceiver(R receiver) { this.receiverEntries.put(receiver, System.currentTimeMillis()); }
    public void removeReceiver(R receiver) { this.receiverEntries.removeLong(receiver); }

    public boolean isProvider(P provider) { return this.providerEntries.containsKey(provider); }
    public void addProvider(P provider) { this.providerEntries.put(provider, System.currentTimeMillis()); }
    public void removeProvider(P provider) { this.providerEntries.removeLong(provider); }

    public void joinNetworks(NodeNet<R, P, L, N> network) {
        if (network == this || !network.isValid()) return;

        for (L conductor : network.links) forceJoinLink(conductor);
        network.links.clear();

        for (R connector : network.receiverEntries.keySet()) this.addReceiver(connector);
        for (P connector : network.providerEntries.keySet()) this.addProvider(connector);
        network.destroy();
    }

    public NodeNet<R, P, L, N> joinLink(L node) {
        if (node.net != null) node.net.leaveLink(node);
        return forceJoinLink(node);
    }

    public NodeNet<R, P, L, N> forceJoinLink(L node) {
        this.links.add(node);

        node.setNet((N) this);
        return this;
    }

    public void leaveLink(L node) {
        node.setNet(null);
        this.links.remove(node);
        if (this.links.isEmpty()) {
            this.destroy();
        }
    }

    public void invalidate() {
        if (!this.valid) return;
        this.valid = false;
        UniNodespace.removeActiveNet(this);
    }

    public boolean isValid() { return this.valid; }
    public void resetTrackers() { }
    public abstract void update();

    public void destroy() {
        this.invalidate();
        for (L link : this.links) {
            link.setNet(null);
        }
        this.links.clear();
        this.receiverEntries.clear();
        this.providerEntries.clear();
    }

    public static boolean isBadLink(Object o) {
        if (o instanceof ILoadedTile && !((ILoadedTile) o).isLoaded()) return true;
        return o instanceof BlockEntity && ((BlockEntity) o).isRemoved();
    }
}
