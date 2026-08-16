package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityFEL;
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
 * Menu for the FEL — the 1.12.2 {@code ContainerFEL} layout, coordinates verbatim: the battery bay at
 * (182, 144) and the laser-crystal slot at (141, 23), with the player inventory at (8, 83).
 *
 * <p>The GUI is 203px wide, which is why the battery sits so far right.
 */
public class ContainerFEL extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 2;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityFEL fel;

    public ContainerFEL(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerFEL(int containerId, Inventory playerInventory, TileEntityFEL machine) {
        this(containerId, playerInventory, machine,
                ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()));
    }

    private ContainerFEL(int containerId, Inventory playerInventory, @Nullable TileEntityFEL machine,
                         ContainerLevelAccess access) {
        super(ModMenus.MACHINE_FEL.get(), containerId);
        this.fel = machine;
        this.machine = machine != null ? machine : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 182, 144));   // battery
        this.addSlot(validated(1, 141, 23));    // laser crystal

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 83);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerFEL.this.machine.canPlaceItem(index, stack); }
        };
    }

    private static @Nullable TileEntityFEL resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityFEL machine ? machine : null;
    }

    /** The live (client) BlockEntity, for the screen's power bar, status text and in-GUI beam. */
    public @Nullable TileEntityFEL getFel() {
        return fel;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModMachineBlocks.machine_fel.get());
    }

    /** The GUI's power switch. Upstream's {@code AuxButtonPacket(pos, 0, 2)} carried meta 2. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 2 || fel == null) return false;
        fel.handleButtonPacket(0, 2);
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
