package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
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
import com.hbm.blocks.ModProduction2Blocks;
import com.hbm.inventory.ModProduction2Menus;
import com.hbm.tileentity.machine.TileEntityRtgFurnace;

/** 1.7 {@code ContainerRtgFurnace}: 176x166, input, three pellets, output. */
public final class ContainerRtgFurnace extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 5;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityRtgFurnace furnace;

    public ContainerRtgFurnace(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerRtgFurnace(int containerId, Inventory playerInventory, TileEntityRtgFurnace furnace) {
        this(containerId, playerInventory, furnace, ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos()));
    }

    private ContainerRtgFurnace(int containerId, Inventory playerInventory,
                                @Nullable TileEntityRtgFurnace furnace, ContainerLevelAccess access) {
        super(ModProduction2Menus.RTG_FURNACE.get(), containerId);
        this.furnace = furnace;
        this.machine = furnace != null ? furnace : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        addSlot(new Slot(machine, 0, 56, 17));
        addSlot(new Slot(machine, 1, 38, 53));
        addSlot(new Slot(machine, 2, 56, 53));
        addSlot(new Slot(machine, 3, 74, 53));
        addSlot(new Slot(machine, 4, 116, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private static @Nullable TileEntityRtgFurnace resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityRtgFurnace furnace ? furnace : null;
    }

    public @Nullable TileEntityRtgFurnace getFurnace() { return furnace; }

    @Override
    public boolean stillValid(Player player) {
        return dev.golem.core.gui.Menus.stillValid(access, player,
                ModProduction2Blocks.MACHINE_RTG_FURNACE.get());
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
            } else if (!moveItemStackTo(stack, 0, 4, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
