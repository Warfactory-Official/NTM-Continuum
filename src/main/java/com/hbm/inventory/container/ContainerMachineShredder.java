package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.ItemBlades;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineShredder;
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

/**
 * Menu for the shredder: a 3×3 input grid (0-8), a 3×6 output grid (9-26), two blade slots (27/28) and a
 * battery slot (29), plus the player inventory. The GUI is 233px tall, so the player inventory sits 67px
 * lower than the 166px default — matching the 1.12.2 layout.
 */
public class ContainerMachineShredder extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 30;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineShredder shredder;

    public ContainerMachineShredder(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineShredder(int containerId, Inventory playerInventory, TileEntityMachineShredder shredder) {
        this(containerId, playerInventory, shredder,
                ContainerLevelAccess.create(shredder.getLevel(), shredder.getBlockPos()));
    }

    private ContainerMachineShredder(int containerId, Inventory playerInventory,
                                     @Nullable TileEntityMachineShredder shredder, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_SHREDDER.get(), containerId);
        this.shredder = shredder;
        this.machine = shredder != null ? shredder : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {   // 0-8 inputs
            this.addSlot(validated(col + row * 3, 44 + col * 18, 18 + row * 18));
        }
        for (int row = 0; row < 6; row++) for (int col = 0; col < 3; col++) {   // 9-26 outputs (take only)
            this.addSlot(takeOnly(9 + col + row * 3, 116 + col * 18, 18 + row * 18));
        }
        this.addSlot(validated(TileEntityMachineShredder.SLOT_BLADE_LEFT, 44, 108));
        this.addSlot(validated(TileEntityMachineShredder.SLOT_BLADE_RIGHT, 80, 108));
        this.addSlot(validated(TileEntityMachineShredder.SLOT_BATTERY, 8, 108));

        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18 + 67));
        }
        for (int col = 0; col < 9; col++) this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142 + 67));
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return ContainerMachineShredder.this.machine.canPlaceItem(index, stack);
            }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineShredder resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineShredder s ? s : null;
    }

    public @Nullable TileEntityMachineShredder getShredder() {
        return shredder;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModMachineBlocks.machine_shredder.get());
    }

    /**
     * Shift-click routing (the old {@code TransferStrategy}): blades go to the blade slots, batteries to
     * the battery slot, everything else into the input grid; machine → player otherwise.
     */
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
            } else if (stack.getItem() instanceof ItemBlades) {
                if (!this.moveItemStackTo(stack, TileEntityMachineShredder.SLOT_BLADE_LEFT,
                        TileEntityMachineShredder.SLOT_BLADE_RIGHT + 1, false)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!this.moveItemStackTo(stack, TileEntityMachineShredder.SLOT_BATTERY,
                        TileEntityMachineShredder.SLOT_BATTERY + 1, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, 9, false)) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
