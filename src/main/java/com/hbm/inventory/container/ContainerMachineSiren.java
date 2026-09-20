package com.hbm.inventory.container;

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
import com.hbm.inventory.ModPowerGenMenus;
import com.hbm.tileentity.machine.TileEntityMachineSiren;

/** 1.7 {@code ContainerMachineSiren}: the cassette slot at (8,35) over the standard 176x166 player inventory. */
public final class ContainerMachineSiren extends AbstractContainerMenu {

    private final Container machine;
    private final @Nullable TileEntityMachineSiren siren;

    public ContainerMachineSiren(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerMachineSiren(int containerId, Inventory playerInventory, @Nullable TileEntityMachineSiren siren) {
        super(ModPowerGenMenus.SIREN.get(), containerId);
        this.siren = siren;
        this.machine = siren != null ? siren : new SimpleContainer(1);

        addSlot(new Slot(machine, 0, 8, 35));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private static @Nullable TileEntityMachineSiren resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineSiren siren ? siren : null;
    }

    public @Nullable TileEntityMachineSiren getSiren() { return siren; }

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

            if (index <= 0) {
                if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
