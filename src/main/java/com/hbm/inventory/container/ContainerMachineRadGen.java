package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityMachineRadGen;
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

/** Four rows of fuel channels and their matching output slots, plus the player inventory. */
public class ContainerMachineRadGen extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 24;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineRadGen generator;

    public ContainerMachineRadGen(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineRadGen(int containerId, Inventory playerInventory, TileEntityMachineRadGen generator) {
        this(containerId, playerInventory, generator,
                ContainerLevelAccess.create(generator.getLevel(), generator.getBlockPos()));
    }

    private ContainerMachineRadGen(int containerId, Inventory playerInventory,
                                   @Nullable TileEntityMachineRadGen generator, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_RAD_GEN.get(), containerId);
        this.generator = generator;
        this.machine = generator != null ? generator : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 3; column++) {
                int slot = column + row * 3;
                addSlot(validated(slot, 8 + column * 18, 17 + row * 18));
            }
        }
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 3; column++) {
                int slot = column + row * 3 + 12;
                addSlot(takeOnly(slot, 116 + column * 18, 17 + row * 18));
            }
        }
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 102);
    }

    private Slot validated(int machineSlot, int x, int y) {
        return new Slot(machine, machineSlot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return machine.canPlaceItem(machineSlot, stack);
            }
        };
    }

    private Slot takeOnly(int machineSlot, int x, int y) {
        return new Slot(machine, machineSlot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
    }

    private static @Nullable TileEntityMachineRadGen resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof TileEntityMachineRadGen generator ? generator : null;
    }

    public @Nullable TileEntityMachineRadGen getGenerator() {
        return generator;
    }

    @Override
    public boolean stillValid(Player player) {
        return Menus.stillValid(access, player, ModMachineBlocks.machine_radgen.get());
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
            } else if (!moveItemStackTo(stack, 0, 12, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
