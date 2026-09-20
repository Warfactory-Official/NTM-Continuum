package com.hbm.inventory.container;

import com.hbm.tileentity.machine.storage.TileEntityBarrel;
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
import com.hbm.inventory.ModFluidNetMenus;

/**
 * The 1.7 {@code ContainerBarrel}: six machine slots at the 1.7 positions (identifier in/out at x=8,
 * drain in/out at x=35, fill in/out at x=125; rows y=17/53) plus the player inventory at (8, 84). The
 * mode button reaches the tile through {@link #clickMenuButton}, the 1.7 {@code AuxButtonPacket}.
 */
public class ContainerBarrel extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 6;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityBarrel barrel;

    public ContainerBarrel(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerBarrel(int containerId, Inventory playerInventory, TileEntityBarrel barrel) {
        this(containerId, playerInventory, barrel, ContainerLevelAccess.create(barrel.getLevel(), barrel.getBlockPos()));
    }

    private ContainerBarrel(int containerId, Inventory playerInventory, @Nullable TileEntityBarrel barrel, ContainerLevelAccess access) {
        super(ModFluidNetMenus.BARREL.get(), containerId);
        this.barrel = barrel;
        this.machine = barrel != null ? barrel : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 8, 17));
        this.addSlot(validated(1, 8, 53));
        this.addSlot(validated(2, 53 - 18, 17));
        this.addSlot(takeOnly(3, 53 - 18, 53));
        this.addSlot(validated(4, 125, 17));
        this.addSlot(takeOnly(5, 125, 53));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerBarrel.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityBarrel resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityBarrel barrel ? barrel : null;
    }

    public @Nullable TileEntityBarrel getBarrel() {
        return barrel;
    }

    /** Button 0 = cycle the I/O mode (1.7 AuxButtonPacket id 0). */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && barrel != null) {
            barrel.cycleMode();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockEntity(pos) instanceof TileEntityBarrel
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D, true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MACHINE_SLOTS;
            int invEnd = MACHINE_SLOTS + 36;
            if (index < invStart) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
