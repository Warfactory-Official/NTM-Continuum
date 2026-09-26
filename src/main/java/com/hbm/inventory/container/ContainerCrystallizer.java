package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityMachineCrystallizer;
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
 * Menu for the ore acidizer: input (0), battery (1), take-only output (2), fluid-container in (3) →
 * emptied-container out (4, take-only), two inert upgrade slots (5–6) and the fluid-identifier slot
 * (7), plus the player inventory — the 1.12.2 {@code ContainerCrystallizer} layout. Slot placement is
 * gated by the machine's {@code canPlaceItem}; the screen reads the live (client) BlockEntity for the
 * power/progress/fluid bars.
 */
public class ContainerCrystallizer extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 8;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineCrystallizer crystallizer;

    public ContainerCrystallizer(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerCrystallizer(int containerId, Inventory playerInventory, TileEntityMachineCrystallizer machine) {
        this(containerId, playerInventory, machine, ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()));
    }

    private ContainerCrystallizer(int containerId, Inventory playerInventory, @Nullable TileEntityMachineCrystallizer machine, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_CRYSTALLIZER.get(), containerId);
        this.crystallizer = machine;
        this.machine = machine != null ? machine : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 62, 45));    // input
        this.addSlot(validated(1, 152, 72));   // battery
        this.addSlot(takeOnly(2, 113, 45));    // output
        this.addSlot(validated(3, 17, 18));    // fluid-container in
        this.addSlot(takeOnly(4, 17, 54));     // emptied-container out
        this.addSlot(validated(5, 80, 18));    // upgrade
        this.addSlot(validated(6, 98, 18));    // upgrade
        this.addSlot(validated(7, 35, 72));    // fluid identifier

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 122);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerCrystallizer.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineCrystallizer resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineCrystallizer machine ? machine : null;
    }

    /** The live (client) BlockEntity, for the screen to read the power/progress/fluid bars. */
    public @Nullable TileEntityMachineCrystallizer getCrystallizer() {
        return crystallizer;
    }

    @Override
    public boolean stillValid(Player player) {
        return Menus.stillValid(access, player, ModMachineBlocks.machine_crystallizer.get());
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
