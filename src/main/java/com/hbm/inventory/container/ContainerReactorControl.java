package com.hbm.inventory.container;

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
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.inventory.ModNuclearMiscMenus;
import com.hbm.tileentity.machine.TileEntityReactorControl;

/** The 1.7 {@code ContainerReactorControl}: the single sensor slot at (92,38). */
public final class ContainerReactorControl extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS = 1;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityReactorControl control;

    public ContainerReactorControl(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerReactorControl(int id, Inventory playerInventory, TileEntityReactorControl tile) {
        this(id, playerInventory, tile, ContainerLevelAccess.create(tile.getLevel(), tile.getBlockPos()));
    }

    private ContainerReactorControl(int id, Inventory playerInventory, @Nullable TileEntityReactorControl tile, ContainerLevelAccess access) {
        super(ModNuclearMiscMenus.REACTOR_CONTROL.get(), id);
        this.control = tile;
        this.machine = tile == null ? new SimpleContainer(MACHINE_SLOTS) : tile;
        this.access = access;

        addSlot(new Slot(machine, 0, 92, 38));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private static @Nullable TileEntityReactorControl resolve(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof TileEntityReactorControl found ? found : null;
    }

    public @Nullable TileEntityReactorControl getControl() { return control; }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModNuclearMiscBlocks.MACHINE_CONTROLLER.get());
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
            } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
