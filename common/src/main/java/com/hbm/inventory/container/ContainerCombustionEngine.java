package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemPistons;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
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

/** Menu for the combustion engine — 1.12.2 {@code ContainerCombustionEngine} layout (176×203). Buttons:
 * 0 = ignition toggle, 1000..1030 = throttle. */
public class ContainerCombustionEngine extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 5;

    private final Container machine;
    private final @Nullable TileEntityMachineCombustionEngine te;

    public ContainerCombustionEngine(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, resolve(playerInv, pos));
    }

    public ContainerCombustionEngine(int id, Inventory playerInv, @Nullable TileEntityMachineCombustionEngine te) {
        super(ModMenus.MACHINE_COMBUSTION_ENGINE.get(), id);
        this.te = te;
        this.machine = te != null ? te : new SimpleContainer(MACHINE_SLOTS);

        this.addSlot(validated(0, 17, 17));   // fuel container in
        this.addSlot(takeOnly(1, 17, 53));    // emptied container out
        this.addSlot(validated(2, 88, 71));   // piston set
        this.addSlot(validated(3, 143, 71));  // battery
        this.addSlot(validated(4, 35, 71));   // fluid identifier

        for (int i = 0; i < 3; i++) for (int j = 0; j < 9; j++)
            this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 121 + i * 18));
        for (int i = 0; i < 9; i++) this.addSlot(new Slot(playerInv, i, 8 + i * 18, 179));

        if (te != null && !playerInv.player.level().isClientSide()) te.playersUsing++;
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerCombustionEngine.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineCombustionEngine resolve(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineCombustionEngine t ? t : null;
    }

    public @Nullable TileEntityMachineCombustionEngine getTile() { return te; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (te != null) { te.handleButton(id); return true; }
        return false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (te != null && !player.level().isClientSide() && te.playersUsing > 0) te.playersUsing--;
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
            int invStart = MACHINE_SLOTS;
            int invEnd = MACHINE_SLOTS + 36;
            if (index < invStart) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!this.moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemPistons) {
                if (!this.moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidIdentifier) {
                if (!this.moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
