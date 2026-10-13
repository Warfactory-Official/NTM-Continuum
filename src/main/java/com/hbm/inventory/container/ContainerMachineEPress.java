package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.tileentity.machine.TileEntityMachineEPress;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** Menu for the electric press: battery, stamp, input, output, upgrade (+ player inv). */
public class ContainerMachineEPress extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 5;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineEPress press;

    public ContainerMachineEPress(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineEPress(int containerId, Inventory playerInventory, TileEntityMachineEPress press) {
        this(containerId, playerInventory, press, ContainerLevelAccess.create(press.getLevel(), press.getBlockPos()));
    }

    private ContainerMachineEPress(int containerId, Inventory playerInventory, @Nullable TileEntityMachineEPress press, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_EPRESS.get(), containerId);
        this.press = press;
        this.machine = press != null ? press : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 152, 54));  // battery (1.7 ContainerMachineEPress positions)
        this.addSlot(validated(1, 19, 15));    // stamp
        this.addSlot(validated(2, 19, 51));    // input
        this.addSlot(new Slot(this.machine, 3, 79, 33) { // output (take only)
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        this.addSlot(validated(4, 111, 32));   // upgrade

        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
        }
        for (int col = 0; col < 9; col++) this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 162));
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachineEPress.this.machine.canPlaceItem(index, stack); }
        };
    }

    private static @Nullable TileEntityMachineEPress resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineEPress p ? p : null;
    }

    public @Nullable TileEntityMachineEPress getPress() {
        return press;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.machine_epress.get());
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
            } else if (stack.getItem() instanceof ItemMachineUpgrade) {
                // upgrades go straight to the upgrade slot (the 1.12.2 transferStackInSlot routing)
                if (!this.moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
            } else {
                // player inv -> machine: battery (0), stamp (1), input (2); skip output (3)
                if (!this.moveItemStackTo(stack, 0, 3, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
