package com.hbm.inventory.container;

import com.hbm.inventory.SlotCraftingOutput;
import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachinePrecAss;
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

/** Menu for the precision assembler: 9 inputs, 9 outputs, battery, blueprint, and two upgrades. */
public final class ContainerMachinePrecAss extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 22;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachinePrecAss assembler;

    public ContainerMachinePrecAss(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachinePrecAss(int containerId, Inventory playerInventory, TileEntityMachinePrecAss assembler) {
        this(containerId, playerInventory, assembler,
                ContainerLevelAccess.create(assembler.getLevel(), assembler.getBlockPos()));
    }

    private ContainerMachinePrecAss(int containerId, Inventory playerInventory,
                                    @Nullable TileEntityMachinePrecAss assembler, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_PREC_ASS.get(), containerId);
        this.assembler = assembler;
        this.machine = assembler != null ? assembler : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        addSlot(validated(0, 152, 81));
        addSlot(validated(1, 35, 126));
        addSlot(validated(2, 152, 108));
        addSlot(validated(3, 152, 126));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++)
            addSlot(validated(4 + row * 3 + col, 8 + col * 18, 27 + row * 18));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++)
            addSlot(new SlotCraftingOutput(playerInventory.player, this.machine, 13 + row * 3 + col, 80 + col * 18, 27 + row * 18)); // 1.7 addOutputSlots
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 174);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return ContainerMachinePrecAss.this.machine.canPlaceItem(index, stack);
            }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachinePrecAss resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachinePrecAss assembler ? assembler : null;
    }

    public @Nullable TileEntityMachinePrecAss getAssembler() {
        return assembler;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.machine_precass.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MACHINE_SLOTS;
            int invEnd = MACHINE_SLOTS + 36;
            if (index < invStart) {
                if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemBlueprints) {
                if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else if (Library.isMachineUpgrade(stack)) {
                if (!moveItemStackTo(stack, 2, 4, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 4, 13, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            slot.onTake(player, result); // 1.7 transferStackInSlot: slot.onPickupFromSlot (SlotCraftingOutput)
        }
        return result;
    }
}
