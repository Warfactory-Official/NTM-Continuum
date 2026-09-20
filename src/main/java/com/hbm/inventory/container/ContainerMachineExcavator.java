package com.hbm.inventory.container;

import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemDrillbit;
import com.hbm.lib.Library;
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
import com.hbm.blocks.ModProduction2Blocks;
import com.hbm.inventory.ModProduction2Menus;
import com.hbm.tileentity.machine.TileEntityMachineExcavator;

/** 1.7 {@code ContainerMachineExcavator}: 242x204, battery, fluid identifier, two upgrades, drill bit, 3x3 take-only buffer. */
public final class ContainerMachineExcavator extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 14;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineExcavator excavator;

    public ContainerMachineExcavator(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineExcavator(int containerId, Inventory playerInventory, TileEntityMachineExcavator excavator) {
        this(containerId, playerInventory, excavator, ContainerLevelAccess.create(excavator.getLevel(), excavator.getBlockPos()));
    }

    private ContainerMachineExcavator(int containerId, Inventory playerInventory,
                                      @Nullable TileEntityMachineExcavator excavator, ContainerLevelAccess access) {
        super(ModProduction2Menus.EXCAVATOR.get(), containerId);
        this.excavator = excavator;
        this.machine = excavator != null ? excavator : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        // Battery: 0
        addSlot(validated(0, 220, 72));
        // Fluid ID: 1
        addSlot(validated(1, 202, 72));
        // Upgrades: 2-3, drill bit: 4
        for (int i = 0; i < 3; i++) {
            addSlot(validated(2 + i, 136 + i * 18, 75));
        }

        // Buffer: 5-13
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(takeOnly(5 + j + i * 3, 136 + j * 18, 5 + i * 18));
            }
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 41, 122);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineExcavator resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineExcavator excavator ? excavator : null;
    }

    public @Nullable TileEntityMachineExcavator getExcavator() { return excavator; }

    @Override
    public boolean stillValid(Player player) {
        return com.hbm.inventory.container.Containers.stillValidMultiblock(access, player,
                ModProduction2Blocks.MACHINE_EXCAVATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else {
                if (Library.isBattery(result)) {
                    if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (result.getItem() instanceof IItemFluidId) {
                    if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
                } else if (Library.isMachineUpgrade(result)) {
                    if (!moveItemStackTo(stack, 2, 4, false)) return ItemStack.EMPTY;
                } else if (result.getItem() instanceof ItemDrillbit) {
                    if (!moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
