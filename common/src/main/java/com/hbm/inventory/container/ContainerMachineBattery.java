package com.hbm.inventory.container;

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

    public ContainerMachineBattery(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineBattery(int containerId, Inventory playerInventory, TileEntityMachineBattery battery) {
        this(containerId, playerInventory, battery,
                ContainerLevelAccess.create(battery.getLevel(), battery.getBlockPos()));
    }

    private ContainerMachineBattery(int containerId, Inventory playerInventory, @Nullable TileEntityMachineBattery battery, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_BATTERY.get(), containerId);
        this.battery = battery;
        this.machine = battery != null ? battery : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(new Slot(machine, 0, 35, 17));
        this.addSlot(new Slot(machine, 1, 35, 53));
        this.addSlot(new Slot(machine, 2, 125, 17));
        this.addSlot(new Slot(machine, 3, 125, 53));
        this.addStandardInventorySlots(playerInventory, 8, 84);
    }

    private static @Nullable TileEntityMachineBattery resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineBattery battery ? battery : null;
    }

    public long getPower() {
        return battery != null ? battery.power : 0L;
    }

    public long getMaxPower() {
        return TileEntityMachineBattery.MAX_POWER;
    }

    public long getDelta() {
        return battery != null ? battery.delta : 0L;
    }

    public int getRedLow() { return battery != null ? battery.redLow : 0; }

    public int getRedHigh() { return battery != null ? battery.redHigh : 0; }

    public int getPriorityOrdinal() { return battery != null ? battery.priority.ordinal() : 0; }

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
