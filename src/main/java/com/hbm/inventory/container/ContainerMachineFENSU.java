package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.storage.TileEntityMachineFENSU;
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

/** The old four-slot battery menu, bound to the FEnSU multiblock core. */
public class ContainerMachineFENSU extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 4;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineFENSU fensu;

    public ContainerMachineFENSU(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineFENSU(int containerId, Inventory playerInventory, TileEntityMachineFENSU fensu) {
        this(containerId, playerInventory, fensu,
                ContainerLevelAccess.create(fensu.getLevel(), fensu.getBlockPos()));
    }

    private ContainerMachineFENSU(int containerId, Inventory playerInventory,
                                  @Nullable TileEntityMachineFENSU fensu, ContainerLevelAccess access) {
        super(ModMenus.FENSU.get(), containerId);
        this.fensu = fensu;
        this.machine = fensu != null ? fensu : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(new FensuSlot(machine, 0, 35, 17));
        this.addSlot(new FensuSlot(machine, 1, 35, 53));
        this.addSlot(new FensuSlot(machine, 2, 125, 17));
        this.addSlot(new FensuSlot(machine, 3, 125, 53));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private static @Nullable TileEntityMachineFENSU resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineFENSU fensu ? fensu : null;
    }

    public long getPower() { return fensu != null ? fensu.power : 0L; }
    public long getMaxPower() { return TileEntityMachineFENSU.MAX_POWER; }
    public long getDelta() { return fensu != null ? fensu.delta : 0L; }
    public long getPowerScaled(long scale) { return fensu != null ? fensu.getPowerRemainingScaled(scale) : 0L; }
    public int getRedLow() { return fensu != null ? fensu.redLow : 0; }
    public int getRedHigh() { return fensu != null ? fensu.redHigh : 0; }
    public int getPriorityOrdinal() { return fensu != null ? fensu.priority.ordinal() : 0; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (fensu == null) return false;
        switch (id) {
            case 0 -> fensu.cycleRedLow();
            case 1 -> fensu.cycleRedHigh();
            case 2 -> fensu.cyclePriority();
            default -> { return false; }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return Menus.stillValid(access, player, ModBatteryBlocks.machine_fensu.get());
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
                if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)
                        && !moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    private static final class FensuSlot extends Slot {
        private FensuSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }
    }
}
