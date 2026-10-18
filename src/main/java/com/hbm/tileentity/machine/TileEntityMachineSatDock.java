package com.hbm.tileentity.machine;

import com.hbm.inventory.container.ContainerSatDock;
import com.hbm.items.ISatChip;
import com.hbm.items.ModSatelliteItems;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.tileentity.ModSatelliteBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityMachineSatDock}, the "Cargo Landing Pad": fifteen cargo slots and a chip slot (15). Once a
 * second, with an ID chip tuned to a satellite holding cargo, it calls that cargo down in a drop pod
 * ({@code EntitySatellitePod}), which fills the pad. Nothing is pushed out; hoppers take the cargo.
 */
public class TileEntityMachineSatDock extends TileEntityMachineBase implements MenuProvider, WorldlyContainer {

    private static final int[] access = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 };

    public TileEntityMachineSatDock(BlockPos pos, BlockState state) {
        super(ModSatelliteBlockEntities.SAT_DOCK.get(), pos, state, 16);
    }

    @Override
    public String getDefaultName() {
        return "container.satDock";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineSatDock dock) {
        if (level instanceof ServerLevel server) dock.update(server);
    }

    private void update(ServerLevel level) {
        BlockPos pos = getBlockPos();
        long time = level.getGameTime() + identity(pos.getX(), pos.getY(), pos.getZ());
        ItemStack chip = inventory.getStackInSlot(15);

        if (time % 20 == 0 && chip.is(ModSatelliteItems.sat_chip.get())) {
            int freq = ISatChip.getFreqS(chip);
            SatelliteSavedData data = SatelliteSavedData.get(level, pos);
            Satellite sat = data.getSatFromFreq(freq);

            if (sat != null && sat.tryRequestItems(level, pos.getX(), pos.getY(), pos.getZ())) data.setDirty();
        }
    }

    /** 1.7 {@code fauxpointtwelve.BlockPos.getIdentity}, which staggers the pads' request ticks. */
    private static int identity(int x, int y, int z) {
        return (y + z * 27644437) * 27644437 + x;
    }

    /**
     * 1.7 {@code InventoryUtil.tryAddItemToInventory} over the cargo slots: tops up matching stacks, then puts the
     * rest into the first empty slot. Returns what did not fit.
     */
    public ItemStack addCargo(ItemStack stack) {
        for (int i = 0; i < 15 && !stack.isEmpty(); i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (slot.isEmpty() || !ItemStacks.sameItemAndData(slot, stack)) continue;
            int transfer = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
            if (transfer > 0) {
                slot.grow(transfer);
                stack.shrink(transfer);
            }
        }
        if (stack.isEmpty()) {
            setChanged();
            return ItemStack.EMPTY;
        }

        for (int i = 0; i < 15; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, stack.copy());
                setChanged();
                return ItemStack.EMPTY;
            }
        }
        setChanged();
        return stack;
    }

    /** 1.7: only the chip slot accepts automation. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 15;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return access;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return access;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName()) : Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerSatDock(containerId, playerInventory, this);
    }
}
