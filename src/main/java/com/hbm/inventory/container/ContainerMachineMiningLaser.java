package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityMachineMiningLaser;
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
 * Menu for the mining laser: battery (0), 2×4 upgrades (1–8) and the 3×7 take-only output buffer
 * (9–29), plus the player inventory (222-tall layout) — the 1.12.2 {@code ContainerMachineMiningLaser}
 * coordinates verbatim.
 */
public class ContainerMachineMiningLaser extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 30;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineMiningLaser laser;

    public ContainerMachineMiningLaser(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineMiningLaser(int containerId, Inventory playerInventory, TileEntityMachineMiningLaser machine) {
        this(containerId, playerInventory, machine, ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()));
    }

    private ContainerMachineMiningLaser(int containerId, Inventory playerInventory, @Nullable TileEntityMachineMiningLaser machine, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_MINING_LASER.get(), containerId);
        this.laser = machine;
        this.machine = machine != null ? machine : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 8, 108)); // battery

        // upgrades, 2 rows × 4 columns
        for (int i = 0; i < 2; i++) for (int j = 0; j < 4; j++) {
            this.addSlot(validated(1 + i * 4 + j, 98 + j * 18, 18 + i * 18));
        }
        // output buffer, 3 rows × 7 columns (take only — filled by the machine's vacuum)
        for (int i = 0; i < 3; i++) for (int j = 0; j < 7; j++) {
            this.addSlot(takeOnly(9 + i * 7 + j, 44 + j * 18, 72 + i * 18));
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 140);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachineMiningLaser.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineMiningLaser resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineMiningLaser machine ? machine : null;
    }

    /** The live (client) BlockEntity, for the screen to read power/progress/tank/state. Null only if unresolved. */
    public @Nullable TileEntityMachineMiningLaser getLaser() {
        return laser;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.machine_mining_laser.get());
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
                // upgrades go straight to the upgrade slots (the 1.12.2 TransferStrategy routing)
                if (!this.moveItemStackTo(stack, 1, 9, false)) return ItemStack.EMPTY;
            } else {
                // player inv -> machine: only the battery slot accepts anything else
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
