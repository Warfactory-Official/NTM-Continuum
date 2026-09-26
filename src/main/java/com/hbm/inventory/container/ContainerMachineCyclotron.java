package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.recipes.CyclotronRecipes;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineCyclotron;
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

/** The cyclotron menu: three particle inputs, three targets, three outputs, battery, and upgrades. */
public class ContainerMachineCyclotron extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 12;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineCyclotron cyclotron;

    public ContainerMachineCyclotron(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineCyclotron(int containerId, Inventory playerInventory,
                                     TileEntityMachineCyclotron cyclotron) {
        this(containerId, playerInventory, cyclotron,
                ContainerLevelAccess.create(cyclotron.getLevel(), cyclotron.getBlockPos()));
    }

    private ContainerMachineCyclotron(int containerId, Inventory playerInventory,
                                      @Nullable TileEntityMachineCyclotron cyclotron,
                                      ContainerLevelAccess access) {
        super(ModMenus.MACHINE_CYCLOTRON.get(), containerId);
        this.cyclotron = cyclotron;
        this.machine = cyclotron != null ? cyclotron : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        addSlot(validated(0, 11, 18));
        addSlot(validated(1, 11, 36));
        addSlot(validated(2, 11, 54));
        addSlot(validated(3, 101, 18));
        addSlot(validated(4, 101, 36));
        addSlot(validated(5, 101, 54));
        addSlot(takeOnly(6, 131, 18));
        addSlot(takeOnly(7, 131, 36));
        addSlot(takeOnly(8, 131, 54));
        addSlot(validated(9, 168, 83));
        addSlot(validated(10, 60, 81));
        addSlot(validated(11, 78, 81));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 15, 133);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return ContainerMachineCyclotron.this.machine.canPlaceItem(index, stack);
            }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineCyclotron resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineCyclotron cyclotron ? cyclotron : null;
    }

    public @Nullable TileEntityMachineCyclotron getCyclotron() {
        return cyclotron;
    }

    @Override
    public boolean stillValid(Player player) {
        return Menus.stillValid(access, player, ModMachineBlocks.machine_cyclotron.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else if (CyclotronRecipes.isAnyParticle(stack)) {
                if (!moveItemStackTo(stack, 0, 3, false)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!moveItemStackTo(stack, 9, 10, false)) return ItemStack.EMPTY;
            } else if (Library.isMachineUpgrade(stack)) {
                if (!moveItemStackTo(stack, 10, 12, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 3, 6, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
