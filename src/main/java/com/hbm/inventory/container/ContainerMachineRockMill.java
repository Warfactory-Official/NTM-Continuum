package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.SlotCraftingOutput;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineRockMill;
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

/** 1.7 {@code ContainerMachineRockMill}. */
public class ContainerMachineRockMill extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 8;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineRockMill mill;

    public ContainerMachineRockMill(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityMachineRockMill te ? te : null,
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineRockMill(int containerId, Inventory playerInventory, TileEntityMachineRockMill machine) {
        this(containerId, playerInventory, machine, ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()));
    }

    private ContainerMachineRockMill(int containerId, Inventory playerInventory, @Nullable TileEntityMachineRockMill machine, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_ROCKMILL.get(), containerId);
        this.mill = machine;
        this.machine = machine != null ? machine : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(new MachineSlot(this.machine, 0, 152, 91));
        this.addSlot(new MachineSlot(this.machine, 1, 35, 90));
        for (int i = 0; i < 3; i++) this.addSlot(new MachineSlot(this.machine, 2 + i, 8 + i * 18, 27));
        for (int i = 0; i < 3; i++) this.addSlot(new SlotCraftingOutput(playerInventory.player, this.machine, 5 + i, 80 + i * 18, 27));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 138);
    }

    public @Nullable TileEntityMachineRockMill getMill() {
        return mill;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.machine_rockmill.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemBlueprints) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 2, 5, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            slot.onTake(player, stack);
        }
        return result;
    }

    /** 1.7 {@code SlotNonRetarded}: a slot that asks the machine what it accepts. */
    private static class MachineSlot extends Slot {
        MachineSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return this.container.canPlaceItem(this.getContainerSlot(), stack); }
    }
}
