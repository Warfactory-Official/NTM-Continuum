package com.hbm.uninos.networkproviders;

import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.uninos.graph.EdgeFaces;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import com.hbm.util.BobMathUtil;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import com.hbm.inventory.ISlotMonitorProvider;
import com.hbm.inventory.StackCache;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;
import com.hbm.uninos.PneumaticData;
import com.hbm.uninos.PneumaticGraph;

/**
 * 1.7 {@code com.hbm.uninos.networkproviders.PneumaticNetwork}. Transfer is source-initiated by the compressor
 * tube; this object is the graph net's attachment, minted on demand and dropped by the kernel on every
 * membership edit (1.7 {@code joinNetworks}/{@code destroy}), after which the members re-register themselves
 * on their next tick exactly as they did every tick in 1.7.
 */
public final class PneumaticNetwork {

    public static final byte SEND_FIRST = 0;
    public static final byte SEND_LAST = 1;
    public static final byte SEND_RANDOM = 2;
    public static final byte RECEIVE_ROBIN = 0;
    public static final byte RECEIVE_RANDOM = 1;
    public static final int ITEMS_PER_TRANSFER = 64;

    public final RandomSource rand = RandomSource.create();

    public final Set<StackCache> accessors = Collections.newSetFromMap(new WeakHashMap<>());
    public final Set<ISlotMonitorProvider> storages = Collections.newSetFromMap(new WeakHashMap<>());

    private static boolean initialised;

    public static void init() {
        if (initialised) return;
        initialised = true;
        Lifecycle.onServerTick(PneumaticNetwork::onServerTick);
    }

    /** The net at pos and its derived index, minted on demand into the kernel attachment slot. */
    public static @Nullable PneumaticNetwork at(ServerLevel level, BlockPos pos) {
        NodeNetwork<PneumaticData> net = PneumaticGraph.netAt(level, pos);
        if (net == null) return null;
        if (net.attachment instanceof PneumaticNetwork state) return state;
        PneumaticNetwork state = new PneumaticNetwork();
        net.attachment = state;
        return state;
    }

