package com.hbm.inventory.container;

import com.hbm.inventory.ModSatelliteMenus;
import com.hbm.items.machine.ItemSatChip;
import com.hbm.tileentity.machine.TileEntityMachineSatDock;
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
 * 1.7 {@code ContainerSatDock}: fifteen take-only cargo slots in three rows of five from (71,18), the chip slot
 * at (26,36) that takes only an {@code ItemSatChip} (a chip or designator, not a {@code satellite} item), and the
 * player inventory at (8,104).
 */
public final class ContainerSatDock extends AbstractContainerMenu {

    private final Container machine;
    private final @Nullable TileEntityMachineSatDock dock;

    public ContainerSatDock(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityMachineSatDock d ? d : null);
    }

    public ContainerSatDock(int containerId, Inventory playerInventory, @Nullable TileEntityMachineSatDock dock) {
        super(ModSatelliteMenus.SAT_DOCK.get(), containerId);
        this.dock = dock;
        this.machine = dock != null ? dock : new SimpleContainer(16);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                this.addSlot(new PneumaticMenuBase.SlotTakeOnly(machine, j + i * 5, 71 + 18 * j, 18 + 18 * i));
            }
        }

        this.addSlot(new Slot(machine, 15, 26, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ItemSatChip;
            }
        });

        dev.golem.core.world.Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 104);
    }

    public @Nullable TileEntityMachineSatDock getDock() {
        return dock;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            // 1.7: from the pad to the player; from the player into slots 0..14. 1.7's mergeItemStack ignored
            // isItemValid and so filled the take-only slots; moveItemStackTo honours mayPlace, as in every other
            // container of this port, so the shift-click finds no room.
            if (index <= 15) {
                if (!this.moveItemStackTo(stack, 16, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, 15, false)) {
                return ItemStack.EMPTY;
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
