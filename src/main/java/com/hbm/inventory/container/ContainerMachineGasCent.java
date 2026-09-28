package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineGasCent;
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

/** The gas centrifuge menu: four outputs, battery, fluid identifier, and upgrade. */
public class ContainerMachineGasCent extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 7;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineGasCent gasCent;

    public ContainerMachineGasCent(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineGasCent(int containerId, Inventory playerInventory,
                                   TileEntityMachineGasCent gasCent) {
        this(containerId, playerInventory, gasCent,
                ContainerLevelAccess.create(gasCent.getLevel(), gasCent.getBlockPos()));
    }

    private ContainerMachineGasCent(int containerId, Inventory playerInventory,
                                    @Nullable TileEntityMachineGasCent gasCent,
                                    ContainerLevelAccess access) {
        super(ModMenus.MACHINE_GAS_CENT.get(), containerId);
        this.gasCent = gasCent;
        this.machine = gasCent != null ? gasCent : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                addSlot(new com.hbm.inventory.SlotCraftingOutput(playerInventory.player, this.machine, column + row * 2, 71 + column * 18, 53 + row * 18)); // 1.7 SlotCraftingOutput
            }
        }
        addSlot(validated(4, 182, 71));
        addSlot(validated(5, 91, 15));
        addSlot(validated(6, 69, 15));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 122);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return machine.canPlaceItem(index, stack);
            }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
    }

    private static @Nullable TileEntityMachineGasCent resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof TileEntityMachineGasCent gasCent ? gasCent : null;
    }

    public @Nullable TileEntityMachineGasCent getGasCent() {
        return gasCent;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player,
                com.hbm.blocks.machine.ModMachineBlocks.machine_gascent.get());
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
            } else if (Library.isBattery(stack)) {
                if (!moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidId) {
                if (!moveItemStackTo(stack, 5, 6, false)) return ItemStack.EMPTY;
            } else if (Library.isMachineUpgrade(stack)) {
                if (!moveItemStackTo(stack, 6, 7, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
