package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.tileentity.machine.TileEntityHeaterOilburner;
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
 * Menu for the fluid burner: 1.7 {@code ContainerOilburner} layout. Fluid container in (26,17),
 * emptied container out (26,53), fluid identifier (44,71), upgrade (80,73); 176×203 with the player
 * inventory offset 37. Buttons: 0 = burn toggle, 1000 + rate = burn rate slider.
 */
public class ContainerOilburner extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 4;

    private final Container machine;
    private final @Nullable TileEntityHeaterOilburner heater;

    public ContainerOilburner(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerOilburner(int containerId, Inventory playerInventory, @Nullable TileEntityHeaterOilburner heater) {
        super(ModMenus.HEATER_OILBURNER.get(), containerId);
        this.heater = heater;
        this.machine = heater != null ? heater : new SimpleContainer(MACHINE_SLOTS);

        this.addSlot(validated(0, 26, 17));
        this.addSlot(takeOnly(1, 26, 53));
        this.addSlot(validated(2, 44, 71));
        this.addSlot(validated(3, 80, 73));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 121);
        // rows 121/139/157, hotbar 179 (old offset 37)
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerOilburner.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityHeaterOilburner resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityHeaterOilburner heater ? heater : null;
    }

    public @Nullable TileEntityHeaterOilburner getHeater() {
        return heater;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (heater == null) return false;
        if (id != 0 && (id <= 1000 || id > 1000 + TileEntityHeaterOilburner.MAX_SLIDER_RATE)) return false;
        heater.handleButtonPacket(id, 0);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.machine.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MACHINE_SLOTS;
            int invEnd = MACHINE_SLOTS + 36;
            if (index < invStart) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidId) {
                if (!this.moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
