package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineLargeTurbine;
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

/** Menu for the legacy four-long steam turbine. */
public class ContainerMachineLargeTurbine extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 7;
    private final Container machine;
    private final @Nullable TileEntityMachineLargeTurbine turbine;

    public ContainerMachineLargeTurbine(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerMachineLargeTurbine(int containerId, Inventory playerInventory,
                                        @Nullable TileEntityMachineLargeTurbine turbine) {
        super(ModMenus.MACHINE_LARGE_TURBINE.get(), containerId);
        this.turbine = turbine;
        this.machine = turbine != null ? turbine : new SimpleContainer(MACHINE_SLOTS);

        addSlot(validated(0, 8, 17));
        addSlot(takeOnly(1, 8, 53));
        addSlot(validated(2, 44, 17));
        addSlot(takeOnly(3, 44, 53));
        addSlot(validated(4, 98, 53));
        addSlot(validated(5, 152, 17));
        addSlot(takeOnly(6, 152, 53));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return machine.canPlaceItem(index, stack);
            }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineLargeTurbine resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineLargeTurbine turbine ? turbine : null;
    }

    public @Nullable TileEntityMachineLargeTurbine getTurbine() { return turbine; }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
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
            } else if (stack.getItem() instanceof IItemFluidId) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 2, 3, false) && !moveItemStackTo(stack, 5, 6, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
