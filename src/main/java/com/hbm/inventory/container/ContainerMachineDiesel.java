package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.inventory.ModMenus;
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
 * Menu for the diesel generator — 1.7.10 {@code ContainerMachineDiesel}: fuel container in (17,17),
 * emptied out (17,53), battery (141,71), fluid identifier (35,71); 176×203 with the player inventory at
 * 121/179. Button 0 is the on/off switch.
 */
public class ContainerMachineDiesel extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 4;
    private final Container machine;
    private final @Nullable TileEntityMachineDiesel diesel;

    public ContainerMachineDiesel(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerMachineDiesel(int containerId, Inventory playerInventory, @Nullable TileEntityMachineDiesel diesel) {
        super(ModMenus.MACHINE_DIESEL.get(), containerId);
        this.diesel = diesel;
        this.machine = diesel != null ? diesel : new SimpleContainer(MACHINE_SLOTS);

        this.addSlot(validated(0, 17, 17));   // fuel container in
        this.addSlot(takeOnly(1, 17, 53));    // emptied container out
        this.addSlot(validated(2, 141, 71));  // battery (charged from the TE)
        this.addSlot(validated(3, 35, 71));   // fluid identifier

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 121);
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

    /** Button 0: the 1.7 "turnOn" control. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (diesel == null || id != 0) return false;
        diesel.handleButtonPacket(0, 0);
        return true;
    }

    /**
     * 1.7 {@code transferStackInSlot}, odd bounds kept: indices up to 4 (the four machine slots AND the
     * first player-inventory slot) go to the player inventory from index 5; anything else tries slot 0,
     * then the battery slot 2, then index 4.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index <= 4) {
                if (!this.moveItemStackTo(stack, 5, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, 1, false)) {
                if (!this.moveItemStackTo(stack, 2, 3, false))
                    if (!this.moveItemStackTo(stack, 4, 5, false))
                        return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
