package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.oil.TileEntityMachineVacuumDistill;
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

/** Menu for the vacuum distiller — 1.12.2 {@code ContainerMachineVacuumDistill} layout (176×238). */
public class ContainerMachineVacuumDistill extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 10;

    private final Container machine;
    private final @Nullable TileEntityMachineVacuumDistill te;

    public ContainerMachineVacuumDistill(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, resolve(playerInv, pos));
    }

    public ContainerMachineVacuumDistill(int id, Inventory playerInv, @Nullable TileEntityMachineVacuumDistill te) {
        super(ModMenus.MACHINE_VACUUM_DISTILL.get(), id);
        this.te = te;
        this.machine = te != null ? te : new SimpleContainer(MACHINE_SLOTS);

        this.addSlot(validated(0, 26, 90));   // battery
        this.addSlot(validated(1, 80, 90));   // heavy load
        this.addSlot(takeOnly(2, 80, 108));
        this.addSlot(validated(3, 98, 90));   // reformate load
        this.addSlot(takeOnly(4, 98, 108));
        this.addSlot(validated(5, 116, 90));  // light load
        this.addSlot(takeOnly(6, 116, 108));
        this.addSlot(validated(7, 134, 90));  // sourgas load
        this.addSlot(takeOnly(8, 134, 108));
        this.addSlot(validated(9, 26, 108));  // fluid identifier

        for (int i = 0; i < 3; i++) for (int j = 0; j < 9; j++)
            this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 156 + i * 18));
        for (int i = 0; i < 9; i++) this.addSlot(new Slot(playerInv, i, 8 + i * 18, 214));
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachineVacuumDistill.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineVacuumDistill resolve(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineVacuumDistill t ? t : null;
    }

    public @Nullable TileEntityMachineVacuumDistill getTile() { return te; }

    @Override
    public boolean stillValid(Player player) { return this.machine.stillValid(player); }

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
            } else if (Library.isBattery(stack)) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidId) {
                if (!this.moveItemStackTo(stack, 9, 10, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 1, 2, false)
                        && !this.moveItemStackTo(stack, 3, 4, false)
                        && !this.moveItemStackTo(stack, 5, 6, false)
                        && !this.moveItemStackTo(stack, 7, 8, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
