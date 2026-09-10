package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityBarrel;
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

/**
 * Menu for the fluid drum: the same six-slot identifier/canister layout the fluid tank uses, at the
 * drum's slightly narrower spacing.
 */
public class ContainerBarrel extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 6;

    private final Container machine;
    private final @Nullable TileEntityBarrel barrel;

    public ContainerBarrel(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerBarrel(int containerId, Inventory playerInventory, @Nullable TileEntityBarrel machine) {
        super(ModMenus.BARREL.get(), containerId);
        this.barrel = machine;
        this.machine = machine != null ? machine : new SimpleContainer(MACHINE_SLOTS);

        addSlot(validated(0, 8, 17));
        addSlot(takeOnly(1, 8, 53));
        addSlot(validated(2, 35, 17));
        addSlot(takeOnly(3, 35, 53));
        addSlot(validated(4, 125, 17));
        addSlot(takeOnly(5, 125, 53));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ContainerBarrel.this.machine.canPlaceItem(index, stack);
            }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
    }

    private static @Nullable TileEntityBarrel resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityBarrel machine ? machine : null;
    }

    public @Nullable TileEntityBarrel getBarrel() {
        return barrel;
    }

    /** Button 0 = cycle the drum's I/O mode. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && barrel != null) {
            barrel.cycleMode();
            return true;
        }
        return false;
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
            int invEnd = MACHINE_SLOTS + 36;
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, invEnd, true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
