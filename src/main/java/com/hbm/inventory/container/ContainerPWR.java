package com.hbm.inventory.container;

import com.hbm.blocks.machine.pwr.ModPWRBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.tileentity.machine.pwr.TileEntityPWRController;
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

/** Menu for the PWR controller: fuel in, spent fuel out, and the coolant identifier. */
public class ContainerPWR extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 3;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityPWRController controller;

    public ContainerPWR(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerPWR(int containerId, Inventory playerInventory, TileEntityPWRController controller) {
        this(containerId, playerInventory, controller,
                ContainerLevelAccess.create(controller.getLevel(), controller.getBlockPos()));
    }

    private ContainerPWR(int containerId, Inventory playerInventory, @Nullable TileEntityPWRController controller,
                         ContainerLevelAccess access) {
        super(ModMenus.PWR_CONTROLLER.get(), containerId);
        this.controller = controller;
        this.machine = controller != null ? controller : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        addSlot(validated(0, 53, 5));
        addSlot(takeOnly(1, 89, 32));
        addSlot(validated(2, 8, 59));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 106);
    }

    private Slot validated(int machineSlot, int x, int y) {
        return new Slot(machine, machineSlot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return machine.canPlaceItem(machineSlot, stack);
            }
        };
    }

    private Slot takeOnly(int machineSlot, int x, int y) {
        return new Slot(machine, machineSlot, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityPWRController resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityPWRController controller ? controller : null;
    }

    public @Nullable TileEntityPWRController getController() {
        return controller;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModPWRBlocks.pwr_controller.get());
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
            } else if (stack.getItem() instanceof IItemFluidId) {
                if (!moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
