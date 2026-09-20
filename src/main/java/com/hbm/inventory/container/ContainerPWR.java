package com.hbm.inventory.container;

import com.hbm.items.machine.IItemFluidId;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.ModPwrMenus;
import com.hbm.tileentity.machine.TileEntityPWRController;

/**
 * The PWR controller menu (1.7 {@code ContainerPWR}): fuel in at (53, 5), hot fuel out at (89, 32), the fluid
 * identifier at (8, 59), then the player inventory from (8, 106) / hotbar at 164.
 */
public class ContainerPWR extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 3;

    private final Container machine;
    private final @Nullable TileEntityPWRController controller;

    public ContainerPWR(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerPWR(int containerId, Inventory playerInventory, @Nullable TileEntityPWRController controller) {
        super(ModPwrMenus.PWR_CONTROLLER.get(), containerId);
        this.controller = controller;
        this.machine = controller != null ? controller : new SimpleContainer(MACHINE_SLOTS);

        addSlot(new Slot(machine, 0, 53, 5) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(0, stack); }
        });
        addSlot(new Slot(machine, 1, 89, 32) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addSlot(new Slot(machine, 2, 8, 59));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 106);
    }

    private static @Nullable TileEntityPWRController resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity entity = playerInventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityPWRController controller ? controller : null;
    }

    public @Nullable TileEntityPWRController getController() {
        return controller;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
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
            } else if (result.getItem() instanceof IItemFluidId) {
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