    /** 1.7 {@code UniNodespace.updateNodespace}: every net's {@code update()} once per server tick. */
    private static void onServerTick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            LevelNodeGraph<PneumaticData> graph = PneumaticGraph.get(level);
            graph.applyCompiledSwaps();
            for (NodeNetwork<PneumaticData> net : graph.networks()) {
                if (net.attachment instanceof PneumaticNetwork state) state.update();
            }
        }
    }

    /** The tube's own cell carries the destination, not one of its faces: {@link PneumaticGraph#setEndpoint}. */
    private static LevelNodeGraph.Probe isDestination(ServerLevel level, long nodeKey, int face) {
        if (face != EdgeFaces.SELF) return LevelNodeGraph.Probe.ABSENT;
        BlockPos pos = BlockPos.of(nodeKey);
        if (!(level.getBlockEntity(pos) instanceof TileEntityPneumoTube tube)) return LevelNodeGraph.Probe.ABSENT;
        if (tube.ejectionDir == null) return LevelNodeGraph.Probe.ABSENT;
        BlockPos target = pos.relative(tube.ejectionDir);
        if (!LevelNodeGraph.resident(level, target.asLong())) return LevelNodeGraph.Probe.UNKNOWN;
        return containerAt(level, target) != null ? LevelNodeGraph.Probe.PRESENT : LevelNodeGraph.Probe.ABSENT;
    }

    /** Every live destination on {@code net}, read off the active-endpoint set. */
    public static List<Destination> destinations(ServerLevel level, LevelNodeGraph<PneumaticData> graph,
                                                 NodeNetwork<PneumaticData> net) {
        List<Destination> out = new ArrayList<>();
        for (GraphNode<PneumaticData> member : graph.drainProbes(net, level,
                (lvl, g, cellPos, faces, face) -> isDestination(lvl, cellPos, face))) {
            EdgeFaces ef = member.ef;
            if (ef == null || (ef.activeMask & (1 << EdgeFaces.SELF)) == 0) continue;
            BlockPos node = BlockPos.of(member.posKey);
            if (!(level.getBlockEntity(node) instanceof TileEntityPneumoTube tube)) continue;
            if (tube.ejectionDir == null) continue;
            Container dest = containerAt(level, node.relative(tube.ejectionDir));
            if (dest != null) out.add(new Destination(dest, tube.ejectionDir, tube));
        }
        return out;
    }

    /** 1.7 {@code Compat.getTileStandard}: the container at pos, never force-loading a chunk to answer. */
    public static @Nullable Container containerAt(ServerLevel level, BlockPos pos) {
        if (level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ())) == null) return null;
        return level.getBlockEntity(pos) instanceof Container container ? container : null;
    }

    /** 1.7 {@code NodeNet.isBadLink}: a container whose block entity has since been unloaded or destroyed. */
    public static boolean isBadLink(Container inventory) {
        return inventory instanceof BlockEntity be && (be.isRemoved() || be.getLevel() == null);
    }

    /**
     * Returns an array of accessible slots from the given side of a Container. If it's a WorldlyContainer, uses
     * the sided restrictions instead. A copy: the order is shuffled by the caller.
     */
    public static int[] getSlotAccess(Container inventory, Direction dir) {
        if (inventory instanceof WorldlyContainer sided) {
            return sided.getSlotsForFace(dir).clone();
        }
        // the port's machine base states its sided access without being a WorldlyContainer; 1.7's was one.
        // Its empty default is what every crate answers here, so an empty answer means "undeclared", not "none".
        if (inventory instanceof TileEntityMachineBase machine) {
            int[] slotAccess = machine.getAccessibleSlotsFromSide(dir.ordinal());
            if (slotAccess != null && slotAccess.length > 0) return slotAccess.clone();
        }
        int[] slotAccess = new int[inventory.getContainerSize()];
        for (int i = 0; i < slotAccess.length; i++) slotAccess[i] = i;
        return slotAccess;
    }

    public void addStackCache(StackCache accessor) {
        if (accessors.add(accessor)) {
            for (ISlotMonitorProvider storage : List.copyOf(storages)) storage.onNewCacheHasJoined(accessor, this);
        }
    }

    /** 1.7 does this inside {@code joinNetworks}; here the kernel invalidates the whole index on a membership edit instead. */
    public void addStorage(ISlotMonitorProvider storage) {
        if (storages.add(storage)) {
            for (StackCache accessor : List.copyOf(accessors)) storage.onNewCacheHasJoined(accessor, this);
        }
    }

    /** Only the accessor caches need reaping now; destinations are derived per send, never accumulated. */
    public void update() {
        accessors.removeIf(x -> x.hasExpired);
    }

    public boolean send(ServerLevel level, Container source, TileEntityPneumoTube tube, Direction accessDir,
                        int sendOrder, int receiveOrder, int maxRange, int nextReceiver) {

        LevelNodeGraph<PneumaticData> graph = PneumaticGraph.get(level);
        NodeNetwork<PneumaticData> net = graph.networkAt(tube.getBlockPos().asLong());
        if (net == null) return false;
        List<Destination> receiverList = destinations(level, graph, net);
        if (receiverList.isEmpty()) return false;

        int[] sourceSlotAccess = getSlotAccess(source, accessDir);

        if (sendOrder == SEND_LAST) BobMathUtil.reverseIntArray(sourceSlotAccess);
        if (sendOrder == SEND_RANDOM) BobMathUtil.shuffleIntArray(sourceSlotAccess, rand);

        WorldlyContainer sidedSource = source instanceof WorldlyContainer w ? w : null;
        boolean hasItem = false;

        for (int i : sourceSlotAccess) {
            ItemStack stack = source.getItem(i);
            if (!stack.isEmpty()) {
                if (sidedSource != null && !sidedSource.canTakeItemThroughFace(i, stack, accessDir)) continue;
                boolean match = tube.matchesFilter(stack);
                if ((match && !tube.whitelist) || (!match && tube.whitelist)) continue;
                hasItem = true;
                break;
            }
        }

        // return early if there aren't any items in the source inventory, saves on some cpu usage for idle networks
        if (!hasItem) return false;

        // for round robin, receivers are ordered by proximity to the source
        if (receiveOrder == RECEIVE_ROBIN) receiverList.sort(new ReceiverComparator(tube.getBlockPos()));
        if (receiveOrder == RECEIVE_RANDOM) Collections.shuffle(receiverList);

        BlockEntity tile1 = source instanceof BlockEntity be ? be : null;

        int attempts = 0;
        int maxAttempts = Math.min(receiverList.size(), 5);

        // try all receivers for both modes, in an attempts based system.
        // instead of bailing out after the first failure (which means a 0.25 second wait), we just try the next one.
        while (attempts < maxAttempts) {
            int index = (receiveOrder == RECEIVE_ROBIN)
                    ? Math.floorMod(nextReceiver + attempts, receiverList.size()) : attempts;

            Destination candidate = receiverList.get(index);

            if (isBadLink(candidate.container())) {
                attempts++;
                continue;
            }

            Container dest = candidate.container();
            TileEntityPneumoTube endpointTile = candidate.tube();
            BlockEntity tile2 = dest instanceof BlockEntity be ? be : null;

            // range check for our compression level, skip if either source or dest aren't tile entities
            if (tile1 != null && tile2 != null) {
                int sq = (int) tile1.getBlockPos().distSqr(tile2.getBlockPos());
                if (sq > maxRange * maxRange) {
                    attempts++;
                    continue;
                }
            }

            WorldlyContainer sidedDest = dest instanceof WorldlyContainer w ? w : null;
            Direction destSide = candidate.pipeDir().getOpposite();
            int[] destSlotAccess = getSlotAccess(dest, destSide);
            int itemsLeftToSend = ITEMS_PER_TRANSFER; // not individual items but the total "mass", based on max stack size
            // 1.7 PneumaticNetwork line 169: the autocrafter takes one item per transfer, everything else 64
            int itemHardCap = dest instanceof com.hbm.tileentity.machine.TileEntityMachineAutocrafter ? 1 : ITEMS_PER_TRANSFER;
            boolean didSomething = false;

            for (int sourceIndex : sourceSlotAccess) {
                ItemStack sourceStack = source.getItem(sourceIndex);
                if (sourceStack.isEmpty()) continue;
                if (sidedSource != null && !sidedSource.canTakeItemThroughFace(sourceIndex, sourceStack, accessDir))
                    continue;
                // filter of the source
                boolean match = tube.matchesFilter(sourceStack);
                if ((match && !tube.whitelist) || (!match && tube.whitelist)) continue;
                // filter of the receiver, only if the sender and receiver aren't the same block
                if (endpointTile != null && endpointTile != tube) {
                    match = endpointTile.matchesFilter(sourceStack);
                    if ((match && !endpointTile.whitelist) || (!match && endpointTile.whitelist)) continue;
                }
                // the "mass" of an item. something that only stacks to 4 has a "mass" of 16.
                int proportionalValue = Mth.clamp(64 / sourceStack.getMaxStackSize(), 1, 64);

                // try to fill partial stacks first
                for (int destIndex : destSlotAccess) {
                    ItemStack destStack = dest.getItem(destIndex);
                    if (destStack.isEmpty()) continue;
                    if (!ItemStacks.sameItemAndData(sourceStack, destStack)) continue;
                    int toMove = BobMathUtil.min(sourceStack.getCount(),
                            destStack.getMaxStackSize() - destStack.getCount(),
                            dest.getMaxStackSize() - destStack.getCount(),
                            itemsLeftToSend / proportionalValue, itemHardCap);
                    if (toMove <= 0) continue;

                    ItemStack checkStack = destStack.copy();
                    checkStack.grow(toMove);
                    if (!dest.canPlaceItem(destIndex, checkStack)) continue;
                    if (sidedDest != null && !sidedDest.canPlaceItemThroughFace(destIndex, checkStack, destSide))
                        continue;

                    sourceStack.shrink(toMove);
                    if (sourceStack.isEmpty()) source.setItem(sourceIndex, ItemStack.EMPTY);
                    destStack.grow(toMove);
                    itemsLeftToSend -= toMove * proportionalValue;
                    didSomething = true;
                    if (itemsLeftToSend <= 0) break;
                }

                // if there's stuff left to send, occupy empty slots
                if (itemsLeftToSend > 0 && !sourceStack.isEmpty()) for (int destIndex : destSlotAccess) {
                    if (!dest.getItem(destIndex).isEmpty()) continue;
                    int toMove = BobMathUtil.min(sourceStack.getCount(), dest.getMaxStackSize(),
                            itemsLeftToSend / proportionalValue, itemHardCap);
                    if (toMove <= 0) continue;

                    ItemStack checkStack = sourceStack.copyWithCount(toMove);
                    if (!dest.canPlaceItem(destIndex, checkStack)) continue;
                    if (sidedDest != null && !sidedDest.canPlaceItemThroughFace(destIndex, checkStack, destSide))
                        continue;

                    ItemStack newStack = sourceStack.copyWithCount(toMove);
                    sourceStack.shrink(toMove);
                    if (sourceStack.isEmpty()) source.setItem(sourceIndex, ItemStack.EMPTY);
                    dest.setItem(destIndex, newStack);
                    itemsLeftToSend -= toMove * proportionalValue;
                    didSomething = true;
                    if (itemsLeftToSend <= 0) break;
                }

                if (itemsLeftToSend <= 0) break;
            }

            // make sure both parties are saved to disk and increment the counter for round robin
            if (didSomething) {
                source.setChanged();
                dest.setChanged();
                return true;
            }

            attempts++;
        }
        return false;
    }

    /**
     * One live destination, derived fresh from the active-endpoint set rather than remembered: 1.7's timestamped
     * subscription table existed because it had no way to notice an owner dying.
     */
    public record Destination(Container container, Direction pipeDir, TileEntityPneumoTube tube) {
    }

    /**
     * Compares containers by distance, going off the assumption that they are block entities. Uses positional data
     * for tie-breaking if the distance is the same.
     */
    public record ReceiverComparator(BlockPos origin) implements Comparator<Destination> {

        @Override
        public int compare(Destination o1, Destination o2) {

            BlockEntity tile1 = o1.container() instanceof BlockEntity be ? be : null;
            BlockEntity tile2 = o2.container() instanceof BlockEntity be ? be : null;

            // prioritize actual block entities
            if (tile1 == null && tile2 != null) return 1;
            if (tile1 != null && tile2 == null) return -1;
            if (tile1 == null) return 0;

            int dist1 = (int) tile1.getBlockPos().distSqr(origin);
            int dist2 = (int) tile2.getBlockPos().distSqr(origin);

            // tie-breaker: use hash value instead
            if (dist1 == dist2) {
                return TileEntityPneumoTube.getIdentifier(tile1.getBlockPos())
                        - TileEntityPneumoTube.getIdentifier(tile2.getBlockPos());
            }

            return dist1 - dist2;
        }
    }
}
