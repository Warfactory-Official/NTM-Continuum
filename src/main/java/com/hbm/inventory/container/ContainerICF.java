package com.hbm.inventory.container;

import com.hbm.inventory.ModIcfMenus;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.tileentity.machine.TileEntityICF;
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
 * The ICF menu (1.7 {@code ContainerICF}): five pellet inputs at (80 + 18i, 18), the chamber at (116, 54), five
 * take-only outputs at (80 + 18i, 90), the coolant identifier at (44, 90), then the player inventory from (44, 140)
 * with the hotbar at 198.
 */
public class ContainerICF extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = TileEntityICF.SLOTS;

    private final Container machine;
    private final @Nullable TileEntityICF icf;

    public ContainerICF(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerICF(int containerId, Inventory playerInventory, @Nullable TileEntityICF icf) {
        super(ModIcfMenus.ICF.get(), containerId);
        this.icf = icf;
        this.machine = icf != null ? icf : new SimpleContainer(MACHINE_SLOTS);

        for (int i = 0; i < 5; i++) addSlot(new Slot(machine, i, 80 + i * 18, 18));
        addSlot(new Slot(machine, 5, 116, 54));
        // 1.7 SlotCraftingOutput: take-only
        for (int i = 0; i < 5; i++) {
            addSlot(new Slot(machine, 6 + i, 80 + i * 18, 90) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        addSlot(new Slot(machine, 11, 44, 90));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 44, 140);
    }

    private static @Nullable TileEntityICF resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity entity = playerInventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityICF icf ? icf : null;
    }

    public @Nullable TileEntityICF getIcf() {
        return icf;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();

            if (index <= 11) {
                if (!moveItemStackTo(stack, 12, slots.size(), true)) return ItemStack.EMPTY;
            } else if (copy.getItem() instanceof IItemFluidId) {
                if (!moveItemStackTo(stack, 11, 12, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 5, 6, false)) {
                if (!moveItemStackTo(stack, 0, 5, false)) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }

        return copy;
    }
}
