package com.hbm.tileentity.network;

import com.hbm.api.block.IDroneContainer;
import com.hbm.entity.item.EntityRequestDrone;
import com.hbm.entity.item.EntityRequestDrone.Step;
import com.hbm.items.ModEntityItems;
import com.hbm.items.tool.ItemDrone.EnumDroneType;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.network.RequestNetwork.Demand;
import com.hbm.tileentity.network.RequestNetwork.OfferNode;
import com.hbm.tileentity.network.RequestNetwork.PathNode;
import com.hbm.tileentity.network.RequestNetwork.RequestNode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.hbm.inventory.container.ContainerDroneDock;
import com.hbm.tileentity.ModDroneBlockEntities;

/**
 * 1.7 {@code TileEntityDroneDock}: parks request drones and dispatches them. Once a second, with a drone in
 * stock, it pairs an unfulfilled request with an offer that answers it, paths the round trip through the waypoints
 * both ends can see, and spends one parked drone on the errand.
 */
public class TileEntityDroneDock extends TileEntityRequestNetwork implements IDroneContainer.Dock {

    public static final int SLOT_COUNT = 9;

    public static final int PATHING_DEPTH = 10;
    private static final int ATTEMPTS = 5;
    private static final int LOCAL_CHUNK_RANGE = 5;
    /** 1.7: emergency halt after 1000 iterations per depth, so at most 10k in total. */
    private static final int ITERATION_BRAKE = 1000;

    public TileEntityDroneDock(BlockPos pos, BlockState state) {
        super(ModDroneBlockEntities.DRONE_DOCK.get(), pos, state, SLOT_COUNT);
    }

    private static boolean isRequestDrone(ItemStack stack) {
        return ModEntityItems.drone.is(stack, EnumDroneType.REQUEST);
    }

    @Override
    public String getDefaultName() {
        return "container.droneDock";
    }

    @Override
    public void serverTick() {
        super.serverTick();

        if (!(level instanceof ServerLevel server) || server.getGameTime() % 20 != 0) return;
        if (published == null || !hasDrone()) return;

        // grab all nodes in a 5 chunk radius
        Map<BlockPos, PathNode> local = RequestNetwork.localNodes(server, worldPosition, LOCAL_CHUNK_RANGE);
        List<RequestNode> requests = new ArrayList<>();
        List<OfferNode> offers = new ArrayList<>();

        for (PathNode node : local.values()) {
            if (node instanceof RequestNode request) requests.add(request);
            if (node instanceof OfferNode offer) offers.add(offer);
        }

        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            // randomize!
            Collections.shuffle(requests);
            Collections.shuffle(offers);

            // simply pick the first request node that has unfulfilled requests
            RequestNode target = null;
            for (RequestNode request : requests) {
                if (request.active && !request.request.isEmpty()) {
                    target = request;
                    break;
                }
            }
            if (target == null) continue;

            Demand demand = target.request.get(server.getRandom().nextInt(target.request.size()));

            offers:
            for (OfferNode offer : offers) {
                for (ItemStack stack : offer.offer) {
                    if (!offer.active || stack.isEmpty() || !demand.test(stack)) continue;
                    if (tryEmbark(server, target, offer, demand, local)) return; // pathed and spawned, stop
                    break offers; // if not, simply continue iterating over offer nodes
                }
            }
        }
    }

    private boolean tryEmbark(ServerLevel server, RequestNode request, OfferNode offer, Demand demand,
                              Map<BlockPos, PathNode> local) {
        PathNode dock = published;
        List<PathNode> toOffer = generatePath(dock, offer, local);
        if (toOffer == null) return false;
        List<PathNode> toRequest = generatePath(offer, request, local);
        if (toRequest == null) return false;
        List<PathNode> home = generatePath(request, dock, local);
        if (home == null) return false;

        for (int i = 0; i < getContainerSize(); i++) {
            if (isRequestDrone(getItem(i))) {
                removeItem(i, 1);
                break;
            }
        }

        EntityRequestDrone drone = new EntityRequestDrone(server);
        drone.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5);

        // write programming
        for (PathNode node : toOffer) drone.program.add(new Step.Goto(node.pos));
        drone.program.add(new Step.Goto(offer.pos));
        drone.program.add(new Step.Load(demand));
        for (PathNode node : toRequest) drone.program.add(new Step.Goto(node.pos));
        drone.program.add(new Step.Goto(request.pos));
        drone.program.add(new Step.Unload());
        for (PathNode node : home) drone.program.add(new Step.Goto(node.pos));
        drone.program.add(new Step.Goto(dock.pos));
        drone.program.add(new Step.Dock());

        server.addFreshEntity(drone);
        server.playSound(null, worldPosition, ModSounds.STORAGE_OPEN.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        return true;
    }

    /**
     * Breadth-first over the nodes each hop could see when it was published, returning the nodes BETWEEN the two
     * ends. Every hop is re-resolved through the live index, so a path never runs through a snapshot that has since
     * expired.
     */
    private static @Nullable List<PathNode> generatePath(PathNode start, PathNode end, Map<BlockPos, PathNode> local) {
        List<List<PathNode>> paths = new ArrayList<>();
        paths.add(new ArrayList<>(List.of(start)));

        for (int depth = 0; depth < PATHING_DEPTH; depth++) {
            int brake = ITERATION_BRAKE;
            List<List<PathNode>> extended = new ArrayList<>();

            outer:
            for (List<PathNode> path : paths) {
                for (BlockPos next : path.get(path.size() - 1).reachable) {
                    PathNode node = local.get(next);

                    if (node != null) {
                        List<PathNode> longer = new ArrayList<>(path);

                        if (node.pos.equals(end.pos)) {
                            longer.remove(0); // we only want the in-betweens
                            return longer;
                        }

                        longer.add(node);
                        extended.add(longer);
                    }

                    if (--brake <= 0) break outer;
                }
            }

            paths = extended;
        }

        return null;
    }

    public boolean hasDrone() {
        for (int i = 0; i < getContainerSize(); i++) if (isRequestDrone(getItem(i))) return true;
        return false;
    }

    @Override
    protected PathNode createNode(BlockPos pos) {
        return new PathNode(pos, reachable);
    }

    @Override
    public Container droneDockInventory() {
        return this;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false; // 1.7 TileEntityRequestNetworkContainer: no automation access
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerDroneDock(containerId, playerInventory, this);
    }
}
