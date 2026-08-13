package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityFireboxBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Menu for the firebox family: two fuel slots + the player inventory (the 1.12.2
 * {@code ContainerFirebox} layout). The screen reads the live (client) BlockEntity for the heat/burn
 * gauges.
 */
public class ContainerFirebox extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 2;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityFireboxBase firebox;
    /** True once this menu counted itself into the firebox's {@code playersUsing} (server side only). */
    private boolean countedOpen = false;

    public ContainerFirebox(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerFirebox(int containerId, Inventory playerInventory, TileEntityFireboxBase machine) {
        this(containerId, playerInventory, machine, ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()));
    }

    private ContainerFirebox(int containerId, Inventory playerInventory, @Nullable TileEntityFireboxBase machine, ContainerLevelAccess access) {
        super(ModMenus.HEATER_FIREBOX.get(), containerId);
        this.firebox = machine;
        this.machine = machine != null ? machine : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        // Count this player into the door-swing counter (server-authoritative; the client value is synced).
        if (firebox != null && firebox.getLevel() != null && !firebox.getLevel().isClientSide()) {
            firebox.setPlayersUsing(firebox.playersUsing + 1);
            countedOpen = true;
        }

        this.addSlot(validated(0, 44, 27)); // fuel
        this.addSlot(validated(1, 62, 27)); // fuel

        //? if >=26.1 {
        this.addStandardInventorySlots(playerInventory, 8, 86);
        //?} else {
        /*Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 86);
        *///?}
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (countedOpen && firebox != null) {
            firebox.setPlayersUsing(firebox.playersUsing - 1);
            countedOpen = false;
        }
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerFirebox.this.machine.canPlaceItem(index, stack); }
        };
    }

    private static @Nullable TileEntityFireboxBase resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityFireboxBase machine ? machine : null;
    }

    /** The live (client) BlockEntity, for the screen to read the heat/burn gauges. */
    public @Nullable TileEntityFireboxBase getFirebox() {
        return firebox;
    }

    @Override
    public boolean stillValid(Player player) {
        return firebox == null || firebox.stillValid(player);
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
            } else {
                if (!this.moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
