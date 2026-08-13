package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityMachineChemicalPlant;
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
 * Menu for the chemical plant: battery, template, 2 upgrades, 3 item-in, 3 item-out, 3 fluid-in load
 * (+3 emptied-bucket out), 3 fluid-out load (+3 filled-bucket out), and the player inventory. Output and
 * "bucket out" slots are take-only. Layout mirrors the 1.12.2 {@code ContainerMachineChemicalPlant}.
 */
public class ContainerMachineChemicalPlant extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 22;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineChemicalPlant chemplant;

    /** Client-side: resolve the BlockEntity by position. */
    public ContainerMachineChemicalPlant(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    /** Server-side: bound to the live BlockEntity. */
    public ContainerMachineChemicalPlant(int containerId, Inventory playerInventory, TileEntityMachineChemicalPlant machine) {
        this(containerId, playerInventory, machine, ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()));
    }

    private ContainerMachineChemicalPlant(int containerId, Inventory playerInventory, @Nullable TileEntityMachineChemicalPlant machine, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_CHEMICAL_PLANT.get(), containerId);
        this.chemplant = machine;
        this.machine = machine != null ? machine : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 152, 81));   // battery
        this.addSlot(validated(1, 35, 126));    // template
        this.addSlot(validated(2, 152, 108));   // upgrade
        this.addSlot(validated(3, 152, 126));   // upgrade

        for (int i = 0; i < 3; i++) this.addSlot(validated(4 + i, 8 + i * 18, 99));   // item input
        for (int i = 0; i < 3; i++) this.addSlot(takeOnly(7 + i, 80 + i * 18, 99));   // item output
        for (int i = 0; i < 3; i++) this.addSlot(validated(10 + i, 8 + i * 18, 54));  // fluid-in load
        for (int i = 0; i < 3; i++) this.addSlot(takeOnly(13 + i, 8 + i * 18, 72));   // emptied bucket out
        for (int i = 0; i < 3; i++) this.addSlot(validated(16 + i, 80 + i * 18, 54)); // fluid-out load
        for (int i = 0; i < 3; i++) this.addSlot(takeOnly(19 + i, 80 + i * 18, 72));  // filled bucket out

        //? if >=26.1 {
        this.addStandardInventorySlots(playerInventory, 8, 174);
        //?} else {
        /*Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 174);
        *///?}
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachineChemicalPlant.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineChemicalPlant resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineChemicalPlant machine ? machine : null;
    }

    /** The live (client) BlockEntity, for the screen to read power/progress/tanks/recipe. */
    public @Nullable TileEntityMachineChemicalPlant getChemplant() {
        return chemplant;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModMachineBlocks.machine_chemical_plant.get());
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
            } else if (stack.getItem() instanceof com.hbm.items.machine.ItemMachineUpgrade) {
                // upgrades go straight to the upgrade slots (the 1.12.2 transferStackInSlot routing)
                if (!this.moveItemStackTo(stack, 2, 4, false)) return ItemStack.EMPTY;
            } else {
                // player inv -> machine: only the insertable slots (battery/template/upgrades/item-in/bucket-in)
                if (!this.moveItemStackTo(stack, 0, 7, false)
                        && !this.moveItemStackTo(stack, 10, 13, false)
                        && !this.moveItemStackTo(stack, 16, 19, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
