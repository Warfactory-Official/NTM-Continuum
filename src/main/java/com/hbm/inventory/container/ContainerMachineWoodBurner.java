package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineWoodBurner;
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

/** Six-slot menu matching the old Wood Burner GUI. */
public class ContainerMachineWoodBurner extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS = 6;

    private final Container machine;
    private final @Nullable TileEntityMachineWoodBurner burner;

    public ContainerMachineWoodBurner(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerMachineWoodBurner(int id, Inventory playerInventory, @Nullable TileEntityMachineWoodBurner burner) {
        super(ModMenus.MACHINE_WOOD_BURNER.get(), id);
        this.burner = burner;
        this.machine = burner != null ? burner : new SimpleContainer(MACHINE_SLOTS);

        addSlot(validated(0, 26, 18));
        addSlot(takeOnly(1, 26, 54));
        addSlot(validated(2, 98, 54));
        addSlot(validated(3, 98, 18));
        addSlot(takeOnly(4, 98, 36));
        addSlot(validated(5, 143, 54));

        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 8 + col * 18, 162));

        if (burner != null && !playerInventory.player.level().isClientSide()) burner.playersUsing++;
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

    private static @Nullable TileEntityMachineWoodBurner resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineWoodBurner burner ? burner : null;
    }

    public @Nullable TileEntityMachineWoodBurner getBurner() { return burner; }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (burner == null) return false;
        burner.handleButton(id);
        return true;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (burner != null && !player.level().isClientSide() && burner.playersUsing > 0) burner.playersUsing--;
    }

    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else if (burner != null && Library.isBattery(stack)) {
                if (!moveItemStackTo(stack, 5, 6, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidId) {
                if (!moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
            } else if (machine.canPlaceItem(0, stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
