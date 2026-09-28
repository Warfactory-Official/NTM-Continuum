package com.hbm.inventory.container;

import com.hbm.inventory.recipes.MagicRecipes;
import com.hbm.items.special.ItemBook;
import dev.golem.core.gui.CompatContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code ContainerBook}: a 2x2 grid spaced 36 apart at (30, 17) whose result at (124, 35) is
 * {@link MagicRecipes#getRecipe}; taking it uses one of each grid item, like {@code SlotCrafting}. The grid is
 * handed back to the player on close, and the menu stays open only while the player carries the book.
 */
public class ContainerBook extends CompatContainerMenu {

    public final SimpleContainer craftMatrix = new SimpleContainer(4) {
        @Override
        public void setChanged() {
            super.setChanged();
            ContainerBook.this.slotsChanged(this);
        }
    };
    public final SimpleContainer craftResult = new SimpleContainer(1);

    public ContainerBook(MenuType<?> type, int containerId, Inventory inventory) {
        super(type, containerId);

        addSlot(new Slot(craftResult, 0, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player taker, ItemStack stack) {
                for (int i = 0; i < 4; i++) {
                    if (!craftMatrix.getItem(i).isEmpty()) craftMatrix.removeItem(i, 1);
                }
                super.onTake(taker, stack);
            }
        });

        for (int l = 0; l < 2; ++l) {
            for (int i1 = 0; i1 < 2; ++i1) {
                addSlot(new Slot(craftMatrix, i1 + l * 2, 30 + i1 * 36, 17 + l * 36));
            }
        }

        for (int l = 0; l < 3; ++l) {
            for (int i1 = 0; i1 < 9; ++i1) {
                addSlot(new Slot(inventory, i1 + l * 9 + 9, 8 + i1 * 18, 84 + l * 18));
            }
        }

        for (int l = 0; l < 9; ++l) {
            addSlot(new Slot(inventory, l, 8 + l * 18, 142));
        }

        slotsChanged(craftMatrix);
    }

    @Override
    public void slotsChanged(Container inventory) {
        if (inventory == craftMatrix) {
            List<ItemStack> grid = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) grid.add(craftMatrix.getItem(i));
            craftResult.setItem(0, MagicRecipes.getRecipe(grid));
        }
        super.slotsChanged(inventory);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) clearContainer(player, craftMatrix);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index == 0) {
                if (!moveItemStackTo(itemstack1, 5, 41, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (index >= 5 && index < 32) {
                if (!moveItemStackTo(itemstack1, 32, 41, false)) return ItemStack.EMPTY;
            } else if (index >= 32 && index < 41) {
                if (!moveItemStackTo(itemstack1, 5, 32, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(itemstack1, 5, 41, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;

            slot.onTake(player, itemstack1);
        }
        return itemstack;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != craftResult && super.canTakeItemForPickAll(stack, slot);
    }

    /** 1.7 {@code player.inventory.hasItem(ModItems.book_of_)}. */
    @Override
    public boolean stillValid(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() instanceof ItemBook) return true;
        }
        return false;
    }
}
