package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
import com.hbm.inventory.ModMenus;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
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

/** Pos-synchronised menu for the 24-channel ZIRNOX reactor. */
public class ContainerReactorZirnox extends AbstractContainerMenu {

    public static final int ROD_SLOTS = 24;
    public static final int MACHINE_SLOTS = 28;
    private static final int PLAYER_INV_START = MACHINE_SLOTS;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityReactorZirnox reactor;

    /** Client-side constructor; {@code pos} is supplied by the loader's menu-open payload. */
    public ContainerReactorZirnox(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    /** Server-side constructor, bound to the live reactor. */
    public ContainerReactorZirnox(int containerId, Inventory playerInventory, TileEntityReactorZirnox reactor) {
        this(containerId, playerInventory, reactor, ContainerLevelAccess.create(reactor.getLevel(), reactor.getBlockPos()));
    }

    private ContainerReactorZirnox(int containerId, Inventory playerInventory, @Nullable TileEntityReactorZirnox reactor,
                                   ContainerLevelAccess access) {
        super(ModMenus.REACTOR_ZIRNOX.get(), containerId);
        this.reactor = reactor;
        this.machine = reactor != null ? reactor : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        int[][] rodPositions = {
                {26, 16}, {62, 16}, {98, 16},
                {8, 34}, {44, 34}, {80, 34}, {116, 34},
                {26, 52}, {62, 52}, {98, 52},
                {8, 70}, {44, 70}, {80, 70}, {116, 70},
                {26, 88}, {62, 88}, {98, 88},
                {8, 106}, {44, 106}, {80, 106}, {116, 106},
                {26, 124}, {62, 124}, {98, 124}
        };
        for (int slot = 0; slot < ROD_SLOTS; slot++) {
            this.addSlot(validated(slot, rodPositions[slot][0], rodPositions[slot][1]));
        }

        this.addSlot(validated(24, 143, 124)); // carbon dioxide container in
        this.addSlot(validated(25, 179, 124)); // water container in
        this.addSlot(takeOnly(26, 143, 142));  // carbon dioxide container out
        this.addSlot(takeOnly(27, 179, 142));  // water container out

        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 174 + row * 18));
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 232));
        }
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return ContainerReactorZirnox.this.machine.canPlaceItem(index, stack);
            }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityReactorZirnox resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityReactorZirnox reactor ? reactor : null;
    }

    public @Nullable TileEntityReactorZirnox getReactor() {
        return reactor;
    }

    /** Button 0 toggles the reactor; button 1 releases 1000 mB of carbon dioxide. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (reactor == null || player.level().isClientSide()) return false;
        reactor.handleButtonPacket(id, 0);
        return id == 0 || id == 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return Menus.stillValid(access, player, ModMachineBlocks.machine_zirnox.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof ItemZirnoxRod) {
            if (!this.moveItemStackTo(stack, 0, ROD_SLOTS, false)) return ItemStack.EMPTY;
        } else if (provides(stack, Fluids.CARBONDIOXIDE)) {
            if (!this.moveItemStackTo(stack, 24, 25, false)) return ItemStack.EMPTY;
        } else if (provides(stack, Fluids.WATER)) {
            if (!this.moveItemStackTo(stack, 25, 26, false)) return ItemStack.EMPTY;
        } else if (index < PLAYER_INV_START + 27) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START + 27, PLAYER_INV_END, false)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_START + 27, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    private static boolean provides(ItemStack stack, FluidType fluid) {
        return stack.getItem() instanceof IFillableItem fillable && fillable.providesFluid(fluid, stack)
                || stack.getItem() instanceof ItemFluidBucket && ItemFluidBucket.getFluid(stack) == fluid;
    }
}
