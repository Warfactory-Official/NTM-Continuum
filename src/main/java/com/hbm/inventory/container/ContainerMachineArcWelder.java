package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineArcWelder;
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

/** The exact 1.12.2 Arc Welder menu: three inputs, output, battery, fluid identifier and two upgrades. */
public class ContainerMachineArcWelder extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 8;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineArcWelder welder;

    public ContainerMachineArcWelder(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineArcWelder(int containerId, Inventory playerInventory, TileEntityMachineArcWelder welder) {
        this(containerId, playerInventory, welder,
                ContainerLevelAccess.create(welder.getLevel(), welder.getBlockPos()));
    }

    private ContainerMachineArcWelder(int containerId, Inventory playerInventory,
                                      @Nullable TileEntityMachineArcWelder welder, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_ARC_WELDER.get(), containerId);
        this.welder = welder;
        this.machine = welder != null ? welder : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        addSlot(validated(0, 17, 36));
        addSlot(validated(1, 35, 36));
        addSlot(validated(2, 53, 36));
        addSlot(takeOnly(3, 107, 36));
        addSlot(validated(4, 152, 72));
        addSlot(validated(5, 17, 63));
        addSlot(validated(6, 89, 63));
        addSlot(validated(7, 107, 63));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 122);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return ContainerMachineArcWelder.this.machine.canPlaceItem(index, stack);
            }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineArcWelder resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineArcWelder welder ? welder : null;
    }

    public @Nullable TileEntityMachineArcWelder getWelder() {
        return welder;
    }

    @Override
    public boolean stillValid(Player player) {
        return Menus.stillValid(access, player, ModMachineBlocks.machine_arc_welder.get());
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
                if (!moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidId) {
                if (!moveItemStackTo(stack, 5, 6, false)) return ItemStack.EMPTY;
            } else if (Library.isMachineUpgrade(stack)) {
                if (!moveItemStackTo(stack, 6, 8, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 4, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
