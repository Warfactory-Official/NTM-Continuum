package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.tileentity.machine.TileEntityMachineGiantTank;
import dev.golem.core.world.Compat;
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

/** Shared six-slot menu used by the legacy Big Ass Tank and BAT-9000. */
public class ContainerMachineGiantTank extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 6;
    private final Container machine;
    private final @Nullable TileEntityMachineGiantTank tank;

    public ContainerMachineGiantTank(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerMachineGiantTank(int containerId, Inventory playerInventory,
                                     @Nullable TileEntityMachineGiantTank tank) {
        super(ModMenus.GIANT_TANK.get(), containerId);
        this.tank = tank;
        this.machine = tank != null ? tank : new SimpleContainer(MACHINE_SLOTS);

        this.addSlot(validated(0, 8, 17));
        this.addSlot(takeOnly(1, 8, 53));
        this.addSlot(validated(2, 35, 17));
        this.addSlot(takeOnly(3, 35, 53));
        this.addSlot(validated(4, 125, 17));
        this.addSlot(takeOnly(5, 125, 53));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ContainerMachineGiantTank.this.machine.canPlaceItem(index, stack);
            }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineGiantTank resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityMachineGiantTank tank ? tank : null;
    }

    public @Nullable TileEntityMachineGiantTank getTank() {
        return tank;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && tank != null) {
            tank.mode = (short) ((tank.mode + 1) % 4);
            tank.setChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return tank != null && tank.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int inventoryStart = MACHINE_SLOTS;
            int inventoryEnd = MACHINE_SLOTS + 36;
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, inventoryStart, inventoryEnd, true)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidId) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (machine.canPlaceItem(2, stack)) {
                if (!moveItemStackTo(stack, 2, 4, false)) return ItemStack.EMPTY;
            } else if (machine.canPlaceItem(4, stack)) {
                if (!moveItemStackTo(stack, 4, 6, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
