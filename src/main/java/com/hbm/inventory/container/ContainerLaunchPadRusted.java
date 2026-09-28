package com.hbm.inventory.container;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.inventory.ModCustomMissileMenus;
import com.hbm.items.ModItems;
import com.hbm.tileentity.bomb.TileEntityLaunchPadRusted;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code ContainerLaunchPadRusted}: released missile (26,72, take-only), launch codes (116,45), launch key
 * (134,45), designator (26,99); player inventory at y=154, hotbar 212 (176x236).
 */
public class ContainerLaunchPadRusted extends AbstractContainerMenu {

    private final Container machine;
    private final @Nullable TileEntityLaunchPadRusted pad;

    public ContainerLaunchPadRusted(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityLaunchPadRusted te ? te : null);
    }

    public ContainerLaunchPadRusted(int containerId, Inventory playerInventory, @Nullable TileEntityLaunchPadRusted pad) {
        super(ModCustomMissileMenus.LAUNCH_PAD_RUSTED.get(), containerId);
        this.pad = pad;
        this.machine = pad != null ? pad : new SimpleContainer(4);

        // 1.7 SlotCraftingOutput: take-only
        addSlot(new Slot(machine, 0, 26, 72) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addSlot(new Slot(machine, 1, 116, 45));
        addSlot(new Slot(machine, 2, 134, 45));
        addSlot(new Slot(machine, 3, 26, 99));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 154);
    }

    public @Nullable TileEntityLaunchPadRusted getPad() {
        return pad;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index <= 3) {
                if (!moveItemStackTo(stack, 4, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IDesignatorItem) {
                if (!moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() == ModItems.launch_code.get()) {
                if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() == ModItems.launch_key.get()) {
                if (!moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
