package com.hbm.inventory.container;

import com.hbm.items.machine.IItemFluidId;
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
import com.hbm.blocks.ModProductionBlocks;
import com.hbm.inventory.ModProductionMenus;
import com.hbm.tileentity.machine.TileEntityMachineRotaryFurnace;

/** 1.7 {@code ContainerMachineRotaryFurnace}: three inputs, a fluid identifier, solid fuel; 176x186. */
public final class ContainerMachineRotaryFurnace extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 5;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineRotaryFurnace furnace;

    public ContainerMachineRotaryFurnace(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineRotaryFurnace(int containerId, Inventory playerInventory, TileEntityMachineRotaryFurnace furnace) {
        this(containerId, playerInventory, furnace, ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos()));
    }

    private ContainerMachineRotaryFurnace(int containerId, Inventory playerInventory,
                                          @Nullable TileEntityMachineRotaryFurnace furnace, ContainerLevelAccess access) {
        super(ModProductionMenus.ROTARY_FURNACE.get(), containerId);
        this.furnace = furnace;
        this.machine = furnace != null ? furnace : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        // Inputs
        addSlot(validated(0, 8, 18));
        addSlot(validated(1, 26, 18));
        addSlot(validated(2, 44, 18));
        // Fluid ID
        addSlot(validated(3, 8, 54));
        // Solid fuel
        addSlot(validated(4, 44, 54));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 104);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return furnace != null ? furnace.isItemValidForSlot(index, stack) || index == 3 : machine.canPlaceItem(index, stack);
            }
        };
    }

    private static @Nullable TileEntityMachineRotaryFurnace resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineRotaryFurnace furnace ? furnace : null;
    }

    public @Nullable TileEntityMachineRotaryFurnace getFurnace() { return furnace; }

    @Override
    public boolean stillValid(Player player) {
        return com.hbm.inventory.container.Containers.stillValidMultiblock(access, player,
                ModProductionBlocks.MACHINE_ROTARY_FURNACE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else if (Compat.burnDuration(player.level(), result) > 0) {
                if (!moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
            } else if (result.getItem() instanceof IItemFluidId) {
                if (!moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, 3, false)) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
