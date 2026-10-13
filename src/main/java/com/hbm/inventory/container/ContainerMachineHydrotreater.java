package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.items.ModItems;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.oil.TileEntityMachineHydrotreater;
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
 * Menu for the hydrotreater: 1.7.10 {@code ContainerMachineHydrotreater} layout (176×238), 11 machine
 * slots. Slots 3/4 (hydrogen containers) are 1.7's {@code SlotDeprecated}: they keep old items but accept
 * nothing and are neither drawn nor hoverable ({@link Slot#isActive()} false).
 */
public class ContainerMachineHydrotreater extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 11;

    private final Container machine;
    private final @Nullable TileEntityMachineHydrotreater te;

    public ContainerMachineHydrotreater(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, resolve(playerInv, pos));
    }

    public ContainerMachineHydrotreater(int id, Inventory playerInv, @Nullable TileEntityMachineHydrotreater te) {
        super(ModMenus.MACHINE_HYDROTREATER.get(), id);
        this.te = te;
        this.machine = te != null ? te : new SimpleContainer(MACHINE_SLOTS);

        this.addSlot(validated(0, 17, 90));    // battery
        this.addSlot(validated(1, 35, 90));    // canister input
        this.addSlot(takeOnly(2, 35, 108));    // canister output
        this.addSlot(deprecated(3, 53, 90));   // hydrogen input (removed, requires pressurization)
        this.addSlot(deprecated(4, 53, 108));  // hydrogen output (samesies)
        this.addSlot(validated(5, 125, 90));   // desulfurized oil input
        this.addSlot(takeOnly(6, 125, 108));
        this.addSlot(validated(7, 143, 90));   // sour gas input
        this.addSlot(takeOnly(8, 143, 108));
        this.addSlot(validated(9, 17, 108));   // fluid identifier
        this.addSlot(validated(10, 89, 36));   // catalyst

        for (int i = 0; i < 3; i++) for (int j = 0; j < 9; j++)
            this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 156 + i * 18));
        for (int i = 0; i < 9; i++) this.addSlot(new Slot(playerInv, i, 8 + i * 18, 214));
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachineHydrotreater.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    /** 1.7 {@code SlotDeprecated}: holds legacy items, otherwise uninteractable and invisible. */
    private Slot deprecated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean isActive() { return false; }
        };
    }

    private static @Nullable TileEntityMachineHydrotreater resolve(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineHydrotreater t ? t : null;
    }

    public @Nullable TileEntityMachineHydrotreater getTile() { return te; }

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
            } else if (stack.getItem() == ModItems.catalytic_converter.get()) {
                if (!this.moveItemStackTo(stack, 10, 11, false)) return ItemStack.EMPTY;
            } else {
                // 1.7 order 1, 3, 5, 7; slot 3 is deprecated and refuses everything.
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
