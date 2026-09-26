package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityMachineBlastFurnace;
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

/** Blast furnace menu: fuel, two inputs, two output slots, and the player inventory. */
public class ContainerBlastFurnace extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 5;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineBlastFurnace furnace;

    public ContainerBlastFurnace(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerBlastFurnace(int id, Inventory playerInventory, TileEntityMachineBlastFurnace furnace) {
        this(id, playerInventory, furnace, ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos()));
    }

    private ContainerBlastFurnace(int id, Inventory playerInventory,
                                  @Nullable TileEntityMachineBlastFurnace furnace, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_BLAST_FURNACE.get(), id);
        this.furnace = furnace;
        this.machine = furnace != null ? furnace : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 80, 81));
        this.addSlot(validated(1, 80, 27));
        this.addSlot(validated(2, 80, 45));
        this.addSlot(takeOnly(3, 134, 72));
        this.addSlot(takeOnly(4, 134, 90));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 140);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineBlastFurnace resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityMachineBlastFurnace furnace ? furnace : null;
    }

    public @Nullable TileEntityMachineBlastFurnace getFurnace() { return furnace; }

    @Override
    public boolean stillValid(Player player) {
        return Menus.stillValid(access, player, ModMachineBlocks.machine_blast_furnace.get());
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
            } else if (!moveItemStackTo(stack, 0, 3, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
