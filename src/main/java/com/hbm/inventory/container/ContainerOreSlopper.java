package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.Library;
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
import com.hbm.tileentity.machine.TileEntityMachineOreSlopper;

/** 1.7 {@code ContainerOreSlopper}: 176x204, battery, fluid identifier, one input, six outputs, two upgrades. */
public final class ContainerOreSlopper extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 11;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineOreSlopper slopper;

    public ContainerOreSlopper(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerOreSlopper(int containerId, Inventory playerInventory, TileEntityMachineOreSlopper slopper) {
        this(containerId, playerInventory, slopper, ContainerLevelAccess.create(slopper.getLevel(), slopper.getBlockPos()));
    }

    private ContainerOreSlopper(int containerId, Inventory playerInventory,
                                @Nullable TileEntityMachineOreSlopper slopper, ContainerLevelAccess access) {
        super(ModProduction2Menus.ORE_SLOPPER.get(), containerId);
        this.slopper = slopper;
        this.machine = slopper != null ? slopper : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        // Battery
        addSlot(validated(0, 8, 72));
        // Fluid ID
        addSlot(validated(1, 26, 72));
        // Input
        addSlot(validated(2, 71, 27));
        // Outputs
        addSlot(takeOnly(3, 134, 18));
        addSlot(takeOnly(4, 152, 18));
        addSlot(takeOnly(5, 134, 36));
        addSlot(takeOnly(6, 152, 36));
        addSlot(takeOnly(7, 134, 54));
        addSlot(takeOnly(8, 152, 54));
        // Upgrades
        addSlot(validated(9, 62, 72));
        addSlot(validated(10, 80, 72));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 122);
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

    private static @Nullable TileEntityMachineOreSlopper resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineOreSlopper slopper ? slopper : null;
    }

    public @Nullable TileEntityMachineOreSlopper getSlopper() { return slopper; }

    @Override
    public boolean stillValid(Player player) {
        return dev.golem.core.gui.Menus.stillValid(access, player,
                ModProduction2Blocks.MACHINE_ORE_SLOPPER.get());
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
            } else {
                if (result.is(ModOreBlocks.bedrock_ore_base.get())) {
                    if (!moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
                } else if (Library.isMachineUpgrade(result)) {
                    if (!moveItemStackTo(stack, 9, 11, false)) return ItemStack.EMPTY;
                } else if (result.getItem() instanceof IItemFluidId) {
                    if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
                } else if (Library.isBattery(result)) {
                    if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
