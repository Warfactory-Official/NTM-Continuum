package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityCrucible;
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
 * Menu for the crucible: the 3×3 input grid (machine slots 1–9, one item per slot) + the player
 * inventory — the 1.12.2 {@code ContainerCrucible} layout (slot 0 is unused, as in the original).
 * The screen reads the live (client) BlockEntity for the heat/progress bars and the molten stacks.
 */
public class ContainerCrucible extends AbstractContainerMenu {

    /** Menu slot count (the 3×3 grid; machine slot index = menu index + 1). */
    public static final int GRID_SLOTS = 9;

    private final Container machine;
    private final @Nullable TileEntityCrucible crucible;

    public ContainerCrucible(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerCrucible(int containerId, Inventory playerInventory, @Nullable TileEntityCrucible machine) {
        super(ModMenus.MACHINE_CRUCIBLE.get(), containerId);
        this.crucible = machine;
        this.machine = machine != null ? machine : new SimpleContainer(10);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(oneItem(j + i * 3 + 1, 107 + j * 18, 18 + i * 18));
            }
        }

        //? if >=26.1 {
        this.addStandardInventorySlots(playerInventory, 8, 132);
        //?} else {
        /*Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 132);
        *///?}
    }

    /** A grid slot: stack limit 1, gated by the machine's smeltability check. */
    private Slot oneItem(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerCrucible.this.machine.canPlaceItem(index, stack); }
            @Override public int getMaxStackSize() { return 1; }
        };
    }

    private static @Nullable TileEntityCrucible resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityCrucible machine ? machine : null;
    }

    /** The live (client) BlockEntity, for the screen. */
    public @Nullable TileEntityCrucible getCrucible() {
        return crucible;
    }

    @Override
    public boolean stillValid(Player player) {
        return crucible == null || crucible.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = GRID_SLOTS;
            int invEnd = GRID_SLOTS + 36;
            if (index < invStart) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, GRID_SLOTS, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
