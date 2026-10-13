package com.hbm.inventory.container;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.inventory.ModMenus;
import com.hbm.lib.Library;
import com.hbm.tileentity.bomb.TileEntityLaunchPadBase;
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
 * Menu for the launch pad, 1.12.2's {@code ContainerLaunchPadLarge}, which every pad type shares.
 *
 * <p>Slot layout is upstream's, unchanged: missile (26,36), designator (26,72), battery (107,90),
 * fuel in (125,90) / out (125,108), oxidiser in (143,90) / out (143,108). The screen is 176×236 with
 * the player inventory at y=154 and the hotbar at y=212; the missile preview needs the extra height.
 */
public class ContainerLaunchPad extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 7;
    private final Container machine;
    private final @Nullable TileEntityLaunchPadBase pad;

    public ContainerLaunchPad(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerLaunchPad(int containerId, Inventory playerInventory, @Nullable TileEntityLaunchPadBase pad) {
        super(ModMenus.LAUNCH_PAD.get(), containerId);
        this.pad = pad;
        this.machine = pad != null ? pad : new SimpleContainer(MACHINE_SLOTS);

        this.addSlot(validated(0, 26, 36));   // missile
        this.addSlot(designator(1, 26, 72));  // designator
        this.addSlot(battery(2, 107, 90));    // battery
        this.addSlot(anything(3, 125, 90));   // fuel in
        this.addSlot(takeOnly(4, 125, 108));  // emptied fuel container out
        this.addSlot(anything(5, 143, 90));   // oxidiser in
        this.addSlot(takeOnly(6, 143, 108));  // emptied oxidiser container out

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 154 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 212));
        }
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerLaunchPad.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot designator(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof IDesignatorItem; }
        };
    }

    private Slot battery(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return Library.isBattery(stack); }
        };
    }

    /** The fluid-in slots take any container; the pad decides whether it can drain it. */
    private Slot anything(int index, int x, int y) {
        return new Slot(this.machine, index, x, y);
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityLaunchPadBase resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityLaunchPadBase p ? p : null;
    }

    public @Nullable TileEntityLaunchPadBase getPad() {
        return pad;
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
            int invStart = MACHINE_SLOTS, invEnd = MACHINE_SLOTS + 36;

            if (index < invStart) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!this.moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
            } else if (pad != null && pad.isMissileValid(stack)) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IDesignatorItem) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else {
                // Anything else is treated as a fluid container: try the fuel slot, then the oxidiser.
                if (!this.moveItemStackTo(stack, 3, 4, false) && !this.moveItemStackTo(stack, 5, 6, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
