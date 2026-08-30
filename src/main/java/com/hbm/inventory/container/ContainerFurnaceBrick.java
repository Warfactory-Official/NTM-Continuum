package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityFurnaceBrick;
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

/** Brick furnace menu: input, fuel, output, ash, and the player inventory. */
public class ContainerFurnaceBrick extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 4;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityFurnaceBrick furnace;

    public ContainerFurnaceBrick(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, resolve(inventory, pos), ContainerLevelAccess.create(inventory.player.level(), pos));
    }

    public ContainerFurnaceBrick(int id, Inventory inventory, TileEntityFurnaceBrick furnace) {
        this(id, inventory, furnace, ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos()));
    }

    private ContainerFurnaceBrick(int id, Inventory inventory, @Nullable TileEntityFurnaceBrick furnace, ContainerLevelAccess access) {
        super(ModMenus.FURNACE_BRICK.get(), id);
        this.furnace = furnace;
        this.machine = furnace != null ? furnace : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;
        addSlot(validated(0, 62, 35));
        addSlot(validated(1, 35, 17));
        addSlot(takeOnly(2, 116, 35));
        addSlot(takeOnly(3, 35, 53));
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

    private static @Nullable TileEntityFurnaceBrick resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityFurnaceBrick furnace ? furnace : null;
    }

    public @Nullable TileEntityFurnaceBrick getFurnace() { return furnace; }

    @Override public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.machine_furnace_brick_off.get());
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else if (machine.canPlaceItem(1, stack) && !moveItemStackTo(stack, 1, 2, false)
                    || !machine.canPlaceItem(1, stack) && !moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
