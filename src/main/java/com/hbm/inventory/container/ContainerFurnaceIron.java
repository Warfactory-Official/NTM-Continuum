package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityFurnaceIron;
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

/** Iron furnace menu: input, two fuel slots, output, upgrade, and player inventory. */
public class ContainerFurnaceIron extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 5;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityFurnaceIron furnace;

    public ContainerFurnaceIron(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, resolve(inventory, pos), ContainerLevelAccess.create(inventory.player.level(), pos));
    }

    public ContainerFurnaceIron(int id, Inventory inventory, TileEntityFurnaceIron furnace) {
        this(id, inventory, furnace, ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos()));
    }

    private ContainerFurnaceIron(int id, Inventory inventory, @Nullable TileEntityFurnaceIron furnace, ContainerLevelAccess access) {
        super(ModMenus.FURNACE_IRON.get(), id);
        this.furnace = furnace;
        this.machine = furnace != null ? furnace : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;
        addSlot(validated(0, 53, 17));
        addSlot(validated(1, 53, 53));
        addSlot(validated(2, 71, 53));
        addSlot(takeOnly(3, 125, 35));
        addSlot(validated(4, 17, 35));
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

    private static @Nullable TileEntityFurnaceIron resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityFurnaceIron furnace ? furnace : null;
    }

    public @Nullable TileEntityFurnaceIron getFurnace() { return furnace; }

    @Override public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.furnace_iron.get());
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
