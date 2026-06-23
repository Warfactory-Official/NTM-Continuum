package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityMachinePress;
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

public class ContainerMachinePress extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 13;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachinePress press;

    public ContainerMachinePress(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachinePress(int containerId, Inventory playerInventory, TileEntityMachinePress press) {
        this(containerId, playerInventory, press, ContainerLevelAccess.create(press.getLevel(), press.getBlockPos()));
    }

    private ContainerMachinePress(int containerId, Inventory playerInventory, @Nullable TileEntityMachinePress press, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_PRESS.get(), containerId);
        this.press = press;
        this.machine = press != null ? press : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 26, 53));
        this.addSlot(validated(1, 80, 17));
        this.addSlot(validated(2, 80, 53));
        this.addSlot(new Slot(this.machine, 3, 140, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int i = 0; i < 9; i++) this.addSlot(validated(4 + i, 8 + i * 18, 84));

        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 120 + row * 18));
        }
        for (int col = 0; col < 9; col++) this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 178));
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachinePress.this.machine.canPlaceItem(index, stack); }
        };
    }

    private static @Nullable TileEntityMachinePress resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachinePress p ? p : null;
    }

    public @Nullable TileEntityMachinePress getPress() {
        return press;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModMachineBlocks.machine_press.get());
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
            } else {

                if (!this.moveItemStackTo(stack, 0, 3, false) && !this.moveItemStackTo(stack, 4, MACHINE_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
