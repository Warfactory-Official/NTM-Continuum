package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare;
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
 * Menu for the flare stack — 1.12.2 {@code ContainerMachineGasFlare}: battery (143,71), fluid container
 * in (17,17), emptied out (17,53), fluid identifier (35,71), upgrades (80,71)/(98,71); 176×203 with the
 * player inventory offset 37 (rows 121/139/157, hotbar 179). Buttons: 0 = valve, 1 = ignition.
 */
public class ContainerMachineGasFlare extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 6;
    private final Container machine;
    private final @Nullable TileEntityMachineGasFlare flare;

    public ContainerMachineGasFlare(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerMachineGasFlare(int containerId, Inventory playerInventory, @Nullable TileEntityMachineGasFlare flare) {
        super(ModMenus.MACHINE_GAS_FLARE.get(), containerId);
        this.flare = flare;
        this.machine = flare != null ? flare : new SimpleContainer(MACHINE_SLOTS);

        this.addSlot(validated(0, 143, 71));  // battery (charged from the TE)
        this.addSlot(validated(1, 17, 17));   // fluid container in
        this.addSlot(takeOnly(2, 17, 53));    // emptied out
        this.addSlot(validated(3, 35, 71));   // fluid identifier
        this.addSlot(validated(4, 80, 71));   // upgrades
        this.addSlot(validated(5, 98, 71));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 121);
        // rows 121/139/157, hotbar 179 (old offset 37)
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachineGasFlare.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineGasFlare resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineGasFlare f ? f : null;
    }

    public @Nullable TileEntityMachineGasFlare getFlare() { return flare; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (flare == null) return false;
        if (id < 0 || id > 1) return false;
        flare.handleButtonPacket(id, 0);
        return true;
    }

    @Override
    public boolean stillValid(Player player) { return this.machine.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MACHINE_SLOTS, invEnd = MACHINE_SLOTS + 36;
            if (index < invStart) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (Library.isMachineUpgrade(stack)) {
                if (!this.moveItemStackTo(stack, 4, 6, false)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidId) {
                if (!this.moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
