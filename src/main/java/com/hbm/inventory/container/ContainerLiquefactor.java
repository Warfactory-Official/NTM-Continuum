package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor;
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
 * Menu for the liquefactor, the 1.12.2 {@code ContainerLiquefactor}: input (35,54), battery (134,72),
 * upgrades (98,36)/(98,54); 176x204 with the player inventory at 122/180.
 */
public class ContainerLiquefactor extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 4;
    private final Container machine;
    private final @Nullable TileEntityMachineLiquefactor liquefactor;

    public ContainerLiquefactor(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerLiquefactor(int containerId, Inventory playerInventory, @Nullable TileEntityMachineLiquefactor liquefactor) {
        super(ModMenus.MACHINE_LIQUEFACTOR.get(), containerId);
        this.liquefactor = liquefactor;
        this.machine = liquefactor != null ? liquefactor : new SimpleContainer(MACHINE_SLOTS);

        this.addSlot(validated(0, 35, 54));   // input
        this.addSlot(validated(1, 134, 72));  // battery
        this.addSlot(validated(2, 98, 36));   // upgrades
        this.addSlot(validated(3, 98, 54));

        for (int i = 0; i < 3; i++) for (int j = 0; j < 9; j++) this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 122 + i * 18));
        for (int i = 0; i < 9; i++) this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 180));
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerLiquefactor.this.machine.canPlaceItem(index, stack); }
        };
    }

    private static @Nullable TileEntityMachineLiquefactor resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineLiquefactor l ? l : null;
    }

    public @Nullable TileEntityMachineLiquefactor getLiquefactor() { return liquefactor; }

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
            } else {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
