package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityFurnaceSteel;
import dev.golem.core.world.Compat;
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

/** Steel furnace menu: three input lanes, three output lanes, and player inventory. */
public class ContainerFurnaceSteel extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 6;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityFurnaceSteel furnace;

    public ContainerFurnaceSteel(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, resolve(inventory, pos), ContainerLevelAccess.create(inventory.player.level(), pos));
    }

    public ContainerFurnaceSteel(int id, Inventory inventory, TileEntityFurnaceSteel furnace) {
        this(id, inventory, furnace, ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos()));
    }

    private ContainerFurnaceSteel(int id, Inventory inventory, @Nullable TileEntityFurnaceSteel furnace, ContainerLevelAccess access) {
        super(ModMenus.FURNACE_STEEL.get(), id);
        this.furnace = furnace;
        this.machine = furnace != null ? furnace : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;
        addSlot(validated(0, 35, 17));
        addSlot(validated(1, 35, 35));
        addSlot(validated(2, 35, 53));
        addSlot(takeOnly(3, 125, 17));
        addSlot(takeOnly(4, 125, 35));
        addSlot(takeOnly(5, 125, 53));
        Compat.addPlayerInventory(this::addSlot, inventory, 8, 84);
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

    private static @Nullable TileEntityFurnaceSteel resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityFurnaceSteel furnace ? furnace : null;
    }

    public @Nullable TileEntityFurnaceSteel getFurnace() { return furnace; }

    @Override public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.furnace_steel.get());
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 3, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
