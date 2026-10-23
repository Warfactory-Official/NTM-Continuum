package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron;
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

/** Pos-synchronised menu for the fusion klystron: one battery slot, plus the output-target field. */
public class ContainerFusionKlystron extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 1;
    private static final int PLAYER_INV_START = MACHINE_SLOTS;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityFusionKlystron klystron;

    /** Client-side constructor; {@code pos} comes from the loader's menu-open payload. */
    public ContainerFusionKlystron(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    /** Server-side constructor, bound to the live emitter. */
    public ContainerFusionKlystron(int containerId, Inventory playerInventory, TileEntityFusionKlystron klystron) {
        this(containerId, playerInventory, klystron,
                ContainerLevelAccess.create(klystron.getLevel(), klystron.getBlockPos()));
    }

    private ContainerFusionKlystron(int containerId, Inventory playerInventory,
                                    @Nullable TileEntityFusionKlystron klystron, ContainerLevelAccess access) {
        super(ModMenus.FUSION_KLYSTRON.get(), containerId);
        this.klystron = klystron;
        this.machine = klystron != null ? klystron : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(new Slot(this.machine, 0, 8, 72) {
            @Override public boolean mayPlace(ItemStack stack) {
                return ContainerFusionKlystron.this.machine.canPlaceItem(0, stack);
            }
        });

        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 17 + column * 18, 118 + row * 18));
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 17 + column * 18, 176));
        }
    }

    private static @Nullable TileEntityFusionKlystron resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityFusionKlystron emitter ? emitter : null;
    }

    public @Nullable TileEntityFusionKlystron getKlystron() {
        return klystron;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.fusion_klystron.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, true)) return ItemStack.EMPTY;
        } else if (Library.isBattery(stack)) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
