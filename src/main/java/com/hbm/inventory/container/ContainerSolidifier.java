package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.oil.TileEntityMachineSolidifier;
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
 * Menu for the solidifier — 1.12.2 {@code ContainerSolidifier}: output (71,45, take-only), battery
 * (134,72), upgrades (98,36)/(98,54), fluid identifier (71,72); 176x204, player inventory at 122/180.
 */
public class ContainerSolidifier extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 5;
    private final Container machine;
    private final @Nullable TileEntityMachineSolidifier solidifier;

    public ContainerSolidifier(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerSolidifier(int containerId, Inventory playerInventory, @Nullable TileEntityMachineSolidifier solidifier) {
        super(ModMenus.MACHINE_SOLIDIFIER.get(), containerId);
        this.solidifier = solidifier;
        this.machine = solidifier != null ? solidifier : new SimpleContainer(MACHINE_SLOTS);

        this.addSlot(takeOnly(0, 71, 45));    // output
        this.addSlot(validated(1, 134, 72));  // battery
        this.addSlot(validated(2, 98, 36));   // upgrades
        this.addSlot(validated(3, 98, 54));
        this.addSlot(validated(4, 71, 72));   // fluid identifier

        for (int i = 0; i < 3; i++) for (int j = 0; j < 9; j++) this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 122 + i * 18));
        for (int i = 0; i < 9; i++) this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 180));
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerSolidifier.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineSolidifier resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineSolidifier s ? s : null;
    }

    public @Nullable TileEntityMachineSolidifier getSolidifier() { return solidifier; }

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
            } else if (Library.isMachineUpgrade(stack)) {
                if (!this.moveItemStackTo(stack, 2, 4, false)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidIdentifier) {
                if (!this.moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
