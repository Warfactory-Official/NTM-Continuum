package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
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

/** Menu for the assembly machine: battery, template, 2 upgrades, 12 inputs, 1 output (+ player inv). */
public class ContainerMachineAssemblyMachine extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 17;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineAssemblyMachine assembler;

    /** Client-side: resolve the BlockEntity by position. */
    public ContainerMachineAssemblyMachine(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    /** Server-side: bound to the live BlockEntity. */
    public ContainerMachineAssemblyMachine(int containerId, Inventory playerInventory, TileEntityMachineAssemblyMachine machine) {
        this(containerId, playerInventory, machine, ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()));
    }

    private ContainerMachineAssemblyMachine(int containerId, Inventory playerInventory, @Nullable TileEntityMachineAssemblyMachine machine, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_ASSEMBLY_MACHINE.get(), containerId);
        this.assembler = machine;
        this.machine = machine != null ? machine : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 152, 81));                          // battery
        this.addSlot(validated(1, 35, 126));                          // template
        this.addSlot(validated(2, 152, 108));                         // upgrade (1.12.2 addSlots 2,152,108,rows=2,cols=1 → vertical)
        this.addSlot(validated(3, 152, 126));                         // upgrade
        for (int row = 0; row < 4; row++) for (int col = 0; col < 3; col++) {
            this.addSlot(validated(4 + row * 3 + col, 8 + col * 18, 18 + row * 18)); // 12 inputs (3 wide × 4 tall)
        }
        this.addSlot(new Slot(this.machine, 16, 98, 45) {             // output (take only)
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        //? if >=26.1 {
        this.addStandardInventorySlots(playerInventory, 8, 174);
        //?} else {
        /*Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 174);
        *///?}
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachineAssemblyMachine.this.machine.canPlaceItem(index, stack); }
        };
    }

    private static @Nullable TileEntityMachineAssemblyMachine resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineAssemblyMachine machine ? machine : null;
    }

    /** The live (client) BlockEntity, for the screen to read power/progress/recipe. Null only if unresolved. */
    public @Nullable TileEntityMachineAssemblyMachine getAssembler() {
        return assembler;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModMachineBlocks.machine_assembly_machine.get());
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
                // player inv -> machine: try template/inputs (skip output slot 16)
                if (!this.moveItemStackTo(stack, 0, 16, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
