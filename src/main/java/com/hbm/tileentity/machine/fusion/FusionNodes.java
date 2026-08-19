package com.hbm.tileentity.machine.fusion;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.UniNodespace;
import com.hbm.uninos.networkproviders.KlystronNetwork;
import com.hbm.uninos.networkproviders.PlasmaNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Node bookkeeping shared by every machine on the fusion networks.
 *
 * <p>Each of these machines has to look up its klystron/plasma node, create it if it is missing or has
 * expired, and destroy it on removal — the same twenty lines per machine per network. Upstream wrote it
 * out longhand every time and left a comment about it ("christ on the cross why didn't i just make a
 * baseclass to shove this crap into"); this is that base, as a helper rather than a superclass so it
 * also serves machines that already extend something else.
 */
public final class FusionNodes {

    private FusionNodes() {}

    /** Returns {@code existing} if it is still live, otherwise finds or creates the node at {@code at}. */
    public static KlystronNetwork.KlystronNode klystron(Level level, KlystronNetwork.@Nullable KlystronNode existing,
                                                        BlockPos at, DirPos connection) {
        if (existing != null && !existing.expired) return existing;
        KlystronNetwork.KlystronNode node = UniNodespace.getNode(level, at, KlystronNetwork.THE_PROVIDER);
        if (node != null) return node;
        node = new KlystronNetwork.KlystronNode(KlystronNetwork.THE_PROVIDER, at);
        node.setConnections(connection);
        UniNodespace.createNode(level, node);
        return node;
    }

    /** Returns {@code existing} if it is still live, otherwise finds or creates the node at {@code at}. */
    public static PlasmaNetwork.PlasmaNode plasma(Level level, PlasmaNetwork.@Nullable PlasmaNode existing,
                                                  BlockPos at, DirPos connection) {
        if (existing != null && !existing.expired) return existing;
        PlasmaNetwork.PlasmaNode node = UniNodespace.getNode(level, at, PlasmaNetwork.THE_PROVIDER);
        if (node != null) return node;
        node = new PlasmaNetwork.PlasmaNode(PlasmaNetwork.THE_PROVIDER, at);
        node.setConnections(connection);
        UniNodespace.createNode(level, node);
        return node;
    }

    /**
     * The node a klystron-style emitter owns: four blocks out the back at +2, reaching one further.
     * Shared by the klystron and its creative twin, which upstream expressed as a static on the former.
     */
    public static KlystronNetwork.KlystronNode emitterNode(Level level, KlystronNetwork.@Nullable KlystronNode existing,
                                                           BlockPos pos, ForgeDirection facing) {
        ForgeDirection dir = facing.getOpposite();
        return klystron(level, existing,
                new BlockPos(pos.getX() + dir.offsetX * 4, pos.getY() + 2, pos.getZ() + dir.offsetZ * 4),
                new DirPos(pos.getX() + dir.offsetX * 5, pos.getY() + 2, pos.getZ() + dir.offsetZ * 5, dir));
    }

    /**
     * Pushes klystron energy into the first live torus on this node's network.
     *
     * @return whether a torus was actually reached - the creative klystron shows this as its "connected"
     *         light, and it is the only feedback a player gets that the beam is landing.
     */
    public static boolean provideKyU(KlystronNetwork.@Nullable KlystronNode node, long output) {
        if (node == null || node.net == null) return false;
        for (net.minecraft.world.level.block.entity.BlockEntity thing : node.net.receiverEntries.keySet()) {
            // Replace with an interface should anything other than a torus ever accept KyU.
            if (thing instanceof TileEntityFusionTorus torus && torus.isLoaded() && !torus.isRemoved()) {
                torus.receiveKlystronEnergy(output);
                return true; // one is all the net ever feeds
            }
        }
        return false;
    }

    /** Tears down this machine's nodes. Call from {@code setRemoved} on the server. */
    public static void destroy(@Nullable Level level, KlystronNetwork.@Nullable KlystronNode klystron,
                               PlasmaNetwork.@Nullable PlasmaNode plasma) {
        if (level == null || level.isClientSide()) return;
        if (klystron != null) UniNodespace.destroyNode(level, klystron);
        if (plasma != null) UniNodespace.destroyNode(level, plasma);
    }

    /**
     * The direction a dummyable machine faces, from its core meta. Every fusion machine positions its
     * nodes relative to this, and upstream spelled out {@code getBlockMetadata() - 10} inline each time.
     */
    public static com.hbm.lib.ForgeDirection facing(net.minecraft.world.level.block.state.BlockState state) {
        int meta = state.hasProperty(com.hbm.blocks.BlockDummyable.META)
                ? state.getValue(com.hbm.blocks.BlockDummyable.META) : com.hbm.blocks.BlockDummyable.offset + 2;
        return com.hbm.lib.ForgeDirection.getOrientation(meta - com.hbm.blocks.BlockDummyable.offset);
    }
}
