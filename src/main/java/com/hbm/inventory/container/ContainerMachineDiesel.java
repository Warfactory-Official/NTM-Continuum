package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineDiesel;
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
 * Menu for the diesel generator — 1.12.2 {@code ContainerMachineDiesel}: fuel container in (44,17),
 * emptied out (44,53), battery (116,53), fluid identifier (8,17), identifier-out (8,53); 176×166 with
 * the player inventory at 84/142.
 */
public class ContainerMachineDiesel extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 5;
    private final Container machine;
    private final @Nullable TileEntityMachineDiesel diesel;

    public ContainerMachineDiesel(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerMachineDiesel(int containerId, Inventory playerInventory, @Nullable TileEntityMachineDiesel diesel) {
        super(ModMenus.MACHINE_DIESEL.get(), containerId);
        this.diesel = diesel;
        this.machine = diesel != null ? diesel : new SimpleContainer(MACHINE_SLOTS);

        this.addSlot(validated(0, 44, 17));   // fuel container in
        this.addSlot(takeOnly(1, 44, 53));    // emptied container out
        this.addSlot(validated(2, 116, 53));  // battery (charged from the TE)
        this.addSlot(validated(3, 8, 17));    // fluid identifier
        this.addSlot(takeOnly(4, 8, 53));     // identifier out

        //? if >=26.1 {
        this.addStandardInventorySlots(playerInventory, 8, 84);
        //?} else {
        /*Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
        *///?}
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachineDiesel.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineDiesel resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineDiesel d ? d : null;
    }

    public @Nullable TileEntityMachineDiesel getDiesel() { return diesel; }

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
            } else if (Library.isBattery(stack)) {
                if (!this.moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidId) {
                if (!this.moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
