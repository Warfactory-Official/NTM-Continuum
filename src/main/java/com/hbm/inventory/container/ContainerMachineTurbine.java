package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineTurbine;
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
 * Menu for the steam turbine — 1.12.2 {@code ContainerMachineTurbine}: fluid identifier in (8,17) and out
 * (8,53), input canister bay (44,17) → (44,53), battery (98,53), output canister bay (152,17) → (152,53);
 * 176×168 with the player inventory at 84/142.
 */
public class ContainerMachineTurbine extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 7;
    private final Container machine;
    private final @Nullable TileEntityMachineTurbine turbine;

    public ContainerMachineTurbine(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerMachineTurbine(int containerId, Inventory playerInventory, @Nullable TileEntityMachineTurbine turbine) {
        super(ModMenus.MACHINE_TURBINE.get(), containerId);
        this.turbine = turbine;
        this.machine = turbine != null ? turbine : new SimpleContainer(MACHINE_SLOTS);

        this.addSlot(validated(0, 8, 17));    // fluid identifier in
        this.addSlot(takeOnly(1, 8, 53));     // identifier out
        this.addSlot(validated(2, 44, 17));   // input tank: container in
        this.addSlot(takeOnly(3, 44, 53));    // input tank: container out
        this.addSlot(validated(4, 98, 53));   // battery (charged from the TE)
        this.addSlot(validated(5, 152, 17));  // output tank: container in
        this.addSlot(takeOnly(6, 152, 53));   // output tank: container out

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachineTurbine.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineTurbine resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineTurbine t ? t : null;
    }

    public @Nullable TileEntityMachineTurbine getTurbine() { return turbine; }

    @Override
    public boolean stillValid(Player player) { return this.machine.stillValid(player); }

    /**
     * The old {@code TransferStrategy}: identifiers to the identifier bay, batteries to the battery slot,
     * a container already holding the turbine's input fluid to the input bay, anything else to the output
     * bay (where empty canisters get filled with the spent steam).
     */
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
            } else if (stack.getItem() instanceof IItemFluidId) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!this.moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
            } else if (this.machine.canPlaceItem(2, stack)) {
                if (!this.moveItemStackTo(stack, 2, 3, false) && !this.moveItemStackTo(stack, 5, 6, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
