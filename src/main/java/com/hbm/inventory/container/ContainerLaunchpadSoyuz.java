package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityLaunchpadSoyuz;
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
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code ContainerLaunchpadSoyuz}: rocket (98,80), designator (80,80), satellite (98,26), orbital module (80,26),
 * kerosene in/out (152,98)/(152,116), oxygen in/out (170,98)/(170,116), battery (134,98), the eighteen cargo slots in
 * three columns growing leftwards from (44,26), player inventory at (17,162). Slots accept only what the pad does.
 */
public class ContainerLaunchpadSoyuz extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 27;

    private final ContainerLevelAccess access;
    private final @Nullable TileEntityLaunchpadSoyuz launcher;

    public ContainerLaunchpadSoyuz(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityLaunchpadSoyuz pad ? pad : null,
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerLaunchpadSoyuz(int containerId, Inventory playerInventory, TileEntityLaunchpadSoyuz launcher) {
        this(containerId, playerInventory, launcher, ContainerLevelAccess.create(launcher.getLevel(), launcher.getBlockPos()));
    }

    private ContainerLaunchpadSoyuz(int containerId, Inventory playerInventory, @Nullable TileEntityLaunchpadSoyuz launcher,
                                    ContainerLevelAccess access) {
        super(ModMenus.LAUNCHPAD_SOYUZ.get(), containerId);
        this.launcher = launcher;
        this.access = access;
        Container machine = launcher != null ? launcher : new SimpleContainer(MACHINE_SLOTS);

        addSlot(new MachineSlot(machine, 0, 98, 80));
        addSlot(new MachineSlot(machine, 1, 80, 80));
        addSlot(new MachineSlot(machine, 2, 98, 26));
        addSlot(new MachineSlot(machine, 3, 80, 26));
        addSlot(new MachineSlot(machine, 4, 152, 98));
        addSlot(new MachineSlot(machine, 5, 152, 116));
        addSlot(new MachineSlot(machine, 6, 170, 98));
        addSlot(new MachineSlot(machine, 7, 170, 116));
        addSlot(new MachineSlot(machine, 8, 134, 98));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 6; j++) {
                addSlot(new MachineSlot(machine, j + i * 6 + 9, 44 - i * 18, 26 + j * 18));
            }
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 17, 162);
    }

    public @Nullable TileEntityLaunchpadSoyuz getLauncher() {
        return launcher;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.launchpad_soyuz.get());
    }

    // 1.7 ContainerBase.transferStackInSlot: machine to player, player into the machine's slots
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    /** 1.7 {@code SlotNonRetarded}: takes only what the machine's {@code isItemValidForSlot} accepts. */
    static class MachineSlot extends Slot {

        MachineSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.getContainerSlot(), stack);
        }
    }
}
