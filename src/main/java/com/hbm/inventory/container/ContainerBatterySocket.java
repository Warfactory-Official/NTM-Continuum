package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Menu for the battery socket: one validated battery slot ({@link com.hbm.api.energymk2.IBatteryItem}
 * only) + the player inventory (laid out for the taller 181px GUI). The screen reads the live (client)
 * BlockEntity for the carried battery's charge / I/O mode; the mode/priority buttons are dispatched
 * through the vanilla {@link #clickMenuButton} path.
 */
public class ContainerBatterySocket extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 1;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityBatterySocket socket;

    /** Client-side: resolve the BlockEntity by position. */
    public ContainerBatterySocket(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    /** Server-side: bound to the live BlockEntity. */
    public ContainerBatterySocket(int containerId, Inventory playerInventory, TileEntityBatterySocket socket) {
        this(containerId, playerInventory, socket,
                ContainerLevelAccess.create(socket.getLevel(), socket.getBlockPos()));
    }

    private ContainerBatterySocket(int containerId, Inventory playerInventory, @Nullable TileEntityBatterySocket socket, ContainerLevelAccess access) {
        super(ModMenus.BATTERY_SOCKET.get(), containerId);
        this.socket = socket;
        this.machine = socket != null ? socket : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(new BatterySlot(this.machine, 0, 35, 35));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 99);
    }

    private static @Nullable TileEntityBatterySocket resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityBatterySocket socket ? socket : null;
    }

    // --- synced readouts for the screen (read off the client-side BlockEntity) ---

    public long getPower() { return socket != null ? socket.getPower() : 0L; }
    public long getMaxPower() { return socket != null ? socket.getMaxPower() : 0L; }
    public long getDelta() { return socket != null ? socket.delta : 0L; }
    public ItemStack getBattery() { return socket != null ? socket.getItem(0) : ItemStack.EMPTY; }
    public int getRedLow() { return socket != null ? socket.redLow : 0; }
    public int getRedHigh() { return socket != null ? socket.redHigh : 0; }
    public int getPriorityOrdinal() { return socket != null ? socket.priority.ordinal() : 0; }

    /** Button 0/1 = cycle the unpowered/powered I/O mode, button 2 = cycle priority. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (socket == null) return false;
        switch (id) {
            case 0 -> socket.cycleRedLow();
            case 1 -> socket.cycleRedHigh();
            case 2 -> socket.cyclePriority();
            default -> { return false; }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModBatteryBlocks.machine_battery_socket.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MACHINE_SLOTS;
            int invEnd = MACHINE_SLOTS + 36;
            if (index < invStart) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, invStart, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    /** A slot that only accepts {@link com.hbm.api.energymk2.IBatteryItem}s (via {@link Container#canPlaceItem}). */
    private static final class BatterySlot extends Slot {
        BatterySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.getContainerSlot(), stack);
        }
    }
}
