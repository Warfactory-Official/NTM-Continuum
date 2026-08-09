package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.oil.TileEntityMachineRefinery;
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
 * Menu for the refinery — 1.12.2 {@code ContainerMachineRefinery} layout (210×231): battery (186,72),
 * input container pair (8,99)/(8,119), four output pairs at x=86/106/126/146 (99/119), sulfur out
 * (58,119), fluid identifier (186,106); player inventory at 150/208.
 */
public class ContainerMachineRefinery extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 13;

    private final Container machine;
    private final @Nullable TileEntityMachineRefinery refinery;

    public ContainerMachineRefinery(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerMachineRefinery(int containerId, Inventory playerInventory, @Nullable TileEntityMachineRefinery refinery) {
        super(ModMenus.MACHINE_REFINERY.get(), containerId);
        this.refinery = refinery;
        this.machine = refinery != null ? refinery : new SimpleContainer(MACHINE_SLOTS);

        this.addSlot(validated(0, 186, 72));   // battery
        this.addSlot(validated(1, 8, 99));     // input container in
        this.addSlot(takeOnly(2, 8, 119));     // input container out
        this.addSlot(validated(3, 86, 99));    // heavy oil out container in
        this.addSlot(takeOnly(4, 86, 119));
        this.addSlot(validated(5, 106, 99));   // naphtha
        this.addSlot(takeOnly(6, 106, 119));
        this.addSlot(validated(7, 126, 99));   // light oil
        this.addSlot(takeOnly(8, 126, 119));
        this.addSlot(validated(9, 146, 99));   // petroleum
        this.addSlot(takeOnly(10, 146, 119));
        this.addSlot(takeOnly(11, 58, 119));   // sulfur / byproduct
        this.addSlot(validated(12, 186, 106)); // fluid identifier

        // player inventory: main rows at y=150, hotbar y=208 (old layout)
        Inventory inv = playerInventory;
        for (int i = 0; i < 3; i++) for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 150 + i * 18));
        }
        for (int i = 0; i < 9; i++) this.addSlot(new Slot(inv, i, 8 + i * 18, 208));
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachineRefinery.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineRefinery resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineRefinery refinery ? refinery : null;
    }

    public @Nullable TileEntityMachineRefinery getRefinery() {
        return refinery;
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
            } else if (Library.isBattery(stack)) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidIdentifier) {
                if (!this.moveItemStackTo(stack, 12, 13, false)) return ItemStack.EMPTY;
            } else {
                // fluid containers → input load, then the four output loads
                if (!this.moveItemStackTo(stack, 1, 2, false)
                        && !this.moveItemStackTo(stack, 3, 4, false)
                        && !this.moveItemStackTo(stack, 5, 6, false)
                        && !this.moveItemStackTo(stack, 7, 8, false)
                        && !this.moveItemStackTo(stack, 9, 10, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
