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
import com.hbm.inventory.ModProductionMenus;
import com.hbm.tileentity.machine.TileEntityMicrowave;

/** 1.7 {@code ContainerMicrowave}: input (80,35), output (140,35), battery (8,53); 176x168. Buttons: 0 = faster, 1 = slower. */
public final class ContainerMicrowave extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 3;

    private final Container machine;
    private final @Nullable TileEntityMicrowave microwave;

    public ContainerMicrowave(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerMicrowave(int containerId, Inventory playerInventory, @Nullable TileEntityMicrowave microwave) {
        super(ModProductionMenus.MICROWAVE.get(), containerId);
        this.microwave = microwave;
        this.machine = microwave != null ? microwave : new SimpleContainer(MACHINE_SLOTS);

        addSlot(new Slot(machine, 0, 80, 35));
        addSlot(new Slot(machine, 1, 140, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addSlot(new Slot(machine, 2, 8, 53));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private static @Nullable TileEntityMicrowave resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMicrowave microwave ? microwave : null;
    }

    public @Nullable TileEntityMicrowave getMicrowave() { return microwave; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (microwave == null || id < 0 || id > 1) return false;
        microwave.handleButtonPacket(id, 0);
        return true;
    }

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

            if (index <= 2) {
                if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, 1, true)) {
                    if (!moveItemStackTo(stack, 2, 3, true)) return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
