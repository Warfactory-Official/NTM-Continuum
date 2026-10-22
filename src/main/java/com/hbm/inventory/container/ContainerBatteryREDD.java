package com.hbm.inventory.container;

import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.storage.TileEntityBatteryREDD;
import dev.golem.core.world.Compat;
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

import java.math.BigInteger;

/** The two-slot FEnSU menu: charge at 26,53 and discharge at 80,53. */
public final class ContainerBatteryREDD extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 2;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityBatteryREDD battery;

    public ContainerBatteryREDD(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerBatteryREDD(int containerId, Inventory playerInventory, TileEntityBatteryREDD battery) {
        this(containerId, playerInventory, battery,
                ContainerLevelAccess.create(battery.getLevel(), battery.getBlockPos()));
    }

    private ContainerBatteryREDD(int containerId, Inventory playerInventory,
                                 @Nullable TileEntityBatteryREDD battery, ContainerLevelAccess access) {
        super(ModMenus.BATTERY_REDD.get(), containerId);
        this.battery = battery;
        this.machine = battery != null ? battery : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(new BatterySlot(machine, 0, 26, 53));
        this.addSlot(new BatterySlot(machine, 1, 80, 53));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 99);
    }

    private static @Nullable TileEntityBatteryREDD resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity entity = playerInventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityBatteryREDD battery ? battery : null;
    }

    public BigInteger getPower() { return battery != null ? battery.power : BigInteger.ZERO; }
    public long getMaxPower() { return TileEntityBatteryREDD.MAX_POWER; }
    public BigInteger getDelta() { return battery != null ? battery.delta : BigInteger.ZERO; }
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
        return Containers.stillValidMultiblock(access, player, ModBatteryBlocks.machine_battery_redd.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    private static final class BatterySlot extends Slot {
        private BatterySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }
    }
}
