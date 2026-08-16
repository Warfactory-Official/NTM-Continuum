package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityMachineSolderingStation;
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
 * Menu for the soldering station: 6 grouped input slots (0–2 toppings, 3–4 pcb, 5 solder), a take-only
 * output (6), a battery slot (7), a fluid-identifier slot (8) and two (inert) upgrade slots (9–10), plus
 * the player inventory. Slot placement is gated by the machine's {@code canPlaceItem}; the screen reads the
 * live (client) BlockEntity for the power/progress/fluid bars.
 */
public class ContainerMachineSolderingStation extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 11;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineSolderingStation station;

    public ContainerMachineSolderingStation(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineSolderingStation(int containerId, Inventory playerInventory, TileEntityMachineSolderingStation machine) {
        this(containerId, playerInventory, machine, ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()));
    }

    private ContainerMachineSolderingStation(int containerId, Inventory playerInventory, @Nullable TileEntityMachineSolderingStation machine, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_SOLDERING_STATION.get(), containerId);
        this.station = machine;
        this.machine = machine != null ? machine : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        // inputs: 0-2 toppings (top row), 3-5 pcb+solder (bottom row)
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 3; j++)
                this.addSlot(validated(i * 3 + j, 17 + j * 18, 18 + i * 18));
        this.addSlot(takeOnly(6, 107, 27));    // output
        this.addSlot(validated(7, 152, 72));   // battery
        this.addSlot(validated(8, 17, 63));    // fluid identifier
        this.addSlot(validated(9, 89, 63));    // upgrade
        this.addSlot(validated(10, 107, 63));  // upgrade

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 122);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachineSolderingStation.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineSolderingStation resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineSolderingStation machine ? machine : null;
    }

    /** The live (client) BlockEntity, for the screen to read the power/progress/fluid bars. */
    public @Nullable TileEntityMachineSolderingStation getStation() {
        return station;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModMachineBlocks.machine_soldering_station.get());
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
