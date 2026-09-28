package com.hbm.inventory.container;

import com.hbm.inventory.ModRadarUtilityMenus;
import com.hbm.tileentity.machine.TileEntityForceField;
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
 * 1.7 {@code ContainerForceField}: the battery at (26,53), the radius upgrade at (89,35), the health upgrade at
 * (107,35), and the player inventory at (8,84). 1.7 put no item filter on the upgrade slots' {@code Slot}s; the
 * tile's {@code isItemValidForSlot} only gated automation, so any item goes in by hand, as it did there. The
 * on/off button arrives through {@link #clickMenuButton}, 1.7's {@code AuxButtonPacket}.
 */
public final class ContainerForceField extends AbstractContainerMenu {

    private final Container machine;
    private final @Nullable TileEntityForceField field;

    public ContainerForceField(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityForceField f ? f : null);
    }

    public ContainerForceField(int containerId, Inventory playerInventory, @Nullable TileEntityForceField field) {
        super(ModRadarUtilityMenus.FORCEFIELD.get(), containerId);
        this.field = field;
        this.machine = field != null ? field : new SimpleContainer(3);

        this.addSlot(new Slot(machine, 0, 26, 53));
        this.addSlot(new Slot(machine, 1, 89, 35));
        this.addSlot(new Slot(machine, 2, 107, 35));

        dev.golem.core.world.Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    public @Nullable TileEntityForceField getField() { return field; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || field == null) return false;
        field.toggle();
        return true;
    }

    /** 1.7: machine slots go to the player; anything else tries the upgrade slots, then the battery. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index <= 2) {
                if (!this.moveItemStackTo(stack, 3, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 1, 3, false)) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }
}
