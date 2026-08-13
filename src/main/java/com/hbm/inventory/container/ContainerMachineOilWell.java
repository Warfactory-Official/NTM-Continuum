package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.inventory.ModMenus;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.oil.TileEntityOilDrillBase;
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
 * Menu shared by the oil derrick and the pumpjack (both are {@link TileEntityOilDrillBase}) — the
 * 1.12.2 {@code ContainerMachineOilWell} layout: battery (8,53), oil container pair (80,17)/(80,53),
 * gas container pair (125,17)/(125,53), 3 upgrade slots (152,17/35/53); 176×222 with the player
 * inventory at 84/142.
 */
public class ContainerMachineOilWell extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 8;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityOilDrillBase drill;

    /** Client-side: resolve the BlockEntity by position. */
    public ContainerMachineOilWell(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    /** Server-side: bound to the live BlockEntity. */
    public ContainerMachineOilWell(int containerId, Inventory playerInventory, TileEntityOilDrillBase drill) {
        this(containerId, playerInventory, drill, ContainerLevelAccess.create(drill.getLevel(), drill.getBlockPos()));
    }

    private ContainerMachineOilWell(int containerId, Inventory playerInventory, @Nullable TileEntityOilDrillBase drill, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_OIL_WELL.get(), containerId);
        this.drill = drill;
        this.machine = drill != null ? drill : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 8, 53));    // battery
        this.addSlot(validated(1, 80, 17));   // oil container in
        this.addSlot(takeOnly(2, 80, 53));    // oil container out
        this.addSlot(validated(3, 125, 17));  // gas container in
        this.addSlot(takeOnly(4, 125, 53));   // gas container out
        this.addSlot(validated(5, 152, 17));  // upgrades
        this.addSlot(validated(6, 152, 35));
        this.addSlot(validated(7, 152, 53));

        //? if >=26.1 {
        this.addStandardInventorySlots(playerInventory, 8, 84);
        //?} else {
        /*Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
        *///?}
        // rows 84/102/120, hotbar 142 (old layout)
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachineOilWell.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityOilDrillBase resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityOilDrillBase drill ? drill : null;
    }

    /** The live (client) BlockEntity, for the screen to read power/indicator/tanks. */
    public @Nullable TileEntityOilDrillBase getDrill() {
        return drill;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.machine.stillValid(player);
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
            } else if (Library.isMachineUpgrade(stack)) {
                if (!this.moveItemStackTo(stack, 5, 8, false)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else {
                // fluid containers → the oil load slot, then the gas load slot (old TransferStrategy rules)
                if (!this.moveItemStackTo(stack, 1, 2, false)
                        && !this.moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
