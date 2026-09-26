package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachinePUREX;
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
 * Menu for the PUREX plant, laid out exactly as the 1.12.2 {@code ContainerMachinePUREX}: battery (152,81),
 * blueprint (35,126), two upgrades (152,108) and (152,126), three item inputs in a row at y=90, six
 * take-only outputs in a 2×3 block from (80,36), and the player inventory at (8,174).
 */
public class ContainerMachinePUREX extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 13;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachinePUREX purex;

    /** Client-side: resolve the BlockEntity by position. */
    public ContainerMachinePUREX(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    /** Server-side: bound to the live BlockEntity. */
    public ContainerMachinePUREX(int containerId, Inventory playerInventory, TileEntityMachinePUREX machine) {
        this(containerId, playerInventory, machine, ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()));
    }

    private ContainerMachinePUREX(int containerId, Inventory playerInventory, @Nullable TileEntityMachinePUREX machine,
                                  ContainerLevelAccess access) {
        super(ModMenus.MACHINE_PUREX.get(), containerId);
        this.purex = machine;
        this.machine = machine != null ? machine : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 152, 81));   // battery
        this.addSlot(validated(1, 35, 126));   // blueprint
        this.addSlot(validated(2, 152, 108));  // upgrade
        this.addSlot(validated(3, 152, 126));  // upgrade

        for (int i = 0; i < 3; i++) this.addSlot(validated(4 + i, 8 + i * 18, 90)); // item inputs
        for (int row = 0; row < 3; row++) {                                          // outputs, 2 per row
            for (int col = 0; col < 2; col++) this.addSlot(takeOnly(7 + row * 2 + col, 80 + col * 18, 36 + row * 18));
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 174);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachinePUREX.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachinePUREX resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachinePUREX machine ? machine : null;
    }

    /** The live (client) BlockEntity, for the screen to read power/progress/tanks/recipe. */
    public @Nullable TileEntityMachinePUREX getPurex() {
        return purex;
    }

    @Override
    public boolean stillValid(Player player) {
        return Menus.stillValid(access, player, ModMachineBlocks.machine_purex.get());
    }

    /** The 1.12.2 routing: batteries → 0, blueprints → 1, upgrades → 2–3, everything else → the inputs. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MACHINE_SLOTS, invEnd = MACHINE_SLOTS + 36;
            if (index < invStart) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemBlueprints) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemMachineUpgrade) {
                if (!this.moveItemStackTo(stack, 2, 4, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 4, 7, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
