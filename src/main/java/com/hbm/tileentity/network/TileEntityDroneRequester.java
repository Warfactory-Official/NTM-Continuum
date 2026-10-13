package com.hbm.tileentity.network;

import com.hbm.api.block.IDroneContainer;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.tileentity.IControlReceiverFilter;
import com.hbm.tileentity.network.RequestNetwork.Demand;
import com.hbm.tileentity.network.RequestNetwork.PathNode;
import com.hbm.tileentity.network.RequestNetwork.RequestNode;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import com.hbm.blocks.network.DroneDock;
import com.hbm.inventory.container.ContainerDroneRequester;
import com.hbm.tileentity.ModDroneBlockEntities;

/**
 * 1.7 {@code TileEntityDroneRequester}: asks the network for whatever its nine filters do not currently see in
 * stock. Slots 0-8 are the filters, 9-17 the delivery bank a drone unloads into.
 */
public class TileEntityDroneRequester extends TileEntityRequestNetwork
        implements IControlReceiverFilter, IDroneContainer.Requester, WorldlyContainer {

    public static final int FILTER_COUNT = 9;
    public static final int SLOT_COUNT = 18;

    private static final int[] SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    /** 1.7 hasPermission: a plain radius, not the machine base's own reach. */
    private static final double CONTROL_RANGE = 20;

    public final ModulePatternMatcher matcher = new ModulePatternMatcher(FILTER_COUNT);

    public TileEntityDroneRequester(BlockPos pos, BlockState state) {
        super(ModDroneBlockEntities.DRONE_REQUESTER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public String getDefaultName() {
        return "container.droneRequester";
    }

    @Override
    public void serverTick() {
        super.serverTick();
        networkPackNT();
    }

    /**
     * 1.7 re-encoded (filter, mode) as an AStack; the matcher answers the pair directly here, so every mode works.
     */
    @Override
    protected PathNode createNode(BlockPos pos) {
        List<Demand> request = new ArrayList<>();

        for (int i = 0; i < FILTER_COUNT; i++) {
            ItemStack filter = getItem(i);
            ItemStack stock = getItem(i + FILTER_COUNT);
            if (filter.isEmpty()) continue;
            String mode = matcher.modes[i];
            if (mode == null) continue;

            if (stock.isEmpty() || !matcher.isValidForFilter(filter, i, stock)) {
                request.add(new Demand(filter.copy(), mode));
            }
        }

        return new RequestNode(pos, reachable, request);
    }

    @Override
    public Container droneDeliveryInventory() {
        return new DeliveryBank(this);
    }

    @Override
    public void nextMode(int i) {
        matcher.nextMode(level, getItem(i), i);
    }

    @Override
    public int[] getFilterSlots() {
        return new int[]{0, FILTER_COUNT};
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.position().distanceTo(Vec3.atLowerCornerOf(worldPosition)) < CONTROL_RANGE;
    }

    /** The delivery bank alone answers automation; the filters are not items. */
    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        matcher.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        matcher.deserialize(buf);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        matcher.load(input);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        matcher.save(output);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerDroneRequester(containerId, playerInventory, this);
    }

    /** 1.7 EntityRequestDrone unloads into slots 9..17 alone - the filters are not cargo space. */
    private record DeliveryBank(TileEntityDroneRequester requester) implements Container {

        @Override
        public int getContainerSize() {
            return FILTER_COUNT;
        }

        @Override
        public boolean isEmpty() {
            for (int slot = 0; slot < FILTER_COUNT; slot++) if (!getItem(slot).isEmpty()) return false;
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return requester.getItem(slot + FILTER_COUNT);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return requester.removeItem(slot + FILTER_COUNT, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return requester.removeItemNoUpdate(slot + FILTER_COUNT);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            requester.setItem(slot + FILTER_COUNT, stack);
        }

        @Override
        public void setChanged() {
            requester.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return requester.stillValid(player);
        }

        @Override
        public void clearContent() {
            for (int slot = 0; slot < FILTER_COUNT; slot++) setItem(slot, ItemStack.EMPTY);
        }
    }

    /** 1.7 {@code DroneDock.breakBlock}: the requester drops slots 9..17 only, its nine filter slots stay put. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) return;
        for (int i = 9; i < SLOT_COUNT; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }
}
