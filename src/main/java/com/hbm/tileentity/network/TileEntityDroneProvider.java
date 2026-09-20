package com.hbm.tileentity.network;

import com.hbm.api.block.IDroneContainer;
import com.hbm.tileentity.network.RequestNetwork.OfferNode;
import com.hbm.tileentity.network.RequestNetwork.PathNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.container.ContainerDroneProvider;
import com.hbm.tileentity.ModDroneBlockEntities;

/** 1.7 {@code TileEntityDroneProvider}: offers its nine slots to the network; drones take from here. */
public class TileEntityDroneProvider extends TileEntityRequestNetwork implements IDroneContainer.Provider, WorldlyContainer {

    public static final int SLOT_COUNT = 9;
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    public TileEntityDroneProvider(BlockPos pos, BlockState state) {
        super(ModDroneBlockEntities.DRONE_PROVIDER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public String getDefaultName() {
        return "container.droneProvider";
    }

    @Override
    protected PathNode createNode(BlockPos pos) {
        java.util.List<ItemStack> offer = new java.util.ArrayList<>();
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty()) offer.add(stack.copy());
        }
        return new OfferNode(pos, reachable, offer);
    }

    @Override
    public Container droneOfferInventory() {
        return this;
    }

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
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerDroneProvider(containerId, playerInventory, this);
    }
}
