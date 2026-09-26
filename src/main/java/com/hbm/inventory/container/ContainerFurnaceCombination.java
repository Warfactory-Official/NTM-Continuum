package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityFurnaceCombination;
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

/** The 1.12.2 combination furnace menu: recipe input, item output, container input, container output. */
public class ContainerFurnaceCombination extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 4;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityFurnaceCombination furnace;

    public ContainerFurnaceCombination(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerFurnaceCombination(int id, Inventory playerInventory, TileEntityFurnaceCombination furnace) {
        this(id, playerInventory, furnace, ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos()));
    }

    private ContainerFurnaceCombination(int id, Inventory playerInventory,
                                         @Nullable TileEntityFurnaceCombination furnace, ContainerLevelAccess access) {
        super(ModMenus.FURNACE_COMBINATION.get(), id);
        this.furnace = furnace;
        this.machine = furnace != null ? furnace : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 26, 36));
        this.addSlot(takeOnly(1, 89, 36));
        this.addSlot(validated(2, 136, 18));
        this.addSlot(takeOnly(3, 136, 54));

        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
        }
        for (int col = 0; col < 9; col++) this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 162));
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityFurnaceCombination resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity entity = playerInventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityFurnaceCombination furnace ? furnace : null;
    }

    public @Nullable TileEntityFurnaceCombination getFurnace() { return furnace; }

    @Override
    public boolean stillValid(Player player) {
        return Menus.stillValid(access, player, com.hbm.blocks.machine.ModMachineBlocks.machine_furnace_combination.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MACHINE_SLOTS;
            int invEnd = MACHINE_SLOTS + 36;
            if (index < invStart) {
                if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 1, false)
                    && !moveItemStackTo(stack, 2, 3, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
