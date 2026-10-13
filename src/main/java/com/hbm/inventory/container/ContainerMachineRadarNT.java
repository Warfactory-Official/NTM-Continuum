package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.ModRadarUtilityMenus;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
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
 * 1.7 {@code ContainerMachineRadarNT}: eight link slots in a row at y=17, the screen-link slot at (26,44), the
 * battery at (152,44), and the player inventory at (8,103). Shift-click sends batteries to the battery slot
 * and everything else to the first free link slot.
 */
public final class ContainerMachineRadarNT extends AbstractContainerMenu {

    private final Container machine;
    private final @Nullable TileEntityMachineRadarNT radar;

    public ContainerMachineRadarNT(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityMachineRadarNT r ? r : null);
    }

    public ContainerMachineRadarNT(int containerId, Inventory playerInventory, @Nullable TileEntityMachineRadarNT radar) {
        super(ModRadarUtilityMenus.RADAR.get(), containerId);
        this.radar = radar;
        this.machine = radar != null ? radar : new SimpleContainer(10);

        for (int i = 0; i < 8; i++) this.addSlot(new Slot(machine, i, 26 + i * 18, 17));

        this.addSlot(new Slot(machine, 8, 26, 44));
        this.addSlot(new Slot(machine, 9, 152, 44));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 103);
    }

    public @Nullable TileEntityMachineRadarNT getRadar() { return radar; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index <= 9) {
                if (!this.moveItemStackTo(stack, 10, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (result.getItem() instanceof IBatteryItem) {
                if (!this.moveItemStackTo(stack, 9, 10, false)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, 9, false)) {
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
