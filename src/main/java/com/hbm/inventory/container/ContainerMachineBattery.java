package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
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

public class ContainerMachineBattery extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 4;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineBattery battery;

    /** Client-side constructor — receives the {@link BlockPos} sent by the server. */
    public ContainerMachineBattery(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    /** Server-side constructor — bound to the live BlockEntity. */
    public ContainerMachineBattery(int containerId, Inventory playerInventory, TileEntityMachineBattery battery) {
        this(containerId, playerInventory, battery,
                ContainerLevelAccess.create(battery.getLevel(), battery.getBlockPos()));
    }

    private ContainerMachineBattery(int containerId, Inventory playerInventory, @Nullable TileEntityMachineBattery battery, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_BATTERY.get(), containerId);
        this.battery = battery;
        this.machine = battery != null ? battery : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        // Match the original battery layout: charge slots top, discharge slots bottom, left & right.
        this.addSlot(new Slot(machine, 0, 35, 17));
        this.addSlot(new Slot(machine, 1, 35, 53));
        this.addSlot(new Slot(machine, 2, 125, 17));
        this.addSlot(new Slot(machine, 3, 125, 53));
        //? if >=26.1 {
        this.addStandardInventorySlots(playerInventory, 8, 84);
        //?} else {
        /*Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
        *///?}
    }

    private static @Nullable TileEntityMachineBattery resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineBattery battery ? battery : null;
    }

    /**
     * Synced energy for the screen. Read straight off the (client-side) BlockEntity, whose power is
     * broadcast as a full {@code long} via the BufPacket — NOT through a {@code ContainerData} slot,
     * which is transmitted as a 16-bit short and would wrap for power values past 32 767.
     */
    public long getPower() {
        return battery != null ? battery.power : 0L;
    }

    public long getMaxPower() {
        return TileEntityMachineBattery.MAX_POWER;
    }

    /** Net throughput (HE/s) for the energy-bar tooltip, read off the synced client BlockEntity. */
    public long getDelta() {
        return battery != null ? battery.delta : 0L;
    }

    /** I/O mode while unpowered (0..3), for the GUI button. */
    public int getRedLow() { return battery != null ? battery.redLow : 0; }
    /** I/O mode while redstone-powered (0..3), for the GUI button. */
    public int getRedHigh() { return battery != null ? battery.redHigh : 0; }
    /** Connection priority ordinal, for the GUI button. */
    public int getPriorityOrdinal() { return battery != null ? battery.priority.ordinal() : 0; }

    /** Button 0/1 = cycle the unpowered/powered I/O mode, button 2 = cycle priority. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (battery == null) return false;
        switch (id) {
            case 0 -> battery.cycleRedLow();
            case 1 -> battery.cycleRedHigh();
            case 2 -> battery.cyclePriority();
            default -> { return false; }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModMachineBlocks.machine_battery.get());
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
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, 0, invStart, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }
}
