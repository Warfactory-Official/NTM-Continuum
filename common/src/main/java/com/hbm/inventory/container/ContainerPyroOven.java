package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven;
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

/** Menu for the pyro oven — 1.12.2 {@code ContainerPyroOven} layout (176×204). */
public class ContainerPyroOven extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 6;

    private final Container machine;
    private final @Nullable TileEntityMachinePyroOven te;

    public ContainerPyroOven(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, resolve(playerInv, pos));
    }

    public ContainerPyroOven(int id, Inventory playerInv, @Nullable TileEntityMachinePyroOven te) {
        super(ModMenus.MACHINE_PYROOVEN.get(), id);
        this.te = te;
        this.machine = te != null ? te : new SimpleContainer(MACHINE_SLOTS);

        this.addSlot(validated(0, 152, 72));  // battery
        this.addSlot(validated(1, 35, 45));   // input
        this.addSlot(takeOnly(2, 89, 45));    // output
        this.addSlot(validated(3, 8, 72));    // fluid identifier
        this.addSlot(validated(4, 71, 72));   // upgrade
        this.addSlot(validated(5, 89, 72));   // upgrade

        for (int i = 0; i < 3; i++) for (int j = 0; j < 9; j++)
            this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 122 + i * 18));
        for (int i = 0; i < 9; i++) this.addSlot(new Slot(playerInv, i, 8 + i * 18, 180));
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerPyroOven.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachinePyroOven resolve(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachinePyroOven t ? t : null;
    }

    public @Nullable TileEntityMachinePyroOven getTile() { return te; }

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
            } else if (Library.isMachineUpgrade(stack)) {
                if (!this.moveItemStackTo(stack, 4, 6, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidIdentifier) {
                if (!this.moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
