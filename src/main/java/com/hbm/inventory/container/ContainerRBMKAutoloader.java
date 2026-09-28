package com.hbm.inventory.container;

import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.inventory.ModRBMKMenus;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKAutoloader;
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
 * 1.7 {@code ContainerRBMKAutoloader}: a 3x3 fresh-rod grid at (17,18), a take-only 3x3 spent-rod grid at
 * (107,18) and the player inventory at (8,100). The input slots accept anything by hand, as the old
 * {@code ContainerBase.addSlots} did; only automation is filtered by the tile.
 */
public class ContainerRBMKAutoloader extends AbstractContainerMenu {

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable BlockEntityRBMKAutoloader loader;

    public ContainerRBMKAutoloader(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof BlockEntityRBMKAutoloader l ? l : null,
                ContainerLevelAccess.create(inventory.player.level(), pos));
    }

    public ContainerRBMKAutoloader(int id, Inventory inventory, BlockEntityRBMKAutoloader loader) {
        this(id, inventory, loader, ContainerLevelAccess.create(loader.getLevel(), loader.getBlockPos()));
    }

    private ContainerRBMKAutoloader(int id, Inventory inventory, @Nullable BlockEntityRBMKAutoloader loader, ContainerLevelAccess access) {
        super(ModRBMKMenus.RBMK_AUTOLOADER.get(), id);
        this.loader = loader;
        this.machine = loader != null ? loader : new SimpleContainer(BlockEntityRBMKAutoloader.SLOTS);
        this.access = access;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            addSlot(new Slot(machine, col + row * 3, 17 + col * 18, 18 + row * 18));
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            addSlot(new Slot(machine, 9 + col + row * 3, 107 + col * 18, 18 + row * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        Compat.addPlayerInventory(this::addSlot, inventory, 8, 100);
    }

    public @Nullable BlockEntityRBMKAutoloader loader() { return loader; }

    @Override public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModRBMKBlocks.rbmk_autoloader.get());
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index <= 17) {
                if (!moveItemStackTo(stack, 18, slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 9, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
