package com.hbm.inventory.container;

import com.hbm.inventory.recipes.LemegetonRecipes;
import com.hbm.items.tool.ItemBookLemegeton;
import dev.golem.core.gui.CompatContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code ContainerLemegeton}: one input at (49, 35) whose {@link LemegetonRecipes} result sits at
 * (107, 35); taking the result uses one input, like {@code SlotCrafting}. The input is handed back on close,
 * and the menu stays open only while the player carries a Lemegeton.
 */
public class ContainerLemegeton extends CompatContainerMenu {

    public final SimpleContainer craftMatrix = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            ContainerLemegeton.this.slotsChanged(this);
        }
    };
    public final SimpleContainer craftResult = new SimpleContainer(1);

    public ContainerLemegeton(MenuType<?> type, int containerId, Inventory inventory) {
        super(type, containerId);

        addSlot(new Slot(craftResult, 0, 107, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player taker, ItemStack stack) {
                if (!craftMatrix.getItem(0).isEmpty()) craftMatrix.removeItem(0, 1);
                super.onTake(taker, stack);
            }
        });
        addSlot(new Slot(craftMatrix, 0, 49, 35));

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
        if (inventory == craftMatrix) craftResult.setItem(0, LemegetonRecipes.getRecipe(craftMatrix.getItem(0)));
        super.slotsChanged(inventory);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) clearContainer(player, craftMatrix);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotNo) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = slots.get(slotNo);

        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (slotNo <= 1) {
                if (!moveItemStackTo(itemstack1, 2, slots.size(), true)) return ItemStack.EMPTY;
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (!moveItemStackTo(itemstack1, 1, 2, false)) {
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

    /** 1.7 {@code player.inventory.hasItem(ModItems.book_lemegeton)}. */
    @Override
    public boolean stillValid(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() instanceof ItemBookLemegeton) return true;
        }
        return false;
    }
}
