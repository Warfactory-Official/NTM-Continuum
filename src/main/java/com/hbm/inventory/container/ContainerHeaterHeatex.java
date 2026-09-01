package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityHeaterHeatex;
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

/** The legacy Heatex menu: one identifier slot followed by the 36 player slots. */
public class ContainerHeaterHeatex extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 1;

    private final Container machine;
    private final @Nullable TileEntityHeaterHeatex heater;

    public ContainerHeaterHeatex(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerHeaterHeatex(int containerId, Inventory playerInventory,
                                 @Nullable TileEntityHeaterHeatex heater) {
        super(ModMenus.HEATER_HEATEX.get(), containerId);
        this.heater = heater;
        this.machine = heater != null ? heater : new SimpleContainer(MACHINE_SLOTS);

        // The old ItemStackHandler accepted any item here; only IItemFluidId changes the tank type.
        addSlot(new Slot(this.machine, 0, 80, 72));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 122);
    }

    private static @Nullable TileEntityHeaterHeatex resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity entity = playerInventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityHeaterHeatex heater ? heater : null;
    }

    public @Nullable TileEntityHeaterHeatex getHeater() {
        return heater;
    }

    /** Heatex had no vanilla menu buttons; amount and delay use the control payload path from the GUI. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
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
            int inventoryStart = MACHINE_SLOTS;
            int inventoryEnd = MACHINE_SLOTS + 36;
            if (index < inventoryStart) {
                if (!moveItemStackTo(stack, inventoryStart, inventoryEnd, true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
