package com.hbm.inventory.container;

import com.hbm.tileentity.machine.TileEntityDiFurnace;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import dev.golem.core.gui.CompatContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;

/** Four-slot DiFurnace menu, including the legacy right-click side-mode controls. */
public class ContainerDiFurnace extends CompatContainerMenu {

    public static final int MACHINE_SLOTS = 4;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityDiFurnace furnace;

    public ContainerDiFurnace(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerDiFurnace(int id, Inventory playerInventory, TileEntityDiFurnace furnace) {
        this(id, playerInventory, furnace, ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos()));
    }

    private ContainerDiFurnace(int id, Inventory playerInventory, @Nullable TileEntityDiFurnace furnace,
                               ContainerLevelAccess access) {
        super(ModMenus.MACHINE_DIFURNACE.get(), id);
        this.furnace = furnace;
        this.machine = furnace != null ? furnace : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        addSlot(validated(0, 80, 18));
        addSlot(validated(1, 80, 54));
        addSlot(validated(2, 8, 36));
        addSlot(takeOnly(3, 134, 36));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
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
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityDiFurnace resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityDiFurnace furnace ? furnace : null;
    }

    public @Nullable TileEntityDiFurnace getFurnace() {
        return furnace;
    }

    @Override
    protected boolean handleClick(int slotId, int button, ClickAction clickType, Player player) {
        if (furnace != null && slotId >= 0 && slotId < 3 && button == 1
                && clickType == ClickAction.PICKUP && !slots.get(slotId).hasItem()
                && getCarried().isEmpty()) {
            if (!player.level().isClientSide()) furnace.cycleSide(slotId);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.machine_difurnace_off.get());
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
            } else if (furnace != null && furnace.hasItemPower(stack)) {
                if (!moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 2, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
