package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntitySILEX;
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

/**
 * Menu for the SILEX — the 1.12.2 {@code ContainerSILEX} layout, coordinates verbatim.
 *
 * <p>Slots: <b>0</b> input (80,12) · <b>1</b> fluid identifier (8,24) · <b>2</b> fluid-container in
 * (26,24) · <b>3</b> emptied-container out, take-only (44,24) · <b>4</b> staging output (116,90) ·
 * <b>5–10</b> the 3×2 output queue at (134/152, 72/90/108).
 *
 * <p>The player inventory sits unusually low because the SILEX GUI is 222px tall (rows at y 140/158/176,
 * hotbar at 198) — upstream expressed that as {@code 84 + i*18 + 18*3 + 2} and {@code 142 + 18*3 + 2}.
 */
public class ContainerSILEX extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 11;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntitySILEX silex;

    public ContainerSILEX(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerSILEX(int containerId, Inventory playerInventory, TileEntitySILEX machine) {
        this(containerId, playerInventory, machine,
                ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()));
    }

    private ContainerSILEX(int containerId, Inventory playerInventory, @Nullable TileEntitySILEX machine,
                           ContainerLevelAccess access) {
        super(ModMenus.MACHINE_SILEX.get(), containerId);
        this.silex = machine;
        this.machine = machine != null ? machine : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 80, 12));    // input
        this.addSlot(validated(1, 8, 24));     // fluid identifier
        this.addSlot(validated(2, 26, 24));    // fluid-container in
        this.addSlot(takeOnly(3, 44, 24));     // emptied-container out
        this.addSlot(takeOnly(4, 116, 90));    // staging output
        this.addSlot(takeOnly(5, 134, 72));    // queue
        this.addSlot(takeOnly(6, 152, 72));
        this.addSlot(takeOnly(7, 134, 90));
        this.addSlot(takeOnly(8, 152, 90));
        this.addSlot(takeOnly(9, 134, 108));
        this.addSlot(takeOnly(10, 152, 108));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 140);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerSILEX.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntitySILEX resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntitySILEX machine ? machine : null;
    }

    /** The live (client) BlockEntity, for the screen's solution/tank/progress bars and wave trace. */
    public @Nullable TileEntitySILEX getSilex() {
        return silex;
    }

    @Override
    public boolean stillValid(Player player) {
        return Menus.stillValid(access, player, ModMachineBlocks.machine_silex.get());
    }

    /** The GUI's single "void contents" button, routed through the vanilla button path. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || silex == null) return false;
        silex.handleButtonPacket(0, 0);
        return true;
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
